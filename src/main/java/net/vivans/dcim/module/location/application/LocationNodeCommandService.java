package net.vivans.dcim.module.location.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.location.api.dto.LocationNodeBulkCreateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeCreateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeDeleteResponse;
import net.vivans.dcim.module.location.api.dto.LocationNodeParentUpdateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeResponse;
import net.vivans.dcim.module.location.api.dto.LocationNodeTreeCreateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeUpdateRequest;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.model.LocationNodeCodeGenerator;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationNodeCommandService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;
    private static final String CHILD_DEEPER_THAN_PARENT_MESSAGE = "child location type must be deeper than parent";
    private static final String CANNOT_DELETE_SYSTEM_NODE_MESSAGE = "cannot delete system location node";
    private static final String DEVICE_NAME_CONFLICT_AT_UNASSIGNED_MESSAGE =
            "device name conflict at UNASSIGNED; rename devices before deleting location";

    private final LocationNodeRepository locationNodeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final DeviceRepository deviceRepository;

    public LocationNodeResponse createLocationNode(LocationNodeCreateRequest request) {
        CommonCode locationType = findLocationType(request.locationTypeId());
        if (request.parentCode() == null || request.parentCode().isBlank()) {
            validateSiblingName(null, request.name(), null);
            LocationNode node = LocationNode.createRoot(
                    generateUniqueCode(), locationType, request.name(), request.rackUCapacity());
            return LocationNodeResponse.from(locationNodeRepository.save(node));
        }

        LocationNode parent = locationNodeRepository.findByCode(request.parentCode())
                .orElseThrow(() -> new EntityNotFoundException(
                        "LocationNode not found: " + request.parentCode()));
        validateSiblingName(parent, request.name(), null);
        validateLocationTypeDepth(parent, locationType);
        LocationNode node = LocationNode.createChild(
                generateUniqueCode(), parent, locationType, request.name(), request.rackUCapacity());
        node = locationNodeRepository.save(node);
        reconstructTreeAfterInsert(parent, node);
        return LocationNodeResponse.from(node);
    }

    public List<LocationNodeResponse> createBatchLocationNodes(LocationNodeBulkCreateRequest request) {
        LocationNode attachParent = resolveParent(blankToNull(request.parentCode()));
        Set<String> batchCodes = new HashSet<>();

        validateBatchSiblingNames(request.nodes());
        for (LocationNodeTreeCreateRequest nodeRequest : request.nodes()) {
            validateSiblingName(attachParent, nodeRequest.name(), null);
            validateLocationTypeDepth(attachParent, findLocationType(nodeRequest.locationTypeId()));
        }

        List<LocationNodeResponse> result = new ArrayList<>();
        for (LocationNodeTreeCreateRequest nodeRequest : request.nodes()) {
            result.add(createTreeNode(attachParent, nodeRequest, batchCodes));
        }
        return result;
    }

    public LocationNodeResponse updateLocationNode(String code, LocationNodeUpdateRequest request) {
        LocationNode node = findNode(code);
        CommonCode locationType = findLocationType(request.locationTypeId());
        validateSiblingName(node.getParent(), request.name(), node.getCode());
        validateLocationTypeDepth(node.getParent(), locationType);
        validateLocationTypeAgainstChildren(node, locationType);
        node.update(locationType, request.name(), request.rackUCapacity());
        return LocationNodeResponse.from(locationNodeRepository.save(node));
    }

    public LocationNodeResponse updateParentLocationNode(String code, LocationNodeParentUpdateRequest request) {
        LocationNode node = findNode(code);
        LocationNode newParent = resolveParent(request.parentCode());
        validateLocationTypeDepth(newParent, node.getLocationType());
        validateSiblingName(newParent, node.getName(), node.getCode());
        node.updateParent(newParent);
        return LocationNodeResponse.from(locationNodeRepository.save(node));
    }

    public LocationNodeDeleteResponse deleteLocationNode(String code) {
        validateNotSystemLocationNode(code);
        LocationNode node = findNode(code);
        if (locationNodeRepository.existsByParent_Code(code)) {
            throw new IllegalArgumentException("cannot delete node with children");
        }
        int reassignedCount = reassignDevicesToUnassigned(List.of(code));
        locationNodeRepository.delete(node);
        return new LocationNodeDeleteResponse(code, reassignedCount);
    }

    public LocationNodeDeleteResponse deleteLocationNodeSubtree(String code) {
        validateNotSystemLocationNode(code);
        if (!locationNodeRepository.existsByCode(code)) {
            throw new EntityNotFoundException("LocationNode not found: " + code);
        }

        List<LocationNode> subtree = filterSubtree(code, locationNodeRepository.findAll());
        int reassignedCount = reassignDevicesToUnassigned(
                subtree.stream().map(LocationNode::getCode).toList());
        locationNodeRepository.deleteAll(sortByDepthDescending(subtree));
        return new LocationNodeDeleteResponse(code, reassignedCount);
    }

    private int reassignDevicesToUnassigned(List<String> locationCodes) {
        List<Device> devicesToReassign = deviceRepository.findByLocationNodeCodeIn(locationCodes);
        if (devicesToReassign.isEmpty()) {
            return 0;
        }
        LocationNode unassigned = locationNodeRepository.findByCode(Device.UNASSIGNED_LOCATION_CODE)
                .orElseThrow(() -> new IllegalStateException("UNASSIGNED location node is not configured"));
        validateNoNameConflictAtUnassigned(devicesToReassign, unassigned);
        devicesToReassign.forEach(device -> device.reassignLocation(unassigned));
        deviceRepository.saveAll(devicesToReassign);
        return devicesToReassign.size();
    }

    private void validateNoNameConflictAtUnassigned(List<Device> devicesToReassign, LocationNode unassigned) {
        Set<String> namesInBatch = new HashSet<>();
        for (Device device : devicesToReassign) {
            if (!namesInBatch.add(device.getName())) {
                throw new ConflictException(DEVICE_NAME_CONFLICT_AT_UNASSIGNED_MESSAGE);
            }
        }
        for (Device existing : deviceRepository.findByLocationNodeCode(unassigned.getCode())) {
            if (namesInBatch.contains(existing.getName())) {
                throw new ConflictException(DEVICE_NAME_CONFLICT_AT_UNASSIGNED_MESSAGE);
            }
        }
    }

    private void validateNotSystemLocationNode(String code) {
        if (Device.UNASSIGNED_LOCATION_CODE.equals(code)) {
            throw new ConflictException(CANNOT_DELETE_SYSTEM_NODE_MESSAGE);
        }
    }

    private List<LocationNode> filterSubtree(String rootCode, List<LocationNode> allNodes) {
        Map<String, List<LocationNode>> childrenByParentCode = buildChildrenByParentCode(allNodes);
        Set<String> subtreeCodes = new HashSet<>();
        subtreeCodes.add(rootCode);
        collectDescendants(rootCode, childrenByParentCode, subtreeCodes);
        return allNodes.stream().filter(node -> subtreeCodes.contains(node.getCode())).toList();
    }

    private Map<String, List<LocationNode>> buildChildrenByParentCode(List<LocationNode> nodes) {
        Map<String, List<LocationNode>> childrenByParentCode = new HashMap<>();
        for (LocationNode node : nodes) {
            if (node.getParent() != null) {
                childrenByParentCode.computeIfAbsent(
                        node.getParent().getCode(), ignored -> new ArrayList<>()).add(node);
            }
        }
        return childrenByParentCode;
    }

    private void collectDescendants(
            String code,
            Map<String, List<LocationNode>> childrenByParentCode,
            Set<String> subtreeCodes
    ) {
        for (LocationNode child : childrenByParentCode.getOrDefault(code, List.of())) {
            subtreeCodes.add(child.getCode());
            collectDescendants(child.getCode(), childrenByParentCode, subtreeCodes);
        }
    }

    private List<LocationNode> sortByDepthDescending(List<LocationNode> nodes) {
        Map<String, String> parentByCode = new HashMap<>();
        for (LocationNode node : nodes) {
            parentByCode.put(node.getCode(), node.getParent() == null ? null : node.getParent().getCode());
        }
        List<LocationNode> sorted = new ArrayList<>(nodes);
        sorted.sort((left, right) -> Integer.compare(
                computeDepth(right, parentByCode), computeDepth(left, parentByCode)));
        return sorted;
    }

    private int computeDepth(LocationNode node, Map<String, String> parentByCode) {
        int depth = 0;
        String parentCode = parentByCode.get(node.getCode());
        while (parentCode != null) {
            depth++;
            parentCode = parentByCode.get(parentCode);
        }
        return depth;
    }

    private LocationNode resolveParent(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return null;
        }
        return findNode(parentCode);
    }

    private LocationNode findNode(String code) {
        return locationNodeRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + code));
    }

    private CommonCode findLocationType(Integer locationTypeId) {
        return commonCodeRepository.findById(locationTypeId)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + locationTypeId));
    }

    private String generateUniqueCode() {
        return generateUniqueCode(new HashSet<>());
    }

    private String generateUniqueCode(Set<String> batchCodes) {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = LocationNodeCodeGenerator.generate();
            if (!locationNodeRepository.existsByCode(code) && batchCodes.add(code)) {
                return code;
            }
        }
        throw new IllegalStateException("failed to generate unique location node code");
    }

    private LocationNodeResponse createTreeNode(
            LocationNode parent,
            LocationNodeTreeCreateRequest request,
            Set<String> batchCodes
    ) {
        validateSiblingName(parent, request.name(), null);
        CommonCode locationType = findLocationType(request.locationTypeId());
        validateLocationTypeDepth(parent, locationType);
        LocationNode node = parent == null
                ? LocationNode.createRoot(generateUniqueCode(batchCodes), locationType, request.name())
                : LocationNode.createChild(generateUniqueCode(batchCodes), parent, locationType, request.name());
        node = locationNodeRepository.save(node);
        if (parent != null) {
            reconstructTreeAfterInsert(parent, node);
        }
        if (request.children() == null || request.children().isEmpty()) {
            return LocationNodeResponse.from(node);
        }
        validateBatchSiblingNames(request.children());
        List<LocationNodeResponse> children = new ArrayList<>();
        for (LocationNodeTreeCreateRequest child : request.children()) {
            children.add(createTreeNode(node, child, batchCodes));
        }
        return LocationNodeResponse.of(node, children);
    }

    private void validateBatchSiblingNames(List<LocationNodeTreeCreateRequest> nodes) {
        Set<String> names = new HashSet<>();
        for (LocationNodeTreeCreateRequest node : nodes) {
            if (!names.add(node.name())) {
                throw new IllegalArgumentException("name already exists under parent");
            }
        }
    }

    private void validateLocationTypeDepth(LocationNode parent, CommonCode childType) {
        if (parent != null && requireSortOrder(childType) <= requireSortOrder(parent.getLocationType())) {
            throw new IllegalArgumentException(CHILD_DEEPER_THAN_PARENT_MESSAGE);
        }
    }

    private void validateLocationTypeAgainstChildren(LocationNode node, CommonCode newType) {
        int nodeOrder = requireSortOrder(newType);
        for (LocationNode child : locationNodeRepository.findByParent_Code(node.getCode())) {
            if (requireSortOrder(child.getLocationType()) <= nodeOrder) {
                throw new IllegalArgumentException(CHILD_DEEPER_THAN_PARENT_MESSAGE);
            }
        }
    }

    private void reconstructTreeAfterInsert(LocationNode parent, LocationNode newNode) {
        int newNodeOrder = requireSortOrder(newNode.getLocationType());
        for (LocationNode sibling : locationNodeRepository.findByParent_Code(parent.getCode())) {
            if (!sibling.getCode().equals(newNode.getCode())
                    && requireSortOrder(sibling.getLocationType()) > newNodeOrder) {
                sibling.updateParent(newNode);
                locationNodeRepository.save(sibling);
            }
        }
    }

    private int requireSortOrder(CommonCode locationType) {
        if (locationType.getSortOrder() == null) {
            throw new IllegalArgumentException("location type sort order is required");
        }
        return locationType.getSortOrder();
    }

    private void validateSiblingName(LocationNode parent, String name, String excludeCode) {
        boolean duplicate;
        if (parent == null) {
            duplicate = excludeCode == null
                    ? locationNodeRepository.existsByParentIsNullAndName(name)
                    : locationNodeRepository.existsByParentIsNullAndNameAndCodeNot(name, excludeCode);
        } else {
            duplicate = excludeCode == null
                    ? locationNodeRepository.existsByParentAndName(parent, name)
                    : locationNodeRepository.existsByParentAndNameAndCodeNot(parent, name, excludeCode);
        }
        if (duplicate) {
            throw new IllegalArgumentException("name already exists under parent");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

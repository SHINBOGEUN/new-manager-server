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
@Transactional(readOnly = true)
public class LocationNodeQueryService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;
    private static final String CHILD_DEEPER_THAN_PARENT_MESSAGE = "child location type must be deeper than parent";
    private static final String CANNOT_DELETE_SYSTEM_NODE_MESSAGE = "cannot delete system location node";
    private static final String DEVICE_NAME_CONFLICT_AT_UNASSIGNED_MESSAGE = "device name conflict at UNASSIGNED; rename devices before deleting location";

    private final LocationNodeRepository locationNodeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public LocationNodeResponse createLocationNode(LocationNodeCreateRequest request) {
        CommonCode locationType = findLocationType(request.locationTypeId());

        LocationNode node;
        if (request.parentCode() == null || request.parentCode().isBlank()) {
            validateSiblingName(null, request.name(), null);
            node = LocationNode.createRoot(generateUniqueCode(), locationType, request.name());
        } else {
            LocationNode parent = locationNodeRepository.findByCode(request.parentCode())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "LocationNode not found: " + request.parentCode()));
            validateSiblingName(parent, request.name(), null);
            validateLocationTypeDepth(parent, locationType);
            node = LocationNode.createChild(
                    generateUniqueCode(), parent, locationType, request.name());
            node = locationNodeRepository.save(node);
            reconstructTreeAfterInsert(parent, node);
            return LocationNodeResponse.from(node);
        }

        return LocationNodeResponse.from(locationNodeRepository.save(node));
    }

    @Transactional
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

    @Transactional
    public LocationNodeResponse updateLocationNode(String code, LocationNodeUpdateRequest request) {
        LocationNode node = locationNodeRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + code));
        CommonCode locationType = findLocationType(request.locationTypeId());

        validateSiblingName(node.getParent(), request.name(), node.getCode());
        validateLocationTypeDepth(node.getParent(), locationType);
        validateLocationTypeAgainstChildren(node, locationType);

        node.update(locationType, request.name());

        return LocationNodeResponse.from(locationNodeRepository.save(node));
    }

    @Transactional
    public LocationNodeResponse updateParentLocationNode(String code, LocationNodeParentUpdateRequest request) {
        LocationNode node = locationNodeRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + code));
        LocationNode newParent = resolveParent(request.parentCode());

        validateLocationTypeDepth(newParent, node.getLocationType());
        validateSiblingName(newParent, node.getName(), node.getCode());
        node.updateParent(newParent);

        return LocationNodeResponse.from(locationNodeRepository.save(node));
    }

    @Transactional
    public LocationNodeDeleteResponse deleteLocationNode(String code) {
        validateNotSystemLocationNode(code);
        LocationNode node = locationNodeRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + code));
        if (locationNodeRepository.existsByParent_Code(code)) {
            throw new IllegalArgumentException("cannot delete node with children");
        }

        int reassignedCount = reassignDevicesToUnassigned(List.of(code));
        locationNodeRepository.delete(node);
        return new LocationNodeDeleteResponse(code, reassignedCount);
    }

    @Transactional
    public LocationNodeDeleteResponse deleteLocationNodeSubtree(String code) {
        validateNotSystemLocationNode(code);
        if (!locationNodeRepository.existsByCode(code)) {
            throw new EntityNotFoundException("LocationNode not found: " + code);
        }

        List<LocationNode> subtree = filterSubtree(code, locationNodeRepository.findAll());
        List<String> subtreeCodes = subtree.stream().map(LocationNode::getCode).toList();
        int reassignedCount = reassignDevicesToUnassigned(subtreeCodes);

        List<LocationNode> deleteOrder = sortByDepthDescending(subtree);
        locationNodeRepository.deleteAll(deleteOrder);
        return new LocationNodeDeleteResponse(code, reassignedCount);
    }

    private int reassignDevicesToUnassigned(List<String> locationCodes) {
        List<Device> devicesToReassign = deviceRepository.findByLocationNodeCodeIn(locationCodes);
        if (devicesToReassign.isEmpty()) {
            return 0;
        }

        LocationNode unassigned = findUnassignedLocationNode();
        validateNoNameConflictAtUnassigned(devicesToReassign, unassigned);

        for (Device device : devicesToReassign) {
            device.reassignLocation(unassigned);
        }
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

    private LocationNode findUnassignedLocationNode() {
        return locationNodeRepository.findByCode(Device.UNASSIGNED_LOCATION_CODE)
                .orElseThrow(() -> new IllegalStateException("UNASSIGNED location node is not configured"));
    }

    private void validateNotSystemLocationNode(String code) {
        if (Device.UNASSIGNED_LOCATION_CODE.equals(code)) {
            throw new ConflictException(CANNOT_DELETE_SYSTEM_NODE_MESSAGE);
        }
    }

    public List<LocationNodeResponse> getLocationNodes(String name, String parentCode, Integer locationTypeId) {
        String normalizedName = blankToNull(name);
        String normalizedParentCode = blankToNull(parentCode);

        if (normalizedParentCode != null && !locationNodeRepository.existsByCode(normalizedParentCode)) {
            throw new EntityNotFoundException("LocationNode not found: " + normalizedParentCode);
        }
        if (locationTypeId != null) {
            findLocationType(locationTypeId);
        }

        List<LocationNode> allNodes = locationNodeRepository.findAll();
        List<LocationNode> scopedNodes = normalizedParentCode == null
                ? allNodes
                : filterSubtree(normalizedParentCode, allNodes);

        List<LocationNode> nodes = applySearchFilters(
                scopedNodes,
                allNodes,
                normalizedName,
                locationTypeId,
                normalizedParentCode
        );

        return LocationNodeResponse.buildTree(nodes, normalizedParentCode);
    }

    private List<LocationNode> applySearchFilters(
            List<LocationNode> scopedNodes,
            List<LocationNode> allNodes,
            String name,
            Integer locationTypeId,
            String parentCode
    ) {
        if (name == null && locationTypeId == null) {
            return scopedNodes;
        }

        Map<String, String> parentByCode = buildParentByCode(allNodes);
        Map<String, List<LocationNode>> childrenByParentCode = buildChildrenByParentCode(allNodes);

        Set<String> scopedCodes = new HashSet<>();
        for (LocationNode node : scopedNodes) {
            scopedCodes.add(node.getCode());
        }

        Set<String> keepCodes = new HashSet<>();
        for (LocationNode node : scopedNodes) {
            if (!matchesName(node, name) || !matchesLocationType(node, locationTypeId)) {
                continue;
            }
            keepCodes.add(node.getCode());
            addAncestors(node.getCode(), parentByCode, keepCodes, scopedCodes);
            addDescendants(node.getCode(), childrenByParentCode, keepCodes, scopedCodes);
        }

        if (parentCode != null && !keepCodes.isEmpty()) {
            keepCodes.add(parentCode);
        }

        if (keepCodes.isEmpty()) {
            return List.of();
        }

        List<LocationNode> filtered = new ArrayList<>();
        for (LocationNode node : scopedNodes) {
            if (keepCodes.contains(node.getCode())) {
                filtered.add(node);
            }
        }
        return filtered;
    }

    private List<LocationNode> filterSubtree(String rootCode, List<LocationNode> allNodes) {
        Map<String, List<LocationNode>> childrenByParentCode = buildChildrenByParentCode(allNodes);

        Set<String> subtreeCodes = new HashSet<>();
        subtreeCodes.add(rootCode);
        collectDescendants(rootCode, childrenByParentCode, subtreeCodes);

        List<LocationNode> subtree = new ArrayList<>();
        for (LocationNode node : allNodes) {
            if (subtreeCodes.contains(node.getCode())) {
                subtree.add(node);
            }
        }
        return subtree;
    }

    private Map<String, String> buildParentByCode(List<LocationNode> nodes) {
        Map<String, String> parentByCode = new HashMap<>();
        for (LocationNode node : nodes) {
            if (node.getParent() == null) {
                parentByCode.put(node.getCode(), null);
                continue;
            }
            parentByCode.put(node.getCode(), node.getParent().getCode());
        }
        return parentByCode;
    }

    private Map<String, List<LocationNode>> buildChildrenByParentCode(List<LocationNode> nodes) {
        Map<String, List<LocationNode>> childrenByParentCode = new HashMap<>();
        for (LocationNode node : nodes) {
            if (node.getParent() == null) {
                continue;
            }
            String parentCode = node.getParent().getCode();
            List<LocationNode> children = childrenByParentCode.get(parentCode);
            if (children == null) {
                children = new ArrayList<>();
                childrenByParentCode.put(parentCode, children);
            }
            children.add(node);
        }
        return childrenByParentCode;
    }

    private void addAncestors(
            String code,
            Map<String, String> parentByCode,
            Set<String> keepCodes,
            Set<String> scopedCodes
    ) {
        String parentCode = parentByCode.get(code);
        while (parentCode != null) {
            if (!scopedCodes.contains(parentCode)) {
                return;
            }
            keepCodes.add(parentCode);
            parentCode = parentByCode.get(parentCode);
        }
    }

    private void addDescendants(
            String code,
            Map<String, List<LocationNode>> childrenByParentCode,
            Set<String> keepCodes,
            Set<String> scopedCodes
    ) {
        List<LocationNode> children = childrenByParentCode.get(code);
        if (children == null) {
            return;
        }
        for (LocationNode child : children) {
            if (!scopedCodes.contains(child.getCode())) {
                continue;
            }
            keepCodes.add(child.getCode());
            addDescendants(child.getCode(), childrenByParentCode, keepCodes, scopedCodes);
        }
    }

    private void collectDescendants(
            String code,
            Map<String, List<LocationNode>> childrenByParentCode,
            Set<String> subtreeCodes
    ) {
        List<LocationNode> children = childrenByParentCode.get(code);
        if (children == null) {
            return;
        }
        for (LocationNode child : children) {
            subtreeCodes.add(child.getCode());
            collectDescendants(child.getCode(), childrenByParentCode, subtreeCodes);
        }
    }

    private List<LocationNode> sortByDepthDescending(List<LocationNode> nodes) {
        Map<String, String> parentByCode = buildParentByCode(nodes);
        List<LocationNode> sorted = new ArrayList<>(nodes);
        sorted.sort((a, b) -> Integer.compare(
                computeDepth(b, parentByCode),
                computeDepth(a, parentByCode)
        ));
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
        return locationNodeRepository.findByCode(parentCode)
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + parentCode));
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
            if (!locationNodeRepository.existsByCode(code) && !batchCodes.contains(code)) {
                batchCodes.add(code);
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

        LocationNode node;
        if (parent == null) {
            node = LocationNode.createRoot(
                    generateUniqueCode(batchCodes), locationType, request.name());
        } else {
            node = LocationNode.createChild(
                    generateUniqueCode(batchCodes), parent, locationType, request.name());
        }
        node = locationNodeRepository.save(node);
        if (parent != null) {
            reconstructTreeAfterInsert(parent, node);
        }

        List<LocationNodeTreeCreateRequest> children = request.children();
        if (children == null || children.isEmpty()) {
            return LocationNodeResponse.from(node);
        }

        validateBatchSiblingNames(children);
        List<LocationNodeResponse> childResponses = new ArrayList<>();
        for (LocationNodeTreeCreateRequest childRequest : children) {
            childResponses.add(createTreeNode(node, childRequest, batchCodes));
        }
        return LocationNodeResponse.of(node, childResponses);
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
        if (parent == null) {
            return;
        }
        int parentOrder = requireSortOrder(parent.getLocationType());
        int childOrder = requireSortOrder(childType);
        if (childOrder <= parentOrder) {
            throw new IllegalArgumentException(CHILD_DEEPER_THAN_PARENT_MESSAGE);
        }
    }

    private void validateLocationTypeAgainstChildren(LocationNode node, CommonCode newType) {
        int nodeOrder = requireSortOrder(newType);
        for (LocationNode child : locationNodeRepository.findByParent_Code(node.getCode())) {
            int childOrder = requireSortOrder(child.getLocationType());
            if (childOrder <= nodeOrder) {
                throw new IllegalArgumentException(CHILD_DEEPER_THAN_PARENT_MESSAGE);
            }
        }
    }

    private void reconstructTreeAfterInsert(LocationNode parent, LocationNode newNode) {
        int newNodeOrder = requireSortOrder(newNode.getLocationType());
        for (LocationNode sibling : locationNodeRepository.findByParent_Code(parent.getCode())) {
            if (sibling.getCode().equals(newNode.getCode())) {
                continue;
            }
            int siblingOrder = requireSortOrder(sibling.getLocationType());
            if (siblingOrder > newNodeOrder) {
                sibling.updateParent(newNode);
                locationNodeRepository.save(sibling);
            }
        }
    }

    private int requireSortOrder(CommonCode locationType) {
        Integer sortOrder = locationType.getSortOrder();
        if (sortOrder == null) {
            throw new IllegalArgumentException("location type sort order is required");
        }
        return sortOrder;
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private boolean matchesName(LocationNode node, String name) {
        if (name == null) {
            return true;
        }
        return node.getName().toLowerCase().contains(name.toLowerCase());
    }

    private boolean matchesLocationType(LocationNode node, Integer locationTypeId) {
        if (locationTypeId == null) {
            return true;
        }
        return node.getLocationType().getId().equals(locationTypeId);
    }
}

package net.vivans.dcim.module.location.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.location.api.dto.LocationNodeResponse;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
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

    private final LocationNodeRepository locationNodeRepository;
    private final CommonCodeRepository commonCodeRepository;

    public List<LocationNodeResponse> getLocationNodes(String name, String parentCode, Integer locationTypeId) {
        String normalizedName = blankToNull(name);
        String normalizedParentCode = blankToNull(parentCode);
        if (normalizedParentCode != null && !locationNodeRepository.existsByCode(normalizedParentCode)) {
            throw new EntityNotFoundException("LocationNode not found: " + normalizedParentCode);
        }
        if (locationTypeId != null && commonCodeRepository.findById(locationTypeId).isEmpty()) {
            throw new EntityNotFoundException("CommonCode not found: " + locationTypeId);
        }

        List<LocationNode> allNodes = locationNodeRepository.findAll();
        List<LocationNode> scopedNodes = normalizedParentCode == null
                ? allNodes
                : filterSubtree(normalizedParentCode, allNodes);
        List<LocationNode> nodes = applySearchFilters(
                scopedNodes, allNodes, normalizedName, locationTypeId, normalizedParentCode);
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
        scopedNodes.forEach(node -> scopedCodes.add(node.getCode()));

        Set<String> keepCodes = new HashSet<>();
        for (LocationNode node : scopedNodes) {
            if (matchesName(node, name) && matchesLocationType(node, locationTypeId)) {
                keepCodes.add(node.getCode());
                addAncestors(node.getCode(), parentByCode, keepCodes, scopedCodes);
                addDescendants(node.getCode(), childrenByParentCode, keepCodes, scopedCodes);
            }
        }
        if (parentCode != null && !keepCodes.isEmpty()) {
            keepCodes.add(parentCode);
        }
        if (keepCodes.isEmpty()) {
            return List.of();
        }
        return scopedNodes.stream().filter(node -> keepCodes.contains(node.getCode())).toList();
    }

    private List<LocationNode> filterSubtree(String rootCode, List<LocationNode> allNodes) {
        Map<String, List<LocationNode>> childrenByParentCode = buildChildrenByParentCode(allNodes);
        Set<String> subtreeCodes = new HashSet<>();
        subtreeCodes.add(rootCode);
        collectDescendants(rootCode, childrenByParentCode, subtreeCodes);
        return allNodes.stream().filter(node -> subtreeCodes.contains(node.getCode())).toList();
    }

    private Map<String, String> buildParentByCode(List<LocationNode> nodes) {
        Map<String, String> parentByCode = new HashMap<>();
        for (LocationNode node : nodes) {
            parentByCode.put(node.getCode(), node.getParent() == null ? null : node.getParent().getCode());
        }
        return parentByCode;
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

    private void addAncestors(
            String code,
            Map<String, String> parentByCode,
            Set<String> keepCodes,
            Set<String> scopedCodes
    ) {
        String parentCode = parentByCode.get(code);
        while (parentCode != null && scopedCodes.contains(parentCode)) {
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
        for (LocationNode child : childrenByParentCode.getOrDefault(code, List.of())) {
            if (scopedCodes.contains(child.getCode())) {
                keepCodes.add(child.getCode());
                addDescendants(child.getCode(), childrenByParentCode, keepCodes, scopedCodes);
            }
        }
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static boolean matchesName(LocationNode node, String name) {
        return name == null || node.getName().toLowerCase().contains(name.toLowerCase());
    }

    private static boolean matchesLocationType(LocationNode node, Integer locationTypeId) {
        return locationTypeId == null || node.getLocationType().getId().equals(locationTypeId);
    }

}

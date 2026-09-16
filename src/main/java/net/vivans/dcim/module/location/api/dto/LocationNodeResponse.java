package net.vivans.dcim.module.location.api.dto;

import net.vivans.dcim.module.location.domain.model.LocationNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record LocationNodeResponse(
        String code,
        String parentCode,
        Integer locationTypeId,
        String name,
        Integer rackUCapacity,
        List<LocationNodeResponse> children
) {

    public static LocationNodeResponse from(LocationNode node) {
        return of(node, List.of());
    }

    public static LocationNodeResponse of(LocationNode node, List<LocationNodeResponse> children) {
        return new LocationNodeResponse(
                node.getCode(),
                node.getParent() != null ? node.getParent().getCode() : null,
                node.getLocationType().getId(),
                node.getName(),
                node.getRackUCapacity(),
                children
        );
    }

    public static List<LocationNodeResponse> buildTree(List<LocationNode> nodes, String rootCode) {
        if (nodes.isEmpty()) {
            return List.of();
        }

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

        for (List<LocationNode> children : childrenByParentCode.values()) {
            children.sort(Comparator.comparing(LocationNode::getName));
        }

        if (rootCode != null) {
            LocationNode root = findNodeByCode(nodes, rootCode);
            if (root == null) {
                return List.of();
            }
            return List.of(toTree(root, childrenByParentCode));
        }

        Set<String> nodeCodes = new HashSet<>();
        for (LocationNode node : nodes) {
            nodeCodes.add(node.getCode());
        }

        List<LocationNode> roots = new ArrayList<>();
        for (LocationNode node : nodes) {
            if (node.getParent() == null || !nodeCodes.contains(node.getParent().getCode())) {
                roots.add(node);
            }
        }
        roots.sort(Comparator.comparing(LocationNode::getName));

        List<LocationNodeResponse> result = new ArrayList<>();
        for (LocationNode root : roots) {
            result.add(toTree(root, childrenByParentCode));
        }
        return result;
    }

    private static LocationNode findNodeByCode(List<LocationNode> nodes, String code) {
        for (LocationNode node : nodes) {
            if (node.getCode().equals(code)) {
                return node;
            }
        }
        return null;
    }

    private static LocationNodeResponse toTree(
            LocationNode node,
            Map<String, List<LocationNode>> childrenByParentCode
    ) {
        List<LocationNode> childNodes = childrenByParentCode.get(node.getCode());
        if (childNodes == null) {
            childNodes = List.of();
        }

        List<LocationNodeResponse> children = new ArrayList<>();
        for (LocationNode child : childNodes) {
            children.add(toTree(child, childrenByParentCode));
        }
        return of(node, children);
    }
}

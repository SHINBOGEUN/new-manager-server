package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.DevicePageDisplayResponse;
import net.vivans.dcim.module.device.api.dto.DevicePageDisplayUpdateRequest;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DevicePageDevice;
import net.vivans.dcim.module.device.domain.model.DevicePageDisplayConfig;
import net.vivans.dcim.module.device.domain.model.DevicePageSceneItem;
import net.vivans.dcim.module.device.domain.model.DevicePageViewMode;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.infrastructure.persistence.DevicePageDeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DevicePageDisplayConfigRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DevicePageSceneItemRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevicePageDisplayService {
    private final CommonCodeRepository commonCodeRepository;
    private final DevicePageDeviceRepository pageDeviceRepository;
    private final DevicePageDisplayConfigRepository configRepository;
    private final DevicePageSceneItemRepository sceneItemRepository;
    private final LocationNodeRepository locationNodeRepository;
    private final SceneModelService sceneModelService;

    public DevicePageDisplayResponse get(String pageCode) {
        CommonCode page = findPage(pageCode);
        DevicePageDisplayConfig config = configRepository.findById(page.getId()).orElse(null);
        List<DevicePageSceneItem> savedItems = sceneItemRepository.findAllForPage(page.getId());
        List<DevicePageDisplayResponse.SceneItem> items = new java.util.ArrayList<>(savedItems.stream()
                .map(item -> new DevicePageDisplayResponse.SceneItem(
                        item.getId(), item.getParent() == null ? null : item.getParent().getId(),
                        item.getItemType(), item.getDevice() == null ? null : item.getDevice().getId(),
                        item.getLocationNode() == null ? null : item.getLocationNode().getCode(),
                        item.getPointName(), item.getLocationNode() == null ? item.getLabel() : item.getLocationNode().getName(),
                        item.getModelKey(), item.getX(), item.getY(),
                        item.getZ(), item.getRotationY(), item.getSizeX(), item.getSizeY(), item.getSizeZ(), item.getGroupKey(), item.getGroupName(), item.isLocked()))
                .toList());

        // Import descendant location racks until the page's scene is next saved.
        // The flag also lets existing configured pages receive this one-time import,
        // while later deletions remain authoritative after the user saves.
        if (config == null || !config.isLocationHierarchyInitialized()) {
            Set<String> mappedRackCodes = new HashSet<>();
            savedItems.stream()
                    .filter(item -> "RACK".equals(item.getItemType()) && item.getLocationNode() != null)
                    .forEach(item -> mappedRackCodes.add(item.getLocationNode().getCode()));
            Map<String, List<LocationNode>> childrenByParent = new HashMap<>();
            for (LocationNode node : locationNodeRepository.findAll()) {
                if (node.getParent() != null) {
                    childrenByParent.computeIfAbsent(node.getParent().getCode(), ignored -> new ArrayList<>()).add(node);
                }
            }
            Map<String, LocationNode> pageRacks = new LinkedHashMap<>();
            Set<String> visitedLocationCodes = new HashSet<>();
            for (DevicePageDevice mapping : pageDeviceRepository.findAllByPageCode_IdOrderByIdAsc(page.getId())) {
                LocationNode assignedLocation = mapping.getDevice().getLocationNode();
                if (assignedLocation == null) continue;
                Deque<LocationNode> pending = new ArrayDeque<>();
                pending.add(assignedLocation);
                while (!pending.isEmpty()) {
                    LocationNode node = pending.removeFirst();
                    if (!visitedLocationCodes.add(node.getCode())) continue;
                    if ("RACK".equals(node.getLocationType().getCode())) pageRacks.putIfAbsent(node.getCode(), node);
                    pending.addAll(childrenByParent.getOrDefault(node.getCode(), List.of()));
                }
            }
            List<LocationNode> orderedPageRacks = pageRacks.values().stream()
                    .sorted(java.util.Comparator.comparing(LocationNode::getName).thenComparing(LocationNode::getCode))
                    .toList();
            int rackIndex = 0;
            for (LocationNode rack : orderedPageRacks) {
                if (!mappedRackCodes.contains(rack.getCode())) {
                    int column = rackIndex % 4;
                    int row = rackIndex / 4;
                    items.add(new DevicePageDisplayResponse.SceneItem(
                            null, null, "RACK", null, rack.getCode(), null, rack.getName(), "RACK",
                            -3.6 + column * 2.4, 0, row % 2 == 0 ? -1.7 : 1.7, 0, 4, 4, 4, null, null, false));
                }
                rackIndex++;
            }
        }
        return new DevicePageDisplayResponse(page.getCode(), page.getName(),
                config == null ? DevicePageViewMode.DATA.name() : config.getViewMode(),
                config == null ? null : config.getLayoutAssetKey(), List.copyOf(items));
    }

    @Transactional
    public DevicePageDisplayResponse update(String pageCode, DevicePageDisplayUpdateRequest request) {
        CommonCode page = findPage(pageCode);
        DevicePageViewMode mode;
        try {
            mode = DevicePageViewMode.valueOf(request.viewMode().trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("viewMode must be DATA, PLAN_2D, or SCENE_3D");
        }
        String assetKey = request.layoutAssetKey() == null || request.layoutAssetKey().isBlank()
                ? null : request.layoutAssetKey().trim();
        if (mode == DevicePageViewMode.DATA) assetKey = null;
        final String selectedAssetKey = assetKey;

        List<DevicePageDevice> pageDevices = pageDeviceRepository.findAllByPageCode_IdOrderByIdAsc(page.getId());
        Map<Integer, Device> devicesById = new HashMap<>();
        pageDevices.forEach(mapping -> devicesById.put(mapping.getDevice().getId(), mapping.getDevice()));
        Set<String> clientKeys = new HashSet<>();
        Map<String, DevicePageDisplayUpdateRequest.SceneItem> sceneItemsByKey = new HashMap<>();
        Map<String, Integer> itemOrder = new HashMap<>();
        Set<String> locationNodeCodes = new HashSet<>();
        Set<Integer> attachedDeviceIds = new HashSet<>();
        for (int index = 0; index < request.items().size(); index++) {
            DevicePageDisplayUpdateRequest.SceneItem item = request.items().get(index);
            if (!clientKeys.add(item.clientKey())) throw new IllegalArgumentException("Duplicate scene item clientKey");
            sceneItemsByKey.put(item.clientKey(), item);
            itemOrder.put(item.clientKey(), index);
            validateItem(item, devicesById);
            if (("DEVICE".equals(item.itemType()) || "SENSOR".equals(item.itemType()))
                    && item.deviceId() != null && !attachedDeviceIds.add(item.deviceId())) {
                throw new IllegalArgumentException("A device can only be attached once on a page");
            }
            if (item.locationNodeCode() != null) {
                if (!"RACK".equals(item.itemType()) && !("EQUIPMENT".equals(item.itemType())
                        && item.modelKey() != null && item.modelKey().matches("SR_.*RACK") && !"SR_PDU_RACK".equals(item.modelKey()))) {
                    throw new IllegalArgumentException("Only a rack scene item can reference a location node");
                }
                if (!locationNodeCodes.add(item.locationNodeCode())) {
                    throw new IllegalArgumentException("A location rack can only be placed once on a page");
                }
            }
        }
        for (DevicePageDisplayUpdateRequest.SceneItem item : request.items()) {
            if (item.parentClientKey() == null || item.parentClientKey().isBlank()) continue;
            DevicePageDisplayUpdateRequest.SceneItem host = sceneItemsByKey.get(item.parentClientKey());
            if (host == null || itemOrder.get(item.parentClientKey()) >= itemOrder.get(item.clientKey())) {
                throw new IllegalArgumentException("A child scene item must reference an earlier host in the submitted scene");
            }
            boolean opening = "EQUIPMENT".equals(item.itemType()) && isWallOpening(item.modelKey());
            if (opening) {
                if (!"EQUIPMENT".equals(host.itemType()) || !isWallModel(host.modelKey())) {
                    throw new IllegalArgumentException("A door or window can only be attached to a wall");
                }
            } else if (("SENSOR".equals(item.itemType()) || "DEVICE".equals(item.itemType()))
                    && !Set.of("RACK", "EQUIPMENT").contains(host.itemType())) {
                throw new IllegalArgumentException("An attached device must reference a rack or equipment host");
            } else if (!Set.of("SENSOR", "DEVICE").contains(item.itemType())) {
                throw new IllegalArgumentException("Only devices or wall openings can be attached to a host");
            }
        }

        DevicePageDisplayConfig config = configRepository.findById(page.getId())
                .orElseGet(() -> DevicePageDisplayConfig.create(page, mode.name(), selectedAssetKey));
        config.update(mode.name(), selectedAssetKey);
        config.markLocationHierarchyInitialized();
        configRepository.save(config);

        sceneItemRepository.deleteAllForPage(page.getId());
        sceneItemRepository.flush();
        Map<String, DevicePageSceneItem> savedByClientKey = new HashMap<>();
        for (DevicePageDisplayUpdateRequest.SceneItem item : request.items()) {
            DevicePageSceneItem parent = item.parentClientKey() == null || item.parentClientKey().isBlank()
                    ? null : savedByClientKey.get(item.parentClientKey());
            boolean opening = "EQUIPMENT".equals(item.itemType()) && isWallOpening(item.modelKey());
            if (("SENSOR".equals(item.itemType()) || "DEVICE".equals(item.itemType()))
                    && (parent == null || !Set.of("RACK", "EQUIPMENT").contains(parent.getItemType()))) {
                throw new IllegalArgumentException("A device can only be attached to a rack or equipment");
            }
            if (opening && (parent == null || !"EQUIPMENT".equals(parent.getItemType()) || !isWallModel(parent.getModelKey()))) {
                throw new IllegalArgumentException("A door or window can only be attached to a wall");
            }
            DevicePageSceneItem saved = sceneItemRepository.saveAndFlush(DevicePageSceneItem.create(
                    page, parent, item.deviceId() == null ? null : devicesById.get(item.deviceId()),
                    resolveRackLocation(item),
                    item.itemType(), item.pointName(), item.label(), item.modelKey(),
                    item.x(), item.y(), item.z(), item.rotationY() == null ? 0 : item.rotationY(),
                    item.sizeX() == null ? 4 : item.sizeX(),
                    item.sizeY() == null ? (isScalableEquipment(item) ? (item.sizeX() == null ? 4 : item.sizeX()) : 4) : item.sizeY(),
                    item.sizeZ() == null ? (isScalableEquipment(item) ? (item.sizeX() == null ? 4 : item.sizeX()) : 4) : item.sizeZ()));
            if (item.groupKey() != null && !isScalableEquipment(item)) throw new IllegalArgumentException("Only equipment can be grouped");
            saved.setGroupKey(item.groupKey());
            saved.setGroupName(item.groupName());
            if (Boolean.TRUE.equals(item.locked()) && !isScalableEquipment(item)) throw new IllegalArgumentException("Only equipment can be locked");
            saved.setLocked(Boolean.TRUE.equals(item.locked()));
            savedByClientKey.put(item.clientKey(), saved);
        }
        return get(pageCode);
    }

    private LocationNode resolveRackLocation(DevicePageDisplayUpdateRequest.SceneItem item) {
        if (item.locationNodeCode() == null || item.locationNodeCode().isBlank()) return null;
        LocationNode node = locationNodeRepository.findByCode(item.locationNodeCode().trim())
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + item.locationNodeCode()));
        if (!"RACK".equals(node.getLocationType().getCode())) {
            throw new IllegalArgumentException("Only LOCATION_TYPE=RACK can be mapped to a scene rack");
        }
        return node;
    }

    private void validateItem(DevicePageDisplayUpdateRequest.SceneItem item, Map<Integer, Device> devicesById) {
        if (!Set.of("RACK", "EQUIPMENT", "SENSOR", "DEVICE").contains(item.itemType())) {
            throw new IllegalArgumentException("Unsupported scene item type: " + item.itemType());
        }
        if (!Set.of("RACK", "PDU", "CRAC", "UPS", "SENSOR", "GENERIC", "CHILLER", "SWITCH", "SERVER",
                "MACHINE", "CONVEYOR", "TANK", "TABLE", "WALL", "WALL_CONCRETE", "WALL_GLASS", "WALL_GLASS_CLEAR", "WALL_PANEL",
                "DOOR", "DOOR_DOUBLE", "DOOR_SLIDING", "WINDOW", "WINDOW_DOUBLE", "CEILING", "CEILING_CLEAR", "FLOOR", "LIGHT", "PIPE", "STAIR",
                "SR_RACK", "SR_RDC", "SR_INROW", "SR_INROW_MOLD", "SR_WALL_COIL", "SR_DLC_RACK", "SR_INROW_RACK",
                "SR_OCP_RACK", "SR_RDC_RACK", "SR_ORV3_RACK", "SR_IMMERSION", "SR_HAC_RACK", "SR_DLC_CDU", "SR_DLC_SERVER", "SR_INROW_PANEL", "SR_PDU_RACK", "SR_PDU_VERTICAL", "SR_GPU_SERVER", "SR_DRAGINO_TH", "SR_DRAGINO_DOOR").contains(item.modelKey())
                && !("EQUIPMENT".equals(item.itemType()) && item.modelKey() != null
                && item.modelKey().matches("USR_[A-Z0-9]{24}") && sceneModelService.usable(item.modelKey()))) {
            throw new IllegalArgumentException("Unsupported scene model: " + item.modelKey());
        }
        if (item.deviceId() != null && !devicesById.containsKey(item.deviceId())) {
            throw new IllegalArgumentException("Scene device is not selected for this page: " + item.deviceId());
        }
        if ("SENSOR".equals(item.itemType())) {
            if (item.deviceId() == null || item.pointName() == null || item.pointName().isBlank()) {
                throw new IllegalArgumentException("A sensor marker requires a page device and measurement point");
            }
            if (item.parentClientKey() == null || item.parentClientKey().isBlank()) {
                throw new IllegalArgumentException("A sensor marker must be placed on a rack");
            }
        } else if (item.parentClientKey() != null && !item.parentClientKey().isBlank()) {
            if (!("DEVICE".equals(item.itemType()) || ("EQUIPMENT".equals(item.itemType()) && isWallOpening(item.modelKey())))) {
                throw new IllegalArgumentException("Only devices or wall openings can be attached to a parent item");
            }
        }
        if ("DEVICE".equals(item.itemType()) && (item.deviceId() == null
                || (item.pointName() != null && !item.pointName().isBlank())
                || item.parentClientKey() == null || item.parentClientKey().isBlank())) {
            throw new IllegalArgumentException("A device attachment requires a page device and a rack/equipment host");
        }
        if (item.label().isBlank() || item.label().length() > 120) throw new IllegalArgumentException("Invalid scene item label");
        if (item.x() == null || item.y() == null || item.z() == null
                || !Double.isFinite(item.x()) || !Double.isFinite(item.y()) || !Double.isFinite(item.z())
                || Math.abs(item.x()) > 100 || Math.abs(item.y()) > 100 || Math.abs(item.z()) > 20) {
            throw new IllegalArgumentException("Scene position is outside the supported room bounds");
        }
        if (item.rotationY() != null && !Double.isFinite(item.rotationY())) {
            throw new IllegalArgumentException("rotationY must be finite");
        }
    }

    private static boolean isWallOpening(String modelKey) {
        return modelKey != null && (modelKey.startsWith("DOOR") || modelKey.startsWith("WINDOW"));
    }

    private static boolean isWallModel(String modelKey) {
        return modelKey != null && modelKey.startsWith("WALL");
    }

    private static boolean isEquipmentFrame(String modelKey) {
        return "SR_INROW_MOLD".equals(modelKey) || "SR_WALL_COIL".equals(modelKey);
    }

    private static boolean isScalableEquipment(DevicePageDisplayUpdateRequest.SceneItem item) {
        String key = item.modelKey();
        return Set.of("RACK", "EQUIPMENT").contains(item.itemType()) && key != null
                && !key.startsWith("WALL") && !key.startsWith("DOOR") && !key.startsWith("WINDOW")
                && !Set.of("FLOOR", "CEILING", "CEILING_CLEAR", "LIGHT", "PIPE", "STAIR").contains(key);
    }

    private CommonCode findPage(String pageCode) {
        if (pageCode == null || pageCode.isBlank()) throw new IllegalArgumentException("pageCode is required");
        return commonCodeRepository.findByCodeGroupGroupKeyAndCode(DevicePageCodes.DEVICE_PAGE_GROUP_KEY,
                        pageCode.trim())
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_PAGE code not found: " + pageCode.trim()));
    }
}

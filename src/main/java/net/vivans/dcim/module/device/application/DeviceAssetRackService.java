package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceImageResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementHistoryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementRequest;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementResponse;
import net.vivans.dcim.module.device.api.dto.RackLayoutResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceAsset;
import net.vivans.dcim.module.device.domain.model.DeviceMountType;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacement;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistory;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistoryAction;
import net.vivans.dcim.module.device.domain.repository.DeviceImageRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementHistoryRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceAssetSpringDataRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceAssetRackService {

    private static final String RACK_LOCATION_TYPE = "RACK";

    private final DeviceRepository deviceRepository;
    private final DeviceAssetSpringDataRepository deviceAssetRepository;
    private final DeviceRackPlacementRepository placementRepository;
    private final DeviceRackPlacementHistoryRepository placementHistoryRepository;
    private final DeviceImageRepository imageRepository;
    private final LocationNodeRepository locationNodeRepository;
    private final net.vivans.dcim.module.device.infrastructure.persistence.SceneModelRepository sceneModelRepository;
    private final net.vivans.dcim.module.device.infrastructure.persistence.RackModelPlacementRepository modelPlacements;
    private final net.vivans.dcim.module.location.infrastructure.persistence.LocationNodeSpringDataRepository locationLocks;

    @Transactional
    public DeviceRackPlacementResponse saveRackPlacement(Integer deviceId, DeviceRackPlacementRequest request) {
        Device device = findDevice(deviceId);
        DeviceMountType mountType = request.mountType() == null ? DeviceMountType.RACK_U : request.mountType();
        LocationNode rack = mountType.requiresRack() ? lockRack(request.rackLocationCode()) : null;
        if (mountType.usesRackU()) {
            if (request.uPosition() == null || request.uHeight() == null) {
                throw new IllegalArgumentException("RACK_U requires uPosition and uHeight");
            }
            int lastU = request.uPosition() + request.uHeight() - 1;
            if (rack.getRackUCapacity() != null && lastU > rack.getRackUCapacity()) {
                throw new IllegalArgumentException("rack placement exceeds rack U capacity: " + rack.getRackUCapacity());
            }
            if (placementRepository.existsOverlapping(rack.getCode(), request.uPosition(), lastU, deviceId)) {
                throw new IllegalArgumentException("rack U position overlaps an existing device");
            }
            if (modelPlacements.overlaps(rack.getCode(), request.uPosition(), lastU))
                throw new IllegalArgumentException("rack U position overlaps an unlinked model");
        }
        DeviceRackPlacement currentPlacement = placementRepository.findByDeviceId(deviceId).orElse(null);
        String selectedKey = Boolean.TRUE.equals(request.updateSceneModel()) ? request.sceneModelKey() : currentPlacement == null ? null : currentPlacement.getSceneModelKey();
        boolean unchanged = currentPlacement != null && currentPlacement.getMountType() == mountType && java.util.Objects.equals(currentPlacement.getSceneModelKey(), selectedKey);
        if (mountType.requiresRack() && selectedKey != null && !selectedKey.isBlank() && !unchanged) validateModelMount(selectedKey, mountType);
        if (rack != null && (device.getLocationNode() == null || !rack.getCode().equals(device.getLocationNode().getCode()))) {
            device.reassignLocation(rack);
            deviceRepository.save(device);
        }
        DeviceRackPlacement placement = currentPlacement;
        boolean sameMount = placement != null && placement.getMountType() == mountType
                && java.util.Objects.equals(placement.getRackLocation() == null ? null : placement.getRackLocation().getCode(), rack == null ? null : rack.getCode())
                && java.util.Objects.equals(placement.getRackSide(), request.rackSide());
        DeviceRackPlacementHistory.PlacementSnapshot previous = DeviceRackPlacementHistory.PlacementSnapshot.from(placement);
        if (placement == null) {
            placement = DeviceRackPlacement.create(device, rack, mountType, request.rackSide(), request.uPosition(),
                    request.uHeight(), request.formFactor());
        }
        placement.update(device, rack, mountType, request.rackSide(), request.uPosition(), request.uHeight(), request.formFactor());
        if (Boolean.TRUE.equals(request.clearPosition())) {
            placement.setVisualizationPosition(null, null, null);
        } else if (request.positionX() != null || request.positionY() != null || request.positionZ() != null) {
            if (mountType != DeviceMountType.RACK_SIDE && mountType != DeviceMountType.RACK_REAR)
                throw new IllegalArgumentException("visual position is only supported for side or rear mounts");
            placement.setVisualizationPosition(request.positionX(), request.positionY(), request.positionZ());
        } else if (!sameMount) placement.setVisualizationPosition(null, null, null);
        if (request.visualLengthRatio() != null) {
            if (mountType != DeviceMountType.RACK_SIDE) throw new IllegalArgumentException("길이 조절은 랙 측면 장착에만 적용됩니다");
            placement.setVisualLengthRatio(request.visualLengthRatio());
        } else if (!sameMount) placement.setVisualLengthRatio(null);
        if (Boolean.TRUE.equals(request.updateSceneModel())) {
            String modelKey = request.sceneModelKey();
            if (modelKey != null && !modelKey.isBlank() && !sceneModelRepository.existsById(modelKey.trim()))
                throw new IllegalArgumentException("3D 모델을 찾을 수 없습니다");
            placement.setSceneModelKey(modelKey);
        }
        DeviceRackPlacement saved = placementRepository.save(placement);
        DeviceRackPlacementHistory.PlacementSnapshot current = DeviceRackPlacementHistory.PlacementSnapshot.from(saved);
        if (!java.util.Objects.equals(previous, current)) {
            DeviceRackPlacementHistoryAction action = previous == null
                    ? DeviceRackPlacementHistoryAction.PLACED
                    : DeviceRackPlacementHistoryAction.MOVED;
            placementHistoryRepository.save(DeviceRackPlacementHistory.create(device, action, previous, current));
        }
        return DeviceRackPlacementResponse.from(saved);
    }

    @Transactional
    public void deleteRackPlacement(Integer deviceId) {
        placementRepository.findByDeviceId(deviceId).ifPresent(this::removePlacementWithHistory);
    }

    @Transactional
    public void removeRackPlacementIfLocationChanged(Integer deviceId, String locationNodeCode) {
        placementRepository.findByDeviceId(deviceId)
                .filter(placement -> placement.getRackLocation() != null
                        && !placement.getRackLocation().getCode().equals(locationNodeCode))
                .ifPresent(this::removePlacementWithHistory);
    }

    public List<DeviceRackPlacementHistoryResponse> getRackPlacementHistory(Integer deviceId) {
        findDevice(deviceId);
        return placementHistoryRepository.findAllByDeviceId(deviceId).stream()
                .map(DeviceRackPlacementHistoryResponse::from)
                .toList();
    }

    public RackLayoutResponse getRackLayout(String rackLocationCode) {
        LocationNode rack = locationNodeRepository.findByCode(rackLocationCode)
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + rackLocationCode));
        if (!RACK_LOCATION_TYPE.equalsIgnoreCase(rack.getLocationType().getCode())) {
            throw new IllegalArgumentException("location must be a RACK");
        }
        List<RackLayoutResponse.RackLayoutItem> items = new java.util.ArrayList<>(placementRepository.findAllByRackLocationCode(rackLocationCode).stream()
                .map(placement -> new RackLayoutResponse.RackLayoutItem(
                        placement.getDevice().getId(), placement.getDevice().getName(), assetCodeOf(placement.getDevice()),
                        placement.getDevice().getDeviceModel().getName(), placement.getMountType(), placement.getRackSide(),
                        placement.getUPosition(), placement.getUHeight(), placement.lastU(), placement.getFormFactor(),
                        assetColorOf(placement.getDevice()), primaryImage(placement.getDevice().getId()),
                        placement.getPositionX(), placement.getPositionY(), placement.getPositionZ(), placement.getSceneModelKey(), null, placement.getVisualLengthRatio()))
                .toList());
        for (var placement : modelPlacements.findAllByRackLocationCode(rackLocationCode)) {
            var model = sceneModelRepository.findById(placement.getSceneModelKey()).orElseThrow();
            items.add(new RackLayoutResponse.RackLayoutItem(null, model.getName(), null, model.getName(),
                    placement.getMountType(), placement.getRackSide(), placement.getUPosition(), placement.getUHeight(), placement.lastU(),
                    placement.getUHeight() == null ? null : placement.getUHeight() + "U", null, null,
                    placement.getPositionX(), placement.getPositionY(), placement.getPositionZ(), placement.getSceneModelKey(), placement.getId(), placement.getVisualLengthRatio()));
        }
        return new RackLayoutResponse(rack.getCode(), rack.getName(), rack.getRackUCapacity(), items);
    }

    @Transactional
    public RackLayoutResponse createModelPlacement(String code, DeviceRackPlacementRequest request) {
        LocationNode rack = lockRack(code);
        String key = request.sceneModelKey();
        if (key == null || key.isBlank()) throw new IllegalArgumentException("3D 모델을 선택하세요");
        var model = sceneModelRepository.findById(key.trim()).orElseThrow(() -> new EntityNotFoundException("3D 모델을 찾을 수 없습니다"));
        if (!model.isEnabled()) throw new IllegalArgumentException("사용 중인 3D 모델만 설치할 수 있습니다");
        if (!model.isRackInstallable()) throw new IllegalArgumentException("랙 설치가 허용된 모델을 선택하세요");
        DeviceMountType type = request.mountType();
        if (type == null || !java.util.Set.of(DeviceMountType.RACK_U, DeviceMountType.RACK_SIDE, DeviceMountType.RACK_REAR).contains(type))
            throw new IllegalArgumentException("랙 설치 방식을 선택하세요");
        if (!model.permitsRackMount(type)) throw new IllegalArgumentException("이 3D 모델은 선택한 랙 설치 방식에 사용할 수 없습니다");
        if (type == DeviceMountType.RACK_U) {
            Integer start = request.uPosition(), height = request.uHeight();
            if (start == null || height == null || start < 1 || height < 1 || rack.getRackUCapacity() == null
                    || (long) start + height - 1 > rack.getRackUCapacity()) throw new IllegalArgumentException("랙 용량 안에서 U 위치를 입력하세요");
            int end = start + height - 1;
            if (placementRepository.existsOverlapping(code, start, end, null) || modelPlacements.overlaps(code, start, end))
                throw new IllegalArgumentException("이미 장비 또는 모델이 설치된 U입니다");
        }
        if (type == DeviceMountType.RACK_SIDE && (request.rackSide() == null || request.rackSide() == net.vivans.dcim.module.device.domain.model.DeviceRackSide.REAR))
            throw new IllegalArgumentException("측면을 선택하세요");
        Double x = request.positionX(), y = request.positionY(), z = request.positionZ();
        if (type == DeviceMountType.RACK_U) { x = null; y = null; z = null; }
        if ((x == null || y == null || z == null) && !(x == null && y == null && z == null)) throw new IllegalArgumentException("위치 좌표 3개를 입력하세요");
        for (Double v : new Double[]{x,y,z}) if (v != null && (!Double.isFinite(v) || v < 0 || v > 1)) throw new IllegalArgumentException("위치 좌표는 0~1이어야 합니다");
        var created = net.vivans.dcim.module.device.domain.model.RackModelPlacement.create(code, model.getKey(), type,
                request.rackSide(), request.uPosition(), request.uHeight(), x, y, z);
        if (request.visualLengthRatio() != null) { if (type != DeviceMountType.RACK_SIDE) throw new IllegalArgumentException("길이 조절은 랙 측면 장착에만 적용됩니다"); created.setVisualLengthRatio(request.visualLengthRatio()); }
        modelPlacements.save(created);
        return getRackLayout(code);
    }

    @Transactional
    public RackLayoutResponse bindModelPlacement(String code, Integer id, Integer deviceId) {
        lockRack(code);
        var model = modelPlacements.findById(id).filter(item -> item.getRackLocationCode().equals(code))
                .orElseThrow(() -> new EntityNotFoundException("모델 배치를 찾을 수 없습니다"));
        if (placementRepository.findByDeviceId(deviceId).isPresent()) throw new IllegalArgumentException("이미 배치된 장비입니다. 기존 배치를 해제한 후 연결하세요");
        DeviceRackPlacementRequest request = new DeviceRackPlacementRequest(model.getMountType(), code, model.getRackSide(), model.getUPosition(), model.getUHeight(), null,
                model.getPositionX(), model.getPositionY(), model.getPositionZ(), false, model.getSceneModelKey(), true, model.getVisualLengthRatio());
        modelPlacements.delete(model); modelPlacements.flush();
        saveRackPlacement(deviceId, request);
        return getRackLayout(code);
    }

    @Transactional
    public RackLayoutResponse replacePlacementDevice(String code, Integer oldDeviceId, Integer newDeviceId) {
        LocationNode rack = lockRack(code);
        var placement = placementRepository.findByDeviceId(oldDeviceId)
                .filter(item -> item.getRackLocation() != null && code.equals(item.getRackLocation().getCode()))
                .orElseThrow(() -> new EntityNotFoundException("이 랙에서 장비 배치를 찾을 수 없습니다"));
        if (newDeviceId == null || newDeviceId <= 0) throw new IllegalArgumentException("연결할 장비를 선택하세요");
        if (oldDeviceId.equals(newDeviceId)) return getRackLayout(code);
        if (placementRepository.findByDeviceId(newDeviceId).isPresent())
            throw new IllegalArgumentException("이미 다른 위치에 배치된 장비입니다. 기존 배치를 해제한 후 연결하세요");
        Device target = findDevice(newDeviceId);
        Device previousDevice = placement.getDevice();
        var snapshot = DeviceRackPlacementHistory.PlacementSnapshot.from(placement);
        if (target.getLocationNode() == null || !rack.getCode().equals(target.getLocationNode().getCode())) {
            target.reassignLocation(rack);
            deviceRepository.save(target);
        }
        placement.update(target, rack, placement.getMountType(), placement.getRackSide(),
                placement.getUPosition(), placement.getUHeight(), placement.getFormFactor());
        placementRepository.save(placement);
        placementHistoryRepository.save(DeviceRackPlacementHistory.create(previousDevice, DeviceRackPlacementHistoryAction.REMOVED, snapshot, null));
        placementHistoryRepository.save(DeviceRackPlacementHistory.create(target, DeviceRackPlacementHistoryAction.PLACED, null, DeviceRackPlacementHistory.PlacementSnapshot.from(placement)));
        return getRackLayout(code);
    }

    @Transactional
    public void deleteModelPlacement(String code, Integer id) {
        lockRack(code);
        var item = modelPlacements.findById(id).filter(p -> p.getRackLocationCode().equals(code))
                .orElseThrow(() -> new EntityNotFoundException("모델 배치를 찾을 수 없습니다"));
        modelPlacements.delete(item);
    }

    @Transactional
    public RackLayoutResponse moveModelPlacement(String code, Integer id, DeviceRackPlacementRequest request) {
        var rack = lockRack(code);
        var item = modelPlacements.findById(id).filter(p -> p.getRackLocationCode().equals(code))
                .orElseThrow(() -> new EntityNotFoundException("모델 배치를 찾을 수 없습니다"));
        if (item.getMountType() == DeviceMountType.RACK_U) {
            Integer start = request.uPosition(), height = request.uHeight() == null ? item.getUHeight() : request.uHeight();
            if (start == null || height == null || start < 1 || height < 1 || rack.getRackUCapacity() == null || (long) start + height - 1 > rack.getRackUCapacity())
                throw new IllegalArgumentException("랙 용량 안에서 U 위치를 입력하세요");
            int end = start + height - 1;
            if (placementRepository.existsOverlapping(code, start, end, null) || modelPlacements.findAllByRackLocationCode(code).stream()
                    .anyMatch(p -> !p.getId().equals(id) && p.getMountType() == DeviceMountType.RACK_U && p.getUPosition() != null && p.lastU() != null && p.getUPosition() <= end && p.lastU() >= start))
                throw new IllegalArgumentException("이미 장비 또는 모델이 설치된 U입니다");
            item.setUPlacement(start, height);
        } else {
            item.setVisualizationPosition(request.positionX(), request.positionY(), request.positionZ());
            if (request.visualLengthRatio() != null) { if (item.getMountType() != DeviceMountType.RACK_SIDE) throw new IllegalArgumentException("길이 조절은 랙 측면 장착에만 적용됩니다"); item.setVisualLengthRatio(request.visualLengthRatio()); }
        }
        modelPlacements.save(item);
        return getRackLayout(code);
    }

    @Transactional
    public RackLayoutResponse replacePlacementModel(String code, Integer deviceId, String modelKey) {
        lockRack(code);
        String key = rackModelForSelection(modelKey);
        DeviceRackPlacement placement = placementRepository.findByDeviceId(deviceId)
                .filter(item -> item.getRackLocation() != null && code.equals(item.getRackLocation().getCode()))
                .orElseThrow(() -> new EntityNotFoundException("이 랙에서 장비 배치를 찾을 수 없습니다"));
        validateModelMount(key, placement.getMountType());
        placement.setSceneModelKey(key);
        placementRepository.save(placement);
        return getRackLayout(code);
    }

    @Transactional
    public RackLayoutResponse replaceUnlinkedModel(String code, Integer id, String modelKey) {
        lockRack(code);
        String key = rackModelForSelection(modelKey);
        var placement = modelPlacements.findById(id).filter(item -> code.equals(item.getRackLocationCode()))
                .orElseThrow(() -> new EntityNotFoundException("이 랙에서 모델 배치를 찾을 수 없습니다"));
        validateModelMount(key, placement.getMountType());
        placement.setSceneModelKey(key);
        modelPlacements.save(placement);
        return getRackLayout(code);
    }

    private void validateModelMount(String key, DeviceMountType type) {
        var model = sceneModelRepository.findById(key.trim()).orElseThrow(() -> new EntityNotFoundException("3D 모델을 찾을 수 없습니다"));
        if (!model.isEnabled() || !model.permitsRackMount(type)) throw new IllegalArgumentException("이 3D 모델은 선택한 랙 설치 방식에 사용할 수 없습니다");
    }

    private String rackModelForSelection(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("3D 모델을 선택하세요");
        var model = sceneModelRepository.findById(key.trim())
                .orElseThrow(() -> new EntityNotFoundException("3D 모델을 찾을 수 없습니다"));
        if (!model.isEnabled() || !model.isRackInstallable()) throw new IllegalArgumentException("랙 설치가 허용된 모델을 선택하세요");
        return model.getKey();
    }

    private LocationNode lockRack(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("랙을 선택하세요");
        var rack = locationLocks.lockByCode(code.trim()).orElseThrow(() -> new EntityNotFoundException("랙을 찾을 수 없습니다"));
        if (!RACK_LOCATION_TYPE.equalsIgnoreCase(rack.getLocationType().getCode())) throw new IllegalArgumentException("랙 위치가 아닙니다");
        return rack;
    }

    private void removePlacementWithHistory(DeviceRackPlacement placement) {
        DeviceRackPlacementHistory.PlacementSnapshot previous = DeviceRackPlacementHistory.PlacementSnapshot.from(placement);
        placementHistoryRepository.save(DeviceRackPlacementHistory.create(placement.getDevice(),
                DeviceRackPlacementHistoryAction.REMOVED, previous, null));
        placementRepository.delete(placement);
    }

    private Device findDevice(Integer id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + id));
    }

    private DeviceAsset assetOf(Device device) {
        if (device.getAsset() != null) {
            return device.getAsset();
        }
        return deviceAssetRepository.findById(device.getId()).orElse(null);
    }

    private String assetCodeOf(Device device) {
        DeviceAsset asset = assetOf(device);
        return asset == null ? null : asset.getAssetCode();
    }

    private String assetColorOf(Device device) {
        DeviceAsset asset = assetOf(device);
        return asset == null ? null : asset.getAssetColor();
    }

    private DeviceImageResponse primaryImage(Integer deviceId) {
        return imageRepository.findAllByDeviceId(deviceId).stream()
                .findFirst()
                .map(DeviceImageResponse::from)
                .orElse(null);
    }
}

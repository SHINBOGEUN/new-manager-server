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

    @Transactional
    public DeviceRackPlacementResponse saveRackPlacement(Integer deviceId, DeviceRackPlacementRequest request) {
        Device device = findDevice(deviceId);
        DeviceMountType mountType = request.mountType() == null ? DeviceMountType.RACK_U : request.mountType();
        LocationNode rack = resolveRack(mountType, request.rackLocationCode());
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
        }
        if (rack != null && !rack.getCode().equals(device.getLocationNode().getCode())) {
            device.reassignLocation(rack);
            deviceRepository.save(device);
        }
        DeviceRackPlacement placement = placementRepository.findByDeviceId(deviceId).orElse(null);
        DeviceRackPlacementHistory.PlacementSnapshot previous = DeviceRackPlacementHistory.PlacementSnapshot.from(placement);
        if (placement == null) {
            placement = DeviceRackPlacement.create(device, rack, mountType, request.rackSide(), request.uPosition(),
                    request.uHeight(), request.formFactor());
        }
        placement.update(device, rack, mountType, request.rackSide(), request.uPosition(), request.uHeight(), request.formFactor());
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
        List<RackLayoutResponse.RackLayoutItem> items = placementRepository.findAllByRackLocationCode(rackLocationCode).stream()
                .map(placement -> new RackLayoutResponse.RackLayoutItem(
                        placement.getDevice().getId(), placement.getDevice().getName(), assetCodeOf(placement.getDevice()),
                        placement.getDevice().getDeviceModel().getName(), placement.getMountType(), placement.getRackSide(),
                        placement.getUPosition(), placement.getUHeight(), placement.lastU(), placement.getFormFactor(),
                        assetColorOf(placement.getDevice()), primaryImage(placement.getDevice().getId())))
                .toList();
        return new RackLayoutResponse(rack.getCode(), rack.getName(), rack.getRackUCapacity(), items);
    }

    private void removePlacementWithHistory(DeviceRackPlacement placement) {
        DeviceRackPlacementHistory.PlacementSnapshot previous = DeviceRackPlacementHistory.PlacementSnapshot.from(placement);
        placementHistoryRepository.save(DeviceRackPlacementHistory.create(placement.getDevice(),
                DeviceRackPlacementHistoryAction.REMOVED, previous, null));
        placementRepository.delete(placement);
    }

    private LocationNode resolveRack(DeviceMountType mountType, String rackLocationCode) {
        if (!mountType.requiresRack()) {
            return null;
        }
        if (rackLocationCode == null || rackLocationCode.isBlank()) {
            throw new IllegalArgumentException(mountType + " requires rackLocationCode");
        }
        LocationNode rack = locationNodeRepository.findByCode(rackLocationCode.trim())
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + rackLocationCode));
        if (!RACK_LOCATION_TYPE.equalsIgnoreCase(rack.getLocationType().getCode())) {
            throw new IllegalArgumentException("rackLocationCode must refer to a RACK location");
        }
        return rack;
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

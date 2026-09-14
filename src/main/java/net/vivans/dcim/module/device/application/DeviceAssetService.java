package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.DeviceAssetDetailResponse;
import net.vivans.dcim.module.device.api.dto.DeviceAssetHistoryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceAssetStatusTransitionRequest;
import net.vivans.dcim.module.device.api.dto.DeviceAssetSummaryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceImageResponse;
import net.vivans.dcim.module.device.api.dto.DeviceProtocolEndpointResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementRequest;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementHistoryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceResponse;
import net.vivans.dcim.module.device.api.dto.RackLayoutResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistory;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistoryAction;
import net.vivans.dcim.module.device.domain.model.DeviceImage;
import net.vivans.dcim.module.device.domain.model.DeviceMountType;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacement;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistory;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistoryAction;
import net.vivans.dcim.module.device.domain.model.DeviceRackSide;
import net.vivans.dcim.module.device.domain.repository.DeviceImageRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceAssetHistoryRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementHistoryRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import net.vivans.dcim.module.pue.application.PueCollectorSyncService;
import net.vivans.dcim.shared.api.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceAssetService {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String RACK_LOCATION_TYPE = "RACK";
    private static final Set<String> LIFECYCLE_STATUS_CODES = Set.of(
            Device.ASSET_STATUS_ACTIVE, Device.ASSET_STATUS_MAINTENANCE, Device.ASSET_STATUS_FAULT,
            Device.ASSET_STATUS_INACTIVE, Device.ASSET_STATUS_RETIRED
    );
    private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            Device.ASSET_STATUS_INACTIVE, Set.of(Device.ASSET_STATUS_ACTIVE, Device.ASSET_STATUS_RETIRED),
            Device.ASSET_STATUS_ACTIVE, Set.of(Device.ASSET_STATUS_MAINTENANCE, Device.ASSET_STATUS_FAULT, Device.ASSET_STATUS_INACTIVE, Device.ASSET_STATUS_RETIRED),
            Device.ASSET_STATUS_MAINTENANCE, Set.of(Device.ASSET_STATUS_ACTIVE, Device.ASSET_STATUS_FAULT, Device.ASSET_STATUS_INACTIVE, Device.ASSET_STATUS_RETIRED),
            Device.ASSET_STATUS_FAULT, Set.of(Device.ASSET_STATUS_MAINTENANCE, Device.ASSET_STATUS_ACTIVE, Device.ASSET_STATUS_INACTIVE, Device.ASSET_STATUS_RETIRED),
            Device.ASSET_STATUS_RETIRED, Set.of(Device.ASSET_STATUS_INACTIVE)
    );

    private final DeviceRepository deviceRepository;
    private final DeviceProtocolEndpointRepository endpointRepository;
    private final DeviceRackPlacementRepository placementRepository;
    private final DeviceRackPlacementHistoryRepository placementHistoryRepository;
    private final DeviceAssetHistoryRepository assetHistoryRepository;
    private final DeviceImageRepository imageRepository;
    private final LocationNodeRepository locationNodeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final CollectionScriptSyncService collectionScriptSyncService;
    private final PueCollectorSyncService pueCollectorSyncService;

    @Value("${asset.image.storage-path:./uploads/device-images}")
    private String imageStoragePath;

    public PageResponse<DeviceAssetSummaryResponse> getAssets(String name, int page, int size) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Page<Device> devices = deviceRepository.findAll(null, null, name, null, null, null,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id")));
        return PageResponse.from(devices, this::toSummary);
    }

    public DeviceAssetDetailResponse getAsset(Integer deviceId) {
        Device device = findDevice(deviceId);
        List<DeviceImageResponse> images = imageRepository.findAllByDeviceId(deviceId).stream()
                .map(DeviceImageResponse::from).toList();
        return new DeviceAssetDetailResponse(
                DeviceResponse.from(device),
                endpointRepository.findAllByDeviceIdOrderByIdAsc(deviceId).stream()
                        .map(DeviceProtocolEndpointResponse::from).toList(),
                placementRepository.findByDeviceId(deviceId).map(DeviceRackPlacementResponse::from).orElse(null),
                images);
    }

    public List<DeviceAssetHistoryResponse> getAssetHistory(Integer deviceId) {
        findDevice(deviceId);
        return assetHistoryRepository.findAllByDeviceId(deviceId).stream()
                .map(DeviceAssetHistoryResponse::from).toList();
    }

    public DeviceAssetHistory.AssetSnapshot snapshotAsset(Device device) {
        return DeviceAssetHistory.AssetSnapshot.from(device);
    }

    @Transactional
    public void recordAssetChange(Device device, DeviceAssetHistory.AssetSnapshot previous) {
        recordAssetChange(device, previous, null, null);
    }

    @Transactional
    public DeviceResponse transitionAssetStatus(Integer deviceId, DeviceAssetStatusTransitionRequest request) {
        Device device = findDevice(deviceId);
        CommonCode nextStatus = commonCodeRepository.findById(request.statusId())
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + request.statusId()));
        validateStatusTransition(device.getAssetStatus(), nextStatus);
        DeviceAssetHistory.AssetSnapshot previous = snapshotAsset(device);
        Integer modelId = device.getDeviceModel().getId();
        device.transitionAssetStatus(nextStatus);
        DeviceResponse response = DeviceResponse.from(deviceRepository.save(device));
        recordAssetChange(device, previous, DeviceAssetHistoryAction.STATUS_CHANGED, request.reason());
        collectionScriptSyncService.regenerateByModelId(modelId);
        pueCollectorSyncService.repushActiveDefinitions();
        return response;
    }

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
        // Rack 장착형은 장비의 실제 설치 위치도 같은 Rack으로 맞춘다.
        if (rack != null && !rack.getCode().equals(device.getLocationNode().getCode())) {
            device.reassignLocation(rack);
            deviceRepository.save(device);
        }
        DeviceRackPlacement placement = placementRepository.findByDeviceId(deviceId).orElse(null);
        DeviceRackPlacementHistory.PlacementSnapshot previous = DeviceRackPlacementHistory.PlacementSnapshot.from(placement);
        if (placement == null) {
            placement = DeviceRackPlacement.create(device, rack, mountType, request.rackSide(), request.uPosition(), request.uHeight(), request.formFactor());
        }
        placement.update(device, rack, mountType, request.rackSide(), request.uPosition(), request.uHeight(), request.formFactor());
        DeviceRackPlacement saved = placementRepository.save(placement);
        DeviceRackPlacementHistory.PlacementSnapshot current = DeviceRackPlacementHistory.PlacementSnapshot.from(saved);
        if (!java.util.Objects.equals(previous, current)) {
            DeviceRackPlacementHistoryAction action = previous == null
                    ? DeviceRackPlacementHistoryAction.PLACED : DeviceRackPlacementHistoryAction.MOVED;
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
                .map(DeviceRackPlacementHistoryResponse::from).toList();
    }

    public RackLayoutResponse getRackLayout(String rackLocationCode) {
        LocationNode rack = locationNodeRepository.findByCode(rackLocationCode)
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + rackLocationCode));
        if (!RACK_LOCATION_TYPE.equalsIgnoreCase(rack.getLocationType().getCode())) {
            throw new IllegalArgumentException("location must be a RACK");
        }
        List<RackLayoutResponse.RackLayoutItem> items = placementRepository.findAllByRackLocationCode(rackLocationCode).stream()
                .map(placement -> new RackLayoutResponse.RackLayoutItem(
                        placement.getDevice().getId(), placement.getDevice().getName(), placement.getDevice().getAssetCode(),
                        placement.getDevice().getDeviceModel().getName(), placement.getMountType(), placement.getRackSide(),
                        placement.getUPosition(), placement.getUHeight(), placement.lastU(), placement.getFormFactor(), placement.getDevice().getAssetColor(),
                        primaryImage(placement.getDevice().getId())))
                .toList();
        return new RackLayoutResponse(rack.getCode(), rack.getName(), rack.getRackUCapacity(), items);
    }

    @Transactional
    public DeviceImageResponse uploadImage(Integer deviceId, MultipartFile file, boolean primary) {
        Device device = findDevice(deviceId);
        validateImage(file);
        List<DeviceImage> images = imageRepository.findAllByDeviceId(deviceId);
        boolean primaryImage = primary || images.isEmpty();
        if (primaryImage) {
            images.forEach(image -> image.setPrimary(false));
            imageRepository.saveAll(images);
        }
        String extension = extensionFor(file.getContentType());
        String storageKey = deviceId + "/" + UUID.randomUUID() + extension;
        Path target = storageRoot().resolve(storageKey).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            DeviceImage image = DeviceImage.create(device, storageKey, safeOriginalName(file.getOriginalFilename()),
                    file.getContentType(), file.getSize(), images.size(), primaryImage);
            return DeviceImageResponse.from(imageRepository.save(image));
        } catch (IOException e) {
            throw new IllegalStateException("failed to store device image", e);
        }
    }

    public Resource loadImage(Integer deviceId, Integer imageId) {
        DeviceImage image = findImage(deviceId, imageId);
        Path path = storageRoot().resolve(image.getStorageKey()).normalize();
        if (!path.startsWith(storageRoot()) || !Files.isRegularFile(path)) {
            throw new EntityNotFoundException("Device image file not found: " + imageId);
        }
        return new FileSystemResource(path);
    }

    public DeviceImageResponse getImage(Integer deviceId, Integer imageId) {
        return DeviceImageResponse.from(findImage(deviceId, imageId));
    }

    @Transactional
    public void deleteImage(Integer deviceId, Integer imageId) {
        DeviceImage image = findImage(deviceId, imageId);
        boolean wasPrimary = image.isPrimary();
        Path path = storageRoot().resolve(image.getStorageKey()).normalize();
        imageRepository.delete(image);
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
        if (wasPrimary) {
            imageRepository.findAllByDeviceId(deviceId).stream().findFirst().ifPresent(next -> {
                next.setPrimary(true);
                imageRepository.save(next);
            });
        }
    }

    @Transactional
    public void deleteAssetData(Integer deviceId) {
        placementRepository.findByDeviceId(deviceId).ifPresent(placementRepository::delete);
        for (DeviceImage image : imageRepository.findAllByDeviceId(deviceId)) {
            Path path = storageRoot().resolve(image.getStorageKey()).normalize();
            imageRepository.delete(image);
            try { Files.deleteIfExists(path); } catch (IOException ignored) { }
        }
    }

    private void removePlacementWithHistory(DeviceRackPlacement placement) {
        DeviceRackPlacementHistory.PlacementSnapshot previous = DeviceRackPlacementHistory.PlacementSnapshot.from(placement);
        placementHistoryRepository.save(DeviceRackPlacementHistory.create(placement.getDevice(),
                DeviceRackPlacementHistoryAction.REMOVED, previous, null));
        placementRepository.delete(placement);
    }

    private void recordAssetChange(
            Device device,
            DeviceAssetHistory.AssetSnapshot previous,
            DeviceAssetHistoryAction requestedAction,
            String reason
    ) {
        DeviceAssetHistory.AssetSnapshot current = snapshotAsset(device);
        if (previous == null || previous.equals(current)) {
            return;
        }
        boolean statusChanged = !java.util.Objects.equals(previous.statusCode(), current.statusCode());
        DeviceAssetHistoryAction action = requestedAction == null
                ? (statusChanged ? DeviceAssetHistoryAction.STATUS_CHANGED : DeviceAssetHistoryAction.ASSET_UPDATED)
                : requestedAction;
        assetHistoryRepository.save(DeviceAssetHistory.create(device, action, currentActor(), reason, previous, current));
    }

    private void validateStatusTransition(CommonCode current, CommonCode next) {
        String nextCode = normalizeStatusCode(next);
        if (!LIFECYCLE_STATUS_CODES.contains(nextCode)) {
            throw new IllegalArgumentException("asset status must use one of: " + String.join(", ", LIFECYCLE_STATUS_CODES));
        }
        if (next.getCodeGroup() == null || !Device.ASSET_STATUS_GROUP_KEY.equals(next.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("status must belong to ASSET_STATUS group");
        }
        if (current == null || !LIFECYCLE_STATUS_CODES.contains(normalizeStatusCode(current))) {
            return;
        }
        String currentCode = normalizeStatusCode(current);
        if (currentCode.equals(nextCode)) {
            throw new IllegalArgumentException("asset is already in status: " + nextCode);
        }
        Set<String> allowed = ALLOWED_STATUS_TRANSITIONS.get(currentCode);
        if (allowed == null || !allowed.contains(nextCode)) {
            throw new IllegalArgumentException("invalid asset status transition: " + currentCode + " -> " + nextCode);
        }
    }

    private static String normalizeStatusCode(CommonCode status) {
        return status.getCode() == null ? "" : status.getCode().trim().toUpperCase(Locale.ROOT);
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "system" : name;
    }

    private DeviceAssetSummaryResponse toSummary(Device device) {
        List<DeviceProtocolEndpoint> endpoints = endpointRepository.findAllByDeviceIdOrderByIdAsc(device.getId());
        String ipAddress = endpoints.stream().filter(DeviceProtocolEndpoint::isEnabled)
                .map(DeviceProtocolEndpoint::getHost).findFirst().orElse(null);
        List<String> protocolCodes = endpoints.stream().filter(DeviceProtocolEndpoint::isEnabled)
                .map(endpoint -> endpoint.getProtocolType().getCode()).distinct().toList();
        return new DeviceAssetSummaryResponse(DeviceResponse.from(device), ipAddress, protocolCodes,
                placementRepository.findByDeviceId(device.getId()).map(DeviceRackPlacementResponse::from).orElse(null),
                primaryImage(device.getId()));
    }

    private DeviceImageResponse primaryImage(Integer deviceId) {
        return imageRepository.findAllByDeviceId(deviceId).stream().findFirst().map(DeviceImageResponse::from).orElse(null);
    }

    private LocationNode resolveRack(DeviceMountType mountType, String rackLocationCode) {
        if (!mountType.requiresRack()) return null;
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
        return deviceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Device not found: " + id));
    }

    private DeviceImage findImage(Integer deviceId, Integer imageId) {
        return imageRepository.findByIdAndDeviceId(imageId, deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceImage not found: " + imageId));
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("image file is required");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(type)) throw new IllegalArgumentException("only JPEG, PNG and WEBP images are allowed");
        if (file.getSize() > MAX_IMAGE_SIZE) throw new IllegalArgumentException("image file must not exceed 10MB");
    }

    private Path storageRoot() {
        return Path.of(imageStoragePath).toAbsolutePath().normalize();
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; default -> ".webp"; };
    }

    private static String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "image";
        return Path.of(name).getFileName().toString();
    }
}

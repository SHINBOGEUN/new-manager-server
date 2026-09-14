package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.DeviceAssetDetailResponse;
import net.vivans.dcim.module.device.api.dto.DeviceAssetHistoryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceAssetStatusTransitionRequest;
import net.vivans.dcim.module.device.api.dto.DeviceAssetUpdateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceAssetSummaryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceAssetDocumentResponse;
import net.vivans.dcim.module.device.api.dto.DeviceImageResponse;
import net.vivans.dcim.module.device.api.dto.DeviceProtocolEndpointResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementRequest;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementResponse;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementHistoryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceResponse;
import net.vivans.dcim.module.device.api.dto.RackLayoutResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceAsset;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistory;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistoryAction;
import net.vivans.dcim.module.device.domain.model.DeviceAssetDocument;
import net.vivans.dcim.module.device.domain.model.DeviceImage;
import net.vivans.dcim.module.device.domain.model.DeviceMountType;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacement;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistory;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistoryAction;
import net.vivans.dcim.module.device.domain.model.DeviceRackSide;
import net.vivans.dcim.module.device.domain.repository.DeviceImageRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceAssetDocumentRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceAssetHistoryRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementHistoryRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceAssetSpringDataRepository;
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
    private static final long MAX_DOCUMENT_SIZE = 20L * 1024 * 1024;
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
    private final DeviceAssetSpringDataRepository deviceAssetRepository;
    private final DeviceProtocolEndpointRepository endpointRepository;
    private final DeviceRackPlacementRepository placementRepository;
    private final DeviceRackPlacementHistoryRepository placementHistoryRepository;
    private final DeviceAssetHistoryRepository assetHistoryRepository;
    private final DeviceImageRepository imageRepository;
    private final DeviceAssetDocumentRepository documentRepository;
    private final LocationNodeRepository locationNodeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final CollectionScriptSyncService collectionScriptSyncService;
    private final PueCollectorSyncService pueCollectorSyncService;

    @Value("${asset.image.storage-path:./uploads/device-images}")
    private String imageStoragePath;

    @Value("${asset.document.storage-path:./uploads/device-documents}")
    private String documentStoragePath;

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
        List<DeviceAssetDocumentResponse> documents = documentRepository.findAllByDeviceId(deviceId).stream()
                .map(DeviceAssetDocumentResponse::from).toList();
        return new DeviceAssetDetailResponse(
                DeviceResponse.from(device),
                endpointRepository.findAllByDeviceIdOrderByIdAsc(deviceId).stream()
                        .map(DeviceProtocolEndpointResponse::from).toList(),
                placementRepository.findByDeviceId(deviceId).map(DeviceRackPlacementResponse::from).orElse(null),
                images,
                documents);
    }

    public List<DeviceAssetHistoryResponse> getAssetHistory(Integer deviceId) {
        findDevice(deviceId);
        return assetHistoryRepository.findAllByDeviceId(deviceId).stream()
                .map(DeviceAssetHistoryResponse::from).toList();
    }

    public DeviceAssetHistory.AssetSnapshot snapshotAsset(Device device) {
        return DeviceAssetHistory.AssetSnapshot.from(device, assetOf(device));
    }

    /** 장비 생성·일반 장비 수정에서도 자산 속성은 이 1:1 테이블만 수정한다. */
    @Transactional
    public void updateAsset(Device device, String assetCode, String serialNumber, Integer assetStatusId, String assetColor) {
        CommonCode assetStatus = assetStatusId == null ? null : commonCodeRepository.findById(assetStatusId)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + assetStatusId));
        validateUniqueAssetCode(assetCode, device.getId());
        DeviceAsset asset = ensureAsset(device);
        asset.update(assetCode, serialNumber, assetStatus, assetColor);
        applyCollectionEligibility(device, assetStatus);
        deviceAssetRepository.save(asset);
    }

    @Transactional
    public DeviceResponse updateAssetDetail(Integer deviceId, DeviceAssetUpdateRequest request) {
        Device device = findDevice(deviceId);
        DeviceAssetHistory.AssetSnapshot previous = snapshotAsset(device);
        DeviceAsset asset = ensureAsset(device);
        validateUniqueAssetCode(request.assetCode(), deviceId);
        asset.update(request.assetCode(), request.serialNumber(), asset.getAssetStatus(), request.assetColor());
        asset.updateDetails(
                request.installedDate(),
                request.assetManagerName(),
                request.supplierName(),
                request.warrantyExpiresOn()
        );
        device.updateDescription(request.description());
        deviceAssetRepository.save(asset);
        DeviceResponse response = DeviceResponse.from(deviceRepository.save(device));
        recordAssetChange(device, previous);
        return response;
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
        DeviceAsset asset = ensureAsset(device);
        validateStatusTransition(asset.getAssetStatus(), nextStatus);
        DeviceAssetHistory.AssetSnapshot previous = snapshotAsset(device);
        Integer modelId = device.getDeviceModel().getId();
        asset.transitionStatus(nextStatus);
        applyCollectionEligibility(device, nextStatus);
        deviceAssetRepository.save(asset);
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
                        placement.getDevice().getId(), placement.getDevice().getName(), assetCodeOf(placement.getDevice()),
                        placement.getDevice().getDeviceModel().getName(), placement.getMountType(), placement.getRackSide(),
                        placement.getUPosition(), placement.getUHeight(), placement.lastU(), placement.getFormFactor(), assetColorOf(placement.getDevice()),
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
    public DeviceAssetDocumentResponse uploadDocument(Integer deviceId, MultipartFile file) {
        Device device = findDevice(deviceId);
        validateDocument(file);
        String originalName = safeOriginalName(file.getOriginalFilename());
        String storageKey = deviceId + "/" + UUID.randomUUID() + extensionFromName(originalName);
        Path target = documentStorageRoot().resolve(storageKey).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            DeviceAssetDocument document = DeviceAssetDocument.create(device, storageKey, originalName,
                    contentType(file), file.getSize());
            DeviceAssetDocumentResponse response = DeviceAssetDocumentResponse.from(documentRepository.save(document));
            recordDocumentChange(device, DeviceAssetHistoryAction.DOCUMENT_UPLOADED, originalName);
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("failed to store asset document", e);
        }
    }

    public Resource loadDocument(Integer deviceId, Integer documentId) {
        DeviceAssetDocument document = findDocument(deviceId, documentId);
        Path path = documentStorageRoot().resolve(document.getStorageKey()).normalize();
        if (!path.startsWith(documentStorageRoot()) || !Files.isRegularFile(path)) {
            throw new EntityNotFoundException("Asset document file not found: " + documentId);
        }
        return new FileSystemResource(path);
    }

    public DeviceAssetDocumentResponse getDocument(Integer deviceId, Integer documentId) {
        return DeviceAssetDocumentResponse.from(findDocument(deviceId, documentId));
    }

    @Transactional
    public void deleteDocument(Integer deviceId, Integer documentId) {
        DeviceAssetDocument document = findDocument(deviceId, documentId);
        Device device = document.getDevice();
        String originalName = document.getOriginalName();
        Path path = documentStorageRoot().resolve(document.getStorageKey()).normalize();
        documentRepository.delete(document);
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
        recordDocumentChange(device, DeviceAssetHistoryAction.DOCUMENT_DELETED, originalName);
    }

    @Transactional
    public void deleteAssetData(Integer deviceId) {
        placementRepository.findByDeviceId(deviceId).ifPresent(placementRepository::delete);
        for (DeviceImage image : imageRepository.findAllByDeviceId(deviceId)) {
            Path path = storageRoot().resolve(image.getStorageKey()).normalize();
            imageRepository.delete(image);
            try { Files.deleteIfExists(path); } catch (IOException ignored) { }
        }
        for (DeviceAssetDocument document : documentRepository.findAllByDeviceId(deviceId)) {
            Path path = documentStorageRoot().resolve(document.getStorageKey()).normalize();
            documentRepository.delete(document);
            try { Files.deleteIfExists(path); } catch (IOException ignored) { }
        }
        placementHistoryRepository.deleteByDeviceId(deviceId);
        assetHistoryRepository.deleteByDeviceId(deviceId);
        deviceAssetRepository.deleteDirectlyByDeviceId(deviceId);
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

    private void recordDocumentChange(Device device, DeviceAssetHistoryAction action, String originalName) {
        assetHistoryRepository.save(DeviceAssetHistory.create(device, action, currentActor(),
                "문서 " + (action == DeviceAssetHistoryAction.DOCUMENT_UPLOADED ? "등록: " : "삭제: ") + originalName,
                null, snapshotAsset(device)));
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
        return new DeviceAssetSummaryResponse(DeviceResponse.from(device, List.of()), ipAddress, protocolCodes,
                placementRepository.findByDeviceId(device.getId()).map(DeviceRackPlacementResponse::from).orElse(null),
                primaryImage(device.getId()), documentRepository.countByDeviceId(device.getId()));
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

    private DeviceAsset assetOf(Device device) {
        if (device.getAsset() != null) {
            return device.getAsset();
        }
        return deviceAssetRepository.findById(device.getId()).orElse(null);
    }

    private DeviceAsset ensureAsset(Device device) {
        DeviceAsset asset = assetOf(device);
        return asset == null ? deviceAssetRepository.save(DeviceAsset.create(device)) : asset;
    }

    private void validateUniqueAssetCode(String assetCode, Integer deviceId) {
        if (assetCode == null || assetCode.isBlank()) return;
        String normalized = assetCode.trim();
        boolean duplicated = deviceId == null
                ? deviceAssetRepository.existsByAssetCode(normalized)
                : deviceAssetRepository.existsByAssetCodeAndDeviceIdNot(normalized, deviceId);
        if (duplicated) throw new IllegalArgumentException("asset code already exists");
    }

    private static void applyCollectionEligibility(Device device, CommonCode status) {
        if (status == null) return;
        String code = normalizeStatusCode(status);
        if (Device.ASSET_STATUS_INACTIVE.equals(code) || Device.ASSET_STATUS_RETIRED.equals(code)) {
            device.setEnabled(false);
        } else if (Device.ASSET_STATUS_ACTIVE.equals(code)) {
            device.setEnabled(true);
        }
    }

    private String assetCodeOf(Device device) {
        DeviceAsset asset = assetOf(device);
        return asset == null ? null : asset.getAssetCode();
    }

    private String assetColorOf(Device device) {
        DeviceAsset asset = assetOf(device);
        return asset == null ? null : asset.getAssetColor();
    }

    private Device findDevice(Integer id) {
        return deviceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Device not found: " + id));
    }

    private DeviceImage findImage(Integer deviceId, Integer imageId) {
        return imageRepository.findByIdAndDeviceId(imageId, deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceImage not found: " + imageId));
    }

    private DeviceAssetDocument findDocument(Integer deviceId, Integer documentId) {
        return documentRepository.findByIdAndDeviceId(documentId, deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceAssetDocument not found: " + documentId));
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("image file is required");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(type)) throw new IllegalArgumentException("only JPEG, PNG and WEBP images are allowed");
        if (file.getSize() > MAX_IMAGE_SIZE) throw new IllegalArgumentException("image file must not exceed 10MB");
    }

    private void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("document file is required");
        if (file.getSize() > MAX_DOCUMENT_SIZE) throw new IllegalArgumentException("document file must not exceed 20MB");
    }

    private Path storageRoot() {
        return Path.of(imageStoragePath).toAbsolutePath().normalize();
    }

    private Path documentStorageRoot() {
        return Path.of(documentStoragePath).toAbsolutePath().normalize();
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; default -> ".webp"; };
    }

    private static String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "image";
        return Path.of(name).getFileName().toString();
    }

    private static String extensionFromName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 1 || dot == name.length() - 1 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static String contentType(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream" : file.getContentType();
    }
}

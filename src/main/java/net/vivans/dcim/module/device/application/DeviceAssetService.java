package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistory;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistoryAction;
import net.vivans.dcim.module.device.domain.model.DeviceAssetDocument;
import net.vivans.dcim.module.device.domain.model.DeviceImage;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.repository.DeviceImageRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceAssetDocumentRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceAssetHistoryRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementHistoryRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.infrastructure.storage.DeviceAssetFileStorage;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceAssetSpringDataRepository;
import net.vivans.dcim.shared.api.PageResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceAssetService {
    private final DeviceRepository deviceRepository;
    private final DeviceAssetSpringDataRepository deviceAssetRepository;
    private final DeviceProtocolEndpointRepository endpointRepository;
    private final DeviceRackPlacementRepository placementRepository;
    private final DeviceRackPlacementHistoryRepository placementHistoryRepository;
    private final DeviceAssetHistoryRepository assetHistoryRepository;
    private final DeviceImageRepository imageRepository;
    private final DeviceAssetDocumentRepository documentRepository;
    private final DeviceAssetLifecycleService lifecycleService;
    private final DeviceAssetRackService rackService;
    private final DeviceAssetFileStorage fileStorage;

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
        return lifecycleService.getAssetHistory(deviceId);
    }

    public DeviceAssetHistory.AssetSnapshot snapshotAsset(Device device) {
        return lifecycleService.snapshotAsset(device);
    }

    /** 장비 생성·일반 장비 수정에서도 자산 속성은 이 1:1 테이블만 수정한다. */
    @Transactional
    public void updateAsset(Device device, String assetCode, String serialNumber, Integer assetStatusId, String assetColor) {
        lifecycleService.updateAsset(device, assetCode, serialNumber, assetStatusId, assetColor);
    }

    @Transactional
    public DeviceResponse updateAssetDetail(Integer deviceId, DeviceAssetUpdateRequest request) {
        return lifecycleService.updateAssetDetail(deviceId, request);
    }

    @Transactional
    public void recordAssetChange(Device device, DeviceAssetHistory.AssetSnapshot previous) {
        lifecycleService.recordAssetChange(device, previous);
    }

    @Transactional
    public DeviceResponse transitionAssetStatus(Integer deviceId, DeviceAssetStatusTransitionRequest request) {
        return lifecycleService.transitionAssetStatus(deviceId, request);
    }

    @Transactional
    public DeviceRackPlacementResponse saveRackPlacement(Integer deviceId, DeviceRackPlacementRequest request) {
        return rackService.saveRackPlacement(deviceId, request);
    }

    @Transactional
    public void deleteRackPlacement(Integer deviceId) {
        rackService.deleteRackPlacement(deviceId);
    }

    @Transactional
    public void removeRackPlacementIfLocationChanged(Integer deviceId, String locationNodeCode) {
        rackService.removeRackPlacementIfLocationChanged(deviceId, locationNodeCode);
    }

    public List<DeviceRackPlacementHistoryResponse> getRackPlacementHistory(Integer deviceId) {
        return rackService.getRackPlacementHistory(deviceId);
    }

    public RackLayoutResponse getRackLayout(String rackLocationCode) {
        return rackService.getRackLayout(rackLocationCode);
    }

    @Transactional
    public DeviceImageResponse uploadImage(Integer deviceId, MultipartFile file, boolean primary) {
        Device device = findDevice(deviceId);
        fileStorage.validateImage(file);
        List<DeviceImage> images = imageRepository.findAllByDeviceId(deviceId);
        boolean primaryImage = primary || images.isEmpty();
        if (primaryImage) {
            images.forEach(image -> image.setPrimary(false));
            imageRepository.saveAll(images);
        }
        DeviceAssetFileStorage.StoredFile stored = fileStorage.storeImage(deviceId, file);
        DeviceImage image = DeviceImage.create(device, stored.storageKey(), stored.originalName(),
                stored.contentType(), stored.size(), images.size(), primaryImage);
        return DeviceImageResponse.from(imageRepository.save(image));
    }

    public Resource loadImage(Integer deviceId, Integer imageId) {
        DeviceImage image = findImage(deviceId, imageId);
        return fileStorage.loadImage(image.getStorageKey(), imageId);
    }

    public DeviceImageResponse getImage(Integer deviceId, Integer imageId) {
        return DeviceImageResponse.from(findImage(deviceId, imageId));
    }

    @Transactional
    public void deleteImage(Integer deviceId, Integer imageId) {
        DeviceImage image = findImage(deviceId, imageId);
        boolean wasPrimary = image.isPrimary();
        imageRepository.delete(image);
        fileStorage.deleteImage(image.getStorageKey());
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
        DeviceAssetFileStorage.StoredFile stored = fileStorage.storeDocument(deviceId, file);
        DeviceAssetDocument document = DeviceAssetDocument.create(device, stored.storageKey(), stored.originalName(),
                stored.contentType(), stored.size());
        DeviceAssetDocumentResponse response = DeviceAssetDocumentResponse.from(documentRepository.save(document));
        lifecycleService.recordDocumentChange(device, DeviceAssetHistoryAction.DOCUMENT_UPLOADED, stored.originalName());
        return response;
    }

    public Resource loadDocument(Integer deviceId, Integer documentId) {
        DeviceAssetDocument document = findDocument(deviceId, documentId);
        return fileStorage.loadDocument(document.getStorageKey(), documentId);
    }

    public DeviceAssetDocumentResponse getDocument(Integer deviceId, Integer documentId) {
        return DeviceAssetDocumentResponse.from(findDocument(deviceId, documentId));
    }

    @Transactional
    public void deleteDocument(Integer deviceId, Integer documentId) {
        DeviceAssetDocument document = findDocument(deviceId, documentId);
        Device device = document.getDevice();
        String originalName = document.getOriginalName();
        documentRepository.delete(document);
        fileStorage.deleteDocument(document.getStorageKey());
        lifecycleService.recordDocumentChange(device, DeviceAssetHistoryAction.DOCUMENT_DELETED, originalName);
    }

    @Transactional
    public void deleteAssetData(Integer deviceId) {
        placementRepository.findByDeviceId(deviceId).ifPresent(placementRepository::delete);
        for (DeviceImage image : imageRepository.findAllByDeviceId(deviceId)) {
            imageRepository.delete(image);
            fileStorage.deleteImage(image.getStorageKey());
        }
        for (DeviceAssetDocument document : documentRepository.findAllByDeviceId(deviceId)) {
            documentRepository.delete(document);
            fileStorage.deleteDocument(document.getStorageKey());
        }
        placementHistoryRepository.deleteByDeviceId(deviceId);
        assetHistoryRepository.deleteByDeviceId(deviceId);
        deviceAssetRepository.deleteDirectlyByDeviceId(deviceId);
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

}

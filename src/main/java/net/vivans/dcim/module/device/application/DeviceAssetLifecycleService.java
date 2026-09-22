package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.DeviceAssetHistoryResponse;
import net.vivans.dcim.module.device.api.dto.DeviceAssetStatusTransitionRequest;
import net.vivans.dcim.module.device.api.dto.DeviceAssetUpdateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceAsset;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistory;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistoryAction;
import net.vivans.dcim.module.device.domain.repository.DeviceAssetHistoryRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceAssetSpringDataRepository;
import net.vivans.dcim.module.pue.application.PueCollectorSyncService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceAssetLifecycleService {

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
    private final DeviceAssetHistoryRepository assetHistoryRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final CollectionScriptSyncService collectionScriptSyncService;
    private final PueCollectorSyncService pueCollectorSyncService;

    public List<DeviceAssetHistoryResponse> getAssetHistory(Integer deviceId) {
        findDevice(deviceId);
        return assetHistoryRepository.findAllByDeviceId(deviceId).stream()
                .map(DeviceAssetHistoryResponse::from)
                .toList();
    }

    public DeviceAssetHistory.AssetSnapshot snapshotAsset(Device device) {
        return DeviceAssetHistory.AssetSnapshot.from(device, assetOf(device));
    }

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
        asset.updateDetails(request.installedDate(), request.assetManagerName(), request.supplierName(),
                request.warrantyExpiresOn());
        device.updateDescription(request.description());
        deviceAssetRepository.save(asset);
        DeviceResponse response = DeviceResponse.from(deviceRepository.save(device));
        recordAssetChange(device, previous);
        return response;
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
    public void recordAssetChange(Device device, DeviceAssetHistory.AssetSnapshot previous) {
        recordAssetChange(device, previous, null, null);
    }

    @Transactional
    public void recordDocumentChange(Device device, DeviceAssetHistoryAction action, String originalName) {
        assetHistoryRepository.save(DeviceAssetHistory.create(device, action, currentActor(),
                "문서 " + (action == DeviceAssetHistoryAction.DOCUMENT_UPLOADED ? "등록: " : "삭제: ") + originalName,
                null, snapshotAsset(device)));
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
        if (assetCode == null || assetCode.isBlank()) {
            return;
        }
        String normalized = assetCode.trim();
        boolean duplicated = deviceId == null
                ? deviceAssetRepository.existsByAssetCode(normalized)
                : deviceAssetRepository.existsByAssetCodeAndDeviceIdNot(normalized, deviceId);
        if (duplicated) {
            throw new IllegalArgumentException("asset code already exists");
        }
    }

    private Device findDevice(Integer id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + id));
    }

    private static void applyCollectionEligibility(Device device, CommonCode status) {
        if (status == null) {
            return;
        }
        String code = normalizeStatusCode(status);
        if (Device.ASSET_STATUS_INACTIVE.equals(code) || Device.ASSET_STATUS_RETIRED.equals(code)) {
            device.setEnabled(false);
        } else if (Device.ASSET_STATUS_ACTIVE.equals(code)) {
            device.setEnabled(true);
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
}

package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device extends BaseEntity {

    /** 위치 미지정(선등록) 시 사용 */
    public static final String UNASSIGNED_LOCATION_CODE = "UNASSIGNED";

    /** PDU Path 피드 (A/B/C…). 차트 by_path 그룹 키 */
    public static final String LOCATION_PATH_GROUP_KEY = "LOCATION_PATH";
    public static final String ASSET_STATUS_GROUP_KEY = "ASSET_STATUS";
    public static final String ASSET_STATUS_ACTIVE = "ACTIVE";
    public static final String ASSET_STATUS_INACTIVE = "INACTIVE";
    public static final String ASSET_STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String ASSET_STATUS_FAULT = "FAULT";
    public static final String ASSET_STATUS_RETIRED = "RETIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private DeviceModel deviceModel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_node_code", nullable = false)
    private LocationNode locationNode;

    /** LOCATION_PATH 그룹. PDU 전원 피드. 차트 by_path 그룹 키. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_code_id")
    private CommonCode pathCode;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "asset_code", length = 100)
    private String assetCode;

    @Column(name = "serial_number", length = 200)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_status_id")
    private CommonCode assetStatus;

    @Column(name = "asset_color", length = 20)
    private String assetColor;

    @Column(nullable = false)
    private boolean enabled;

    @ManyToMany(mappedBy = "devices", fetch = FetchType.LAZY)
    private Set<DeviceGroup> deviceGroups = new LinkedHashSet<>();

    private Device(
            DeviceModel deviceModel,
            LocationNode locationNode,
            CommonCode pathCode,
            String name,
            String description,
            boolean enabled,
            String assetCode,
            String serialNumber,
            CommonCode assetStatus,
            String assetColor
    ) {
        validateDeviceModel(deviceModel);
        validateLocationNode(locationNode);
        validatePathCode(pathCode);
        validateName(name);
        validateAssetStatus(assetStatus);
        this.deviceModel = deviceModel;
        this.locationNode = locationNode;
        this.pathCode = pathCode;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.assetCode = blankToNull(assetCode);
        this.serialNumber = blankToNull(serialNumber);
        this.assetStatus = assetStatus;
        this.assetColor = blankToNull(assetColor);
        applyCollectionEligibility(assetStatus);
    }

    public static Device create(
            DeviceModel deviceModel,
            LocationNode locationNode,
            String name,
            String description
    ) {
        return create(deviceModel, locationNode, name, description, true, null);
    }

    public static Device create(
            DeviceModel deviceModel,
            LocationNode locationNode,
            String name,
            String description,
            boolean enabled
    ) {
        return create(deviceModel, locationNode, name, description, enabled, null);
    }

    public static Device create(
            DeviceModel deviceModel,
            LocationNode locationNode,
            String name,
            String description,
            boolean enabled,
            CommonCode pathCode
    ) {
        return new Device(deviceModel, locationNode, pathCode, name, description, enabled, null, null, null, null);
    }

    public static Device create(
            DeviceModel deviceModel,
            LocationNode locationNode,
            String name,
            String description,
            boolean enabled,
            CommonCode pathCode,
            String assetCode,
            String serialNumber,
            CommonCode assetStatus,
            String assetColor
    ) {
        return new Device(deviceModel, locationNode, pathCode, name, description, enabled,
                assetCode, serialNumber, assetStatus, assetColor);
    }

    public void update(
            DeviceModel deviceModel,
            LocationNode locationNode,
            String name,
            String description,
            boolean enabled
    ) {
        update(deviceModel, locationNode, name, description, enabled, this.pathCode);
    }

    public void update(
            DeviceModel deviceModel,
            LocationNode locationNode,
            String name,
            String description,
            boolean enabled,
            CommonCode pathCode
    ) {
        validateDeviceModel(deviceModel);
        validateLocationNode(locationNode);
        validatePathCode(pathCode);
        validateName(name);
        this.deviceModel = deviceModel;
        this.locationNode = locationNode;
        this.pathCode = pathCode;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    public void updateAsset(String assetCode, String serialNumber, CommonCode assetStatus, String assetColor) {
        validateAssetStatus(assetStatus);
        this.assetCode = blankToNull(assetCode);
        this.serialNumber = blankToNull(serialNumber);
        this.assetStatus = assetStatus;
        this.assetColor = blankToNull(assetColor);
        applyCollectionEligibility(assetStatus);
    }

    /** 자산 상태 전환. 미사용·폐기는 수집/장비 기반 위젯 대상에서 제외하고, 운영중은 다시 활성화한다. */
    public void transitionAssetStatus(CommonCode assetStatus) {
        validateAssetStatus(assetStatus);
        this.assetStatus = assetStatus;
        if (assetStatus == null) {
            return;
        }
        String code = assetStatus.getCode();
        if (ASSET_STATUS_INACTIVE.equalsIgnoreCase(code) || ASSET_STATUS_RETIRED.equalsIgnoreCase(code)) {
            this.enabled = false;
        } else if (ASSET_STATUS_ACTIVE.equalsIgnoreCase(code)) {
            this.enabled = true;
        }
    }

    public void reassignLocation(LocationNode locationNode) {
        validateLocationNode(locationNode);
        this.locationNode = locationNode;
    }

    public boolean isLocationUnassigned() {
        return UNASSIGNED_LOCATION_CODE.equals(locationNode.getCode());
    }

    private static void validateDeviceModel(DeviceModel deviceModel) {
        if (deviceModel == null) {
            throw new IllegalArgumentException("deviceModel is required");
        }
    }

    private static void validateLocationNode(LocationNode locationNode) {
        if (locationNode == null) {
            throw new IllegalArgumentException("locationNode is required");
        }
    }

    private static void validatePathCode(CommonCode pathCode) {
        if (pathCode == null) {
            return;
        }
        if (pathCode.getCodeGroup() == null
                || !LOCATION_PATH_GROUP_KEY.equals(pathCode.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("pathCode must belong to LOCATION_PATH group");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private static void validateAssetStatus(CommonCode assetStatus) {
        if (assetStatus == null) return;
        if (assetStatus.getCodeGroup() == null
                || !ASSET_STATUS_GROUP_KEY.equals(assetStatus.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("assetStatus must belong to ASSET_STATUS group");
        }
    }

    private void applyCollectionEligibility(CommonCode assetStatus) {
        if (assetStatus == null) return;
        String code = assetStatus.getCode();
        if (ASSET_STATUS_INACTIVE.equalsIgnoreCase(code) || ASSET_STATUS_RETIRED.equalsIgnoreCase(code)) {
            this.enabled = false;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

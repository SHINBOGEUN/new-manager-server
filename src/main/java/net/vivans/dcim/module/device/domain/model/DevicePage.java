package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(
        name = "device_page",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_page_device_page_code",
                columnNames = {"device_id", "page_code_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DevicePage extends BaseEntity {

    public static final String DEVICE_PAGE_GROUP_KEY = "DEVICE_PAGE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_code_id", nullable = false)
    private CommonCode pageCode;

    private DevicePage(Device device, CommonCode pageCode) {
        validateDevice(device);
        validatePageCode(pageCode);
        this.device = device;
        this.pageCode = pageCode;
    }

    public static DevicePage create(Device device, CommonCode pageCode) {
        return new DevicePage(device, pageCode);
    }

    private static void validateDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
    }

    private static void validatePageCode(CommonCode pageCode) {
        if (pageCode == null) {
            throw new IllegalArgumentException("pageCode is required");
        }
        if (!DEVICE_PAGE_GROUP_KEY.equals(pageCode.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("pageCode must belong to DEVICE_PAGE group");
        }
    }
}

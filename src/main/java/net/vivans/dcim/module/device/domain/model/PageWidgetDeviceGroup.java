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
import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "page_widget_device_group", uniqueConstraints = @UniqueConstraint(
        name = "uk_page_widget_device_group", columnNames = {"widget_id", "device_group_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetDeviceGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidget widget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_group_id", nullable = false)
    private DeviceGroup deviceGroup;

    private PageWidgetDeviceGroup(PageWidget widget, DeviceGroup deviceGroup) {
        this.widget = widget;
        this.deviceGroup = deviceGroup;
    }

    public static PageWidgetDeviceGroup create(PageWidget widget, DeviceGroup deviceGroup) {
        if (widget == null || deviceGroup == null || deviceGroup.getId() == null) {
            throw new IllegalArgumentException("widget and deviceGroup are required");
        }
        return new PageWidgetDeviceGroup(widget, deviceGroup);
    }
}

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

@Entity
@Table(
        name = "page_widget_device",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_page_widget_device_widget_device",
                columnNames = {"widget_id", "device_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidget widget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    private PageWidgetDevice(PageWidget widget, Device device) {
        this.widget = widget;
        this.device = device;
    }

    static PageWidgetDevice create(PageWidget widget, Device device) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
        return new PageWidgetDevice(widget, device);
    }
}

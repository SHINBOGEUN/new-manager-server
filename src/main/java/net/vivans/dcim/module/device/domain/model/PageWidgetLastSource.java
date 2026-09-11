package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
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
        name = "page_widget_last_source",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_page_widget_last_source_widget_device_point",
                columnNames = {"widget_id", "device_id", "point_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetLastSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidget widget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "point_name", nullable = false, length = 100)
    private String pointName;

    private PageWidgetLastSource(PageWidget widget, Device device, String pointName) {
        this.widget = widget;
        this.device = device;
        this.pointName = pointName;
    }

    static PageWidgetLastSource create(PageWidget widget, Device device, String pointName) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        if (device == null || device.getId() == null) {
            throw new IllegalArgumentException("device is required");
        }
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException("pointName is required");
        }
        return new PageWidgetLastSource(widget, device, pointName.trim());
    }
}

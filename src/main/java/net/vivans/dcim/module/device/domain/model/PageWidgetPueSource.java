package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "page_widget_pue_source", uniqueConstraints =
        @UniqueConstraint(name = "uk_page_widget_pue_source_widget_device", columnNames = {"widget_id", "device_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetPueSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidget widget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PageWidgetPueSourceRole role;

    @Column(name = "point_name", nullable = false, length = 100)
    private String pointName;

    private PageWidgetPueSource(PageWidget widget, Device device, PageWidgetPueSourceRole role, String pointName) {
        if (device == null || device.getId() == null) throw new IllegalArgumentException("PUE source device is required");
        if (pointName == null || pointName.isBlank()) throw new IllegalArgumentException("PUE source pointName is required");
        this.widget = widget;
        this.device = device;
        this.role = role;
        this.pointName = pointName.trim();
    }

    public static PageWidgetPueSource create(PageWidget widget, Device device, PageWidgetPueSourceRole role, String pointName) {
        return new PageWidgetPueSource(widget, device, role, pointName);
    }

    public void update(PageWidgetPueSourceRole role, String pointName) {
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException("PUE source pointName is required");
        }
        this.role = role;
        this.pointName = pointName.trim();
    }
}

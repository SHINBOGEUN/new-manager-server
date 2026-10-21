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
@Table(name = "page_widget_cctv", uniqueConstraints = @UniqueConstraint(
        name = "uk_page_widget_cctv_widget_camera", columnNames = {"widget_id", "camera_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetCctv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidget widget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camera_id", nullable = false)
    private CctvCamera camera;

    @jakarta.persistence.Column(name = "sort_order", nullable = false)
    private int sortOrder;

    private PageWidgetCctv(PageWidget widget, CctvCamera camera, int sortOrder) {
        if (widget == null || camera == null || camera.getId() == null) {
            throw new IllegalArgumentException("widget and camera are required");
        }
        this.widget = widget;
        this.camera = camera;
        this.sortOrder = sortOrder;
    }

    static PageWidgetCctv create(PageWidget widget, CctvCamera camera, int sortOrder) {
        return new PageWidgetCctv(widget, camera, sortOrder);
    }
}

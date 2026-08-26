package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "page_widget_layout")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetLayout extends BaseEntity {

    @Id
    @Column(name = "widget_id")
    private Integer widgetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "widget_id")
    private PageWidget widget;

    @Column(name = "grid_x", nullable = false)
    private int gridX;

    @Column(name = "grid_y", nullable = false)
    private int gridY;

    @Column(nullable = false)
    private int w;

    @Column(nullable = false)
    private int h;

    private PageWidgetLayout(PageWidget widget, int gridX, int gridY, int w, int h) {
        validate(gridX, gridY, w, h);
        this.widget = widget;
        this.gridX = gridX;
        this.gridY = gridY;
        this.w = w;
        this.h = h;
    }

    static PageWidgetLayout create(PageWidget widget, int gridX, int gridY, int w, int h) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        return new PageWidgetLayout(widget, gridX, gridY, w, h);
    }

    void update(int gridX, int gridY, int w, int h) {
        validate(gridX, gridY, w, h);
        this.gridX = gridX;
        this.gridY = gridY;
        this.w = w;
        this.h = h;
    }

    private static void validate(int gridX, int gridY, int w, int h) {
        if (gridX < 0) {
            throw new IllegalArgumentException("gridX must be >= 0");
        }
        if (gridY < 0) {
            throw new IllegalArgumentException("gridY must be >= 0");
        }
        if (w < 1) {
            throw new IllegalArgumentException("w must be >= 1");
        }
        if (h < 1) {
            throw new IllegalArgumentException("h must be >= 1");
        }
    }
}

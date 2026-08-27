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
        name = "page_widget_point",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_page_widget_point_widget_name",
                columnNames = {"widget_id", "point_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidget widget;

    @Column(name = "point_name", nullable = false, length = 100)
    private String pointName;

    private PageWidgetPoint(PageWidget widget, String pointName) {
        this.widget = widget;
        this.pointName = pointName;
    }

    static PageWidgetPoint create(PageWidget widget, String pointName) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException("pointName is required");
        }
        return new PageWidgetPoint(widget, pointName.trim());
    }
}

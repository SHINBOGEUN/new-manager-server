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
        name = "page_widget_model",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_page_widget_model_widget_model",
                columnNames = {"widget_id", "model_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidget widget;

    @Column(name = "model_id", nullable = false)
    private Integer modelId;

    private PageWidgetModel(PageWidget widget, Integer modelId) {
        this.widget = widget;
        this.modelId = modelId;
    }

    static PageWidgetModel create(PageWidget widget, Integer modelId) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        if (modelId == null || modelId <= 0) {
            throw new IllegalArgumentException("modelId is required");
        }
        return new PageWidgetModel(widget, modelId);
    }
}

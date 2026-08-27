package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "page_widget_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetCount extends BaseEntity {

    @Id
    @Column(name = "widget_id")
    private Integer widgetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "widget_id")
    private PageWidget widget;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_mode", nullable = false, length = 16)
    private PageWidgetCountMode countMode;

    @Column(name = "count_model_id")
    private Integer countModelId;

    private PageWidgetCount(PageWidget widget, PageWidgetCountMode countMode, Integer countModelId) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        this.widget = widget;
        this.countMode = countMode == null ? PageWidgetCountMode.by_model : countMode;
        this.countModelId = countModelId;
        validate();
    }

    static PageWidgetCount create(PageWidget widget, PageWidgetCountMode countMode, Integer countModelId) {
        return new PageWidgetCount(widget, countMode, countModelId);
    }

    void update(PageWidgetCountMode countMode, Integer countModelId) {
        this.countMode = countMode == null ? PageWidgetCountMode.by_model : countMode;
        this.countModelId = countModelId;
        validate();
    }

    private void validate() {
        if (countMode == PageWidgetCountMode.model && countModelId == null) {
            throw new IllegalArgumentException("countModelId is required when countMode is model");
        }
        if (countMode != PageWidgetCountMode.model && countModelId != null) {
            throw new IllegalArgumentException("countModelId is only allowed when countMode is model");
        }
    }
}

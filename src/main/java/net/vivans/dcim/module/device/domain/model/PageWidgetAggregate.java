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
@Table(name = "page_widget_aggregate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetAggregate extends BaseEntity {

    @Id
    @Column(name = "widget_id")
    private Integer widgetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "widget_id")
    private PageWidget widget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PageWidgetOp op;

    @Enumerated(EnumType.STRING)
    @Column(name = "range_preset", length = 16)
    private PageWidgetChartRangePreset rangePreset;

    private PageWidgetAggregate(
            PageWidget widget,
            PageWidgetOp op,
            PageWidgetChartRangePreset rangePreset
    ) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        if (op == null) {
            throw new IllegalArgumentException("aggregatePreset/op is required for aggregate");
        }
        this.widget = widget;
        this.op = op;
        this.rangePreset = resolveRangePreset(op, rangePreset);
        validate();
    }

    static PageWidgetAggregate create(
            PageWidget widget,
            PageWidgetOp op,
            PageWidgetChartRangePreset rangePreset
    ) {
        return new PageWidgetAggregate(widget, op, rangePreset);
    }

    void update(PageWidgetOp op, PageWidgetChartRangePreset rangePreset) {
        if (op == null) {
            throw new IllegalArgumentException("aggregatePreset/op is required for aggregate");
        }
        this.op = op;
        this.rangePreset = resolveRangePreset(op, rangePreset);
        validate();
    }

    private static PageWidgetChartRangePreset resolveRangePreset(
            PageWidgetOp op,
            PageWidgetChartRangePreset rangePreset
    ) {
        if (rangePreset != null) {
            return rangePreset;
        }
        if (op == PageWidgetOp.usage) {
            return PageWidgetChartRangePreset.today;
        }
        return rangePreset;
    }

    private void validate() {
        if (op == PageWidgetOp.usage && rangePreset == null) {
            throw new IllegalArgumentException("aggregateRangePreset is required for usage");
        }
    }
}

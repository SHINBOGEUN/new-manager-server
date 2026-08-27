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

    @Column(name = "weight_point", length = 100)
    private String weightPoint;

    @Column(name = "numerator_point", length = 100)
    private String numeratorPoint;

    @Column(name = "denominator_point", length = 100)
    private String denominatorPoint;

    private PageWidgetAggregate(
            PageWidget widget,
            PageWidgetOp op,
            String weightPoint,
            String numeratorPoint,
            String denominatorPoint
    ) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        if (op == null) {
            throw new IllegalArgumentException("op is required for aggregate");
        }
        this.widget = widget;
        this.op = op;
        this.weightPoint = blankToNull(weightPoint);
        this.numeratorPoint = blankToNull(numeratorPoint);
        this.denominatorPoint = blankToNull(denominatorPoint);
        validate();
    }

    static PageWidgetAggregate create(
            PageWidget widget,
            PageWidgetOp op,
            String weightPoint,
            String numeratorPoint,
            String denominatorPoint
    ) {
        return new PageWidgetAggregate(widget, op, weightPoint, numeratorPoint, denominatorPoint);
    }

    void update(
            PageWidgetOp op,
            String weightPoint,
            String numeratorPoint,
            String denominatorPoint
    ) {
        if (op == null) {
            throw new IllegalArgumentException("op is required for aggregate");
        }
        this.op = op;
        this.weightPoint = blankToNull(weightPoint);
        this.numeratorPoint = blankToNull(numeratorPoint);
        this.denominatorPoint = blankToNull(denominatorPoint);
        validate();
    }

    private void validate() {
        if (op == PageWidgetOp.weighted_avg && weightPoint == null) {
            throw new IllegalArgumentException("weightPoint is required for weighted_avg");
        }
        if (op == PageWidgetOp.divide && (numeratorPoint == null || denominatorPoint == null)) {
            throw new IllegalArgumentException("numeratorPoint and denominatorPoint are required for divide");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

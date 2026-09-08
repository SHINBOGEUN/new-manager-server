package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "page_widget_pue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetPue extends BaseEntity {

    @Id
    private Integer widgetId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id")
    private PageWidget widget;

    @Enumerated(EnumType.STRING)
    private PageWidgetChartRangePreset rangePreset;

    @Column(nullable = false)
    private int freshnessMinutes;

    private PageWidgetPue(PageWidget widget, PageWidgetChartRangePreset rangePreset, Integer freshnessMinutes) {
        this.widget = widget;
        this.rangePreset = rangePreset == null ? PageWidgetChartRangePreset.last_24h : rangePreset;
        this.freshnessMinutes = normalizeFreshnessMinutes(freshnessMinutes);
    }

    public static PageWidgetPue create(PageWidget widget, PageWidgetChartRangePreset rangePreset, Integer freshnessMinutes) {
        return new PageWidgetPue(widget, rangePreset, freshnessMinutes);
    }

    public void update(PageWidgetChartRangePreset rangePreset, Integer freshnessMinutes) {
        this.rangePreset = rangePreset == null ? PageWidgetChartRangePreset.last_24h : rangePreset;
        this.freshnessMinutes = normalizeFreshnessMinutes(freshnessMinutes);
    }

    private static int normalizeFreshnessMinutes(Integer value) {
        int resolved = value == null ? 15 : value;
        if (resolved < 1 || resolved > 1440) {
            throw new IllegalArgumentException("freshnessMinutes must be between 1 and 1440");
        }
        return resolved;
    }
}

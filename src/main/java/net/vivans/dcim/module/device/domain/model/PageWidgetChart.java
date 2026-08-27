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

import java.util.Set;

@Entity
@Table(name = "page_widget_chart")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetChart extends BaseEntity {

    private static final Set<String> ALLOWED_WINDOWS = Set.of("1m", "5m", "15m", "1h", "1d");

    @Id
    @Column(name = "widget_id")
    private Integer widgetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "widget_id")
    private PageWidget widget;

    @Enumerated(EnumType.STRING)
    @Column(name = "chart_scope", nullable = false, length = 16)
    private PageWidgetChartScope chartScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "chart_series_mode", nullable = false, length = 16)
    private PageWidgetChartSeriesMode chartSeriesMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "chart_range_preset", nullable = false, length = 16)
    private PageWidgetChartRangePreset chartRangePreset;

    @Column(name = "chart_window", nullable = false, length = 8)
    private String chartWindow;

    private PageWidgetChart(
            PageWidget widget,
            PageWidgetChartScope chartScope,
            PageWidgetChartSeriesMode chartSeriesMode,
            PageWidgetChartRangePreset chartRangePreset,
            String chartWindow
    ) {
        if (widget == null) {
            throw new IllegalArgumentException("widget is required");
        }
        this.widget = widget;
        this.chartScope = chartScope == null ? PageWidgetChartScope.devices : chartScope;
        this.chartSeriesMode = chartSeriesMode == null ? PageWidgetChartSeriesMode.per_device : chartSeriesMode;
        this.chartRangePreset = chartRangePreset == null ? PageWidgetChartRangePreset.last_24h : chartRangePreset;
        this.chartWindow = chartWindow == null || chartWindow.isBlank() ? "5m" : chartWindow.trim();
        validate();
    }

    static PageWidgetChart create(
            PageWidget widget,
            PageWidgetChartScope chartScope,
            PageWidgetChartSeriesMode chartSeriesMode,
            PageWidgetChartRangePreset chartRangePreset,
            String chartWindow
    ) {
        return new PageWidgetChart(widget, chartScope, chartSeriesMode, chartRangePreset, chartWindow);
    }

    void update(
            PageWidgetChartScope chartScope,
            PageWidgetChartSeriesMode chartSeriesMode,
            PageWidgetChartRangePreset chartRangePreset,
            String chartWindow
    ) {
        this.chartScope = chartScope == null ? PageWidgetChartScope.devices : chartScope;
        this.chartSeriesMode = chartSeriesMode == null ? PageWidgetChartSeriesMode.per_device : chartSeriesMode;
        this.chartRangePreset = chartRangePreset == null ? PageWidgetChartRangePreset.last_24h : chartRangePreset;
        this.chartWindow = chartWindow == null || chartWindow.isBlank() ? "5m" : chartWindow.trim();
        validate();
    }

    private void validate() {
        if (!ALLOWED_WINDOWS.contains(chartWindow)) {
            throw new IllegalArgumentException("chartWindow must be 1m, 5m, 15m, 1h, or 1d");
        }
    }
}

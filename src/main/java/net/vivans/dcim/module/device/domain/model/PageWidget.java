package net.vivans.dcim.module.device.domain.model;



import jakarta.persistence.CascadeType;

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

import jakarta.persistence.OneToMany;

import jakarta.persistence.OneToOne;

import jakarta.persistence.OrderBy;

import jakarta.persistence.Table;

import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;

import lombok.Getter;

import lombok.NoArgsConstructor;

import net.vivans.dcim.module.common.domain.model.CommonCode;

import net.vivans.dcim.shared.persistence.BaseEntity;



import java.util.ArrayList;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Set;



@Entity

@Table(

        name = "page_widget",

        uniqueConstraints = @UniqueConstraint(

                name = "uk_page_widget_page_name",

                columnNames = {"page_code_id", "name"}

        )

)

@Getter

@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class PageWidget extends BaseEntity {



    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Integer id;



    @ManyToOne(fetch = FetchType.LAZY, optional = false)

    @JoinColumn(name = "page_code_id", nullable = false)

    private CommonCode pageCode;



    @Column(nullable = false, length = 100)

    private String name;



    @Column(nullable = false)

    private boolean enabled;



    @Enumerated(EnumType.STRING)

    @Column(name = "query_kind", nullable = false, length = 16)

    private PageWidgetQueryKind queryKind;



    @Enumerated(EnumType.STRING)

    @Column(name = "group_by", length = 16)

    private PageWidgetGroupBy groupBy;



    @OneToOne(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)

    private PageWidgetAggregate aggregate;



    @OneToOne(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)

    private PageWidgetCount count;



    @OneToOne(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)

    private PageWidgetChart chart;



    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)

    @OrderBy("id ASC")

    private final List<PageWidgetPoint> points = new ArrayList<>();



    /** Set: Hibernate MultipleBagFetchException 회피 (points List와 동시 fetch) */

    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)

    @OrderBy("id ASC")

    private final Set<PageWidgetDevice> devices = new LinkedHashSet<>();



    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)

    @OrderBy("id ASC")

    private final Set<PageWidgetModel> models = new LinkedHashSet<>();



    @OneToOne(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)

    private PageWidgetLayout layout;



    private PageWidget(

            CommonCode pageCode,

            String name,

            boolean enabled,

            PageWidgetQueryKind queryKind,

            PageWidgetGroupBy groupBy

    ) {

        validatePageCode(pageCode);

        validateName(name);

        validateQueryKind(queryKind);

        this.pageCode = pageCode;

        this.name = name.trim();

        this.enabled = enabled;

        this.queryKind = queryKind;

        this.groupBy = groupBy;

    }



    public static PageWidget create(

            CommonCode pageCode,

            String name,

            boolean enabled,

            PageWidgetQueryKind queryKind,

            PageWidgetOp op,

            PageWidgetGroupBy groupBy,

            String weightPoint,

            String numeratorPoint,

            String denominatorPoint,

            PageWidgetCountMode countMode,

            Integer countModelId,

            PageWidgetChartScope chartScope,

            PageWidgetChartSeriesMode chartSeriesMode,

            PageWidgetChartRangePreset chartRangePreset,

            String chartWindow,

            List<String> pointNames,

            List<Device> devices,

            List<Integer> modelIds

    ) {

        PageWidget widget = new PageWidget(pageCode, name, enabled, queryKind, groupBy);

        widget.syncKindConfig(

                op, weightPoint, numeratorPoint, denominatorPoint,

                countMode, countModelId,

                chartScope, chartSeriesMode, chartRangePreset, chartWindow

        );

        widget.applyBindings(pointNames, devices, modelIds);

        return widget;

    }



    public void update(

            String name,

            boolean enabled,

            PageWidgetQueryKind queryKind,

            PageWidgetOp op,

            PageWidgetGroupBy groupBy,

            String weightPoint,

            String numeratorPoint,

            String denominatorPoint,

            PageWidgetCountMode countMode,

            Integer countModelId,

            PageWidgetChartScope chartScope,

            PageWidgetChartSeriesMode chartSeriesMode,

            PageWidgetChartRangePreset chartRangePreset,

            String chartWindow,

            List<String> pointNames,

            List<Device> devices,

            List<Integer> modelIds

    ) {

        validateName(name);

        validateQueryKind(queryKind);

        this.name = name.trim();

        this.enabled = enabled;

        this.queryKind = queryKind;

        this.groupBy = groupBy;

        syncKindConfig(

                op, weightPoint, numeratorPoint, denominatorPoint,

                countMode, countModelId,

                chartScope, chartSeriesMode, chartRangePreset, chartWindow

        );

        applyBindings(pointNames, devices, modelIds);

    }



    public PageWidgetOp getOp() {

        return aggregate == null ? null : aggregate.getOp();

    }



    public String getWeightPoint() {

        return aggregate == null ? null : aggregate.getWeightPoint();

    }



    public String getNumeratorPoint() {

        return aggregate == null ? null : aggregate.getNumeratorPoint();

    }



    public String getDenominatorPoint() {

        return aggregate == null ? null : aggregate.getDenominatorPoint();

    }



    public PageWidgetCountMode getCountMode() {

        return count == null ? null : count.getCountMode();

    }



    public Integer getCountModelId() {

        return count == null ? null : count.getCountModelId();

    }



    public PageWidgetChartScope getChartScope() {

        return chart == null ? null : chart.getChartScope();

    }



    public PageWidgetChartSeriesMode getChartSeriesMode() {

        return chart == null ? null : chart.getChartSeriesMode();

    }



    public PageWidgetChartRangePreset getChartRangePreset() {

        return chart == null ? null : chart.getChartRangePreset();

    }



    public String getChartWindow() {

        return chart == null ? null : chart.getChartWindow();

    }



    public void setEnabled(boolean enabled) {

        this.enabled = enabled;

    }



    public void upsertLayout(int gridX, int gridY, int w, int h) {

        if (this.layout == null) {

            this.layout = PageWidgetLayout.create(this, gridX, gridY, w, h);

        } else {

            this.layout.update(gridX, gridY, w, h);

        }

    }



    public void clearLayout() {

        this.layout = null;

    }



    public List<String> pointNames() {

        List<String> names = new ArrayList<>();

        for (PageWidgetPoint point : points) {

            names.add(point.getPointName());

        }

        return names;

    }



    public List<Integer> deviceIds() {

        List<Integer> ids = new ArrayList<>();

        for (PageWidgetDevice mapping : devices) {

            ids.add(mapping.getDevice().getId());

        }

        return ids;

    }



    public List<Integer> modelIds() {

        List<Integer> ids = new ArrayList<>();

        for (PageWidgetModel mapping : models) {

            ids.add(mapping.getModelId());

        }

        return ids;

    }



    private void syncKindConfig(

            PageWidgetOp op,

            String weightPoint,

            String numeratorPoint,

            String denominatorPoint,

            PageWidgetCountMode countMode,

            Integer countModelId,

            PageWidgetChartScope chartScope,

            PageWidgetChartSeriesMode chartSeriesMode,

            PageWidgetChartRangePreset chartRangePreset,

            String chartWindow

    ) {

        if (queryKind != PageWidgetQueryKind.aggregate) {

            this.aggregate = null;

        }

        if (queryKind != PageWidgetQueryKind.count) {

            this.count = null;

        }

        if (queryKind != PageWidgetQueryKind.chart) {

            this.chart = null;

        }



        switch (queryKind) {

            case aggregate -> {

                if (this.aggregate == null) {

                    this.aggregate = PageWidgetAggregate.create(

                            this, op, weightPoint, numeratorPoint, denominatorPoint);

                } else {

                    this.aggregate.update(op, weightPoint, numeratorPoint, denominatorPoint);

                }

            }

            case count -> {

                if (this.count == null) {

                    this.count = PageWidgetCount.create(this, countMode, countModelId);

                } else {

                    this.count.update(countMode, countModelId);

                }

            }

            case chart -> {

                if (this.chart == null) {

                    this.chart = PageWidgetChart.create(

                            this, chartScope, chartSeriesMode, chartRangePreset, chartWindow);

                } else {

                    this.chart.update(chartScope, chartSeriesMode, chartRangePreset, chartWindow);

                }

            }

            default -> { /* last: no extension row */ }

        }

        validateKindOptions(op, countMode, countModelId, chartScope, chartSeriesMode, chartRangePreset, chartWindow);

    }



    private void applyBindings(List<String> pointNames, List<Device> devices, List<Integer> modelIds) {

        if (queryKind == PageWidgetQueryKind.count) {

            replacePoints(List.of());

            replaceDevices(List.of());

            replaceModels(List.of());

        } else if (queryKind == PageWidgetQueryKind.chart

                && resolvedChartScope() == PageWidgetChartScope.models) {

            replacePoints(pointNames);

            replaceDevices(List.of());

            replaceModels(modelIds);

        } else {

            replacePoints(pointNames);

            replaceDevices(devices);

            replaceModels(List.of());

        }

        validateBindings();

    }



    private PageWidgetChartScope resolvedChartScope() {

        return chart == null ? PageWidgetChartScope.devices : chart.getChartScope();

    }



    private void replacePoints(List<String> pointNames) {

        points.clear();

        if (pointNames == null) {

            return;

        }

        Set<String> unique = new LinkedHashSet<>();

        for (String raw : pointNames) {

            if (raw != null && !raw.isBlank()) {

                unique.add(raw.trim());

            }

        }

        for (String pointName : unique) {

            points.add(PageWidgetPoint.create(this, pointName));

        }

    }



    private void replaceDevices(List<Device> devices) {

        this.devices.clear();

        if (devices == null) {

            return;

        }

        Set<Integer> uniqueIds = new LinkedHashSet<>();

        for (Device device : devices) {

            if (device == null || device.getId() == null) {

                throw new IllegalArgumentException("deviceId is required");

            }

            if (uniqueIds.add(device.getId())) {

                this.devices.add(PageWidgetDevice.create(this, device));

            }

        }

    }



    private void replaceModels(List<Integer> modelIds) {

        this.models.clear();

        if (modelIds == null) {

            return;

        }

        Set<Integer> uniqueIds = new LinkedHashSet<>();

        for (Integer modelId : modelIds) {

            if (modelId == null || modelId <= 0) {

                throw new IllegalArgumentException("modelIds must contain positive integers");

            }

            if (uniqueIds.add(modelId)) {

                this.models.add(PageWidgetModel.create(this, modelId));

            }

        }

    }



    private void validateBindings() {

        if (queryKind == PageWidgetQueryKind.count) {

            return;

        }

        if (queryKind == PageWidgetQueryKind.chart) {

            if (points.isEmpty()) {

                throw new IllegalArgumentException("pointNames is required for chart");

            }

            if (resolvedChartScope() == PageWidgetChartScope.models) {

                if (models.isEmpty()) {

                    throw new IllegalArgumentException("modelIds is required when chartScope is models");

                }

            } else if (devices.isEmpty()) {

                throw new IllegalArgumentException("deviceIds is required when chartScope is devices");

            }

            return;

        }

        if (devices.isEmpty()) {

            throw new IllegalArgumentException("deviceIds is required");

        }

        if (queryKind == PageWidgetQueryKind.last && points.isEmpty()) {

            throw new IllegalArgumentException("pointNames is required for last");

        }

        if (queryKind == PageWidgetQueryKind.aggregate && points.isEmpty()

                && getOp() != PageWidgetOp.divide) {

            throw new IllegalArgumentException("pointNames is required for aggregate");

        }

    }



    private void validateKindOptions(

            PageWidgetOp op,

            PageWidgetCountMode countMode,

            Integer countModelId,

            PageWidgetChartScope chartScope,

            PageWidgetChartSeriesMode chartSeriesMode,

            PageWidgetChartRangePreset chartRangePreset,

            String chartWindow

    ) {

        if (queryKind == PageWidgetQueryKind.aggregate && aggregate == null) {

            throw new IllegalArgumentException("op is required for aggregate");

        }

        if (queryKind != PageWidgetQueryKind.aggregate && op != null) {

            throw new IllegalArgumentException("op is only allowed for aggregate");

        }

        if (queryKind != PageWidgetQueryKind.count && (countMode != null || countModelId != null)) {

            throw new IllegalArgumentException("countMode is only allowed for count");

        }

        if (queryKind != PageWidgetQueryKind.chart

                && (chartScope != null || chartSeriesMode != null

                || chartRangePreset != null || (chartWindow != null && !chartWindow.isBlank()))) {

            throw new IllegalArgumentException("chart options are only allowed for chart");

        }

    }



    private static void validatePageCode(CommonCode pageCode) {

        if (pageCode == null) {

            throw new IllegalArgumentException("pageCode is required");

        }

        if (!DevicePageCodes.DEVICE_PAGE_GROUP_KEY.equals(pageCode.getCodeGroup().getGroupKey())) {

            throw new IllegalArgumentException("pageCode must belong to DEVICE_PAGE group");

        }

    }



    private static void validateName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException("name is required");

        }

    }



    private static void validateQueryKind(PageWidgetQueryKind queryKind) {

        if (queryKind == null) {

            throw new IllegalArgumentException("queryKind is required");

        }

    }

}



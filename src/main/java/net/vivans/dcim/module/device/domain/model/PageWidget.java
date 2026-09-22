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
import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;
import net.vivans.dcim.module.pue.domain.model.PueDefinition;
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

    private static final int DEFAULT_DATA_FRESHNESS_MINUTES = 15;

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

    @Column(name = "data_freshness_minutes", nullable = false)
    private int dataFreshnessMinutes = DEFAULT_DATA_FRESHNESS_MINUTES;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_kind", nullable = false, length = 32)
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

    @OneToOne(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PageWidgetPue pue;

    @OneToOne(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PageWidgetPsychrometric psychrometric;

    @OneToOne(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PageWidgetPowerDistribution powerDistribution;

    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<PageWidgetPoint> points = new ArrayList<>();

    /** Set: Hibernate MultipleBagFetchException 회피 (points List와 동시 fetch) */
    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final Set<PageWidgetDevice> devices = new LinkedHashSet<>();

    /** 차트·집계 위젯의 동적 대상. 그룹의 장비가 바뀌면 조회 대상도 함께 바뀐다. */
    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final Set<PageWidgetDeviceGroup> deviceGroups = new LinkedHashSet<>();

    /** latest 위젯에서 장비마다 서로 다른 측정항목을 선택하는 매핑 */
    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final Set<PageWidgetLastSource> lastSources = new LinkedHashSet<>();

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
        PageWidgetPolicy.validateIdentity(pageCode, name, queryKind);
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
            PageWidgetChartRangePreset aggregateRangePreset,
            PageWidgetCountMode countMode,
            Integer countModelId,
            PageWidgetChartScope chartScope,
            PageWidgetChartSeriesMode chartSeriesMode,
            PageWidgetChartRangePreset chartRangePreset,
            String chartWindow,
            List<String> pointNames,
            List<Device> devices,
            List<DeviceGroup> deviceGroups,
            List<Device> itDevices,
            List<Integer> modelIds
    ) {
        PageWidget widget = new PageWidget(pageCode, name, enabled, queryKind, groupBy);
        widget.syncKindConfig(op, aggregateRangePreset, countMode, countModelId,
                chartScope, chartSeriesMode, chartRangePreset, chartWindow);
        widget.applyBindings(pointNames, devices, deviceGroups, itDevices, modelIds);
        return widget;
    }

    /** 기존 호출부 호환용. 장비 그룹을 사용하지 않는 위젯 생성. */
    public static PageWidget create(
            CommonCode pageCode, String name, boolean enabled, PageWidgetQueryKind queryKind,
            PageWidgetOp op, PageWidgetGroupBy groupBy, PageWidgetChartRangePreset aggregateRangePreset,
            PageWidgetCountMode countMode, Integer countModelId, PageWidgetChartScope chartScope,
            PageWidgetChartSeriesMode chartSeriesMode, PageWidgetChartRangePreset chartRangePreset,
            String chartWindow, List<String> pointNames, List<Device> devices,
            List<Device> itDevices, List<Integer> modelIds
    ) {
        return create(pageCode, name, enabled, queryKind, op, groupBy, aggregateRangePreset,
                countMode, countModelId, chartScope, chartSeriesMode, chartRangePreset, chartWindow,
                pointNames, devices, List.of(), itDevices, modelIds);
    }

    public static PageWidget createPue(
            CommonCode pageCode,
            String name,
            boolean enabled,
            PueDefinition pueDefinition,
            PageWidgetChartRangePreset rangePreset,
            Integer freshnessMinutes
    ) {
        PageWidget widget = new PageWidget(pageCode, name, enabled, PageWidgetQueryKind.pue, null);
        widget.pue = PageWidgetPue.create(widget, pueDefinition, rangePreset, freshnessMinutes);
        return widget;
    }

    public static PageWidget createLast(
            CommonCode pageCode,
            String name,
            boolean enabled,
            List<LastSourceDefinition> sources
    ) {
        PageWidget widget = new PageWidget(pageCode, name, enabled, PageWidgetQueryKind.last, null);
        widget.syncKindConfig(null, null, null, null, null, null, null, null);
        widget.replaceLastSources(sources);
        widget.validateBindings();
        return widget;
    }

    public static PageWidget createPsychrometric(
            CommonCode pageCode,
            String name,
            boolean enabled,
            List<PageWidgetPsychrometric.SourceDefinition> sources
    ) {
        PageWidget widget = new PageWidget(pageCode, name, enabled, PageWidgetQueryKind.psychrometric, null);
        widget.psychrometric = PageWidgetPsychrometric.create(widget, sources);
        return widget;
    }

    public static PageWidget createPowerDistribution(
            CommonCode pageCode,
            String name,
            boolean enabled,
            List<PageWidgetPowerDistribution.GroupDefinition> groups
    ) {
        PageWidget widget = new PageWidget(pageCode, name, enabled, PageWidgetQueryKind.power_distribution, null);
        widget.powerDistribution = PageWidgetPowerDistribution.create(widget, groups);
        return widget;
    }

    public void update(
            String name,
            boolean enabled,
            PageWidgetQueryKind queryKind,
            PageWidgetOp op,
            PageWidgetGroupBy groupBy,
            PageWidgetChartRangePreset aggregateRangePreset,
            PageWidgetCountMode countMode,
            Integer countModelId,
            PageWidgetChartScope chartScope,
            PageWidgetChartSeriesMode chartSeriesMode,
            PageWidgetChartRangePreset chartRangePreset,
            String chartWindow,
            List<String> pointNames,
            List<Device> devices,
            List<DeviceGroup> deviceGroups,
            List<Device> itDevices,
            List<Integer> modelIds
    ) {
        PageWidgetPolicy.validateName(name);
        if (queryKind == null) {
            throw new IllegalArgumentException("queryKind is required");
        }
        this.name = name.trim();
        this.enabled = enabled;
        this.queryKind = queryKind;
        this.groupBy = groupBy;
        syncKindConfig(op, aggregateRangePreset, countMode, countModelId,
                chartScope, chartSeriesMode, chartRangePreset, chartWindow);
        applyBindings(pointNames, devices, deviceGroups, itDevices, modelIds);
    }

    /** 기존 호출부 호환용. 장비 그룹을 사용하지 않는 위젯 수정. */
    public void update(
            String name, boolean enabled, PageWidgetQueryKind queryKind, PageWidgetOp op,
            PageWidgetGroupBy groupBy, PageWidgetChartRangePreset aggregateRangePreset,
            PageWidgetCountMode countMode, Integer countModelId, PageWidgetChartScope chartScope,
            PageWidgetChartSeriesMode chartSeriesMode, PageWidgetChartRangePreset chartRangePreset,
            String chartWindow, List<String> pointNames, List<Device> devices,
            List<Device> itDevices, List<Integer> modelIds
    ) {
        update(name, enabled, queryKind, op, groupBy, aggregateRangePreset, countMode, countModelId,
                chartScope, chartSeriesMode, chartRangePreset, chartWindow, pointNames, devices,
                List.of(), itDevices, modelIds);
    }

    public void updateLast(String name, boolean enabled, List<LastSourceDefinition> sources) {
        PageWidgetPolicy.validateName(name);
        this.name = name.trim();
        this.enabled = enabled;
        this.queryKind = PageWidgetQueryKind.last;
        this.groupBy = null;
        syncKindConfig(null, null, null, null, null, null, null, null);
        replaceDeviceGroups(List.of());
        replaceLastSources(sources);
        validateBindings();
    }

    public PageWidgetOp getOp() {
        return aggregate == null ? null : aggregate.getOp();
    }

    public PageWidgetChartRangePreset getAggregateRangePreset() {
        return aggregate == null ? null : aggregate.getRangePreset();
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

    public PageWidgetChartRangePreset getPueRangePreset() {
        return pue == null ? null : pue.getRangePreset();
    }

    public Integer getPueFreshnessMinutes() {
        return pue == null ? null : pue.getFreshnessMinutes();
    }
    public Integer getPueDefinitionId() { return pue == null ? null : pue.getPueDefinition().getId(); }

    public void updatePsychrometric(
            String name,
            boolean enabled,
            List<PageWidgetPsychrometric.SourceDefinition> sources
    ) {
        if (queryKind != PageWidgetQueryKind.psychrometric || psychrometric == null) {
            throw new IllegalArgumentException("queryKind must be psychrometric");
        }
        PageWidgetPolicy.validateName(name);
        this.name = name.trim();
        this.enabled = enabled;
        psychrometric.update(sources);
    }

    public void updatePue(
            String name,
            boolean enabled,
            PueDefinition pueDefinition,
            PageWidgetChartRangePreset rangePreset,
            Integer freshnessMinutes
    ) {
        if (queryKind != PageWidgetQueryKind.pue || pue == null) {
            throw new IllegalArgumentException("queryKind must be pue");
        }
        PageWidgetPolicy.validateName(name);
        this.name = name.trim();
        this.enabled = enabled;
        pue.update(pueDefinition, rangePreset, freshnessMinutes);
    }

    public void updatePowerDistribution(
            String name,
            boolean enabled,
            List<PageWidgetPowerDistribution.GroupDefinition> groups
    ) {
        if (queryKind != PageWidgetQueryKind.power_distribution || powerDistribution == null) {
            throw new IllegalArgumentException("queryKind must be power_distribution");
        }
        PageWidgetPolicy.validateName(name);
        this.name = name.trim();
        this.enabled = enabled;
        powerDistribution.update(groups);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void updateDataFreshnessMinutes(Integer freshnessMinutes) {
        int value = freshnessMinutes == null ? DEFAULT_DATA_FRESHNESS_MINUTES : freshnessMinutes;
        if (value < 1 || value > 1440) {
            throw new IllegalArgumentException("dataFreshnessMinutes must be between 1 and 1440");
        }
        this.dataFreshnessMinutes = value;
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

    /** DEFAULT 또는 TOTAL 역할 장비 (non-IT). */
    public List<Integer> deviceIds() {
        List<Integer> ids = new ArrayList<>();
        for (PageWidgetDevice mapping : devices) {
            if (mapping.getDeviceRole() != PageWidgetDeviceRole.IT) {
                ids.add(mapping.getDevice().getId());
            }
        }
        return ids;
    }

    public List<Integer> deviceGroupIds() {
        return deviceGroups.stream().map(mapping -> mapping.getDeviceGroup().getId()).toList();
    }

    /** 직접 선택 장비와 활성 장비 그룹의 현재 장비를 중복 없이 합친다. */
    public List<Device> resolvedDefaultDevices() {
        java.util.Map<Integer, Device> resolved = new java.util.LinkedHashMap<>();
        for (PageWidgetDevice mapping : devices) {
            if (mapping.getDeviceRole() != PageWidgetDeviceRole.IT) {
                resolved.putIfAbsent(mapping.getDevice().getId(), mapping.getDevice());
            }
        }
        for (PageWidgetDeviceGroup mapping : deviceGroups) {
            DeviceGroup group = mapping.getDeviceGroup();
            if (!group.isEnabled()) {
                continue;
            }
            for (Device device : group.getDevices()) {
                resolved.putIfAbsent(device.getId(), device);
            }
        }
        return new ArrayList<>(resolved.values());
    }

    public List<Integer> modelIds() {
        List<Integer> ids = new ArrayList<>();
        for (PageWidgetModel mapping : models) {
            ids.add(mapping.getModelId());
        }
        return ids;
    }

    /**
     * 최신값 위젯의 장비별 포인트 선택. 기존 위젯은 deviceIds × pointNames 조합으로 호환 처리한다.
     */
    public List<LastSourceDefinition> lastSourceDefinitions() {
        if (!lastSources.isEmpty()) {
            java.util.Map<Integer, LastSourceDefinition> grouped = new java.util.LinkedHashMap<>();
            for (PageWidgetLastSource source : lastSources) {
                Device device = source.getDevice();
                LastSourceDefinition current = grouped.get(device.getId());
                if (current == null) {
                    grouped.put(device.getId(), new LastSourceDefinition(device,
                            new ArrayList<>(List.of(source.getPointName()))));
                } else {
                    current.pointNames().add(source.getPointName());
                }
            }
            return grouped.values().stream()
                    .map(source -> new LastSourceDefinition(source.device(), List.copyOf(source.pointNames())))
                    .toList();
        }
        List<String> names = pointNames();
        return devices.stream()
                .filter(mapping -> mapping.getDeviceRole() != PageWidgetDeviceRole.IT)
                .map(mapping -> new LastSourceDefinition(mapping.getDevice(), names))
                .toList();
    }

    private void syncKindConfig(
            PageWidgetOp op,
            PageWidgetChartRangePreset aggregateRangePreset,
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
        if (queryKind != PageWidgetQueryKind.pue) {
            this.pue = null;
        }
        if (queryKind != PageWidgetQueryKind.psychrometric) {
            this.psychrometric = null;
        }
        if (queryKind != PageWidgetQueryKind.power_distribution) {
            this.powerDistribution = null;
        }
        if (queryKind != PageWidgetQueryKind.last) {
            this.lastSources.clear();
        }

        switch (queryKind) {
            case aggregate -> {
                if (this.aggregate == null) {
                    this.aggregate = PageWidgetAggregate.create(this, op, aggregateRangePreset);
                } else {
                    this.aggregate.update(op, aggregateRangePreset);
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
        PageWidgetPolicy.validateKindOptions(queryKind, aggregate != null, op, aggregateRangePreset,
                countMode, countModelId, chartScope, chartSeriesMode, chartRangePreset, chartWindow);
    }

    private void applyBindings(
            List<String> pointNames,
            List<Device> devices,
            List<DeviceGroup> deviceGroups,
            List<Device> itDevices,
            List<Integer> modelIds
    ) {
        if (queryKind == PageWidgetQueryKind.count || queryKind == PageWidgetQueryKind.pue
                || queryKind == PageWidgetQueryKind.psychrometric || queryKind == PageWidgetQueryKind.power_distribution) {
            replacePoints(List.of());
            replaceDevices(List.of(), List.of());
            replaceDeviceGroups(List.of());
            replaceModels(List.of());
        } else if (queryKind == PageWidgetQueryKind.chart
                && resolvedChartScope() == PageWidgetChartScope.models) {
            replacePoints(pointNames);
            replaceDevices(List.of(), List.of());
            replaceDeviceGroups(List.of());
            replaceModels(modelIds);
        } else if (queryKind == PageWidgetQueryKind.aggregate) {
            replacePoints(pointNames);
            replaceDevices(devices, itDevices);
            replaceDeviceGroups(deviceGroups);
            replaceModels(List.of());
        } else {
            this.lastSources.clear();
            replacePoints(pointNames);
            replaceDevices(devices, List.of());
            replaceDeviceGroups(deviceGroups);
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

    private void replaceDevices(List<Device> devices, List<Device> itDevices) {
        this.devices.clear();
        if (devices == null) {
            return;
        }
        Set<Integer> uniqueIds = new LinkedHashSet<>();
        for (Device device : devices) {
            requireDevice(device);
            if (uniqueIds.add(device.getId())) {
                this.devices.add(PageWidgetDevice.create(this, device, PageWidgetDeviceRole.DEFAULT));
            }
        }
    }

    private void replaceDeviceGroups(List<DeviceGroup> groups) {
        deviceGroups.clear();
        if (groups == null) {
            return;
        }
        Set<Integer> uniqueIds = new LinkedHashSet<>();
        for (DeviceGroup group : groups) {
            if (group != null && group.getId() != null && uniqueIds.add(group.getId())) {
                deviceGroups.add(PageWidgetDeviceGroup.create(this, group));
            }
        }
    }

    private void replaceLastSources(List<LastSourceDefinition> sources) {
        this.lastSources.clear();
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("lastSources is required for last");
        }
        List<Device> selectedDevices = new ArrayList<>();
        List<String> selectedPointNames = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (LastSourceDefinition source : sources) {
            if (source == null) {
                continue;
            }
            requireDevice(source.device());
            if (source.pointNames() == null || source.pointNames().isEmpty()) {
                throw new IllegalArgumentException("pointNames is required for last source");
            }
            selectedDevices.add(source.device());
            for (String pointName : source.pointNames()) {
                if (pointName == null || pointName.isBlank()) {
                    throw new IllegalArgumentException("pointName is required for last source");
                }
                String normalized = pointName.trim();
                String key = source.device().getId() + "\u0000" + normalized;
                if (unique.add(key)) {
                    this.lastSources.add(PageWidgetLastSource.create(this, source.device(), normalized));
                    selectedPointNames.add(normalized);
                }
            }
        }
        if (this.lastSources.isEmpty()) {
            throw new IllegalArgumentException("lastSources is required for last");
        }
        replacePoints(selectedPointNames);
        replaceDevices(selectedDevices, List.of());
        replaceModels(List.of());
    }

    private static void requireDevice(Device device) {
        if (device == null || device.getId() == null) {
            throw new IllegalArgumentException("deviceId is required");
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
        PageWidgetPolicy.validateBindings(queryKind, resolvedChartScope(), points.size(), devices.size(),
                deviceGroups.size(), models.size());
    }

    public record LastSourceDefinition(Device device, List<String> pointNames) {
    }
}

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
    @Column(length = 16)
    private PageWidgetOp op;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_by", length = 16)
    private PageWidgetGroupBy groupBy;

    @Column(name = "weight_point", length = 100)
    private String weightPoint;

    @Column(name = "numerator_point", length = 100)
    private String numeratorPoint;

    @Column(name = "denominator_point", length = 100)
    private String denominatorPoint;

    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<PageWidgetPoint> points = new ArrayList<>();

    /** Set: Hibernate MultipleBagFetchException 회피 (points List와 동시 fetch) */
    @OneToMany(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final Set<PageWidgetDevice> devices = new LinkedHashSet<>();

    @OneToOne(mappedBy = "widget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PageWidgetLayout layout;

    private PageWidget(
            CommonCode pageCode,
            String name,
            boolean enabled,
            PageWidgetQueryKind queryKind,
            PageWidgetOp op,
            PageWidgetGroupBy groupBy,
            String weightPoint,
            String numeratorPoint,
            String denominatorPoint
    ) {
        validatePageCode(pageCode);
        validateName(name);
        validateQueryKind(queryKind);
        this.pageCode = pageCode;
        this.name = name.trim();
        this.enabled = enabled;
        this.queryKind = queryKind;
        this.op = op;
        this.groupBy = groupBy;
        this.weightPoint = blankToNull(weightPoint);
        this.numeratorPoint = blankToNull(numeratorPoint);
        this.denominatorPoint = blankToNull(denominatorPoint);
        validateOptions();
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
            List<String> pointNames,
            List<Device> devices
    ) {
        PageWidget widget = new PageWidget(
                pageCode, name, enabled, queryKind, op, groupBy,
                weightPoint, numeratorPoint, denominatorPoint
        );
        widget.replacePoints(pointNames);
        widget.replaceDevices(devices);
        widget.validateBindings();
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
            List<String> pointNames,
            List<Device> devices
    ) {
        validateName(name);
        validateQueryKind(queryKind);
        this.name = name.trim();
        this.enabled = enabled;
        this.queryKind = queryKind;
        this.op = op;
        this.groupBy = groupBy;
        this.weightPoint = blankToNull(weightPoint);
        this.numeratorPoint = blankToNull(numeratorPoint);
        this.denominatorPoint = blankToNull(denominatorPoint);
        validateOptions();
        replacePoints(pointNames);
        replaceDevices(devices);
        validateBindings();
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

    private void validateBindings() {
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("deviceIds is required");
        }
        if (queryKind == PageWidgetQueryKind.last && points.isEmpty()) {
            throw new IllegalArgumentException("pointNames is required for last");
        }
        if (queryKind == PageWidgetQueryKind.aggregate && points.isEmpty()
                && op != PageWidgetOp.divide) {
            throw new IllegalArgumentException("pointNames is required for aggregate");
        }
    }

    private void validateOptions() {
        if (queryKind == PageWidgetQueryKind.aggregate && op == null) {
            throw new IllegalArgumentException("op is required for aggregate");
        }
        if (queryKind != PageWidgetQueryKind.aggregate && op != null) {
            throw new IllegalArgumentException("op is only allowed for aggregate");
        }
        if (op == PageWidgetOp.weighted_avg && weightPoint == null) {
            throw new IllegalArgumentException("weightPoint is required for weighted_avg");
        }
        if (op == PageWidgetOp.divide && (numeratorPoint == null || denominatorPoint == null)) {
            throw new IllegalArgumentException("numeratorPoint and denominatorPoint are required for divide");
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

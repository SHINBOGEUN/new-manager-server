package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistributionGroup;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistributionSource;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.query.api.dto.PowerDistributionGroupValueResponse;
import net.vivans.dcim.module.query.api.dto.PowerDistributionSourceValueResponse;
import net.vivans.dcim.module.query.api.dto.PowerDistributionWidgetResponse;
import net.vivans.dcim.module.query.api.dto.WidgetDataStatusResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PowerDistributionQueryService {

    private static final Duration LOOKBACK = Duration.ofHours(24);

    private final PageWidgetRepository pageWidgetRepository;
    private final PointQuery pointQuery;
    private final WidgetDataStatusResolver widgetDataStatusResolver;

    public PowerDistributionWidgetResponse getPowerDistribution(Integer widgetId) {
        PageWidget widget = pageWidgetRepository.findById(widgetId)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + widgetId));
        if (widget.getQueryKind() != PageWidgetQueryKind.power_distribution) {
            throw new IllegalArgumentException("queryKind must be power_distribution");
        }
        if (!widget.isEnabled()) throw new IllegalArgumentException("widget is disabled");
        if (widget.getPowerDistribution() == null || widget.getPowerDistribution().getGroups().isEmpty()) {
            throw new IllegalArgumentException("power distribution groups are required");
        }

        List<PageWidgetPowerDistributionSource> sources = widget.getPowerDistribution().getGroups().stream()
                .flatMap(group -> group.getSources().stream())
                .toList();
        List<Integer> deviceIds = sources.stream().map(source -> source.getDevice().getId()).distinct().toList();
        List<String> pointNames = sources.stream().map(PageWidgetPowerDistributionSource::getPointName).distinct().toList();
        Map<String, LastPoint> latestBySource = new HashMap<>();
        for (LastPoint point : pointQuery.findLast(deviceIds, pointNames, LOOKBACK)) {
            latestBySource.put(key(point.deviceId(), point.pointName()), point);
        }

        List<PowerDistributionGroupValueResponse> groups = new ArrayList<>();
        double total = 0;
        boolean complete = true;
        for (PageWidgetPowerDistributionGroup group : widget.getPowerDistribution().getGroups()) {
            double sum = 0;
            boolean groupComplete = true;
            List<PowerDistributionSourceValueResponse> rows = new ArrayList<>();
            for (PageWidgetPowerDistributionSource source : group.getSources()) {
                LastPoint point = latestBySource.get(key(source.getDevice().getId(), source.getPointName()));
                if (point == null) {
                    groupComplete = false;
                } else {
                    sum += point.value();
                }
                rows.add(new PowerDistributionSourceValueResponse(
                        source.getDevice().getId(), source.getDevice().getName(), source.getPointName(),
                        point == null ? null : QueryValues.round2(point.value()), point == null ? null : point.time()));
            }
            if (!groupComplete) complete = false;
            if (groupComplete) total += sum;
            groups.add(new PowerDistributionGroupValueResponse(
                    group.getName(), group.getColor(), groupComplete ? QueryValues.round2(sum) : null,
                    null, groupComplete, List.copyOf(rows)));
        }

        BigDecimal totalPowerW = complete ? QueryValues.round2(total) : null;
        List<PowerDistributionGroupValueResponse> withRatios = new ArrayList<>();
        for (PowerDistributionGroupValueResponse group : groups) {
            withRatios.add(new PowerDistributionGroupValueResponse(
                    group.name(), group.color(), group.powerW(),
                    totalPowerW == null || total == 0 ? null
                            : QueryValues.round2(group.powerW().doubleValue() / total * 100),
                    group.complete(), group.sources()));
        }
        List<Instant> collectedTimes = sources.stream()
                .map(source -> latestBySource.get(key(source.getDevice().getId(), source.getPointName())))
                .map(point -> point == null ? null : point.time())
                .toList();
        WidgetDataStatusResponse dataStatus = widgetDataStatusResolver
                .resolve(collectedTimes, widget.getDataFreshnessMinutes());
        return new PowerDistributionWidgetResponse(widget.getId(), widget.getName(), totalPowerW, complete, withRatios, dataStatus);
    }

    private static String key(int deviceId, String pointName) {
        return deviceId + "\u0000" + pointName;
    }
}

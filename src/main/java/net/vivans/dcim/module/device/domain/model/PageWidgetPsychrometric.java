package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Entity
@Table(name = "page_widget_psychrometric")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetPsychrometric {

    @Id
    private Integer widgetId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id")
    private PageWidget widget;

    @OneToMany(mappedBy = "psychrometric", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final Set<PageWidgetPsychrometricSource> sources = new LinkedHashSet<>();

    private PageWidgetPsychrometric(PageWidget widget, List<SourceDefinition> sourceDefinitions) {
        this.widget = widget;
        replaceSources(sourceDefinitions);
    }

    public static PageWidgetPsychrometric create(PageWidget widget, List<SourceDefinition> sources) {
        return new PageWidgetPsychrometric(widget, sources);
    }

    public void update(List<SourceDefinition> sourceDefinitions) {
        replaceSources(sourceDefinitions);
    }

    private void replaceSources(List<SourceDefinition> sourceDefinitions) {
        if (sourceDefinitions == null) {
            throw new IllegalArgumentException("psychrometric sources are required");
        }
        boolean hasTemperature = false;
        boolean hasHumidity = false;
        Set<String> unique = new HashSet<>();
        for (SourceDefinition source : sourceDefinitions) {
            if (source.device() == null || source.device().getId() == null) {
                throw new IllegalArgumentException("psychrometric source device is required");
            }
            if (source.role() == null) {
                throw new IllegalArgumentException("psychrometric source role is required");
            }
            String pointName = source.pointName() == null ? "" : source.pointName().trim();
            if (pointName.isEmpty()) {
                throw new IllegalArgumentException("psychrometric source pointName is required");
            }
            String key = source.device().getId() + "|" + pointName;
            if (!unique.add(key)) {
                throw new IllegalArgumentException("psychrometric source cannot be assigned more than once: " + key);
            }
            hasTemperature |= source.role() == PageWidgetPsychrometricSourceRole.temperature;
            hasHumidity |= source.role() == PageWidgetPsychrometricSourceRole.humidity;
        }
        if (!hasTemperature || !hasHumidity) {
            throw new IllegalArgumentException("psychrometric requires temperature and humidity sources");
        }
        Map<String, PageWidgetPsychrometricSource> existingByKey = new HashMap<>();
        for (PageWidgetPsychrometricSource source : sources) {
            existingByKey.put(sourceKey(source.getDevice().getId(), source.getPointName()), source);
        }

        Set<String> requestedKeys = new HashSet<>();
        for (SourceDefinition source : sourceDefinitions) {
            String key = sourceKey(source.device().getId(), source.pointName());
            requestedKeys.add(key);
            PageWidgetPsychrometricSource existing = existingByKey.get(key);
            if (existing == null) {
                sources.add(PageWidgetPsychrometricSource.create(this, source.device(), source.role(), source.pointName()));
            } else {
                existing.update(source.role());
            }
        }
        sources.removeIf(source -> !requestedKeys.contains(sourceKey(source.getDevice().getId(), source.getPointName())));
    }

    private static String sourceKey(Integer deviceId, String pointName) {
        return deviceId + "|" + pointName.trim();
    }

    public record SourceDefinition(Device device, PageWidgetPsychrometricSourceRole role, String pointName) {
    }
}

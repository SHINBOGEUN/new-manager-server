package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "page_widget_power_distribution_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetPowerDistributionGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidgetPowerDistribution powerDistribution;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 16)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final Set<PageWidgetPowerDistributionSource> sources = new LinkedHashSet<>();

    private PageWidgetPowerDistributionGroup(
            PageWidgetPowerDistribution powerDistribution,
            String name,
            String color,
            int sortOrder,
            List<PageWidgetPowerDistribution.SourceDefinition> sourceDefinitions
    ) {
        if (powerDistribution == null) throw new IllegalArgumentException("powerDistribution is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("group name is required");
        if (sourceDefinitions == null || sourceDefinitions.isEmpty()) {
            throw new IllegalArgumentException("power distribution group sources are required");
        }
        this.powerDistribution = powerDistribution;
        this.name = name.trim();
        this.color = normalizeColor(color);
        this.sortOrder = sortOrder;
        Set<String> uniqueSources = new LinkedHashSet<>();
        for (PageWidgetPowerDistribution.SourceDefinition source : sourceDefinitions) {
            if (source == null || source.device() == null || source.device().getId() == null
                    || source.pointName() == null || source.pointName().isBlank()) {
                throw new IllegalArgumentException("power distribution source is invalid");
            }
            String pointName = source.pointName().trim();
            if (uniqueSources.add(source.device().getId() + "\u0000" + pointName)) {
                sources.add(PageWidgetPowerDistributionSource.create(this, source.device(), pointName));
            }
        }
        if (sources.isEmpty()) throw new IllegalArgumentException("power distribution group sources are required");
    }

    static PageWidgetPowerDistributionGroup create(
            PageWidgetPowerDistribution powerDistribution,
            String name,
            String color,
            int sortOrder,
            List<PageWidgetPowerDistribution.SourceDefinition> sourceDefinitions
    ) {
        return new PageWidgetPowerDistributionGroup(powerDistribution, name, color, sortOrder, sourceDefinitions);
    }

    private static String normalizeColor(String color) {
        if (color == null || color.isBlank()) return null;
        String normalized = color.trim();
        if (!normalized.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("power distribution color must be a hex color (#RRGGBB)");
        }
        return normalized.toUpperCase();
    }
}

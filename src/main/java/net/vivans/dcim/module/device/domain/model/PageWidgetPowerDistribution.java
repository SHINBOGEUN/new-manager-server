package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "page_widget_power_distribution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetPowerDistribution extends BaseEntity {

    @Id
    @Column(name = "widget_id")
    private Integer widgetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "widget_id")
    private PageWidget widget;

    @OneToMany(mappedBy = "powerDistribution", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private final Set<PageWidgetPowerDistributionGroup> groups = new LinkedHashSet<>();

    private PageWidgetPowerDistribution(PageWidget widget, List<GroupDefinition> definitions) {
        if (widget == null) throw new IllegalArgumentException("widget is required");
        this.widget = widget;
        replaceGroups(definitions);
    }

    static PageWidgetPowerDistribution create(PageWidget widget, List<GroupDefinition> definitions) {
        return new PageWidgetPowerDistribution(widget, definitions);
    }

    void update(List<GroupDefinition> definitions) {
        replaceGroups(definitions);
    }

    private void replaceGroups(List<GroupDefinition> definitions) {
        if (definitions == null || definitions.size() < 2) {
            throw new IllegalArgumentException("power distribution requires at least two groups");
        }
        if (definitions.size() > 12) {
            throw new IllegalArgumentException("power distribution supports at most 12 groups");
        }
        Set<String> names = new LinkedHashSet<>();
        groups.clear();
        for (int index = 0; index < definitions.size(); index++) {
            GroupDefinition definition = definitions.get(index);
            if (definition == null || definition.name() == null || definition.name().isBlank()) {
                throw new IllegalArgumentException("power distribution group name is required");
            }
            String name = definition.name().trim();
            if (!names.add(name.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("power distribution group names must be unique");
            }
            groups.add(PageWidgetPowerDistributionGroup.create(
                    this, name, definition.color(), index + 1, definition.sources()));
        }
    }

    public record GroupDefinition(String name, String color, List<SourceDefinition> sources) {
    }

    public record SourceDefinition(Device device, String pointName) {
    }
}

package net.vivans.dcim.module.pue.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.*;

@Entity
@Table(name = "pue_definition", uniqueConstraints = @UniqueConstraint(name = "uk_pue_definition_name", columnNames = "name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PueDefinition extends BaseEntity {
    private static final String DEFAULT_CRON = "0 */5 * * * *";
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "calculation_cron", nullable = false, length = 64)
    private String calculationCron;
    @Column(name = "collection_enabled", nullable = false)
    private boolean collectionEnabled;
    @Column(name = "config_version", nullable = false)
    private int configVersion;
    @Column(name = "collector_job_id", length = 100)
    private String collectorJobId;
    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final Set<PueDefinitionSource> sources = new LinkedHashSet<>();

    private PueDefinition(String name, String calculationCron, boolean collectionEnabled, List<SourceDefinition> sourceDefinitions) {
        this.name = requireName(name);
        this.calculationCron = normalizeCron(calculationCron);
        this.collectionEnabled = collectionEnabled;
        this.configVersion = 1;
        replaceSources(sourceDefinitions);
    }
    public static PueDefinition create(String name, String calculationCron, boolean collectionEnabled, List<SourceDefinition> sources) {
        return new PueDefinition(name, calculationCron, collectionEnabled, sources);
    }
    public void update(String name, String calculationCron, List<SourceDefinition> sourceDefinitions) {
        this.name = requireName(name);
        this.calculationCron = normalizeCron(calculationCron);
        replaceSources(sourceDefinitions);
        this.configVersion++;
    }
    public void setCollectionEnabled(boolean collectionEnabled) { this.collectionEnabled = collectionEnabled; }
    public void updateCollectorJobId(String collectorJobId) { this.collectorJobId = collectorJobId; }
    private void replaceSources(List<SourceDefinition> sourceDefinitions) {
        if (sourceDefinitions == null) throw new IllegalArgumentException("PUE sources are required");
        Set<Integer> ids = new HashSet<>(); boolean total = false, cooler = false;
        for (SourceDefinition source : sourceDefinitions) {
            if (!ids.add(source.device().getId())) throw new IllegalArgumentException("PUE device cannot be assigned more than once: " + source.device().getId());
            total |= source.role() == PueDefinitionSourceRole.total;
            cooler |= source.role() == PueDefinitionSourceRole.cooler;
        }
        if (!total || !cooler) throw new IllegalArgumentException("PUE requires total and cooler sources");
        sources.clear();
        for (SourceDefinition source : sourceDefinitions) sources.add(PueDefinitionSource.create(this, source.device(), source.role(), source.pointName()));
    }
    private static String requireName(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("name is required"); return value.trim(); }
    private static String normalizeCron(String value) { return value == null || value.isBlank() ? DEFAULT_CRON : value.trim(); }
    public record SourceDefinition(Device device, PueDefinitionSourceRole role, String pointName) {}
}

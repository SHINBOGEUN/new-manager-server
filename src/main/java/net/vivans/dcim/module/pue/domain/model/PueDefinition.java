package net.vivans.dcim.module.pue.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;
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
    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final Set<PueDefinitionDeviceGroup> deviceGroups = new LinkedHashSet<>();

    private PueDefinition(String name, String calculationCron, boolean collectionEnabled,
                          List<SourceDefinition> sourceDefinitions,
                          List<DeviceGroupDefinition> deviceGroupDefinitions) {
        this.name = requireName(name);
        this.calculationCron = normalizeCron(calculationCron);
        this.collectionEnabled = collectionEnabled;
        this.configVersion = 1;
        replaceSources(sourceDefinitions);
        replaceDeviceGroups(deviceGroupDefinitions);
    }
    public static PueDefinition create(String name, String calculationCron, boolean collectionEnabled, List<SourceDefinition> sources) {
        return new PueDefinition(name, calculationCron, collectionEnabled, sources, List.of());
    }
    public static PueDefinition create(String name, String calculationCron, boolean collectionEnabled,
                                       List<SourceDefinition> sources,
                                       List<DeviceGroupDefinition> deviceGroups) {
        return new PueDefinition(name, calculationCron, collectionEnabled, sources, deviceGroups);
    }
    public void update(String name, String calculationCron, List<SourceDefinition> sourceDefinitions) {
        this.name = requireName(name);
        this.calculationCron = normalizeCron(calculationCron);
        replaceSources(sourceDefinitions);
        replaceDeviceGroups(List.of());
        this.configVersion++;
    }
    public void update(String name, String calculationCron, List<SourceDefinition> sourceDefinitions,
                       List<DeviceGroupDefinition> deviceGroupDefinitions) {
        this.name = requireName(name);
        this.calculationCron = normalizeCron(calculationCron);
        replaceSources(sourceDefinitions);
        replaceDeviceGroups(deviceGroupDefinitions);
        this.configVersion++;
    }
    public void setCollectionEnabled(boolean collectionEnabled) { this.collectionEnabled = collectionEnabled; }
    public void updateCollectorJobId(String collectorJobId) { this.collectorJobId = collectorJobId; }
    public void refreshResolvedSources() {
        validateResolvedSources();
        this.configVersion++;
    }
    private void replaceSources(List<SourceDefinition> sourceDefinitions) {
        if (sourceDefinitions == null) throw new IllegalArgumentException("PUE sources are required");
        Set<Integer> ids = new HashSet<>();
        for (SourceDefinition source : sourceDefinitions) {
            if (!ids.add(source.device().getId())) throw new IllegalArgumentException("PUE device cannot be assigned more than once: " + source.device().getId());
        }
        Map<Integer, PueDefinitionSource> existingByDeviceId = new HashMap<>();
        for (PueDefinitionSource source : sources) {
            existingByDeviceId.put(source.getDevice().getId(), source);
        }

        Set<Integer> requestedIds = new HashSet<>();
        for (SourceDefinition source : sourceDefinitions) {
            Integer deviceId = source.device().getId();
            requestedIds.add(deviceId);
            PueDefinitionSource existing = existingByDeviceId.get(deviceId);
            if (existing == null) {
                sources.add(PueDefinitionSource.create(this, source.device(), source.role(), source.pointName()));
            } else {
                existing.update(source.role(), source.pointName());
            }
        }
        sources.removeIf(source -> !requestedIds.contains(source.getDevice().getId()));
    }
    private void replaceDeviceGroups(List<DeviceGroupDefinition> definitions) {
        if (definitions == null) throw new IllegalArgumentException("PUE device groups are required");
        Map<Integer, PueDefinitionDeviceGroup> existingByGroupId = new HashMap<>();
        for (PueDefinitionDeviceGroup item : deviceGroups) {
            existingByGroupId.put(item.getDeviceGroup().getId(), item);
        }
        Set<Integer> requestedIds = new HashSet<>();
        for (DeviceGroupDefinition item : definitions) {
            Integer groupId = item.deviceGroup().getId();
            if (!requestedIds.add(groupId)) {
                throw new IllegalArgumentException("PUE device group cannot be assigned more than once: " + groupId);
            }
            PueDefinitionDeviceGroup existing = existingByGroupId.get(groupId);
            if (existing == null) deviceGroups.add(PueDefinitionDeviceGroup.create(this, item.deviceGroup(), item.role(), item.pointName()));
            else existing.update(item.role(), item.pointName());
        }
        deviceGroups.removeIf(item -> !requestedIds.contains(item.getDeviceGroup().getId()));
        validateResolvedSources();
    }
    public List<SourceDefinition> resolvedSources() {
        Map<Integer, SourceDefinition> result = new LinkedHashMap<>();
        for (PueDefinitionSource source : sources) {
            addResolvedSource(result, new SourceDefinition(source.getDevice(), source.getRole(), source.getPointName()));
        }
        for (PueDefinitionDeviceGroup group : deviceGroups) {
            for (Device device : group.getDeviceGroup().getDevices()) {
                addResolvedSource(result, new SourceDefinition(device, group.getRole(), group.getPointName()));
            }
        }
        return List.copyOf(result.values());
    }
    private void validateResolvedSources() {
        List<SourceDefinition> resolved = resolvedSources();
        boolean total = resolved.stream().anyMatch(source -> source.role() == PueDefinitionSourceRole.total);
        boolean cooler = resolved.stream().anyMatch(source -> source.role() == PueDefinitionSourceRole.cooler);
        if (!total || !cooler) throw new IllegalArgumentException("PUE requires total and cooler sources");
    }
    private static void addResolvedSource(Map<Integer, SourceDefinition> sources, SourceDefinition source) {
        SourceDefinition existing = sources.putIfAbsent(source.device().getId(), source);
        if (existing == null) return;
        if (existing.role() != source.role()) {
            throw new IllegalArgumentException("PUE device cannot belong to both total and cooler: " + source.device().getId());
        }
        if (!existing.pointName().equalsIgnoreCase(source.pointName())) {
            throw new IllegalArgumentException("PUE device has conflicting POWER points: " + source.device().getId());
        }
    }
    private static String requireName(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("name is required"); return value.trim(); }
    private static String normalizeCron(String value) { return value == null || value.isBlank() ? DEFAULT_CRON : value.trim(); }
    public record SourceDefinition(Device device, PueDefinitionSourceRole role, String pointName) {}
    public record DeviceGroupDefinition(DeviceGroup deviceGroup, PueDefinitionSourceRole role, String pointName) {}
}

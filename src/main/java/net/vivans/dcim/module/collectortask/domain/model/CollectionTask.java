package net.vivans.dcim.module.collectortask.domain.model;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "collection_task",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_collection_task_model_script",
                columnNames = {"model_id", "script_type_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionTask extends BaseEntity {

    public static final String SCRIPT_TYPE_GROUP_KEY = "PROTOCOL_TYPE";
    public static final String DEFAULT_GROUP_NAME = "기본 그룹";
    public static final String DEFAULT_GROUP_CRON = "0 */1 * * * *";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private DeviceModel deviceModel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "script_type_id", nullable = false)
    private CommonCode scriptType;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<CollectionTaskGroup> groups = new ArrayList<>();

    private CollectionTask(String name, DeviceModel deviceModel, CommonCode scriptType, boolean active) {
        validateName(name);
        validateDeviceModel(deviceModel);
        validateScriptType(scriptType);
        validateModelSupportsScriptType(deviceModel, scriptType);
        this.name = name;
        this.deviceModel = deviceModel;
        this.scriptType = scriptType;
        this.active = active;
    }

    public static CollectionTask create(String name, DeviceModel deviceModel, CommonCode scriptType, boolean active) {
        return new CollectionTask(name, deviceModel, scriptType, active);
    }

    public void update(String name, boolean active) {
        validateName(name);
        this.name = name;
        this.active = active;
    }

    public void toggleActive() {
        this.active = !this.active;
    }

    public void addGroup(CollectionTaskGroup group) {
        groups.add(group);
    }

    public CollectionTaskGroup ensureDefaultGroup() {
        CollectionTaskGroup existing = defaultGroup();
        if (existing != null) {
            return existing;
        }
        return CollectionTaskGroup.create(this, DEFAULT_GROUP_NAME, DEFAULT_GROUP_CRON, true);
    }

    public CollectionTaskGroup defaultGroup() {
        for (CollectionTaskGroup group : groups) {
            if (group.isActive()) {
                return group;
            }
        }
        return groups.isEmpty() ? null : groups.get(0);
    }

    public boolean hasCronExpression(String cronExpression, Integer excludeGroupId) {
        for (CollectionTaskGroup group : groups) {
            if (excludeGroupId != null && excludeGroupId.equals(group.getId())) {
                continue;
            }
            if (group.getCronExpression().equals(cronExpression)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsDevice(Integer deviceId, Integer excludeGroupId) {
        for (CollectionTaskGroup group : groups) {
            if (excludeGroupId != null && excludeGroupId.equals(group.getId())) {
                continue;
            }
            if (group.containsDevice(deviceId)) {
                return true;
            }
        }
        return false;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private static void validateDeviceModel(DeviceModel deviceModel) {
        if (deviceModel == null) {
            throw new IllegalArgumentException("deviceModel is required");
        }
    }

    private static void validateScriptType(CommonCode scriptType) {
        if (scriptType == null) {
            throw new IllegalArgumentException("scriptType is required");
        }
        if (!SCRIPT_TYPE_GROUP_KEY.equals(scriptType.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("scriptType must belong to PROTOCOL_TYPE group");
        }
    }

    private static void validateModelSupportsScriptType(DeviceModel deviceModel, CommonCode scriptType) {
        for (DeviceModelProtocol protocol : deviceModel.getProtocols()) {
            if (scriptType.getId().equals(protocol.getProtocolType().getId())) {
                return;
            }
        }
        throw new IllegalArgumentException("scriptType is not supported by device model");
    }
}

package net.vivans.dcim.module.collectortask.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "collection_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionTask extends BaseEntity {

    public static final String SCRIPT_TYPE_GROUP_KEY = "PROTOCOL_TYPE";

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String cronExpression;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "script_type_id", nullable = false)
    private CommonCode scriptType;

    @Column(columnDefinition = "LONGTEXT")
    private String generatedScript;

    @Column(length = 100)
    private String collectorTaskId;

    @Column(nullable = false)
    private boolean active;

    private CollectionTask(
            String id,
            String name,
            String cronExpression,
            CommonCode scriptType,
            String generatedScript,
            String collectorTaskId,
            boolean active
    ) {
        validateName(name);
        validateCronExpression(cronExpression);
        validateScriptType(scriptType);
        this.id = id;
        this.name = name;
        this.cronExpression = cronExpression;
        this.scriptType = scriptType;
        this.generatedScript = generatedScript;
        this.collectorTaskId = collectorTaskId;
        this.active = active;
    }

    public static CollectionTask create(String name, String cronExpression, CommonCode scriptType, boolean active) {
        return new CollectionTask(
                UUID.randomUUID().toString(),
                name,
                cronExpression,
                scriptType,
                null,
                null,
                active
        );
    }

    public void update(String name, String cronExpression, CommonCode scriptType, boolean active) {
        validateName(name);
        validateCronExpression(cronExpression);
        validateScriptType(scriptType);
        this.name = name;
        this.cronExpression = cronExpression;
        this.scriptType = scriptType;
        this.active = active;
    }

    public void toggleActive() {
        this.active = !this.active;
    }

    public void updateGeneratedScript(String generatedScript) {
        this.generatedScript = blankToNull(generatedScript);
    }

    public void updateCollectorTaskId(String collectorTaskId) {
        this.collectorTaskId = blankToNull(collectorTaskId);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private static void validateCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("cronExpression is required");
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

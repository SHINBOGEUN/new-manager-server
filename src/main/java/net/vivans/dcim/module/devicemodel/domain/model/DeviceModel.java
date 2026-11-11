package net.vivans.dcim.module.devicemodel.domain.model;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "device_model")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceModel extends BaseEntity {

    private static final String MODEL_TYPE_GROUP_KEY = "MODEL_TYPE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String manufacturer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_type_id", nullable = false)
    private CommonCode deviceType;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_notation_code_id")
    private CommonCode phaseNotation;

    @OneToMany(mappedBy = "deviceModel", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DeviceModelProtocol> protocols = new ArrayList<>();

    private DeviceModel(String name, String manufacturer, CommonCode deviceType, String description) {
        validateName(name);
        validateManufacturer(manufacturer);
        validateDeviceType(deviceType);
        this.name = name;
        this.manufacturer = manufacturer;
        this.deviceType = deviceType;
        this.description = description;
    }

    public static DeviceModel create(String name, String manufacturer, CommonCode deviceType, String description) {
        return new DeviceModel(name, manufacturer, deviceType, description);
    }

    public void update(String name, String manufacturer, CommonCode deviceType, String description) {
        validateName(name);
        validateManufacturer(manufacturer);
        validateDeviceType(deviceType);
        this.name = name;
        this.manufacturer = manufacturer;
        this.deviceType = deviceType;
        this.description = description;
    }

    public void setPhaseNotation(CommonCode phaseNotation) {
        if (phaseNotation != null && !"PHASE_NOTATION".equals(phaseNotation.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("phaseNotation must belong to PHASE_NOTATION group");
        }
        if (phaseNotation != null) validatePhaseNotationFormat(phaseNotation.getName());
        this.phaseNotation = phaseNotation;
    }

    public String phaseLabel(String phaseCode) {
        String[] labels = new String[]{"L1", "L2", "L3", "N"};
        if (phaseNotation != null) {
            try {
                labels = parsePhaseNotation(phaseNotation.getName());
            } catch (IllegalArgumentException ignored) {
                // Defensive fallback for legacy or externally corrupted common-code data.
            }
        }
        return switch (phaseCode == null ? "" : phaseCode) {
            case "P1" -> labels[0];
            case "P2" -> labels[1];
            case "P3" -> labels[2];
            case "N" -> labels[3];
            default -> null;
        };
    }

    public java.util.Map<String, String> phaseLabels() {
        return java.util.Map.of("P1", phaseLabel("P1"), "P2", phaseLabel("P2"),
                "P3", phaseLabel("P3"), "N", phaseLabel("N"));
    }

    public static void validatePhaseNotationFormat(String notation) {
        parsePhaseNotation(notation);
    }

    private static String[] parsePhaseNotation(String notation) {
        String[] labels = notation == null ? new String[0] : notation.split("/", -1);
        if (labels.length != 4 || java.util.Arrays.stream(labels).anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("상 표기 이름은 '/'로 구분한 4개 항목이어야 합니다");
        }
        return labels;
    }

    public void replaceProtocols(List<DeviceModelProtocol> newProtocols) {
        protocols.clear();
        for (DeviceModelProtocol protocol : newProtocols) {
            addProtocol(protocol);
        }
    }

    void addProtocol(DeviceModelProtocol protocol) {
        protocols.add(protocol);
    }

    public List<DeviceModelProtocol> getSortedProtocols() {
        List<DeviceModelProtocol> sorted = new ArrayList<>(protocols);
        sorted.sort((left, right) -> {
            Integer leftId = left.getId();
            Integer rightId = right.getId();
            if (leftId == null && rightId == null) {
                return 0;
            }
            if (leftId == null) {
                return 1;
            }
            if (rightId == null) {
                return -1;
            }
            return leftId.compareTo(rightId);
        });
        return sorted;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private static void validateManufacturer(String manufacturer) {
        if (manufacturer == null || manufacturer.isBlank()) {
            throw new IllegalArgumentException("manufacturer is required");
        }
    }

    private static void validateDeviceType(CommonCode deviceType) {
        if (deviceType == null) {
            throw new IllegalArgumentException("deviceType is required");
        }
        if (!MODEL_TYPE_GROUP_KEY.equals(deviceType.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("deviceType must belong to MODEL_TYPE group");
        }
    }
}

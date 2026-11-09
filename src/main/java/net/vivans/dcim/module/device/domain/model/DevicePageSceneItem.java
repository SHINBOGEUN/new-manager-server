package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "device_page_scene_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_device_page_scene_item_page_device_type",
                columnNames = {"page_code_id", "device_id", "item_type"}),
        @UniqueConstraint(name = "uk_device_page_scene_item_page_location_type",
                columnNames = {"page_code_id", "location_node_code", "item_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DevicePageSceneItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_code_id", nullable = false)
    private CommonCode pageCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id")
    private DevicePageSceneItem parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_node_code")
    private LocationNode locationNode;

    @Column(name = "item_type", nullable = false, length = 16)
    private String itemType;

    @Column(name = "point_name", length = 100)
    private String pointName;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "model_key", nullable = false, length = 32)
    private String modelKey;

    @Column(name = "position_x", nullable = false)
    private double x;

    @Column(name = "position_y", nullable = false)
    private double y;

    @Column(name = "position_z", nullable = false)
    private double z;

    @Column(name = "rotation_y", nullable = false)
    private double rotationY;

    @Column(name = "size_x", nullable = false)
    private double sizeX;

    @Column(name = "size_y", nullable = false)
    private double sizeY;

    @Column(name = "size_z", nullable = false)
    private double sizeZ;

    @Column(name = "group_key", length = 36)
    private String groupKey;

    @Column(name = "group_name", length = 80)
    private String groupName;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Column(name = "rack_color", length = 7)
    private String rackColor;

    @jakarta.persistence.Lob
    @Column(name = "panel_photo", columnDefinition = "LONGTEXT")
    private String panelPhoto;

    @Column(name = "panel_layout_initialized", nullable = false)
    private boolean panelLayoutInitialized;

    public void setPanelLayoutInitialized(boolean value) {
        if (value && !"SR_INROW_PANEL".equals(modelKey)) throw new IllegalArgumentException("Panel layout requires a panel");
        this.panelLayoutInitialized = value;
    }

    public void setPanelPhoto(String value) {
        if (value == null || value.isBlank()) { this.panelPhoto = null; return; }
        if (!"SR_INROW_PANEL".equals(modelKey) || !"EQUIPMENT".equals(itemType)
                || value.length() > 1_500_000 || !value.startsWith("data:image/jpeg;base64,")) {
            throw new IllegalArgumentException("Invalid panel photo");
        }
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(value.substring("data:image/jpeg;base64,".length()));
            if (bytes.length < 4 || (bytes[0] & 255) != 255 || (bytes[1] & 255) != 216 || (bytes[bytes.length-2] & 255) != 255 || (bytes[bytes.length-1] & 255) != 217)
                throw new IllegalArgumentException("Invalid JPEG image");
        } catch (IllegalArgumentException error) { throw new IllegalArgumentException("Invalid panel photo", error); }
        this.panelPhoto = value;
    }

    public void setRackColor(String color) {
        if (color == null || color.isBlank()) { this.rackColor = null; return; }
        if (!("RACK".equals(itemType) || (modelKey != null && modelKey.matches("SR_.*RACK") && !"SR_PDU_RACK".equals(modelKey)))
                || !color.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid rack color");
        this.rackColor = color.toUpperCase(java.util.Locale.ROOT);
    }

    public void setLocked(boolean locked) { this.locked = locked; }

    public void setGroupName(String groupName) {
        if (groupName == null || groupName.isBlank()) { this.groupName = null; return; }
        String name = groupName.trim();
        if (groupKey == null || name.length() > 80) throw new IllegalArgumentException("Invalid scene group name");
        this.groupName = name;
    }

    public void setGroupKey(String groupKey) {
        if (groupKey != null && !groupKey.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Invalid scene group key");
        if (groupKey != null && (parent != null || !("EQUIPMENT".equals(itemType) || "RACK".equals(itemType))))
            throw new IllegalArgumentException("Only independent equipment can be grouped");
        this.groupKey = groupKey;
        if (groupKey == null) this.groupName = null;
    }

    private DevicePageSceneItem(CommonCode pageCode, DevicePageSceneItem parent, Device device, LocationNode locationNode,
                                String itemType, String pointName, String label, String modelKey,
                                double x, double y, double z, double rotationY, double sizeX, double sizeY, double sizeZ) {
        this.pageCode = pageCode;
        update(parent, device, locationNode, itemType, pointName, label, modelKey, x, y, z, rotationY, sizeX, sizeY, sizeZ);
    }

    public static DevicePageSceneItem create(CommonCode pageCode, DevicePageSceneItem parent, Device device, LocationNode locationNode,
                                               String itemType, String pointName, String label, String modelKey,
                                               double x, double y, double z, double rotationY, double sizeX, double sizeY, double sizeZ) {
        if (pageCode == null) throw new IllegalArgumentException("pageCode is required");
        return new DevicePageSceneItem(pageCode, parent, device, locationNode, itemType, pointName, label,
                modelKey, x, y, z, rotationY, sizeX, sizeY, sizeZ);
    }

    public void update(DevicePageSceneItem parent, Device device, LocationNode locationNode, String itemType, String pointName,
                       String label, String modelKey, double x, double y, double z, double rotationY,
                       double sizeX, double sizeZ) {
        update(parent, device, locationNode, itemType, pointName, label, modelKey, x, y, z, rotationY,
                sizeX, isEquipmentFrame(modelKey) ? sizeX : 4, sizeZ);
    }

    public void update(DevicePageSceneItem parent, Device device, LocationNode locationNode, String itemType, String pointName,
                       String label, String modelKey, double x, double y, double z, double rotationY,
                       double sizeX, double sizeY, double sizeZ) {
        if (itemType == null || !java.util.Set.of("RACK", "EQUIPMENT", "SENSOR", "DEVICE", "PANEL_COMPONENT").contains(itemType)) {
            throw new IllegalArgumentException("Unsupported itemType");
        }
        if (label == null || label.isBlank() || label.length() > 120) throw new IllegalArgumentException("Invalid label");
        if (modelKey == null || !java.util.Set.of("RACK", "PDU", "CRAC", "UPS", "SENSOR", "GENERIC", "CHILLER", "SWITCH", "SERVER",
                "MACHINE", "CONVEYOR", "TANK", "TABLE", "WALL", "WALL_CONCRETE", "WALL_GLASS", "WALL_GLASS_CLEAR", "WALL_PANEL",
                "DOOR", "DOOR_DOUBLE", "DOOR_SLIDING", "WINDOW", "WINDOW_DOUBLE", "CEILING", "CEILING_CLEAR", "FLOOR", "LIGHT", "PIPE", "STAIR",
                "SR_RACK", "SR_DOUBLE_RACK", "SR_RDC", "SR_INROW", "SR_INROW_MOLD", "SR_WALL_COIL", "SR_DLC_RACK", "SR_INROW_RACK",
                "SR_OCP_RACK", "SR_RDC_RACK", "SR_ORV3_RACK", "SR_IMMERSION", "SR_HAC_RACK", "SR_DLC_CDU", "SR_DLC_SERVER", "SR_INROW_PANEL", "SR_PDU_RACK", "SR_PDU_VERTICAL", "SR_GPU_SERVER", "SR_DRAGINO_TH", "SR_DRAGINO_DOOR", "SR_ACCURA_2350_1P", "SR_ACCURA_2350_3P", "SR_PANEL_BREAKER", "SR_PANEL_BRANCH", "SR_PANEL_MAIN", "SR_PANEL_METER", "SR_PANEL_BUSBAR", "SR_PANEL_WIRE").contains(modelKey)
                && !modelKey.matches("USR_[A-Z0-9]{24}")) {
            throw new IllegalArgumentException("Unsupported modelKey");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Double.isFinite(rotationY)) {
            throw new IllegalArgumentException("Scene coordinates must be finite");
        }
        boolean architecturalSpan = modelKey.startsWith("WALL") || modelKey.startsWith("CEILING");
        boolean buildingPart = architecturalSpan || modelKey.startsWith("DOOR") || modelKey.startsWith("WINDOW")
                || java.util.Set.of("FLOOR", "LIGHT", "PIPE", "STAIR").contains(modelKey);
        boolean scalableEquipment = ("RACK".equals(itemType) || "EQUIPMENT".equals(itemType))
                && !modelKey.startsWith("WALL") && !modelKey.startsWith("DOOR") && !modelKey.startsWith("WINDOW")
                && !java.util.Set.of("FLOOR", "CEILING", "CEILING_CLEAR", "LIGHT", "PIPE", "STAIR").contains(modelKey);
        boolean equipmentFrame = isEquipmentFrame(modelKey);
        boolean panelComponent = "PANEL_COMPONENT".equals(itemType);
        if (!Double.isFinite(sizeX) || !Double.isFinite(sizeY) || !Double.isFinite(sizeZ)
                || sizeY < (panelComponent ? .5 : scalableEquipment ? 2 : 4) || sizeY > (panelComponent ? 16 : scalableEquipment ? 64 : 4)
                || sizeX < .5 || sizeX > (panelComponent ? 16 : equipmentFrame ? 32 : architecturalSpan ? 24 : scalableEquipment ? (modelKey.startsWith("SR_") || modelKey.startsWith("USR_")) ? 32 : 16 : buildingPart ? 5 : 4)
                || sizeZ < .5 || sizeZ > (panelComponent ? 16 : scalableEquipment ? 96 : modelKey.startsWith("CEILING") ? 24 : buildingPart ? 5 : 4)) {
            throw new IllegalArgumentException("Invalid scene item size");
        }
        if ("SENSOR".equals(itemType) && (parent == null || device == null || pointName == null || pointName.isBlank())) {
            throw new IllegalArgumentException("A sensor marker requires a rack, device, and point");
        }
        if ("DEVICE".equals(itemType) && (parent == null || device == null || pointName != null)) {
            throw new IllegalArgumentException("A device attachment requires a host and device, but no single point");
        }
        if (locationNode != null && (!("RACK".equals(itemType) || ("EQUIPMENT".equals(itemType) && modelKey.matches("SR_.*RACK") && !"SR_PDU_RACK".equals(modelKey)))
                || !"RACK".equals(locationNode.getLocationType().getCode()))) {
            throw new IllegalArgumentException("A location node can only identify a RACK scene item");
        }
        boolean wallOpening = "EQUIPMENT".equals(itemType)
                && (modelKey.startsWith("DOOR") || modelKey.startsWith("WINDOW"));
        if (wallOpening && (parent == null || !"EQUIPMENT".equals(parent.getItemType())
                || parent.getModelKey() == null || !parent.getModelKey().startsWith("WALL"))) {
            throw new IllegalArgumentException("A door or window must be attached to a wall");
        }
        if ("PANEL_COMPONENT".equals(itemType) && (parent == null || !"SR_INROW_PANEL".equals(parent.getModelKey()) || pointName != null)) {
            throw new IllegalArgumentException("A panel component requires a distribution panel host");
        }
        if (!java.util.Set.of("SENSOR", "DEVICE", "PANEL_COMPONENT").contains(itemType) && parent != null && !wallOpening) {
            throw new IllegalArgumentException("Only devices or wall openings can be attached to a parent item");
        }
        this.parent = parent;
        this.device = device;
        this.locationNode = locationNode;
        this.itemType = itemType;
        this.pointName = pointName == null || pointName.isBlank() ? null : pointName.trim();
        this.label = label.trim();
        this.modelKey = modelKey;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotationY = rotationY;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    private static boolean isEquipmentFrame(String modelKey) {
        return "SR_INROW_MOLD".equals(modelKey) || "SR_WALL_COIL".equals(modelKey);
    }
}

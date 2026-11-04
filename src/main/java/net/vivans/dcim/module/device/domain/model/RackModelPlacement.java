package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "rack_model_placement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RackModelPlacement extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "rack_location_code", nullable = false, length = 10) private String rackLocationCode;
    @Column(name = "scene_model_key", nullable = false, length = 32) private String sceneModelKey;
    @Enumerated(EnumType.STRING) @Column(name = "mount_type", nullable = false) private DeviceMountType mountType;
    @Enumerated(EnumType.STRING) @Column(name = "rack_side") private DeviceRackSide rackSide;
    @Column(name = "u_position") private Integer uPosition;
    @Column(name = "u_height") private Integer uHeight;
    @Column(name = "position_x") private Double positionX;
    @Column(name = "position_y") private Double positionY;
    @Column(name = "position_z") private Double positionZ;
    @Column(name = "visual_length_ratio") private Double visualLengthRatio;

    public void setVisualLengthRatio(Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0.1 || value > 1.0)) throw new IllegalArgumentException("PDU 길이는 랙 높이의 10~100%로 설정하세요");
        visualLengthRatio = value;
    }

    public void setSceneModelKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("3D 모델을 선택하세요");
        this.sceneModelKey = key.trim();
    }

    public void setVisualizationPosition(Double x, Double y, Double z) {
        if (mountType != DeviceMountType.RACK_SIDE && mountType != DeviceMountType.RACK_REAR)
            throw new IllegalArgumentException("측면 또는 후면 배치만 위치를 변경할 수 있습니다");
        for (Double value : new Double[]{x, y, z})
            if (value == null || !Double.isFinite(value) || value < 0 || value > 1)
                throw new IllegalArgumentException("위치 좌표는 0~1이어야 합니다");
        positionX = x; positionY = y; positionZ = z;
    }

    public void setUPlacement(int position, int height) {
        if (mountType != DeviceMountType.RACK_U || position < 1 || height < 1) throw new IllegalArgumentException("올바른 U 위치와 크기를 입력하세요");
        uPosition = position; uHeight = height;
    }

    public static RackModelPlacement create(String code, String model, DeviceMountType type, DeviceRackSide side,
            Integer start, Integer height, Double x, Double y, Double z) {
        RackModelPlacement item = new RackModelPlacement();
        item.rackLocationCode = code; item.sceneModelKey = model; item.mountType = type;
        item.rackSide = type == DeviceMountType.RACK_SIDE ? side : type == DeviceMountType.RACK_REAR ? DeviceRackSide.REAR : null;
        item.uPosition = type == DeviceMountType.RACK_U ? start : null;
        item.uHeight = type == DeviceMountType.RACK_U ? height : null;
        item.positionX = x; item.positionY = y; item.positionZ = z;
        return item;
    }
    public Integer lastU() { return uPosition == null || uHeight == null ? null : uPosition + uHeight - 1; }
}

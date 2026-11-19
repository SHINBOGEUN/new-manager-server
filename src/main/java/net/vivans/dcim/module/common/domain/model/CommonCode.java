package net.vivans.dcim.module.common.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Slf4j
@Getter
@Entity
@Table(name = "common_code")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private CodeGroup codeGroup;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "threshold_supported", nullable = false)
    private boolean thresholdSupported;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_unit_id")
    private CommonCode baseUnit;

    @Column(name = "unit_factor", precision = 24, scale = 12)
    private java.math.BigDecimal unitFactor;

    public void setUnitConversion(CommonCode baseUnit, java.math.BigDecimal factor) {
        if (baseUnit == null && factor == null) { this.baseUnit = null; this.unitFactor = null; return; }
        if (!"UNIT".equals(codeGroup.getGroupKey()) || baseUnit == null || factor == null || factor.signum() <= 0
                || !"UNIT".equals(baseUnit.getCodeGroup().getGroupKey()))
            throw new IllegalArgumentException("환산 기준은 UNIT 단위와 양수 배수가 필요합니다");
        if (baseUnit == this) {
            if (factor.compareTo(java.math.BigDecimal.ONE) != 0) throw new IllegalArgumentException("기준 단위의 배수는 1이어야 합니다");
        } else if (baseUnit.getBaseUnit() == null || !java.util.Objects.equals(baseUnit.getBaseUnit().getId(), baseUnit.getId()) || baseUnit.getUnitFactor() == null
                || baseUnit.getUnitFactor().compareTo(java.math.BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("기준 단위는 자기 자신을 기준으로 하는 단위여야 합니다");
        }
        this.baseUnit = baseUnit; this.unitFactor = factor;
    }

    private CommonCode(CodeGroup codeGroup, String code, String name, Integer sortOrder, boolean thresholdSupported) {
        this.codeGroup = codeGroup;
        this.code = code;
        this.name = name;
        this.sortOrder = sortOrder;
        this.thresholdSupported = thresholdSupported;
    }

    private static void validate(CodeGroup codeGroup, String code, String name){
        if (codeGroup == null) {
            throw new IllegalArgumentException("codeGroup is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    public static CommonCode createCommonCode(CodeGroup codeGroup, String code, String name, Integer sortOrder) {
        return createCommonCode(codeGroup, code, name, sortOrder, false);
    }

    public static CommonCode createCommonCode(CodeGroup codeGroup, String code, String name, Integer sortOrder,
                                               boolean thresholdSupported) {
        validate(codeGroup, code, name);
        if (thresholdSupported && !"DATA_POINT_TYPE".equals(codeGroup.getGroupKey())) {
            throw new IllegalArgumentException("임계값 지원은 DATA_POINT_TYPE 코드에서만 설정할 수 있습니다");
        }
        return new CommonCode(codeGroup, code, name, sortOrder, thresholdSupported);
    }

    public void update(CodeGroup codeGroup, String code, String name, Integer sortOrder){
        update(codeGroup, code, name, sortOrder,
                codeGroup != null && "DATA_POINT_TYPE".equals(codeGroup.getGroupKey()) && thresholdSupported);
    }

    public void update(CodeGroup codeGroup, String code, String name, Integer sortOrder, boolean thresholdSupported){
        validate(codeGroup, code, name);
        if (thresholdSupported && !"DATA_POINT_TYPE".equals(codeGroup.getGroupKey())) {
            throw new IllegalArgumentException("임계값 지원은 DATA_POINT_TYPE 코드에서만 설정할 수 있습니다");
        }
        this.codeGroup = codeGroup;
        this.code = code;
        this.name = name;
        this.sortOrder = sortOrder;
        this.thresholdSupported = thresholdSupported;
    }
}

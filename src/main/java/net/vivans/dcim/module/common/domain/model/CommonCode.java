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

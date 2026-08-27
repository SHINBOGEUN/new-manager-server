package net.vivans.dcim.module.location.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "location_node")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationNode extends BaseEntity {

    public static final String LOCATION_TYPE_GROUP_KEY = "LOCATION_TYPE";

    @Id
    @Column(name = "code", length = LocationNodeCodeGenerator.CODE_LENGTH, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code")
    private LocationNode parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_type_id", nullable = false)
    private CommonCode locationType;

    @Column(nullable = false)
    private String name;

    private LocationNode(
            String code,
            LocationNode parent,
            CommonCode locationType,
            String name
    ) {
        this.code = code;
        this.parent = parent;
        this.locationType = locationType;
        this.name = name;
    }

    public static LocationNode createRoot(String code, CommonCode locationType, String name) {
        validateCode(code);
        validateLocationType(locationType);
        validateName(name);
        return new LocationNode(code, null, locationType, name);
    }

    public static LocationNode createChild(
            String code,
            LocationNode parent,
            CommonCode locationType,
            String name
    ) {
        if (parent == null) {
            throw new IllegalArgumentException("parent is required");
        }
        validateCode(code);
        validateLocationType(locationType);
        validateName(name);
        return new LocationNode(code, parent, locationType, name);
    }

    public void update(CommonCode locationType, String name) {
        validateLocationType(locationType);
        validateName(name);
        this.locationType = locationType;
        this.name = name;
    }

    public void updateParent(LocationNode newParent) {
        if (newParent != null && newParent.getCode().equals(this.code)) {
            throw new IllegalArgumentException("cannot set parent to itself");
        }
        if (newParent != null && isAncestorOf(newParent)) {
            throw new IllegalArgumentException("circular reference is not allowed");
        }
        this.parent = newParent;
    }

    private boolean isAncestorOf(LocationNode descendant) {
        LocationNode current = descendant;
        while (current != null) {
            if (current.getCode().equals(this.code)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    public boolean isRoot() {
        return parent == null;
    }

    private static void validateCode(String code) {
        if (code == null || code.length() != LocationNodeCodeGenerator.CODE_LENGTH) {
            throw new IllegalArgumentException("code is invalid");
        }
        for (int i = 0; i < code.length(); i++) {
            if (LocationNodeCodeGenerator.BASE62_ALPHABET.indexOf(code.charAt(i)) < 0) {
                throw new IllegalArgumentException("code is invalid");
            }
        }
    }

    private static void validateLocationType(CommonCode locationType) {
        if (locationType == null) {
            throw new IllegalArgumentException("locationType is required");
        }
        if (!LOCATION_TYPE_GROUP_KEY.equals(locationType.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("locationType must belong to LOCATION_TYPE group");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }
}

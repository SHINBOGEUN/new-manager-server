package net.vivans.dcim.module.location.domain.model;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationNodeTest {

    private static final String ROOT_CODE = "K7mN2pQx9L";
    private static final String CHILD_CODE = "A1b2C3d4E5";

    @Test
    void create_LocationNodeRoot() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode locationType = CommonCode.createCommonCode(codeGroup, "container", "컨테이너", 1);
        LocationNode node = LocationNode.createRoot(ROOT_CODE, locationType, "컨테이너 A");

        assertThat(node.getCode()).isEqualTo(ROOT_CODE);
        assertThat(node.getName()).isEqualTo("컨테이너 A");
        assertThat(node.getLocationType()).isEqualTo(locationType);
        assertThat(node.isRoot()).isTrue();
        assertThat(node.getParent()).isNull();
    }

    @Test
    void create_LocationNodeChild() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode containerType = CommonCode.createCommonCode(codeGroup, "CONTAINER", "컨테이너", 1);
        CommonCode rowType = CommonCode.createCommonCode(codeGroup, "ROW", "열", 2);
        LocationNode parent = LocationNode.createRoot(ROOT_CODE, containerType, "컨테이너 A");

        LocationNode childNode = LocationNode.createChild(CHILD_CODE, parent, rowType, "A 열");

        assertThat(childNode.getCode()).isEqualTo(CHILD_CODE);
        assertThat(childNode.getName()).isEqualTo("A 열");
        assertThat(childNode.getLocationType()).isEqualTo(rowType);
        assertThat(childNode.getParent()).isEqualTo(parent);
        assertThat(childNode.isRoot()).isFalse();
    }

    @Test
    void update_LocationNode() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode containerType = CommonCode.createCommonCode(codeGroup, "CONTAINER", "컨테이너", 1);
        CommonCode zoneType = CommonCode.createCommonCode(codeGroup, "ZONE", "존", 2);
        LocationNode node = LocationNode.createRoot(ROOT_CODE, containerType, "컨테이너 A");

        node.update(zoneType, "컨테이너 A (수정)");

        assertThat(node.getCode()).isEqualTo(ROOT_CODE);
        assertThat(node.getName()).isEqualTo("컨테이너 A (수정)");
        assertThat(node.getLocationType()).isEqualTo(zoneType);
    }

    @Test
    void createChild_throwsWhenParentIsNull() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode rowType = CommonCode.createCommonCode(codeGroup, "ROW", "열", 1);

        assertThatThrownBy(() -> LocationNode.createChild(CHILD_CODE, null, rowType, "A 열"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parent is required");
    }

    @Test
    void updateParent_changesParent() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode containerType = CommonCode.createCommonCode(codeGroup, "CONTAINER", "컨테이너", 1);
        CommonCode rowType = CommonCode.createCommonCode(codeGroup, "ROW", "열", 2);
        CommonCode zoneType = CommonCode.createCommonCode(codeGroup, "ZONE", "존", 3);

        LocationNode root = LocationNode.createRoot(ROOT_CODE, containerType, "컨테이너 A");
        LocationNode zone = LocationNode.createChild("Z9y8X7w6V5", root, zoneType, "Zone 1");
        LocationNode row = LocationNode.createChild(CHILD_CODE, zone, rowType, "A 열");

        row.updateParent(root);

        assertThat(row.getParent()).isEqualTo(root);
    }

    @Test
    void updateParent_promotesToRoot() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode containerType = CommonCode.createCommonCode(codeGroup, "CONTAINER", "컨테이너", 1);
        CommonCode rowType = CommonCode.createCommonCode(codeGroup, "ROW", "열", 2);

        LocationNode root = LocationNode.createRoot(ROOT_CODE, containerType, "컨테이너 A");
        LocationNode row = LocationNode.createChild(CHILD_CODE, root, rowType, "A 열");

        row.updateParent(null);

        assertThat(row.getParent()).isNull();
        assertThat(row.isRoot()).isTrue();
    }

    @Test
    void updateParent_throwsWhenParentIsItself() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode containerType = CommonCode.createCommonCode(codeGroup, "CONTAINER", "컨테이너", 1);
        LocationNode root = LocationNode.createRoot(ROOT_CODE, containerType, "컨테이너 A");

        assertThatThrownBy(() -> root.updateParent(root))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cannot set parent to itself");
    }

    @Test
    void updateParent_throwsOnCircularReference() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode containerType = CommonCode.createCommonCode(codeGroup, "CONTAINER", "컨테이너", 1);
        CommonCode rowType = CommonCode.createCommonCode(codeGroup, "ROW", "열", 2);
        CommonCode rackType = CommonCode.createCommonCode(codeGroup, "RACK", "랙", 3);

        LocationNode root = LocationNode.createRoot(ROOT_CODE, containerType, "컨테이너 A");
        LocationNode row = LocationNode.createChild(CHILD_CODE, root, rowType, "A 열");
        LocationNode rack = LocationNode.createChild("M4n3B2v1C0", row, rackType, "Rack-01");

        assertThatThrownBy(() -> root.updateParent(rack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("circular reference is not allowed");
    }

    @Test
    void createRoot_throwsWhenLocationTypeIsNotLocationTypeGroup() {
        CodeGroup deviceGroup = CodeGroup.createCodeGroup("DEVICE_TYPE", "장비 유형");
        CommonCode pdu = CommonCode.createCommonCode(deviceGroup, "pdu", "PDU", 1);

        assertThatThrownBy(() -> LocationNode.createRoot(ROOT_CODE, pdu, "컨테이너 A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("locationType must belong to LOCATION_TYPE group");
    }

    @Test
    void createRoot_throwsWhenNameIsBlank() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CommonCode locationType = CommonCode.createCommonCode(codeGroup, "container", "컨테이너", 1);

        assertThatThrownBy(() -> LocationNode.createRoot(ROOT_CODE, locationType, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }
}

package net.vivans.dcim.module.devicemodel.domain.model;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceModelSnmpPointTest {

    @Test
    void create_withFixedOid_succeeds() {
        DeviceModelSnmpPoint point = DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "V",
                "1.3.6.1.4.1.12345.10.1.0",
                false,
                "V",
                true
        );

        assertThat(point.getName()).isEqualTo("V");
        assertThat(point.getOid()).isEqualTo("1.3.6.1.4.1.12345.10.1.0");
        assertThat(point.isRequiresInstance()).isFalse();
        assertThat(point.getUnit()).isEqualTo("V");
        assertThat(point.isEnabled()).isTrue();
    }

    @Test
    void create_withTemplateOid_succeeds() {
        DeviceModelSnmpPoint point = DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "PRI-FLOW",
                "1.3.6.1.4.1.12345.{instanceId}.10.1.0",
                true,
                "L/min",
                true
        );

        assertThat(point.isRequiresInstance()).isTrue();
        assertThat(point.getOid()).contains("{instanceId}");
    }

    @Test
    void create_withoutName_throws() {
        assertThatThrownBy(() -> DeviceModelSnmpPoint.create(
                snmpProtocol(),
                " ",
                "1.3.6.1.4.1.12345.10.1.0",
                false,
                null,
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void create_withoutOid_throws() {
        assertThatThrownBy(() -> DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "V",
                " ",
                false,
                null,
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oid is required");
    }

    @Test
    void create_requiresInstanceWithoutPlaceholder_throws() {
        assertThatThrownBy(() -> DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "PRI-FLOW",
                "1.3.6.1.4.1.12345.10.1.0",
                true,
                null,
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oid must contain {instanceId}");
    }

    @Test
    void create_fixedOidWithPlaceholder_throws() {
        assertThatThrownBy(() -> DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "V",
                "1.3.6.1.4.1.12345.{instanceId}.10.1.0",
                false,
                null,
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oid must not contain {instanceId}");
    }

    @Test
    void create_withInvalidOidFormat_throws() {
        assertThatThrownBy(() -> DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "V",
                "not-an-oid",
                false,
                null,
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid oid format");
    }

    @Test
    void create_withNonSnmpProtocol_throws() {
        DeviceModel model = DeviceModel.create("PDU-3P", "Vendor", modelType(), null);
        CommonCode modbus = protocolType("modbus", "Modbus");
        DeviceModelProtocol protocol = DeviceModelProtocol.of(model, modbus);

        assertThatThrownBy(() -> DeviceModelSnmpPoint.create(
                protocol,
                "V",
                "1.3.6.1.4.1.12345.10.1.0",
                false,
                null,
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("protocol must be snmp");
    }

    @Test
    void resolveOid_withFixedOid_returnsOid() {
        DeviceModelSnmpPoint point = DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "temp",
                "1.3.6.1.4.1.12345.10.1.0",
                false,
                "C",
                true
        );

        assertThat(point.resolveOid(null)).isEqualTo("1.3.6.1.4.1.12345.10.1.0");
        assertThat(point.resolveOid(1)).isEqualTo("1.3.6.1.4.1.12345.10.1.0");
    }

    @Test
    void resolveOid_withTemplateOid_replacesInstanceId() {
        DeviceModelSnmpPoint point = DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "V",
                "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.{instanceId}.3",
                true,
                "V",
                true
        );

        assertThat(point.resolveOid(1)).isEqualTo("1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.1.3");
        assertThat(point.resolveOid(null)).isNull();
    }

    @Test
    void update_replacesFields() {
        DeviceModelSnmpPoint point = DeviceModelSnmpPoint.create(
                snmpProtocol(),
                "V",
                "1.3.6.1.4.1.12345.10.1.0",
                false,
                "V",
                true
        );

        point.update(
                "A",
                "1.3.6.1.4.1.12345.{instanceId}.10.2.0",
                true,
                "A",
                false
        );

        assertThat(point.getName()).isEqualTo("A");
        assertThat(point.isRequiresInstance()).isTrue();
        assertThat(point.getUnit()).isEqualTo("A");
        assertThat(point.isEnabled()).isFalse();
    }

    private DeviceModelProtocol snmpProtocol() {
        DeviceModel model = DeviceModel.create("IRCR01K41CDU", "Vendor", modelType(), null);
        return DeviceModelProtocol.of(model, protocolType("snmp", "SNMP"));
    }

    private CommonCode modelType() {
        CodeGroup group = CodeGroup.createCodeGroup("MODEL_TYPE", "Model Type");
        return CommonCode.createCommonCode(group, "CDU", "CDU", 1);
    }

    private CommonCode protocolType(String code, String name) {
        CodeGroup group = CodeGroup.createCodeGroup("PROTOCOL_TYPE", "Protocol Type");
        return CommonCode.createCommonCode(group, code, name, 1);
    }
}
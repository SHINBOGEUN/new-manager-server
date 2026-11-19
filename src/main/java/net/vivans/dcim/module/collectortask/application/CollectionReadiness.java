package net.vivans.dcim.module.collectortask.application;

/** 수집 설정을 고칠 화면과 항목. 상태 판정이나 수집 실행 여부를 변경하지 않는다. */
public record CollectionReadiness(Code code, String item, String href) {
    public enum Code {
        DEVICE_DISABLED, NO_PROTOCOL, NO_ENDPOINT, NO_POINTS, MISSING_SNMP_INSTANCE,
        MISSING_MODBUS_SETTINGS, MISSING_UNIT_ID, INVALID_POINT, MAPPING_REQUIRED,
        NO_SOURCE, NO_IDENTITY, NO_MAPPING
    }
}

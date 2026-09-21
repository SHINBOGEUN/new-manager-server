package net.vivans.dcim.module.lora.domain.model;

/**
 * LoRa/Dragino 외부 식별자 종류.
 * 값이 늘어나면(devAddr, serialNumber 등) 여기에 추가하고 DDL의 CHECK 제약도 같이 갱신한다.
 */
public enum LoraIdType {
    DEV_EUI,
    DEVICE_NAME
}

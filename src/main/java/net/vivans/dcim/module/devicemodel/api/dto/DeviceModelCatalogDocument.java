package net.vivans.dcim.module.devicemodel.api.dto;

import java.time.Instant;
import java.util.List;

public record DeviceModelCatalogDocument(String format, int version, Instant exportedAt, List<Model> models) {
    public record Model(String name, String manufacturer, String deviceType, String description,
                        List<String> protocols, List<SnmpPoint> snmpPoints,
                        List<ModbusPoint> modbusPoints, List<MqttPoint> mqttPoints) {}
    public record SnmpPoint(String name, String dataPointType, String category, String unit, String oid,
                            boolean requiresInstance, Double scale, Boolean enabled) {}
    public record ModbusPoint(String name, String dataPointType, String category, String unit,
                              String registerType, String dataType, String byteOrder, Integer address,
                              boolean requiresInstance, Double scale, Double offset, Boolean enabled) {}
    public record MqttPoint(String payloadField, String pointName, String dataPointType,
                            String category, String unit, String componentScope, String componentType,
                            String componentPath, String componentIndexField, String componentUuidField,
                            Double scale, String valueMap, Boolean enabled) {}
}

package net.vivans.dcim.module.devicemodel.api.dto;

import java.time.Instant;
import java.util.List;

public record DeviceModelCatalogDocument(String format, int version, Instant exportedAt, List<Model> models) {
    public record Model(String name, String manufacturer, String deviceType, String description,
                        List<String> protocols, List<SnmpPoint> snmpPoints,
                        List<ModbusPoint> modbusPoints, List<MqttPoint> mqttPoints,
                        String phaseNotation) {
        public Model(String name, String manufacturer, String deviceType, String description,
                     List<String> protocols, List<SnmpPoint> snmpPoints,
                     List<ModbusPoint> modbusPoints, List<MqttPoint> mqttPoints) {
            this(name, manufacturer, deviceType, description, protocols, snmpPoints, modbusPoints, mqttPoints, null);
        }
    }
    public record SnmpPoint(String name, String dataPointType, String category, String unit, String oid,
                            boolean requiresInstance, Double scale, Boolean enabled, String phase) {
        public SnmpPoint(String name, String dataPointType, String category, String unit, String oid,
                         boolean requiresInstance, Double scale, Boolean enabled) {
            this(name, dataPointType, category, unit, oid, requiresInstance, scale, enabled, null);
        }
    }
    public record ModbusPoint(String name, String dataPointType, String category, String unit,
                              String registerType, String dataType, String byteOrder, Integer address,
                              boolean requiresInstance, Double scale, Double offset, Boolean enabled, String phase) {
        public ModbusPoint(String name, String dataPointType, String category, String unit,
                           String registerType, String dataType, String byteOrder, Integer address,
                           boolean requiresInstance, Double scale, Double offset, Boolean enabled) {
            this(name, dataPointType, category, unit, registerType, dataType, byteOrder, address,
                    requiresInstance, scale, offset, enabled, null);
        }
    }
    public record MqttPoint(String payloadField, String pointName, String dataPointType,
                            String category, String unit, String componentScope, String componentType,
                            String componentPath, String componentIndexField, String componentUuidField,
                            Double scale, String valueMap, Boolean enabled, String phase) {
        public MqttPoint(String payloadField, String pointName, String dataPointType,
                         String category, String unit, String componentScope, String componentType,
                         String componentPath, String componentIndexField, String componentUuidField,
                         Double scale, String valueMap, Boolean enabled) {
            this(payloadField, pointName, dataPointType, category, unit, componentScope, componentType,
                    componentPath, componentIndexField, componentUuidField, scale, valueMap, enabled, null);
        }
    }
}

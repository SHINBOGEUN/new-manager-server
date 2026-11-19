package net.vivans.dcim.module.collectortask.application;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Collector의 ModbusCollectionGroupSpec JSON 계약과 동일한 필드만 직렬화한다. */
public record CollectionGroupModbusSpec(
        Integer taskId,
        Integer groupId,
        Integer modelId,
        String protocol,
        String cronExpression,
        int timeoutMs,
        int retries,
        int maxConcurrency,
        List<ModbusPoint> points,
        List<ModbusTarget> targets,
        List<String> skipped,
        Integer failureThreshold, Integer backoffMaxSeconds
) {
    public CollectionGroupModbusSpec(Integer taskId,
        Integer groupId,
        Integer modelId,
        String protocol,
        String cronExpression,
        int timeoutMs,
        int retries,
        int maxConcurrency,
        List<ModbusPoint> points,
        List<ModbusTarget> targets,
        List<String> skipped){this(taskId,groupId,modelId,protocol,cronExpression,timeoutMs,retries,maxConcurrency,points,targets,skipped,null,null);}

    public record ModbusPoint(
            String name,
            String registerType,
            Integer address,
            String dataType,
            String byteOrder,
            Double scale,
            Double offset,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ModbusBitField> bitFields
    ) {
        public ModbusPoint(String name, String registerType, Integer address, String dataType,
                           String byteOrder, Double scale, Double offset) {
            this(name, registerType, address, dataType, byteOrder, scale, offset, List.of());
        }
    }

    public record ModbusBitField(String name, int bitOffset, int bitWidth,
                                 java.util.Map<String, Long> valueMap, Long unmappedValue) {
    }

    public record ModbusTarget(
            Integer deviceId,
            String host,
            int port,
            Integer unitId,
            List<ModbusPoint> points,
            Integer sourceDeviceId
    ) {
        public ModbusTarget(Integer deviceId,String host,int port,Integer unitId,List<ModbusPoint> points){this(deviceId,host,port,unitId,points,null);}
    }
}

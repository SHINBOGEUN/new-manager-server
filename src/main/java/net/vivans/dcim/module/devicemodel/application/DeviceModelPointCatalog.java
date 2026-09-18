package net.vivans.dcim.module.devicemodel.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 모델에 등록된 측정항목의 표시 메타데이터(단위)를 프로토콜 구분 없이 돌려준다.
 * <p>
 * 호출하는 쪽은 프로토콜을 알 필요가 없다. 프로토콜이 늘어나면 이 클래스에 해당 point 저장소를
 * 한 줄 추가하면 되고, 사용하는 화면·서비스는 그대로 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModelPointCatalog {

    private final DeviceModelSnmpPointRepository snmpPointRepository;
    private final DeviceModelModbusPointRepository modbusPointRepository;

    /**
     * modelId → (측정항목 이름 → 단위). 단위가 없으면 값은 null이다.
     * 같은 모델에 이름이 겹치는 항목이 있으면 먼저 조회된 쪽을 유지한다.
     */
    public Map<Integer, Map<String, String>> unitsByModelId(Collection<Integer> deviceModelIds) {
        if (deviceModelIds == null || deviceModelIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Map<String, String>> result = new HashMap<>();
        for (DeviceModelSnmpPoint point : snmpPointRepository.findAllEnabledByDeviceModelIds(deviceModelIds)) {
            put(result, point.getModelProtocol().getDeviceModel().getId(), point.getName(), point.getUnit());
        }
        for (DeviceModelModbusPoint point : modbusPointRepository.findAllEnabledByDeviceModelIds(deviceModelIds)) {
            put(result, point.getModelProtocol().getDeviceModel().getId(), point.getName(), point.getUnit());
        }
        return result;
    }

    private static void put(Map<Integer, Map<String, String>> result, Integer modelId, String name, String unit) {
        if (modelId == null || name == null || name.isBlank()) {
            return;
        }
        result.computeIfAbsent(modelId, ignored -> new LinkedHashMap<>()).putIfAbsent(name, unit);
    }
}

package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionReconciler;
import net.vivans.dcim.module.device.api.dto.DeviceModbusBitFieldRequest;
import net.vivans.dcim.module.device.api.dto.DeviceModbusBitFieldResponse;
import net.vivans.dcim.module.device.domain.model.DeviceModbusBitField;
import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusDataType;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusRegisterType;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModbusBitFieldService {
    private final DeviceModbusReadingRepository readingRepository;
    private final DeviceModbusBitFieldRepository fieldRepository;
    private final CollectionReconciler collectionReconciler;

    public List<DeviceModbusBitFieldResponse> get(Integer deviceId, Integer endpointId, Integer readingId) {
        findReading(deviceId, endpointId, readingId);
        return fields(readingId);
    }

    @Transactional
    public List<DeviceModbusBitFieldResponse> replace(Integer deviceId, Integer endpointId, Integer readingId,
                                                       List<DeviceModbusBitFieldRequest> requested) {
        DeviceModbusReading reading = findReading(deviceId, endpointId, readingId);
        var point = reading.getPoint();
        if (point.getRegisterType() != ModbusRegisterType.HOLDING
                && point.getRegisterType() != ModbusRegisterType.INPUT) {
            throw new IllegalArgumentException("bit fields require HOLDING or INPUT registers");
        }
        if (point.getDataType() == ModbusDataType.FLOAT32
                || point.getScale() != null && point.getScale() != 1.0
                || point.getOffset() != null && point.getOffset() != 0.0) {
            throw new IllegalArgumentException("bit fields require an unscaled integer model point");
        }
        if (requested.size() > 32) throw new IllegalArgumentException("at most 32 bit fields are allowed");

        Set<String> names = new HashSet<>();
        names.add(reading.getPointName());
        for (DeviceModbusReading other : readingRepository.findAllByTargetDeviceIdOrderByIdAsc(
                reading.getTargetDevice().getId())) {
            if (!other.getId().equals(readingId)) {
                names.add(other.getPointName());
                fieldRepository.findAllByReading_IdOrderByIdAsc(other.getId())
                        .forEach(field -> names.add(field.getPointName()));
            }
        }

        for (DeviceModbusBitFieldRequest field : requested) {
            if (field == null || !validPointName(field.pointName())) {
                throw new IllegalArgumentException("bit field point name is invalid");
            }
            if (!names.add(field.pointName())) {
                throw new ConflictException("Modbus point name already exists for target device: " + field.pointName());
            }
            if (field.bitOffset() == null || field.bitWidth() == null || field.bitOffset() < 0
                    || field.bitWidth() < 1 || field.bitWidth() > 32
                    || (long) field.bitOffset() + field.bitWidth() > point.getDataType().getRegisterCount() * 16) {
                throw new IllegalArgumentException("bit range exceeds source register width");
            }
            long max = (1L << field.bitWidth()) - 1;
            for (Map.Entry<String, Long> entry :
                    (field.valueMap() == null ? Map.<String, Long>of() : field.valueMap()).entrySet()) {
                try {
                    long code = Long.parseLong(entry.getKey());
                    if (code < 0 || code > max || !Long.toString(code).equals(entry.getKey())
                            || entry.getValue() == null) {
                        throw new IllegalArgumentException("invalid bit mapping key/value: " + entry.getKey());
                    }
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("invalid bit mapping key: " + entry.getKey(), exception);
                }
            }
        }

        fieldRepository.deleteAllByReading_Id(readingId);
        fieldRepository.flush();
        fieldRepository.saveAll(requested.stream().map(field -> DeviceModbusBitField.create(
                reading, field.pointName(), field.bitOffset(), field.bitWidth(), field.valueMap(),
                field.unmappedValue())).toList());
        fieldRepository.flush();
        collectionReconciler.reconcileModel(reading.getEndpointModbus().getEndpoint()
                .getDevice().getDeviceModel().getId());
        return fields(readingId);
    }

    private List<DeviceModbusBitFieldResponse> fields(Integer readingId) {
        return fieldRepository.findAllByReading_IdOrderByIdAsc(readingId).stream()
                .map(DeviceModbusBitFieldResponse::from).toList();
    }

    private DeviceModbusReading findReading(Integer deviceId, Integer endpointId, Integer readingId) {
        return readingRepository.findByIdAndEndpointId(readingId, endpointId)
                .filter(reading -> reading.getEndpointModbus().getEndpoint().getDevice().getId().equals(deviceId))
                .orElseThrow(() -> new EntityNotFoundException("DeviceModbusReading not found: " + readingId));
    }

    private static boolean validPointName(String name) {
        if (name == null || name.isBlank() || name.length() > 255) return false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isWhitespace(ch) || Character.isSpaceChar(ch) || ch == ',' || ch == '=' || ch == '"') {
                return false;
            }
        }
        return true;
    }
}

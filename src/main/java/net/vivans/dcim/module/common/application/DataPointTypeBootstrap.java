package net.vivans.dcim.module.common.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CodeGroupRepository;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataPointTypeBootstrap implements CommandLineRunner {
    private final CodeGroupRepository codeGroupRepository;
    private final CommonCodeRepository commonCodeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        CodeGroup group = codeGroupRepository.findAll().stream()
                .filter(it -> "DATA_POINT_TYPE".equals(it.getGroupKey()))
                .findFirst()
                .orElseGet(() -> codeGroupRepository.save(CodeGroup.createCodeGroup(
                        "DATA_POINT_TYPE", "Data Point Type")));
        // HUMIDITY: Dragino LoRa 온습도 센서 등 실사용 확인됨. UNCLASSIFIED: 배터리·문열림·누수·CO2 등
        // DATA_POINT_TYPE을 불필요하게 세분화하지 않기 위한 공용 미분류 타입 (point_name으로 실제 의미 구분).
        String[] codes = {"POWER", "ENERGY", "CURRENT", "VOLTAGE", "TEMPERATURE", "PRESSURE", "FLOW", "POWER_FACTOR", "HUMIDITY", "UNCLASSIFIED"};
        for (int i = 0; i < codes.length; i++) {
            if (!commonCodeRepository.findByCodeGroupGroupKeyAndCode("DATA_POINT_TYPE", codes[i]).isPresent()) {
                commonCodeRepository.save(CommonCode.createCommonCode(group, codes[i], codes[i], i + 1));
            }
        }
    }
}

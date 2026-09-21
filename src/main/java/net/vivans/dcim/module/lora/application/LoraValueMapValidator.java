package net.vivans.dcim.module.lora.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * value_map 컬럼(상태값→숫자 매핑 JSON, 예: {"leak":1,"no leak":0})의 형식을 저장 전에 검증한다.
 * null/blank는 허용(매핑 없음 = 숫자 파싱만 시도).
 */
@Component
@RequiredArgsConstructor
public class LoraValueMapValidator {

    private final ObjectMapper objectMapper;

    public void validate(String valueMap) {
        if (valueMap == null || valueMap.isBlank()) {
            return;
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(valueMap);
        } catch (Exception e) {
            throw new IllegalArgumentException("valueMap must be a valid JSON object: " + e.getMessage());
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("valueMap must be a JSON object (key: string, value: number)");
        }
        node.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isNumber()) {
                throw new IllegalArgumentException("valueMap values must be numbers: " + entry.getKey());
            }
        });
    }
}

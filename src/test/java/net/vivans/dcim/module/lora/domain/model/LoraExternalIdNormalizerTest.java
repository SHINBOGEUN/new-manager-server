package net.vivans.dcim.module.lora.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sensor Data의 net.vivans.dcim.module.lora.domain.LoraExternalIdNormalizer와
 * 반드시 동일한 결과를 내야 한다(등록 시점과 수신 조회 시점의 정규화 규칙이 어긋나면
 * 등록된 장비가 있어도 수신 시 매칭에 실패한다).
 */
class LoraExternalIdNormalizerTest {

    @Test
    void normalize_stripsColonsFromDevEui() {
        assertThat(LoraExternalIdNormalizer.normalize("24:E1:24:71:0C:12:34:56"))
                .isEqualTo("24E124710C123456");
    }

    @Test
    void normalize_stripsDashesUnderscoresAndSpacesFromDeviceName() {
        assertThat(LoraExternalIdNormalizer.normalize("dragino_lht65n-01"))
                .isEqualTo("DRAGINOLHT65N01");
    }

    @Test
    void normalize_trimsSurroundingWhitespace() {
        assertThat(LoraExternalIdNormalizer.normalize("  abc 123  "))
                .isEqualTo("ABC123");
    }

    @Test
    void normalize_alreadyNormalizedValueIsUnchanged() {
        assertThat(LoraExternalIdNormalizer.normalize("24E124710C123456"))
                .isEqualTo("24E124710C123456");
    }

    @Test
    void normalize_returnsNullForNull() {
        assertThat(LoraExternalIdNormalizer.normalize(null)).isNull();
    }
}

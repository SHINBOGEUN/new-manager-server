package net.vivans.dcim.module.lora.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoraValueMapValidatorTest {

    private final LoraValueMapValidator validator = new LoraValueMapValidator(new ObjectMapper());

    @Test
    void validate_allowsNull() {
        assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
    }

    @Test
    void validate_allowsBlank() {
        assertThatCode(() -> validator.validate("   ")).doesNotThrowAnyException();
    }

    @Test
    void validate_allowsValidNumericObject() {
        assertThatCode(() -> validator.validate("{\"leak\":1,\"no leak\":0}"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsInvalidJson() {
        assertThatThrownBy(() -> validator.validate("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid JSON object");
    }

    @Test
    void validate_rejectsJsonArray() {
        assertThatThrownBy(() -> validator.validate("[1,2,3]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON object");
    }

    @Test
    void validate_rejectsNonNumericValues() {
        assertThatThrownBy(() -> validator.validate("{\"leak\":\"high\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be numbers");
    }
}

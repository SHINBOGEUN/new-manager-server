package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceModbusReadingRequestTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void createAllowsOmittedEnabled() {
        var request = new DeviceModbusReadingCreateRequest(
                1, 0, 11265, 14, "TOTAL_WT", null);

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void updateRejectsOmittedEnabled() {
        var request = new DeviceModbusReadingUpdateRequest(
                1, 0, 11265, 14, "TOTAL_WT", null);

        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("enabled");
    }

    @Test
    void updateAllowsExplicitlyDisabledReading() {
        var request = new DeviceModbusReadingUpdateRequest(
                1, 0, 11265, 14, "TOTAL_WT", false);

        assertThat(VALIDATOR.validate(request)).isEmpty();
        assertThat(request.enabled()).isFalse();
    }
}

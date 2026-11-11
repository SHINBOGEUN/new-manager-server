package net.vivans.dcim.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequiredSettingsValidatorTest {

    private static MockEnvironment complete() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:x")
                .withProperty("spring.datasource.username", "sa")
                .withProperty("jwt.secret", "secret")
                .withProperty("manager.inbound.service-api-key", "manager-key")
                .withProperty("collector.service.api-key", "collector-key");
    }

    @Test
    void completeSettingsPass() {
        new RequiredSettingsValidator().postProcessEnvironment(complete(), new SpringApplication());
    }

    @Test
    void reportsEveryMissingSettingAtOnce() {
        var environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mariadb://${MARIADB_HOST}:${MARIADB_PORT}/dcim");
        assertThatThrownBy(() -> new RequiredSettingsValidator().postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MARIADB_HOST")
                .hasMessageContaining("DB_USER")
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("MANAGER_API_KEY")
                .hasMessageContaining("COLLECTOR_API_KEY");
    }

    @Test
    void conditionalSettingsAreCheckedOnlyWhenTheFeatureIsOn() {
        var environment = complete().withProperty("collector.service.enabled", "false")
                .withProperty("collector.service.api-key", "");
        assertThat(RequiredSettingsValidator.missing(environment)).isEmpty();

        environment.withProperty("influx.enabled", "true");
        assertThat(RequiredSettingsValidator.missing(environment))
                .extracting(RequiredSettingsValidator.Requirement::envHint)
                .containsExactly("INFLUX_URL", "INFLUX_TOKEN");
    }

    @Test
    void canBeSwitchedOff() {
        var environment = new MockEnvironment().withProperty("startup.required-settings.enabled", "false");
        new RequiredSettingsValidator().postProcessEnvironment(environment, new SpringApplication());
    }
}

package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.location.infrastructure.persistence.LocationNodeSpringDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.bootstrap.config.JpaConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = ManagerServerApplication.class)
@Import(JpaConfig.class)
@EnabledIfEnvironmentVariable(named = "DCIM_RUN_RACK_DB_TEST", matches = "true")
class RackModelPlacementRepositoryTest {
    @Autowired RackModelPlacementRepository models;
    @Autowired LocationNodeSpringDataRepository locations;

    @Test void lookupAndOverlapQueriesRunAgainstTheMigratedSchema() {
        assertDoesNotThrow(() -> models.findAllByRackLocationCode("0000000000"));
        assertDoesNotThrow(() -> models.overlaps("0000000000", 1, 4));
        assertDoesNotThrow(() -> locations.lockByCode("0000000000"));
    }
}

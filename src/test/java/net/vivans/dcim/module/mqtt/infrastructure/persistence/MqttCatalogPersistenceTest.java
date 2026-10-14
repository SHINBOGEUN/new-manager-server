package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.mqtt.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/** H2를 명시해 운영/개발 MariaDB에 접속하지 않고 엔티티와 저장소 쿼리를 검증한다. */
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:mqtt_catalog;MODE=MariaDB;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = MqttCatalogPersistenceTest.Config.class)
class MqttCatalogPersistenceTest {
    @Configuration
    @EntityScan("net.vivans.dcim.module")
    @EnableJpaRepositories(basePackageClasses = DeviceMqttIdentitySpringDataRepository.class)
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired DeviceMqttIdentitySpringDataRepository identities;
    @Autowired DeviceModelMqttPointSpringDataRepository points;

    @Test
    void renamedEntitiesAndQueriesRoundTripAndMacUpdateKeepsDeviceId() {
        DeviceModel model = em.persist(DeviceModel.create("GPU test", "Test", code("MODEL_TYPE", "GPU_SERVER"), null));
        LocationNode location = em.persist(LocationNode.createRoot("UNASSIGNED", code("LOCATION_TYPE", "UNASSIGNED"), "Unassigned"));
        Device device = em.persist(Device.create(model, location, "Server 1", null));
        Integer deviceId = device.getId();
        var identity = identities.save(DeviceMqttIdentity.create(device, MqttIdentityType.MAC, "9c6b009b4bd7", true));
        Integer identityId = identity.getId();
        var point = DeviceModelMqttPoint.create(model, "gpu_temperature", "gpu_temperature",
                code("DATA_POINT_TYPE", "TEMPERATURE"), null, 1.0, null, true);
        point.configureComponent("GPU", "gpus", "gpu_index", "gpu_uuid");
        points.save(point);
        em.flush();
        em.clear();

        var stored = identities.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(MqttIdentityType.MAC, "9C6B009B4BD7").orElseThrow();
        assertThat(stored.getDevice().getId()).isEqualTo(deviceId);
        assertThat(identities.existsByDeviceIdAndIdentityType(deviceId, MqttIdentityType.MAC)).isTrue();
        assertThat(identities.existsByDeviceIdAndIdentityTypeAndIdNot(deviceId, MqttIdentityType.MAC, identityId)).isFalse();
        stored.update("AA:BB:CC:DD:EE:FF", true);
        identities.saveAndFlush(stored);
        em.clear();
        assertThat(identities.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(MqttIdentityType.MAC, "9C6B009B4BD7")).isEmpty();
        var updated = identities.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(MqttIdentityType.MAC, "AABBCCDDEEFF").orElseThrow();
        assertThat(updated.getId()).isEqualTo(identityId);
        assertThat(updated.getDevice().getId()).isEqualTo(deviceId);
        var mappings = points.findAllEnabledByDeviceModelIdIn(java.util.List.of(model.getId()));
        assertThat(mappings).hasSize(1);
        assertThat(mappings.get(0).getComponentScope()).isEqualTo(MqttPointScope.COMPONENT);
        assertThat(mappings.get(0).getComponentIndexField()).isEqualTo("gpu_index");
        assertThat(mappings.get(0).getComponentUuidField()).isEqualTo("gpu_uuid");
    }

    private CommonCode code(String groupKey, String value) {
        CodeGroup group = em.persist(CodeGroup.createCodeGroup(groupKey, groupKey));
        return em.persist(CommonCode.createCommonCode(group, value, value, 1));
    }
}

package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.parser.PartTree;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceModbusReadingRepositoryPathTest {
    @Test
    void endpointQueriesUseModbusSharedPrimaryKey() {
        for (String method : new String[]{
                "findAllByEndpointModbus_EndpointIdOrderByIdAsc",
                "findByIdAndEndpointModbus_EndpointId",
                "existsByEndpointModbus_EndpointIdAndUnitIdAndAddress",
                "existsByEndpointModbus_EndpointIdAndUnitIdAndAddressAndIdNot"}) {
            var tree = new PartTree(method, DeviceModbusReading.class);
            assertThat(tree.getParts().stream().map(part -> part.getProperty().toDotPath()))
                    .contains("endpointModbus.endpointId");
        }
    }
}

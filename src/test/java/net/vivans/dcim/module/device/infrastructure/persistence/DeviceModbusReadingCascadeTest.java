package net.vivans.dcim.module.device.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실제 DB에 접속하지 않고 23/27 DDL의 FK 삭제 범위를 H2 MariaDB 모드로 검증합니다. */
class DeviceModbusReadingCascadeTest {
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MariaDB");
        execute("CREATE TABLE device_protocol_endpoint (id INT PRIMARY KEY)");
        execute("CREATE TABLE devices (id INT PRIMARY KEY)");
        execute("CREATE TABLE device_model_modbus_point (id INT PRIMARY KEY)");
        execute(Files.readString(Path.of("sql/schema/23_device_endpoint_modbus.sql")));
        execute(Files.readString(Path.of("sql/schema/43_device_modbus_reading.sql")));
        execute("INSERT INTO device_protocol_endpoint VALUES (14), (15)");
        execute("INSERT INTO devices VALUES (14), (20)");
        execute("INSERT INTO device_model_modbus_point VALUES (1)");
        execute("INSERT INTO device_endpoint_modbus(endpoint_id) VALUES (14), (15)");
        execute("INSERT INTO device_modbus_reading(id,endpoint_id,point_id,unit_id,address,target_device_id,point_name) "
                + "VALUES (1,14,1,0,11265,14,'TOTAL_WT'), (2,14,1,1,11415,20,'TOTAL_WT'), "
                + "(3,15,1,0,11265,14,'OTHER_WT')");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    void deleteModbusConfig_deletesOnlyOwnedReadings() throws Exception {
        execute("DELETE FROM device_endpoint_modbus WHERE endpoint_id=14");
        assertThat(count("device_modbus_reading")).isEqualTo(1);
        assertThat(count("device_protocol_endpoint")).isEqualTo(2);
        assertThat(count("device_endpoint_modbus")).isEqualTo(1);
        assertCatalogAndTargetsRemain();
    }

    @Test
    void deleteCommonEndpoint_cascadesThroughModbusConfig() throws Exception {
        execute("DELETE FROM device_protocol_endpoint WHERE id=14");
        assertThat(count("device_modbus_reading")).isEqualTo(1);
        assertThat(count("device_endpoint_modbus")).isEqualTo(1);
        assertThat(count("device_protocol_endpoint")).isEqualTo(1);
        assertCatalogAndTargetsRemain();
    }

    @Test
    void deleteReading_keepsParentsAndOtherReadings() throws Exception {
        execute("DELETE FROM device_modbus_reading WHERE id=1");
        assertThat(count("device_modbus_reading")).isEqualTo(2);
        assertThat(count("device_endpoint_modbus")).isEqualTo(2);
        assertThat(count("device_protocol_endpoint")).isEqualTo(2);
        assertCatalogAndTargetsRemain();
    }

    @Test
    void readingWithoutModbusConfig_isRejectedEvenIfCommonEndpointExists() throws Exception {
        execute("DELETE FROM device_endpoint_modbus WHERE endpoint_id=14");
        assertThatThrownBy(() -> execute("INSERT INTO device_modbus_reading"
                + "(endpoint_id,point_id,unit_id,address,target_device_id,point_name)"
                + " VALUES (14,1,0,11265,14,'TOTAL_WT')"))
                .isInstanceOf(SQLException.class);
    }

    private void assertCatalogAndTargetsRemain() throws Exception {
        assertThat(count("devices")).isEqualTo(2);
        assertThat(count("device_model_modbus_point")).isEqualTo(1);
    }

    private int count(String table) throws Exception {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}

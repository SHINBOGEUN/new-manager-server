package net.vivans.dcim.module.device.application;

import net.vivans.dcim.module.device.domain.model.DeviceAssetFileKind;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static org.junit.jupiter.api.Assertions.*;

class DeviceAssetModelValidationTest {
    @Test void acceptsGlb2WithMatchingLength() {
        byte[] header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                .put(new byte[]{'g','l','T','F'}).putInt(2).putInt(12).array();
        assertDoesNotThrow(() -> DeviceAssetService.validateAssetModelFile(
                new MockMultipartFile("file", "rack.glb", "model/gltf-binary", header), DeviceAssetFileKind.ASSET_3D));
    }

    @Test void rejectsInvalidOrTruncatedGlb() {
        byte[] wrongLength = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                .put(new byte[]{'g','l','T','F'}).putInt(2).putInt(100).array();
        assertThrows(IllegalArgumentException.class, () -> DeviceAssetService.validateAssetModelFile(
                new MockMultipartFile("file", "rack.glb", "model/gltf-binary", wrongLength), DeviceAssetFileKind.ASSET_3D));
        assertThrows(IllegalArgumentException.class, () -> DeviceAssetService.validateAssetModelFile(
                new MockMultipartFile("file", "rack.glb", "model/gltf-binary", new byte[]{1, 2, 3}), DeviceAssetFileKind.ASSET_3D));
    }
}

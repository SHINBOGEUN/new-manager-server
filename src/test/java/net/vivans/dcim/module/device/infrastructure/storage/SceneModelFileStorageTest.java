package net.vivans.dcim.module.device.infrastructure.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class SceneModelFileStorageTest {
    private final SceneModelFileStorage storage = new SceneModelFileStorage("./uploads/test-scene-models");

    @Test
    void acceptsCompleteGlbTwoHeader() {
        byte[] bytes = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
                .put(new byte[]{'g', 'l', 'T', 'F'}).putInt(2).putInt(24)
                .putInt(4).putInt(0x4E4F534A).put(new byte[]{'{', '}', ' ', ' '}).array();
        storage.validate(new MockMultipartFile("file", "rack.glb", "model/gltf-binary", bytes));
    }

    @Test
    void rejectsIncorrectHeaderLengthAndExtension() {
        byte[] bytes = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
                .put(new byte[]{'g', 'l', 'T', 'F'}).putInt(2).putInt(50)
                .putInt(4).putInt(0x4E4F534A).put(new byte[]{'{', '}', ' ', ' '}).array();
        assertThrows(IllegalArgumentException.class, () ->
                storage.validate(new MockMultipartFile("file", "rack.glb", "model/gltf-binary", bytes)));
        assertThrows(IllegalArgumentException.class, () ->
                storage.validate(new MockMultipartFile("file", "rack.obj", "text/plain", bytes)));
    }
}

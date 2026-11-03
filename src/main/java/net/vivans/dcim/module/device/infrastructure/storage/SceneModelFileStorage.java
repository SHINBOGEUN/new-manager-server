package net.vivans.dcim.module.device.infrastructure.storage;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class SceneModelFileStorage {
    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private final Path root;

    public SceneModelFileStorage(@Value("${scene.model.storage-path:./uploads/scene-models}") String storagePath) {
        root = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public void validate(MultipartFile file) {
        String name = file == null ? null : file.getOriginalFilename();
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE
                || name == null || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".glb")) {
            throw new IllegalArgumentException("20MB 이하 GLB 2.0 파일을 선택하세요.");
        }
        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(20);
            if (header.length != 20 || header[0] != 'g' || header[1] != 'l' || header[2] != 'T' || header[3] != 'F') {
                throw new IllegalArgumentException("올바른 GLB 파일이 아닙니다.");
            }
            ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            long jsonLength = Integer.toUnsignedLong(buffer.getInt(12));
            if (buffer.getInt(4) != 2 || Integer.toUnsignedLong(buffer.getInt(8)) != file.getSize()
                    || jsonLength < 2 || jsonLength > file.getSize() - 20 || buffer.getInt(16) != 0x4E4F534A) {
                throw new IllegalArgumentException("GLB 2.0 형식과 파일 크기를 확인하세요.");
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("GLB 파일을 읽지 못했습니다.", error);
        }
    }

    public String store(MultipartFile file) {
        validate(file);
        String key = UUID.randomUUID() + ".glb";
        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), root.resolve(key));
            return key;
        } catch (IOException error) {
            throw new IllegalStateException("3D 모델 파일 저장 실패", error);
        }
    }

    public Resource load(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new EntityNotFoundException("3D 모델 파일이 없습니다.");
        }
        return new FileSystemResource(file);
    }

    public void delete(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) return;
        try { Files.deleteIfExists(file); } catch (IOException ignored) { }
    }
}

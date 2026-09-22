package net.vivans.dcim.module.device.infrastructure.storage;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class DeviceAssetFileStorage {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_DOCUMENT_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path imageRoot;
    private final Path documentRoot;

    public DeviceAssetFileStorage(
            @Value("${asset.image.storage-path:./uploads/device-images}") String imageStoragePath,
            @Value("${asset.document.storage-path:./uploads/device-documents}") String documentStoragePath
    ) {
        this.imageRoot = Path.of(imageStoragePath).toAbsolutePath().normalize();
        this.documentRoot = Path.of(documentStoragePath).toAbsolutePath().normalize();
    }

    public StoredFile storeImage(Integer deviceId, MultipartFile file) {
        validateImageFile(file);
        String originalName = safeOriginalName(file.getOriginalFilename());
        String storageKey = deviceId + "/" + UUID.randomUUID() + extensionFor(file.getContentType());
        store(file, imageRoot.resolve(storageKey).normalize(), "failed to store device image");
        return new StoredFile(storageKey, originalName, file.getContentType(), file.getSize());
    }

    public StoredFile storeDocument(Integer deviceId, MultipartFile file) {
        validateDocument(file);
        String originalName = safeOriginalName(file.getOriginalFilename());
        String storageKey = deviceId + "/" + UUID.randomUUID() + extensionFromName(originalName);
        store(file, documentRoot.resolve(storageKey).normalize(), "failed to store asset document");
        return new StoredFile(storageKey, originalName, contentType(file), file.getSize());
    }

    public Resource loadImage(String storageKey, Integer imageId) {
        return load(imageRoot, storageKey, "Device image file not found: " + imageId);
    }

    public Resource loadDocument(String storageKey, Integer documentId) {
        return load(documentRoot, storageKey, "Asset document file not found: " + documentId);
    }

    public void deleteImage(String storageKey) {
        delete(imageRoot, storageKey);
    }

    public void deleteDocument(String storageKey) {
        delete(documentRoot, storageKey);
    }

    public void validateImage(MultipartFile file) {
        validateImageFile(file);
    }

    private static void store(MultipartFile file, Path target, String failureMessage) {
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    private static Resource load(Path root, String storageKey, String notFoundMessage) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new EntityNotFoundException(notFoundMessage);
        }
        return new FileSystemResource(path);
    }

    private static void delete(Path root, String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // DB 삭제 결과를 유지하기 위해 파일 정리 실패는 기존 동작과 동일하게 무시한다.
        }
    }

    private static void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("image file is required");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(type)) {
            throw new IllegalArgumentException("only JPEG, PNG and WEBP images are allowed");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("image file must not exceed 10MB");
        }
    }

    private static void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("document file is required");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException("document file must not exceed 20MB");
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> ".webp";
        };
    }

    private static String safeOriginalName(String name) {
        if (name == null || name.isBlank()) {
            return "image";
        }
        return Path.of(name).getFileName().toString();
    }

    private static String extensionFromName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 1 || dot == name.length() - 1 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static String contentType(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();
    }

    public record StoredFile(String storageKey, String originalName, String contentType, long size) {
    }
}

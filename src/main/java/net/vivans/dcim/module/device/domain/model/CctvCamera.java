package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.net.URI;

@Entity
@Table(name = "cctv_camera", uniqueConstraints = @UniqueConstraint(
        name = "uk_cctv_camera_camera_key", columnNames = "camera_key"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CctvCamera extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "camera_key", nullable = false, length = 50)
    private String cameraKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "stream_url", nullable = false, length = 1000)
    private String streamUrl;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static void validateUrl(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("camera URL is required");
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("camera URL is invalid");
        }
        if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme())) || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("camera URL must be an HTTP(S) URL without embedded credentials");
        }
    }
}

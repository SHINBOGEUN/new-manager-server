package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "page_widget_psychrometric_source", uniqueConstraints =
        @UniqueConstraint(name = "uk_page_widget_psych_source", columnNames = {"widget_id", "device_id", "point_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetPsychrometricSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private PageWidgetPsychrometric psychrometric;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PageWidgetPsychrometricSourceRole role;

    @Column(name = "point_name", nullable = false, length = 100)
    private String pointName;

    private PageWidgetPsychrometricSource(
            PageWidgetPsychrometric psychrometric,
            Device device,
            PageWidgetPsychrometricSourceRole role,
            String pointName
    ) {
        if (device == null || device.getId() == null) {
            throw new IllegalArgumentException("psychrometric source device is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("psychrometric source role is required");
        }
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException("psychrometric source pointName is required");
        }
        this.psychrometric = psychrometric;
        this.device = device;
        this.role = role;
        this.pointName = pointName.trim();
    }

    static PageWidgetPsychrometricSource create(
            PageWidgetPsychrometric psychrometric,
            Device device,
            PageWidgetPsychrometricSourceRole role,
            String pointName
    ) {
        return new PageWidgetPsychrometricSource(psychrometric, device, role, pointName);
    }

    void update(PageWidgetPsychrometricSourceRole role) {
        if (role == null) {
            throw new IllegalArgumentException("psychrometric source role is required");
        }
        this.role = role;
    }
}

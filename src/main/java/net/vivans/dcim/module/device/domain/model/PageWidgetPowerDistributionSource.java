package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "page_widget_power_distribution_source", uniqueConstraints =
        @UniqueConstraint(name = "uk_page_widget_power_distribution_source", columnNames = {"group_id", "device_id", "point_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageWidgetPowerDistributionSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private PageWidgetPowerDistributionGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "point_name", nullable = false, length = 100)
    private String pointName;

    private PageWidgetPowerDistributionSource(PageWidgetPowerDistributionGroup group, Device device, String pointName) {
        this.group = group;
        this.device = device;
        this.pointName = pointName;
    }

    static PageWidgetPowerDistributionSource create(PageWidgetPowerDistributionGroup group, Device device, String pointName) {
        return new PageWidgetPowerDistributionSource(group, device, pointName);
    }
}

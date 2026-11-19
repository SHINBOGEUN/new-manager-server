package net.vivans.dcim.module.collectortask.domain.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.shared.persistence.BaseEntity;
@Entity @Getter @NoArgsConstructor
@Table(name="device_collection_setting",uniqueConstraints=@UniqueConstraint(name="uk_device_collection_setting_device_protocol",columnNames={"device_id","protocol_type_id"}))
public class DeviceCollectionSetting extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="device_id",nullable=false) private Device device;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="protocol_type_id",nullable=false) private CommonCode protocolType;
    @Column(nullable=false) private boolean enabled;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="interval_code_id") private CommonCode intervalCode;
    private Integer expectedSilenceSeconds;
    public DeviceCollectionSetting(Device device,CommonCode protocol) {
        if(!"PROTOCOL_TYPE".equals(protocol.getCodeGroup().getGroupKey())) throw new IllegalArgumentException("PROTOCOL_TYPE 코드가 필요합니다.");
        this.device=device;this.protocolType=protocol;
    }
    public void update(boolean enabled,CommonCode interval,Integer silence) {
        if(interval!=null && !"COLLECTION_INTERVAL".equals(interval.getCodeGroup().getGroupKey())) throw new IllegalArgumentException("수집 주기 코드가 필요합니다.");
        this.enabled=enabled;this.intervalCode=interval;this.expectedSilenceSeconds=silence;
    }
}

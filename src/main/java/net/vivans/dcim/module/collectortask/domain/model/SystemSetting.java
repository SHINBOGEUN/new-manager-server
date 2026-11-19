package net.vivans.dcim.module.collectortask.domain.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;
@Entity @Table(name="system_setting") @Getter @NoArgsConstructor
public class SystemSetting extends BaseEntity {
    @Id @Column(name="setting_key",length=100) private String key;
    @Column(name="setting_value",nullable=false,length=500) private String value;
    @Column(length=255) private String description;
    public SystemSetting(String key,String value) { this.key=key;this.value=value; }
    public void setValue(String value) { this.value=value; }
}

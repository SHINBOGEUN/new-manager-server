package net.vivans.dcim.module.collectortask.domain.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import net.vivans.dcim.module.collectortask.domain.model.SystemSetting;
public interface SystemSettingRepository extends JpaRepository<SystemSetting,String> {}

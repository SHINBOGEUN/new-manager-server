package net.vivans.dcim.module.collectortask.application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.vivans.dcim.module.collectortask.domain.model.SystemSetting;
import net.vivans.dcim.module.collectortask.domain.repository.SystemSettingRepository;
@Service @RequiredArgsConstructor @Slf4j @Transactional(readOnly=true)
public class SystemSettingService {
    public static final String AUTO="collection.auto.enabled", DEFAULT_INTERVAL="collection.default.interval";
    private final SystemSettingRepository repository;
    public String getString(String key) {
        return repository.findById(key).map(SystemSetting::getValue).orElseGet(()->{log.warn("system setting missing: {}",key);return null;});
    }
    public boolean getBoolean(String key) { return "true".equalsIgnoreCase(getString(key)); }
    public boolean autoEnabled() { return getBoolean(AUTO); }
    @Transactional public void put(String key,String value) {
        SystemSetting row=repository.findById(key).orElseGet(()->new SystemSetting(key,value));row.setValue(value);repository.save(row);
    }
}

package net.vivans.dcim.module.live.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "live.telemetry")
public class LiveTelemetryProperties {

    /**
     * 선택 후 자동 중지까지 분. 0 이하면 제한 없음.
     */
    private int sessionTimeoutMinutes = 30;
}

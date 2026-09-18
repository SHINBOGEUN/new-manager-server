package net.vivans.dcim.module.query.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 수집 상태 판정 기준. 수집 주기(cron)에 배수를 곱해 허용 지연·실패 기준을 만든다.
 * 프로토콜과 무관하게 같은 기준을 쓴다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "collection.status")
public class CollectionStatusProperties {

    /** cron 해석이 불가능할 때 사용할 기본 수집 주기(초). */
    private long defaultIntervalSeconds = 900;

    /** 수집 주기 × 이 배수를 넘기면 '지연'으로 본다. */
    private int staleMultiplier = 2;

    /** 지연 판정의 최소 기준(초). 주기가 짧아도 이 시간까지는 정상으로 본다. */
    private long minStaleSeconds = 300;

    /** 수집 주기 × 이 배수를 넘기면 과거 저장값이 남아 있어도 '응답 없음'으로 본다. */
    private int failureMultiplier = 6;

    /** 실패 판정의 최소 기준(초). */
    private long minFailureSeconds = 1800;
}

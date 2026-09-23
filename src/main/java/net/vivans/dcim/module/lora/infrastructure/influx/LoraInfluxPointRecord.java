package net.vivans.dcim.module.lora.infrastructure.influx;

import java.time.Instant;

/** protocol=mqtt 포인트의 point_name별 마지막 값. deviceId는 호출 측 Map의 key로 별도 보관한다. */
public record LoraInfluxPointRecord(String pointName, Double value, Instant time) {
}

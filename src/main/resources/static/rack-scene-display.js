export function rackTemperatureColor(value, warning = 25, danger = 30) {
  if (value == null || !Number.isFinite(Number(value))) return null;
  return Number(value) >= danger ? '#f05b57' : Number(value) > warning ? '#f1c450' : '#4cd47b';
}

export async function readRackTemperatureHumidity(layout, fetchPoint) {
  const ids = [...new Set((layout?.placements || [])
    .filter((item) => item.sceneModelKey === 'SR_DRAGINO_TH' && item.deviceId != null)
    .map((item) => Number(item.deviceId)).filter((id) => Number.isSafeInteger(id) && id > 0))];
  const entries = await Promise.all(ids.map(async (id) => {
    const results = await Promise.allSettled(['TEMPERATURE', 'HUMIDITY'].map((point) => fetchPoint(id, point)));
    return [id, results.map((result) => result.status === 'fulfilled' ? result.value?.value ?? null : null)];
  }));
  return Object.fromEntries(entries);
}

export function temperatureHumidityLines(values) {
  return [0, 1].map((index) => {
    const value = values?.[index];
    const number = value == null || value === '' ? NaN : Number(value);
    return `${Number.isFinite(number) ? number.toLocaleString('ko-KR', { maximumFractionDigits: 1 }) : '—'}${index === 0 ? '°C' : '%'}`;
  });
}
export function rackDisplayLines(display) {
  const lines = (display?.labels || []).map((reading) => {
    const value = reading.value == null || !Number.isFinite(Number(reading.value)) ? '—' : Number(reading.value).toLocaleString('ko-KR', {maximumFractionDigits:2});
    const label = [reading.deviceName, reading.source?.pointName || '측정값'].filter(Boolean).join(' · ');
    return `${label}  ${value}${reading.unit ? ` ${reading.unit}` : ''}`;
  });
  if (display?.sum) {
    const value = display.sum.value == null || !Number.isFinite(Number(display.sum.value)) ? '—' : Number(display.sum.value).toLocaleString('ko-KR', {maximumFractionDigits:2});
    lines.push(`${display.sum.title || '전력 합계'}  ${value} ${display.sum.unit || 'W'}`);
  }
  return lines;
}

// Keep the configured measurement identity visible while rendering its value separately.
export function rackDisplayRows(display) {
  const format = (reading, fallbackUnit = '') => {
    const value = reading?.value == null || !Number.isFinite(Number(reading.value))
      ? '—' : Number(reading.value).toLocaleString('ko-KR', { maximumFractionDigits: 2 });
    const unit = reading?.unit || fallbackUnit;
    return `${value}${unit ? ` ${unit}` : ''}`;
  };
  const rows = (display?.labels || []).map((reading) => ({
    label: [reading.deviceName, reading.source?.pointName].filter(Boolean).join(' · ') || '측정값',
    value: format(reading)
  }));
  if (display?.sum) rows.push({ label: '', value: format(display.sum, 'W') });
  return rows;
}

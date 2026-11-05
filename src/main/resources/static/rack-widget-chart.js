(function (global) {
  'use strict';

  function prepare(series) {
    const cleaned = series.map((item) => ({ ...item, unit: item.unit || '', samples: (item.samples || [])
      .map((point) => ({ time: Date.parse(point.time), value: point.value == null || point.value === '' ? NaN : Number(point.value) }))
      .filter((point) => Number.isFinite(point.time) && Number.isFinite(point.value)).sort((a, b) => a.time - b.time) }));
    const all = cleaned.flatMap((item) => item.samples);
    if (!all.length) return null;
    const units = [...new Set(cleaned.filter((item) => item.samples.length).map((item) => item.unit))];
    const axes = units.map((unit) => {
      const values = cleaned.filter((item) => item.unit === unit).flatMap((item) => item.samples.map((point) => point.value));
      const low = Math.min(...values), high = Math.max(...values);
      const pad = high === low ? Math.max(Math.abs(high) * .1, 1) : (high - low) * .1;
      return { unit, min: low >= 0 ? Math.max(0, low - pad) : low - pad, max: high + pad };
    });
    return { series: cleaned, axes, start: Math.min(...all.map((p) => p.time)), end: Math.max(...all.map((p) => p.time)) };
  }

  function svgElement(name, attrs = {}, text) {
    const node = document.createElementNS('http://www.w3.org/2000/svg', name);
    for (const [key, value] of Object.entries(attrs)) node.setAttribute(key, String(value));
    if (text != null) node.textContent = text;
    return node;
  }

  function render(container, series, options = {}) {
    const data = prepare(series);
    if (!data) { container.textContent = '선택한 기간에 수집된 데이터가 없습니다.'; return; }
    container.replaceChildren();
    const width = Math.max(240, container.clientWidth || 640), height = Math.max(60, container.clientHeight || 240);
    // Match SVG coordinates to actual pixels so axis text is never stretched or flattened.
    const svg = svgElement('svg', { viewBox: `0 0 ${width} ${height}`, preserveAspectRatio: 'none', role: 'img', 'aria-label': '시간별 측정값 그래프' });
    const left = 55, right = Math.max(left + 60, width - 16 - Math.max(0, data.axes.length - 1) * 58), top = 24, bottom = height - 28;
    const timeRange = Math.max(data.end - data.start, 1);
    const x = (time) => left + (time - data.start) / timeRange * (right - left);
    const y = (point, axis) => bottom - (point.value - axis.min) / (axis.max - axis.min) * (bottom - top);
    data.axes.forEach((axis, index) => {
      const side = index === 0 ? left : right + (index - 1) * 58, anchor = index === 0 ? 'end' : 'start', dx = index === 0 ? -7 : 7;
      {
        svg.append(svgElement('text', { x: side + dx, y: 13, 'text-anchor': anchor, class: 'rack-chart-axis-unit' }, axis.unit || '값'));
        const ticks = height < 150 ? 2 : 4;
        for (let i = 0; i <= ticks; i++) {
          const pos = bottom - i * (bottom - top) / ticks, v = axis.min + i * (axis.max - axis.min) / ticks;
          if (index === 0) svg.append(svgElement('line', { x1: left, x2: right, y1: pos, y2: pos, class: 'rack-chart-grid' }));
          svg.append(svgElement('text', { x: side + dx, y: pos + 4, 'text-anchor': anchor, class: 'rack-chart-tick' }, v.toLocaleString('ko-KR', { maximumFractionDigits: 2 })));
        }
      }
    });
    for (let i = 0; i <= 4; i++) {
      const pos = left + i * (right - left) / 4, time = data.start + timeRange * i / 4;
      svg.append(svgElement('line', { x1: pos, x2: pos, y1: top, y2: bottom, class: 'rack-chart-grid' }));
      svg.append(svgElement('text', { x: pos, y: bottom + 22, 'text-anchor': i === 0 ? 'start' : i === 4 ? 'end' : 'middle', class: 'rack-chart-tick' }, new Date(time).toLocaleString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false })));
    }
    data.series.forEach((item) => {
      const axis = data.axes.find((a) => a.unit === item.unit); if (!axis) return;
      let path = '', previous = null;
      for (const point of item.samples) {
        const split = previous == null || (options.gapMs && point.time - previous > options.gapMs);
        path += `${split ? 'M' : 'L'}${x(point.time).toFixed(2)} ${y(point, axis).toFixed(2)} `; previous = point.time;
      }
      svg.append(svgElement('path', { d: path, stroke: item.color, class: 'rack-chart-series' }));
      if (item.samples.length === 1) svg.append(svgElement('circle', { cx: x(item.samples[0].time), cy: y(item.samples[0], axis), r: 3, fill: item.color }));
    });
    const crosshair = svgElement('line', { y1: top, y2: bottom, class: 'rack-chart-crosshair', visibility: 'hidden' }); svg.append(crosshair);
    const hit = svgElement('rect', { x: left, y: top, width: right - left, height: bottom - top, fill: 'transparent', 'pointer-events': 'all' }); svg.append(hit);
    const tooltip = document.createElement('div'); tooltip.className = 'rack-chart-tooltip'; tooltip.hidden = true;
    const timestamps = [...new Set(data.series.flatMap((item) => item.samples.map((point) => point.time)))].sort((a,b) => a-b);
    hit.addEventListener('pointermove', (event) => {
      const rect = svg.getBoundingClientRect(), px = (event.clientX - rect.left) / rect.width * width;
      const wanted = data.start + Math.max(0, Math.min(1, (px - left) / (right - left))) * timeRange;
      let lo = 0, hi = timestamps.length - 1;
      while (lo < hi) { const mid = Math.floor((lo + hi) / 2); if (timestamps[mid] < wanted) lo = mid + 1; else hi = mid; }
      const time = lo > 0 && Math.abs(timestamps[lo - 1] - wanted) < Math.abs(timestamps[lo] - wanted) ? timestamps[lo - 1] : timestamps[lo];
      crosshair.setAttribute('x1', x(time)); crosshair.setAttribute('x2', x(time)); crosshair.setAttribute('visibility', 'visible');
      tooltip.replaceChildren(); const title = document.createElement('strong'); title.textContent = new Date(time).toLocaleString('ko-KR'); tooltip.append(title);
      for (const item of data.series) {
        const point = item.samples.find((sample) => sample.time === time); if (!point) continue;
        const line = document.createElement('div'); const dot = document.createElement('span'); dot.className = 'rack-chart-tooltip-dot'; dot.style.background = item.color;
        line.append(dot, document.createTextNode(`${item.name}: ${point.value.toLocaleString('ko-KR', { maximumFractionDigits: 2 })}${item.unit ? ` ${item.unit}` : ''}`)); tooltip.append(line);
      }
      tooltip.hidden = false; tooltip.style.left = `${Math.max(0, Math.min(container.clientWidth - tooltip.offsetWidth, event.clientX - rect.left + 12))}px`;
      tooltip.style.top = `${Math.max(0, Math.min(container.clientHeight - tooltip.offsetHeight, event.clientY - rect.top + 12))}px`;
    });
    hit.addEventListener('pointerleave', () => { tooltip.hidden = true; crosshair.setAttribute('visibility', 'hidden'); });
    container.append(svg, tooltip);
  }
  const mounted = new WeakMap();
  function dispose(root) {
    const targets = [root, ...Array.from(root.querySelectorAll?.('[data-rack-chart]') || [])];
    for (const target of targets) {
      const current = mounted.get(target); current?.disconnect(); mounted.delete(target);
    }
  }
  function mount(container, series, options = {}) {
    dispose(container);
    render(container, series, options);
    if (typeof ResizeObserver === 'undefined') return;
    let width = container.clientWidth, height = container.clientHeight;
    const observer = new ResizeObserver(() => {
      if (!container.isConnected) return;
      if (container.clientWidth === width && container.clientHeight === height) return;
      width = container.clientWidth; height = container.clientHeight;
      if (width && height) render(container, series, options);
    });
    observer.observe(container); mounted.set(container, observer);
  }
  global.RackWidgetChart = { prepare, mount, dispose };
})(globalThis);

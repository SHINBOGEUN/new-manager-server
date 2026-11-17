(function (global) {
  "use strict";

  const CHART_PALETTE = ["#4cc9f0", "#53d6a2", "#ffd166", "#ff8696", "#b59cff", "#80d8cf", "#ffae70", "#b9dd76"];
  const RANGE_OPTIONS = [["last_1h","최근 1시간"],["last_6h","최근 6시간"],["last_24h","최근 24시간"],["today","오늘"],["yesterday","어제"],["last_3d","최근 3일"],["last_7d","최근 7일"],["this_month","이번 달"],["last_month","지난 달"]];
  const SERIES_OPTIONS = [["per_device","장비별"],["sum","합계"],["by_phase","상별"],["by_path","위치별"]];
  const WINDOW_OPTIONS = [["","간격 자동"],["1m","1분"],["5m","5분"],["15m","15분"],["1h","1시간"],["1d","1일"]];
  const STYLE_OPTIONS = [["line","선"],["bar","막대"]];
  const COMPARE_OPTIONS = [["none","비교 안 함"],["today_yesterday","오늘 / 어제"],["week_lastweek","이번 주 / 지난주"],["month_lastmonth","이번 달 / 지난달"]];
  let modal;
  let titleNode;
  let metaNode;
  let chartsNode;
  let controls;
  let advancedControls;
  let rangeSelect;
  let windowSelect;
  let seriesSelect;
  let styleSelect;
  let compareSelect;
  let activeLoad;
  let activeNote = "";
  let activeCharts = [];
  let activeConfig = {};
  let activeShowSeries = true;
  let activeShowCompare = false;

  function formatValue(value) {
    return Number(value).toLocaleString("ko-KR", { maximumFractionDigits: 6 });
  }

  function formatTime(iso) {
    if (!iso) return "";
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return String(iso);
    return date.toLocaleString("ko-KR", {
      month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false,
    });
  }

  function escapeHtml(value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, function (character) {
      return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character];
    });
  }

  function style() {
    if (document.getElementById("dcimWidgetDetailStyle")) return;
    const element = document.createElement("style");
    element.id = "dcimWidgetDetailStyle";
    element.textContent = [
      "#dcimWidgetDetailModal{position:fixed;inset:0;width:min(1120px,96vw);height:min(760px,92vh);max-width:none;max-height:none;margin:auto;padding:18px;background:#171d25;color:#e7edf8;border:1px solid #44536a;border-radius:12px;box-shadow:0 18px 56px rgba(0,0,0,.55)}",
      "#dcimWidgetDetailModal::backdrop{background:rgba(4,8,14,.72)}",
      ".dcim-widget-detail-dialog{height:100%;display:flex;flex-direction:column;min-height:0}",
      ".dcim-widget-detail-head{display:flex;align-items:center;gap:12px;padding-bottom:12px;border-bottom:1px solid #2a3a52}",
      ".dcim-widget-detail-head h2{margin:0;flex:1;font-size:17px}",
      ".dcim-widget-detail-head button{padding:4px 10px;font-size:20px;line-height:1;background:transparent;color:#93a7c1;border-color:transparent}",
      ".dcim-widget-detail-controls{display:flex;gap:8px;flex-wrap:wrap;margin:10px 0}",
      ".dcim-widget-detail-controls select{min-width:120px;max-width:100%;padding:7px 9px;background:#121a29;color:#e7edf8;border:1px solid #34445b;border-radius:7px}",
      ".dcim-widget-detail-advanced{margin:0 0 8px;color:#93a7c1;font-size:12px}",
      ".dcim-widget-detail-advanced summary{cursor:pointer;padding:7px 0}",
      ".dcim-widget-detail-advanced .dcim-widget-detail-controls{margin:4px 0 8px}",
      ".dcim-widget-detail-meta{padding:9px 0;font-size:12px;color:#93a7c1;min-height:32px}",
      ".dcim-widget-detail-chart{position:relative;min-height:0;flex:1;overflow:auto}",
      ".dcim-widget-detail-chart canvas{width:100%!important;height:100%!important}",
      ".dcim-widget-detail-unit-group{height:300px;margin:8px 0}",
      "@media(max-width:760px){#dcimWidgetDetailModal{width:100%;height:92vh;padding:14px}.dcim-widget-detail-controls select{flex:1;min-width:130px}}"
    ].join("\n");
    document.head.appendChild(element);
  }

  function select(id, options) {
    const element = document.createElement("select");
    element.id = id;
    element.setAttribute("aria-label", id);
    options.forEach(function (entry) {
      const option = document.createElement("option");
      option.value = entry[0];
      option.textContent = entry[1];
      element.appendChild(option);
    });
    return element;
  }

  function ensureModal() {
    if (modal) return;
    style();
    modal = document.createElement("dialog");
    modal.id = "dcimWidgetDetailModal";
    modal.setAttribute("aria-modal", "true");
    modal.setAttribute("aria-labelledby", "dcimWidgetDetailTitle");
    modal.innerHTML = '<div class="dcim-widget-detail-dialog"><div class="dcim-widget-detail-head"><h2 id="dcimWidgetDetailTitle"></h2><button type="button" aria-label="닫기">×</button></div><div class="dcim-widget-detail-controls dcim-widget-detail-basic"></div><details class="dcim-widget-detail-advanced"><summary>더 보기</summary><div class="dcim-widget-detail-controls"></div></details><div class="dcim-widget-detail-meta" role="status"></div><div class="dcim-widget-detail-chart"></div></div>';
    document.body.appendChild(modal);
    titleNode = modal.querySelector("#dcimWidgetDetailTitle");
    metaNode = modal.querySelector(".dcim-widget-detail-meta");
    chartsNode = modal.querySelector(".dcim-widget-detail-chart");
    controls = modal.querySelector(".dcim-widget-detail-controls");
    advancedControls = modal.querySelector(".dcim-widget-detail-advanced .dcim-widget-detail-controls");
    rangeSelect = select("dcimDetailRange", RANGE_OPTIONS);
    windowSelect = select("dcimDetailWindow", WINDOW_OPTIONS);
    windowSelect.setAttribute("aria-label", "집계 간격");
    seriesSelect = select("dcimDetailSeries", SERIES_OPTIONS);
    styleSelect = select("dcimDetailStyle", STYLE_OPTIONS);
    compareSelect = select("dcimDetailCompare", COMPARE_OPTIONS);
    controls.append(rangeSelect, compareSelect, windowSelect);
    advancedControls.append(seriesSelect, styleSelect);
    modal.querySelector("button").addEventListener("click", close);
    modal.addEventListener("click", function (event) { if (event.target === modal) close(); });
    modal.addEventListener("close", destroyCharts);
    controls.addEventListener("change", function () { render().catch(showError); });
    advancedControls.addEventListener("change", function () { render().catch(showError); });
    global.addEventListener("keydown", function (event) { if (event.key === "Escape" && modal.open) close(); });
  }

  function destroyCharts() {
    activeCharts.forEach(function (chart) { try { chart.destroy(); } catch (_) {} });
    activeCharts = [];
  }

  function fromCalculated(data) {
    const trend = data && Array.isArray(data.trend) ? data.trend : [];
    const current = trend.filter(function (point) { return !point.compare; });
    const compared = trend.filter(function (point) { return point.compare; });
    const series = [{ key: "calculated", label: "계산 결과", unit: data && data.unit || "값", axis: "left",
      times: current.map(function (point) { return point.time; }),
      values: current.map(function (point) { return point.value; }) }];
    if (compared.length) series.push({ key: "calculated:compare", label: "계산 결과 (" + (compared[0].compareLabel || "비교") + ")",
      unit: data && data.unit || "값", axis: "left", compare: true, offsetSeconds: compared[0].offsetSeconds,
      originalTimes: compared.map(function (point) { return point.originalTime; }),
      times: compared.map(function (point) { return point.time; }),
      values: compared.map(function (point) { return point.value; }) });
    return {
      units: [data && data.unit || "값"],
      series: series,
      dataStatus: data && data.dataStatus,
      calculationStatus: data && data.calculationStatus,
    };
  }

  function close() {
    if (modal && modal.open) modal.close();
    destroyCharts();
  }

  function trendRangeLabel(range) {
    return ({ last_1h:"최근 1시간", last_6h:"최근 6시간", last_24h:"최근 24시간", today:"오늘", yesterday:"어제", last_3d:"최근 3일", last_7d:"최근 7일", this_month:"이번 달", last_month:"지난 달" })[range] || range;
  }

  function trendWindowLabel(windowSize) {
    return ({ "1m":"1분", "5m":"5분", "15m":"15분", "1h":"1시간", "1d":"1일" })[windowSize] || windowSize;
  }

  function normalizeCompare(value, preset) {
    if (value === "previous_day") return "today_yesterday";
    if (value === "previous_week") return "week_lastweek";
    if (value === "previous_period") {
      return preset === "this_month" || preset === "last_month" ? "month_lastmonth"
        : preset === "last_7d" ? "week_lastweek" : "today_yesterday";
    }
    return value || "none";
  }

  function createChart(canvas, data, chartStyle, options) {
    chartStyle = chartStyle || "line";
    if (typeof global.Chart === "undefined" || !data || data.error || !canvas) return null;
    const series = data.series || [];
    const showLegend = !options || options.showLegend !== false;
    if (!series.length) return null;
    const timeSet = new Set();
    series.forEach(function (item) { (item.times || []).forEach(function (time) { if (time) timeSet.add(time); }); });
    const times = Array.from(timeSet).sort();
    const labels = times.map(formatTime);
    const units = data.units || (data.unit ? [data.unit] : []);
    const hasRightAxis = units.length === 2;
    const datasets = series.map(function (item, index) {
      const byTime = new Map((item.times || []).map(function (time, i) { return [time, item.values[i]]; }));
      const baseKey = item.compare && item.key ? item.key.replace(/:compare$/, "") : null;
      const baseIndex = baseKey ? series.findIndex(function (candidate) { return candidate.key === baseKey; }) : -1;
      const color = item.compare ? "#FFC857" : CHART_PALETTE[(baseIndex >= 0 ? baseIndex : index) % CHART_PALETTE.length];
      const baseSeries = baseIndex >= 0 ? series[baseIndex] : null;
      const baseValues = baseSeries ? new Map((baseSeries.times || []).map(function (time, i) { return [formatTime(time), baseSeries.values[i]]; })) : new Map();
      const originalByTime = new Map((item.times || []).map(function (time, i) { return [time, (item.originalTimes || [])[i] || null]; }));
      return {
        label: item.label || item.key || ("series-" + index),
        unit: item.unit || data.unit || "",
        data: times.map(function (time) { const value = byTime.get(time); return value == null ? null : Number(value); }),
        borderColor: color,
        backgroundColor: color + "33",
        fill: false,
        tension: 0,
        pointRadius: times.length > 80 ? 0 : 2,
        borderWidth: item.compare ? 2.5 : 2,
        pointHoverRadius: 4,
        pointHoverBorderWidth: 2,
        pointHoverBackgroundColor: color,
        pointHoverBorderColor: "#ffffff",
        order: 1,
        borderDash: item.compare ? [6, 4] : [],
        originalTimes: times.map(function (time) { return originalByTime.get(time) || null; }),
        baseValues: baseValues,
        isCompare: item.compare === true,
        spanGaps: true,
        yAxisID: hasRightAxis && item.axis === "right" ? "y1" : "y",
      };
    });
    const hasComparison = series.some(function (item) { return item.compare; });
    if (hasComparison && chartStyle !== "bar") {
      const current = datasets.find(function (dataset) { return !dataset.isCompare; });
      if (current) datasets.push({
        type: "bar", label: current.label, data: current.data, yAxisID: current.yAxisID,
        backgroundColor: "#16B8F04D", borderColor: "#16B8F080", borderWidth: 0,
        barPercentage: 1, categoryPercentage: 1, order: 2, visualOnly: true,
      });
    }
    const guide = {
      id: "dcimComparisonGuide",
      afterDatasetsDraw: function (chart) {
        const active = chart.tooltip && chart.tooltip.getActiveElements();
        if (!active || !active.length) return;
        const x = active[0].element.x, ctx = chart.ctx;
        ctx.save(); ctx.strokeStyle = "rgba(198,218,235,.55)"; ctx.setLineDash([3, 4]);
        ctx.beginPath(); ctx.moveTo(x, chart.chartArea.top); ctx.lineTo(x, chart.chartArea.bottom); ctx.stroke(); ctx.restore();
      },
    };
    return new global.Chart(canvas, {
      type: chartStyle === "bar" ? "bar" : "line",
      plugins: [guide],
      data: { labels: labels, datasets: datasets },
      options: {
        animation: false,
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: "index", intersect: false },
        plugins: {
          legend: { display: showLegend && hasComparison, position: "bottom", labels: { color: "#e7edf8", usePointStyle: true, filter: function (item, chartData) { return !chartData.datasets[item.datasetIndex].visualOnly; } } },
          tooltip: { backgroundColor: "#111b2a", titleColor: "#a5bdd1", bodyColor: "#e7edf8", borderColor: "#34445b", borderWidth: 1, padding: 12,
            filter: function (item) { return !item.dataset.visualOnly; },
            callbacks: { title: function (items) {
              if (!items.length) return "";
              const date = new Date(times[items[0].dataIndex]);
              if (Number.isNaN(date.getTime())) return labels[items[0].dataIndex] || "";
              const pad = function (n) { return String(n).padStart(2, "0"); };
              return date.getFullYear() + "-" + pad(date.getMonth()+1) + "-" + pad(date.getDate()) + " " + pad(date.getHours()) + ":" + pad(date.getMinutes()) + ":" + pad(date.getSeconds());
            }, label: function (context) {
            const value = context.parsed.y;
            const unit = context.dataset.unit ? " " + context.dataset.unit : "";
            let text = context.dataset.label + ": " + (value == null ? "-" : formatValue(value)) + unit;
            return text;
          } } },
        },
        scales: {
          x: { ticks: { color: "#8b98a8", maxTicksLimit: 6, maxRotation: 0 }, grid: { color: "#2a3544" } },
          y: { position: "left", title: { display: Boolean(units[0]), text: units[0] || "", color: "#8b98a8" }, ticks: { color: "#8b98a8" }, grid: { color: "#2a3544" } },
          y1: { display: hasRightAxis, position: "right", title: { display: hasRightAxis, text: units[1] || "", color: "#8b98a8" }, ticks: { color: "#8b98a8" }, grid: { drawOnChartArea: false } },
        },
      },
    });
  }

  function setMessage(message) {
    ensureModal();
    metaNode.textContent = message || "";
  }

  function showError(error) {
    if (metaNode) metaNode.textContent = error && error.message ? error.message : String(error || "트렌드 조회 실패");
  }

  async function render() {
    if (!activeLoad) return;
    destroyCharts();
    chartsNode.replaceChildren();
    metaNode.textContent = "데이터를 불러오는 중…";
    rangeSelect.hidden = compareSelect.value !== "none";
    const options = {
      rangePreset: rangeSelect.value,
      window: windowSelect.value,
      seriesMode: seriesSelect.value,
      compare: compareSelect.value,
    };
    const data = await activeLoad(options);
    if (data && data.error) throw new Error(data.error);
    const supportedModes = Array.isArray(data.availableSeriesModes) ? data.availableSeriesModes : ["sum", "per_device"];
    const allowedModes = options.compare && options.compare !== "none" ? ["sum"] : supportedModes;
    Array.from(seriesSelect.options).forEach(function (option) {
      option.hidden = !allowedModes.includes(option.value);
    });
    if (!allowedModes.includes(seriesSelect.value)) seriesSelect.value = allowedModes[0] || "sum";
    const hasData = (data && data.series || []).some(function (item) {
      return (item.values || item.data || []).some(function (value) { return value != null; });
    });
    const status = data.dataStatus || data.collectionStatus;
    function statusSummary(value, calculationStatus) {
      const labels = { NORMAL: "정상", PARTIAL: "일부 지연", PREVIOUS: "새 수집 대기", STALE: "수집 지연", MISSING: "데이터 없음" };
      const parts = [];
      if (value) {
        parts.push(labels[value.status] || value.status);
        if (value.missingSourceCount) parts.push("누락 " + value.missingSourceCount);
        if (value.staleSourceCount) parts.push("지연 " + value.staleSourceCount);
      }
      const calculationLabels = { STALE_DATA: "저장값 오래됨", PREVIOUS_VERSION: "이전 설정 결과 · 새 수집 대기" };
      if (calculationStatus && calculationStatus !== "OK" && calculationLabels[calculationStatus]) {
        parts.push(calculationLabels[calculationStatus]);
      }
      return parts.length ? "수집 상태 " + parts.join(" · ") : "";
    }
    if (!hasData) {
      metaNode.textContent = "이 기간에 데이터가 없습니다 · 기간 바꾸기"
        + (statusSummary(status, data.calculationStatus) ? " · " + statusSummary(status, data.calculationStatus) : "")
        + (activeNote ? " · " + activeNote : "");
      return;
    }
    const units = data.units || (data.unit ? [data.unit] : []);
    const compareLabels = { today_yesterday: "오늘 / 어제", week_lastweek: "이번 주 / 지난주", month_lastmonth: "이번 달 / 지난달" };
    const comparedUsage = options.compare && options.compare !== "none" && (data.series || []).some(function (item) { return item.compare && String(item.label || "").includes("사용량 합계"); });
    const meta = [options.compare && options.compare !== "none" ? compareLabels[options.compare] : trendRangeLabel(options.rangePreset), trendWindowLabel(data.window || options.window)].concat(units).filter(Boolean);
    if (comparedUsage && styleSelect.value !== "bar") meta.push("사용량 비교는 막대 권장");
    if (units.length > 1) meta.push("단위가 달라 합계를 낼 수 없습니다");
    if (statusSummary(status, data.calculationStatus)) meta.push(statusSummary(status, data.calculationStatus));
    if (activeNote) meta.push(activeNote);
    metaNode.textContent = meta.join(" · ");
    const groups = data.unitGroups || [];
    if (groups.length > 2) {
      groups.forEach(function (group, index) {
        const section = document.createElement("section");
        section.className = "dcim-widget-detail-unit-group";
        const heading = document.createElement("strong");
        heading.textContent = group.unit || "단위 없음";
        const canvasWrap = document.createElement("div");
        canvasWrap.style.height = "270px";
        const canvas = document.createElement("canvas");
        canvas.id = "dcimWidgetDetailCanvas" + index;
        canvasWrap.appendChild(canvas);
        section.append(heading, canvasWrap);
        chartsNode.appendChild(section);
        const chart = createChart(canvas, Object.assign({}, data, { unit: group.unit, units: [group.unit], series: group.series }), styleSelect.value);
        if (chart) activeCharts.push(chart);
      });
    } else {
      const canvas = document.createElement("canvas");
      chartsNode.appendChild(canvas);
      const chart = createChart(canvas, data, styleSelect.value);
      if (chart) activeCharts.push(chart);
    }
  }

  async function open(options) {
    ensureModal();
    options = options || {};
    activeLoad = options.load;
    activeConfig = options.config || {};
    activeNote = options.note || "";
    activeShowSeries = options.showSeries !== false;
    activeShowCompare = options.showCompare === true;
    titleNode.textContent = options.title || "트렌드";
    rangeSelect.value = activeConfig.rangePreset || "last_3d";
    seriesSelect.value = activeConfig.seriesMode || "sum";
    styleSelect.value = activeConfig.chartStyle === "area" ? "line" : activeConfig.chartStyle || "line";
    compareSelect.value = normalizeCompare(activeConfig.compare, rangeSelect.value);
    windowSelect.value = "";
    modal.querySelector(".dcim-widget-detail-advanced").open = false;
    seriesSelect.hidden = !activeShowSeries;
    compareSelect.hidden = !activeShowCompare;
    if (!modal.open) modal.showModal();
    await render();
  }

  global.DcimWidgetDetail = {
    open: open,
    close: close,
    createChart: createChart,
    fromCalculated: fromCalculated,
    formatTime: formatTime,
    formatValue: formatValue,
    palette: CHART_PALETTE,
    setMessage: setMessage,
  };
})(window);

const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const html = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/widget-dashboard.html'), 'utf8');

test('analysis only hides the widget main, not analysis or standalone page switcher', () => {
  assert.match(html, /body\.analysis-page-active main:not\(#analysisMain\)\s*\{\s*display:none;/);
  assert.doesNotMatch(html, /body\.analysis-page-active main\s*\{\s*display:none;/);
  assert.doesNotMatch(html, /body\.analysis-page-active \.page-switcher\s*\{\s*display:none;/);
  assert.match(html, /body\.embedded-analysis \.page-switcher\s*\{\s*display:none;/);
});

test('embedded widget board includes Analysis while dedicated analysis stays fixed', async () => {
  const from = html.indexOf('    async function loadDashboardPages()');
  const to = html.indexOf('\n    }', from) + 6;
  assert.ok(from >= 0 && to > from);
  for (const fixed of [false, true]) {
    const controls = new Map();
    const select = { value: '', innerHTML: '', disabled: false, style: {} };
    controls.set('pageCode', select);
    const context = vm.createContext({
      api: async () => [{ code: 'dashboard', name: 'Dashboard' }, { code: 'ANALYSIS', name: 'Analysis' }],
      embeddedInConsole: true, fixedAnalysisPage: fixed, fixedPageView: false,
      boardView: !fixed, state: { editMode: false }, consoleAuth: () => ({ getRole: () => 'ADMIN' }),
      requestedPageCode: fixed ? 'ANALYSIS' : 'dashboard',
      localStorage: { getItem: () => null }, $: id => {
        if (!controls.has(id)) controls.set(id, { style: {}, disabled: false });
        return controls.get(id);
      },
      escapeHtml: s => s, updateDashboardTitle() {},
    });
    vm.runInContext(html.slice(from, to), context);
    await context.loadDashboardPages();
    assert.match(select.innerHTML, /ANALYSIS/);
    assert.equal(select.disabled, fixed);
    if (fixed) assert.doesNotMatch(select.innerHTML, /dashboard/);
    else assert.match(select.innerHTML, /dashboard/);
    assert.equal(controls.get('addWidgetBtn').style.display, fixed ? 'none' : 'inline-flex');
    assert.equal(controls.get('drawerBtn').style.display, fixed ? 'none' : 'inline-flex');
  }
});

test('dashboard to analysis and back restores the board and reloads it', async () => {
  const from = html.indexOf('    async function activateSelectedPage()');
  const to = html.indexOf('    async function loadDashboardPages()', from);
  assert.ok(from >= 0 && to > from);
  const elements = new Map();
  const $ = id => {
    if (!elements.has(id)) elements.set(id, { hidden: false, style: {}, textContent: '', selectedOptions: [{ textContent: '환경' }] });
    return elements.get(id);
  };
  let analysis = false;
  let options = 0, reloads = 0;
  const classes = new Set();
  const context = vm.createContext({ $, isAnalysisPage: () => analysis,
    analysisState: { viewMode: 'trend' },
    document: { body: { classList: { toggle(name, on) { if (on) classes.add(name); else classes.delete(name); } } } },
    stopPolling() {}, async loadAnalysisOptions() { options++; }, async reload() { reloads++; },
  });
  vm.runInContext(html.slice(from, to), context);
  await context.activateSelectedPage();
  assert.equal($('analysisMain').hidden, true);
  analysis = true;
  await context.activateSelectedPage();
  assert.equal($('analysisMain').hidden, false);
  assert.equal(classes.has('analysis-page-active'), true);
  assert.equal(options, 1);
  analysis = false;
  await context.activateSelectedPage();
  assert.equal($('analysisMain').hidden, true);
  assert.equal(classes.has('analysis-page-active'), false);
  assert.equal(reloads, 2);
});

test('board defaults to widgets and dropdown resets trend mode before switching', () => {
  assert.match(html, /viewMode: fixedAnalysisPage \? "trend" : "widgets"/);
  const from = html.indexOf('    $("pageCode").addEventListener("change", () => {');
  const to = html.indexOf('\n    });', from);
  const code = html.slice(from, to + 8);
  assert.match(code, /analysisState\.viewMode = "widgets"/);
  assert.ok(code.indexOf('viewMode = "widgets"') < code.indexOf('activateSelectedPage()'));
});

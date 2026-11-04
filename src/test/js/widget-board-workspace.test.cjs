const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const consoleHtml = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/ops-console.html'), 'utf8');
const boardHtml = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/widget-dashboard.html'), 'utf8');

test('widget actions stay on Widget Board and the old widget menu is removed', () => {
  assert.doesNotMatch(consoleHtml, /data-tab="widgets"/);
  assert.doesNotMatch(consoleHtml, /id="tab-widgets"/);
  assert.match(consoleHtml, /id="widgetEditorSupport" hidden/);
  assert.match(consoleHtml, /document\.body\.append\(\$\("wEditorBackdrop"\), \$\("wEditorCard"\)/);
  assert.match(boardHtml, /data-edit-widget="\$\{w\.id\}"/);
  const from = consoleHtml.indexOf('    function openWidgetEditorForPage(pageCode)');
  const to = consoleHtml.indexOf('    function openCalculatedMetrics()', from);
  assert.ok(from >= 0 && to > from);
  assert.doesNotMatch(consoleHtml.slice(from, to), /goTab\("widgets"\)/);
});

test('widget creation shows all sections together and validates inputs on save', () => {
  const begin = consoleHtml.indexOf('    function showWidgetEditorStep() {');
  const end = consoleHtml.indexOf('    function updateWidgetPagePreview()', begin);
  assert.ok(begin >= 0 && end > begin);
  const create = tag => ({ tag, children: [], dataset: {}, className: '', classList: { contains: () => false, remove() {} },
    append(child) { this.children.push(child); }, addEventListener() {} });
  const stages = [1, 2, 3].map(n => ({ ...create('div'), dataset: { widgetStage: String(n) } }));
  const review = { ...create('div'), dataset: { widgetStage: '3' } };
  const preview = { ...create('section'), dataset: { widgetStage: '3' } };
  const footer = create('footer'), save = { style: {} };
  let grid, details;
  const editor = { dataset: {}, scrollTop: 0,
    querySelector(selector) { return selector === '.widget-form-grid' ? grid : selector === '.widget-page-preview' ? preview : footer; },
    querySelectorAll(selector) { return selector.startsWith(':scope') ? [...stages, review, preview] : [...stages, review, preview]; },
    insertBefore(element) { if (element.className === 'widget-form-grid') grid = element; else details = element; } };
  const context = vm.createContext({ document: { createElement: create }, $: id => ({ wEditorCard: editor, wReview: review, wCreate: save }[id]) });
  vm.runInContext(consoleHtml.slice(begin, end), context);
  context.showWidgetEditorStep();
  assert.equal(grid.children.length, 3);
  for (let i = 0; i < 3; i++) assert.ok(grid.children[i].children.includes(stages[i]));
  assert.ok(details.children.includes(review));
  assert.ok(details.children.includes(preview));
  assert.equal(save.style.display, 'inline-flex');
  const saveHandler = consoleHtml.slice(consoleHtml.indexOf('    $("wCreate").onclick = async () => {'));
  assert.match(saveHandler, /validateWidgetEditorStep\(1\);\s*validateWidgetEditorStep\(2\);/);
  assert.doesNotMatch(consoleHtml, /id="wStepNext"|id="wStepBack"/);
});

test('creating and editing every widget form saves the selected design in the content request', async () => {
  const begin = consoleHtml.indexOf('    function selectedWidgetVisualStyle() {');
  const end = consoleHtml.indexOf('    function updateWidgetPagePreview()', begin);
  const saveBegin = consoleHtml.indexOf('    $("wCreate").onclick = async () => {');
  const saveEnd = consoleHtml.indexOf('    setAuth();', saveBegin);
  for (const kind of ['count', 'cctv', 'psychrometric', 'power_distribution']) {
    for (const editing of [false, true]) {
      const radios = ['classic', 'minimal', 'vivid'].map(value => ({ value, checked: value === 'classic' }));
      const titles = [{ textContent: '' }, { textContent: '' }];
      const inputs = new Proxy({}, { get(target, id) { return target[id] ||= { value: '', style: {}, dataset: {} }; } });
      Object.assign(inputs.wName, { value: '전력 현황' });
      Object.assign(inputs.wKind, { value: kind });
      Object.assign(inputs.wCreatePage, { value: 'dashboard' });
      Object.assign(inputs.wCountMode, { value: 'total' });
      const requests = [], errors = [];
      const context = vm.createContext({ $: id => inputs[id],
        document: { querySelectorAll: selector => selector.includes('wVisualStyle') ? radios : titles,
          querySelector: () => radios.find(radio => radio.checked) },
        state: { editingWidgetId: editing ? 17 : null, editingEnabled: true, editingLayout: { gridX: 4, gridY: 2, w: 8, h: 6 },
          cctvCameraIds: [1], psychrometricTemperatureSources: [{ deviceId: 1, pointName: 'TEMP' }],
          psychrometricHumiditySources: [{ deviceId: 1, pointName: 'HUM' }],
          powerDistributionGroups: [{ name: 'A', color: '#000000', sources: [{ deviceId: 1, pointName: 'W' }] },
            { name: 'B', color: '#ffffff', sources: [{ deviceId: 2, pointName: 'W' }] }] },
        validateWidgetEditorStep() {}, clearWidgetForm() {}, refreshWidgetPages() {},
        toast(message, error) { if (error) errors.push(message); },
        async api(url, options) { requests.push({ url, method: options.method, body: JSON.parse(options.body) }); return { id: 17 }; } });
      vm.runInContext(consoleHtml.slice(begin, end), context);
      context.setWidgetVisualStyle(editing ? 'minimal' : 'vivid');
      assert.equal(titles[0].textContent, '전력 현황');
      vm.runInContext(consoleHtml.slice(saveBegin, saveEnd), context);
      await inputs.wCreate.onclick();
      assert.deepEqual(errors, []);
      assert.equal(requests.length, 1);
      assert.equal(requests[0].method, editing ? 'PUT' : 'POST');
      assert.equal(requests[0].body.visualStyle, editing ? 'minimal' : 'vivid');
      assert.deepEqual(requests[0].body.layout, { gridX: 4, gridY: 2, w: 8, h: 6 });
      context.setWidgetVisualStyle('invalid');
      assert.equal(context.selectedWidgetVisualStyle(), 'classic');
    }
  }
});

test('2D and 3D expose separate scene and widget edit controls with draft protection', () => {
  const from = boardHtml.indexOf('    function createSceneHeader(page, modeLabel, devices) {');
  const to = boardHtml.indexOf('    function clearSceneDraft()', from);
  assert.ok(from >= 0 && to > from);
  for (const mode of ['PLAN_2D', 'SCENE_3D', 'DATA']) {
    let approval = false, sent = [];
    const create = tag => ({ tag, children: [], style: {}, append(...children) { this.children.push(...children); },
      setAttribute() {} });
    const context = vm.createContext({ document: { createElement: create }, consoleAuth: () => ({ getRole: () => 'ADMIN' }),
      state: { pageVisualizationEditMode: false, pageSceneDirty: true }, widgetWorkspaceRequested: false,
      window: { parent: { postMessage: message => sent.push(message) } }, location: { origin: 'http://localhost' },
      confirm: () => approval, renderPageVisualization() {}, clearSceneDraft() {} });
    vm.runInContext(boardHtml.slice(from, to), context);
    const head = context.createSceneHeader({ pageCode: 'ENVIRONMENT', viewMode: mode }, '', []);
    const tools = head.children.at(-1).children;
    const scene = tools.find(button => button.className === 'page-edit-icon');
    const widget = tools.find(button => button.className === 'page-widget-edit');
    assert.equal(!!scene, mode !== 'DATA');
    assert.equal(widget.textContent, '위젯 편집');
    widget.onclick();
    assert.equal(sent.length, 0);
    approval = true; widget.onclick();
    assert.equal(sent[0].type, 'dcim:dashboard:open-widget-editor');
    assert.equal(sent[0].pageCode, 'ENVIRONMENT');
  }
  assert.doesNotMatch(boardHtml, /data-scene-add-widget|data-scene-edit-widgets/);
});

test('saving widget positions keeps widget management open', async () => {
  const from = boardHtml.indexOf('    async function saveLayoutEdits() {');
  const to = boardHtml.indexOf('    function setEditMode(on) {', from);
  assert.ok(from >= 0 && to > from);
  const messages = [], controls = {};
  const widget = { id: 1, _draftLayout: { gridX: 2, gridY: 3, w: 4, h: 5 } };
  const context = vm.createContext({ state: { widgets: [widget], saving: false },
    widgetWorkspaceRequested: true, embeddedInConsole: true,
    window: { parent: { postMessage: value => messages.push(value) } }, location: { origin: 'http://localhost' },
    $: id => controls[id] ||= { disabled: false, value: 'ENVIRONMENT' },
    hasWidgetLayoutChanges: item => !!item._draftLayout,
    api: async () => ({ id: 1, layout: widget._draftLayout }),
    setEditMode: on => { context.editing = on; }, log() {}, renderWidgetList() {}, refreshBoard: async () => {}, alert() {} });
  vm.runInContext(boardHtml.slice(from, to), context);
  await context.saveLayoutEdits();
  assert.equal(context.editing, false);
  assert.equal(context.state.widgets[0].layout.gridX, 2);
  assert.equal(messages.length, 0, 'saving layout should not close widget management');
});

test('hidden widget list and query previews are removed while modal backing fields remain', () => {
  assert.doesNotMatch(consoleHtml, /\b(?:wBody|wLastOut|wOpenEditor|wReload|loadWidgets|renderLastPretty|renderCountPretty|renderChartPretty|renderAggregatePretty|renderPsychrometricPretty|renderPowerDistributionPretty|renderPueResult|formatPowerW)\b/);
  for (const id of ['wPage', 'wCreatePage', 'wEditorCard', 'wEditorBackdrop', 'calculatedMetricCard', 'metricBackdrop', 'wPueDefinition', 'pueResult']) {
    assert.match(consoleHtml, new RegExp(`id="${id}"`));
  }
});

test('deleting a widget still refreshes its page using the retained backing field', async () => {
  const from = consoleHtml.indexOf('    async function deleteWidget(id, name) {');
  const to = consoleHtml.indexOf('    async function loadPueDefinitions() {', from);
  assert.ok(from >= 0 && to > from);
  const requests = [], refreshed = [];
  const context = vm.createContext({ state: { editingWidgetId: null }, confirm: () => true,
    api: async (...args) => { requests.push(args); }, toast() {},
    $: id => { assert.equal(id, 'wPage'); return { value: 'environment' }; },
    refreshWidgetPages: page => refreshed.push(page) });
  vm.runInContext(consoleHtml.slice(from, to), context);
  await context.deleteWidget(7, '온도');
  assert.equal(requests[0][0], '/api/manager/widgets/7');
  assert.equal(requests[0][1].method, 'DELETE');
  assert.deepEqual(refreshed, ['environment']);
});

test('deleting a widget during layout editing preserves other drafts without reloading the board', async () => {
  const from = boardHtml.indexOf('    async function deleteWidget(id, name) {');
  const to = boardHtml.indexOf('    function notifyWidgetChange()', from);
  assert.ok(from >= 0 && to > from);
  const draft = { gridX: 5, gridY: 3, w: 4, h: 4 };
  const state = { editMode: true, saving: false, widgets: [{ id: 7 }, { id: 8, _draftLayout: draft }],
    chartInstances: new Map(), lastByWidget: new Map(), countByWidget: new Map(), chartByWidget: new Map(),
    aggregateByWidget: new Map(), pueByWidget: new Map(), psychrometricByWidget: new Map(), powerDistributionByWidget: new Map() };
  const requests = [], calls = [];
  const context = vm.createContext({ state, confirm: () => true,
    api: async (...args) => requests.push(args), renderWidgetList: () => calls.push('list'),
    refreshBoard: async fetch => calls.push(`refresh:${fetch}`), notifyWidgetChange: () => calls.push('notify'), log() {} });
  vm.runInContext(boardHtml.slice(from, to), context);
  await context.deleteWidget(7, '온도');
  assert.equal(requests[0][0], '/api/manager/widgets/7');
  assert.equal(requests[0][1].method, 'DELETE');
  assert.deepEqual([...state.widgets].map(widget => widget.id), [8]);
  assert.equal(state.widgets[0]._draftLayout, draft);
  assert.ok(calls.includes('refresh:false'));
  assert.ok(calls.includes('notify'));
  assert.equal(state.saving, false);
  assert.equal(state.editMode, true);
});

test('a canceled or failed deletion keeps the widget and other layout drafts', async () => {
  const from = boardHtml.indexOf('    async function deleteWidget(id, name) {');
  const to = boardHtml.indexOf('    function notifyWidgetChange()', from);
  for (const allowed of [false, true]) {
    const widgets = [{ id: 7 }, { id: 8, _draftLayout: { gridX: 4 } }];
    const state = { saving: false, widgets };
    const context = vm.createContext({ state, confirm: () => allowed, renderWidgetList() {},
      api: async () => { throw new Error('서버 오류'); } });
    vm.runInContext(boardHtml.slice(from, to), context);
    if (allowed) await assert.rejects(context.deleteWidget(7, '온도'), /서버 오류/);
    else await context.deleteWidget(7, '온도');
    assert.equal(state.widgets, widgets);
    assert.equal(state.widgets[1]._draftLayout.gridX, 4);
    assert.equal(state.saving, false);
  }
});

test('page device bulk actions select and clear every loaded device without saving', () => {
  const from = consoleHtml.indexOf('    $("pdSelectAll").onclick = () => {');
  const to = consoleHtml.indexOf('    $("pdSave").onclick = async () => {', from);
  assert.ok(from >= 0 && to > from);
  const elements = { pdPage: { value: 'dashboard' }, pdSelectAll: {}, pdClearAll: {} };
  const state = { role: 'ADMIN', pageDeviceLoadedCode: 'dashboard',
    pageDeviceCatalog: [{ id: 1 }, { id: 2 }, { id: 3 }], pageDeviceIds: new Set([1]) };
  let renders = 0, displayRenders = 0;
  const context = vm.createContext({ $: id => elements[id], state,
    renderPageDeviceCandidates() { renders++; },
    renderPageDeviceDisplayEditor() { displayRenders++; } });
  vm.runInContext(consoleHtml.slice(from, to), context);
  elements.pdSelectAll.onclick();
  assert.deepEqual([...state.pageDeviceIds], [1, 2, 3]);
  elements.pdClearAll.onclick();
  assert.equal(state.pageDeviceIds.size, 0);
  assert.equal(renders, 2);
  assert.equal(displayRenders, 2);
});

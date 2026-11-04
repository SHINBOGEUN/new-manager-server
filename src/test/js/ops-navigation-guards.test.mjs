import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync(new URL('../../main/resources/static/ops-console.html', import.meta.url), 'utf8');
const registry = fs.readFileSync(new URL('../../main/resources/static/ops-navigation.js', import.meta.url), 'utf8');
function section(from, to) {
  const start = html.indexOf(from), end = html.indexOf(to, start);
  assert.ok(start >= 0 && end > start, from);
  return html.slice(start, end);
}
const permissions = section('    const ADMIN_ONLY_TABS =', '    function applyRolePermissions()');
const goTabSource = section('    function goTab(name) {', '    function restoreActiveTab() {');
const dirtySource = section('    function hasUnsavedPageDeviceSelection() {', '    function updatePageDeviceSelectionSummary() {');
const pageEvents = section('    $("pdPage").addEventListener("change",', '    $("pdSearch").addEventListener');
const unloadSource = section('    window.addEventListener("beforeunload",', '    function renderWorkspaceNavigation(name) {');
const panelIds = [...html.matchAll(/id="(tab-[^"]+)"/g)].map(match => match[1]);

function navigation({ active = 'device-pages', selected = [1], saved = [1], displayDirty = false,
    accept = false, sceneDirty = false, missing = [] } = {}) {
  const effects = [], confirmations = [];
  const context = vm.createContext({ state: { role: 'ADMIN', token: 'token', pageDeviceLoadedCode: 'dashboard',
    pageDeviceIds: new Set(selected), savedPageDeviceIds: new Set(saved), pageDisplayDirty: displayDirty } });
  context.window = context;
  const embedded = new Set(['dashboard-page', 'environment-page', 'cooling-page', 'analysis', 'live-telemetry', 'mqtt-monitor']);
  const frame = { contentWindow: { dcimSceneHasUnsavedChanges: () => sceneDirty } };
  const panels = new Map(panelIds.filter(id => !missing.includes(id)).map(id => {
    let isActive = id === `tab-${active}`;
    return [id, { id, hidden: !isActive, querySelector: () => frame,
      classList: { contains: () => isActive, toggle: (_, on) => { isActive = on; effects.push(id); } } }];
  }));
  Object.assign(context, {
    $: id => panels.get(id), confirm: message => { confirmations.push(message); return accept; },
    toast: message => effects.push(message),
    document: {
      getElementById: id => panels.get(id),
      querySelector: () => embedded.has(active) ? panels.get(`tab-${active}`) : null,
      querySelectorAll: selector => selector === '.panel' ? [...panels.values()]
        : selector === '.embedded-page' ? [...panels.values()].filter(panel => embedded.has(panel.id.slice(4))) : [],
      body: { classList: { toggle() {}, remove() {} } },
    },
    sessionStorage: { setItem: (_, value) => effects.push(`saved:${value}`) },
    closeDeviceDetail() {}, renderWorkspaceNavigation() {}, updatePageContext() {}, scheduleHomeOverviewRefresh() {},
  });
  vm.runInContext(registry + '\n' + permissions + '\n' + dirtySource + '\n' + goTabSource, context);
  return { context, effects, confirmations, panels };
}

test('goTab rejects unknown or missing destinations before navigation changes', () => {
  for (const [name, missing] of [['unknown', []], ['tasks', ['tab-tasks']]]) {
    const result = navigation({ selected: [2], missing });
    assert.equal(result.context.goTab(name), false);
    assert.equal(result.confirmations.length, 0);
    assert.equal(result.effects.length, 1, 'only the error toast should run');
    assert.equal(result.panels.get('tab-device-pages').classList.contains('active'), true);
  }
});

test('canceling navigation preserves page selections and view-mode drafts', () => {
  for (const changes of [{ selected: [2] }, { displayDirty: true }]) {
    const result = navigation(changes);
    assert.equal(result.context.goTab('tasks'), false);
    assert.equal(result.confirmations.length, 1);
    assert.deepEqual(result.effects, []);
    assert.equal(result.panels.get('tab-device-pages').classList.contains('active'), true);
    assert.deepEqual([...result.context.state.pageDeviceIds], changes.selected || [1]);
    assert.equal(result.context.state.pageDisplayDirty, !!changes.displayDirty);
  }
});

test('confirmed or clean navigation proceeds; the current page needs no confirmation', () => {
  for (const options of [{ selected: [2], accept: true }, {}]) {
    const result = navigation(options);
    assert.equal(result.context.goTab('tasks'), true);
    assert.equal(result.effects.includes('saved:tasks'), true);
    assert.equal(result.panels.get('tab-tasks').classList.contains('active'), true);
  }
  const current = navigation({ selected: [2] });
  Object.assign(current.context, { fillPageDevicePageSelect() {}, renderPageDeviceCandidates() {},
    $: id => id === 'pdPage' ? { value: 'dashboard' } : current.panels.get(id) });
  assert.equal(current.context.goTab('device-pages'), true);
  assert.equal(current.confirmations.length, 0);
});

test('existing scene guard still cancels before navigation mutations', () => {
  const result = navigation({ active: 'dashboard-page', sceneDirty: true });
  assert.equal(result.context.goTab('tasks'), false);
  assert.equal(result.confirmations.length, 1);
  assert.deepEqual(result.effects, []);
});

test('changing a directly linked page updates the address used by refresh', () => {
  const { context } = navigation({ active: 'analysis' });
  let address = new URL('http://localhost:8080/ops-console.html?embedded=1&page=analysis#workspace');
  Object.assign(context, {
    URL,
    URLSearchParams,
    location: { get href() { return address.href; }, get search() { return address.search; } },
    history: { state: { source: 'test' }, replaceState(state, _, path) {
      assert.equal(state.source, 'test');
      address = new URL(path, address);
    } },
  });
  assert.equal(context.goTab('tasks'), true);
  assert.equal(address.searchParams.get('page'), 'tasks');
  assert.equal(address.searchParams.get('embedded'), '1');
  assert.equal(address.hash, '#workspace');
  let restored;
  context.sessionStorage.getItem = () => 'analysis';
  context.goTab = name => { restored = name; };
  vm.runInContext(section('    function restoreActiveTab() {', '    window.addEventListener("message"') + '\nrestoreActiveTab()', context);
  assert.equal(restored, 'tasks', 'the previous direct link must not pin refresh to analysis');
});

test('canceling navigation keeps the original address', () => {
  const { context } = navigation({ active: 'environment-page', sceneDirty: true });
  let writes = 0;
  Object.assign(context, { URL, location: { href: 'http://localhost:8080/ops-console.html?page=environment-page' },
    history: { replaceState() { writes++; } } });
  assert.equal(context.goTab('tasks'), false);
  assert.equal(writes, 0);
});

test('page switch and reload guard selection and view-mode drafts', () => {
  for (const changes of [{ selected: [2] }, { displayDirty: true }]) {
    const { context } = navigation(changes);
    let accept = false, loads = 0, confirmations = 0;
    const page = { value: 'environment', addEventListener: (_, listener) => { page.change = listener; } };
    const reload = {};
    Object.assign(context, { $: id => id === 'pdPage' ? page : reload,
      confirm: () => { confirmations++; return accept; },
      loadPageDeviceEditor: async () => { loads++; } });
    vm.runInContext(pageEvents, context);
    page.change(); reload.onclick();
    assert.equal(page.value, 'dashboard');
    assert.equal(loads, 0);
    accept = true;
    page.value = 'environment'; page.change(); reload.onclick();
    assert.equal(loads, 2);
    assert.equal(confirmations, 4);
  }
});

test('browser unload guards page drafts and existing scene drafts', () => {
  for (const changes of [{ selected: [2] }, { displayDirty: true }, { sceneDirty: true }, {}]) {
    const { context } = navigation(changes);
    let unload, prevented = false;
    context.addEventListener = (_, callback) => { unload = callback; };
    context.document.querySelectorAll = () => [{ contentWindow: {
      dcimSceneHasUnsavedChanges: () => !!changes.sceneDirty,
    } }];
    vm.runInContext(unloadSource, context);
    unload({ preventDefault() { prevented = true; } });
    assert.equal(prevented, Object.keys(changes).length > 0);
  }
});

test('auth loss immediately hides subnavigation and clears menu search', () => {
  const { context } = navigation();
  const root = { hidden: false }, search = { value: '모델' };
  let refreshed = false;
  context.state.token = '';
  Object.assign(context, {
    $: id => id === 'workspaceContext' ? root : id === 'workspaceSearch' ? search : { style: {} },
    applyRolePermissions() {}, renderWorkspaceSearch() { refreshed = true; },
  });
  vm.runInContext(section('    function setAuth() {', '    function clearAuth() {'), context);
  context.setAuth();
  assert.equal(root.hidden, true);
  assert.equal(search.value, '');
  assert.equal(refreshed, true);
});

test('authorized search results navigate directly and preserve search on canceled navigation', () => {
  const { context } = navigation();
  context.state.role = 'USER';
  const node = () => ({ children: [], hidden: false, append(child) { this.children.push(child); },
    replaceChildren() { this.children = []; }, get childElementCount() { return this.children.length; } });
  const results = node(), search = { value: '모델' }, parent = { dataset: { tab: 'devices' }, hidden: false };
  Object.assign(context, { $: id => id === 'workspaceSearch' ? search : results });
  context.document.createElement = node;
  context.document.querySelectorAll = () => [parent];
  vm.runInContext(section('    function renderWorkspaceSearch() {', '    $("workspaceSearch").addEventListener'), context);
  context.renderWorkspaceSearch();
  assert.deepEqual(results.children.map(button => button.textContent), ['장비 모델']);
  assert.equal(results.children[0].dataset, undefined, 'search buttons must not carry data-tab');
  let destination;
  context.goTab = name => { destination = name; return false; };
  results.children[0].onclick();
  assert.equal(destination, 'models');
  assert.equal(search.value, '모델');
  context.goTab = name => { destination = name; return true; };
  results.children[0].onclick();
  assert.equal(search.value, '');
  assert.equal(results.hidden, true);
  assert.equal(parent.hidden, false);
});

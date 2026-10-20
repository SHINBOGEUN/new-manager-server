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

test('page device bulk actions select and clear every loaded device without saving', () => {
  const from = consoleHtml.indexOf('    $("pdSelectAll").onclick = () => {');
  const to = consoleHtml.indexOf('    $("pdSave").onclick = async () => {', from);
  assert.ok(from >= 0 && to > from);
  const elements = { pdPage: { value: 'dashboard' }, pdSelectAll: {}, pdClearAll: {} };
  const state = { role: 'ADMIN', pageDeviceLoadedCode: 'dashboard',
    pageDeviceCatalog: [{ id: 1 }, { id: 2 }, { id: 3 }], pageDeviceIds: new Set([1]) };
  let renders = 0;
  const context = vm.createContext({ $: id => elements[id], state,
    renderPageDeviceCandidates() { renders++; } });
  vm.runInContext(consoleHtml.slice(from, to), context);
  elements.pdSelectAll.onclick();
  assert.deepEqual([...state.pageDeviceIds], [1, 2, 3]);
  elements.pdClearAll.onclick();
  assert.equal(state.pageDeviceIds.size, 0);
  assert.equal(renders, 2);
});

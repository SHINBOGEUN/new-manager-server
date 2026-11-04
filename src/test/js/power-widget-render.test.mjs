import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync(new URL('../../main/resources/static/widget-dashboard.html', import.meta.url), 'utf8');
const start = html.indexOf('    function renderAggregateBody(agg) {');
const end = html.indexOf('    function widgetKindLabel(kind) {', start);
assert.ok(start >= 0 && end > start);
const context = vm.createContext({
  escapeHtml: value => String(value).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;'),
  formatValue: value => String(value),
  aggregatePresetLabel: value => value,
});
vm.runInContext(html.slice(start,end), context);

test('ranking orders values, limits to ten and escapes device names', () => {
  const devices = Array.from({length: 12}, (_, i) => ({deviceId:i, deviceName:i === 11 ? '<rack>' : `R${i}`, value:i}));
  const output = context.renderAggregateBody({aggregatePreset:'power_rank', rangePreset:'today', unit:'W', devices});
  assert.ok(output.indexOf('&lt;rack&gt;') < output.indexOf('R10'));
  assert.equal((output.match(/class="power-rank-row"/g) || []).length, 10);
  assert.match(output, /width:100%/);
});

test('missing demand remains unknown and has no fabricated timestamp', () => {
  const output = context.renderAggregateBody({aggregatePreset:'demand', unit:'W', value:null});
  assert.match(output, /—/);
  assert.match(output, /완전한 15분 데이터가 없습니다/);
});

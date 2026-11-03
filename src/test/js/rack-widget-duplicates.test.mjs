import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync(new URL('../../main/resources/static/ops-console.html', import.meta.url), 'utf8');
const start = html.indexOf('    function rackWidgetDuplicateMessage(');
const end = html.indexOf('    let rackDetailViewer', start);
assert.ok(start >= 0 && end > start);
const context = vm.createContext({});
vm.runInContext(`${html.slice(start, end)}\nglobalThis.check = rackWidgetDuplicateMessage;`, context);
const a = {deviceId:1, protocol:'snmp', pointName:'WATT'};
const b = {deviceId:2, protocol:'snmp', pointName:'WATT'};
const widget = (title,type,sources,extra={}) => ({title,type,sources,...extra});

test('reports duplicate names even with case and outer whitespace differences', () => {
  assert.match(context.check([widget(' Power ','VALUE',[a]),widget('power','VALUE',[b])]), /이미 존재하는 위젯 이름/);
});
test('reports repeated definitions regardless of title and placement order', () => {
  assert.match(context.check([widget('전력 1','VALUE',[a]),widget('전력 2','VALUE',[a])]), /이미 존재하는 위젯입니다/);
  assert.match(context.check([widget('비율 1','PIE',[a,b]),widget('비율 2','PIE',[b,a])]), /이미 존재하는 위젯입니다/);
});
test('permits distinct types and time ranges, and recognizes default trend settings', () => {
  assert.equal(context.check([widget('현재','VALUE',[a]),widget('비율','PIE',[a])]), '');
  assert.equal(context.check([widget('하루','POWER_TREND',[a,b],{rangeHours:24,intervalMinutes:15}),widget('한 시간','POWER_TREND',[a,b],{rangeHours:1,intervalMinutes:15})]), '');
  assert.match(context.check([widget('하루','POWER_TREND',[a,b],{rangeHours:24,intervalMinutes:15}),widget('기본','POWER_TREND',[a,b])]), /이미 존재하는 위젯입니다/);
});

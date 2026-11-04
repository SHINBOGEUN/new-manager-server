import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync(new URL('../../main/resources/static/ops-console.html', import.meta.url), 'utf8');
const begin = html.indexOf("        const colors = ['#40d6ff'");
const end = html.indexOf('        if (editing) {', begin);
assert.ok(begin >= 0 && end > begin, 'shared rack widget renderer exists');
const source = html.slice(begin, end);
const pdus = [{deviceId:1,pathCode:'A',deviceName:'PDU A'}, {deviceId:2,pathCode:'B',deviceName:'PDU B'}];
const sources = pdus.map((d) => ({deviceId:d.deviceId,deviceName:d.deviceName,pointName:'TOTAL_WT',protocol:'snmp',unit:'W'}));
const context = {pdus, sources, esc:(v) => String(v ?? ''), sourceLabel:(s) => `${s.deviceName || s.deviceId} · ${s.pointName}`, sourceFor:(s) => sources.find((v) => v.deviceId === s?.deviceId && v.pointName === s?.pointName)};
vm.createContext(context);
vm.runInContext(`${source}\nglobalThis.render = renderRackWidget;`, context);
const entry = (id, value, samples=[]) => ({source:{deviceId:id,pointName:'TOTAL_WT',protocol:'snmp'},value,samples});

test('shared renderer displays actual values and path balance percentages', () => {
  const widget = {title:'경로 전력',type:'PATH_BALANCE',sources:[entry(1,0).source,entry(2,5).source]};
  const result = context.render(widget, [entry(1,0),entry(2,5)]);
  assert.match(result, /경로 전력/);
  assert.match(result, />A 경로<.*?>0\.0%\s+·\s+0 W</);
  assert.match(result, />B 경로<.*?>100\.0%\s+·\s+5 W</);
  assert.match(result, />5 W<small>합계/);
  assert.match(result, /conic-gradient/);
});

test('shared renderer keeps missing data separate from zero', () => {
  const widget = {title:'경로',type:'PATH_BALANCE',sources:[entry(1,null).source,entry(2,5).source]};
  const result = context.render(widget, [entry(1,null),entry(2,5)]);
  assert.match(result, /데이터 없음/);
  assert.doesNotMatch(result, /100\.0%/);
});

test('shared renderer includes power trend and calculated total label', () => {
  const widget = {title:'추이',type:'POWER_TREND',intervalMinutes:15,sources:[entry(1,1).source,entry(2,2).source]};
  const t = '2026-11-02T00:00:00Z';
  const result = context.render(widget, [entry(1,1,[{time:t,value:1}]),entry(2,2,[{time:t,value:2}])]);
  assert.match(result, /전체 전력 \(A\+B\)/);
  assert.match(result, /data-rack-chart/);
  assert.match(result, /"unit":"W"/);
});

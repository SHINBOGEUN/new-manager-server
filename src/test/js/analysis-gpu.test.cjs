const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const html = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/widget-dashboard.html'), 'utf8');
test('server scope and GPU scope expose only their own points', () => {
  const begin = html.indexOf('    function analysisScope(device)');
  const end = html.indexOf('    function renderAnalysisDeviceList()', begin);
  const context = vm.createContext({analysisState:{scopes:new Map()}});
  vm.runInContext(html.slice(begin,end),context);
  const device={deviceId:33,points:[{pointName:'power',componentScope:'COMPONENT'},{pointName:'memory_used_GB',componentScope:'DEVICE'}]};
  assert.equal(context.analysisPoints(device).map(p=>p.pointName).join(','),'memory_used_GB');
  context.analysisState.scopes.set(33,'COMPONENT');
  assert.equal(context.analysisPoints(device).map(p=>p.pointName).join(','),'power');
  assert.equal(context.analysisPoints(device,true).length,2);
});
test('normal devices remain DEVICE and GPU-only catalogs default to components', () => {
  const begin=html.indexOf('    function analysisScope(device)'), end=html.indexOf('    function renderAnalysisDeviceList()',begin);
  const context=vm.createContext({analysisState:{scopes:new Map()}});
  vm.runInContext(html.slice(begin,end),context);
  assert.equal(context.analysisScope({deviceId:1,points:[{pointName:'TEMP'}]}),'DEVICE');
  assert.equal(context.analysisScope({deviceId:33,points:[{pointName:'power',componentScope:'COMPONENT'}]}),'COMPONENT');
});
test('component display names reach chart legends', () => {
  const begin=html.indexOf('    function flattenAnalysis('), end=html.indexOf('    function renderAnalysisResults(',begin);
  assert.ok(begin>=0 && end>begin);
  const context=vm.createContext({}); vm.runInContext(html.slice(begin,end),context);
  const rows=context.flattenAnalysis([{deviceId:33,displayName:'서버1',aggregate:'raw',sensors:[{category:'전력',units:[{unit:'W',fields:[{pointName:'power',displayName:'GPU 0 · power',raws:[]}]}]}]}]);
  assert.equal(rows[0].pointName,'GPU 0 · power'); assert.equal(rows[0].unit,'W');
});

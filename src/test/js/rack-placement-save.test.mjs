import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync(new URL('../../main/resources/static/ops-console.html', import.meta.url), 'utf8');
const start = html.indexOf('      const placementKey = (item)');
const end = html.indexOf('      function renderSelectedRackItem()', start);
assert.ok(start >= 0 && end > start);
function setup(item, apiResult = null) {
  const calls = [], applied = [], notifications = [];
  const context = {
    placements:[item], capacity:42, capacityBadge:{textContent:''}, selectedEditor:{isConnected:true}, selectedRackItem:null,
    layout:{rackLocationCode:'RACK1'}, location:{origin:'http://localhost'},
    rackDetailViewer:{applyPlacement:(value) => applied.push({...value})},
    document:{querySelectorAll:() => [{contentWindow:{postMessage:(...args) => notifications.push(args)}}]},
    $:(id) => id === 'rackPreviewModal' ? {classList:{contains:() => true}} : {querySelector:() => null},
    api:async (url, options) => { calls.push({url, ...options}); if (apiResult) throw new Error(apiResult); },
    renderSelectedRackItem:() => {},
  };
  vm.createContext(context);
  vm.runInContext(`${html.slice(start,end)}\nglobalThis.save = saveRackItem;`, context);
  return {context,calls,applied,notifications};
}

test('moving or resizing an installed U device updates in place without reopening the rack', async () => {
  const item = {deviceId:8,mountType:'RACK_U',uPosition:5,uHeight:4};
  const {context,calls,applied,notifications} = setup(item);
  await context.save(item,{uPosition:10,uHeight:6});
  assert.equal(item.lastU,15); assert.equal(context.capacityBadge.textContent,'6 / 42U 사용 · 36U 여유');
  assert.equal(calls[0].url,'/api/manager/assets/8/rack-placement');
  assert.equal(JSON.parse(calls[0].body).uHeight,6);
  assert.equal(applied.length,1); assert.equal(notifications.length,1);
});
test('model-only side PDU keeps exact mirrored position and length', async () => {
  const item = {modelPlacementId:3,mountType:'RACK_SIDE',rackSide:'RIGHT'};
  const {context,calls} = setup(item);
  await context.save(item,{positionX:.97,positionY:.47,positionZ:.92,visualLengthRatio:.73});
  assert.equal(calls[0].method,'PATCH'); assert.equal(item.visualLengthRatio,.73);
  assert.equal(JSON.parse(calls[0].body).positionZ,.92);
});
test('a failed save does not mutate the placement or update the viewer', async () => {
  const item = {deviceId:8,mountType:'RACK_U',uPosition:5,uHeight:4};
  const {context,applied} = setup(item,'이미 장착된 U');
  await assert.rejects(context.save(item,{uPosition:6,uHeight:4}), /이미 장착된 U/);
  assert.equal(item.uPosition,5); assert.equal(applied.length,0);
});

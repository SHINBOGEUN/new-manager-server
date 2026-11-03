import test from 'node:test';
import assert from 'node:assert/strict';
import {doorStateFromValues,readRackDoorState} from '../../main/resources/static/rack-door-attachment.js';
test('contact false is open, true is closed, unknown is never guessed', () => {
 assert.equal(doorStateFromValues([0]),true); assert.equal(doorStateFromValues([1]),false);
 assert.equal(doorStateFromValues([false]),true); assert.equal(doorStateFromValues([true]),false);
 assert.equal(doorStateFromValues([]),null); assert.equal(doorStateFromValues([null]),null);
 assert.equal(doorStateFromValues([1,null]),null); assert.equal(doorStateFromValues([0,null]),true);
});
test('only linked door sensors are queried and request failures stay unknown', async () => {
 const layout={placements:[{sceneModelKey:'SR_DRAGINO_DOOR',deviceId:21},{sceneModelKey:'SR_DRAGINO_TH',deviceId:22},{sceneModelKey:'SR_DRAGINO_DOOR',deviceId:null}]};
 const ids=[]; assert.equal(await readRackDoorState(layout,async id => {ids.push(id);return {value:0};}),true); assert.deepEqual(ids,[21]);
 assert.equal(await readRackDoorState(layout,async () => {throw new Error('offline')}),null);
 assert.equal(await readRackDoorState({placements:[]},async () => {throw new Error('not called')}),null);
});

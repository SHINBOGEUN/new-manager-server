import test from 'node:test';
import assert from 'node:assert/strict';
import * as THREE from '../../main/resources/static/vendor/three/three.module.js';
import { isDoorAttachment, updateDoorAttachmentTransform } from '../../main/resources/static/rack-door-attachment.js';

test('only sensors explicitly positioned on the front door follow it', () => {
 const sensor = {sceneModelKey:'SR_DRAGINO_TH', mountType:'RACK_REAR', positionZ:1};
 assert.ok(isDoorAttachment(sensor));
 assert.ok(isDoorAttachment({...sensor,sceneModelKey:'SR_DRAGINO_DOOR'}));
 assert.equal(isDoorAttachment({...sensor,positionZ:.95}),false);
 assert.equal(isDoorAttachment({...sensor,mountType:'RACK_SIDE'}),false);
 assert.equal(isDoorAttachment({...sensor,sceneModelKey:'SR_PDU_VERTICAL'}),false);
});
test('door attachment follows the hinge and keeps draggable closed-door coordinates', () => {
 const group = new THREE.Group(), pivot = new THREE.Vector3(.5,0,.49);
 const closed = new THREE.Vector3(-.2,1.1,.48), angle=1.2;
 updateDoorAttachmentTransform(group,pivot,angle);
 const actual = closed.clone().applyMatrix4(group.matrixWorld);
 const expected = closed.clone().sub(pivot).applyAxisAngle(new THREE.Vector3(0,1,0),angle).add(pivot);
 assert.ok(actual.distanceTo(expected)<1e-10);
 assert.ok(actual.clone().applyMatrix4(group.matrixWorld.clone().invert()).distanceTo(closed)<1e-10);
 updateDoorAttachmentTransform(group,pivot,0);
 assert.ok(closed.clone().applyMatrix4(group.matrixWorld).distanceTo(closed)<1e-10);
});

import test from 'node:test';
import assert from 'node:assert/strict';
import { allowsInrowInteriorOverlap, fitEquipmentInsideFrame, fitFrameAroundEquipment, inrowFrameScale, isEquipmentFrameKey, isFrameEquipmentItem, resizedInrowFrame } from '../../main/resources/static/inrow-placement.js';
import { prepareShowroomObject, showroomDisplaySize, showroomFramePreviewSize, SHOWROOM_MODELS } from '../../main/resources/static/showroom-models.js';
import { GLTFLoader } from '../../main/resources/static/vendor/three/loaders/GLTFLoader.js';
import * as THREE from '../../main/resources/static/vendor/three/three.module.js';
import { readFile } from 'node:fs/promises';

const box = (minX, minY, minZ, maxX, maxY, maxZ) => ({
  min: { x: minX, y: minY, z: minZ }, max: { x: maxX, y: maxY, z: maxZ },
});
const frame = box(-1.575, 0, -2.175, 1.575, 3.12, 2.175);
const rack = box(-.44, 0, -.9, .44, 3, .9);

test('frame accepts showroom, factory and built-in equipment, but not building parts', () => {
  for (const model of SHOWROOM_MODELS.filter((entry) => !isEquipmentFrameKey(entry.key))) {
    assert.equal(isFrameEquipmentItem({ itemType: 'EQUIPMENT', modelKey: model.key }), true, model.key);
  }
  for (const key of ['UPS', 'CRAC', 'MACHINE', 'CONVEYOR', 'TANK', 'TABLE', 'RACK']) {
    assert.equal(isFrameEquipmentItem({ itemType: 'EQUIPMENT', modelKey: key }), true, key);
  }
  for (const key of ['SR_INROW_MOLD', 'SR_WALL_COIL', 'FLOOR', 'CEILING', 'CEILING_CLEAR', 'WALL_GLASS', 'DOOR', 'WINDOW', 'STAIR', 'PIPE']) {
    assert.equal(isFrameEquipmentItem({ itemType: 'EQUIPMENT', modelKey: key }), false, key);
  }
  assert.equal(isFrameEquipmentItem({ itemType: 'SENSOR', modelKey: 'UPS' }), false);
});

test('all eligible equipment may be placed within the hollow in-row frame', () => {
  for (const frameKey of ['SR_INROW_MOLD', 'SR_WALL_COIL']) for (const key of ['SR_INROW_RACK', 'SR_INROW', 'SR_DLC_CDU', 'SR_IMMERSION', 'UPS', 'CRAC']) {
    assert.equal(allowsInrowInteriorOverlap(key, frameKey, rack, frame, true), true, `${frameKey}/${key}`);
    assert.equal(allowsInrowInteriorOverlap(frameKey, key, frame, rack, true), true, `${frameKey}/${key}`);
  }
});

test('the frame edge, the roof and other racks still block placement', () => {
  assert.equal(allowsInrowInteriorOverlap('SR_DLC_CDU', 'SR_INROW_MOLD', box(1.2, 0, 0, 2, 3, 1), frame, true), false);
  assert.equal(allowsInrowInteriorOverlap('SR_DLC_CDU', 'SR_INROW_MOLD', box(0, 0, 0, .8, 3.5, 1), frame, true), false);
  assert.equal(allowsInrowInteriorOverlap('SR_DLC_CDU', 'SR_RACK', rack, rack, true), false);
  assert.equal(allowsInrowInteriorOverlap('SR_INROW_MOLD', 'SR_INROW_MOLD', frame, frame, true), false);
  assert.equal(allowsInrowInteriorOverlap('SR_WALL_COIL', 'SR_INROW_MOLD', frame, frame, true), false);
  assert.equal(allowsInrowInteriorOverlap('WALL_GLASS', 'SR_INROW_MOLD', rack, frame, false), false);
});

test('a normal rack is reduced and centered within the frame when dropped inside', () => {
  const standardRack = box(-.665, 0, -1.35, .665, 4.5, 1.35);
  const fitted = fitEquipmentInsideFrame({ x: 1.4, z: 1.9 }, standardRack, frame, 12, 12);
  assert.equal(fitted.sizeX, 8);
  const scale = fitted.sizeX / 12;
  const placed = box(fitted.x - .665 * scale, 0, fitted.z - 1.35 * scale,
    fitted.x + .665 * scale, 4.5 * scale, fitted.z + 1.35 * scale);
  assert.equal(allowsInrowInteriorOverlap('SR_RACK', 'SR_INROW_MOLD', placed, frame, true), true);
  assert.equal(fitEquipmentInsideFrame({ x: 4, z: 0 }, standardRack, frame, 12, 12), null);
});

test('a frame dropped onto a normal rack grows enough to enclose it', () => {
  const standardRack = box(-.665, 0, -1.35, .665, 4.5, 1.35);
  const fitted = fitFrameAroundEquipment(frame, standardRack, 12, 12);
  const scaleX = fitted.sizeX / 12, scaleY = fitted.sizeY / 12, scaleZ = fitted.sizeZ / 12;
  const placed = box(frame.min.x * scaleX, 0, frame.min.z * scaleZ,
    frame.max.x * scaleX, frame.max.y * scaleY, frame.max.z * scaleZ);
  assert.equal(allowsInrowInteriorOverlap('SR_INROW_MOLD', 'SR_RACK', placed, standardRack, true), true);
  assert.equal(fitted.sizeX, 12);
  assert.ok(fitted.sizeY > fitted.sizeX);
  assert.equal(fitted.x, 0);
  assert.equal(fitted.z, 0);
});

test('regular +/- scales the whole frame and the separate length control extends it along Z', () => {
  for (const modelKey of ['SR_INROW_MOLD', 'SR_WALL_COIL']) {
  const original = inrowFrameScale({ modelKey, sizeX: 12, sizeY: 12, sizeZ: 12 });
  const grownSize = resizedInrowFrame({ modelKey, sizeX: 12, sizeY: 12, sizeZ: 12 }, 1);
  const grown = inrowFrameScale(grownSize);
  const extendedSize = resizedInrowFrame({ modelKey, sizeX: 12, sizeY: 12, sizeZ: 12 }, 1, 'length');
  const extended = inrowFrameScale(extendedSize);
  const raisedSize = resizedInrowFrame({ modelKey, sizeX: 12, sizeY: 12, sizeZ: 12 }, 1, 'height');
  const raised = inrowFrameScale(raisedSize);
  const widened = inrowFrameScale(resizedInrowFrame({ modelKey, sizeX: 12, sizeY: 12, sizeZ: 12 }, 1, 'width'));
  assert.deepEqual(original, { x: 3, y: 3, z: 3 });
  assert.deepEqual(grown, { x: 3.25, y: 3.25, z: 3.25 });
  assert.deepEqual(extended, { x: 3, y: 3, z: 4 });
  assert.deepEqual(raised, { x: 3, y: 3.25, z: 3 });
  assert.deepEqual(widened, { x: 3.25, y: 3, z: 3 });
  assert.deepEqual(resizedInrowFrame({ modelKey, sizeX: 12, sizeY: 16, sizeZ: 20 }, 1),
    { sizeX: 13, sizeY: 16 * 13 / 12, sizeZ: 20 * 13 / 12 });
  }
  const extendedFrame = box(-1.575, 0, -2.9, 1.575, 3.12, 2.9);
  for (const centerZ of [-1.8, 0, 1.8]) {
    assert.equal(allowsInrowInteriorOverlap('SR_INROW_RACK', 'SR_INROW_MOLD',
      box(-.44, 0, centerZ - .9, .44, 3, centerZ + .9), extendedFrame, true), true);
  }
});

test('every showroom equipment GLB can be fitted into the frame', async () => {
  const loader = new GLTFLoader().register(() => ({ name: 'node-test-textures', loadTexture: () => Promise.resolve(null) }));
  const boundsFor = async (key, sizeX) => {
    const model = SHOWROOM_MODELS.find((entry) => entry.key === key);
    const bytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${model.file}`, import.meta.url));
    const gltf = await loader.parseAsync(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength), '');
    const object = prepareShowroomObject(model, gltf.scene);
    const raw = new THREE.Box3().setFromObject(object).getSize(new THREE.Vector3());
    const display = showroomDisplaySize(model);
    const scale = Math.min(model.width / raw.x, model.height / raw.y, model.depth / raw.z) * sizeX / 4;
    const width = display ? display.width * sizeX / 4 : raw.x * scale;
    const height = display ? display.height * sizeX / 4 : raw.y * scale;
    const depth = display ? display.depth * sizeX / 4 : raw.z * scale;
    return box(-width / 2, 0, -depth / 2, width / 2, height, depth / 2);
  };
  const actualFrame = await boundsFor('SR_INROW_MOLD', 12);
  for (const model of SHOWROOM_MODELS.filter((entry) => !isEquipmentFrameKey(entry.key))) {
    const initial = await boundsFor(model.key, 12);
    const fitted = fitEquipmentInsideFrame({ x: 0, z: 0 }, initial, actualFrame, 12, 12);
    assert.ok(fitted, model.key);
    const actualContent = await boundsFor(model.key, fitted.sizeX);
    assert.equal(allowsInrowInteriorOverlap(model.key, 'SR_INROW_MOLD', actualContent, actualFrame, true), true, model.key);
  }
  const standardRack = await boundsFor('SR_RACK', 12);
  const fittedRack = fitEquipmentInsideFrame({ x: 0, z: 0 }, standardRack, actualFrame, 12, 12);
  assert.ok(fittedRack);
  assert.equal(allowsInrowInteriorOverlap('SR_RACK', 'SR_INROW_MOLD',
    await boundsFor('SR_RACK', fittedRack.sizeX), actualFrame, true), true);
  const fittedFrame = fitFrameAroundEquipment(actualFrame, standardRack, 12, 12);
  const scaleX = fittedFrame.sizeX / 12, scaleY = fittedFrame.sizeY / 12, scaleZ = fittedFrame.sizeZ / 12;
  const enclosingFrame = box(actualFrame.min.x * scaleX, 0, actualFrame.min.z * scaleZ,
    actualFrame.max.x * scaleX, actualFrame.max.y * scaleY, actualFrame.max.z * scaleZ);
  assert.equal(allowsInrowInteriorOverlap('SR_INROW_MOLD', 'SR_RACK', enclosingFrame, standardRack, true), true);
});

test('wall-coil GLB exposes an empty bay, with a deeper bay after length extension', async () => {
  const model = SHOWROOM_MODELS.find((entry) => entry.key === 'SR_WALL_COIL');
  const bytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${model.file}`, import.meta.url));
  const loader = new GLTFLoader().register(() => ({ name: 'node-test-textures', loadTexture: () => Promise.resolve(null) }));
  const gltf = await loader.parseAsync(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength), '');
  const object = prepareShowroomObject(model, gltf.scene);
  assert.equal(object.getObjectByName('BUJINHAM_01'), undefined);
  const shell = object.getObjectByName('Mesh014');
  assert.ok(shell);
  const raw = new THREE.Box3().setFromObject(object).getSize(new THREE.Vector3());
  const shellSize = new THREE.Box3().setFromObject(shell).getSize(new THREE.Vector3());
  const scale = Math.min(model.width / raw.x, model.height / raw.y, model.depth / raw.z) * 3;
  const bay = box(-shellSize.x * scale / 2, 0, -shellSize.z * scale / 2,
    shellSize.x * scale / 2, shellSize.y * scale, shellSize.z * scale / 2);
  const preview = showroomFramePreviewSize(model);
  assert.ok(Math.abs(preview.height * 3 - (bay.max.y - bay.min.y)) < .01);
  const standardRack = box(-.665, 0, -1.35, .665, 4.5, 1.35);
  const fitted = fitEquipmentInsideFrame({ x: 0, z: 0 }, standardRack, bay, 12, 12);
  assert.ok(fitted);
  assert.equal(fitted.sizeX, 10);
  for (const equipment of SHOWROOM_MODELS.filter((entry) => !isEquipmentFrameKey(entry.key))) {
    const modelBytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${equipment.file}`, import.meta.url));
    const loaded = await loader.parseAsync(modelBytes.buffer.slice(modelBytes.byteOffset, modelBytes.byteOffset + modelBytes.byteLength), '');
    const prepared = prepareShowroomObject(equipment, loaded.scene);
    const rawSize = new THREE.Box3().setFromObject(prepared).getSize(new THREE.Vector3());
    const display = showroomDisplaySize(equipment);
    const normalized = Math.min(equipment.width / rawSize.x, equipment.height / rawSize.y, equipment.depth / rawSize.z) * 3;
    const width = display ? display.width * 3 : rawSize.x * normalized;
    const height = display ? display.height * 3 : rawSize.y * normalized;
    const depth = display ? display.depth * 3 : rawSize.z * normalized;
    const original = box(-width / 2, 0, -depth / 2, width / 2, height, depth / 2);
    const equipmentFit = fitEquipmentInsideFrame({ x: 0, z: 0 }, original, bay, 12, 12);
    assert.ok(equipmentFit, equipment.key);
    const ratio = equipmentFit.sizeX / 12;
    const placed = box(-width * ratio / 2, 0, -depth * ratio / 2, width * ratio / 2, height * ratio, depth * ratio / 2);
    assert.equal(allowsInrowInteriorOverlap('SR_WALL_COIL', equipment.key, bay, placed, true), true, equipment.key);
  }
  const extended = box(bay.min.x, 0, bay.min.z * 4 / 3, bay.max.x, bay.max.y, bay.max.z * 4 / 3);
  for (const centerZ of [-1.7, 1.7]) {
    const width = .665 * fitted.sizeX / 12, depth = 1.35 * fitted.sizeX / 12;
    const placed = box(-width, 0, centerZ - depth, width, 4.5 * fitted.sizeX / 12, centerZ + depth);
    assert.equal(allowsInrowInteriorOverlap('SR_WALL_COIL', 'SR_RACK', extended, placed, true), true);
  }
});

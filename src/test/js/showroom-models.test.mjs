import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { prepareShowroomObject, showroomDisplaySize, SHOWROOM_MODELS, setShowroomModels, showroomModelByKey, showroomModelUrl } from '../../main/resources/static/showroom-models.js';
import { GLTFLoader } from '../../main/resources/static/vendor/three/loaders/GLTFLoader.js';
import * as THREE from '../../main/resources/static/vendor/three/three.module.js';

test('each showroom model has a unique key and a complete GLB 2 file', async () => {
  assert.equal(SHOWROOM_MODELS.length, 20);
  assert.equal(new Set(SHOWROOM_MODELS.map((model) => model.key)).size, SHOWROOM_MODELS.length);
  for (const model of SHOWROOM_MODELS) {
    const bytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${encodeURIComponent(model.file)}`, import.meta.url));
    assert.equal(bytes.subarray(0, 4).toString(), 'glTF', model.file);
    assert.equal(bytes.readUInt32LE(4), 2, model.file);
    assert.equal(bytes.readUInt32LE(8), bytes.length, model.file);
    assert.ok(model.width > 0 && model.height > 0 && model.depth > 0);
    assert.equal(showroomModelUrl(model), `/scene-assets/showroom/${encodeURIComponent(model.file)}`);
  }
});

test('in-row frame and distribution panel render as separate 3D assets', async () => {
  const loader = new GLTFLoader().register(() => ({ name: 'node-test-textures', loadTexture: () => Promise.resolve(null) }));
  for (const [file, included, excluded] of [
    ['in_row_frame.glb', 'S_R_05', 'BUJINHAM_01'],
    ['in_row_distribution_panel.glb', 'BUJINHAM_01', 'S_R_05'],
  ]) {
    const bytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${file}`, import.meta.url));
    const buffer = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
    const gltf = await loader.parseAsync(buffer, '');
    assert.ok(gltf.scene.getObjectByName(included), `${file} must contain ${included}`);
    assert.equal(gltf.scene.getObjectByName(excluded), undefined, `${file} must exclude ${excluded}`);
  }
});

test('rack PDU, vertical PDU and GPU server have the expected orientations and visible front details', async () => {
  const loader = new GLTFLoader();
  for (const [key, longAxis, required] of [
    ['SR_PDU_RACK', 'x', 'outlet'],
    ['SR_PDU_VERTICAL', 'y', 'outlet opening'],
    ['SR_GPU_SERVER', 'x', 'vent fin'],
  ]) {
    const model = SHOWROOM_MODELS.find((entry) => entry.key === key);
    const bytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${model.file}`, import.meta.url));
    const gltf = await loader.parseAsync(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength), '');
    const size = new THREE.Box3().setFromObject(gltf.scene).getSize(new THREE.Vector3());
    assert.ok(size[longAxis] > (longAxis === 'x' ? size.y : size.x) * 2, key);
    let found = false;
    gltf.scene.traverse((node) => { if ((node.name || '').replace(/_/g, ' ') === required) found = true; });
    assert.ok(found, `${key} must contain ${required}`);
  }
});

test('Three.js loads the dedicated server rack instead of the cooler model', async () => {
  const rack = SHOWROOM_MODELS.find((model) => model.key === 'SR_RACK');
  const cooler = SHOWROOM_MODELS.find((model) => model.key === 'SR_INROW');
  assert.notEqual(rack.file, cooler.file);
  const bytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${encodeURIComponent(rack.file)}`, import.meta.url));
  const buffer = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
  // Node has no browser image decoder; test the geometry independently of textures.
  const loader = new GLTFLoader().register(() => ({ name: 'node-test-textures', loadTexture: () => Promise.resolve(null) }));
  const gltf = await loader.parseAsync(buffer, '');
  let meshes = 0;
  gltf.scene.traverse((object) => { if (object.isMesh) meshes++; });
  assert.ok(meshes > 0);
  const door = gltf.scene.getObjectByName('Inrow_Cooling_Rack_Front_Door');
  assert.ok(door, 'server rack front door must be separately selectable');
  assert.ok(!new THREE.Box3().setFromObject(door).isEmpty(), 'server rack front door has visible geometry');
  const scene = new THREE.Scene(); scene.add(gltf.scene); scene.updateMatrixWorld(true);
  const doorBox = new THREE.Box3().setFromObject(door), before = doorBox.getCenter(new THREE.Vector3());
  const hinge = new THREE.Group(); hinge.position.set(doorBox.max.x, before.y, before.z); scene.add(hinge);
  scene.updateMatrixWorld(true); hinge.attach(door); hinge.rotation.y = 1.95; scene.updateMatrixWorld(true);
  const after = new THREE.Box3().setFromObject(door).getCenter(new THREE.Vector3());
  assert.ok(after.z > before.z, 'front door must swing outward, toward the front of the rack');
});

test('RDC cabinet removes the unfolded wings and matches the neighboring rack footprint', async () => {
  const model = SHOWROOM_MODELS.find((entry) => entry.key === 'SR_RDC');
  const bytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${model.file}`, import.meta.url));
  const loader = new GLTFLoader().register(() => ({ name: 'node-test-textures', loadTexture: () => Promise.resolve(null) }));
  const gltf = await loader.parseAsync(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength), '');
  const original = new THREE.Box3().setFromObject(gltf.scene).getSize(new THREE.Vector3());
  const cabinet = prepareShowroomObject(model, gltf.scene.clone(true));
  for (let id = 267; id <= 274; id++) assert.equal(cabinet.getObjectByName(`Mesh${id}`), undefined);
  assert.ok(cabinet.getObjectByName('Mesh258'));
  assert.equal(cabinet.rotation.y, -Math.PI / 2);
  const compact = new THREE.Box3().setFromObject(cabinet).getSize(new THREE.Vector3());
  assert.ok(compact.x < original.z);
  assert.ok(Math.abs(compact.z - original.x) < .01);
  const target = showroomDisplaySize(model);
  const wrapper = new THREE.Group();
  wrapper.add(cabinet);
  wrapper.scale.set(target.width / compact.x, target.height / compact.y, target.depth / compact.z);
  wrapper.scale.multiplyScalar(3);
  const bounds = new THREE.Box3().setFromObject(wrapper);
  const placed = bounds.getSize(new THREE.Vector3()).toArray();
  for (const [actual, expected] of [[placed[0], 1.33], [placed[1], 4.5], [placed[2], 2.7]]) {
    assert.ok(Math.abs(actual - expected) < .01, `${actual} should match ${expected}`);
  }
  assert.equal(showroomDisplaySize(SHOWROOM_MODELS.find((entry) => entry.key === 'SR_RACK')), null);
});

test('API catalog replaces the model picker and resolves authenticated files', () => {
  const uploaded = { key: 'USR_123456789012345678901234', label: '테스트 랙', file: 'rack.glb', width: 1, height: 2, depth: 1, url: '/api/manager/scene-models/USR_123456789012345678901234/content?v=1' };
  setShowroomModels([uploaded]);
  assert.equal(showroomModelByKey(uploaded.key), uploaded);
  assert.equal(showroomModelUrl(uploaded), uploaded.url);
  assert.equal(showroomModelByKey('SR_RACK'), undefined);
  setShowroomModels(SHOWROOM_MODELS);
});


test('Dragino examples contain their recognizable sensor and magnet parts', async () => {
  const loader = new GLTFLoader();
  for (const [key, names] of [['SR_DRAGINO_TH', ['white enclosure', 'blue connector']], ['SR_DRAGINO_DOOR', ['wired contact', 'magnet']]]) {
    const model = SHOWROOM_MODELS.find((entry) => entry.key === key);
    assert.equal(model.category, '센서');
    const bytes = await readFile(new URL(`../../main/resources/static/scene-assets/showroom/${model.file}`, import.meta.url));
    const gltf = await loader.parseAsync(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength), '');
    const actual = []; gltf.scene.traverse((node) => actual.push((node.name || '').replace(/_/g, ' ')));
    for (const name of names) assert.ok(actual.includes(name), `${key}: ${name}`);
  }
});

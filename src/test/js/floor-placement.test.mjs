import test from 'node:test';
import assert from 'node:assert/strict';
import { floorRect, floorRectsOverlap, floorBatchPositions, wallBatchPositions, wallBatchGrid, keyboardMoveDelta, snapFloorPosition, wallCornerCandidates } from '../../main/resources/static/floor-placement.js';

test('arrow movement follows the camera direction and keeps the requested step size', () => {
  assert.deepEqual(keyboardMoveDelta('ArrowUp', 0, -1), { x: 0, z: -.1 });
  assert.deepEqual(keyboardMoveDelta('ArrowRight', 0, -1), { x: .1, z: 0 });
  assert.deepEqual(keyboardMoveDelta('ArrowUp', 1, 0, 1), { x: 1, z: 0 });
  assert.deepEqual(keyboardMoveDelta('ArrowRight', 1, 0, 1), { x: -0, z: 1 });
  assert.equal(keyboardMoveDelta('KeyW', 0, -1), null);
});

test('wall drag creates two rows at the actual wall height and enforces the total limit', () => {
  const walls = wallBatchGrid({ x: 0, y: 0, z: 0 }, { x: 10, z: 0 }, 5, 0, 3.5, 2);
  assert.equal(walls.length, 6);
  assert.deepEqual(walls.map((wall) => wall.y), [0, 0, 0, 3.5, 3.5, 3.5]);
  assert.deepEqual(walls.filter((wall) => wall.row === 1).map((wall) => wall.x), [0, 5, 10]);
  assert.equal(wallBatchGrid({ x: 0, y: 0, z: 0 }, { x: 75, z: 0 }, 5, 0, 3.5, 2), null);
});

test('wall drag creates continuous segments in the chosen direction', () => {
  const horizontal = wallBatchPositions({ x: 0, z: 0 }, { x: 10, z: 2 }, 5, 0);
  assert.deepEqual(horizontal.map((point) => point.x), [0, 5, 10]);
  const vertical = wallBatchPositions({ x: 0, z: 0 }, { x: 0, z: 10 }, 5, 90);
  assert.equal(vertical.length, 3);
  assert.ok(Math.abs(vertical[2].z - 10) < 1e-9);
  assert.equal(wallBatchPositions({ x: 0, z: 0 }, { x: 150, z: 0 }, 5, 0), null);
});

test('dragging produces a rectangular grid of five meter tiles in either direction', () => {
  const tiles = floorBatchPositions({ x: 10, z: 10 }, { x: 0, z: 15 }, 5, 5);
  assert.equal(tiles.length, 6);
  assert.ok(tiles.some((tile) => tile.x === 0 && tile.z === 15));
  for (let i = 0; i < tiles.length; i++) for (let j = i + 1; j < tiles.length; j++)
    assert.equal(floorRectsOverlap(floorRect(tiles[i].x, tiles[i].z, 5, 5), floorRect(tiles[j].x, tiles[j].z, 5, 5)), false);
});

test('a drag over the limit is rejected instead of partially installing a floor', () => {
  assert.equal(floorBatchPositions({ x: 0, z: 0 }, { x: 50, z: 50 }, 5, 5), null);
});

test('a moved tile returns to its original footprint without changing size', () => {
  const original = floorRect(0, 0, 4, 4);
  const neighbors = [floorRect(4, 0, 4, 4), floorRect(0, 4, 4, 4)];
  const position = snapFloorPosition(.2, -.1, 4, 4, neighbors, [{ x: 0, z: 0, sizeX: 4, sizeZ: 4 }]);
  assert.deepEqual(position, { x: 0, z: 0 });
  assert.deepEqual(floorRect(position.x, position.z, 4, 4), original);
  assert.ok(neighbors.every((floor) => !floorRectsOverlap(original, floor)));
});

test('a remembered slot occupied by a legacy overlapping tile is not reused', () => {
  const neighbors = [floorRect(0, 3, 4, 4)];
  const slot = { x: 0, z: 0, sizeX: 4, sizeZ: 4 };
  const position = snapFloorPosition(0, 0, 4, 4, neighbors, [slot]);
  assert.notDeepEqual(position, { x: 0, z: 0 });
  assert.equal(floorRectsOverlap(floorRect(position.x, position.z, 4, 4), neighbors[0]), false);
});

test('a tile snaps into an L shaped corner and adjoining edges do not overlap', () => {
  const floors = [floorRect(0, 0, 4, 4), floorRect(4, 0, 4, 4), floorRect(0, 4, 4, 4)];
  const position = snapFloorPosition(3.8, 4.2, 4, 4, floors);
  assert.deepEqual(position, { x: 4, z: 4 });
  assert.ok(floors.every((floor) => !floorRectsOverlap(floorRect(position.x, position.z, 4, 4), floor)));
});

test('an empty canvas allows the first floor at the requested position', () => {
  assert.deepEqual(snapFloorPosition(12.5, -8, 4, 4, []), { x: 12.5, z: -8 });
});

test('rectangular tiles retain their dimensions when snapping beside another floor', () => {
  const neighbor = floorRect(0, 0, 4, 4);
  const position = snapFloorPosition(4.1, 0, 4, 2, [neighbor]);
  assert.deepEqual(position, { x: 4, z: 0 });
  assert.equal(floorRectsOverlap(floorRect(position.x, position.z, 4, 2), neighbor), false);
});

test('a new five meter floor fits against an existing four meter floor', () => {
  const neighbor = floorRect(0, 0, 4, 4);
  const position = snapFloorPosition(4.4, 0, 5, 5, [neighbor]);
  assert.equal(position.x, 4.5);
  assert.equal(floorRectsOverlap(floorRect(position.x, position.z, 5, 5), neighbor), false);
});

test('a vertical glass wall joins either end of a horizontal wall inside the floor', () => {
  const candidates = wallCornerCandidates(2, .11, false, 2, .11, 2, .11);
  const onFloor = candidates.filter(({ x, z }) => x - .11 >= -1e-9 && x + .11 <= 4 + 1e-9
    && z - 2 >= -1e-9 && z + 2 <= 4 + 1e-9);
  assert.equal(onFloor.length, 2);
  assert.ok(onFloor.some(({ x, z }) => Math.abs(x - .11) < 1e-9 && Math.abs(z - 2) < 1e-9));
  assert.ok(onFloor.some(({ x, z }) => Math.abs(x - 3.89) < 1e-9 && Math.abs(z - 2) < 1e-9));
});

test('a horizontal wall joins the ends of a vertical wall with unequal thicknesses', () => {
  const candidates = wallCornerCandidates(.1, 2, true, 2, .1, 2, .11);
  const onFloor = candidates.filter(({ x, z }) => x - 2 >= -1e-9 && x + 2 <= 4 + 1e-9
    && z - .11 >= -1e-9 && z + .11 <= 4 + 1e-9);
  assert.equal(onFloor.length, 2);
  assert.ok(onFloor.every(({ x }) => Math.abs(x - 2) < 1e-9));
});

test('five meter and four meter glass walls meet at a corner without an empty gap', () => {
  const candidates = wallCornerCandidates(0, 0, false, 2.5, .1, 2, .1);
  const corner = candidates.find(({ x, z }) => x > 0 && z > 0);
  const horizontal = { minX: -2.5, maxX: 2.5, minZ: -.1, maxZ: .1 };
  const vertical = { minX: corner.x - .1, maxX: corner.x + .1, minZ: corner.z - 2, maxZ: corner.z + 2 };
  assert.ok(horizontal.maxX >= vertical.minX && vertical.maxX >= horizontal.minX);
  assert.ok(horizontal.maxZ >= vertical.minZ && vertical.maxZ >= horizontal.minZ);
});

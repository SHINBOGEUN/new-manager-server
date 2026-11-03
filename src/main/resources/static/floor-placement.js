// Shared floor geometry rules. Coordinates and dimensions are in scene units.
export const FLOOR_OVERLAP_TOLERANCE = 0.005;

export function keyboardMoveDelta(key, forwardX, forwardZ, distance = .1) {
  if (!["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(key)) return null;
  let x = 0, z = 0;
  if (Math.abs(forwardX) > Math.abs(forwardZ)) x = Math.sign(forwardX) || 1;
  else z = Math.sign(forwardZ) || -1;
  const rightX = -z, rightZ = x;
  if (key === "ArrowUp") return { x: x * distance, z: z * distance };
  if (key === "ArrowDown") return { x: -x * distance, z: -z * distance };
  if (key === "ArrowRight") return { x: rightX * distance, z: rightZ * distance };
  return { x: -rightX * distance, z: -rightZ * distance };
}

export function floorRect(x, z, width, depth) {
  return { minX: x - width / 2, maxX: x + width / 2,
    minZ: z - depth / 2, maxZ: z + depth / 2 };
}

// A drag covers whole tiles starting at the first placement position.
export function floorBatchPositions(start, end, width, depth, limit = 100) {
  const dx = Math.round((end.x - start.x) / width), dz = Math.round((end.z - start.z) / depth);
  if ((Math.abs(dx) + 1) * (Math.abs(dz) + 1) > limit) return null;
  const positions = [];
  for (let ix = 0; ix <= Math.abs(dx); ix++) for (let iz = 0; iz <= Math.abs(dz); iz++)
    positions.push({ x: start.x + ix * Math.sign(dx) * width, z: start.z + iz * Math.sign(dz) * depth });
  return positions;
}

export function wallBatchPositions(start, end, length, rotationY, limit = 30) {
  const radians = rotationY * Math.PI / 180;
  const ux = Math.cos(radians), uz = -Math.sin(radians);
  const steps = Math.round(((end.x - start.x) * ux + (end.z - start.z) * uz) / length);
  if (Math.abs(steps) + 1 > limit) return null;
  return Array.from({ length: Math.abs(steps) + 1 }, (_, index) => ({
    x: start.x + index * Math.sign(steps) * length * ux,
    z: start.z + index * Math.sign(steps) * length * uz,
  }));
}

export function wallBatchGrid(start, end, length, rotationY, height, rows, limit = 30) {
  if (!Number.isInteger(rows) || rows < 1 || !Number.isFinite(height) || height <= 0) return null;
  const positions = wallBatchPositions(start, end, length, rotationY, limit);
  if (!positions || positions.length * rows > limit) return null;
  return Array.from({ length: rows }, (_, row) => positions.map((position) => ({
    ...position, y: Number(start.y || 0) + row * height, row,
  }))).flat();
}

export function floorRectsOverlap(a, b) {
  return Math.min(a.maxX, b.maxX) - Math.max(a.minX, b.minX) > FLOOR_OVERLAP_TOLERANCE
    && Math.min(a.maxZ, b.maxZ) - Math.max(a.minZ, b.minZ) > FLOOR_OVERLAP_TOLERANCE;
}

// The two wall ends meet at a corner; offsets keep their full thickness on
// the same side of the joint, so both outer faces remain inside the floor.
export function wallCornerCandidates(peerX, peerZ, peerAlongZ, peerHalfLength, peerHalfThickness, movingHalfLength, movingHalfThickness) {
  const offsetX = peerAlongZ ? movingHalfLength - peerHalfThickness : peerHalfLength - movingHalfThickness;
  const offsetZ = peerAlongZ ? peerHalfLength - movingHalfThickness : movingHalfLength - peerHalfThickness;
  return [-1, 1].flatMap((sx) => [-1, 1].map((sz) => ({ x: peerX + sx * offsetX, z: peerZ + sz * offsetZ })));
}

export function snapFloorPosition(x, z, width, depth, floors, vacantSlots = []) {
  if (!floors.length) return { x, z };
  const isFree = (candidate) => !floors.some((floor) => floorRectsOverlap(floorRect(candidate.x, candidate.z, width, depth), floor));
  const candidates = [];
  const add = (candidate) => {
    if (Math.hypot(candidate.x - x, candidate.z - z) <= 1.5 && isFree(candidate)) candidates.push(candidate);
  };
  for (const slot of vacantSlots) {
    if (Math.abs(slot.sizeX - width) < FLOOR_OVERLAP_TOLERANCE && Math.abs(slot.sizeZ - depth) < FLOOR_OVERLAP_TOLERANCE) add(slot);
  }
  const xPositions = new Set(), zPositions = new Set();
  for (const floor of floors) {
    for (const value of [floor.minX - width / 2, floor.maxX + width / 2,
      floor.minX + width / 2, floor.maxX - width / 2, (floor.minX + floor.maxX) / 2]) {
      if (Math.abs(value - x) <= 1.5) xPositions.add(value);
    }
    for (const value of [floor.minZ - depth / 2, floor.maxZ + depth / 2,
      floor.minZ + depth / 2, floor.maxZ - depth / 2, (floor.minZ + floor.maxZ) / 2]) {
      if (Math.abs(value - z) <= 1.5) zPositions.add(value);
    }
  }
  for (const candidateX of xPositions) for (const candidateZ of zPositions) add({ x: candidateX, z: candidateZ });
  candidates.sort((a, b) => Math.hypot(a.x - x, a.z - z) - Math.hypot(b.x - x, b.z - z));
  return candidates.length ? { x: candidates[0].x, z: candidates[0].z } : { x, z };
}

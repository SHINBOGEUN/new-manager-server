// The frame is hollow; only an actual equipment item may occupy its interior.
// Whether an item is equipment is decided by the scene, not its model key.
const FRAME_KEYS = new Set(["SR_INROW_MOLD", "SR_WALL_COIL"]);
export const isEquipmentFrameKey = (key) => FRAME_KEYS.has(key);
export const isFrameEquipmentItem = (item) => Boolean(item && ["RACK", "EQUIPMENT"].includes(item.itemType)
  && !isEquipmentFrameKey(item.modelKey)
  && !/^(WALL|DOOR|WINDOW)/.test(String(item.modelKey || ""))
  && !["FLOOR", "CEILING", "CEILING_CLEAR", "LIGHT", "PIPE", "STAIR"].includes(item.modelKey));

const extent = (box, axis) => box.max[axis] - box.min[axis];
const within = (value, min, max) => Math.max(min, Math.min(max, value));

export function fitEquipmentInsideFrame(pointer, equipmentBox, frameBox, currentSizeX, preferredSizeX, margin = .08) {
  if (pointer.x < frameBox.min.x || pointer.x > frameBox.max.x
      || pointer.z < frameBox.min.z || pointer.z > frameBox.max.z) return null;
  const current = Number(currentSizeX) || 4;
  const width = extent(equipmentBox, "x"), height = extent(equipmentBox, "y"), depth = extent(equipmentBox, "z");
  if (width <= 0 || height <= 0 || depth <= 0) return null;
  const maxSize = Math.floor(Math.min(
    Number(preferredSizeX) || current,
    current * (extent(frameBox, "x") - 2 * margin) / width,
    current * (extent(frameBox, "y") - margin) / height,
    current * (extent(frameBox, "z") - 2 * margin) / depth,
  ));
  if (maxSize < 2) return null;
  const scale = maxSize / current;
  const halfX = width * scale / 2, halfZ = depth * scale / 2;
  return {
    sizeX: maxSize,
    x: within(pointer.x, frameBox.min.x + margin + halfX, frameBox.max.x - margin - halfX),
    z: within(pointer.z, frameBox.min.z + margin + halfZ, frameBox.max.z - margin - halfZ),
  };
}

export function fitFrameAroundEquipment(frameBox, equipmentBox, sizeX, sizeZ, margin = .08, quarterTurn = false, sizeY = sizeX) {
  const crossAxis = quarterTurn ? "z" : "x";
  const lengthAxis = quarterTurn ? "x" : "z";
  if (extent(frameBox, crossAxis) <= 0 || extent(frameBox, lengthAxis) <= 0 || extent(frameBox, "y") <= 0) return null;
  const requiredX = Math.ceil(Math.max(Number(sizeX) || 12,
    (Number(sizeX) || 12) * (extent(equipmentBox, crossAxis) + 2 * margin) / extent(frameBox, crossAxis)));
  const requiredY = Math.ceil(Math.max(Number(sizeY) || Number(sizeX) || 12,
    (Number(sizeY) || Number(sizeX) || 12) * (extent(equipmentBox, "y") + margin) / extent(frameBox, "y")));
  const requiredZ = Math.ceil(Math.max(Number(sizeZ) || 12,
    (Number(sizeZ) || 12) * (extent(equipmentBox, lengthAxis) + 2 * margin) / extent(frameBox, lengthAxis)));
  if (requiredX > 32 || requiredY > 64 || requiredZ > 96) return null;
  return { sizeX: requiredX, sizeY: requiredY, sizeZ: requiredZ,
    x: (equipmentBox.min.x + equipmentBox.max.x) / 2,
    z: (equipmentBox.min.z + equipmentBox.max.z) / 2 };
}

// sizeX controls the overall frame scale. sizeY controls height and sizeZ
// controls front-to-back length. Normal +/- preserves the current proportions.
export function inrowFrameScale(item) {
  const uniformScale = (Number(item.sizeX) || 12) / 4;
  return { x: uniformScale, y: (Number(item.sizeY) || Number(item.sizeX) || 12) / 4,
    z: (Number(item.sizeZ) || Number(item.sizeX) || 12) / 4 };
}

export function resizedInrowFrame(item, direction, mode = "uniform") {
  const sizeX = Number(item.sizeX) || 12;
  const sizeY = Number(item.sizeY) || sizeX;
  const sizeZ = Number(item.sizeZ) || sizeX;
  if (mode === "width") {
    return { sizeX: Math.max(2, Math.min(32, sizeX + direction)), sizeY, sizeZ };
  }
  if (mode === "length") {
    return { sizeX, sizeY, sizeZ: Math.max(2, Math.min(96, sizeZ + direction * 4)) };
  }
  if (mode === "height") {
    return { sizeX, sizeY: Math.max(2, Math.min(64, sizeY + direction)), sizeZ };
  }
  const nextX = Math.max(2, Math.min(32, sizeX + direction));
  const nextY = sizeY * nextX / sizeX;
  const nextZ = sizeZ * nextX / sizeX;
  if (nextY < 2 || nextY > 64 || nextZ < 2 || nextZ > 96) return { sizeX, sizeY, sizeZ };
  return { sizeX: nextX, sizeY: nextY, sizeZ: nextZ };
}

export function allowsInrowInteriorOverlap(movingKey, installedKey, moving, installed, contentIsEquipment = false) {
  const movingFrame = isEquipmentFrameKey(movingKey);
  const installedFrame = isEquipmentFrameKey(installedKey);
  if (movingFrame === installedFrame) return false;
  if (!contentIsEquipment) return false;
  const frame = movingFrame ? moving : installed;
  const content = movingFrame ? installed : moving;
  const margin = .04;
  return content.min.x >= frame.min.x + margin
    && content.max.x <= frame.max.x - margin
    && content.min.z >= frame.min.z + margin
    && content.max.z <= frame.max.z - margin
    && content.min.y >= frame.min.y - .06
    && content.max.y <= frame.max.y + .06;
}

export const isDoorAttachment = (item) => ['SR_DRAGINO_TH', 'SR_DRAGINO_DOOR'].includes(item?.sceneModelKey)
  && item.mountType === 'RACK_REAR' && item.positionZ != null && Number(item.positionZ) >= .995;

// Keep placement coordinates in the closed-door rack coordinate system.
export function updateDoorAttachmentTransform(group, pivot, angle) {
  group.rotation.y = angle;
  const cosine = Math.cos(angle), sine = Math.sin(angle);
  group.position.set(pivot.x - (cosine * pivot.x + sine * pivot.z), 0,
    pivot.z - (-sine * pivot.x + cosine * pivot.z));
  group.updateMatrixWorld(true);
}

// Dragino DOOR_CONTACT: false/0 is open and true/1 is closed.
export function doorStateFromValues(values) {
  if (!values.length) return null;
  if (values.some((v) => v === 0 || v === false)) return true;
  if (values.every((v) => v === 1 || v === true)) return false;
  return null;
}

export async function readRackDoorState(layout, fetchPoint) {
  const ids = [...new Set((layout?.placements || []).filter((p) => p.sceneModelKey === 'SR_DRAGINO_DOOR' && p.deviceId != null).map((p) => Number(p.deviceId)).filter((id) => Number.isSafeInteger(id) && id > 0))];
  if (!ids.length) return null;
  const results = await Promise.allSettled(ids.map((id) => fetchPoint(id)));
  return doorStateFromValues(results.map((result) => result.status === 'fulfilled' ? result.value?.value ?? null : null));
}

// The sample contact has one shared plate. Split it into fixed and door halves,
// retaining each side's screws alongside the contact or magnet.
export function prepareDoorSensorParts(object) {
  const plate = object.getObjectByName('mounting_plate');
  const magnet = object.getObjectByName('magnet');
  if (!plate || !magnet) return null;
  const rightPlate = plate.clone();
  rightPlate.name = 'door_mounting_plate';
  const fullWidth = plate.scale.x;
  plate.scale.x = fullWidth / 2; plate.position.x -= fullWidth / 4;
  rightPlate.scale.x = fullWidth / 2; rightPlate.position.x += fullWidth / 4;
  plate.parent.add(rightPlate);
  const movingParts = [magnet, rightPlate];
  object.traverse((node) => {
    if (node.isMesh && /^mounting_screw/.test(node.name) && node.position.x > 0) movingParts.push(node);
  });
  for (const part of movingParts) part.userData.doorSensorMovingPart = true;
  return {movingParts, gap: object.getObjectByName('contact_gap')};
}

export function setDoorSensorPartsAttached(parts, attached) {
  if (!parts) return;
  for (const part of parts.movingParts) part.visible = !attached;
  if (parts.gap) parts.gap.visible = !attached;
}

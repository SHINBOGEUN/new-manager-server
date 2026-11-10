import { showroomModelByKey } from "/showroom-models.js?v=20261109paneledit";

export function sceneRoleForModelKey(modelKey) {
  return showroomModelByKey(modelKey)?.sceneRole || null;
}

export function sceneRoleForItem(item) {
  return item?.modelRole || sceneRoleForModelKey(item?.modelKey);
}

export function isRackItem(item) {
  return item?.itemType === "RACK" || sceneRoleForItem(item) === "RACK";
}

export function isPanelItem(item) {
  return sceneRoleForItem(item) === "PANEL";
}

export function isFrameItem(item) {
  return sceneRoleForItem(item) === "FRAME";
}

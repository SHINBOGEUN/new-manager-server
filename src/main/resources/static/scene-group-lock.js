import { isRackItem } from "/scene-item-role.js?v=20261110roles";

// Update only the requested group; return the number of members changed.
export function setGroupLockState(items, groupKey, locked) {
  if (!groupKey || typeof locked !== "boolean") return 0;
  let changed = 0;
  for (const item of items) {
    if (item.groupKey !== groupKey || Boolean(item.locked) === locked) continue;
    item.locked = locked;
    changed++;
  }
  return changed;
}

export function rackDetailAction(item) {
  if (!isRackItem(item)) return "ignore";
  return item.locked ? "locked" : "open";
}

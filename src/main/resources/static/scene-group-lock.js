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
  if (!item || !(item.itemType === "RACK" || (/(?:^|_)RACK$/.test(String(item.modelKey || "")) && item.modelKey !== "SR_PDU_RACK"))) return "ignore";
  return item.locked ? "locked" : "open";
}

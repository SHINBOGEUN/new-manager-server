import test from 'node:test';
import assert from 'node:assert/strict';
import { setGroupLockState, rackDetailAction } from '../../main/resources/static/scene-group-lock.js';

test('locks and unlocks all members of one group, including mixed states', () => {
  const items = [{ groupKey: 'a', locked: true }, { groupKey: 'a', locked: false },
    { groupKey: 'a' }, { groupKey: 'b', locked: false }, { groupKey: null }];
  assert.equal(setGroupLockState(items, 'a', true), 2);
  assert.deepEqual(items.map((item) => Boolean(item.locked)), [true, true, true, false, false]);
  assert.equal(setGroupLockState(items, 'a', true), 0);
  assert.equal(setGroupLockState(items, 'a', false), 3);
  assert.deepEqual(items.map((item) => Boolean(item.locked)), [false, false, false, false, false]);
  assert.equal(setGroupLockState(items, null, true), 0);
});

test('rack detail shows a lock notice until that rack is unlocked', () => {
  const rack = { itemType: 'EQUIPMENT', modelKey: 'SR_RACK', groupKey: 'a', locked: true };
  assert.equal(rackDetailAction(rack), 'locked');
  setGroupLockState([rack], 'a', false);
  assert.equal(rackDetailAction(rack), 'open');
  assert.equal(rackDetailAction({ itemType: 'EQUIPMENT', modelKey: 'SR_PDU_RACK' }), 'ignore');
  assert.equal(rackDetailAction({ itemType: 'EQUIPMENT', modelKey: 'SR_PDU_VERTICAL' }), 'ignore');
});

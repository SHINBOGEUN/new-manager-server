import test from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const layout = createRequire(import.meta.url)('../../main/resources/static/rack-widget-layout.js');

test('existing half and full width positions convert without overlap', () => {
  const widgets = [{width:1,gridX:0,gridY:0}, {width:1,gridX:1,gridY:0}, {width:2,gridX:0,gridY:1}];
  layout.normalize(widgets);
  assert.deepEqual(widgets.map(layout.rect), [{x:0,y:0,w:6,h:4}, {x:6,y:0,w:6,h:4}, {x:0,y:4,w:12,h:4}]);
  assert.ok(widgets.every((w) => w.gridX === null && w.gridY === null));
  const restored = JSON.parse(JSON.stringify(widgets));
  layout.normalize(restored);
  assert.deepEqual(restored.map(layout.rect), widgets.map(layout.rect));
});

test('resize and movement fit actual rectangular area, including touching edges', () => {
  const a = {layoutX:0,layoutY:0,layoutW:4,layoutH:4};
  const b = {layoutX:4,layoutY:0,layoutW:4,layoutH:4};
  assert.equal(layout.canPlace({x:0,y:0,w:4,h:4}, [a,b], a), true);
  assert.equal(layout.canPlace({x:0,y:0,w:5,h:4}, [a,b], a), false);
  assert.equal(layout.canPlace({x:0,y:4,w:8,h:2}, [a,b], a), true);
  assert.equal(layout.canPlace({x:11,y:0,w:2,h:2}, [a,b], a), false);
  assert.equal(layout.canPlace({x:0,y:191,w:2,h:2}, [a,b], a), false);
  assert.equal(layout.canPlace({x:0,y:0,w:1,h:2}, [a,b], a), false);
});

test('new widgets find available space without moving explicit layouts', () => {
  const fixed = {layoutX:2,layoutY:1,layoutW:5,layoutH:6,width:1};
  const created = {width:1};
  layout.normalize([fixed,created]);
  assert.deepEqual(layout.rect(fixed), {x:2,y:1,w:5,h:6});
  assert.equal(layout.overlaps(layout.rect(fixed),layout.rect(created)), false);
});

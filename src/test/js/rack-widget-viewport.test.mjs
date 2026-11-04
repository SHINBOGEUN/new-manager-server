import test from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const layout = createRequire(import.meta.url)('../../main/resources/static/rack-widget-layout.js');
test('viewport board contains saved widgets without adding rows below the visible area', () => {
 const widgets=[{layoutX:0,layoutY:0,layoutW:6,layoutH:6},{layoutX:0,layoutY:14,layoutW:7,layoutH:5}];
 const original=JSON.stringify(widgets), grid=layout.viewportGrid(widgets,760);
 assert.ok(grid.rows>=19); assert.ok(grid.rows * grid.rowHeight + (grid.rows-1)*grid.gap <= 760.00001);
 assert.equal(JSON.stringify(widgets),original);
 assert.equal(grid.rows,19);
});
test('far existing placements remain in bounds and board still fits viewport', () => {
 const widgets=[{layoutX:0,layoutY:90,layoutW:6,layoutH:6}], grid=layout.viewportGrid(widgets,640);
 assert.equal(grid.rows,96); assert.ok(grid.rowHeight>0);
 assert.ok(grid.rows*grid.rowHeight+(grid.rows-1)*grid.gap <= 640.00001);
});

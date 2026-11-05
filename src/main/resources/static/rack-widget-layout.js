(function (root) {
  'use strict';
  const columns = 12, rows = 192, minSize = 2;
  function valid(rect) {
    return rect && ['x','y','w','h'].every((key) => Number.isInteger(rect[key])) &&
      rect.x >= 0 && rect.y >= 0 && rect.w >= minSize && rect.h >= minSize && rect.x + rect.w <= columns && rect.y + rect.h <= rows;
  }
  function overlaps(a, b) {
    return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
  }
  function rect(widget) { return {x:widget.layoutX, y:widget.layoutY, w:widget.layoutW, h:widget.layoutH}; }
  function canPlace(candidate, widgets, except) {
    return valid(candidate) && widgets.every((widget) => widget === except || !valid(rect(widget)) || !overlaps(candidate, rect(widget)));
  }
  function normalize(widgets) {
    const positioned = [], deferred = [];
    for (const widget of widgets) {
      const current = rect(widget);
      if (valid(current) && positioned.every((p) => !overlaps(current, p))) { positioned.push(current); continue; }
      const legacy = Number.isInteger(widget.gridX) && Number.isInteger(widget.gridY);
      const old = {x:legacy ? widget.gridX * 6 : 0, y:legacy ? widget.gridY * 4 : 0,
        w:legacy ? (widget.width === 2 ? 12 : 6) : ['TREND','POWER_TREND'].includes(widget.type) ? 12 : widget.type === 'VALUE' ? 4 : widget.width === 2 ? 12 : 6,
        h:legacy || !widget.type ? 4 : widget.type === 'VALUE' ? 3 : 8};
      deferred.push({widget, old, hadPosition:Number.isInteger(widget.gridX) && Number.isInteger(widget.gridY)});
    }
    for (const item of deferred) {
      let place = item.hadPosition && valid(item.old) && positioned.every((p) => !overlaps(item.old, p)) ? item.old : null;
      if (!place) for (let y=0;y<=rows-item.old.h && !place;y++) for (let x=0;x<=columns-item.old.w;x++) {
        const candidate={x,y,w:item.old.w,h:item.old.h}; if (positioned.every((p) => !overlaps(candidate,p))) { place=candidate; break; }
      }
      if (!place) throw new Error('위젯을 배치할 공간이 없습니다.');
      Object.assign(item.widget,{layoutX:place.x, layoutY:place.y, layoutW:place.w, layoutH:place.h}); positioned.push(place);
    }
    for (const widget of widgets) { widget.gridX = null; widget.gridY = null; }
    return widgets;
  }
  // Keep existing saved coordinates visible while fitting the board into the available height.
  function viewportGrid(widgets, height) {
    const available = Number.isFinite(height) && height > 0 ? height : 720;
    const bottom = widgets.reduce((max, w) => Math.max(max, Number(w.layoutY || 0) + Number(w.layoutH || 0)), 0);
    const count = Math.min(rows, Math.max(8, Math.floor((available + 4) / 40), bottom));
    const gap = Math.min(4, available / (count * 4));
    return { rows:count, gap, rowHeight:Math.max(1, (available - gap * (count - 1)) / count) };
  }
  const api = { columns, rows, minSize, rect, valid, overlaps, canPlace, normalize, viewportGrid };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.RackWidgetLayout = api;
})(globalThis);

import test from 'node:test';
import assert from 'node:assert/strict';
import '../../main/resources/static/rack-widget-chart.js';

test('chart keeps temperature and humidity on separate unit scales', () => {
 const t='2026-11-03T07:16:30Z';
 const result=globalThis.RackWidgetChart.prepare([
  {name:'온도',unit:'°C',samples:[{time:t,value:25.85},{time:t,value:null}]},
  {name:'습도',unit:'%',samples:[{time:t,value:31.8}]}]);
 assert.deepEqual(result.axes.map(a=>a.unit),['°C','%']);
 assert.equal(result.series[0].samples.length,1);
 assert.equal(result.series[0].samples[0].value,25.85);
 assert.equal(result.series[1].samples[0].value,31.8);
 assert.ok(result.axes.every(a=>a.min<a.max));
});

test('chart does not turn missing or invalid values into zero', () => {
 assert.equal(globalThis.RackWidgetChart.prepare([{unit:'W',samples:[{time:'2026-11-03T07:00:00Z',value:null},{time:'invalid',value:4}]}]),null);
 const result=globalThis.RackWidgetChart.prepare([{unit:'W',samples:[{time:'2026-11-03T07:00:00Z',value:0}]}]);
 assert.equal(result.series[0].samples[0].value,0);
});

test('hover displays only real readings at the closest timestamp', () => {
 class Element {
  constructor(name) { this.name=name; this.children=[]; this.attributes={}; this.listeners={}; this.style={}; this.clientWidth=640; this.clientHeight=280; this.offsetWidth=150; this.offsetHeight=60; }
  setAttribute(key,value) { this.attributes[key]=String(value); }
  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children=[...nodes]; }
  addEventListener(name,callback) { this.listeners[name]=callback; }
  getBoundingClientRect() { return {left:0,top:0,width:640,height:280}; }
 }
 const old=globalThis.document;
 globalThis.document={createElementNS:(_ns,name)=>new Element(name),createElement:(name)=>new Element(name),createTextNode:(text)=>({textContent:text})};
 try {
  const container=new Element('div'), t='2026-11-03T07:00:00Z', t2='2026-11-03T08:00:00Z';
  globalThis.RackWidgetChart.mount(container,[{name:'온도',unit:'°C',color:'#0ff',samples:[{time:t,value:25.5}]},{name:'습도',unit:'%',color:'#f0f',samples:[{time:t2,value:31.8}]}]);
  const [svg,tooltip]=container.children, hit=svg.children.at(-1);
  hit.listeners.pointermove({clientX:65,clientY:100});
  assert.equal(tooltip.hidden,false);
  assert.match(tooltip.children[1].children[1].textContent,/온도: 25.5 °C/);
  assert.equal(tooltip.children.length,2);
  hit.listeners.pointerleave(); assert.equal(tooltip.hidden,true);
 } finally { globalThis.document=old; }
});

import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
const html=fs.readFileSync(new URL('../../main/resources/static/ops-console.html',import.meta.url),'utf8');
const start=html.indexOf('    function restoreActiveTab() {'), end=html.indexOf('\n    window.addEventListener("message"',start);
assert.ok(start>=0 && end>start);
function restore(saved, canAccess=()=>true) {
 let selected;
 const context={sessionStorage:{getItem:()=>saved},document:{querySelectorAll:()=>[{dataset:{tab:'home'}},{dataset:{tab:'environment-page'}},{dataset:{tab:'users'}}],getElementById:(id)=>['tab-home','tab-environment-page','tab-users'].includes(id)?{}:null},canAccessTab:canAccess,goTab:(name)=>{selected=name;}};
 vm.createContext(context); vm.runInContext(html.slice(start,end)+'\nrestoreActiveTab()',context); return selected;
}
test('restores the previous authorized console page',()=>assert.equal(restore('environment-page'),'environment-page'));
test('falls back to home for unknown or unauthorized menus',()=>{assert.equal(restore('nonexistent'),'home');assert.equal(restore('users',(name)=>name!=='users'),'home');});

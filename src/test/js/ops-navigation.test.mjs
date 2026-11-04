import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import '../../main/resources/static/ops-navigation.js';
const navigation=globalThis.OpsNavigation;
const html=readFileSync(new URL('../../main/resources/static/ops-console.html',import.meta.url),'utf8');
test('each workspace destination names a real panel and has one parent',()=>{
 const ids=navigation.groups.flatMap(g=>[...g.items,...(g.advanced||[])].map(i=>i[0]));
 assert.equal(new Set(ids).size,ids.length);
 for(const id of ids){assert.ok(html.includes(`id="tab-${id}"`),id);assert.ok(navigation.labelFor(id),id);}
 const panels=[...html.matchAll(/<section[^>]+id="tab-([^\"]+)"/g)].map(m=>m[1]);
 for(const id of panels)assert.ok(navigation.groupFor(id),`unreachable panel: ${id}`);
});
test('related tasks share a workspace while monitoring remains directly reachable',()=>{
 for(const id of ['devices','wizard','device-groups'])assert.equal(navigation.groupFor(id).key,'devices');
 for(const id of ['collection-status','tasks','lora-endpoints','mqtt-monitor'])assert.equal(navigation.groupFor(id).key,'collection-status');
 assert.equal(navigation.groupFor('environment-page').key,'environment-page');
 assert.equal(navigation.groupFor('missing'),undefined);
});

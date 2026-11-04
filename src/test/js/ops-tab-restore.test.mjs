import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync(new URL('../../main/resources/static/ops-console.html', import.meta.url), 'utf8');
const registry = fs.readFileSync(new URL('../../main/resources/static/ops-navigation.js', import.meta.url), 'utf8');
const start = html.indexOf('    function restoreActiveTab() {');
const end = html.indexOf('\n    window.addEventListener("message"', start);
const permissionStart = html.indexOf('    const ADMIN_ONLY_TABS =');
const permissionEnd = html.indexOf('    function applyRolePermissions()', permissionStart);
assert.ok(start >= 0 && end > start && permissionStart >= 0 && permissionEnd > permissionStart);
const permissions = html.slice(permissionStart, permissionEnd);
const panelIds = [...html.matchAll(/id="(tab-[^"]+)"/g)].map(match => match[1]);

function contextFor(role = 'ADMIN') {
  const context = vm.createContext({ state: { role } });
  context.window = context;
  vm.runInContext(registry + '\n' + permissions, context);
  return context;
}

function restore(saved, { search, role = 'ADMIN', missing = [] } = {}) {
  let selected;
  const context = contextFor(role);
  Object.assign(context, {
    sessionStorage: { getItem: () => saved },
    document: { getElementById: id => panelIds.includes(id) && !missing.includes(id) ? {} : null },
    goTab: name => { selected = name; },
  });
  if (search !== undefined) Object.assign(context, { location: { search }, URLSearchParams });
  vm.runInContext(html.slice(start, end) + '\nrestoreActiveTab()', context);
  return selected;
}

test('restores registry subpages without requiring a sidebar button or location', () => {
  for (const tab of ['environment-page', 'tasks', 'users', 'lora-mqtt-sources', 'notes']) {
    assert.equal(restore(tab), tab);
  }
});

test('validated explicit page takes priority over the saved page', () => {
  assert.equal(restore('tasks', { search: '?page=analysis' }), 'analysis');
  assert.equal(restore('', { search: '?page=analysis' }), 'analysis');
});

test('invalid, unauthorized or missing explicit pages fall back to a valid saved page', () => {
  assert.equal(restore('tasks', { search: '?page=nonexistent' }), 'tasks');
  assert.equal(restore('tasks', { search: '?page=scene-models', role: 'USER' }), 'tasks');
  assert.equal(restore('tasks', { search: '?page=analysis', missing: ['tab-analysis'] }), 'tasks');
  assert.equal(restore('tasks', { search: '?page=__proto__' }), 'tasks');
});

test('unknown, unauthorized and missing saved destinations fall back to home', () => {
  assert.equal(restore('nonexistent'), 'home');
  assert.equal(restore('users', { role: 'USER' }), 'home');
  assert.equal(restore('scene-models', { role: 'GUEST' }), 'home');
  assert.equal(restore('tasks', { missing: ['tab-tasks'] }), 'home');
});

test('scene models are admin-only while Help remains accessible to every role', () => {
  for (const role of ['ADMIN', 'USER', 'GUEST', '']) {
    const context = contextFor(role);
    assert.equal(context.canAccessTab('scene-models'), role === 'ADMIN');
    assert.equal(context.canAccessTab('notes'), true);
  }
});

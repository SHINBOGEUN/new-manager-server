const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const html = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/ops-console.html'), 'utf8');
const from = html.indexOf('$("mqttSourceSave").onclick = async () => {');
const to = html.indexOf('$("loraEpReload").onclick', from);
assert.ok(from >= 0 && to > from);
const save = html.slice(from, to);

function harness(type, id) {
  const values = { mqttSourceName: 'source', mqttSourceHost: 'broker', mqttSourcePort: '1883',
    mqttSourceTopic: 'gpu/+/telemetry', mqttSourceType: type, mqttSourceClientId: '',
    mqttSourceCredentialKey: 'gpu-key', mqttSourceEnabled: 'true' };
  const elements = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, { value }]));
  const calls = [], errors = [];
  const context = vm.createContext({ $: key => elements[key] ||= { value: '' }, state: { editingMqttSourceId: id },
    Number, JSON, clearMqttSourceForm() {}, loadMqttSources: async () => {},
    toast(message, error) { if (error) errors.push(message); },
    api: async (url, options) => calls.push({ url, method: options.method, body: JSON.parse(options.body) }) });
  vm.runInContext(save, context);
  return { elements, calls, errors };
}

test('GPU agent registration uses common source API and AGENT format', async () => {
  const h = harness('AGENT');
  await h.elements.mqttSourceSave.onclick();
  assert.deepEqual(h.errors, []);
  assert.equal(h.calls[0].url, '/api/manager/mqtt/sources');
  assert.equal(h.calls[0].body.sourceType, 'AGENT');
  assert.equal(h.calls[0].body.brokerUrl, 'tcp://broker:1883');
  assert.equal(h.calls[0].body.credentialKey, 'gpu-key');
  assert.equal(h.calls[0].body.password, undefined);
});

test('existing LoRa source update preserves its ID and format', async () => {
  const h = harness('CHIRPSTACK_LORA', 7);
  await h.elements.mqttSourceSave.onclick();
  assert.equal(h.calls[0].url, '/api/manager/mqtt/sources/7');
  assert.equal(h.calls[0].method, 'PUT');
  assert.equal(h.calls[0].body.sourceType, 'CHIRPSTACK_LORA');
});

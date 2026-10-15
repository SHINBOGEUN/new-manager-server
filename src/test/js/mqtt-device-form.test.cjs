const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const html = fs.readFileSync(path.resolve(__dirname, '../../main/resources/static/ops-console.html'), 'utf8');

function handler(start, end) {
  const from = html.indexOf(start);
  const to = html.indexOf(end, from);
  assert.ok(from >= 0 && to > from, 'UI handler must exist');
  return html.slice(from, to);
}

function harness(protocolCode = 'mqtt', modelType = 'GPU_SERVER') {
  const elements = new Map();
  const $ = id => {
    if (!elements.has(id)) elements.set(id, {
      value: '', textContent: '', innerHTML: '', disabled: false,
      style: { display: 'none' }, classList: { add() {}, remove() {} },
    });
    return elements.get(id);
  };
  const values = {
    wzName: 'GPU 1', wzModel: '8', wzProtocol: '80', wzLoc: 'UNASSIGNED',
    wzPort: '161', wzMqttIdentityType: 'MAC', wzMqttExternalId: '9c:6b:00:9b:4b:d7',
    editDevName: 'GPU 1', editDevModel: '8', editDevLoc: 'UNASSIGNED', editDevEnabled: 'true',
  };
  Object.entries(values).forEach(([id, value]) => { $(id).value = value; });
  const calls = [], errors = [], selected = [];
  const context = vm.createContext({
    $, JSON, Number, String, console,
    state: {
      models: [{ id: 8, deviceTypeCode: modelType, protocols: [{ id: 80, protocolTypeId: 10, protocolCode }] }],
      selectedDeviceId: 40, devices: [{ id: 40 }],
    },
    protocolCode: p => p?.protocolCode || '', protocolTypeId: p => p?.protocolTypeId,
    isLoraMqtt: model => model.deviceTypeCode === 'LORA_SENSOR' && protocolCode === 'mqtt',
    confirm: () => true, esc: s => String(s), log() {}, setWizProgress() {}, updateWizardSummary() {},
    toast(message, error) { if (error) errors.push(message); },
    api: async (url, options) => {
      calls.push({ url, method: options.method, body: JSON.parse(options.body) });
      return url.endsWith('device-registrations') ? { device: { id: 40, name: 'GPU 1' } } : { id: 40 };
    },
    loadDevices: async () => {}, loadTasks: async () => {}, refreshDeviceCounts: async () => {},
    paintLocationTree() {}, selectDevice: id => selected.push(id),
    readDeviceMqttIdentities: () => [{ identityType: 'MAC', externalId: 'AA:BB:CC:DD:EE:FF', enabled: true }],
  });
  return { $, calls, errors, selected, context };
}

const wizard = handler('$("wzSubmit").onclick = async () => {', '// sync wizard model');
const save = handler('$("devSave").onclick = async () => {', '$("devDelete").onclick');

test('MQTT registration submits MAC through existing wizard API without host or port', async () => {
  const h = harness();
  vm.runInContext(wizard, h.context);
  await h.$('wzSubmit').onclick();
  assert.deepEqual(h.errors, []);
  assert.equal(h.calls.length, 1);
  assert.equal(h.calls[0].url, '/api/manager/device-registrations');
  assert.deepEqual(h.calls[0].body.device.mqttIdentities, [
    { identityType: 'MAC', externalId: '9c:6b:00:9b:4b:d7', enabled: true },
  ]);
  assert.equal(h.calls[0].body.endpoint, undefined);
  assert.equal(h.calls[0].body.loraIdentity, undefined);
  assert.equal(h.calls[0].body.collectionGroup, undefined);
});

test('LoRa wizard uses the same nested identity field', async () => {
  const h = harness('mqtt', 'LORA_SENSOR');
  h.$('wzMqttIdentityType').value = 'DEV_EUI';
  h.$('wzMqttExternalId').value = '24E124710C123456';
  vm.runInContext(wizard, h.context);
  await h.$('wzSubmit').onclick();
  assert.deepEqual(h.errors, []);
  assert.equal(h.calls[0].body.device.mqttIdentities[0].identityType, 'DEV_EUI');
  assert.equal(h.calls[0].body.endpoint, undefined);
});

test('SNMP registration still requires host and sends IP settings', async () => {
  const h = harness('snmp', 'PDU');
  vm.runInContext(wizard, h.context);
  await h.$('wzSubmit').onclick();
  assert.equal(h.calls.length, 0);
  assert.equal(h.errors.length, 1);
  h.errors.length = 0;
  h.$('wzHost').value = '192.168.0.10';
  await h.$('wzSubmit').onclick();
  assert.deepEqual(h.errors, []);
  assert.equal(h.calls[0].body.endpoint.host, '192.168.0.10');
  assert.equal(h.calls[0].body.device.mqttIdentities, undefined);
});

test('existing device save sends MAC changes to PUT of the same device ID', async () => {
  const h = harness();
  vm.runInContext(save, h.context);
  await h.$('devSave').onclick();
  assert.deepEqual(h.errors, []);
  assert.equal(h.calls.length, 1);
  assert.equal(h.calls[0].method, 'PUT');
  assert.equal(h.calls[0].url, '/api/manager/devices/40');
  assert.equal(h.calls[0].body.mqttIdentities[0].externalId, 'AA:BB:CC:DD:EE:FF');
  assert.deepEqual(h.selected, [40]);
});

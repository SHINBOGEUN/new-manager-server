import test from 'node:test';
import assert from 'node:assert/strict';
import {rackTemperatureColor,rackDisplayLines,rackDisplayRows,readRackTemperatureHumidity,temperatureHumidityLines} from '../../main/resources/static/rack-scene-display.js';
test('temperature color changes only at configured Celsius boundaries', () => {
 assert.equal(rackTemperatureColor(25),'#4cd47b'); assert.equal(rackTemperatureColor(25.1),'#f1c450'); assert.equal(rackTemperatureColor(30),'#f05b57');
 assert.equal(rackTemperatureColor(22,20,24),'#f1c450'); assert.equal(rackTemperatureColor(null),null); assert.equal(rackTemperatureColor(NaN),null);
});

test('rack sensor readings use linked temperature/humidity sensors and tolerate missing data', async () => {
 const calls=[];
 const readings=await readRackTemperatureHumidity({placements:[{sceneModelKey:'SR_DRAGINO_TH',deviceId:20},{sceneModelKey:'SR_DRAGINO_TH',deviceId:20},{sceneModelKey:'SR_DRAGINO_TH',deviceId:null},{sceneModelKey:'SR_DRAGINO_DOOR',deviceId:21}]}, async (id,point)=>{ calls.push([id,point]); if(point==='HUMIDITY') throw new Error('unavailable'); return {value:25.85}; });
 assert.deepEqual(calls,[[20,'TEMPERATURE'],[20,'HUMIDITY']]);
 assert.deepEqual(temperatureHumidityLines(readings[20]),['25.9°C','—%']);
 assert.deepEqual(temperatureHumidityLines([0,31.8]),['0°C','31.8%']);
});
test('only explicitly configured readings are displayed and missing data stays unknown', () => {
 assert.deepEqual(rackDisplayLines(null), []);
 assert.deepEqual(rackDisplayLines({labels:[{source:{pointName:'TEMPERATURE'},value:23.25,unit:'°C'},{source:{pointName:'TOTAL_WT'},value:null,unit:'W'}]}),['TEMPERATURE  23.25 °C','TOTAL_WT  — W']);
});

test('sum appears as one labeled value and missing inputs remain unknown', () => {
 assert.deepEqual(rackDisplayLines({labels:[],sum:{title:'랙 전력',value:4933,unit:'W'}}),['랙 전력  4,933 W']);
 assert.deepEqual(rackDisplayLines({labels:[],sum:{title:'랙 전력',value:null,unit:'W'}}),['랙 전력  — W']);
});

test('3D labels preserve configured sources, values, units and missing readings', () => {
 assert.deepEqual(rackDisplayRows({labels:[{deviceName:'PDU A',source:{pointName:'TOTAL_WT'},value:5,unit:'W'},{source:{pointName:'TEMPERATURE'},value:null,unit:'°C'}],sum:{title:'합계',value:4933,unit:'W'}}),[
  {label:'PDU A · TOTAL_WT',value:'5 W'}, {label:'TEMPERATURE',value:'— °C'}, {label:'',value:'4,933 W'}
 ]);
 assert.deepEqual(rackDisplayRows(null),[]);
});

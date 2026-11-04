// Models used by the showroom; files are kept locally for reliable display.
export const SHOWROOM_MODELS = [
  { key: 'SR_RACK', label: '서버 랙 (전면 도어)', file: 'Inrow_Cooling_Rack.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_DOUBLE_RACK', label: '서버 랙 (양문형)', file: 'rack_double_door.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_RDC', label: 'RDC 장비', file: 'rdc.glb', width: 1.2, height: 2.1, depth: 1.1 },
  { key: 'SR_INROW', label: '인로우 냉각기', file: 'inrow-cooler.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_INROW_MOLD', label: '인로우 프레임', file: 'in_row_frame.glb', width: 1.05, height: 1.05, depth: 1.45 },
  { key: 'SR_INROW_PANEL', label: '인로우 분전반', file: 'in_row_distribution_panel.glb', width: 1, height: 1.77, depth: .4 },
  { key: 'SR_WALL_COIL', label: '월 코일 프레임', file: 'wall_coil_mold.glb', width: 2.8, height: 2.2, depth: 1.6 },
  { key: 'SR_DLC_RACK', label: 'DLC 랙', file: 'DLC_Rack.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_INROW_RACK', label: '인로우 냉각 랙', file: 'Inrow_Cooling_Rack.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_OCP_RACK', label: 'OCP 랙', file: 'OCP_Rack.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_RDC_RACK', label: 'RDC 랙', file: 'RDC_Rack.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_ORV3_RACK', label: 'ORV3 랙', file: 'ORV3_Rack.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_IMMERSION', label: '액침 냉각 설비', file: 'Immersion_Cooling.glb', width: 1.4, height: 1.5, depth: 1.2 },
  { key: 'SR_HAC_RACK', label: 'HAC 랙', file: 'HAC_Rack.glb', width: .9, height: 2.35, depth: .9 },
  { key: 'SR_DLC_CDU', label: 'DLC CDU', file: 'DLC_CDU_CHx200.glb', width: 1.2, height: 2, depth: 1 },
  { key: 'SR_DLC_SERVER', label: 'DLC 서버', file: 'DLC_Server_SuperMicro SYS-422GS-NBRT-LCC.glb', width: 1, height: 1.2, depth: 1 },
  { key: 'SR_PDU_RACK', label: '랙 장착형 PDU', file: 'pdu_rack_horizontal.glb', width: .482, height: .11, depth: .24 },
  { key: 'SR_PDU_VERTICAL', label: '세로형 PDU', file: 'pdu_rack_vertical.glb', width: .1, height: 1.6, depth: .1 },
  { key: 'SR_DRAGINO_TH', label: 'Dragino 온습도 센서', file: 'dragino_temperature_humidity.glb', width: .068, height: .12, depth: .027 },
  { key: 'SR_DRAGINO_DOOR', label: 'Dragino 도어 센서', file: 'dragino_door_contact.glb', width: .13, height: .10, depth: .03 },
  { key: 'SR_GPU_SERVER', label: 'GPU 서버 (4U 예시)', file: 'gpu_server_rack.glb', width: .482, height: .178, depth: .72 },
].map((model) => ({ ...model, category: (
  ['SR_DRAGINO_TH', 'SR_DRAGINO_DOOR'].includes(model.key) ? '센서' :
  ['SR_INROW_MOLD', 'SR_WALL_COIL'].includes(model.key) ? '프레임·구조물' :
  ['SR_RACK', 'SR_DOUBLE_RACK', 'SR_INROW_RACK', 'SR_DLC_RACK', 'SR_OCP_RACK', 'SR_RDC_RACK', 'SR_ORV3_RACK', 'SR_HAC_RACK', 'SR_DLC_SERVER', 'SR_GPU_SERVER'].includes(model.key) ? '랙·서버' :
  ['SR_INROW', 'SR_IMMERSION', 'SR_DLC_CDU'].includes(model.key) ? '냉각 설비' : '전력 설비'
) }));
let catalogModels = SHOWROOM_MODELS;
export const setShowroomModels = (models) => {
  if (Array.isArray(models)) catalogModels = models;
};
export const showroomModelByKey = (key) => catalogModels.find((model) => model.key === key);
export const showroomModelUrl = (model) => {
  const url = model.url || `/scene-assets/showroom/${encodeURIComponent(model.file)}`;
  return model.key === 'SR_DOUBLE_RACK' ? `${url}${url.includes('?') ? '&' : '?'}asset=20261103b` : url;
};

// The source RDC asset includes two unfolded side panels. In the room layout
// we show the cabinet with those panels closed so it can stand beside a rack.
const RDC_WING_MESHES = new Set(['Mesh267', 'Mesh268', 'Mesh269', 'Mesh270', 'Mesh271', 'Mesh272', 'Mesh273', 'Mesh274']);
const isBuiltinRdc = (model) => model?.key === 'SR_RDC' && String(model.file || '').split(/[\\/]/).pop().toLowerCase() === 'rdc.glb';
export const showroomDisplaySize = (model) => isBuiltinRdc(model)
  ? { width: .443333, height: 1.5, depth: .9 } : null;
// The wall-coil source has an overhead unit above the usable equipment bay.
// Its placeholder represents the bay so placement stays accurate while loading.
export const showroomFramePreviewSize = (model) => model?.key === 'SR_WALL_COIL'
  && String(model.file || '').split(/[\\/]/).pop().toLowerCase() === 'wall_coil_mold.glb'
  ? { width: 2.028, height: 1.352, depth: 1.525 } : null;
export function prepareShowroomObject(model, object) {
  if (model?.key === 'SR_WALL_COIL' && String(model.file || '').split(/[\\/]/).pop().toLowerCase() === 'wall_coil_mold.glb') {
    // The source contains a distribution panel at the entrance. Leave the
    // frame empty so a separately placed panel or any other equipment can fit.
    const embedded = [];
    object.traverse((node) => {
      if (['BUJINHAM_01', 'Sign_01', 'Sign_02'].includes(node.name)) embedded.push(node);
    });
    for (const node of embedded) node.parent?.remove(node);
    return object;
  }
  if (!isBuiltinRdc(model)) return object;
  const wings = [];
  object.traverse((node) => { if (RDC_WING_MESHES.has(node.name)) wings.push(node); });
  for (const wing of wings) wing.parent?.remove(wing);
  // The source faces +X; the rack fronts face +Z.
  object.rotation.y = -Math.PI / 2;
  return object;
}

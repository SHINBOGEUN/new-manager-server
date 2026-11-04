import * as THREE from "/vendor/three/three.module.js?v=20261022d";
import { OrbitControls } from "/vendor/three/controls/OrbitControls.js";
import { floorRect, floorRectsOverlap, floorBatchPositions, wallBatchGrid, keyboardMoveDelta, snapFloorPosition, wallCornerCandidates } from "/floor-placement.js?v=20261028ae";
import { GLTFLoader } from "/vendor/three/loaders/GLTFLoader.js";
import { prepareShowroomObject, showroomDisplaySize, showroomFramePreviewSize, showroomModelByKey, showroomModelUrl } from "/showroom-models.js?v=20261103d";
import { allowsInrowInteriorOverlap, fitEquipmentInsideFrame, fitFrameAroundEquipment, inrowFrameScale, isEquipmentFrameKey, isFrameEquipmentItem, resizedInrowFrame } from "/inrow-placement.js?v=20261029b";
import { setGroupLockState, rackDetailAction } from "/scene-group-lock.js?v=20261101b";

import { isDoorAttachment, updateDoorAttachmentTransform, prepareDoorSensorParts, setDoorSensorPartsAttached } from "/rack-door-attachment.js?v=20261103c";

import { rackTemperatureColor, rackDisplayRows, temperatureHumidityLines } from "/rack-scene-display.js?v=20261103e";

const views = new WeakMap();
const makeKey = () => `scene-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const showroomModelCache = new Map();
const showroomThumbnailCache = new Map();
const builtinThumbnailCache = new Map();
const builtinAssetLabels = new Map();
function loadShowroomModel(model) {
  const url = showroomModelUrl(model);
  if (!showroomModelCache.has(url)) {
    const token = window.dcimModelToken?.();
    const loader = new GLTFLoader();
    if (token && model.url) loader.setRequestHeader({ Authorization: `Bearer ${token}` });
    const pending = loader.loadAsync(url).catch((error) => {
      showroomModelCache.delete(url); throw error;
    });
    showroomModelCache.set(url, pending);
  }
  return showroomModelCache.get(url);
}

async function renderModelThumbnails(models, onThumbnail, shouldContinue = () => true) {
  let renderer;
  try {
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
    renderer.setPixelRatio(1); renderer.setSize(180, 120, false);
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.18;
    const scene = new THREE.Scene();
    scene.add(new THREE.HemisphereLight(0xe6f6ff, 0x425564, 2.2));
    const light = new THREE.DirectionalLight(0xffffff, 3); light.position.set(4, 8, 6); scene.add(light);
    const camera = new THREE.PerspectiveCamera(35, 1.5, .01, 10000);
    const loader = new GLTFLoader();
    const token = window.dcimModelToken?.();
    if (token) loader.setRequestHeader({ Authorization: `Bearer ${token}` });
    for (const model of models) {
      if (!shouldContinue()) break;
      const cacheKey = showroomModelUrl(model);
      const cached = showroomThumbnailCache.get(cacheKey);
      if (cached) { onThumbnail(model.key, cached); continue; }
      let object;
      try {
        const gltf = await loader.loadAsync(showroomModelUrl(model)); object = gltf.scene;
        if (!shouldContinue()) break;
        const bounds = new THREE.Box3().setFromObject(object);
        if (bounds.isEmpty()) throw new Error("Empty 3D model");
        const sphere = bounds.getBoundingSphere(new THREE.Sphere());
        const distance = Math.max(sphere.radius, .01) / Math.sin(THREE.MathUtils.degToRad(camera.fov / 2)) * 1.2;
        camera.position.copy(sphere.center).add(new THREE.Vector3(1.6, 1.1, 2.2).normalize().multiplyScalar(distance));
        camera.near = Math.max(.001, distance / 1000); camera.far = distance * 10; camera.lookAt(sphere.center); camera.updateProjectionMatrix();
        scene.add(object); renderer.render(scene, camera);
        const data = renderer.domElement.toDataURL("image/png");
        showroomThumbnailCache.set(cacheKey, data); onThumbnail(model.key, data);
      } catch (error) {
        console.warn(`3D model thumbnail failed: ${model.key}`, error);
        if (shouldContinue()) onThumbnail(model.key, null);
      } finally {
        if (object) {
          scene.remove(object);
          const geometries = new Set(), materials = new Set(), textures = new Set();
          object.traverse((node) => {
            if (node.geometry) geometries.add(node.geometry);
            for (const material of (Array.isArray(node.material) ? node.material : [node.material])) if (material) {
              materials.add(material); for (const value of Object.values(material)) if (value?.isTexture) textures.add(value);
            }
          });
          geometries.forEach((geometry) => geometry.dispose()); textures.forEach((texture) => texture.dispose()); materials.forEach((material) => material.dispose());
        }
      }
      await new Promise((resolve) => requestAnimationFrame(resolve));
    }
  } catch (error) {
    console.warn("3D model thumbnails unavailable", error);
    if (shouldContinue()) for (const model of models) onThumbnail(model.key, null);
  } finally { renderer?.dispose(); }
}

async function renderBuiltinThumbnails(assets, onThumbnail, shouldContinue = () => true) {
  let renderer;
  try {
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
    renderer.setPixelRatio(1); renderer.setSize(180, 120, false);
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    const scene = new THREE.Scene();
    scene.add(new THREE.HemisphereLight(0xe6f6ff, 0x425564, 2.4));
    const light = new THREE.DirectionalLight(0xffffff, 3); light.position.set(4, 8, 6); scene.add(light);
    const camera = new THREE.PerspectiveCamera(35, 1.5, .01, 1000);
    for (const asset of assets) {
      if (!shouldContinue()) break;
      if (builtinThumbnailCache.has(asset.key)) {
        onThumbnail(asset.key, builtinThumbnailCache.get(asset.key)); continue;
      }
      let object;
      try {
        object = makeEquipment({ modelKey: asset.key, itemType: "EQUIPMENT", sizeX: 4, sizeZ: 4 });
        const bounds = new THREE.Box3().setFromObject(object);
        if (bounds.isEmpty()) throw new Error("Empty 3D asset");
        const sphere = bounds.getBoundingSphere(new THREE.Sphere());
        const distance = Math.max(sphere.radius, .01) / Math.sin(THREE.MathUtils.degToRad(camera.fov / 2)) * 1.35;
        camera.position.copy(sphere.center).add(new THREE.Vector3(1.6, 1.1, 2.2).normalize().multiplyScalar(distance));
        camera.lookAt(sphere.center); camera.updateProjectionMatrix();
        scene.add(object); renderer.render(scene, camera);
        const data = renderer.domElement.toDataURL("image/png");
        builtinThumbnailCache.set(asset.key, data); onThumbnail(asset.key, data);
      } catch (error) {
        console.warn(`3D asset thumbnail failed: ${asset.key}`, error);
        if (shouldContinue()) onThumbnail(asset.key, null);
      } finally {
        if (object) {
          scene.remove(object);
          object.traverse((node) => {
            node.geometry?.dispose();
            for (const material of (Array.isArray(node.material) ? node.material : [node.material])) material?.dispose();
          });
        }
      }
      await new Promise((resolve) => requestAnimationFrame(resolve));
    }
  } catch (error) {
    console.warn("3D asset thumbnails unavailable", error);
    if (shouldContinue()) for (const asset of assets) onThumbnail(asset.key, null);
  } finally { renderer?.dispose(); }
}

function labelSprite(text, color = "#e8fbff") {
  const canvas = document.createElement("canvas");
  canvas.width = 512; canvas.height = 128;
  const ctx = canvas.getContext("2d");
  ctx.fillStyle = "rgba(6,18,29,.88)"; ctx.strokeStyle = "#37c6e7"; ctx.lineWidth = 4;
  ctx.beginPath(); ctx.roundRect(5, 5, 502, 118, 18); ctx.fill(); ctx.stroke();
  ctx.fillStyle = color; ctx.font = "bold 36px sans-serif"; ctx.textAlign = "center"; ctx.textBaseline = "middle";
  ctx.fillText(String(text || ""), 256, 64, 470);
  const texture = new THREE.CanvasTexture(canvas); texture.colorSpace = THREE.SRGBColorSpace;
  const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: texture, transparent: true, depthTest: false }));
  sprite.scale.set(1.25, .31, 1); sprite.userData.ownedTexture = texture;
  return sprite;
}

function rackUnitSprite(unit) {
  const canvas = document.createElement("canvas");
  canvas.width = 64; canvas.height = 32;
  const ctx = canvas.getContext("2d");
  ctx.font = "bold 26px sans-serif"; ctx.textAlign = "center"; ctx.textBaseline = "middle";
  ctx.lineWidth = 4; ctx.strokeStyle = "#07131d"; ctx.strokeText(String(unit), 32, 16);
  ctx.fillStyle = "#ffffff"; ctx.fillText(String(unit), 32, 16);
  const texture = new THREE.CanvasTexture(canvas); texture.colorSpace = THREE.SRGBColorSpace;
  const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: texture, transparent: true, depthTest: false, depthWrite: false }));
  sprite.userData.ownedTexture = texture;
  return sprite;
}

function makeRoom(scene, flat = false) {
  const floor = new THREE.Mesh(new THREE.BoxGeometry(20, .18, 12), new THREE.MeshStandardMaterial({ color: 0xc4ceca, roughness: .82 }));
  floor.position.y = -.1; floor.receiveShadow = true; scene.add(floor);
  const grid = new THREE.GridHelper(20, 20, 0x78918f, 0x9aadaa); grid.position.y = .002; scene.add(grid);
  if (!flat) {
    const wallMat = new THREE.MeshStandardMaterial({ color: 0x526a70, roughness: .92, transparent: true, opacity: .72, side: THREE.DoubleSide });
    const back = new THREE.Mesh(new THREE.BoxGeometry(20, 3.4, .16), wallMat); back.position.set(0, 1.6, -6); scene.add(back);
    const side = new THREE.Mesh(new THREE.BoxGeometry(.16, 3.4, 12), wallMat); side.position.set(-10, 1.6, 0); scene.add(side);
  }
  const aisleMat = new THREE.MeshStandardMaterial({ color: 0x65d1de, transparent: true, opacity: .12, roughness: 1 });
  for (const z of [-1.25, 2.55]) { const aisle = new THREE.Mesh(new THREE.BoxGeometry(17, .012, 1.15), aisleMat); aisle.position.set(0, .012, z); scene.add(aisle); }
  if (!flat) {
    const lightMat = new THREE.MeshStandardMaterial({ color: 0xb7ffff, emissive: 0x58eaff, emissiveIntensity: 1.7 });
    for (const x of [-6, -2, 2, 6]) { const light = new THREE.Mesh(new THREE.BoxGeometry(2.2, .035, .12), lightMat); light.position.set(x, 3.08, -1.2); scene.add(light); }
  }
}

function isColorableRack(item) {
  return item?.itemType === "RACK" || (/^SR_.*RACK$/.test(item?.modelKey || "") && item.modelKey !== "SR_PDU_RACK");
}

// Doors named front_door_left/right have separate hinges; a single front_door
// keeps the existing right-hand opening behavior.
function attachRackDoorPivots(searchRoot, parent) {
  const doors = [];
  searchRoot.traverse((node) => {
    if (/front[_\s-]*door(?:[_\s-]*(?:left|right))?(?:[.\s_-]\d+)?$/i.test(node.name || "")) doors.push(node);
  });
  if (!doors.length) return [];
  parent.updateMatrixWorld(true);
  const inverse = parent.matrixWorld.clone().invert(), pivots = [];
  for (const door of doors) {
    const bounds = new THREE.Box3();
    door.traverse((node) => {
      if (!node.isMesh || !node.geometry) return;
      if (!node.geometry.boundingBox) node.geometry.computeBoundingBox();
      if (node.geometry.boundingBox) bounds.union(node.geometry.boundingBox.clone().applyMatrix4(inverse.clone().multiply(node.matrixWorld)));
    });
    if (bounds.isEmpty()) continue;
    const left = /[_\s-]left$/i.test(door.name || "");
    const pivot = new THREE.Group();
    pivot.position.set(left ? bounds.min.x : bounds.max.x, (bounds.min.y + bounds.max.y) / 2, (bounds.min.z + bounds.max.z) / 2);
    parent.add(pivot); parent.updateMatrixWorld(true); pivot.attach(door);
    pivots.push({ pivot, direction: left ? -1 : 1 });
  }
  return pivots;
}

function applyRackColor(group, color) {
  group.traverse((node) => {
    if (!node.isMesh || !node.material) return;
    for (let current = node; current && current !== group; current = current.parent) {
      if (current.name === "rack-asset-overlay" || current.userData?.sceneKind === "SENSOR") return;
    }
    const change = (material) => {
      if (!material?.color || material.transparent || material.emissiveIntensity > 1) return material;
      if (!material.userData.rackColorOwned) {
        material = material.clone(); material.userData.rackColorOwned = true;
        material.userData.rackOriginalColor = material.color.getHex();
        material.userData.rackOriginalEmissive = material.emissive?.getHex();
        material.userData.rackOriginalEmissiveIntensity = material.emissiveIntensity;
      }
      material.color.set(color || material.userData.rackOriginalColor);
      if (material.emissive && material.userData.rackOriginalEmissive != null) {
        material.emissive.set(color || material.userData.rackOriginalEmissive);
        material.emissiveIntensity = color ? .12 : material.userData.rackOriginalEmissiveIntensity;
      }
      return material;
    };
    node.material = Array.isArray(node.material) ? node.material.map(change) : change(node.material);
  });
}

function makeRack(item) {
  const group = new THREE.Group();
  const width = .9, depth = .9, height = 2.35;
  const dark = new THREE.MeshStandardMaterial({ color: 0x12232e, metalness: .68, roughness: .36 });
  const edge = new THREE.MeshStandardMaterial({ color: 0x315565, metalness: .55, roughness: .38 });
  const body = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), dark);
  body.position.y = height / 2; body.castShadow = true; body.receiveShadow = true; body.userData.sceneRoot = true; group.add(body);
  const front = new THREE.Mesh(new THREE.BoxGeometry(width * .82, height * .9, .035), new THREE.MeshStandardMaterial({ color: 0x192f3b, metalness: .52, roughness: .4 }));
  front.position.set(0, height / 2, depth / 2 + .025); group.add(front);
  for (let i = 0; i < 18; i++) {
    const y = .2 + i * .106;
    const unit = new THREE.Mesh(new THREE.BoxGeometry(width * .68, .074, .025), new THREE.MeshStandardMaterial({ color: i % 4 === 0 ? 0x273d48 : 0x20333e, metalness: .4, roughness: .55 }));
    unit.position.set(0, y, depth / 2 + .05); group.add(unit);
  }
  for (const x of [-width * .39, width * .39]) { const rail = new THREE.Mesh(new THREE.BoxGeometry(.035, height * .92, .04), edge); rail.position.set(x, height / 2, depth / 2 + .065); group.add(rail); }
  const ledMat = new THREE.MeshStandardMaterial({ color: 0x39e88d, emissive: 0x13c965, emissiveIntensity: 2.2 });
  for (const y of [.35, .78, 1.22, 1.66, 2.05]) { const led = new THREE.Mesh(new THREE.SphereGeometry(.035, 12, 8), ledMat); led.position.set(width * .34, y, depth / 2 + .09); group.add(led); }
  const top = new THREE.Mesh(new THREE.BoxGeometry(width * 1.06, .06, depth * 1.06), edge); top.position.y = height + .03; group.add(top);
  group.scale.set((Number(item.sizeX) || 4) / 4, (Number(item.sizeY) || Number(item.sizeX) || 4) / 4, (Number(item.sizeZ) || Number(item.sizeX) || 4) / 4);
  group.userData.sceneItem = item; group.userData.sceneKind = "RACK";
  if (item.rackColor) applyRackColor(group, item.rackColor);
  return group;
}

const isCeilingModel = (modelKey) => modelKey === "CEILING" || modelKey === "CEILING_CLEAR";
const isWallModel = (modelKey) => String(modelKey || "").startsWith("WALL");
const isDoorModel = (modelKey) => String(modelKey || "").startsWith("DOOR");
const isWindowModel = (modelKey) => String(modelKey || "").startsWith("WINDOW");
function openingSize(modelKey, sizeZ = 4) {
  const scale = (Number(sizeZ) || 4) / 4;
  if (modelKey === "DOOR_DOUBLE") return { width: 1.6 * scale, bottom: 0, height: 2.35 * scale };
  if (isDoorModel(modelKey)) return { width: 1.4 * scale, bottom: 0, height: 2.35 * scale };
  if (modelKey === "WINDOW_DOUBLE") return { width: 2.9 * scale, bottom: .9 * scale, height: 1.35 * scale };
  if (isWindowModel(modelKey)) return { width: 2.55 * scale, bottom: .9 * scale, height: 1.35 * scale };
  return null;
}
function rebuildWallSurface(wallSegment, openings, modelKey, length = 4, joins = {}, height = 2.8) {
  const halfLength = length / 2;
  const previousSurface = wallSegment.userData.surfaceGroup;
  if (previousSurface) {
    const geometries = new Set(), materials = new Set();
    previousSurface.traverse((node) => {
      if (node.geometry) geometries.add(node.geometry);
      for (const material of (Array.isArray(node.material) ? node.material : [node.material])) if (material) materials.add(material);
    });
    previousSurface.parent?.remove(previousSurface);
    geometries.forEach((geometry) => geometry.dispose()); materials.forEach((material) => material.dispose());
  }
  const surface = new THREE.Group(); wallSegment.userData.surfaceGroup = surface; wallSegment.add(surface);
  const clearGlass = modelKey === "WALL_GLASS_CLEAR";
  const wallColor = modelKey === "WALL_CONCRETE" ? 0x777f82 : clearGlass ? 0xc3d0d1 : modelKey === "WALL_GLASS" ? 0x4a8798 : modelKey === "WALL_PANEL" ? 0x9da9a5 : 0x87979a;
  const xCuts = new Set([-halfLength, halfLength]), yCuts = new Set([0, height]);
  const holes = openings.map((item) => {
    const size = openingSize(item.modelKey, item.sizeZ); if (!size) return null;
    return { minX: THREE.MathUtils.clamp(Number(item.x || 0) - size.width / 2, -halfLength, halfLength),
      maxX: THREE.MathUtils.clamp(Number(item.x || 0) + size.width / 2, -halfLength, halfLength),
      minY: size.bottom + Number(item.y || 0), maxY: size.bottom + size.height + Number(item.y || 0) };
  }).filter(Boolean);
  for (const hole of holes) { xCuts.add(hole.minX); xCuts.add(hole.maxX); yCuts.add(hole.minY); yCuts.add(hole.maxY); }
  const xs = [...xCuts].sort((a, b) => a - b), ys = [...yCuts].sort((a, b) => a - b);
  const glassWall = modelKey === "WALL_GLASS" || clearGlass;
  const wallMaterial = new THREE.MeshStandardMaterial({ color: wallColor, roughness: glassWall ? .08 : modelKey === "WALL_CONCRETE" ? .98 : .72,
    metalness: glassWall ? .12 : modelKey === "WALL_PANEL" ? .3 : .04, transparent: glassWall, opacity: clearGlass ? .12 : glassWall ? .2 : 1, depthWrite: !clearGlass, side: THREE.DoubleSide });
  for (let xi = 0; xi < xs.length - 1; xi++) for (let yi = 0; yi < ys.length - 1; yi++) {
    const left = xs[xi], right = xs[xi + 1], bottom = ys[yi], top = ys[yi + 1];
    if (right - left < .005 || top - bottom < .005) continue;
    const centerX = (left + right) / 2, centerY = (bottom + top) / 2;
    if (holes.some((hole) => centerX > hole.minX && centerX < hole.maxX && centerY > hole.minY && centerY < hole.maxY)) continue;
    const geometry = new THREE.BoxGeometry(right - left, top - bottom, .16);
    if (glassWall && ((joins.top && Math.abs(top - height) < .001) || (joins.bottom && Math.abs(bottom) < .001)
      || (joins.left && Math.abs(left + halfLength) < .001) || (joins.right && Math.abs(right - halfLength) < .001))) {
      // BoxGeometry has separate top and bottom faces. At a stacked joint those
      // transparent faces overlap and draw a visible stripe across the glass.
      const indices = [];
      for (const face of geometry.groups) {
        if ((face.materialIndex === 2 && joins.top && Math.abs(top - height) < .001)
          || (face.materialIndex === 3 && joins.bottom && Math.abs(bottom) < .001)
          || (face.materialIndex === 0 && joins.right && Math.abs(right - halfLength) < .001)
          || (face.materialIndex === 1 && joins.left && Math.abs(left + halfLength) < .001)) continue;
        for (let i = face.start; i < face.start + face.count; i++) indices.push(geometry.index.getX(i));
      }
      geometry.clearGroups(); geometry.setIndex(indices);
    }
    const panel = new THREE.Mesh(geometry, wallMaterial);
    // Transparent glass should not cast an opaque shadow or receive a dark
    // shadow on its pane; the frames remain visible as normal geometry.
    panel.position.set(centerX, centerY, 0); panel.castShadow = !glassWall; panel.receiveShadow = !glassWall; surface.add(panel);
  }
  if (!glassWall) {
    const seamMaterial = new THREE.MeshStandardMaterial({ color: 0xb8c1c0 });
    for (let y = .3; y < height; y += .55) {
      const intervals = [[-halfLength, halfLength]];
      for (const hole of holes.filter((entry) => y > entry.minY && y < entry.maxY)) {
        for (let i = intervals.length - 1; i >= 0; i--) {
          const [left, right] = intervals[i];
          if (hole.maxX <= left || hole.minX >= right) continue;
          intervals.splice(i, 1, ...(hole.minX > left ? [[left, hole.minX]] : []), ...(hole.maxX < right ? [[hole.maxX, right]] : []));
        }
      }
      for (const [left, right] of intervals) if (right - left > .02) {
        const seam = new THREE.Mesh(new THREE.BoxGeometry(right - left, .025, .18), seamMaterial);
        seam.position.set((left + right) / 2, y, 0); surface.add(seam);
      }
    }
  }
  if (glassWall || modelKey === "WALL_PANEL" || modelKey === "WALL_CONCRETE") {
    const trimMaterial = new THREE.MeshStandardMaterial({
      color: clearGlass ? 0x10191d : modelKey === "WALL_GLASS" ? 0x244b59 : modelKey === "WALL_PANEL" ? 0x607a82 : 0x626a6d,
      metalness: modelKey === "WALL_PANEL" || glassWall ? .58 : .04, roughness: modelKey === "WALL_CONCRETE" ? .95 : .42,
    });
    const innerLines = glassWall ? []
      : Array.from({ length: Math.max(0, Math.floor(length / 4) - 1) }, (_, index) => -halfLength + (index + 1) * 4);
    const xLines = [...new Set([...(glassWall && joins.left ? [] : [-halfLength + .04]),
      ...(glassWall && joins.right ? [] : [halfLength - .04]), ...innerLines])];
    for (const x of xLines) {
      const segments = [[0, height]];
      for (const hole of holes.filter((entry) => x > entry.minX - .04 && x < entry.maxX + .04)) {
        for (let index = segments.length - 1; index >= 0; index--) {
          const [bottom, top] = segments[index];
          if (hole.maxY <= bottom || hole.minY >= top) continue;
          segments.splice(index, 1, ...(hole.minY > bottom ? [[bottom, hole.minY]] : []), ...(hole.maxY < top ? [[hole.maxY, top]] : []));
        }
      }
      for (const [bottom, top] of segments) if (top - bottom > .04) {
        const rail = new THREE.Mesh(new THREE.BoxGeometry(glassWall ? .055 : .035, top - bottom, .2), trimMaterial);
        rail.position.set(x, (bottom + top) / 2, 0); surface.add(rail);
      }
    }
    if (glassWall) {
      for (const y of [.08, height - .08]) {
        if ((y < 1 && joins.bottom) || (y > 1 && joins.top)) continue;
        const rail = new THREE.Mesh(new THREE.BoxGeometry(length, .075, .22), trimMaterial);
        rail.position.set(0, y, 0); surface.add(rail);
      }
    }
  }
}

function rebuildCeilingSurface(group, sceneItems = [], currentItem = group.userData.sceneItem) {
  for (const child of [...group.children]) {
    child.traverse((node) => { node.geometry?.dispose();
      for (const material of (Array.isArray(node.material) ? node.material : [node.material])) material?.dispose(); });
    group.remove(child);
  }
  const item = group.userData.sceneItem;
  const width = Number(item?.sizeX) || 4, depth = Number(item?.sizeZ) || 4;
  // Keep the ceiling as one glass pane. A mostly opaque tint prevents wall
  // mullions below it from reading as joints through the roof surface.
  const clear = item?.modelKey === "CEILING_CLEAR";
  const glass = new THREE.MeshPhysicalMaterial({ color: clear ? 0xbdebf2 : 0x91dbea, metalness: .08, roughness: clear ? .08 : .24,
    transmission: clear ? .35 : 0, transparent: true, opacity: clear ? .25 : .98, depthWrite: !clear, side: THREE.DoubleSide, thickness: .08 });
  const pane = new THREE.Mesh(new THREE.BoxGeometry(width, .12, depth), glass);
  pane.position.y = .06; pane.receiveShadow = true; pane.castShadow = false; group.add(pane);
  const frame = new THREE.MeshStandardMaterial({ color: 0x387f91, metalness: .42, roughness: .3 });
  const railWidth = .08;
  const neighbors = sceneItems.filter((item) => item !== currentItem && isCeilingModel(item.modelKey));
  const adjacent = (side) => neighbors.some((other) => {
    if (Math.abs(Number(other.y || 0) - Number(currentItem?.y || 0)) > .12) return false;
    const ox = Number(other.x) || 0, oz = Number(other.z) || 0;
    const otherHalfX = (Number(other.sizeX) || 4) / 2, otherHalfZ = (Number(other.sizeZ) || 4) / 2;
    if (side === "left" || side === "right") {
      const edge = Number(currentItem?.x || 0) + (side === "right" ? width / 2 : -width / 2);
      const otherEdge = ox + (side === "right" ? -otherHalfX : otherHalfX);
      return Math.abs(edge - otherEdge) < .06 && Math.min(Number(currentItem?.z || 0) + depth / 2, oz + otherHalfZ)
        - Math.max(Number(currentItem?.z || 0) - depth / 2, oz - otherHalfZ) > .1;
    }
    const edge = Number(currentItem?.z || 0) + (side === "front" ? depth / 2 : -depth / 2);
    const otherEdge = oz + (side === "front" ? -otherHalfZ : otherHalfZ);
    return Math.abs(edge - otherEdge) < .06 && Math.min(Number(currentItem?.x || 0) + width / 2, ox + otherHalfX)
      - Math.max(Number(currentItem?.x || 0) - width / 2, ox - otherHalfX) > .1;
  });
  for (const [side, x] of [["left", -width / 2 + railWidth / 2], ["right", width / 2 - railWidth / 2]]) {
    if (adjacent(side)) continue;
    const rail = new THREE.Mesh(new THREE.BoxGeometry(railWidth, .035, depth), frame);
    rail.position.set(x, .13, 0); group.add(rail);
  }
  for (const [side, z] of [["back", -depth / 2 + railWidth / 2], ["front", depth / 2 - railWidth / 2]]) {
    if (adjacent(side)) continue;
    const rail = new THREE.Mesh(new THREE.BoxGeometry(width, .035, railWidth), frame);
    rail.position.set(0, .13, z); group.add(rail);
  }
}

function makeEquipment(item) {
  const group = new THREE.Group();
  const showroomModel = showroomModelByKey(item.modelKey);
  if (showroomModel) {
    // Match the normalized GLB bounds while it loads. An oversized box would
    // falsely block a rack/cooler from being placed inside the in-row frame.
    const previewSize = showroomFramePreviewSize(showroomModel) || showroomDisplaySize(showroomModel) || (item.modelKey === "SR_INROW_RACK" || item.modelKey === "SR_RACK"
      ? { width: .444, height: 1.5, depth: .9 }
      : item.modelKey === "SR_INROW" ? { width: .9, height: 1.737, depth: .789 }
        : showroomModel);
    const placeholder = new THREE.Mesh(new THREE.BoxGeometry(previewSize.width, previewSize.height, previewSize.depth),
      new THREE.MeshStandardMaterial({ color: 0x365562, metalness: .25, roughness: .5 }));
    placeholder.position.y = previewSize.height / 2; placeholder.castShadow = true; group.add(placeholder);
    loadShowroomModel(showroomModel).then((gltf) => {
      if (!group.parent) return;
      const object = prepareShowroomObject(showroomModel, gltf.scene.clone(true));
      const bounds = new THREE.Box3().setFromObject(object);
      if (bounds.isEmpty()) throw new Error("모델에 표시할 형상이 없습니다.");
      const size = bounds.getSize(new THREE.Vector3()), center = bounds.getCenter(new THREE.Vector3());
      const displaySize = showroomDisplaySize(showroomModel);
      const uniformScale = Math.min(showroomModel.width / Math.max(size.x, .001), showroomModel.height / Math.max(size.y, .001), showroomModel.depth / Math.max(size.z, .001));
      const scale = displaySize
        ? new THREE.Vector3(displaySize.width / size.x, displaySize.height / size.y, displaySize.depth / size.z)
        : new THREE.Vector3(uniformScale, uniformScale, uniformScale);
      const wrapper = new THREE.Group(); wrapper.add(object); wrapper.scale.copy(scale);
      wrapper.position.set(-center.x * scale.x, -bounds.min.y * scale.y, -center.z * scale.z);
      object.traverse((node) => { if (node.isMesh) { node.castShadow = true; node.receiveShadow = true; } });
      group.add(wrapper); group.remove(placeholder);
      if (isColorableRack(item) && item.rackColor) applyRackColor(group, item.rackColor);
      placeholder.geometry.dispose(); placeholder.material.dispose();
      group.userData.onImportedModelReady?.(wrapper);
    }).catch((error) => { console.error(`쇼룸 모델 ${showroomModel.label} 로딩 실패`, error); });
  } else if (item.modelKey === "CONVEYOR") {
    const metal = new THREE.MeshStandardMaterial({ color: 0x596a70, metalness: .65, roughness: .4 });
    const base = new THREE.Mesh(new THREE.BoxGeometry(3.2, .22, .72), metal); base.position.y = .55; base.castShadow = true; group.add(base);
    const belt = new THREE.Mesh(new THREE.BoxGeometry(3.05, .04, .65), new THREE.MeshStandardMaterial({ color: 0x18242a, roughness: .85 })); belt.position.y = .69; group.add(belt);
    for (const x of [-1.3, -.78, -.26, .26, .78, 1.3]) { const roller = new THREE.Mesh(new THREE.CylinderGeometry(.045, .045, .68, 12), metal); roller.rotation.x = Math.PI / 2; roller.position.set(x, .72, 0); group.add(roller); }
  } else if (item.modelKey === "TANK") {
    const metal = new THREE.MeshStandardMaterial({ color: 0x66919a, metalness: .58, roughness: .35 });
    const tank = new THREE.Mesh(new THREE.CylinderGeometry(.62, .62, 1.8, 24), metal); tank.position.y = .95; tank.castShadow = true; group.add(tank);
    const lid = new THREE.Mesh(new THREE.CylinderGeometry(.64, .64, .1, 24), metal); lid.position.y = 1.9; group.add(lid);
    const pipe = new THREE.Mesh(new THREE.CylinderGeometry(.07, .07, .46, 12), metal); pipe.position.set(.22, 2.17, 0); group.add(pipe);
  } else if (isWallModel(item.modelKey)) {
    const wallSegment = new THREE.Group(); wallSegment.name = "wallSegment";
    rebuildWallSurface(wallSegment, [], item.modelKey, Number(item.sizeX) || 4, {}, 2.8 * (Number(item.sizeZ) || 4) / 4);
    group.add(wallSegment); group.userData.wallSegment = wallSegment;
  } else if (isDoorModel(item.modelKey)) {
    const frame = new THREE.MeshStandardMaterial({ color: item.modelKey === "DOOR_SLIDING" ? 0x98c3c8 : 0x768b90, metalness: .48, roughness: .42 });
    const doubleDoor = item.modelKey === "DOOR_DOUBLE";
    const slidingDoor = item.modelKey === "DOOR_SLIDING";
    const width = doubleDoor ? 1.48 : 1.28;
    for (const x of [-width / 2, width / 2]) { const post = new THREE.Mesh(new THREE.BoxGeometry(.12, 2.35, .16), frame); post.position.set(x, 1.175, 0); group.add(post); }
    const lintel = new THREE.Mesh(new THREE.BoxGeometry(width + .12, .12, .16), frame); lintel.position.y = 2.35; group.add(lintel);
    if (slidingDoor) {
      const track = new THREE.Mesh(new THREE.BoxGeometry(width + .24, .08, .22), frame); track.position.set(0, 2.26, 0); group.add(track);
      const glass = new THREE.MeshStandardMaterial({ color: 0x58bfd0, transparent: true, opacity: .58, metalness: .18, roughness: .22 });
      for (const x of [-.27, .27]) {
        const leaf = new THREE.Mesh(new THREE.BoxGeometry(.68, 2.12, .055), glass); leaf.position.set(x, 1.12, x < 0 ? -.025 : .045); group.add(leaf);
        const handle = new THREE.Mesh(new THREE.BoxGeometry(.035, .3, .06), frame); handle.position.set(x < 0 ? .28 : -.28, 1.1, .085); group.add(handle);
      }
    } else if (doubleDoor) {
      const panel = new THREE.MeshStandardMaterial({ color: 0x274957, roughness: .7 });
      for (const x of [-.34, .34]) {
        const leaf = new THREE.Mesh(new THREE.BoxGeometry(.64, 2.12, .065), panel); leaf.position.set(x, 1.08, .04); group.add(leaf);
        const handle = new THREE.Mesh(new THREE.BoxGeometry(.035, .24, .07), frame); handle.position.set(x < 0 ? .22 : -.22, 1.05, .09); group.add(handle);
      }
    } else {
      const leaf = new THREE.Mesh(new THREE.BoxGeometry(1.08, 2.15, .075), new THREE.MeshStandardMaterial({ color: 0x315768, roughness: .72 })); leaf.position.set(-.04, 1.08, .04); group.add(leaf);
      const handle = new THREE.Mesh(new THREE.SphereGeometry(.055, 12, 8), new THREE.MeshStandardMaterial({ color: 0xe0bc69, metalness: .72, roughness: .25 })); handle.position.set(.38, 1.05, .1); group.add(handle);
    }
  } else if (isWindowModel(item.modelKey)) {
    const frame = new THREE.MeshStandardMaterial({ color: item.modelKey === "WINDOW_DOUBLE" ? 0x526b73 : 0x93a7aa, metalness: .52, roughness: .32 });
    const glass = new THREE.MeshStandardMaterial({ color: item.modelKey === "WINDOW_DOUBLE" ? 0x55b2c2 : 0x61c9df, metalness: .18, roughness: .2, transparent: true, opacity: item.modelKey === "WINDOW_DOUBLE" ? .3 : .42 });
    const wideWindow = item.modelKey === "WINDOW_DOUBLE";
    const width = wideWindow ? 2.8 : 2.45;
    const pane = new THREE.Mesh(new THREE.BoxGeometry(width - .2, 1.35, .045), glass); pane.position.y = 1.65; group.add(pane);
    for (const x of [-width / 2, width / 2]) { const post = new THREE.Mesh(new THREE.BoxGeometry(.09, 1.55, .12), frame); post.position.set(x, 1.65, 0); group.add(post); }
    for (const y of [.88, 2.42]) { const rail = new THREE.Mesh(new THREE.BoxGeometry(width, .09, .12), frame); rail.position.y = y; group.add(rail); }
    if (wideWindow) {
      for (const x of [-.68, .68]) { const divider = new THREE.Mesh(new THREE.BoxGeometry(.075, 1.35, .13), frame); divider.position.set(x, 1.65, .01); group.add(divider); }
      for (const x of [-.7, .7]) { const latch = new THREE.Mesh(new THREE.BoxGeometry(.04, .18, .065), frame); latch.position.set(x, 1.65, .1); group.add(latch); }
    } else {
      const mullion = new THREE.Mesh(new THREE.BoxGeometry(.07, 1.38, .13), frame); mullion.position.set(0, 1.65, .01); group.add(mullion);
    }
  } else if (isCeilingModel(item.modelKey)) {
    group.userData.sceneItem = item; rebuildCeilingSurface(group);
  } else if (item.modelKey === "FLOOR") {
    const slab = new THREE.Mesh(new THREE.BoxGeometry(4, .14, 4), new THREE.MeshStandardMaterial({ color: 0x788c92, roughness: .92 })); slab.position.y = -.07; slab.receiveShadow = true; group.add(slab);
  } else if (item.modelKey === "LIGHT") {
    const cable = new THREE.Mesh(new THREE.CylinderGeometry(.025, .025, 1.7, 8), new THREE.MeshStandardMaterial({ color: 0x52666d })); cable.position.y = 2.35; group.add(cable);
    const housing = new THREE.Mesh(new THREE.BoxGeometry(1.1, .14, .34), new THREE.MeshStandardMaterial({ color: 0xd5e2dd, emissive: 0x79eaff, emissiveIntensity: .65 })); housing.position.y = 1.48; group.add(housing);
  } else if (item.modelKey === "PIPE") {
    const pipeMat = new THREE.MeshStandardMaterial({ color: 0x42a7a3, metalness: .62, roughness: .32 });
    const pipe = new THREE.Mesh(new THREE.CylinderGeometry(.09, .09, 4, 16), pipeMat); pipe.rotation.z = Math.PI / 2; pipe.position.y = 2.7; group.add(pipe);
    for (const x of [-1.55, 1.55]) { const support = new THREE.Mesh(new THREE.BoxGeometry(.07, 2.7, .07), pipeMat); support.position.set(x, 1.33, 0); group.add(support); }
  } else if (item.modelKey === "TABLE") {
    const metal = new THREE.MeshStandardMaterial({ color: 0x6d7d81, metalness: .35, roughness: .58 });
    const top = new THREE.Mesh(new THREE.BoxGeometry(1.8, .12, .9), metal); top.position.y = .9; top.castShadow = true; group.add(top);
    for (const x of [-.78, .78]) for (const z of [-.34, .34]) { const leg = new THREE.Mesh(new THREE.BoxGeometry(.08, .85, .08), metal); leg.position.set(x, .43, z); group.add(leg); }
  } else if (item.modelKey === "STAIR") {
    const stepMaterial = new THREE.MeshStandardMaterial({ color: 0x7f9194, metalness: .22, roughness: .72 });
    for (let index = 0; index < 8; index++) {
      const stepHeight = .2 + index * .2;
      const step = new THREE.Mesh(new THREE.BoxGeometry(1.45, stepHeight, .36), stepMaterial);
      step.position.set(0, stepHeight / 2, 1.26 - index * .36); step.castShadow = true; step.receiveShadow = true; group.add(step);
    }
    const leftRail = new THREE.Mesh(new THREE.BoxGeometry(.07, 1.8, 3.0), new THREE.MeshStandardMaterial({ color: 0x456671, metalness: .6, roughness: .42 })); leftRail.position.set(-.78, .95, -.02); group.add(leftRail);
    const rightRail = leftRail.clone(); rightRail.position.x = .78; group.add(rightRail);
  } else if (item.modelKey === "MACHINE") {
    const body = new THREE.Mesh(new THREE.BoxGeometry(1.45, 1.3, 1.15), new THREE.MeshStandardMaterial({ color: 0x3e7785, metalness: .45, roughness: .42 })); body.position.y = .72; body.castShadow = true; group.add(body);
    const window = new THREE.Mesh(new THREE.BoxGeometry(1.02, .68, .06), new THREE.MeshStandardMaterial({ color: 0x9ad7d4, transparent: true, opacity: .72 })); window.position.set(0, 1.35, .61); group.add(window);
  } else {
  const color = item.modelKey === "CRAC" ? 0xbcc9c6 : item.modelKey === "UPS" ? 0x748b92
    : item.modelKey === "CHILLER" ? 0x4f9a9b : item.modelKey === "SWITCH" ? 0x4876a1
    : item.modelKey === "SERVER" ? 0x202d38 : 0x365562;
  const height = item.modelKey === "CRAC" || item.modelKey === "CHILLER" ? 1.7 : item.modelKey === "SERVER" ? 2.05 : 1.3;
  const shell = new THREE.Mesh(new THREE.BoxGeometry(item.modelKey === "CHILLER" ? 1.5 : 1.05, height, .85), new THREE.MeshStandardMaterial({ color, metalness: .28, roughness: .5 }));
  shell.position.y = shell.geometry.parameters.height / 2; shell.castShadow = true; shell.receiveShadow = true; group.add(shell);
  const panel = new THREE.Mesh(new THREE.BoxGeometry(.76, shell.geometry.parameters.height * .75, .025), new THREE.MeshStandardMaterial({ color: 0x182a33, metalness: .55, roughness: .45 }));
  panel.position.set(0, shell.geometry.parameters.height * .52, .44); group.add(panel);
  }
  const isArchitectureAsset = isWallModel(item.modelKey) || isDoorModel(item.modelKey) || isWindowModel(item.modelKey)
    || ["CEILING", "CEILING_CLEAR", "FLOOR", "LIGHT", "PIPE", "STAIR"].includes(item.modelKey);
  if (item.modelKey === "FLOOR") group.scale.set((Number(item.sizeX) || 4) / 4, 1, (Number(item.sizeZ) || 4) / 4);
  else if (isDoorModel(item.modelKey) || isWindowModel(item.modelKey) || ["LIGHT", "PIPE", "STAIR"].includes(item.modelKey)) group.scale.setScalar((Number(item.sizeZ) || 4) / 4);
  if (!isArchitectureAsset) {
    if (isEquipmentFrameKey(item.modelKey)) {
      const scale = inrowFrameScale(item);
      group.scale.set(scale.x, scale.y, scale.z);
    } else group.scale.set((Number(item.sizeX) || 4) / 4, (Number(item.sizeY) || Number(item.sizeX) || 4) / 4, (Number(item.sizeZ) || Number(item.sizeX) || 4) / 4);
  }
  group.userData.sceneItem = item; group.userData.sceneKind = "EQUIPMENT";
  return group;
}

function makeSensor(item, valueText) {
  const group = new THREE.Group();
  const stem = new THREE.Mesh(new THREE.CylinderGeometry(.025, .035, .18, 12), new THREE.MeshStandardMaterial({ color: 0x48d989, emissive: 0x1bdc71, emissiveIntensity: 1.4 }));
  stem.position.y = .1; group.add(stem);
  const bulb = new THREE.Mesh(new THREE.SphereGeometry(.085, 18, 12), new THREE.MeshStandardMaterial({ color: 0x28eb73, emissive: 0x10d45f, emissiveIntensity: 1.8, roughness: .22 }));
  bulb.position.y = .24; bulb.castShadow = true; group.add(bulb);
  const glow = new THREE.Mesh(new THREE.CircleGeometry(.38, 32), new THREE.MeshBasicMaterial({ color: 0x19ec68, transparent: true, opacity: .2, depthWrite: false }));
  glow.rotation.x = -Math.PI / 2; glow.position.y = .014; group.add(glow);
  group.userData.sceneItem = item; group.userData.sceneKind = "SENSOR";
  return group;
}

function findItemKey(item) { return item.id != null ? `db-${item.id}` : item.clientKey; }

function mount(container, page, options = {}) {
  views.get(container)?.dispose();
  container.replaceChildren();
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
  renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 2)); renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap; renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.18;
  renderer.setClearColor(0x07111b, 1); container.appendChild(renderer.domElement);
  const sceneActionBar = document.createElement("div");
  sceneActionBar.setAttribute("aria-label", "선택 오브젝트 편집");
  Object.assign(sceneActionBar.style, {
    position: "absolute", zIndex: "12", display: "none", flexDirection: "column", gap: "7px", padding: "8px",
    right: "12px", bottom: "96px", width: "min(240px, calc(100% - 24px))",
    border: "1px solid #43c9e7", borderRadius: "9px", background: "rgba(7,18,30,.94)",
    boxShadow: "0 5px 16px #0008", pointerEvents: "auto",
  });
  const actionHeader = document.createElement("div");
  Object.assign(actionHeader.style, { display: "flex", alignItems: "center", gap: "8px" });
  const selectedLabel = document.createElement("div");
  Object.assign(selectedLabel.style, { flex: "1", minWidth: "0", color: "#d9f5ff", fontSize: "12px", fontWeight: "700", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" });
  const closeActionBar = document.createElement("button");
  closeActionBar.type = "button"; closeActionBar.textContent = "×";
  closeActionBar.title = "편집창 닫기"; closeActionBar.setAttribute("aria-label", "편집창 닫기");
  Object.assign(closeActionBar.style, { flex: "0 0 auto", width: "26px", height: "26px", margin: "0", padding: "0", border: "0", borderRadius: "5px", color: "#a8c8d8", background: "transparent", cursor: "pointer", fontSize: "20px", lineHeight: "1" });
  closeActionBar.addEventListener("pointerdown", (event) => { event.preventDefault(); event.stopPropagation(); });
  closeActionBar.addEventListener("click", (event) => {
    event.preventDefault(); event.stopPropagation(); sceneActionsVisible = false;
    sceneActionBar.style.display = "none";
  });
  actionHeader.append(selectedLabel, closeActionBar); sceneActionBar.append(actionHeader);
  const mainActions = document.createElement("div");
  Object.assign(mainActions.style, { display: "flex", flexWrap: "wrap", gap: "5px", alignItems: "center" });
  sceneActionBar.append(mainActions);
  const makeActionButton = (label, title, color, action) => {
    const button = document.createElement("button");
    button.type = "button"; button.textContent = label; button.title = title;
    Object.assign(button.style, { minWidth: "34px", height: "30px", padding: "2px 8px", margin: "0", border: "1px solid #36566a", borderRadius: "6px", color, background: "#13283a", cursor: "pointer", fontSize: "16px", fontWeight: "700" });
    button.addEventListener("pointerdown", (event) => { event.preventDefault(); event.stopPropagation(); });
    button.addEventListener("click", (event) => { event.preventDefault(); event.stopPropagation(); action(); });
    mainActions.append(button); return button;
  };
  const rotateLeftButton = makeActionButton("↶", "반시계 방향 90° 회전", "#8beaff", () => rotateFocusedItem(-90));
  const rotateRightButton = makeActionButton("↷", "시계 방향 90° 회전", "#8beaff", () => rotateFocusedItem(90));
  const shrinkButton = makeActionButton("−", "설비 크기 줄이기", "#8beaff", () => resizeFocusedItem(-1));
  const enlargeButton = makeActionButton("+", "설비 크기 늘리기", "#8beaff", () => resizeFocusedItem(1));
  const widthShrinkButton = makeActionButton("너−", "설비 너비 줄이기", "#8beaff", () => resizeFocusedItem(-1, "width"));
  const widthEnlargeButton = makeActionButton("너+", "설비 너비 늘리기", "#8beaff", () => resizeFocusedItem(1, "width"));
  const depthShrinkButton = makeActionButton("↕−", "천장 세로 폭 줄이기", "#8beaff", () => resizeFocusedItem(-1, "z"));
  const depthEnlargeButton = makeActionButton("↕+", "천장 세로 폭 늘리기", "#8beaff", () => resizeFocusedItem(1, "z"));
  const heightShrinkButton = makeActionButton("높−", "프레임 높이 줄이기", "#8beaff", () => resizeFocusedItem(-1, "y"));
  const heightEnlargeButton = makeActionButton("높+", "프레임 높이 늘리기", "#8beaff", () => resizeFocusedItem(1, "y"));
  const removeFocusedButton = makeActionButton("삭제", "선택한 오브젝트 삭제", "#ff9caa", () => {
    if (placement) cancelPlacement(); else if (selectedItem) api.removeItem(findItemKey(selectedItem));
  });
  removeFocusedButton.style.fontSize = "12px";
  const copyFocusedButton = makeActionButton("복사", "같은 크기와 방향으로 복사", "#8beaff", () => {
    if (selectedItem) duplicateItem(selectedItem);
  });
  copyFocusedButton.style.fontSize = "12px";
  mainActions.insertBefore(copyFocusedButton, removeFocusedButton);
  const lockFocusedButton = makeActionButton("잠금", "설비 잠금", "#8beaff", () => {
    if (!selectedItem || !(isFrameEquipmentItem(selectedItem) || isEquipmentFrameKey(selectedItem.modelKey))) return;
    selectedItem.locked = !selectedItem.locked;
    options.onHostSelected?.(selectedItem);
    options.onChange?.(items); options.onHint?.(`${selectedItem.label} ${selectedItem.locked ? "잠금" : "잠금 해제"} · 저장하면 반영됩니다.`);
  });
  lockFocusedButton.style.fontSize = "12px";
  const selectGroupButton = makeActionButton("묶음 전체 선택", "이 설비가 속한 묶음 전체 선택", "#8beaff", () => api.selectGroup());
  Object.assign(selectGroupButton.style, { width: "100%", fontSize: "12px" });
  sceneActionBar.append(selectGroupButton);
  const sizePanel = document.createElement("div");
  Object.assign(sizePanel.style, { display: "none", gap: "5px", flexDirection: "column", paddingTop: "6px", borderTop: "1px solid #36566a" });
  const sizeToggle = makeActionButton("크기 조절", "크기 조절 메뉴", "#8beaff", () => {
    sizePanel.style.display = sizePanel.style.display === "none" ? "flex" : "none";
    sizeToggle.setAttribute("aria-expanded", String(sizePanel.style.display !== "none"));
  });
  sizeToggle.style.fontSize = "12px";
  sizeToggle.setAttribute("aria-expanded", "false");
  mainActions.insertBefore(sizeToggle, removeFocusedButton);
  const rackColorLabel = document.createElement("label");
  Object.assign(rackColorLabel.style, { display:"flex", alignItems:"center", gap:"5px", padding:"3px 6px", color:"#8beaff", fontSize:"12px", fontWeight:"700" });
  rackColorLabel.append("랙 색상");
  const rackColorInput = document.createElement("input"); rackColorInput.type = "color"; rackColorInput.setAttribute("aria-label", "랙 색상");
  Object.assign(rackColorInput.style, { width:"30px", height:"26px", padding:"0", border:"0", cursor:"pointer", background:"transparent" });
  rackColorInput.addEventListener("input", () => {
    if (!selectedItem || selectedItem.locked || !isColorableRack(selectedItem)) return;
    selectedItem.rackColor = rackColorInput.value; const group = itemGroups.get(findItemKey(selectedItem));
    if (group) applyRackColor(group, selectedItem.rackColor); options.onChange?.(items);
  });
  rackColorLabel.append(rackColorInput); mainActions.append(rackColorLabel);
  const rackColorReset = makeActionButton("기본색", "랙 원래 색상으로 복원", "#8beaff", () => {
    if (!selectedItem || selectedItem.locked || !isColorableRack(selectedItem)) return;
    selectedItem.rackColor = null; const group = itemGroups.get(findItemKey(selectedItem));
    if (group) applyRackColor(group, null); options.onChange?.(items);
  });
  rackColorReset.style.fontSize = "12px";
  const sizeRows = [];
  for (const [label, minus, plus] of [["전체", shrinkButton, enlargeButton], ["너비", widthShrinkButton, widthEnlargeButton], ["높이", heightShrinkButton, heightEnlargeButton], ["앞뒤 길이", depthShrinkButton, depthEnlargeButton]]) {
    const row = document.createElement("div"), name = document.createElement("span");
    Object.assign(row.style, { display: "flex", gap: "5px", alignItems: "center" });
    name.textContent = label; Object.assign(name.style, { flex: "1", color: "#cfe5ef", fontSize: "12px" });
    minus.textContent = "−"; plus.textContent = "+";
    row.append(name, minus, plus); sizePanel.append(row); sizeRows.push({ row, minus });
  }
  sceneActionBar.append(sizePanel);
  container.appendChild(sceneActionBar);
  const flat = page.viewMode === "PLAN_2D";
  renderer.shadowMap.enabled = !flat;
  const scene = new THREE.Scene(); scene.background = new THREE.Color(0x07111b); scene.fog = null;
  const camera = flat
    ? new THREE.OrthographicCamera(-11, 11, 6.6, -6.6, .1, 100)
    : new THREE.PerspectiveCamera(43, 1, .1, 100);
  if (flat) camera.position.set(0, 22, .01); else camera.position.set(13, 10, 16);
  const controls = new OrbitControls(camera, renderer.domElement); controls.target.set(0, .9, 0); controls.enableDamping = true; controls.maxPolarAngle = Math.PI * .5; controls.minDistance = 5; controls.maxDistance = 60;
  if (flat) { controls.target.set(0, 0, 0); controls.enableRotate = false; controls.maxPolarAngle = .001; controls.minPolarAngle = .001; controls.minZoom = .55; controls.maxZoom = 2.5; }
  const viewStorageKey = `dcim.page-scene-view:${page.pageCode || "default"}:${page.viewMode || "SCENE_3D"}`;
  const defaultViewStorageKey = `${viewStorageKey}:default`;
  let defaultCameraPosition = camera.position.clone();
  let defaultCameraTarget = controls.target.clone();
  let defaultCameraZoom = camera.zoom;
  let viewSaveTimer = 0;
  const saveViewState = () => {
    try {
      localStorage.setItem(viewStorageKey, JSON.stringify({ camera: camera.position.toArray(), target: controls.target.toArray(), zoom: camera.zoom }));
    } catch { /* Camera preference is optional when browser storage is unavailable. */ }
  };
  const queueViewStateSave = () => {
    clearTimeout(viewSaveTimer);
    viewSaveTimer = window.setTimeout(saveViewState, 280);
  };
  try {
    const savedDefault = JSON.parse(localStorage.getItem(defaultViewStorageKey) || "null");
    if (Array.isArray(savedDefault?.camera) && savedDefault.camera.length === 3
        && Array.isArray(savedDefault?.target) && savedDefault.target.length === 3) {
      defaultCameraPosition.fromArray(savedDefault.camera);
      defaultCameraTarget.fromArray(savedDefault.target);
      if (flat && Number.isFinite(savedDefault.zoom)) defaultCameraZoom = THREE.MathUtils.clamp(savedDefault.zoom, controls.minZoom, controls.maxZoom);
    }
  } catch { /* Ignore malformed or unavailable default camera preferences. */ }
  try {
    const saved = JSON.parse(localStorage.getItem(viewStorageKey) || "null");
    if (Array.isArray(saved?.camera) && saved.camera.length === 3 && Array.isArray(saved?.target) && saved.target.length === 3) {
      camera.position.fromArray(saved.camera);
      controls.target.fromArray(saved.target);
      if (flat && Number.isFinite(saved.zoom)) camera.zoom = THREE.MathUtils.clamp(saved.zoom, controls.minZoom, controls.maxZoom);
      camera.updateProjectionMatrix(); controls.update();
    }
  } catch { /* Ignore malformed or unavailable saved camera preferences. */ }
  controls.addEventListener("change", queueViewStateSave);
  controls.addEventListener("end", saveViewState);
  const resetViewToDefault = () => {
    clearTimeout(viewSaveTimer);
    controls.reset();
    camera.position.copy(defaultCameraPosition);
    controls.target.copy(defaultCameraTarget);
    camera.zoom = defaultCameraZoom;
    camera.updateProjectionMatrix();
    controls.update();
    saveViewState();
    options.onHint?.("화면을 기본 시점으로 되돌렸습니다.");
  };
  const saveCurrentViewAsDefault = () => {
    defaultCameraPosition.copy(camera.position);
    defaultCameraTarget.copy(controls.target);
    defaultCameraZoom = camera.zoom;
    try {
      localStorage.setItem(defaultViewStorageKey, JSON.stringify({
        camera: defaultCameraPosition.toArray(), target: defaultCameraTarget.toArray(), zoom: defaultCameraZoom,
      }));
    } catch { options.onHint?.("브라우저 저장소에 접근할 수 없어 기본 시점을 저장하지 못했습니다."); return false; }
    saveViewState();
    options.onHint?.("현재 화면을 이 페이지의 기본 시점으로 저장했습니다. 오른쪽 클릭하면 이 시점으로 돌아옵니다.");
    return true;
  };
  scene.add(new THREE.HemisphereLight(0xbfeaff, 0x25363e, 2.2));
  const keyLight = new THREE.DirectionalLight(0xe5f6ff, 3.2); keyLight.position.set(4, 12, 7); keyLight.castShadow = !flat;
  keyLight.shadow.mapSize.set(2048, 2048);
  Object.assign(keyLight.shadow.camera, { left: -40, right: 40, top: 40, bottom: -40, near: .5, far: 150 });
  keyLight.shadow.camera.updateProjectionMatrix(); keyLight.shadow.bias = -.0001; keyLight.shadow.normalBias = .02;
  scene.add(keyLight);
  const fill = new THREE.PointLight(0x2ea7ca, 70, 25); fill.position.set(-6, 4, -3); scene.add(fill);
  // The scene starts empty: users build the floor and walls from the asset dock.
  let walking = false;
  let wholeSceneMoveMode = false;
  let areaSelectMode = false, areaMoveMode = false, areaSelectEquipmentOnly = false;
  const areaKeys = new Set(), areaHelpers = [];
  const areaRectangle = document.createElement("div");
  Object.assign(areaRectangle.style, { position: "absolute", display: "none", zIndex: "15", pointerEvents: "none",
    border: "1px solid #47dfff", background: "rgba(39,212,239,.18)" });
  container.appendChild(areaRectangle);
  function clearAreaHelpers() {
    for (const helper of areaHelpers) { helper.parent?.remove(helper); helper.geometry.dispose(); helper.material.dispose(); }
    areaHelpers.length = 0;
  }
  function refreshAreaHelpers() {
    clearAreaHelpers();
    for (const key of areaKeys) {
      const group = itemGroups.get(key); if (!group) continue;
      const helper = new THREE.Box3Helper(footprintBounds(group), 0x42e8ff); helper.material.depthTest = false; helper.renderOrder = 30;
      scene.add(helper); areaHelpers.push(helper);
    }
    options.onAreaSelectionChange?.(areaKeys.size);
  }
  let walkAnimationTime = 0;
  let lastFrameTime = performance.now();
  const defaultMouseButtons = flat
    ? { LEFT: THREE.MOUSE.PAN, MIDDLE: THREE.MOUSE.PAN, RIGHT: THREE.MOUSE.PAN }
    : { LEFT: THREE.MOUSE.ROTATE, MIDDLE: THREE.MOUSE.PAN, RIGHT: THREE.MOUSE.ROTATE };
  function updateControlMode() {
    controls.enabled = !dragState && !wholeSceneMoveMode && !areaSelectMode && !areaMoveMode;
    controls.mouseButtons = placement
      ? (flat
        ? { LEFT: THREE.MOUSE.PAN, MIDDLE: THREE.MOUSE.PAN, RIGHT: THREE.MOUSE.PAN }
        : { LEFT: THREE.MOUSE.ROTATE, MIDDLE: THREE.MOUSE.PAN, RIGHT: THREE.MOUSE.ROTATE })
      : { ...defaultMouseButtons };
  }
  const preventCanvasContextMenu = (event) => event.preventDefault();
  const onRightClickResetView = (event) => {
    if (event.button !== 2) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    resetViewToDefault();
  };
  renderer.domElement.addEventListener("contextmenu", preventCanvasContextMenu);
  renderer.domElement.addEventListener("pointerdown", onRightClickResetView, true);
  const player = new THREE.Group();
  const suit = new THREE.MeshStandardMaterial({ color: 0x36bfd0, metalness: .04, roughness: .68 });
  const skin = new THREE.MeshStandardMaterial({ color: 0xffd7b5, roughness: .78 });
  const jointMat = new THREE.MeshStandardMaterial({ color: 0xffd7b5, roughness: .72 });
  const accentMat = new THREE.MeshStandardMaterial({ color: 0xffbc5e, roughness: .62 });
  const darkMat = new THREE.MeshStandardMaterial({ color: 0x263747, roughness: .58 });
  const blushMat = new THREE.MeshStandardMaterial({ color: 0xff8f9d, roughness: .8 });
  const rod = (length, radius, material = suit) => {
    const mesh = new THREE.Mesh(new THREE.CylinderGeometry(radius * .78, radius, length, 8), material);
    mesh.position.y = -length / 2; mesh.castShadow = true; return mesh;
  };
  const torso = new THREE.Mesh(new THREE.SphereGeometry(.205, 18, 14), suit); torso.scale.set(.82, 1.38, .72); torso.position.y = .88; torso.castShadow = true; player.add(torso);
  const backpack = new THREE.Mesh(new THREE.BoxGeometry(.2, .28, .12), new THREE.MeshStandardMaterial({ color: 0x7387dc, roughness: .72 })); backpack.position.set(0, .91, -.16); backpack.castShadow = true; player.add(backpack);
  const neck = new THREE.Mesh(new THREE.CylinderGeometry(.045, .05, .1, 10), accentMat); neck.position.y = 1.19; player.add(neck);
  const hood = new THREE.Mesh(new THREE.SphereGeometry(.22, 20, 16), suit); hood.position.set(0, 1.4, -.025); hood.scale.set(1, 1.05, .85); hood.castShadow = true; player.add(hood);
  const head = new THREE.Mesh(new THREE.SphereGeometry(.17, 20, 16), skin); head.position.set(0, 1.4, .065); player.add(head);
  const hair = new THREE.Mesh(new THREE.SphereGeometry(.155, 16, 10, 0, Math.PI * 2, 0, Math.PI * .42), darkMat); hair.position.set(0, 1.47, .07); player.add(hair);
  for (const side of [-1, 1]) {
    const ear = new THREE.Mesh(new THREE.SphereGeometry(.052, 12, 10), skin); ear.position.set(side * .16, 1.42, .055); ear.scale.set(.82, 1.1, .55); player.add(ear);
    const eye = new THREE.Mesh(new THREE.SphereGeometry(.022, 10, 8), darkMat); eye.position.set(side * .06, 1.405, .218); player.add(eye);
    const cheek = new THREE.Mesh(new THREE.SphereGeometry(.027, 10, 8), blushMat); cheek.position.set(side * .103, 1.355, .204); cheek.scale.set(1.2, .65, .35); player.add(cheek);
  }
  const smile = new THREE.Mesh(new THREE.SphereGeometry(.024, 12, 8), darkMat); smile.position.set(0, 1.345, .222); smile.scale.set(1.35, .36, .3); player.add(smile);
  const hairTuft = new THREE.Mesh(new THREE.SphereGeometry(.055, 12, 10), darkMat); hairTuft.position.set(.035, 1.59, .04); hairTuft.scale.set(.8, .72, .8); player.add(hairTuft);
  const shoulder = new THREE.Mesh(new THREE.SphereGeometry(.13, 12, 10), suit); shoulder.position.y = 1.06; player.add(shoulder);
  const hip = new THREE.Mesh(new THREE.SphereGeometry(.11, 12, 10), suit); hip.position.y = .59; player.add(hip);
  const armPivots = [], legPivots = [];
  for (const side of [-1, 1]) {
    const arm = new THREE.Group(); arm.position.set(side * .13, 1.03, 0); arm.add(rod(.48, .045));
    const hand = new THREE.Mesh(new THREE.SphereGeometry(.045, 8, 6), skin); hand.position.y = -.48; arm.add(hand); player.add(arm); armPivots.push(arm);
    const leg = new THREE.Group(); leg.position.set(side * .065, .57, 0); leg.add(rod(.52, .055, jointMat));
    const foot = new THREE.Mesh(new THREE.BoxGeometry(.12, .065, .2), suit); foot.position.set(0, -.5, .055); leg.add(foot); player.add(leg); legPivots.push(leg);
  }
  player.position.set(0, 0, 4.4); player.visible = false; scene.add(player);
  const suppliedItems = Array.isArray(page.items) ? structuredClone(page.items) : [];
  let items = suppliedItems.map((item) => ({
    ...item,
    sizeY: Number(item.sizeY) || Number(item.sizeX) || 4,
    sizeZ: item.sizeZ,
    clientKey: item.clientKey || (item.locationNodeCode ? `location-${item.locationNodeCode}` : `db-${item.id}`),
  }));
  const vacantFloorSlots = [];
  const rememberVacantFloorSlot = (x, z, sizeX = 4, sizeZ = 4) => {
    if (!Number.isFinite(Number(x)) || !Number.isFinite(Number(z))) return;
    if (!vacantFloorSlots.some((slot) => Math.abs(slot.x - Number(x)) < .025 && Math.abs(slot.z - Number(z)) < .025)) {
      vacantFloorSlots.push({ x: Number(x), z: Number(z), sizeX: Number(sizeX) || 4, sizeZ: Number(sizeZ) || 4 });
    }
  };
  const consumeVacantFloorSlot = (x, z) => {
    const index = vacantFloorSlots.findIndex((slot) => Math.abs(slot.x - Number(x)) < .025 && Math.abs(slot.z - Number(z)) < .025);
    if (index >= 0) vacantFloorSlots.splice(index, 1);
  };
  const itemGroups = new Map();
  for (const item of items.filter((entry) => entry.itemType !== "SENSOR" && entry.itemType !== "DEVICE" && entry.parentId == null && !entry.parentClientKey)) {
    const group = item.itemType === "RACK" ? makeRack(item) : makeEquipment(item);
    if (item.modelKey === "FLOOR") group.scale.set((Number(item.sizeX) || 4) / 4, 1, (Number(item.sizeZ) || 4) / 4);
    else if (isCeilingModel(item.modelKey)) rebuildCeilingSurface(group, items, item);
    group.position.set(Number(item.x), Number(item.y || 0), Number(item.z)); applyItemRotation(item, group);
    scene.add(group); itemGroups.set(findItemKey(item), group);
  }
  for (const wall of items.filter((item) => isWallModel(item.modelKey) && Number(item.y || 0) > 0)
    .sort((a, b) => Number(a.y) - Number(b.y))) {
    const support = wallTopSupport(wall, Number(wall.x), Number(wall.z), Number(wall.y));
    const group = itemGroups.get(findItemKey(wall));
    if (support && group) { wall.y = support.topY; group.position.y = support.topY; }
  }
  const refreshCeilingSurfaces = () => {
    for (const ceiling of items.filter((item) => isCeilingModel(item.modelKey))) {
      const group = itemGroups.get(findItemKey(ceiling));
      if (group) rebuildCeilingSurface(group, items, ceiling);
    }
  };
  refreshCeilingSurfaces();
  for (const item of items.filter((entry) => entry.itemType === "EQUIPMENT" && (entry.parentId != null || entry.parentClientKey))) {
    const parentKey = item.parentId != null ? `db-${item.parentId}` : item.parentClientKey;
    const parent = itemGroups.get(parentKey);
    const wallSegment = parent?.userData.wallSegment;
    if (!wallSegment || !(isDoorModel(item.modelKey) || isWindowModel(item.modelKey))) continue;
    const group = makeEquipment(item);
    group.position.set(Number(item.x || 0), Number(item.y || 0), Number(item.z || 0));
    wallSegment.add(group); itemGroups.set(findItemKey(item), group);
  }
  for (const wall of items.filter((entry) => isWallModel(entry.modelKey) && entry.itemType === "EQUIPMENT")) {
    const wallGroup = itemGroups.get(findItemKey(wall));
    if (!wallGroup?.userData.wallSegment) continue;
    rebuildWallSurface(wallGroup.userData.wallSegment, openingsForWall(wall), wall.modelKey, Number(wall.sizeX) || 4, wallStackJoins(wall), 2.8 * (Number(wall.sizeZ) || 4) / 4);
  }
  function openingsForWall(wall) {
    return items.filter((entry) => (isDoorModel(entry.modelKey) || isWindowModel(entry.modelKey))
      && (entry.parentId != null ? Number(entry.parentId) === Number(wall.id) : entry.parentClientKey === findItemKey(wall)));
  }
  function refreshWallSurface(wall) {
    const wallGroup = itemGroups.get(findItemKey(wall));
    if (wallGroup?.userData.wallSegment) rebuildWallSurface(wallGroup.userData.wallSegment, openingsForWall(wall), wall.modelKey, Number(wall.sizeX) || 4, wallStackJoins(wall), 2.8 * (Number(wall.sizeZ) || 4) / 4);
  }
  function wallStackJoins(wall) {
    const alongZ = Math.abs(Math.round(Number(wall.rotationY || 0) / 90)) % 2 === 1;
    const center = alongZ ? Number(wall.z) : Number(wall.x);
    const cross = alongZ ? Number(wall.x) : Number(wall.z);
    const halfLength = (Number(wall.sizeX) || 4) / 2;
    const coveredAt = (top) => {
      const intervals = items.filter((other) => other !== wall && isWallModel(other.modelKey)
        && (Math.abs(Math.round(Number(other.rotationY || 0) / 90)) % 2 === 1) === alongZ
        && Math.abs((alongZ ? Number(other.x) : Number(other.z)) - cross) < .12
        && Math.abs(top ? Number(other.y || 0) - (Number(wall.y || 0) + 2.8 * (Number(wall.sizeZ) || 4) / 4)
          : Number(other.y || 0) + 2.8 * (Number(other.sizeZ) || 4) / 4 - Number(wall.y || 0)) < .12)
        .map((other) => { const c = alongZ ? Number(other.z) : Number(other.x), h = (Number(other.sizeX) || 4) / 2; return [c - h, c + h]; });
      for (let pos = center - halfLength; pos <= center + halfLength + .001; pos += .1)
        if (!intervals.some(([min, max]) => pos >= min - .08 && pos <= max + .08)) return false;
      return intervals.length > 0;
    };
    const angle = THREE.MathUtils.degToRad(Number(wall.rotationY || 0));
    const ux = Math.cos(angle), uz = -Math.sin(angle);
    const sideCovered = (direction) => items.some((other) => {
      if (other === wall || other.modelKey !== wall.modelKey || !["WALL_GLASS", "WALL_GLASS_CLEAR"].includes(other.modelKey)) return false;
      if ((Math.abs(Math.round(Number(other.rotationY || 0) / 90)) % 2 === 1) !== alongZ) return false;
      if (Math.abs(Number(other.y || 0) - Number(wall.y || 0)) > .05
        || Math.abs((Number(other.sizeZ) || 4) - (Number(wall.sizeZ) || 4)) > .01) return false;
      const dx = Number(other.x) - Number(wall.x), dz = Number(other.z) - Number(wall.z);
      const along = dx * ux + dz * uz, across = -dx * uz + dz * ux;
      const distance = halfLength + (Number(other.sizeX) || 4) / 2;
      return Math.abs(across) < .06 && Math.abs(along - direction * distance) < .06;
    });
    return { bottom: coveredAt(false), top: coveredAt(true), left: sideCovered(-1), right: sideCovered(1) };
  }
  function refreshAllWallSurfaces() {
    for (const wall of items.filter((item) => isWallModel(item.modelKey))) refreshWallSurface(wall);
  }
  const unsupportedRoots = new Set();
  for (const ceiling of items.filter((item) => isCeilingModel(item.modelKey))) {
    const support = ceilingWallSupport(Number(ceiling.x || 0), Number(ceiling.z || 0), ceiling.sizeX, ceiling.sizeZ, Number(ceiling.y || 0));
    const group = itemGroups.get(findItemKey(ceiling));
    if (!support || !group) unsupportedRoots.add(findItemKey(ceiling));
    else { ceiling.y = support.wallTop; group.position.y = support.wallTop; }
  }
  const initialFloors = installedSupportSurfaces().filter(({ item }) => !unsupportedRoots.has(findItemKey(item)));
  for (const item of items) {
    if (["FLOOR", "CEILING", "CEILING_CLEAR"].includes(item.modelKey) || item.parentId != null || item.parentClientKey) continue;
    const group = itemGroups.get(findItemKey(item));
    if (!group || (!isFootprintOnInstalledFloor(footprintBounds(group), initialFloors, Number(item.y || 0))
      && !(isWallModel(item.modelKey) && wallTopSupport(item, Number(item.x), Number(item.z), Number(item.y || 0))))) {
      unsupportedRoots.add(findItemKey(item));
    }
  }
  let removedUnsupportedChild = true;
  while (removedUnsupportedChild) {
    removedUnsupportedChild = false;
    for (const item of items) {
      if (unsupportedRoots.has(findItemKey(item))) continue;
      const parentKey = item.parentId != null ? `db-${item.parentId}` : item.parentClientKey;
      if (parentKey && unsupportedRoots.has(parentKey)) {
        unsupportedRoots.add(findItemKey(item)); removedUnsupportedChild = true;
      }
    }
  }
  if (unsupportedRoots.size) {
    for (const key of unsupportedRoots) {
      const group = itemGroups.get(key); group?.parent?.remove(group); itemGroups.delete(key);
    }
    items = items.filter((item) => !unsupportedRoots.has(findItemKey(item)));
  }
  const devicesByPoint = new Map(), devicesById = new Map();
  for (const device of page.devices || []) {
    devicesById.set(device.deviceId, device);
    for (const point of device.points || []) devicesByPoint.set(`${device.deviceId}|${point.pointName}`, { device, point });
  }
  for (const item of items.filter((entry) => entry.itemType === "SENSOR")) {
    const parent = itemGroups.get(item.parentId != null ? `db-${item.parentId}` : item.parentClientKey);
    if (!parent) continue;
    const pair = devicesByPoint.get(`${item.deviceId}|${item.pointName}`);
    const valueText = pair?.point?.value == null ? "—" : `${Number(pair.point.value).toLocaleString("ko-KR", { maximumFractionDigits: 1 })}${pair.point.unit ? ` ${pair.point.unit}` : ""}`;
    const group = makeSensor(item, valueText); group.position.set(Number(item.x || 0), Number(item.y || 1.8), Number(item.z || 0)); parent.add(group);
    itemGroups.set(findItemKey(item), group);
  }
  for (const item of items.filter((entry) => entry.itemType === "DEVICE")) {
    const parent = itemGroups.get(item.parentId != null ? `db-${item.parentId}` : item.parentClientKey);
    if (!parent) continue;
    const device = devicesById.get(item.deviceId);
    const summary = (device?.points || []).filter((point) => point.value != null).slice(0, 2)
      .map((point) => `${point.pointName} ${Number(point.value).toLocaleString("ko-KR", { maximumFractionDigits: 1 })}${point.unit ? ` ${point.unit}` : ""}`).join(" · ");
    const group = makeSensor(item, summary || "최신값 없음");
    group.position.set(Number(item.x || 0), Number(item.y || 1.8), Number(item.z || 0));
    parent.add(group); itemGroups.set(findItemKey(item), group);
  }

  const raycaster = new THREE.Raycaster(); const pointer = new THREE.Vector2(); const ground = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0);
  let dragState = null, placement = null, destroyed = false, raf = 0;
  const floorBatchPreview = new THREE.Group(); scene.add(floorBatchPreview);
  function clearFloorBatchPreview() {
    for (const mesh of [...floorBatchPreview.children]) {
      mesh.geometry.dispose(); mesh.material.dispose(); floorBatchPreview.remove(mesh);
    }
  }
  let hoveredItem = null, selectedItem = null, selectionBox = null, sceneActionsVisible = true, snapEnabled = true, mirrorNext = false, moveMode = false;
  const resize = () => {
    const rect = container.getBoundingClientRect(); if (!rect.width || !rect.height) return;
    renderer.setSize(rect.width, rect.height, false);
    if (flat) { const halfHeight = 6.6; camera.left = -halfHeight * rect.width / rect.height; camera.right = halfHeight * rect.width / rect.height; camera.top = halfHeight; camera.bottom = -halfHeight; }
    else camera.aspect = rect.width / rect.height;
    camera.updateProjectionMatrix();
  };
  const ro = new ResizeObserver(resize); ro.observe(container); resize();
  const intersect = (event, objects) => {
    const rect = renderer.domElement.getBoundingClientRect(); pointer.set(((event.clientX - rect.left) / rect.width) * 2 - 1, -((event.clientY - rect.top) / rect.height) * 2 + 1); raycaster.setFromCamera(pointer, camera);
    return raycaster.intersectObjects(objects, true);
  };
  function localWallPoint(event, wall) {
    const wallGroup = itemGroups.get(findItemKey(wall));
    const segment = wallGroup?.userData.wallSegment;
    if (!segment) return null;
    segment.updateWorldMatrix(true, false);
    const normal = new THREE.Vector3(0, 0, 1).transformDirection(segment.matrixWorld);
    const anchor = segment.localToWorld(new THREE.Vector3(0, 1.4, 0));
    const plane = new THREE.Plane().setFromNormalAndCoplanarPoint(normal, anchor);
    intersect(event, []);
    const point = new THREE.Vector3();
    if (!raycaster.ray.intersectPlane(plane, point)) return null;
    return segment.worldToLocal(point);
  }
  const roots = () => [...itemGroups.values()];
  const isEditableSceneRoot = (root) => {
    const item = root?.userData?.sceneItem;
    return Boolean(item && ["RACK", "EQUIPMENT"].includes(item.itemType));
  };
  const walkKeys = new Set();
  const walkKeyCodes = new Set(["KeyW", "KeyA", "KeyS", "KeyD", "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"]);
  const onKeyDown = (event) => {
    if (event.target instanceof HTMLElement && event.target.closest("input,select,textarea,[contenteditable=true]")) return;
    if (event.code === "Escape" && placement) { cancelPlacement(); return; }
    if (event.code === "Escape" && wholeSceneMoveMode) { api.setWholeSceneMove(false); return; }
    if (event.code === "Escape" && (areaSelectMode || areaMoveMode)) { api.setAreaSelect(false); api.setAreaMove(false); return; }
    if (event.code === "Escape" && walking) { setWalking(false); return; }
    if (event.code === "Escape" && areaKeys.size) { areaKeys.clear(); refreshAreaHelpers(); options.onHint?.("영역 선택을 해제했습니다."); return; }
    if (walking) {
      if (walkKeyCodes.has(event.code)) { walkKeys.add(event.code); event.preventDefault(); }
      return;
    }
    if (options.editing && options.canBuild && !placement && !dragState && !areaSelectMode && !wholeSceneMoveMode
      && ["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(event.code)
      && (areaKeys.size > 0 || Boolean(selectedItem))) {
      event.preventDefault(); moveSelectionByKeyboard(event);
    }
  };
  const onKeyUp = (event) => { walkKeys.delete(event.code); };
  const clearWalkKeys = () => walkKeys.clear();
  const updateSelectionBox = () => {
    selectionBox?.parent?.remove(selectionBox); selectionBox?.geometry?.dispose(); selectionBox?.material?.dispose(); selectionBox = null;
    if (!selectedItem) return;
    const group = itemGroups.get(findItemKey(selectedItem)); if (!group) return;
    selectionBox = new THREE.Box3Helper(footprintBounds(group), 0x39e4ff); selectionBox.material.depthTest = false; selectionBox.material.transparent = true; selectionBox.material.opacity = .95; selectionBox.renderOrder = 30; scene.add(selectionBox);
  };
  const updateSceneActionBar = () => {
    const item = placement?.item || selectedItem;
    const group = placement?.group || (selectedItem ? itemGroups.get(findItemKey(selectedItem)) : null);
    const canShow = options.editing && options.canBuild && !walking && !areaSelectMode && !areaMoveMode && item && group && (placement || sceneActionsVisible)
      && sceneActionsVisible && (!item.modelKey || item.itemType === "RACK" || item.itemType === "EQUIPMENT");
    if (!canShow) { sceneActionBar.style.display = "none"; return; }
    const attachedOpening = (item.parentId != null || item.parentClientKey)
      && (isDoorModel(item.modelKey) || isWindowModel(item.modelKey));
    const canRotate = !item.locked && !item.locationNodeCode && !attachedOpening && !["FLOOR", "CEILING", "CEILING_CLEAR"].includes(item.modelKey);
    rotateLeftButton.hidden = !canRotate; rotateRightButton.hidden = !canRotate;
    const canResize = !item.locked && ["RACK", "EQUIPMENT"].includes(item.itemType)
      && (isWallModel(item.modelKey) || isCeilingModel(item.modelKey)
        || (!isDoorModel(item.modelKey) && !isWindowModel(item.modelKey)
          && !["FLOOR", "LIGHT", "PIPE", "STAIR"].includes(item.modelKey)));
    shrinkButton.hidden = !canResize; enlargeButton.hidden = !canResize;
    const isFrame = isEquipmentFrameKey(item.modelKey);
    const isEquipment = isFrame || isFrameEquipmentItem(item);
    lockFocusedButton.hidden = Boolean(placement) || !isEquipment || item.parentId != null || Boolean(item.parentClientKey);
    lockFocusedButton.textContent = item.locked ? "잠금 해제" : "잠금";
    lockFocusedButton.setAttribute("aria-pressed", String(Boolean(item.locked)));
    removeFocusedButton.disabled = Boolean(item.locked);
    removeFocusedButton.style.opacity = item.locked ? ".45" : "1";
    copyFocusedButton.hidden = Boolean(placement) || !isEquipment || item.parentId != null || Boolean(item.parentClientKey);
    widthShrinkButton.hidden = !isEquipment || Boolean(item.locked); widthEnlargeButton.hidden = !isEquipment || Boolean(item.locked);
    shrinkButton.title = isFrame ? "프레임 전체 크기 줄이기" : "설비 크기 줄이기";
    enlargeButton.title = isFrame ? "프레임 전체 크기 늘리기" : "설비 크기 늘리기";
    depthShrinkButton.title = isFrame ? "프레임 세로 길이 줄이기" : "천장 세로 폭 줄이기";
    depthEnlargeButton.title = isFrame ? "프레임 세로 길이 늘리기" : "천장 세로 폭 늘리기";
    depthShrinkButton.hidden = Boolean(item.locked) || !(isCeilingModel(item.modelKey) || isEquipment);
    depthEnlargeButton.hidden = Boolean(item.locked) || !(isCeilingModel(item.modelKey) || isEquipment);
    heightShrinkButton.hidden = !isEquipment || Boolean(item.locked);
    heightEnlargeButton.hidden = !isEquipment || Boolean(item.locked);
    heightShrinkButton.title = "설비 높이 줄이기"; heightEnlargeButton.title = "설비 높이 늘리기";
    if (isEquipment) { depthShrinkButton.title = "설비 앞뒤 길이 줄이기"; depthEnlargeButton.title = "설비 앞뒤 길이 늘리기"; }
    selectedLabel.textContent = `${item.locked ? "🔒 " : ""}${item.label || item.modelKey || "선택한 설비"}`;
    const showRackColor = !placement && isColorableRack(item);
    rackColorLabel.style.display = showRackColor ? "flex" : "none"; rackColorInput.disabled = Boolean(item.locked);
    rackColorInput.value = /^#[0-9a-f]{6}$/i.test(item.rackColor || "") ? item.rackColor : "#365562";
    rackColorReset.hidden = !showRackColor; rackColorReset.disabled = Boolean(item.locked);
    selectGroupButton.hidden = !item.groupKey || Boolean(placement);
    sizeToggle.hidden = !canResize;
    if (!canResize) { sizePanel.style.display = "none"; sizeToggle.setAttribute("aria-expanded", "false"); }
    for (const { row, minus } of sizeRows) row.style.display = minus.hidden ? "none" : "flex";
    sceneActionBar.style.display = "flex";
    removeFocusedButton.textContent = placement ? "배치 취소" : "삭제";
    removeFocusedButton.title = placement ? "현재 배치 취소" : "선택한 오브젝트 삭제";
  };
  const nearestSceneItemRoot = (object) => {
    let node = object;
    while (node && node !== scene) { if (node.userData?.sceneItem) return node; node = node.parent; }
    return null;
  };
  const onHover = (event) => {
    if (walking || dragState || placement) return;
    const hit = intersect(event, roots()).find((entry) => isEditableSceneRoot(nearestSceneItemRoot(entry.object)));
    hoveredItem = hit ? nearestSceneItemRoot(hit.object)?.userData?.sceneItem || null : null;
  };
  function duplicateItem(source) {
    if (!source || !(isFrameEquipmentItem(source) || isEquipmentFrameKey(source.modelKey))
      || source.parentId != null || source.parentClientKey) return false;
    return beginPlacement(source.modelKey, source.rotationY, null, source);
  }
  function selectItem(key) {
    if (!selectedItem || findItemKey(selectedItem) !== key) sceneActionsVisible = true;
    selectedItem = key ? items.find((item) => findItemKey(item) === key) || null : null;
    updateSelectionBox();
    return selectedItem;
  }
  function flipItem(key) {
    return rotateItem(key, 180);
  }
  function rotateItem(key, degrees = 90) {
    const target = items.find((item) => findItemKey(item) === key);
    if (target?.locked) { options.onHint?.("잠긴 설비는 먼저 잠금을 해제하세요."); return false; }
    if (!target || !["EQUIPMENT", "RACK"].includes(target.itemType) || target.locationNodeCode
      || ["FLOOR", "CEILING", "CEILING_CLEAR"].includes(target.modelKey)) return false;
    target.rotationY = THREE.MathUtils.euclideanModulo(Number(target.rotationY || 0) + degrees, 360);
    const group = itemGroups.get(findItemKey(target)); if (group) applyItemRotation(target, group);
    updateSelectionBox();
    options.onChange?.(items); options.onHint?.(`${target.label} 방향을 ${target.rotationY}°로 돌렸습니다.`); return true;
  }
  function rotateFocusedItem(degrees) {
    if (placement) {
      placement.item.rotationY = THREE.MathUtils.euclideanModulo(Number(placement.item.rotationY || 0) + degrees, 360);
      applyItemRotation(placement.item, placement.group);
      updatePlacementPreview();
      options.onHint?.(`${placement.item.label} 방향을 ${placement.item.rotationY}°로 돌렸습니다.`);
      return true;
    }
    return selectedItem ? rotateItem(findItemKey(selectedItem), degrees) : false;
  }
  function toggleSnap() { snapEnabled = !snapEnabled; if (!snapEnabled) clearGuides(); options.onHint?.(`정렬 스냅 ${snapEnabled ? "켜짐" : "꺼짐"}`); return snapEnabled; }
  function applyItemRotation(item, group) {
    const degrees = Number(item.rotationY || 0);
    group.rotation.y = THREE.MathUtils.degToRad(degrees);
    // Walls keep the same 4m module in every direction so corner joints line up.
    if (group.userData.wallSegment) group.userData.wallSegment.scale.x = 1;
  }
  function setMoveTarget(key) {
    const target = selectItem(key);
    moveMode = Boolean(target && ["RACK", "EQUIPMENT"].includes(target.itemType) && target.modelKey !== "FLOOR");
    options.onHint?.(moveMode ? `${target.label} 선택됨 · 선택한 오브젝트를 드래그해 이동하세요.` : "이동할 랙/설비를 선택하세요.");
    return moveMode;
  }
  function cancelPlacement() {
    if (!placement) return;
    clearFloorBatchPreview();
    placement.group.parent?.remove(placement.group);
    placement = null; updateControlMode(); clearGuides(); options.onHint?.("배치가 취소되었습니다.");
  }
  function beginPlacement(modelKey, initialRotation = null, wallFloor = null, copySource = null) {
    if (walking) setWalking(false);
    areaSelectMode = false; areaMoveMode = false; areaRectangle.style.display = "none";
    if (wholeSceneMoveMode) { wholeSceneMoveMode = false; renderer.domElement.style.cursor = ""; options.onWholeSceneMoveChange?.(false); }
    const allowed = ["RACK", "PDU", "CRAC", "UPS", "CHILLER", "SWITCH", "SERVER", "GENERIC", "MACHINE", "CONVEYOR", "TANK", "TABLE", "WALL", "WALL_CONCRETE", "WALL_GLASS", "WALL_GLASS_CLEAR", "WALL_PANEL", "CEILING", "CEILING_CLEAR", "FLOOR", "LIGHT", "PIPE", "STAIR"];
    if (!allowed.includes(modelKey) && !showroomModelByKey(modelKey)) return false;
    sceneActionsVisible = true;
    if (isDoorModel(modelKey) || isWindowModel(modelKey)) {
      options.onHint?.("문·창문은 먼저 벽을 클릭해 선택한 뒤 카탈로그에서 설치하세요."); return false;
    }
    if (modelKey !== "FLOOR" && !items.some((item) => item.modelKey === "FLOOR")) {
      options.onHint?.("바닥이 설치된 곳에만 설비를 배치할 수 있습니다. 바닥을 먼저 설치하세요."); return false;
    }
    cancelPlacement();
    const n = items.filter((item) => item.itemType === "EQUIPMENT" && item.modelKey === modelKey).length + 1;
    const isBuildingPart = isWallModel(modelKey) || isDoorModel(modelKey) || isWindowModel(modelKey)
      || ["FLOOR", "CEILING", "CEILING_CLEAR", "LIGHT", "PIPE", "STAIR"].includes(modelKey);
    const inrowDefaultSize = modelKey === "SR_INROW_PANEL" ? 4 : modelKey === "SR_INROW_RACK" ? 8 : modelKey === "SR_INROW" ? 7 : null;
    const item = { id: null, clientKey: makeKey(), parentId: null, parentClientKey: null, itemType: "EQUIPMENT", label: `${builtinAssetLabels.get(modelKey) || showroomModelByKey(modelKey)?.label || modelKey} ${n}`, modelKey, deviceId: null, pointName: null, x: 0, y: 0, z: 0, rotationY: initialRotation == null ? (mirrorNext ? 180 : 0) : THREE.MathUtils.euclideanModulo(initialRotation, 360), sizeX: isBuildingPart ? 5 : (inrowDefaultSize ?? (showroomModelByKey(modelKey) ? 12 : 6)), sizeY: isEquipmentFrameKey(modelKey) ? (inrowDefaultSize ?? 12) : 4, sizeZ: isEquipmentFrameKey(modelKey) ? 12 : isBuildingPart ? 5 : 4 };
    if (!isBuildingPart && !isEquipmentFrameKey(modelKey)) item.sizeY = item.sizeZ = item.sizeX;
    // Match the server rack already placed in the Environment scene.
    if (!copySource && modelKey === "SR_RACK") { item.sizeX = 14; item.sizeY = 16; item.sizeZ = 16; }
    if (copySource) Object.assign(item, { itemType: copySource.itemType, label: `${String(copySource.label || modelKey).slice(0, 117)} 복사`,
      x: Number(copySource.x) || 0, y: Number(copySource.y) || 0, z: Number(copySource.z) || 0,
      sizeX: Number(copySource.sizeX) || 4, sizeY: Number(copySource.sizeY) || Number(copySource.sizeX) || 4,
      sizeZ: Number(copySource.sizeZ) || Number(copySource.sizeX) || 4, rackColor: copySource.rackColor || null, groupKey: null, groupName: null });
    if (isWallModel(modelKey) && wallFloor != null) item._wallFloor = wallFloor;
    const group = item.itemType === "RACK" ? makeRack(item) : makeEquipment(item); group.position.set(item.x, item.y, item.z); applyItemRotation(item, group);
    if (isCeilingModel(item.modelKey)) rebuildCeilingSurface(group, [...items, item], item);
    const opacityMemo = [];
    group.traverse((node) => {
      if (!node.isMesh && !node.isSprite) return;
      const original = Array.isArray(node.material) ? node.material : [node.material];
      const isMaterialArray = Array.isArray(node.material);
      const previewMaterials = original.map((material) => {
        const clone = material.clone(); opacityMemo.push([clone, clone.transparent, clone.opacity, clone.depthWrite]);
        clone.userData.previewBaseColor = clone.color?.clone();
        clone.transparent = true; clone.opacity *= .48; clone.depthWrite = false; return clone;
      });
      node.material = isMaterialArray ? previewMaterials : previewMaterials[0];
    });
    scene.add(group); placement = {
      item, group, opacityMemo, keepCopySize: Boolean(copySource),
      preferredSizeX: item.sizeX, preferredSizeY: item.sizeY, preferredSizeZ: item.sizeZ,
      previewMaterials: opacityMemo.map(([material]) => material).filter((material) => material.color && material.userData.previewBaseColor),
      invalidColor: new THREE.Color(0xff334f), invalidReason: "",
    };
    group.userData.onImportedModelReady = (root) => {
      if (!placement || placement.group !== group) return;
      root.traverse((node) => {
        if (!node.isMesh || !node.material) return;
        const original = Array.isArray(node.material) ? node.material : [node.material];
        const previewMaterials = original.map((source) => {
          const clone = source.clone(); opacityMemo.push([clone, clone.transparent, clone.opacity, clone.depthWrite]);
          clone.userData.previewBaseColor = clone.color?.clone();
          clone.transparent = true; clone.opacity *= .48; clone.depthWrite = false;
          if (clone.color && clone.userData.previewBaseColor) placement.previewMaterials.push(clone);
          return clone;
        });
        node.material = Array.isArray(node.material) ? previewMaterials : previewMaterials[0];
      });
      positionPlacementAt(item.x, item.z);
    };
    updateControlMode();
    if (copySource) { positionPlacementAt(item.x, item.z); options.onHint?.("복사할 위치를 클릭하세요. 원본과 크기·방향이 같으며 Esc로 취소합니다."); return true; }
    options.onHint?.(modelKey === "FLOOR" ? "바닥 배치 중 · 클릭해 한 칸 설치하거나 드래그해 여러 칸을 한 번에 채우세요 · Esc 취소" : isWallModel(modelKey) ? "벽 배치 중 · 클릭해 한 구간 설치하거나 벽 방향으로 드래그해 여러 구간을 설치하세요 · Esc 취소" : `${item.label} 배치 중 · 도면을 클릭해 설치 · Esc 취소 · T로 스냅 전환`);
    return true;
  }
  renderer.domElement.addEventListener("pointermove", onHover);
  window.addEventListener("keydown", onKeyDown);
  window.addEventListener("keyup", onKeyUp);
  window.addEventListener("blur", clearWalkKeys);
  function setWalking(enabled) {
    if (flat) { options.onHint?.("아바타 둘러보기는 3D 장면에서 사용할 수 있습니다."); return false; }
    const nextWalking = Boolean(enabled);
    if (nextWalking && !walking) {
      const surfaces = installedSupportSurfaces();
      if (!surfaces.length) { options.onHint?.("아바타가 걸을 바닥을 먼저 설치하세요."); return false; }
      const levels = [...new Set(surfaces.map(({ topY }) => Math.round(topY * 20) / 20))]
        .sort((a, b) => Math.abs(a - controls.target.y) - Math.abs(b - controls.target.y));
      const spawnBoxes = walkCollisionBoxes();
      let spawn = null, spawnY = 0;
      for (const level of levels) {
        const levelSurfaces = surfaces.filter(({ topY }) => Math.abs(topY - level) < .04);
        const candidates = [];
        for (const { box } of levelSurfaces) {
          for (let z = box.min.z + walkRadius; z <= box.max.z - walkRadius; z += .3) {
            for (let x = box.min.x + walkRadius; x <= box.max.x - walkRadius; x += .3) candidates.push({ x, z });
          }
          candidates.push({ x: (box.min.x + box.max.x) / 2, z: (box.min.z + box.max.z) / 2 });
        }
        candidates.sort((a, b) => ((a.x - controls.target.x) ** 2 + (a.z - controls.target.z) ** 2)
          - ((b.x - controls.target.x) ** 2 + (b.z - controls.target.z) ** 2));
        spawn = candidates.find(({ x, z }) => !isWalkBlocked(x, z, spawnBoxes, surfaces, level));
        if (spawn) { spawnY = level; break; }
      }
      if (!spawn) { options.onHint?.("바닥 위에 아바타를 놓을 빈 공간이 없습니다."); return false; }
      player.position.set(spawn.x, spawnY, spawn.z);
      const nextTarget = new THREE.Vector3(spawn.x, spawnY + .95, spawn.z);
      const cameraShift = nextTarget.clone().sub(controls.target);
      controls.target.copy(nextTarget); camera.position.add(cameraShift);
    }
    walking = nextWalking; player.visible = walking; walkKeys.clear();
    options.onWalkChange?.(walking);
    options.onHint?.(walking
      ? "WASD 또는 방향키로 이동 · Shift로 달리기 · 마우스로 시점 회전 · 휠로 확대/축소 · Esc 종료"
      : "아바타 둘러보기를 종료했습니다.");
    return true;
  }
  function updateWalking(deltaSeconds) {
    const up = new THREE.Vector3(0, 1, 0);
    const forward = camera.getWorldDirection(new THREE.Vector3());
    forward.y = 0;
    if (forward.lengthSq() < 1e-6) forward.set(0, 0, -1);
    forward.normalize();
    const right = forward.clone().cross(up).normalize();
    const movement = new THREE.Vector3();
    if (walkKeys.has("KeyW") || walkKeys.has("ArrowUp")) movement.add(forward);
    if (walkKeys.has("KeyS") || walkKeys.has("ArrowDown")) movement.sub(forward);
    if (walkKeys.has("KeyD") || walkKeys.has("ArrowRight")) movement.add(right);
    if (walkKeys.has("KeyA") || walkKeys.has("ArrowLeft")) movement.sub(right);

    let moving = false;
    if (walking && movement.lengthSq() > 0) {
      movement.normalize();
      const speed = (walkKeys.has("ShiftLeft") || walkKeys.has("ShiftRight")) ? 5.2 : 3.1;
      const step = movement.multiplyScalar(speed * deltaSeconds);
      const surfaces = installedSupportSurfaces();
      const boxes = walkCollisionBoxes();
      const current = player.position.clone();
      const canMoveTo = (x, z) => !isWalkBlocked(x, z, boxes, surfaces, current.y);
      let nextX = current.x, nextZ = current.z;
      if (canMoveTo(current.x + step.x, current.z)) nextX += step.x;
      if (canMoveTo(nextX, current.z + step.z)) nextZ += step.z;
      const dx = nextX - current.x, dz = nextZ - current.z;
      if (Math.abs(dx) > 1e-6 || Math.abs(dz) > 1e-6) {
        moving = true;
        player.position.x = nextX;
        player.position.z = nextZ;
        const direction = new THREE.Vector3(dx, 0, dz).normalize();
        player.rotation.y = Math.atan2(direction.x, direction.z);
        const follow = new THREE.Vector3(dx, 0, dz);
        controls.target.add(follow);
        camera.position.add(follow);
        walkAnimationTime += deltaSeconds * (speed === 5.2 ? 12 : 9);
      }
    }
    const swing = moving ? Math.sin(walkAnimationTime) * .48 : 0;
    legPivots.forEach((pivot, index) => { pivot.rotation.x = index === 0 ? swing : -swing; });
    armPivots.forEach((pivot, index) => { pivot.rotation.x = index === 0 ? -swing * .72 : swing * .72; });
  }
  const sceneRoot = (object) => { let node = object; while (node && node.parent !== scene) node = node.parent; return node; };
  const walkRadius = .28;
  function footprintBounds(group) {
    group.updateMatrixWorld(true);
    const bounds = new THREE.Box3();
    group.traverse((node) => {
      if (!node.isMesh || !node.geometry) return;
      // Attached sensor markers are annotations, not part of the equipment's
      // physical footprint. Ignore hidden model parts for the same reason.
      for (let parent = node; parent && parent !== group; parent = parent.parent) {
        if (!parent.visible || parent.userData?.sceneKind === "SENSOR") return;
      }
      if (!node.geometry.boundingBox) node.geometry.computeBoundingBox();
      if (node.geometry.boundingBox) bounds.union(node.geometry.boundingBox.clone().applyMatrix4(node.matrixWorld));
    });
    return bounds;
  }
  function frameInteriorBounds(item, group) {
    if (item.modelKey !== "SR_WALL_COIL") return footprintBounds(group);
    group.updateMatrixWorld(true);
    const shell = group.getObjectByName("Mesh014");
    return shell ? new THREE.Box3().setFromObject(shell) : footprintBounds(group);
  }
  function circleHitsBox(x, z, box) {
    if (box.isEmpty()) return false;
    const closestX = THREE.MathUtils.clamp(x, box.min.x, box.max.x);
    const closestZ = THREE.MathUtils.clamp(z, box.min.z, box.max.z);
    return (x - closestX) ** 2 + (z - closestZ) ** 2 < walkRadius ** 2;
  }
  function meshBounds(group) {
    group.updateMatrixWorld(true);
    const bounds = new THREE.Box3();
    group.traverse((node) => {
      if (!node.isMesh || !node.geometry) return;
      const part = new THREE.Box3().setFromObject(node);
      const baseY = group.position.y;
      if (part.max.y > baseY + .05 && part.min.y < baseY + 1.72) bounds.union(part);
    });
    return bounds;
  }
  function walkCollisionBoxes() {
    const boxes = [];
    for (const item of items) {
      if (!["RACK", "EQUIPMENT"].includes(item.itemType) || ["FLOOR", "CEILING", "CEILING_CLEAR"].includes(item.modelKey)) continue;
      const group = itemGroups.get(findItemKey(item));
      if (!group) continue;
      const bounds = meshBounds(group);
      if (!bounds.isEmpty()) boxes.push(bounds);
    }
    return boxes;
  }
  function installedFloorBoxes(excludeItem = null) {
    return items.filter((item) => item.modelKey === "FLOOR" && item !== excludeItem)
      .map((item) => itemGroups.get(findItemKey(item)))
      .filter(Boolean).map(footprintBounds);
  }
  function floorBoundsRect(box) {
    return { minX: box.min.x, maxX: box.max.x, minZ: box.min.z, maxZ: box.max.z };
  }
  function installedSupportSurfaces() {
    return items.filter((item) => ["FLOOR", "CEILING", "CEILING_CLEAR"].includes(item.modelKey))
      .map((item) => ({ item, group: itemGroups.get(findItemKey(item)) }))
      .filter(({ group }) => Boolean(group)).map(({ item, group }) => {
        const box = footprintBounds(group);
        return { item, box, topY: isCeilingModel(item.modelKey) ? group.position.y + .12 : box.max.y };
      });
  }
  function ceilingWallSupport(x, z, sizeX = 4, sizeZ = 4, requiredY = null) {
    const halfX = (Number(sizeX) || 4) / 2, halfZ = (Number(sizeZ) || 4) / 2;
    const tile = { minX: x - halfX, maxX: x + halfX, minZ: z - halfZ, maxZ: z + halfZ };
    const levels = new Map();
    for (const wall of items.filter((item) => isWallModel(item.modelKey) && item.itemType === "EQUIPMENT"
      && item.parentId == null && !item.parentClientKey)) {
      const group = itemGroups.get(findItemKey(wall));
      if (!group) continue;
      const bounds = footprintBounds(group);
      const overlapX = Math.min(tile.maxX, bounds.max.x) - Math.max(tile.minX, bounds.min.x);
      const overlapZ = Math.min(tile.maxZ, bounds.max.z) - Math.max(tile.minZ, bounds.min.z);
      const alongZ = Math.abs(Math.round(Number(wall.rotationY || 0) / 90)) % 2 === 1;
      const lengthOverlap = alongZ ? overlapZ : overlapX;
      const thicknessOverlap = alongZ ? overlapX : overlapZ;
      if (lengthOverlap < .3 || thicknessOverlap < .025) continue;
      const wallTop = bounds.max.y;
      if (requiredY != null && Math.abs(wallTop - requiredY) > .16) continue;
      const key = Math.round(wallTop * 20) / 20;
      if (!levels.has(key)) levels.set(key, { wallTop, walls: new Set() });
      levels.get(key).walls.add(wall);
    }
    return [...levels.values()].filter((level) => level.walls.size >= 2)
      .sort((a, b) => b.wallTop - a.wallTop)[0] || null;
  }
  function snapCeilingPosition(x, z, sizeX = 4, sizeZ = 4) {
    const installedCeilings = items.filter((item) => isCeilingModel(item.modelKey));
    if (installedCeilings.length) {
      const adjacentSlots = [];
      for (const ceiling of installedCeilings) {
        const cx = Number(ceiling.x), cz = Number(ceiling.z);
        const offsetX = ((Number(ceiling.sizeX) || 4) + (Number(sizeX) || 4)) / 2;
        const offsetZ = ((Number(ceiling.sizeZ) || 4) + (Number(sizeZ) || 4)) / 2;
        for (const candidate of [{ x: cx - offsetX, z: cz }, { x: cx + offsetX, z: cz }, { x: cx, z: cz - offsetZ }, { x: cx, z: cz + offsetZ }]) {
          if (Math.hypot(x - candidate.x, z - candidate.z) > 2.5 || !ceilingWallSupport(candidate.x, candidate.z, sizeX, sizeZ)) continue;
          const halfX = sizeX / 2, halfZ = sizeZ / 2;
          const rect = { minX: candidate.x - halfX, maxX: candidate.x + halfX, minZ: candidate.z - halfZ, maxZ: candidate.z + halfZ };
          const overlaps = installedCeilings.some((other) => {
            const group = itemGroups.get(findItemKey(other)); if (!group) return false;
            const bounds = floorBoundsRect(footprintBounds(group));
            return Math.min(rect.maxX, bounds.maxX) - Math.max(rect.minX, bounds.minX) > .15
              && Math.min(rect.maxZ, bounds.maxZ) - Math.max(rect.minZ, bounds.minZ) > .15;
          });
          if (!overlaps) adjacentSlots.push(candidate);
        }
      }
      if (adjacentSlots.length) return adjacentSlots.reduce((best, candidate) => Math.hypot(x - candidate.x, z - candidate.z)
        < Math.hypot(x - best.x, z - best.z) ? candidate : best);
    }
    const walls = items.filter((item) => isWallModel(item.modelKey) && item.itemType === "EQUIPMENT"
      && item.parentId == null && !item.parentClientKey)
      .map((item) => ({ item, bounds: itemGroups.get(findItemKey(item)) && footprintBounds(itemGroups.get(findItemKey(item))) }))
      .filter(({ bounds }) => bounds && !bounds.isEmpty());
    const candidates = [];
    for (const floor of items.filter((item) => item.modelKey === "FLOOR")) {
      const candidate = { x: Number(floor.x), z: Number(floor.z) };
      if (ceilingWallSupport(candidate.x, candidate.z, sizeX, sizeZ)) candidates.push(candidate);
    }
    for (const horizontal of walls.filter(({ item }) => Math.abs(Math.round(Number(item.rotationY || 0) / 90)) % 2 === 0)) {
      for (const vertical of walls.filter(({ item }) => Math.abs(Math.round(Number(item.rotationY || 0) / 90)) % 2 === 1)) {
        const cornerX = vertical.bounds.min.x + (vertical.bounds.max.x - vertical.bounds.min.x) / 2;
        const cornerZ = horizontal.bounds.min.z + (horizontal.bounds.max.z - horizontal.bounds.min.z) / 2;
        if (cornerX < horizontal.bounds.min.x - .2 || cornerX > horizontal.bounds.max.x + .2
          || cornerZ < vertical.bounds.min.z - .2 || cornerZ > vertical.bounds.max.z + .2) continue;
        for (const dx of [-sizeX / 2, sizeX / 2]) for (const dz of [-sizeZ / 2, sizeZ / 2]) {
          const candidate = { x: cornerX + dx, z: cornerZ + dz };
          if (ceilingWallSupport(candidate.x, candidate.z, sizeX, sizeZ)) candidates.push(candidate);
        }
      }
    }
    const grid = { x: Math.round(x / sizeX) * sizeX, z: Math.round(z / sizeZ) * sizeZ };
    if (ceilingWallSupport(grid.x, grid.z, sizeX, sizeZ)) candidates.push(grid);
    const available = candidates.filter((candidate, index) => {
      if (candidates.findIndex((other) => Math.abs(other.x - candidate.x) < .025 && Math.abs(other.z - candidate.z) < .025) !== index) return false;
      const box = { minX: candidate.x - sizeX / 2, maxX: candidate.x + sizeX / 2, minZ: candidate.z - sizeZ / 2, maxZ: candidate.z + sizeZ / 2 };
      return items.filter((item) => isCeilingModel(item.modelKey)).every((item) => {
        const group = itemGroups.get(findItemKey(item)); if (!group) return true;
        const other = floorBoundsRect(footprintBounds(group));
        return Math.min(box.maxX, other.maxX) - Math.max(box.minX, other.minX) <= .15
          || Math.min(box.maxZ, other.maxZ) - Math.max(box.minZ, other.minZ) <= .15;
      });
    });
    if (!available.length) return grid;
    return available.reduce((best, candidate) => Math.hypot(x - candidate.x, z - candidate.z)
      < Math.hypot(x - best.x, z - best.z) ? candidate : best);
  }
  function isFootprintOnInstalledFloor(box, floors = installedSupportSurfaces(), baseY = box?.min.y ?? 0) {
    if (box.isEmpty() || !floors.length) return false;
    for (let x = box.min.x; x <= box.max.x + .001; x += .2) {
      for (let z = box.min.z; z <= box.max.z + .001; z += .2) {
        if (!floors.some(({ box: floor, topY }) => Math.abs(topY - baseY) < .16
          && x >= floor.min.x - .025 && x <= floor.max.x + .025
          && z >= floor.min.z - .025 && z <= floor.max.z + .025)) return false;
      }
    }
    return true;
  }
  function wallTopSupport(item, x, z, requiredY = null) {
    if (!isWallModel(item.modelKey)) return null;
    const alongZ = Math.abs(Math.round(Number(item.rotationY || 0) / 90)) % 2 === 1;
    const halfLength = (Number(item.sizeX) || 4) / 2;
    const levels = new Map();
    for (const wall of items) {
      if (!isWallModel(wall.modelKey) || findItemKey(wall) === findItemKey(item) || wall.parentId != null || wall.parentClientKey) continue;
      if ((Math.abs(Math.round(Number(wall.rotationY || 0) / 90)) % 2 === 1) !== alongZ) continue;
      const group = itemGroups.get(findItemKey(wall)); if (!group) continue;
      const box = footprintBounds(group), topY = Number(wall.y || 0) + 2.8 * (Number(wall.sizeZ) || 4) / 4;
      if (requiredY != null && Math.abs(topY - requiredY) > .12) continue;
      const cross = alongZ ? x : z, minCross = alongZ ? box.min.x : box.min.z, maxCross = alongZ ? box.max.x : box.max.z;
      if (cross < minCross - .12 || cross > maxCross + .12) continue;
      const key = Math.round(topY * 20) / 20;
      if (!levels.has(key)) levels.set(key, { topY, intervals: [] });
      levels.get(key).intervals.push(alongZ ? [box.min.z, box.max.z] : [box.min.x, box.max.x]);
    }
    const center = alongZ ? z : x;
    for (const level of [...levels.values()].sort((a, b) => b.topY - a.topY)) {
      let covered = true;
      for (let pos = center - halfLength; pos <= center + halfLength + .001; pos += .1) {
        if (!level.intervals.some(([min, max]) => pos >= min - .08 && pos <= max + .08)) { covered = false; break; }
      }
      if (covered) return level;
    }
    return null;
  }
  function includeSceneDescendants(removed) {
    let changed = true;
    while (changed) {
      changed = false;
      for (const item of items) {
        if (removed.has(item)) continue;
        const parent = items.find((candidate) => item.parentId != null
          ? candidate.id != null && Number(candidate.id) === Number(item.parentId)
          : candidate.clientKey === item.parentClientKey);
        if (parent && removed.has(parent)) { removed.add(item); changed = true; }
      }
    }
    return removed;
  }
  function detachSceneItems(removed) {
    includeSceneDescendants(removed);
    for (const item of removed) {
      const key = findItemKey(item), group = itemGroups.get(key);
      group?.parent?.remove(group); itemGroups.delete(key);
      if (selectedItem === item) selectedItem = null;
    }
    items = items.filter((item) => !removed.has(item));
  }
  function pruneUnsupportedStructures() {
    let removedCount = 0, changed = true;
    while (changed) {
      changed = false;
      const unsupported = new Set();
      for (const ceiling of items.filter((item) => isCeilingModel(item.modelKey))) {
        if (!ceilingWallSupport(Number(ceiling.x || 0), Number(ceiling.z || 0), ceiling.sizeX, ceiling.sizeZ, Number(ceiling.y || 0))) unsupported.add(ceiling);
      }
      const surfaces = installedSupportSurfaces();
      for (const item of items) {
        if (item.parentId != null || item.parentClientKey || ["FLOOR", "CEILING", "CEILING_CLEAR"].includes(item.modelKey)) continue;
        const group = itemGroups.get(findItemKey(item));
        if (!group || (!isFootprintOnInstalledFloor(footprintBounds(group), surfaces, Number(item.y || 0))
          && !(isWallModel(item.modelKey) && wallTopSupport(item, Number(item.x), Number(item.z), Number(item.y || 0))))) unsupported.add(item);
      }
      if (unsupported.size) {
        removedCount += unsupported.size;
        detachSceneItems(unsupported);
        changed = true;
      }
    }
    return removedCount;
  }
  function placementLevelAt(item, x, z) {
    if (item.modelKey === "FLOOR") return 0;
    if (isCeilingModel(item.modelKey)) return ceilingWallSupport(x, z, item.sizeX, item.sizeZ)?.wallTop ?? 0;
    if (isWallModel(item.modelKey)) {
      if (item._wallFloor === 1) return 0;
      const support = wallTopSupport(item, x, z);
      if (support && item._wallFloor !== 1) return support.topY;
      if (item._wallFloor === 2) return 0;
    }
    const halfX = halfSize(item, "x"), halfZ = halfSize(item, "z");
    const surfaces = installedSupportSurfaces();
    const levels = [...new Set(surfaces.map(({ topY }) => Math.round(topY * 20) / 20))]
      .sort((a, b) => b - a);
    for (const level of levels) {
      let supported = true;
      for (let px = x - halfX; px <= x + halfX + .001 && supported; px += .2) {
        for (let pz = z - halfZ; pz <= z + halfZ + .001; pz += .2) {
          if (!surfaces.some(({ box, topY }) => Math.abs(topY - level) < .04
            && px >= box.min.x - .025 && px <= box.max.x + .025
            && pz >= box.min.z - .025 && pz <= box.max.z + .025)) { supported = false; break; }
        }
      }
      if (supported) return level;
    }
    return 0;
  }
  function isOnInstalledFloor(x, z, floors) {
    return floors.some((box) => x >= box.min.x && x <= box.max.x && z >= box.min.z && z <= box.max.z);
  }
  function isWalkBlocked(x, z, boxes, surfaces, level = 0) {
    // Treat adjacent floor tiles as one walkable surface. The avatar's center
    // may reach the tile edge; requiring its full collision radius inside each
    // individual tile creates invisible seams between otherwise connected tiles.
    const edgeTolerance = .08;
    const supported = surfaces.some(({ box, topY }) => Math.abs(topY - level) < .16
      && x >= box.min.x - edgeTolerance && x <= box.max.x + edgeTolerance
      && z >= box.min.z - edgeTolerance && z <= box.max.z + edgeTolerance);
    if (!supported) return true;
    for (const box of boxes) {
      if (box.max.y > level + .05 && box.min.y < level + 1.65 && circleHitsBox(x, z, box)) return true;
    }
    return false;
  }
  const guideMaterial = new THREE.LineBasicMaterial({ color: 0x22e6ff, transparent: true, opacity: .95, depthTest: false });
  const makeGuide = () => {
    const geometry = new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(), new THREE.Vector3()]);
    const line = new THREE.Line(geometry, guideMaterial); line.visible = false; line.renderOrder = 20; scene.add(line); return line;
  };
  const guideX = makeGuide(), guideZ = makeGuide();
  const clearGuides = () => { guideX.visible = false; guideZ.visible = false; };
  const showGuide = (line, start, end) => {
    line.geometry.setFromPoints([start, end]); line.visible = true;
  };
  const overlapLines = new THREE.LineSegments(new THREE.BufferGeometry(),
    new THREE.LineBasicMaterial({ color: 0xff8a42, depthTest: false }));
  overlapLines.renderOrder = 31; scene.add(overlapLines);
  const overlapNotice = document.createElement("div");
  Object.assign(overlapNotice.style, { position: "absolute", left: "12px", top: "12px", zIndex: "11",
    padding: "8px 10px", borderRadius: "6px", background: "rgba(76,35,15,.94)", color: "#ffe0bd",
    fontSize: "12px", pointerEvents: "none", display: "none" });
  container.appendChild(overlapNotice);
  let overlapSignature = "", lastOverlapUpdate = 0;
  function updateFloorOverlapIndicators() {
    if (!options.editing) return;
    const floors = installedFloorBoxes();
    const points = [];
    let count = 0;
    for (let i = 0; i < floors.length; i++) for (let j = i + 1; j < floors.length; j++) {
      const a = floors[i], b = floors[j];
      if (Math.abs(a.max.y - b.max.y) > .16 || !floorRectsOverlap(floorBoundsRect(a), floorBoundsRect(b))) continue;
      count++;
      const minX = Math.max(a.min.x, b.min.x), maxX = Math.min(a.max.x, b.max.x);
      const minZ = Math.max(a.min.z, b.min.z), maxZ = Math.min(a.max.z, b.max.z);
      const y = Math.max(a.max.y, b.max.y) + .04;
      const corners = [[minX, y, minZ], [maxX, y, minZ], [maxX, y, maxZ], [minX, y, maxZ]];
      for (let k = 0; k < 4; k++) points.push(new THREE.Vector3(...corners[k]), new THREE.Vector3(...corners[(k + 1) % 4]));
    }
    const signature = points.map((point) => `${point.x.toFixed(3)},${point.y.toFixed(3)},${point.z.toFixed(3)}`).join(";");
    if (signature !== overlapSignature) {
      const previous = overlapLines.geometry;
      overlapLines.geometry = new THREE.BufferGeometry().setFromPoints(points);
      previous.dispose(); overlapSignature = signature;
    }
    overlapNotice.style.display = count ? "block" : "none";
    if (count) overlapNotice.textContent = `겹친 바닥 ${count}곳 · 주황색 영역을 확인해 타일을 이동하거나 삭제하세요.`;
  }
  function placementInvalidReason(item, group, ignoreKey = null) {
    const bounds = footprintBounds(group);
    const surfaces = installedSupportSurfaces();
    if (isWallModel(item.modelKey) && item._wallFloor === 2
      && !wallTopSupport(item, Number(item.x), Number(item.z), group.position.y)) {
      return "2층 벽은 같은 방향의 아래 벽 위에 설치해 주세요.";
    }
    if (isCeilingModel(item.modelKey) && !ceilingWallSupport(Number(item.x || 0), Number(item.z || 0), item.sizeX, item.sizeZ)) {
      return "천장 타일은 아래에 벽이 2개 이상 있는 위치에만 설치할 수 있습니다.";
    }
    if (item.modelKey !== "FLOOR" && !isCeilingModel(item.modelKey)
      && !isFootprintOnInstalledFloor(bounds, surfaces, group.position.y)
      && !(isWallModel(item.modelKey) && wallTopSupport(item, Number(item.x), Number(item.z), group.position.y))) {
      return "설비는 바닥 위에, 벽은 바닥이나 같은 방향의 벽 위에 놓아주세요.";
    }
    const candidateIsSurface = ["FLOOR", "CEILING", "CEILING_CLEAR"].includes(item.modelKey);
    for (const installed of items) {
      if (ignoreKey && findItemKey(installed) === ignoreKey) continue;
      if (installed.parentId != null || installed.parentClientKey) continue;
      const installedIsSurface = ["FLOOR", "CEILING", "CEILING_CLEAR"].includes(installed.modelKey);
      if (candidateIsSurface || installedIsSurface) {
        if (item.modelKey !== installed.modelKey && !(isCeilingModel(item.modelKey) && isCeilingModel(installed.modelKey))) continue;
      } else if (Math.abs(Number(item.y || 0) - Number(installed.y || 0)) > .16) continue;
      if (isCeilingModel(item.modelKey) && isWallModel(installed.modelKey)) continue;
      const installedGroup = itemGroups.get(findItemKey(installed));
      if (!installedGroup) continue;
      const other = footprintBounds(installedGroup);
      const frameContent = isEquipmentFrameKey(item.modelKey) ? installed : item;
      const movingInterior = isEquipmentFrameKey(item.modelKey) ? frameInteriorBounds(item, group) : bounds;
      const installedInterior = isEquipmentFrameKey(installed.modelKey) ? frameInteriorBounds(installed, installedGroup) : other;
      if (allowsInrowInteriorOverlap(item.modelKey, installed.modelKey, movingInterior, installedInterior,
        isFrameEquipmentItem(frameContent))) continue;
      if (item.modelKey === "FLOOR" && Math.abs(bounds.max.y - other.max.y) > .16) continue;
      const overlapX = Math.min(bounds.max.x, other.max.x) - Math.max(bounds.min.x, other.min.x);
      const overlapZ = Math.min(bounds.max.z, other.max.z) - Math.max(bounds.min.z, other.min.z);
      const overlaps = item.modelKey === "FLOOR"
        ? floorRectsOverlap(floorBoundsRect(bounds), floorBoundsRect(other))
        : isCeilingModel(item.modelKey) && isCeilingModel(installed.modelKey)
          ? overlapX > .15 && overlapZ > .15
          : overlapX > .025 && overlapZ > .025;
      // Allow only a real end-to-end or perpendicular joint; negative overlap
      // means there is still a visible gap and should not count as connected.
      const movingAlongZ = Math.abs(Math.round(Number(item.rotationY || 0) / 90)) % 2 === 1;
      const installedAlongZ = Math.abs(Math.round(Number(installed.rotationY || 0) / 90)) % 2 === 1;
      const joinedWallCorner = isWallModel(item.modelKey) && isWallModel(installed.modelKey)
        && (movingAlongZ === installedAlongZ
          ? (movingAlongZ ? overlapZ : overlapX) >= -.025
            && (movingAlongZ ? overlapZ : overlapX) <= .24
            && (movingAlongZ ? overlapX : overlapZ) > .025
          : overlapX > .025 && overlapX <= .24 && overlapZ > .025 && overlapZ <= .24);
      if (joinedWallCorner) continue;
      if (overlaps) return candidateIsSurface
        ? "같은 층의 바닥·천장 타일과 겹칩니다. 빈 곳에 이어서 설치하세요."
        : `이미 설치된 「${installed.label || installed.modelKey}」과 겹칩니다. 빈 곳으로 옮기세요.`;
    }
    if (walking && Math.abs(group.position.y - player.position.y) < 1.6
      && circleHitsBox(player.position.x, player.position.z, meshBounds(group))) {
      return "아바타와 겹치는 위치에는 설치할 수 없습니다. 다른 곳을 선택하세요.";
    }
    return "";
  }
  function resizeFocusedItem(direction, axis = "x") {
    const item = placement?.item || selectedItem;
    const group = placement?.group || (item && itemGroups.get(findItemKey(item)));
    if (!item || !group) return;
    if (item.locked) { options.onHint?.("잠긴 설비는 먼저 잠금을 해제하세요."); return; }
    const isFrame = isEquipmentFrameKey(item.modelKey);
    const isEquipment = isFrame || isFrameEquipmentItem(item);
    const isFrameLength = isFrame && axis === "z";
    const isFrameHeight = isFrame && axis === "y";
    const oldFrameSizeX = Number(item.sizeX) || 12, oldFrameSizeY = Number(item.sizeY) || oldFrameSizeX;
    const oldFrameSizeZ = Number(item.sizeZ) || oldFrameSizeX;
    const field = axis === "z" ? "sizeZ" : axis === "y" ? "sizeY" : "sizeX";
    const previous = Number(item[field]) || 4;
    const previousX = Number(item.x) || 0, previousZ = Number(item.z) || 0;
    const previousFrameBounds = isFrameLength ? footprintBounds(group) : null;
    const step = isWallModel(item.modelKey) || isCeilingModel(item.modelKey) ? 4 : 1;
    const maxSize = isWallModel(item.modelKey) || isCeilingModel(item.modelKey) ? 24 : showroomModelByKey(item.modelKey) ? 32 : 16;
    const resizedFrame = isEquipment ? resizedInrowFrame(item, direction, axis === "width" ? "width" : axis === "z" ? "length" : axis === "y" ? "height" : "uniform") : null;
    const next = isEquipment ? resizedFrame[field] : THREE.MathUtils.clamp(previous + direction * step, 2, maxSize);
    if (isEquipment && resizedFrame.sizeX > maxSize) { options.onHint?.("크기 조절 한도에 도달했습니다."); return; }
    if (next === previous) { options.onHint?.("크기 조절 한도에 도달했습니다."); return; }
    item[field] = next;
    if (isEquipment) { item.sizeX = resizedFrame.sizeX; item.sizeY = resizedFrame.sizeY; item.sizeZ = resizedFrame.sizeZ; }
    const shift = (next - previous) / 2;
    if (isFrameLength) {
      const angle = THREE.MathUtils.degToRad(Number(item.rotationY) || 0);
      const alongX = Math.abs(Math.round(Number(item.rotationY || 0) / 90)) % 2 === 1;
      const oldLength = previousFrameBounds.max[alongX ? "x" : "z"] - previousFrameBounds.min[alongX ? "x" : "z"];
      const physicalShift = shift * oldLength / previous;
      item.x += Math.sin(angle) * physicalShift; item.z += Math.cos(angle) * physicalShift;
    } else if (isWallModel(item.modelKey) && axis === "x") {
      const angle = THREE.MathUtils.degToRad(Number(item.rotationY) || 0);
      item.x += Math.cos(angle) * shift; item.z -= Math.sin(angle) * shift;
    } else if (isCeilingModel(item.modelKey)) {
      if (axis === "x") item.x += shift;
      else item.z += shift;
    }
    group.position.set(Number(item.x) || 0, Number(item.y) || 0, Number(item.z) || 0);
    if (isWallModel(item.modelKey)) {
      const segment = group.userData.wallSegment;
      if (segment) rebuildWallSurface(segment, placement ? [] : openingsForWall(item), item.modelKey, next, {}, 2.8 * (Number(item.sizeZ) || 4) / 4);
    }
    else if (isCeilingModel(item.modelKey)) rebuildCeilingSurface(group, items, item);
    else if (isEquipment) { const scale = inrowFrameScale(item); group.scale.set(scale.x, scale.y, scale.z); }
    else group.scale.setScalar(next / 4);
    group.updateMatrixWorld(true);
    const reason = placementInvalidReason(item, group, placement ? null : findItemKey(item));
    if (reason) {
      item[field] = previous;
      if (isEquipment) { item.sizeX = oldFrameSizeX; item.sizeY = oldFrameSizeY; item.sizeZ = oldFrameSizeZ; }
      item.x = previousX; item.z = previousZ;
      group.position.set(previousX, Number(item.y) || 0, previousZ);
      if (isWallModel(item.modelKey)) {
        const segment = group.userData.wallSegment;
        if (segment) rebuildWallSurface(segment, placement ? [] : openingsForWall(item), item.modelKey, previous, {}, 2.8 * (Number(item.sizeZ) || 4) / 4);
      }
      else if (isCeilingModel(item.modelKey)) rebuildCeilingSurface(group, items, item);
      else if (isEquipment) { const scale = inrowFrameScale(item); group.scale.set(scale.x, scale.y, scale.z); }
      else group.scale.setScalar(previous / 4);
      group.updateMatrixWorld(true);
      options.onHint?.(reason); return;
    }
    if (placement) {
      placement.preferredSizeX = item.sizeX;
      placement.preferredSizeY = item.sizeY;
      placement.preferredSizeZ = item.sizeZ;
      updatePlacementPreview();
    } else updateSelectionBox();
    if (!placement && isWallModel(item.modelKey)) refreshAllWallSurfaces();
    options.onChange?.(items); options.onHint?.(isEquipment && axis === "width"
      ? `${item.label} 너비 ${Math.round(next / 4 * 100)}% · 저장 시 반영됩니다.`
      : isEquipment && axis === "y"
      ? `${item.label} 높이 ${Math.round(next / 4 * 100)}% · 저장 시 반영됩니다.`
      : isEquipment && axis === "z"
      ? `${item.label} 세로 길이 ${Math.round(next / 4 * 100)}% · 저장 시 반영됩니다.`
      : isFrame
      ? `${item.label} 전체 크기 ${Math.round(next / 4 * 100)}% · 저장 시 반영됩니다.`
      : showroomModelByKey(item.modelKey)
      ? `${item.label} 크기 ${Math.round(next / 4 * 100)}% · 저장 시 반영됩니다.`
      : `${item.label} ${axis === "x" ? "가로" : "세로"} ${next}m · 저장 시 반영됩니다.`);
  }
  function updatePlacementPreview() {
    if (!placement) return "";
    const reason = placementInvalidReason(placement.item, placement.group);
    placement.invalidReason = reason;
    for (const material of placement.previewMaterials) {
      material.color.copy(material.userData.previewBaseColor).lerp(placement.invalidColor, reason ? .78 : 0);
    }
    return reason;
  }
  const halfSize = (item, axis) => {
    let x, z;
    if (item.itemType === "RACK") x = z = .45;
    else if (isWallModel(item.modelKey)) {
      const quarterTurn = Math.abs(Math.round(Number(item.rotationY || 0) / 90)) % 2 === 1;
      const thickness = ["WALL_GLASS", "WALL_GLASS_CLEAR"].includes(item.modelKey) ? .11
        : ["WALL_PANEL", "WALL_CONCRETE"].includes(item.modelKey) ? .1 : .09;
      const halfLength = (Number(item.sizeX) || 4) / 2;
      x = quarterTurn ? thickness : halfLength; z = quarterTurn ? halfLength : thickness;
      return axis === "x" ? x : z;
    }
    else if (isDoorModel(item.modelKey)) { x = item.modelKey === "DOOR_DOUBLE" ? .8 : .7; z = .08; }
    else if (isWindowModel(item.modelKey)) { x = item.modelKey === "WINDOW_DOUBLE" ? 1.45 : 1.275; z = .08; }
    else if (item.modelKey === "FLOOR") { x = (Number(item.sizeX) || 4) / 2; z = (Number(item.sizeZ) || 4) / 2; }
    else if (isCeilingModel(item.modelKey)) { x = (Number(item.sizeX) || 4) / 2; z = (Number(item.sizeZ) || 4) / 2; }
    else if (item.modelKey === "PIPE") { x = 2; z = .09; }
    else if (item.modelKey === "LIGHT") { x = .55; z = .17; }
    else if (item.modelKey === "STAIR") { x = .815; z = 1.52; }
    else if (showroomModelByKey(item.modelKey)) { const model = showroomModelByKey(item.modelKey); const display = showroomDisplaySize(model) || model; x = display.width / 2; z = display.depth / 2; }
    else { x = item.modelKey === "CHILLER" ? .75 : .525; z = .425; }
    if (["RACK", "EQUIPMENT"].includes(item.itemType) && !isDoorModel(item.modelKey) && !isWindowModel(item.modelKey)
      && !["FLOOR", "CEILING", "CEILING_CLEAR", "LIGHT", "PIPE", "STAIR"].includes(item.modelKey)) {
      x *= (Number(item.sizeX) || 4) / 4;
      z *= (Number(item.sizeZ) || Number(item.sizeX) || 4) / 4;
    }
    if (isDoorModel(item.modelKey) || isWindowModel(item.modelKey) || ["LIGHT", "PIPE", "STAIR"].includes(item.modelKey)) { const factor = (Number(item.sizeZ) || 4) / 4; x *= factor; z *= factor; }
    if (Math.abs(Math.round(Number(item.rotationY || 0) / 90)) % 2 === 1) [x, z] = [z, x];
    return axis === "x" ? x : z;
  };
  const snapPosition = (x, z, moving) => {
    if (isCeilingModel(moving.modelKey)) {
      clearGuides();
      return snapCeilingPosition(x, z, moving.sizeX, moving.sizeZ);
    }
    if (isWallModel(moving.modelKey) && moving._wallFloor !== 1) {
      const sameDirection = (wall) => Math.abs(Math.round(Number(wall.rotationY || 0) / 90)) % 2
        === Math.abs(Math.round(Number(moving.rotationY || 0) / 90)) % 2;
      const target = items.filter((wall) => isWallModel(wall.modelKey) && wall !== moving && sameDirection(wall))
        .map((wall) => ({ x: Number(wall.x), z: Number(wall.z), distance: Math.hypot(x - Number(wall.x), z - Number(wall.z)) }))
        .filter((candidate) => candidate.distance < .75 && wallTopSupport(moving, candidate.x, candidate.z))
        .sort((a, b) => a.distance - b.distance)[0];
      if (target) { clearGuides(); return { x: target.x, z: target.z }; }
    }
    if (moving.modelKey === "FLOOR") {
      clearGuides();
      const floors = installedFloorBoxes(moving)
        .filter((box) => Math.abs(box.max.y - Number(moving.y || 0)) < .16)
        .map(floorBoundsRect);
      const width = halfSize(moving, "x") * 2, depth = halfSize(moving, "z") * 2;
      return snapFloorPosition(x, z, width, depth, floors, vacantFloorSlots);
    }
    if (!snapEnabled) { clearGuides(); return { x, z }; }
    const step = .5;
    let nextX = Math.round(x / step) * step, nextZ = Math.round(z / step) * step;
    let alignedX = false, alignedZ = false;
    const peers = items.filter((item) => item !== moving && !["CEILING", "CEILING_CLEAR", "FLOOR"].includes(item.modelKey)
      && Math.abs(Number(item.y || 0) - Number(moving.y || 0)) < .16 && item.parentId == null && !item.parentClientKey
      && (item.itemType === "RACK" || item.itemType === "EQUIPMENT"));
    let bestX = .24, bestZ = .24;
    const snapAnchors = (item, axis) => {
      if (item.itemType === "RACK" || showroomModelByKey(item.modelKey)) {
        const group = placement?.item === item ? placement.group : itemGroups.get(findItemKey(item));
        if (group) {
          const box = footprintBounds(group);
          if (!box.isEmpty()) return [0, box.min[axis] - group.position[axis], box.max[axis] - group.position[axis]];
        }
      }
      return [0, -halfSize(item, axis), halfSize(item, axis)];
    };
    for (const peer of peers) {
      const peerX = Number(peer.x), peerZ = Number(peer.z);
      const movingXAnchors = snapAnchors(moving, "x");
      const peerXAnchors = snapAnchors(peer, "x");
      const movingZAnchors = snapAnchors(moving, "z");
      const peerZAnchors = snapAnchors(peer, "z");
      for (const movingAnchor of movingXAnchors) for (const peerAnchor of peerXAnchors) {
        const candidate = peerX + peerAnchor - movingAnchor, delta = Math.abs(nextX - candidate);
        if (delta < bestX) { bestX = delta; nextX = candidate; alignedX = true; }
      }
      for (const movingAnchor of movingZAnchors) for (const peerAnchor of peerZAnchors) {
        const candidate = peerZ + peerAnchor - movingAnchor, delta = Math.abs(nextZ - candidate);
        if (delta < bestZ) { bestZ = delta; nextZ = candidate; alignedZ = true; }
      }
    }
    for (let i = 0; i < peers.length; i++) for (let j = i + 1; j < peers.length; j++) {
      const a = peers[i], b = peers[j];
      if ((Number(a.x) < Number(moving.x) && Number(b.x) > Number(moving.x))
          || (Number(b.x) < Number(moving.x) && Number(a.x) > Number(moving.x))) {
        const left = Number(a.x) < Number(b.x) ? a : b, right = left === a ? b : a;
        const candidate = (Number(left.x) + halfSize(left, "x") + Number(right.x) - halfSize(right, "x")) / 2;
        const delta = Math.abs(nextX - candidate);
        if (delta < bestX) { bestX = delta; nextX = candidate; alignedX = true; }
      }
      if ((Number(a.z) < Number(moving.z) && Number(b.z) > Number(moving.z))
          || (Number(b.z) < Number(moving.z) && Number(a.z) > Number(moving.z))) {
        const back = Number(a.z) < Number(b.z) ? a : b, front = back === a ? b : a;
        const candidate = (Number(back.z) + halfSize(back, "z") + Number(front.z) - halfSize(front, "z")) / 2;
        const delta = Math.abs(nextZ - candidate);
        if (delta < bestZ) { bestZ = delta; nextZ = candidate; alignedZ = true; }
      }
    }
    let wallJointSnapped = false;
    if (isWallModel(moving.modelKey)) {
      let nearestJoint = null;
      for (const peer of peers.filter((item) => isWallModel(item.modelKey))) {
        const movingAlongZ = Math.abs(Math.round(Number(moving.rotationY || 0) / 90)) % 2 === 1;
        const peerAlongZ = Math.abs(Math.round(Number(peer.rotationY || 0) / 90)) % 2 === 1;
        const candidates = [];
        if (movingAlongZ === peerAlongZ) {
          const axis = movingAlongZ ? "z" : "x";
          const gap = halfSize(moving, axis) + halfSize(peer, axis);
          for (const direction of [-1, 1]) candidates.push({
            x: axis === "x" ? Number(peer.x) + direction * gap : Number(peer.x),
            z: axis === "z" ? Number(peer.z) + direction * gap : Number(peer.z),
          });
        } else {
          const peerLengthAxis = peerAlongZ ? "z" : "x";
          const peerThicknessAxis = peerAlongZ ? "x" : "z";
          const movingLengthAxis = movingAlongZ ? "z" : "x";
          const movingThicknessAxis = movingAlongZ ? "x" : "z";
          candidates.push(...wallCornerCandidates(Number(peer.x), Number(peer.z), peerAlongZ,
            halfSize(peer, peerLengthAxis), halfSize(peer, peerThicknessAxis),
            halfSize(moving, movingLengthAxis), halfSize(moving, movingThicknessAxis)));
        }
        for (const candidate of candidates) {
          const distance = (candidate.x - nextX) ** 2 + (candidate.z - nextZ) ** 2;
          if (distance >= 2.25 ** 2 || (nearestJoint && distance >= nearestJoint.distance)) continue;
          const group = placement?.item === moving ? placement.group : itemGroups.get(findItemKey(moving));
          if (!group) continue;
          const oldX = group.position.x, oldY = group.position.y, oldZ = group.position.z;
          const oldItemX = moving.x, oldItemY = moving.y, oldItemZ = moving.z;
          const level = placementLevelAt(moving, candidate.x, candidate.z);
          moving.x = candidate.x; moving.y = level; moving.z = candidate.z;
          group.position.set(candidate.x, level, candidate.z);
          const reason = placementInvalidReason(moving, group, findItemKey(moving));
          moving.x = oldItemX; moving.y = oldItemY; moving.z = oldItemZ;
          group.position.set(oldX, oldY, oldZ); group.updateMatrixWorld(true);
          if (!reason) nearestJoint = { ...candidate, distance };
        }
      }
      if (nearestJoint && nearestJoint.distance < 2.25 ** 2) {
        nextX = nearestJoint.x; nextZ = nearestJoint.z; alignedX = alignedZ = true; wallJointSnapped = true;
      }
    }
    const floorBounds = installedFloorBoxes();
    if (isWallModel(moving.modelKey) && !wallJointSnapped) {
      const quarterTurn = Math.abs(Math.round(Number(moving.rotationY || 0) / 90)) % 2 === 1;
      let closest = null;
      for (const floor of floorBounds) {
        // Snap to all four floor edges, including the ends of the wall run.
        // Previously only the two long-side midpoints were offered, so a wall
        // dragged to the left/right edge could remain one half-tile off-grid.
        const candidates = quarterTurn
          ? [
            ...[floor.min.x + .09, floor.max.x - .09].flatMap((x) => [
              { x, z: floor.min.z + halfSize(moving, "z") },
              { x, z: (floor.min.z + floor.max.z) / 2 },
              { x, z: floor.max.z - halfSize(moving, "z") },
            ]),
          ]
          : [
            ...[floor.min.z + .09, floor.max.z - .09].flatMap((z) => [
              { x: floor.min.x + halfSize(moving, "x"), z },
              { x: (floor.min.x + floor.max.x) / 2, z },
              { x: floor.max.x - halfSize(moving, "x"), z },
            ]),
          ];
        for (const candidate of candidates) {
          const distance = (candidate.x - nextX) ** 2 + (candidate.z - nextZ) ** 2;
          if (!closest || distance < closest.distance) closest = { ...candidate, distance };
        }
      }
      if (closest && closest.distance < .9 ** 2) { nextX = closest.x; nextZ = closest.z; alignedX = alignedZ = true; }

      // The edge magnet above is intentionally local so interior partition
      // walls remain freely placeable. If the preview is still hanging over
      // the outside edge, clamp its full footprint back onto the installed
      // floor union instead of leaving an un-installable red preview.
      const supportSurfaces = installedSupportSurfaces();
      const halfX = halfSize(moving, "x"), halfZ = halfSize(moving, "z");
      const wallFootprintAt = (x, z) => new THREE.Box3(
        new THREE.Vector3(x - halfX, Number(moving.y || 0), z - halfZ),
        new THREE.Vector3(x + halfX, Number(moving.y || 0) + 2.8 * (Number(moving.sizeZ) || 4) / 4, z + halfZ),
      );
      if (!isFootprintOnInstalledFloor(wallFootprintAt(nextX, nextZ), supportSurfaces, Number(moving.y || 0))
          && floorBounds.length) {
        const minX = Math.min(...floorBounds.map((floor) => floor.min.x));
        const maxX = Math.max(...floorBounds.map((floor) => floor.max.x));
        const minZ = Math.min(...floorBounds.map((floor) => floor.min.z));
        const maxZ = Math.max(...floorBounds.map((floor) => floor.max.z));
        const safeX = THREE.MathUtils.clamp(nextX, minX + halfX + .01, maxX - halfX - .01);
        const safeZ = THREE.MathUtils.clamp(nextZ, minZ + halfZ + .01, maxZ - halfZ - .01);
        if (isFootprintOnInstalledFloor(wallFootprintAt(safeX, safeZ), supportSurfaces, Number(moving.y || 0))) {
          nextX = safeX; nextZ = safeZ; alignedX = alignedZ = true;
        }
      }
    }
    const minX = floorBounds.length ? Math.min(...floorBounds.map((box) => box.min.x)) : -9.7;
    const maxX = floorBounds.length ? Math.max(...floorBounds.map((box) => box.max.x)) : 9.7;
    const minZ = floorBounds.length ? Math.min(...floorBounds.map((box) => box.min.z)) : -5.7;
    const maxZ = floorBounds.length ? Math.max(...floorBounds.map((box) => box.max.z)) : 5.7;
    if (alignedX) showGuide(guideX, new THREE.Vector3(nextX, .035, minZ), new THREE.Vector3(nextX, .035, maxZ));
    else guideX.visible = false;
    if (alignedZ) showGuide(guideZ, new THREE.Vector3(minX, .035, nextZ), new THREE.Vector3(maxX, .035, nextZ));
    else guideZ.visible = false;
    return { x: nextX, z: nextZ };
  };
  function positionPlacementAt(x, z) {
    if (!placement) return "";
    const { item, group } = placement;
    const resetSize = () => {
      if (!isEquipmentFrameKey(item.modelKey) && !isFrameEquipmentItem(item)) return;
      if (item.sizeX === placement.preferredSizeX && item.sizeY === placement.preferredSizeY && item.sizeZ === placement.preferredSizeZ) return;
      item.sizeX = placement.preferredSizeX; item.sizeY = placement.preferredSizeY; item.sizeZ = placement.preferredSizeZ;
      if (isEquipmentFrameKey(item.modelKey)) {
        const scale = inrowFrameScale(item);
        group.scale.set(scale.x, scale.y, scale.z);
      } else { const scale = inrowFrameScale(item); group.scale.set(scale.x, scale.y, scale.z); }
    };
    resetSize();
    item.y = placementLevelAt(item, x, z);
    let anchored = null;
    if (!placement.keepCopySize && isFrameEquipmentItem(item)) {
      for (const frame of items.filter((entry) => isEquipmentFrameKey(entry.modelKey))) {
        const frameGroup = itemGroups.get(findItemKey(frame));
        if (!frameGroup) continue;
        const candidate = fitEquipmentInsideFrame({ x, z }, footprintBounds(group), frameInteriorBounds(frame, frameGroup), item.sizeX, placement.preferredSizeX);
        if (!candidate) continue;
        const ratio = candidate.sizeX / item.sizeX;
        if (item.sizeY * ratio < 2 || item.sizeZ * ratio < .5) continue;
        item.sizeX = candidate.sizeX; item.sizeY *= ratio; item.sizeZ *= ratio;
        const scale = inrowFrameScale(item); group.scale.set(scale.x, scale.y, scale.z);
        anchored = candidate;
        break;
      }
    } else if (!placement.keepCopySize && isEquipmentFrameKey(item.modelKey)) {
      for (const equipment of items.filter(isFrameEquipmentItem)) {
        const equipmentGroup = itemGroups.get(findItemKey(equipment));
        if (!equipmentGroup) continue;
        const equipmentBounds = footprintBounds(equipmentGroup);
        if (x < equipmentBounds.min.x || x > equipmentBounds.max.x || z < equipmentBounds.min.z || z > equipmentBounds.max.z) continue;
        const quarterTurn = Math.abs(Math.round(Number(item.rotationY || 0) / 90)) % 2 === 1;
        const candidate = fitFrameAroundEquipment(frameInteriorBounds(item, group), equipmentBounds, item.sizeX, item.sizeZ, .08, quarterTurn, item.sizeY);
        if (!candidate) continue;
        item.sizeX = candidate.sizeX; item.sizeY = candidate.sizeY; item.sizeZ = candidate.sizeZ;
        const scale = inrowFrameScale(item);
        group.scale.set(scale.x, scale.y, scale.z);
        anchored = candidate;
        break;
      }
    }
    if (anchored) clearGuides();
    const snapped = anchored || snapPosition(x, z, item);
    const level = placementLevelAt(placement.item, snapped.x, snapped.z);
    item.x = snapped.x; item.y = level; item.z = snapped.z;
    group.position.set(snapped.x, level, snapped.z);
    return updatePlacementPreview();
  }
  function placementPointerPoint(event, dragHeight = null) {
    const upperWall = isWallModel(placement?.item?.modelKey) && placement.item._wallFloor === 2;
    const inrowTarget = isEquipmentFrameKey(placement?.item?.modelKey)
      ? "equipment" : isFrameEquipmentItem(placement?.item) ? "frame" : null;
    const hits = intersect(event, (upperWall && dragHeight == null) || inrowTarget ? roots() : []);
    if (inrowTarget) {
      const target = hits.find((entry) => {
        const targetItem = nearestSceneItemRoot(entry.object)?.userData?.sceneItem;
        return inrowTarget === "equipment" ? isFrameEquipmentItem(targetItem) : isEquipmentFrameKey(targetItem?.modelKey);
      });
      if (target) {
        const root = nearestSceneItemRoot(target.object);
        return inrowTarget === "equipment"
          ? new THREE.Vector3(root.position.x, 0, root.position.z)
          : new THREE.Vector3(target.point.x, 0, target.point.z);
      }
    }
    let height = dragHeight;
    if (upperWall && height == null) {
      const hit = hits.find((entry) => isWallModel(nearestSceneItemRoot(entry.object)?.userData?.sceneItem?.modelKey));
      const wall = hit && nearestSceneItemRoot(hit.object)?.userData?.sceneItem;
      if (wall) height = Number(wall.y || 0) + 2.8 * (Number(wall.sizeZ) || 4) / 4;
    }
    const plane = upperWall && height != null ? new THREE.Plane(new THREE.Vector3(0, 1, 0), -height) : ground;
    const point = new THREE.Vector3();
    return raycaster.ray.intersectPlane(plane, point) ? point : null;
  }
  function floorBatchCandidates(start, end, width, depth) {
    const positions = floorBatchPositions(start, end, width, depth);
    if (!positions) return null;
    const existing = installedFloorBoxes().map(floorBoundsRect);
    return positions.map((point) => ({ ...point, blocked: existing.some((floor) => floorRectsOverlap(floorRect(point.x, point.z, width, depth), floor)) }));
  }
  function showFloorBatch(candidates, width, depth) {
    clearFloorBatchPreview();
    if (!candidates) { options.onHint?.("한 번에 최대 100칸까지 설치할 수 있습니다. 영역을 줄여 주세요."); return; }
    for (const tile of candidates) {
      const mesh = new THREE.Mesh(new THREE.PlaneGeometry(width - .03, depth - .03),
        new THREE.MeshBasicMaterial({ color: tile.blocked ? 0xff5267 : 0x24d9ed, transparent: true, opacity: .42, depthWrite: false, side: THREE.DoubleSide }));
      mesh.rotation.x = -Math.PI / 2; mesh.position.set(tile.x, .04, tile.z); mesh.renderOrder = 20; floorBatchPreview.add(mesh);
    }
    const count = candidates.filter((tile) => !tile.blocked).length;
    options.onHint?.(`${count}칸 설치 예정 · 빨간 칸은 기존 바닥과 겹쳐 제외됩니다.`);
  }
  function commitFloorBatch(candidates) {
    if (!candidates) { options.onHint?.("한 번에 최대 100칸까지 설치할 수 있습니다."); return; }
    let count = 0;
    for (const tile of candidates) {
      if (tile.blocked) continue;
      const item = { ...placement.item, id: null, clientKey: makeKey(), x: tile.x, y: 0, z: tile.z,
        label: `바닥 ${items.filter((entry) => entry.modelKey === "FLOOR").length + 1}` };
      const group = makeEquipment(item); group.position.set(tile.x, 0, tile.z); applyItemRotation(item, group);
      scene.add(group); items.push(item); itemGroups.set(item.clientKey, group); consumeVacantFloorSlot(tile.x, tile.z); count++;
    }
    if (count) options.onChange?.(items);
    options.onHint?.(`${count}칸 설치했습니다. 배치가 끝나면 저장하세요.`);
  }
  function wallBatchCandidates(start, end, rows = 1) {
    const item = placement.item, group = placement.group;
    const length = Number(item.sizeX) || 4;
    const height = 2.8 * (Number(item.sizeZ) || 4) / 4;
    const positions = wallBatchGrid(start, end, length, Number(item.rotationY) || 0, height, rows);
    if (!positions) return null;
    const original = { x: item.x, y: item.y, z: item.z, position: group.position.clone() };
    const originalItems = items, temporaryKeys = [], result = [];
    items = [...items];
    try {
      for (const point of positions) {
        const y = point.y;
        item.x = point.x; item.y = y; item.z = point.z; group.position.set(point.x, y, point.z);
        const unsupported = point.row > 0 && !wallTopSupport(item, point.x, point.z, y);
        const reason = unsupported ? "아래 벽이 필요합니다." : placementInvalidReason(item, group);
        result.push({ ...point, y, blocked: Boolean(reason) });
        if (!reason) {
          const temporary = { ...item, id: null, clientKey: makeKey() };
          const temporaryGroup = group.clone(); temporaryGroup.position.set(point.x, y, point.z);
          temporaryGroup.updateMatrixWorld(true);
          items.push(temporary); itemGroups.set(temporary.clientKey, temporaryGroup); temporaryKeys.push(temporary.clientKey);
        }
      }
    } finally {
      items = originalItems; temporaryKeys.forEach((key) => itemGroups.delete(key));
      item.x = original.x; item.y = original.y; item.z = original.z; group.position.copy(original.position); group.updateMatrixWorld(true);
    }
    return result;
  }
  function wallDragExtent(start, event, startX, startY) {
    const item = placement.item, length = Number(item.sizeX) || 4;
    const height = 2.8 * (Number(item.sizeZ) || 4) / 4, angle = THREE.MathUtils.degToRad(Number(item.rotationY) || 0);
    const ux = Math.cos(angle), uz = -Math.sin(angle), rect = renderer.domElement.getBoundingClientRect();
    const screen = (point) => { point.project(camera); return { x: point.x * rect.width / 2, y: -point.y * rect.height / 2 }; };
    const origin = screen(new THREE.Vector3(start.x, start.y, start.z));
    const along = screen(new THREE.Vector3(start.x + ux * length, start.y, start.z + uz * length));
    const up = screen(new THREE.Vector3(start.x, start.y + height, start.z));
    const ax = along.x - origin.x, ay = along.y - origin.y, bx = up.x - origin.x, by = up.y - origin.y;
    const dx = event.clientX - startX, dy = event.clientY - startY, det = ax * by - ay * bx;
    if (Math.abs(det) < 1) return { end: start, rows: 1 };
    const horizontal = (dx * by - dy * bx) / det, vertical = (ax * dy - ay * dx) / det;
    return { end: { x: start.x + horizontal * length * ux, z: start.z + horizontal * length * uz }, rows: Math.max(1, Math.round(vertical) + 1) };
  }
  function showWallBatch(candidates) {
    clearFloorBatchPreview();
    if (!candidates) { options.onHint?.("한 번에 최대 30구간까지 설치할 수 있습니다. 영역을 줄여 주세요."); return; }
    const item = placement.item, length = Number(item.sizeX) || 4, height = 2.8 * (Number(item.sizeZ) || 4) / 4;
    for (const wall of candidates) {
      const mesh = new THREE.Mesh(new THREE.BoxGeometry(length - .03, height, .16),
        new THREE.MeshBasicMaterial({ color: wall.blocked ? 0xff5267 : 0x24d9ed, transparent: true, opacity: .38, depthWrite: false, side: THREE.DoubleSide }));
      mesh.position.set(wall.x, wall.y + height / 2, wall.z); mesh.rotation.y = THREE.MathUtils.degToRad(Number(item.rotationY) || 0);
      mesh.renderOrder = 20; floorBatchPreview.add(mesh);
    }
    options.onHint?.(`${candidates.filter((wall) => !wall.blocked).length}구간 설치 예정 · 빨간 구간은 바닥·벽 지지 또는 겹침을 확인하세요.`);
  }
  function commitWallBatch(candidates) {
    if (!candidates) { options.onHint?.("한 번에 최대 30구간까지 설치할 수 있습니다."); return; }
    let count = 0;
    for (const wall of candidates) {
      if (wall.blocked) continue;
      const item = { ...placement.item, id: null, clientKey: makeKey(), x: wall.x, y: wall.y, z: wall.z,
        label: `벽 ${items.filter((entry) => isWallModel(entry.modelKey)).length + 1}` };
      const group = makeEquipment(item); group.position.set(item.x, item.y, item.z); applyItemRotation(item, group); scene.add(group);
      if (placementInvalidReason(item, group)) {
        group.parent?.remove(group); group.traverse((node) => { node.geometry?.dispose();
          for (const material of (Array.isArray(node.material) ? node.material : [node.material])) material?.dispose(); });
        continue;
      }
      delete item._wallFloor;
      items.push(item); itemGroups.set(item.clientKey, group); count++;
    }
    if (count) { refreshAllWallSurfaces(); options.onChange?.(items); }
    options.onHint?.(`${count}구간 설치했습니다. 배치가 끝나면 저장하세요.`);
  }
  function commitPlacement() {
    if (!placement) return false;
    const invalidReason = updatePlacementPreview();
    if (invalidReason) { options.onHint?.(invalidReason); return false; }
    const completed = placement; placement = null; updateControlMode();
    completed.item.x = completed.group.position.x; completed.item.y = completed.group.position.y;
    completed.item.z = completed.group.position.z;
    for (const [material, transparent, opacity, depthWrite] of completed.opacityMemo) {
      material.transparent = transparent; material.opacity = opacity; material.depthWrite = depthWrite; material.needsUpdate = true;
    }
    const wallFloor = completed.item._wallFloor;
    delete completed.item._wallFloor;
    items.push(completed.item); itemGroups.set(completed.item.clientKey, completed.group); selectItem(completed.item.clientKey);
    refreshCeilingSurfaces();
    refreshAllWallSurfaces();
    if (completed.item.modelKey === "FLOOR") consumeVacantFloorSlot(completed.item.x, completed.item.z);
    clearGuides(); options.onChange?.(items);
    if (completed.keepCopySize) { options.onHint?.(`${completed.item.label}을(를) 배치했습니다. 저장을 눌러 반영하세요.`); return true; }
    beginPlacement(completed.item.modelKey, completed.item.rotationY, wallFloor);
    options.onHint?.(`${completed.item.label} 설치됨 · 같은 종류를 계속 배치하거나 다른 자산을 선택하세요. 다 배치한 뒤 한 번에 저장하고, 종료는 Esc를 누르세요.`);
    return true;
  }
  function areaChangeProblem(movedKeys = new Set()) {
    const surfaces = installedSupportSurfaces();
    for (const item of items) {
      if (item.parentId != null || item.parentClientKey) continue;
      const group = itemGroups.get(findItemKey(item)); if (!group) continue;
      if (movedKeys.has(findItemKey(item))) {
        const reason = placementInvalidReason(item, group, findItemKey(item));
        if (reason) return reason;
      } else if (isCeilingModel(item.modelKey)) {
        if (!ceilingWallSupport(Number(item.x), Number(item.z), item.sizeX, item.sizeZ, Number(item.y || 0))) return "선택하지 않은 천장의 지지 벽이 사라집니다.";
      } else if (item.modelKey !== "FLOOR" && !isFootprintOnInstalledFloor(footprintBounds(group), surfaces, Number(item.y || 0))
        && !(isWallModel(item.modelKey) && wallTopSupport(item, Number(item.x), Number(item.z), Number(item.y || 0)))) {
        return `선택하지 않은 「${item.label}」의 지지 위치가 사라집니다.`;
      }
    }
    return "";
  }
  function moveSelectionByKeyboard(event) {
    const selected = areaKeys.size ? items.filter((item) => areaKeys.has(findItemKey(item))) : [selectedItem];
    if (!selected.length) return;
    if (selected.some((item) => item?.locked)) { options.onHint?.("선택 항목에 잠긴 설비가 있습니다. 잠금을 해제한 뒤 이동하세요."); return; }
    if (selected.some((item) => item.parentId != null || item.parentClientKey || !["RACK", "EQUIPMENT"].includes(item.itemType))) {
      options.onHint?.("방향키 이동은 바닥·벽과 독립 설비를 선택해 사용할 수 있습니다."); return;
    }
    const direction = camera.getWorldDirection(new THREE.Vector3());
    const delta = keyboardMoveDelta(event.code, direction.x, direction.z, event.shiftKey ? 1 : .1);
    if (!delta) return;
    const previous = selected.map((item) => ({ item, x: Number(item.x), z: Number(item.z), group: itemGroups.get(findItemKey(item)) }));
    if (previous.some((entry) => !entry.group)) return;
    for (const entry of previous) {
      entry.item.x = Math.round((entry.x + delta.x) * 10000) / 10000;
      entry.item.z = Math.round((entry.z + delta.z) * 10000) / 10000;
      entry.group.position.set(entry.item.x, Number(entry.item.y || 0), entry.item.z);
    }
    const keys = new Set(selected.map(findItemKey));
    const problem = areaChangeProblem(keys);
    if (problem) {
      for (const entry of previous) { entry.item.x = entry.x; entry.item.z = entry.z; entry.group.position.set(entry.x, Number(entry.item.y || 0), entry.z); }
      options.onHint?.(problem); return;
    }
    refreshCeilingSurfaces(); refreshAllWallSurfaces();
    if (areaKeys.size) refreshAreaHelpers(); else updateSelectionBox();
    options.onChange?.(items); options.onHint?.(`방향키로 ${event.shiftKey ? "1m" : "0.1m"} 이동했습니다. 저장하면 반영됩니다.`);
  }
  const onDown = (event) => {
    if (placement) {
      if (event.button !== 0) return;
      const point = placementPointerPoint(event);
      if (!point) return;
      positionPlacementAt(point.x, point.z);
      dragState = { kind: "placement", pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, moved: false };
      if (placement.item.modelKey === "FLOOR") {
        dragState.floorStart = { x: placement.item.x, z: placement.item.z };
        dragState.floorPointerStart = { x: point.x, z: point.z };
      }
      if (isWallModel(placement.item.modelKey)) {
        dragState.wallStart = { x: placement.item.x, y: placement.item.y, z: placement.item.z };
        dragState.wallPointerStart = { x: point.x, z: point.z };
        if (placement.item._wallFloor === 2) dragState.wallPointerHeight = point.y;
      }
      controls.enabled = false;
      renderer.domElement.setPointerCapture(event.pointerId);
      event.preventDefault(); return;
    }
    if (!options.editing || walking) return;
    if (areaSelectMode && event.button === 0) {
      dragState = { kind: "area-select", pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, moved: false };
      controls.enabled = false; renderer.domElement.setPointerCapture(event.pointerId); event.preventDefault(); return;
    }
    if ((wholeSceneMoveMode || areaMoveMode) && event.button === 0) {
      if (items.some((item) => item.locked && (wholeSceneMoveMode || areaKeys.has(findItemKey(item))))) {
        options.onHint?.("잠긴 설비가 포함되어 이동할 수 없습니다. 잠금을 먼저 해제하세요."); return;
      }
      intersect(event, []);
      const point = new THREE.Vector3();
      if (!raycaster.ray.intersectPlane(ground, point)) return;
      const rootsAtStart = items.filter((item) => item.parentId == null && !item.parentClientKey && (!areaMoveMode || areaKeys.has(findItemKey(item))))
        .map((item) => ({ item, group: itemGroups.get(findItemKey(item)) }))
        .filter((entry) => entry.group)
        .map((entry) => ({ ...entry, x: entry.group.position.x, y: entry.group.position.y, z: entry.group.position.z }));
      dragState = { kind: areaMoveMode ? "area-move" : "whole-scene", pointerId: event.pointerId, startPoint: point.clone(), startX: event.clientX,
        startY: event.clientY, moved: false, roots: rootsAtStart };
      controls.enabled = false; renderer.domElement.setPointerCapture(event.pointerId); event.preventDefault(); return;
    }
    const hits = intersect(event, roots());
    const hit = hits.find((entry) => isEditableSceneRoot(nearestSceneItemRoot(entry.object)));
    if (hit) {
      const rack = nearestSceneItemRoot(hit.object);
      const hitItem = rack.userData.sceneItem;
      if (areaKeys.size) { areaKeys.clear(); refreshAreaHelpers(); }
      if (hitItem.locked) {
        sceneActionsVisible = true; selectItem(findItemKey(hitItem)); options.onHostSelected?.(hitItem);
        options.onHint?.("잠긴 설비입니다. 편집하려면 잠금 해제를 누르세요."); event.preventDefault(); return;
      }
      const wasSelected = selectedItem && findItemKey(selectedItem) === findItemKey(hitItem);
      if ((hitItem.parentId != null || hitItem.parentClientKey) && (isDoorModel(hitItem.modelKey) || isWindowModel(hitItem.modelKey))) {
        selectItem(findItemKey(hitItem));
        const wall = items.find((item) => item.id != null
          ? Number(item.id) === Number(hitItem.parentId)
          : item.clientKey === hitItem.parentClientKey);
        const local = wall && localWallPoint(event, wall);
        if (local) {
          dragState = { kind: "wall-opening", group: rack, item: hitItem, wall, pointerId: event.pointerId,
            offsetX: Number(hitItem.x || 0) - local.x, originalX: Number(hitItem.x || 0),
            startX: event.clientX, startY: event.clientY, moved: false, invalidReason: "", wasSelected };
          controls.enabled = false; renderer.domElement.setPointerCapture(event.pointerId);
        }
        event.preventDefault(); return;
      }
      if (moveMode && selectedItem && findItemKey(selectedItem) !== findItemKey(hitItem)) {
        options.onHint?.(`이동 대상으로 「${selectedItem.label}」이 선택되어 있습니다.`); return;
      }
      if (moveMode && !selectedItem) { options.onHint?.("이동할 오브젝트를 목록에서 선택하세요."); return; }
      selectItem(findItemKey(hitItem));
      const point = new THREE.Vector3(); raycaster.ray.intersectPlane(ground, point);
      dragState = { group: rack, item: hitItem, pointerId: event.pointerId,
        offset: new THREE.Vector3(rack.position.x - point.x, 0, rack.position.z - point.z),
        originalX: rack.position.x, originalY: rack.position.y, originalZ: rack.position.z, invalidReason: "",
        originalSizeX: Number(hitItem.sizeX) || 4, originalSizeZ: Number(hitItem.sizeZ) || 4,
        startX: event.clientX, startY: event.clientY, moved: false, wasSelected };
      controls.enabled = false; renderer.domElement.setPointerCapture(event.pointerId); event.preventDefault();
    }
  };
  const onMove = (event) => {
    if (placement) {
      const point = placementPointerPoint(event, dragState?.wallPointerHeight ?? null);
      if (point) {
        if (dragState?.floorStart && dragState.pointerId === event.pointerId) {
          if (Math.hypot(event.clientX - dragState.startX, event.clientY - dragState.startY) > 5) dragState.moved = true;
          if (dragState.moved) {
            const width = Number(placement.item.sizeX) || 4, depth = Number(placement.item.sizeZ) || 4;
            const end = { x: dragState.floorStart.x + point.x - dragState.floorPointerStart.x, z: dragState.floorStart.z + point.z - dragState.floorPointerStart.z };
            dragState.floorCandidates = floorBatchCandidates(dragState.floorStart, end, width, depth);
            placement.group.visible = false; showFloorBatch(dragState.floorCandidates, width, depth);
          }
        } else if (dragState?.wallStart && dragState.pointerId === event.pointerId) {
          if (Math.hypot(event.clientX - dragState.startX, event.clientY - dragState.startY) > 5) dragState.moved = true;
          if (dragState.moved) {
            const extent = wallDragExtent(dragState.wallStart, event, dragState.startX, dragState.startY);
            dragState.wallCandidates = wallBatchCandidates(dragState.wallStart, extent.end, extent.rows);
            placement.group.visible = false; showWallBatch(dragState.wallCandidates);
          }
        } else positionPlacementAt(point.x, point.z);
      }
      return;
    }
    if (!dragState || event.pointerId !== dragState.pointerId) return;
    if (Math.hypot(event.clientX - dragState.startX, event.clientY - dragState.startY) > 5) dragState.moved = true;
    if (!dragState.moved) return;
    if (dragState.kind === "area-select") {
      const rect = container.getBoundingClientRect();
      Object.assign(areaRectangle.style, { display: "block", left: `${Math.min(dragState.startX, event.clientX) - rect.left}px`,
        top: `${Math.min(dragState.startY, event.clientY) - rect.top}px`, width: `${Math.abs(event.clientX - dragState.startX)}px`, height: `${Math.abs(event.clientY - dragState.startY)}px` });
      event.preventDefault(); return;
    }
    if (dragState.kind === "whole-scene" || dragState.kind === "area-move") {
      intersect(event, []);
      const point = new THREE.Vector3();
      if (!raycaster.ray.intersectPlane(ground, point)) return;
      const deltaX = point.x - dragState.startPoint.x, deltaZ = point.z - dragState.startPoint.z;
      for (const root of dragState.roots) {
        root.group.position.set(root.x + deltaX, root.y, root.z + deltaZ);
        root.item.x = root.group.position.x; root.item.y = root.group.position.y; root.item.z = root.group.position.z;
      }
      if (selectedItem) updateSelectionBox();
      renderer.domElement.style.cursor = "grabbing"; event.preventDefault(); return;
    }
    if (dragState.kind === "wall-opening") {
      const local = localWallPoint(event, dragState.wall);
      if (!local) return;
      const size = openingSize(dragState.item.modelKey, dragState.item.sizeZ);
      const wallHalf = (Number(dragState.wall.sizeX) || 4) / 2;
      const x = THREE.MathUtils.clamp(Math.round((local.x + dragState.offsetX) / .05) * .05, -wallHalf + size.width / 2, wallHalf - size.width / 2);
      const collides = openingsForWall(dragState.wall).some((opening) => opening !== dragState.item
        && Math.abs(x - Number(opening.x || 0)) < (size.width + openingSize(opening.modelKey, opening.sizeZ).width) / 2 + .06);
      dragState.item.x = x; dragState.group.position.x = x;
      dragState.invalidReason = collides ? "문·창문끼리 겹치지 않도록 위치를 옮기세요." : "";
      refreshWallSurface(dragState.wall);
      renderer.domElement.style.cursor = dragState.invalidReason ? "not-allowed" : "grabbing";
      event.preventDefault(); return;
    }
    intersect(event, []);
    const point = new THREE.Vector3(); if (!raycaster.ray.intersectPlane(ground, point)) return;
    const snapped = snapPosition(point.x + dragState.offset.x, point.z + dragState.offset.z, dragState.item);
    const level = placementLevelAt(dragState.item, snapped.x, snapped.z);
    dragState.group.position.set(snapped.x, level, snapped.z);
    dragState.item.x = dragState.group.position.x; dragState.item.y = level; dragState.item.z = dragState.group.position.z;
    dragState.invalidReason = placementInvalidReason(dragState.item, dragState.group, findItemKey(dragState.item));
    renderer.domElement.style.cursor = dragState.invalidReason ? "not-allowed" : "grabbing";
    event.preventDefault();
  };
  const onUp = (event) => {
    if (!dragState || event.pointerId !== dragState.pointerId) return;
    const completed = dragState; dragState = null; updateControlMode(); clearGuides();
    renderer.domElement.style.cursor = wholeSceneMoveMode ? "grab" : "";
    if (completed.kind === "area-select") {
      areaRectangle.style.display = "none"; areaKeys.clear();
      if (completed.moved && event.type !== "pointercancel") {
        const rect = renderer.domElement.getBoundingClientRect();
        const minX = Math.min(completed.startX, event.clientX), maxX = Math.max(completed.startX, event.clientX);
        const minY = Math.min(completed.startY, event.clientY), maxY = Math.max(completed.startY, event.clientY);
        for (const item of items.filter((entry) => (areaSelectEquipmentOnly
          ? (isEquipmentFrameKey(entry.modelKey) || isFrameEquipmentItem(entry))
          : (entry.modelKey === "FLOOR" || isWallModel(entry.modelKey))) && entry.parentId == null && !entry.parentClientKey)) {
          const group = itemGroups.get(findItemKey(item)); if (!group) continue;
          const bounds = footprintBounds(group); if (bounds.isEmpty()) continue;
          const point = bounds.getCenter(new THREE.Vector3()).project(camera);
          const sx = rect.left + (point.x + 1) * rect.width / 2, sy = rect.top + (1 - point.y) * rect.height / 2;
          if (point.z >= -1 && point.z <= 1 && sx >= minX && sx <= maxX && sy >= minY && sy <= maxY) areaKeys.add(findItemKey(item));
        }
      }
      areaSelectMode = false;
      if (areaKeys.size) selectItem(null);
      updateControlMode(); refreshAreaHelpers();
      options.onHint?.(`${areaKeys.size}개 선택됨 · 방향키로 함께 이동하거나 선택 이동을 사용하세요. Esc로 선택 해제`); return;
    }
    if (completed.kind === "area-move") {
      const problem = event.type === "pointercancel" ? "이동을 취소했습니다." : completed.moved ? areaChangeProblem(areaKeys) : "";
      if (problem) {
        for (const root of completed.roots) { root.group.position.set(root.x, root.y, root.z); root.item.x = root.x; root.item.y = root.y; root.item.z = root.z; }
        options.onHint?.(problem);
      } else if (completed.moved) { refreshAllWallSurfaces(); refreshCeilingSurfaces(); options.onChange?.(items); options.onHint?.("선택한 배치를 이동했습니다. 저장하면 반영됩니다."); }
      areaMoveMode = false; updateControlMode(); refreshAreaHelpers(); return;
    }
    if (completed.kind === "whole-scene") {
      if (completed.moved) {
        options.onChange?.(items); options.onHint?.("도면 전체를 이동했습니다. 구조물 간 배치는 유지됩니다. 저장을 눌러 반영하세요.");
      } else options.onHint?.("도면 전체 이동 모드입니다. 드래그해 옮기거나 Esc를 눌러 종료하세요.");
      return;
    }
    if (completed.kind === "placement") {
      if (completed.floorStart && completed.moved) {
        clearFloorBatchPreview();
        if (placement) { placement.group.visible = true; if (event.type !== "pointercancel") commitFloorBatch(completed.floorCandidates); }
        return;
      }
      if (completed.wallStart && completed.moved) {
        clearFloorBatchPreview();
        if (placement) { placement.group.visible = true; if (event.type !== "pointercancel") commitWallBatch(completed.wallCandidates); }
        return;
      }
      commitPlacement();
      return;
    }
    if (completed.kind === "wall-opening") {
      if (completed.invalidReason) {
        completed.item.x = completed.originalX; completed.group.position.x = completed.originalX;
        refreshWallSurface(completed.wall); options.onHint?.(completed.invalidReason);
      } else if (completed.moved) {
        options.onChange?.(items); options.onHint?.(`${completed.item.label} 위치를 조정했습니다. 저장을 눌러 반영하세요.`);
      } else { if (completed.wasSelected) sceneActionsVisible = !sceneActionsVisible; options.onHint?.("문·창문을 좌우로 드래그해 벽에서 위치를 조정하세요."); }
      return;
    }
    if (completed.moved && completed.invalidReason) {
      completed.group.position.set(completed.originalX, completed.originalY, completed.originalZ);
      completed.item.x = completed.originalX; completed.item.y = completed.originalY; completed.item.z = completed.originalZ;
      if (completed.item.modelKey === "FLOOR") {
        completed.item.sizeX = completed.originalSizeX; completed.item.sizeZ = completed.originalSizeZ;
        completed.group.scale.set(completed.originalSizeX / 4, 1, completed.originalSizeZ / 4);
      }
      options.onHint?.(completed.invalidReason); return;
    }
    if (completed.moved && completed.item.modelKey === "FLOOR") {
      rememberVacantFloorSlot(completed.originalX, completed.originalZ, completed.originalSizeX, completed.originalSizeZ);
      consumeVacantFloorSlot(completed.item.x, completed.item.z);
    }
    if (completed.moved) { refreshCeilingSurfaces(); refreshAllWallSurfaces(); options.onChange?.(items); }
    else { if (completed.wasSelected) sceneActionsVisible = !sceneActionsVisible; options.onHostSelected?.(completed.item); }
  };
  renderer.domElement.addEventListener("pointerdown", onDown); renderer.domElement.addEventListener("pointermove", onMove); renderer.domElement.addEventListener("pointerup", onUp); renderer.domElement.addEventListener("pointercancel", onUp);
  const onRackDoubleClick = (event) => {
    if (walking || placement || areaSelectMode || areaMoveMode || wholeSceneMoveMode) return;
    const hit = intersect(event, roots()).find((entry) => isEditableSceneRoot(nearestSceneItemRoot(entry.object)));
    const item = hit && nearestSceneItemRoot(hit.object)?.userData?.sceneItem;
    if (item?.modelKey === "SR_INROW_PANEL") { event.preventDefault(); options.onPanelDetail?.(structuredClone(item)); return; }
    const action = rackDetailAction(item);
    if (action === "ignore") return;
    event.preventDefault();
    if (action === "locked") {
      options.onLockedRack?.(structuredClone(item));
      return;
    }
    options.onRackDetail?.(structuredClone(item));
  };
  renderer.domElement.addEventListener("dblclick", onRackDoubleClick);
  function renderRackLayout(key, layout) {
    const rack = itemGroups.get(key);
    if (!rack) return;
    const old = rack.getObjectByName("rack-asset-overlay");
    if (old) { rack.remove(old); old.traverse((node) => { if (node.userData.rackSharedModel) return; node.geometry?.dispose(); node.material?.map?.dispose(); node.material?.dispose(); }); }
    rack.updateMatrixWorld(true);
    const inverse = rack.matrixWorld.clone().invert(), bounds = new THREE.Box3();
    rack.traverse((node) => {
      if (!node.isMesh || !node.geometry) return;
      for (let p = node; p && p !== rack; p = p.parent) if (p.userData?.sceneKind === "SENSOR") return;
      if (!node.geometry.boundingBox) node.geometry.computeBoundingBox();
      if (node.geometry.boundingBox) bounds.union(node.geometry.boundingBox.clone().applyMatrix4(inverse.clone().multiply(node.matrixWorld)));
    });
    if (bounds.isEmpty()) return;
    const dimensions = bounds.getSize(new THREE.Vector3()), center = bounds.getCenter(new THREE.Vector3());
    if (!rack.userData.sensorDoorPivots?.length) {
      rack.userData.sensorDoorPivots = attachRackDoorPivots(rack, rack);
      if (rack.userData.sensorDoorPivots.length) rack.userData.sensorDoorAngle = 0;
    }
    rack.userData.sensorDoorTarget = typeof layout.doorOpen === "boolean" ? (layout.doorOpen ? 1.95 : 0) : null;
    const overlay = new THREE.Group(); overlay.name = "rack-asset-overlay"; overlay.userData.sceneKind = "SENSOR";
    const display = flat ? null : layout.sceneDisplay;
    const rows = rackDisplayRows(display);
    if (rows.length) {
      const canvas = document.createElement("canvas"); canvas.width = 680; canvas.height = 72 + rows.length * 68;
      const ctx = canvas.getContext("2d");
      ctx.textAlign = "center"; ctx.textBaseline = "middle"; ctx.lineJoin = "round";
      ctx.strokeStyle = "rgba(3, 11, 18, .95)"; ctx.lineWidth = 8;
      const outlinedText = (text, x, y, maxWidth, color) => {
        ctx.strokeText(text, x, y, maxWidth); ctx.fillStyle = color; ctx.fillText(text, x, y, maxWidth);
      };
      const name = rack.userData.sceneItem?.label || rack.userData.sceneItem?.locationNodeCode || "랙";
      ctx.font = "bold 40px sans-serif"; outlinedText(name, canvas.width / 2, 32, 640, "#f4f8fb");
      rows.forEach((row, index) => {
        const y = 98 + index * 68;
        if (row.label) {
          ctx.font = "bold 31px sans-serif"; ctx.textAlign = "right";
          outlinedText(row.label, 425, y, 406, "#e7f2f5");
        }
        ctx.font = "bold 52px sans-serif"; ctx.textAlign = row.label ? "left" : "center";
        outlinedText(row.value, row.label ? 438 : canvas.width / 2, y, row.label ? 228 : 640, "#91ed42");
      });
      const texture = new THREE.CanvasTexture(canvas); texture.colorSpace = THREE.SRGBColorSpace;
      const sprite = new THREE.Sprite(new THREE.SpriteMaterial({map:texture, transparent:true, depthTest:false}));
      sprite.position.set(center.x, bounds.max.y + Math.max(.12, dimensions.y * .035), center.z); sprite.center.set(.5, 0);
      const position = new THREE.Vector3(), parentScale = new THREE.Vector3();
      sprite.onBeforeRender = () => {
        sprite.getWorldPosition(position); rack.getWorldScale(parentScale);
        const distance = camera.position.distanceTo(position), pixelWidth = 210;
        const worldPerPixel = 2 * distance * Math.tan(THREE.MathUtils.degToRad(camera.fov) / 2) / Math.max(renderer.domElement.clientHeight, 1);
        sprite.scale.set(pixelWidth * worldPerPixel / Math.max(Math.abs(parentScale.x), .001), pixelWidth * (canvas.height / canvas.width) * worldPerPixel / Math.max(Math.abs(parentScale.y), .001), 1);
      };
      sprite.renderOrder = 30; overlay.add(sprite);
    }
    const heatColor = rackTemperatureColor(display?.temperature?.value, display?.warningC ?? 25, display?.dangerC ?? 30);
    if (heatColor) {
      const canvas = document.createElement("canvas"); canvas.width = canvas.height = 128;
      const ctx = canvas.getContext("2d"), gradient = ctx.createRadialGradient(64,64,4,64,64,64);
      gradient.addColorStop(0, heatColor); gradient.addColorStop(.45, heatColor + "99"); gradient.addColorStop(1, heatColor + "00");
      ctx.fillStyle = gradient; ctx.fillRect(0,0,128,128);
      const texture = new THREE.CanvasTexture(canvas); texture.colorSpace = THREE.SRGBColorSpace;
      const heat = new THREE.Mesh(new THREE.PlaneGeometry(Math.max(dimensions.x*3,1.4), Math.max(dimensions.z*3,1.4)), new THREE.MeshBasicMaterial({map:texture,transparent:true,opacity:.7,depthWrite:false,side:THREE.DoubleSide,polygonOffset:true,polygonOffsetFactor:-2}));
      heat.rotation.x = -Math.PI / 2; heat.position.set(center.x, bounds.min.y + .025, center.z); heat.renderOrder = 2; overlay.add(heat);
    }
    const capacity = Number(layout.rackUCapacity) || 0;
    for (const placement of layout.placements || []) {
      let width, height, depth, x = center.x, y = center.y, z = bounds.max.z + .015;
      const custom = [placement.positionX, placement.positionY, placement.positionZ].every((v) => v != null && Number.isFinite(Number(v)));
      if (custom && !placement.sceneModelKey && (placement.mountType === "RACK_SIDE" || placement.mountType === "RACK_REAR")) {
        const device = devicesById.get(Number(placement.deviceId));
        const values = (device?.points || []).filter((point) => point.value != null).slice(0, 2)
          .map((point) => `${point.pointName} ${Number(point.value).toLocaleString("ko-KR", { maximumFractionDigits: 1 })}${point.unit || ""}`).join(" · ");
        const marker = makeSensor({ label: placement.deviceName }, values || "최신값 없음");
        marker.position.set(bounds.min.x + dimensions.x * Number(placement.positionX), bounds.min.y + dimensions.y * Number(placement.positionY), bounds.min.z + dimensions.z * Number(placement.positionZ));
        overlay.add(marker); continue;
      }
      if (custom && placement.sceneModelKey && (placement.mountType === "RACK_SIDE" || placement.mountType === "RACK_REAR")) {
        const verticalPdu = placement.sceneModelKey === "SR_PDU_VERTICAL" && placement.mountType === "RACK_SIDE";
        width = dimensions.x * (verticalPdu ? .07 : .12); height = dimensions.y * (verticalPdu ? Math.max(.1, Math.min(1, Number(placement.visualLengthRatio ?? .78))) : .06); depth = dimensions.z * (verticalPdu ? .07 : .12);
        x = bounds.min.x + dimensions.x * Number(placement.positionX);
        y = bounds.min.y + dimensions.y * Number(placement.positionY);
        z = bounds.min.z + dimensions.z * Number(placement.positionZ);
      } else if (placement.mountType === "RACK_U" && capacity && Number(placement.uPosition) > 0 && Number(placement.uHeight) > 0) {
        width = dimensions.x * .7; height = dimensions.y * .88 * Number(placement.uHeight) / capacity; depth = .035;
        y = bounds.min.y + dimensions.y * .06 + dimensions.y * .88 * (Number(placement.uPosition) - 1 + Number(placement.uHeight) / 2) / capacity;
      } else if (placement.mountType === "RACK_SIDE") {
        const verticalPdu = placement.sceneModelKey === "SR_PDU_VERTICAL";
        width = verticalPdu ? dimensions.x * .07 : .035; height = dimensions.y * (verticalPdu ? Math.max(.1, Math.min(1, Number(placement.visualLengthRatio ?? (1.8 / 2.3)))) : .75); depth = dimensions.z * (verticalPdu ? .07 : .12);
        x = verticalPdu ? bounds.min.x + dimensions.x * (placement.rackSide === "LEFT" ? .03125 : .96875) : placement.rackSide === "LEFT" ? bounds.min.x - .02 : bounds.max.x + .02;
        if (verticalPdu) y = bounds.min.y + dimensions.y * (1.18 / 2.3);
        z = verticalPdu ? bounds.min.z + dimensions.z * ((.4 + .48) / .96) : center.z;
      } else if (placement.mountType === "RACK_REAR") {
        width = dimensions.x * .3; height = dimensions.y * .65; depth = .035; z = bounds.min.z - .02;
      } else continue;
      const color = /^#[0-9a-f]{6}$/i.test(placement.assetColor || "") ? placement.assetColor : "#267588";
      const marker = new THREE.Mesh(new THREE.BoxGeometry(Math.max(width, .02), Math.max(height, .02), Math.max(depth, .02)), new THREE.MeshBasicMaterial({ color }));
      marker.position.set(x, y, z); marker.userData.rackDeviceId = placement.deviceId; overlay.add(marker);
      if (!flat && placement.sceneModelKey === "SR_DRAGINO_TH" && placement.deviceId != null) {
        const canvas = document.createElement("canvas"); canvas.width = 380; canvas.height = 156;
        const ctx = canvas.getContext("2d");
        ctx.fillStyle = "#72e835"; ctx.beginPath(); ctx.roundRect(8, 44, 30, 58, 10); ctx.fill();
        ctx.font = "bold 44px sans-serif"; ctx.textBaseline = "middle"; ctx.lineJoin = "round";
        ctx.lineWidth = 7; ctx.strokeStyle = "rgba(3,11,18,.95)"; ctx.fillStyle = "#f5fafc";
        temperatureHumidityLines(layout.sensorReadings?.[placement.deviceId]).forEach((line, index) => {
          const baseline = 48 + index * 57;
          ctx.strokeText(line, 54, baseline, 315); ctx.fillText(line, 54, baseline, 315);
        });
        const texture = new THREE.CanvasTexture(canvas); texture.colorSpace = THREE.SRGBColorSpace;
        const text = new THREE.Sprite(new THREE.SpriteMaterial({ map: texture, transparent: true, depthTest: false, depthWrite: false }));
        text.position.set(x, y, z); text.center.set(.05, .5); text.renderOrder = 31;
        const position = new THREE.Vector3(), parentScale = new THREE.Vector3();
        text.onBeforeRender = () => {
          text.getWorldPosition(position); rack.getWorldScale(parentScale);
          const distance = camera.position.distanceTo(position), pixelWidth = 100;
          const worldPerPixel = 2 * distance * Math.tan(THREE.MathUtils.degToRad(camera.fov) / 2) / Math.max(renderer.domElement.clientHeight, 1);
          text.scale.set(pixelWidth * worldPerPixel / Math.max(Math.abs(parentScale.x), .001), pixelWidth * canvas.height / canvas.width * worldPerPixel / Math.max(Math.abs(parentScale.y), .001), 1);
        };
        overlay.add(text);
      }
      const model = placement.sceneModelKey && showroomModelByKey(placement.sceneModelKey);
      if (model) loadShowroomModel(model).then((gltf) => {
        if (destroyed || overlay.parent !== rack) return;
        const object = prepareShowroomObject(model, gltf.scene.clone(true));
        object.traverse((node) => { node.userData.rackSharedModel = true; });
        const modelBounds = new THREE.Box3().setFromObject(object);
        if (modelBounds.isEmpty()) return;
        const modelSize = modelBounds.getSize(new THREE.Vector3()), modelCenter = modelBounds.getCenter(new THREE.Vector3());
        const wrapper = new THREE.Group(); wrapper.add(object);
        const displayDepth = placement.mountType === "RACK_U" ? dimensions.z * .72 : Math.max(depth, .02);
        wrapper.scale.set(Math.max(width, .02) / Math.max(modelSize.x, .001), Math.max(height, .02) / Math.max(modelSize.y, .001), displayDepth / Math.max(modelSize.z, .001));
        object.position.sub(modelCenter);
        wrapper.position.set(x, y, placement.mountType === "RACK_U" ? center.z : z);
        if (["SR_DRAGINO_TH", "SR_DRAGINO_DOOR"].includes(placement.sceneModelKey)) wrapper.rotation.y = placement.mountType === "RACK_REAR" ? (z >= center.z ? 0 : Math.PI) : placement.rackSide === "LEFT" ? -Math.PI / 2 : Math.PI / 2;
        overlay.add(wrapper); overlay.remove(marker); marker.geometry.dispose(); marker.material.dispose();
      }).catch((error) => console.warn("랙 장착 모델을 불러오지 못했습니다", error));
    }
    rack.add(overlay);
  }
  const animate = (now = performance.now()) => {
    if (destroyed) return;
    const deltaSeconds = Math.min(.05, Math.max(0, (now - lastFrameTime) / 1000));
    lastFrameTime = now;
    updateWalking(deltaSeconds);
    if (options.editing && now - lastOverlapUpdate > 350) { updateFloorOverlapIndicators(); lastOverlapUpdate = now; }
    for (const group of itemGroups.values()) {
      const pivots = group.userData.sensorDoorPivots, target = group.userData.sensorDoorTarget;
      if (pivots?.length && typeof target === "number") { group.userData.sensorDoorAngle += (target - group.userData.sensorDoorAngle) * (1 - Math.exp(-7 * deltaSeconds)); for (const door of pivots) door.pivot.rotation.y = door.direction * group.userData.sensorDoorAngle; }
    }
    controls.update();
    if (selectionBox && selectedItem) {
      const selectedGroup = itemGroups.get(findItemKey(selectedItem));
      if (selectedGroup) selectionBox.box.copy(footprintBounds(selectedGroup));
    }
    updateSceneActionBar();
    renderer.render(scene, camera); raf = requestAnimationFrame(animate);
  }; animate();
  const api = {
    setRackLayout(locationCode, layout) {
      for (const item of items.filter((entry) => entry.locationNodeCode === locationCode)) {
        const key = findItemKey(item), group = itemGroups.get(key);
        if (!group) continue;
        group.userData.rackLayout = layout;
        const prior = group.userData.onImportedModelReady;
        if (!group.userData.rackLayoutHooked) {
          group.userData.rackLayoutHooked = true;
          group.userData.onImportedModelReady = (object) => { prior?.(object); renderRackLayout(key, group.userData.rackLayout); };
        }
        renderRackLayout(key, layout);
      }
    },
    setGroupLocked(groupKey, locked) {
      if (!groupKey || typeof locked !== "boolean") return false;
      const members = items.filter((item) => item.groupKey === groupKey);
      if (!members.length) { options.onHint?.("묶음을 찾을 수 없습니다."); return false; }
      const changed = setGroupLockState(items, groupKey, locked);
      if (changed) {
        options.onChange?.(items);
        if (selectedItem?.groupKey === groupKey) options.onHostSelected?.(selectedItem);
      }
      options.onHint?.(`${members[0].groupName || "설비 묶음"} ${members.length}개 ${locked ? "전체 잠금" : "전체 잠금 해제"} · 저장하면 반영됩니다.`);
      return true;
    },
    selectGroup(groupKey = selectedItem?.groupKey) {
      if (!groupKey || !items.some((item) => item.groupKey === groupKey)) { options.onHint?.("묶인 설비를 먼저 선택하세요."); return false; }
      areaKeys.clear();
      for (const item of items) if (item.groupKey === groupKey) areaKeys.add(findItemKey(item));
      selectItem(null); refreshAreaHelpers();
      options.onHint?.(`묶음 ${areaKeys.size}개 선택됨 · 방향키 또는 선택 이동으로 함께 옮기세요.`);
      return true;
    },
    selectGroupMember(key) {
      const item = items.find((entry) => findItemKey(entry) === key && entry.groupKey);
      if (!item) return false;
      areaKeys.clear(); refreshAreaHelpers(); selectItem(key);
      options.onHostSelected?.(item);
      options.onHint?.(`‘${item.label}’ 개별 선택됨 · 묶음 전체 선택으로 함께 옮길 수 있습니다.`);
      return true;
    },
    groupSelection(name) {
      const selected = items.filter((item) => areaKeys.has(findItemKey(item)));
      if (selected.length < 2 || selected.some((item) => !(isFrameEquipmentItem(item) || isEquipmentFrameKey(item.modelKey)) || item.parentId != null || item.parentClientKey)) {
        options.onHint?.("설비 영역 선택으로 독립 설비를 2개 이상 선택하세요."); return false;
      }
      if (selected.some((item) => item.groupKey)) { options.onHint?.("이미 묶인 설비는 먼저 묶음을 해제하세요."); return false; }
      const groupName = String(name || "").trim();
      if (!groupName || groupName.length > 80) { options.onHint?.("묶음 이름을 1~80자로 입력하세요."); return false; }
      const key = THREE.MathUtils.generateUUID();
      for (const item of selected) { item.groupKey = key; item.groupName = groupName; }
      options.onChange?.(items); options.onHint?.(`${selected.length}개 설비를 묶었습니다. 저장하면 다음에도 함께 선택됩니다.`); return true;
    },
    getGroupableItems() {
      return structuredClone(items.filter((item) => (isFrameEquipmentItem(item) || isEquipmentFrameKey(item.modelKey))
        && item.parentId == null && !item.parentClientKey));
    },
    updateGroup(groupKey, name, memberKeys) {
      const groupName = String(name || "").trim();
      const members = new Set(memberKeys);
      const eligible = items.filter((item) => (isFrameEquipmentItem(item) || isEquipmentFrameKey(item.modelKey))
        && item.parentId == null && !item.parentClientKey);
      if (!groupName || groupName.length > 80 || !items.some((item) => item.groupKey === groupKey)
        || !members.size || eligible.filter((item) => members.has(findItemKey(item))).length !== members.size) {
        options.onHint?.("묶음 이름과 설비를 확인하세요."); return false;
      }
      for (const item of eligible) {
        if (members.has(findItemKey(item))) { item.groupKey = groupKey; item.groupName = groupName; }
        else if (item.groupKey === groupKey) { item.groupKey = null; item.groupName = null; }
      }
      areaKeys.clear(); refreshAreaHelpers();
      options.onChange?.(items); options.onHint?.(`‘${groupName}’ 묶음을 수정했습니다. 도면 저장을 눌러 반영하세요.`); return true;
    },
    ungroupSelection() {
      const keys = new Set(items.filter((item) => areaKeys.has(findItemKey(item)) && item.groupKey).map((item) => item.groupKey));
      if (selectedItem?.groupKey) keys.add(selectedItem.groupKey);
      if (!keys.size) { options.onHint?.("묶인 설비를 먼저 선택하세요."); return false; }
      for (const item of items) if (keys.has(item.groupKey)) { item.groupKey = null; item.groupName = null; }
      options.onChange?.(items); options.onHint?.("설비 묶음을 해제했습니다. 저장하면 반영됩니다."); return true;
    },
    setAreaSelect(enabled, equipmentOnly = false) {
      if (placement) cancelPlacement();
      if (walking) setWalking(false);
      wholeSceneMoveMode = false; areaMoveMode = false; areaSelectMode = Boolean(enabled); areaSelectEquipmentOnly = Boolean(equipmentOnly);
      areaRectangle.style.display = "none"; renderer.domElement.style.cursor = areaSelectMode ? "crosshair" : "";
      updateControlMode(); options.onHint?.(areaSelectMode ? (areaSelectEquipmentOnly ? "함께 옮길 프레임과 설비를 사각형으로 드래그해 선택하세요." : "화면에서 선택할 바닥·벽을 사각형으로 드래그하세요.") : "영역 선택을 종료했습니다.");
      return areaSelectMode;
    },
    setAreaMove(enabled) {
      if (enabled && !areaKeys.size) { options.onHint?.("먼저 영역을 드래그해 이동할 항목을 선택하세요."); return false; }
      if (placement) cancelPlacement();
      areaSelectMode = false; wholeSceneMoveMode = false; areaMoveMode = Boolean(enabled);
      renderer.domElement.style.cursor = areaMoveMode ? "grab" : "";
      updateControlMode(); options.onHint?.(areaMoveMode ? "화면을 드래그해 선택한 항목을 함께 이동하세요." : "선택 이동을 종료했습니다.");
      return areaMoveMode;
    },
    removeAreaSelection() {
      if (!areaKeys.size) { options.onHint?.("먼저 영역을 드래그해 바닥이나 벽을 선택하세요."); return false; }
      const selected = new Set(items.filter((item) => areaKeys.has(findItemKey(item))));
      if ([...selected].some((item) => item.locked)) { options.onHint?.("선택 항목에 잠긴 설비가 있습니다. 잠금을 해제한 뒤 삭제하세요."); return false; }
      const originalItems = items;
      items = items.filter((item) => !selected.has(item));
      const problem = areaChangeProblem();
      items = originalItems;
      if (problem) { options.onHint?.(`${problem} 함께 선택하거나 먼저 옮겨 주세요.`); return false; }
      detachSceneItems(selected); areaKeys.clear(); areaMoveMode = false; refreshAreaHelpers();
      refreshAllWallSurfaces(); refreshCeilingSurfaces(); options.onChange?.(items);
      options.onHint?.(`${selected.size}개를 제거했습니다. 저장하면 반영됩니다.`); return true;
    },
    setWholeSceneMove(enabled) {
      if (enabled && items.some((item) => item.locked)) { options.onHint?.("잠긴 설비가 있어 도면 전체를 이동할 수 없습니다."); return false; }
      wholeSceneMoveMode = Boolean(enabled);
      if (wholeSceneMoveMode) { areaSelectMode = false; areaMoveMode = false; areaRectangle.style.display = "none"; }
      if (wholeSceneMoveMode && placement) cancelPlacement();
      if (wholeSceneMoveMode) moveMode = false;
      renderer.domElement.style.cursor = wholeSceneMoveMode ? "grab" : "";
      updateControlMode();
      options.onWholeSceneMoveChange?.(wholeSceneMoveMode);
      options.onHint?.(wholeSceneMoveMode ? "도면 전체 이동 모드 · 도면을 드래그해 옮기세요 · Esc로 종료" : "도면 전체 이동을 종료했습니다.");
      return wholeSceneMoveMode;
    },
    saveViewAsDefault: saveCurrentViewAsDefault,
    alignView(direction) {
      if (flat || walking || !["top", "front", "side", "down45", "down60"].includes(direction)) return false;
      const distance = THREE.MathUtils.clamp(camera.position.distanceTo(controls.target), controls.minDistance, controls.maxDistance);
      const damping = controls.enableDamping;
      controls.enableDamping = false; controls.update();
      let offset;
      if (direction === "down45" || direction === "down60") {
        const angle = THREE.MathUtils.degToRad(direction === "down45" ? 45 : 60);
        const horizontal = camera.position.clone().sub(controls.target).setY(0);
        if (horizontal.lengthSq() < .0001) horizontal.set(0, 0, 1);
        horizontal.normalize().multiplyScalar(distance * Math.cos(angle));
        offset = horizontal.setY(distance * Math.sin(angle));
      } else {
        offset = direction === "top" ? new THREE.Vector3(0, distance, .001)
          : direction === "front" ? new THREE.Vector3(0, 0, distance) : new THREE.Vector3(distance, 0, 0);
      }
      camera.position.copy(controls.target).add(offset);
      controls.update(); controls.enableDamping = damping; saveViewState();
      return true;
    },
    addEquipment(modelKey, initialRotation = null, wallFloor = null) {
      return beginPlacement(modelKey, initialRotation, wallFloor);
    },
    addWallOpening(modelKey) {
      if (!(isDoorModel(modelKey) || isWindowModel(modelKey))) return false;
      if (walking) setWalking(false);
      if (placement) cancelPlacement();
      const wall = selectedItem;
      const wallGroup = wall && itemGroups.get(findItemKey(wall));
      if (!wall || wall.itemType !== "EQUIPMENT" || !isWallModel(wall.modelKey) || !wallGroup?.userData.wallSegment) {
        options.onHint?.("문·창문을 설치할 벽을 먼저 클릭해 선택하세요."); return false;
      }
      const openingScale = Math.min(5, Number(wall.sizeZ) || 4);
      const size = openingSize(modelKey, openingScale);
      if (size.bottom + size.height > 2.8 * (Number(wall.sizeZ) || 4) / 4 + .001) {
        options.onHint?.("선택한 벽의 높이가 새 문·창문보다 낮습니다. 더 높은 벽을 선택하세요."); return false;
      }
      const existing = items.filter((item) => (isDoorModel(item.modelKey) || isWindowModel(item.modelKey))
        && (item.parentId != null ? Number(item.parentId) === Number(wall.id) : item.parentClientKey === findItemKey(wall)));
      const candidates = [0, -.7, .7, -1.2, 1.2].filter((candidate) => Math.abs(candidate) <= (Number(wall.sizeX) || 4) / 2 - size.width / 2 - .05);
      const x = candidates.find((candidate) => existing.every((item) => {
        const other = openingSize(item.modelKey, item.sizeZ);
        return !other || Math.abs(candidate - Number(item.x || 0)) >= (size.width + other.width) / 2 + .08;
      }));
      if (x == null) { options.onHint?.("선택한 벽에 더 설치할 공간이 없습니다."); return false; }
      const number = existing.filter((item) => item.modelKey === modelKey).length + 1;
      const item = { id: null, clientKey: makeKey(), parentId: wall.id ?? null,
        parentClientKey: wall.id != null ? `db-${wall.id}` : wall.clientKey, itemType: "EQUIPMENT",
        deviceId: null, pointName: null, label: `${modelKey === "DOOR" ? "문" : modelKey === "WINDOW" ? "창문" : modelKey === "DOOR_DOUBLE" ? "양문" : modelKey === "DOOR_SLIDING" ? "미닫이문" : "큰 창문"} ${number}`,
        modelKey, x, y: 0, z: 0, rotationY: 0, sizeX: openingScale, sizeZ: openingScale };
      const group = makeEquipment(item); group.position.set(x, 0, 0); wallGroup.userData.wallSegment.add(group);
      items.push(item); itemGroups.set(item.clientKey, group);
      refreshWallSurface(wall);
      selectItem(item.clientKey); options.onChange?.(items);
      options.onHint?.(`${item.label}을(를) ${wall.label}에 설치했습니다. 저장하면 반영됩니다.`); return true;
    },
    duplicateItem,
    duplicateSelected(key) { const target = items.find((item) => findItemKey(item) === key); return target ? duplicateItem(target) : false; },
    selectItem,
    setMoveTarget,
    flipItem,
    rotateItem,
    toggleSnap,
    setBuildMode(enabled) { options.onBuildModeToggle?.(Boolean(enabled)); },
    linkRackLocation(key, locationCode) {
      const target = items.find((item) => findItemKey(item) === key);
      if (!target || rackDetailAction({ ...target, locked: false }) !== "open") return false;
      if (target.locked) { options.onHint?.("잠긴 랙은 잠금 해제 후 연결할 수 있습니다."); return false; }
      if (items.some((item) => item !== target && item.locationNodeCode === locationCode)) {
        options.onHint?.("이 페이지에 이미 연결된 랙입니다."); return false;
      }
      target.locationNodeCode = locationCode;
      options.onChange?.(items);
      options.onHint?.("실제 랙을 연결했습니다. 도면 저장을 눌러 연결을 보관하세요.");
      return true;
    },
    attachDevice(hostKey, selection) {
      const host = items.find((item) => findItemKey(item) === hostKey && ["RACK", "EQUIPMENT"].includes(item.itemType));
      const deviceId = Number(selection?.deviceId);
      const device = devicesById.get(deviceId);
      if (!host || !device) { options.onHint?.("설비 또는 페이지 장비를 찾을 수 없습니다."); return false; }
      if (host.locked) { options.onHint?.("잠긴 설비에는 장비를 부착할 수 없습니다. 먼저 잠금을 해제하세요."); return false; }
      if (items.some((item) => ["DEVICE", "SENSOR"].includes(item.itemType) && Number(item.deviceId) === deviceId)) {
        options.onHint?.("이 장비는 이미 다른 위치에 부착되어 있습니다."); return false;
      }
      const parentKey = findItemKey(host);
      const siblings = items.filter((item) => item.itemType === "DEVICE" && (item.parentId != null
        ? host.id != null && Number(item.parentId) === Number(host.id)
        : item.parentClientKey === parentKey));
      const x = (siblings.length - (siblings.length ? (siblings.length - 1) / 2 : 0)) * .24;
      const item = { id: null, clientKey: makeKey(), parentId: host.id ?? null,
        parentClientKey: host.id != null ? `db-${host.id}` : host.clientKey, itemType: "DEVICE",
        deviceId, pointName: null, label: device.deviceName, modelKey: "SENSOR",
        x: THREE.MathUtils.clamp(x, -.36, .36), y: 1.05, z: .49, rotationY: 0 };
      const summary = (device.points || []).filter((point) => point.value != null).slice(0, 2)
        .map((point) => `${point.pointName} ${point.value}${point.unit ? ` ${point.unit}` : ""}`).join(" · ");
      const marker = makeSensor(item, summary || "최신값 없음"); marker.position.set(item.x, item.y, item.z);
      itemGroups.get(parentKey)?.add(marker); items.push(item); itemGroups.set(item.clientKey, marker);
      options.onChange?.(items); options.onHint?.(`${device.deviceName}을(를) ${host.label}에 부착했습니다. 저장을 눌러 반영하세요.`);
      return true;
    },
    removeItem(clientKey) {
      const target = items.find((item) => findItemKey(item) === clientKey);
      if (!target) { options.onHint?.("삭제할 배치 항목을 찾지 못했습니다. 목록을 새로 고쳐주세요."); return false; }
      if (target.locked) { options.onHint?.("잠긴 설비는 먼저 잠금을 해제하세요."); return false; }
      const targetParent = items.find((item) => target.parentId != null ? item.id === target.parentId
        : target.parentClientKey && findItemKey(item) === target.parentClientKey);
      if (targetParent?.locked) { options.onHint?.("부착된 설비가 잠겨 있습니다. 먼저 잠금을 해제하세요."); return false; }
      if ((target.modelKey === "FLOOR" || isWallModel(target.modelKey) || isCeilingModel(target.modelKey)) && items.some((item) => item.locked)) {
        const originalItems = items; items = items.filter((item) => item !== target);
        const problem = areaChangeProblem(); items = originalItems;
        if (problem) { options.onHint?.(`잠긴 설비를 보호하기 위해 삭제할 수 없습니다. ${problem}`); return false; }
      }
      if (target.modelKey === "FLOOR") {
        rememberVacantFloorSlot(target.x, target.z, target.sizeX, target.sizeZ);
      }
      const removed = new Set([target]);
      const openingParent = (isDoorModel(target.modelKey) || isWindowModel(target.modelKey))
        ? items.find((item) => item.id != null && Number(item.id) === Number(target.parentId)
          || item.id == null && item.clientKey === target.parentClientKey) : null;
      detachSceneItems(removed);
      refreshCeilingSurfaces();
      const prunedCount = pruneUnsupportedStructures();
      refreshCeilingSurfaces();
      refreshAllWallSurfaces();
      if (openingParent) refreshWallSurface(openingParent);
      updateSelectionBox();
      options.onChange?.(items);
      options.onHint?.(`${target.label}을(를) 도면에서 제거했습니다.${target.locationNodeCode ? " 실제 위치 정보는 삭제되지 않습니다." : ""}${prunedCount ? ` 지지 벽이 부족한 천장/상부 설비 ${prunedCount}개도 함께 정리했습니다.` : ""} 저장을 눌러 반영하세요.`);
      return true;
    },
    applyTemplate(templateKey) {
      if (items.some((item) => item.locked)) { options.onHint?.("잠긴 설비가 있어 템플릿을 적용할 수 없습니다. 먼저 잠금을 해제하세요."); return false; }
      const presets = {
        SERVER_ROOM: {
          floor: { xs: [-6, -2, 2, 6], zs: [-4, 0, 4] }, wall: "WALL_PANEL",
          racks: [-4, 0, 4].flatMap((x) => [-2, 2].map((z) => ({ x, z }))),
          equipment: [{ modelKey: "CRAC", label: "항온항습기", x: -6.4, z: 0 }, { modelKey: "UPS", label: "UPS", x: 6.4, z: 0 }],
        },
        OFFICE: {
          floor: { xs: [-6, -2, 2, 6], zs: [-4, 0, 4] }, wall: "WALL_PANEL",
          partitions: [{ x: -6, z: 0, rotationY: 0 }, { x: -2, z: 0, rotationY: 0 }, { x: 2, z: 0, rotationY: 0 }, { x: 6, z: 0, rotationY: 0 }],
          tables: [{ x: -4.8, z: -2.5 }, { x: 4.8, z: -2.5 }, { x: -4.8, z: 2.5 }, { x: 4.8, z: 2.5 }],
          windows: [{ wall: "north", x: -2 }, { wall: "north", x: 2 }],
        },
        FACTORY: {
          floor: { xs: [-8, -4, 0, 4, 8], zs: [-4, 0, 4] }, wall: "WALL_CONCRETE",
          equipment: [
            { modelKey: "CONVEYOR", label: "컨베이어 A", x: -3.2, z: 0 }, { modelKey: "CONVEYOR", label: "컨베이어 B", x: 3.2, z: 0 },
            { modelKey: "MACHINE", label: "생산 설비 A", x: -7, z: -3 }, { modelKey: "MACHINE", label: "생산 설비 B", x: 7, z: -3 },
            { modelKey: "TANK", label: "저장 탱크", x: -7, z: 3 }, { modelKey: "TABLE", label: "작업대", x: 7, z: 3 },
          ],
        },
        EMPTY: { floor: { xs: [], zs: [] } },
      };
      const preset = presets[templateKey];
      if (!preset) return false;
      if (placement) cancelPlacement();
      detachSceneItems(new Set(items));
      vacantFloorSlots.length = 0;
      selectedItem = null; hoveredItem = null; updateSelectionBox();
      const labels = { FLOOR: "바닥", WALL_PANEL: "외벽", WALL_CONCRETE: "외벽", WALL_GLASS: "유리 칸막이", DOOR: "출입문", WINDOW: "창문", RACK: "랙" };
      const addPresetItem = ({ itemType = "EQUIPMENT", modelKey, label, x = 0, z = 0, rotationY = 0, parent = null }) => {
        const item = { id: null, clientKey: makeKey(), parentId: null, parentClientKey: parent?.clientKey || null,
          itemType, deviceId: null, locationNodeCode: null, pointName: null,
          label: label || `${labels[modelKey] || modelKey} ${items.filter((entry) => entry.modelKey === modelKey).length + 1}`,
          modelKey, x, y: 0, z, rotationY };
        const group = itemType === "RACK" ? makeRack(item) : makeEquipment(item);
        group.position.set(x, 0, z); applyItemRotation(item, group);
        if (parent) {
          const wallSegment = itemGroups.get(findItemKey(parent))?.userData.wallSegment;
          if (!wallSegment) return null;
          group.position.set(0, 0, 0); wallSegment.add(group);
        } else scene.add(group);
        items.push(item); itemGroups.set(findItemKey(item), group);
        return item;
      };
      const xs = preset.floor.xs, zs = preset.floor.zs;
      for (const z of zs) for (const x of xs) addPresetItem({ modelKey: "FLOOR", x, z });
      if (xs.length && zs.length && preset.wall) {
        const minX = Math.min(...xs) - 2, maxX = Math.max(...xs) + 2;
        const minZ = Math.min(...zs) - 2, maxZ = Math.max(...zs) + 2;
        const wallKeys = { north: [], south: [], west: [], east: [] };
        for (const x of xs) {
          wallKeys.north.push(addPresetItem({ modelKey: preset.wall, x, z: minZ }));
          wallKeys.south.push(addPresetItem({ modelKey: preset.wall, x, z: maxZ }));
        }
        for (const z of zs) {
          wallKeys.west.push(addPresetItem({ modelKey: preset.wall, x: minX, z, rotationY: 90 }));
          wallKeys.east.push(addPresetItem({ modelKey: preset.wall, x: maxX, z, rotationY: 90 }));
        }
        for (const segment of preset.partitions || []) addPresetItem({ modelKey: "WALL_GLASS", label: "유리 칸막이", ...segment });
        const entryWall = wallKeys.south[Math.floor(wallKeys.south.length / 2)];
        if (entryWall) addPresetItem({ modelKey: "DOOR", label: "출입문", parent: entryWall });
        for (const window of preset.windows || []) {
          const wall = window.wall === "north" ? wallKeys.north.find((entry) => Math.abs(entry.x - window.x) < .1) : null;
          if (wall) addPresetItem({ modelKey: "WINDOW", label: "창문", parent: wall });
        }
      }
      for (const rack of preset.racks || []) addPresetItem({ itemType: "RACK", modelKey: "RACK", label: `랙 ${items.filter((entry) => entry.itemType === "RACK").length + 1}`, ...rack });
      for (const equipment of preset.equipment || []) addPresetItem(equipment);
      for (const table of preset.tables || []) addPresetItem({ modelKey: "TABLE", label: "업무용 책상", ...table });
      for (const wall of items.filter((entry) => isWallModel(entry.modelKey))) refreshWallSurface(wall);
      options.onChange?.(items);
      options.onHint?.(`${{ SERVER_ROOM: "서버실", OFFICE: "사무실", FACTORY: "공장", EMPTY: "빈 공간" }[templateKey]} 템플릿을 적용했습니다. 실제 장비·위치 데이터는 변경되지 않습니다. 저장을 눌러 반영하세요.`);
      return true;
    },
    clearUserPlacedItems() {
      if (items.some((item) => item.locked)) { options.onHint?.("잠긴 설비가 있어 전체 삭제할 수 없습니다. 먼저 잠금을 해제하세요."); return 0; }
      if (placement) cancelPlacement();
      const count = items.length;
      if (!count) {
        options.onHint?.("도면에 삭제할 항목이 없습니다.");
        return 0;
      }
      detachSceneItems(new Set(items));
      vacantFloorSlots.length = 0;
      selectedItem = null; hoveredItem = null;
      updateSelectionBox();
      options.onChange?.(items);
      options.onHint?.(`도면 항목 ${count}개를 모두 지웠습니다. 실제 위치 정보와 장비 데이터는 삭제되지 않습니다. 반영하려면 저장을 누르세요.`);
      return count;
    },
    setWalking,
    getItems() { return structuredClone(items); },
    dispose() { destroyed = true; clearTimeout(viewSaveTimer); saveViewState(); cancelPlacement(); cancelAnimationFrame(raf); ro.disconnect(); controls.removeEventListener("change", queueViewStateSave); controls.removeEventListener("end", saveViewState); controls.dispose(); window.removeEventListener("keydown", onKeyDown); window.removeEventListener("keyup", onKeyUp); window.removeEventListener("blur", clearWalkKeys); renderer.domElement.removeEventListener("dblclick", onRackDoubleClick); renderer.domElement.removeEventListener("pointermove", onHover); renderer.domElement.removeEventListener("pointerdown", onDown); renderer.domElement.removeEventListener("pointerdown", onRightClickResetView, true); renderer.domElement.removeEventListener("pointermove", onMove); renderer.domElement.removeEventListener("pointerup", onUp); renderer.domElement.removeEventListener("pointercancel", onUp); renderer.domElement.removeEventListener("contextmenu", preventCanvasContextMenu); selectionBox?.geometry?.dispose(); selectionBox?.material?.dispose(); guideX.geometry.dispose(); guideZ.geometry.dispose(); guideMaterial.dispose(); overlapLines.geometry.dispose(); overlapLines.material.dispose(); renderer.dispose(); container.replaceChildren(); },
  };
  views.set(container, api); return api;
}

function mountRackDetail(container, layout, models = [], onUnitSelected = () => {}, options = {}) {
  const renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 2)); renderer.setClearColor(0x0b1724);
  renderer.outputColorSpace = THREE.SRGBColorSpace; container.replaceChildren(renderer.domElement);
  const scene = new THREE.Scene(), camera = new THREE.PerspectiveCamera(38, 1, .01, 100);
  camera.position.set(0, 1.15, 5.3);
  scene.add(new THREE.HemisphereLight(0xdcefff, 0x1b283a, 2));
  const light = new THREE.DirectionalLight(0xffffff, 2.5); light.position.set(3, 6, 5); scene.add(light);
  const controls = new OrbitControls(camera, renderer.domElement); controls.target.set(0, 1.15, 0); controls.enableDamping = true;
  const frame = new THREE.MeshStandardMaterial({ color: 0x24465d, metalness: .65, roughness: .4 });
  const box = (w, h, d, x, y, z, material = frame, parent = scene) => {
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), material); mesh.position.set(x, y, z); parent.add(mesh); return mesh;
  };
  const fallbackRack = new THREE.Group(); scene.add(fallbackRack);
  for (const x of [-.48, .48]) for (const z of [-.46, .46]) box(.065, 2.35, .065, x, 1.175, z, frame, fallbackRack);
  for (const y of [.03, 2.32]) { box(1, .06, 1, 0, y, 0, frame, fallbackRack); }
  box(.94, 2.25, .035, 0, 1.17, -.48, new THREE.MeshStandardMaterial({ color: 0x142734 }), fallbackRack);
  const hinge = new THREE.Group(); hinge.position.set(.5, 0, .49); scene.add(hinge);
  const glass = new THREE.MeshPhysicalMaterial({ color: 0x83c4d4, transparent: true, opacity: .18, depthWrite: false, side: THREE.DoubleSide });
  box(.94, 2.22, .025, -.49, 1.17, 0, glass, hinge);
  for (const x of [-.96, -.02]) box(.045, 2.3, .05, x, 1.17, 0, frame, hinge);
  for (const y of [.04, 2.3]) box(.96, .045, .05, -.49, y, 0, frame, hinge);
  const openDoorAngle = 1.95;
  let doorOpen = true, actualDoorHinge = null, actualDoorHinges = [], doorAngle = openDoorAngle, doorLastFrame = null; hinge.rotation.y = doorAngle;
  const doorAttachments = new THREE.Group(); scene.add(doorAttachments);
  updateDoorAttachmentTransform(doorAttachments, hinge.position, doorAngle);
  const rackModelKey = options.modelKey && options.modelKey !== "RACK" ? options.modelKey : "SR_RACK";
  const rackModel = models.find((model) => model.key === rackModelKey);
  let disposed = false;
  if (rackModel) {
    loadShowroomModel(rackModel).then((gltf) => {
      if (disposed) return;
      const object = prepareShowroomObject(rackModel, gltf.scene.clone(true));
      object.traverse((node) => { node.userData.rackSharedModel = true; });
      if (options.rackColor) applyRackColor(object, options.rackColor);
      const bounds = new THREE.Box3().setFromObject(object);
      if (bounds.isEmpty()) throw new Error("모델에 형상이 없습니다.");
      const size = bounds.getSize(new THREE.Vector3()), center = bounds.getCenter(new THREE.Vector3());
      const wrapper = new THREE.Group(); wrapper.add(object);
      wrapper.scale.set(1 / Math.max(size.x, .001), 2.35 / Math.max(size.y, .001), 1 / Math.max(size.z, .001));
      object.position.set(-center.x, -bounds.min.y, -center.z);
      scene.add(wrapper); scene.updateMatrixWorld(true);
      actualDoorHinges = attachRackDoorPivots(object, scene);
      if (actualDoorHinges.length) {
        actualDoorHinge = (actualDoorHinges.find((door) => door.direction === 1) || actualDoorHinges[0]).pivot;
        for (const door of actualDoorHinges) door.pivot.rotation.y = door.direction * doorAngle;
        hinge.visible = false;
      }
      if (!actualDoorHinge) options.onError?.("이 랙 모델에는 분리된 전면 문이 없어 기본 문을 표시합니다.");
      fallbackRack.visible = false;
    }).catch((error) => { if (!disposed) options.onError?.(`랙 모델을 불러오지 못해 기본 랙을 표시합니다: ${error.message}`); });
  } else options.onError?.("선택한 랙 모델을 찾지 못해 기본 랙을 표시합니다.");
  const configuredCapacity = Number(layout.rackUCapacity) || 0;
  const capacity = Math.max(1, configuredCapacity), top = 2.26, bottom = .08, unitHeight = (top - bottom) / capacity;
  const hitAreas = [], deviceMaterial = new THREE.MeshStandardMaterial({ color: 0x182e3e, metalness: .4, roughness: .45 });
  for (let u = 1; u <= configuredCapacity; u++) {
    const y = bottom + (u - .5) * unitHeight;
    const slot = box(.78, unitHeight, .01, 0, y, .45, new THREE.MeshBasicMaterial({ transparent: true, opacity: 0, depthWrite: false }));
    slot.userData.u = u; hitAreas.push(slot);
    for (const x of [-.46, .46]) {
      const label = rackUnitSprite(u);
      label.position.set(x, y, .52);
      label.scale.set(.075, Math.min(.04, unitHeight * .85), 1);
      label.renderOrder = 10;
      scene.add(label);
    }
  }
  const selectablePlacements = [];
  const selectedBounds = new THREE.Box3Helper(new THREE.Box3(), 0x39e4ff);
  selectedBounds.visible = false; selectedBounds.material.depthTest = false; selectedBounds.renderOrder = 30; scene.add(selectedBounds);
  let selectedPlacement = null;
  for (const item of layout.placements || []) {
    const isU = item.mountType === "RACK_U" && item.uPosition && item.uHeight;
    if (isU && !configuredCapacity) continue;
    if (!isU && !["RACK_SIDE", "RACK_REAR"].includes(item.mountType)) continue;
    const custom = [item.positionX, item.positionY, item.positionZ].every((v) => v != null && Number.isFinite(Number(v)));
    const verticalPdu = item.sceneModelKey === "SR_PDU_VERTICAL" && item.mountType === "RACK_SIDE";
    const sensor = ["SR_DRAGINO_TH", "SR_DRAGINO_DOOR"].includes(item.sceneModelKey) && !isU;
    const width = isU ? .77 : sensor ? (item.sceneModelKey === "SR_DRAGINO_DOOR" ? .13 : .068) : custom && !verticalPdu ? .10 : .07;
    const height = isU ? unitHeight * Math.min(capacity, Number(item.uHeight)) * .92 : sensor ? (item.sceneModelKey === "SR_DRAGINO_DOOR" ? .10 : .12) : verticalPdu ? 2.3 * Math.max(.1, Math.min(1, Number(item.visualLengthRatio ?? (1.8 / 2.3)))) : custom ? .10 : 1.8;
    const depth = isU ? .68 : sensor ? .03 : custom && !verticalPdu ? .10 : .07;
    const x = isU ? 0 : custom ? -.48 + .96 * Number(item.positionX) : item.mountType === "RACK_REAR" ? 0 : item.rackSide === "LEFT" ? -.45 : .45;
    const y = isU ? bottom + (Number(item.uPosition) - 1) * unitHeight + height / 2 : custom ? 2.3 * Number(item.positionY) : 1.18;
    const z = isU ? 0 : custom ? -.48 + .96 * Number(item.positionZ) : item.mountType === "RACK_REAR" ? -.43 : item.sceneModelKey === "SR_PDU_VERTICAL" ? .4 : .25;
    const splitDoorSensor = sensor && item.sceneModelKey === "SR_DRAGINO_DOOR";
    const holder = new THREE.Group(); holder.position.set(x, y, z); if (sensor) holder.rotation.y = item.mountType === "RACK_REAR" ? (z >= 0 ? 0 : Math.PI) : item.rackSide === "LEFT" ? -Math.PI / 2 : Math.PI / 2; (isDoorAttachment(item) && !splitDoorSensor ? doorAttachments : scene).add(holder);
    const movingHolder = splitDoorSensor ? new THREE.Group() : null;
    if (movingHolder) { movingHolder.position.copy(holder.position); movingHolder.rotation.copy(holder.rotation); movingHolder.visible = isDoorAttachment(item); doorAttachments.add(movingHolder); }
    const placeholder = box(width, height, depth, 0, 0, 0, deviceMaterial, holder);
    const entry = { item, holder, movingHolder, sensorParts:null, isU, verticalPdu, sensor, splitDoorSensor, height, baseHeight:height };
    if (options.onSelectPlacement) selectablePlacements.push(entry);
    const model = models.find((entry) => entry.key === item.sceneModelKey);
    if (!model) continue;
    loadShowroomModel(model).then((gltf) => {
      if (disposed) return;
      const object = prepareShowroomObject(model, gltf.scene.clone(true));
      object.traverse((node) => { node.userData.rackSharedModel = true; });
      const bounds = new THREE.Box3().setFromObject(object); if (bounds.isEmpty()) return;
      const size = bounds.getSize(new THREE.Vector3()), center = bounds.getCenter(new THREE.Vector3());
      const wrapper = new THREE.Group(); wrapper.add(object);
      wrapper.scale.set(width / Math.max(size.x, .001), height / Math.max(size.y, .001), depth / Math.max(size.z, .001));
      object.position.sub(center); holder.add(wrapper); holder.remove(placeholder); placeholder.geometry.dispose();
      if (splitDoorSensor) {
        entry.sensorParts = prepareDoorSensorParts(object);
        if (entry.sensorParts) {
          const movingWrapper = wrapper.clone(true);
          movingWrapper.traverse((node) => { if (node.isMesh) node.visible = !!node.userData.doorSensorMovingPart; });
          movingHolder.add(movingWrapper);
          setDoorSensorPartsAttached(entry.sensorParts, isDoorAttachment(entry.item));
        }
      }
    }).catch((error) => console.warn("랙 장착 모델 로딩 실패", error));
  }
  const raycaster = new THREE.Raycaster(), pointer = new THREE.Vector2();
  const samePlacement = (a, b) => a && b && (a.modelPlacementId ? a.modelPlacementId === b.modelPlacementId : !b.modelPlacementId && a.deviceId === b.deviceId);
  function selectPlacement(entry) {
    selectedPlacement = entry; selectedBounds.visible = !!entry;
    if (entry) selectedBounds.box.setFromObject(entry.holder);
    options.onSelectPlacement?.(entry?.item || null);
  }
  function applyPlacement(item, commit = true) {
    const entry = selectablePlacements.find((candidate) => samePlacement(candidate.item, item));
    if (!entry) return;
    const custom = [item.positionX, item.positionY, item.positionZ].every((v) => v != null && Number.isFinite(Number(v)));
    const height = entry.isU ? unitHeight * Math.min(capacity, Number(item.uHeight)) * .92 : entry.verticalPdu ? 2.3 * Math.max(.1, Math.min(1, Number(item.visualLengthRatio ?? (1.8 / 2.3)))) : entry.baseHeight;
    entry.holder.scale.y = height / entry.baseHeight;
    entry.holder.position.set(entry.isU ? 0 : custom ? -.48 + .96 * Number(item.positionX) : item.mountType === "RACK_REAR" ? 0 : item.rackSide === "LEFT" ? -.45 : .45,
      entry.isU ? bottom + (Number(item.uPosition) - 1) * unitHeight + height / 2 : custom ? 2.3 * Number(item.positionY) : 1.18,
      entry.isU ? 0 : custom ? -.48 + .96 * Number(item.positionZ) : item.mountType === "RACK_REAR" ? -.43 : entry.verticalPdu ? .4 : .25);
    const targetParent = isDoorAttachment(item) && !entry.splitDoorSensor ? doorAttachments : scene;
    if (entry.holder.parent !== targetParent) targetParent.add(entry.holder);
    if (entry.sensor && item.mountType === "RACK_REAR") entry.holder.rotation.y = entry.holder.position.z >= 0 ? 0 : Math.PI;
    if (entry.movingHolder) { entry.movingHolder.position.copy(entry.holder.position); entry.movingHolder.rotation.copy(entry.holder.rotation); entry.movingHolder.visible = isDoorAttachment(item); setDoorSensorPartsAttached(entry.sensorParts, isDoorAttachment(item)); }
    if (commit) { Object.assign(entry.item, item); entry.height = height; }
    if (selectedPlacement === entry) selectedBounds.box.setFromObject(entry.holder);
  }
  let dragPdu = null;
  const rayAt = (event) => {
    const rect = renderer.domElement.getBoundingClientRect();
    pointer.set((event.clientX - rect.left) / rect.width * 2 - 1, -(event.clientY - rect.top) / rect.height * 2 + 1);
    raycaster.setFromCamera(pointer, camera);
  };
  const onPduDown = (event) => {
    if (event.button !== 0 || !selectablePlacements.length) return;
    rayAt(event);
    const hit = raycaster.intersectObjects(selectablePlacements.flatMap((entry) => [entry.holder, ...(entry.movingHolder?.visible ? [entry.movingHolder] : [])]), true)[0];
    if (!hit) { if (selectedPlacement) selectPlacement(null); return; }
    const entry = selectablePlacements.find((candidate) => { for (let node = hit.object; node; node = node.parent) if (node === candidate.holder || node === candidate.movingHolder) return true; return false; });
    if (!entry) return;
    selectPlacement(entry);
    if (!options.onPduMove || (!entry.isU && !entry.verticalPdu && !entry.sensor)) return;
    let hitMoving = false; for (let node = hit.object; node; node = node.parent) if (node === entry.movingHolder) { hitMoving = true; break; }
    const activeParent = hitMoving ? doorAttachments : entry.holder.parent;
    const plane = entry.isU ? new THREE.Plane(new THREE.Vector3(0, 0, 1), -.45) : entry.item.mountType === "RACK_REAR" ? new THREE.Plane(new THREE.Vector3(0, 0, 1), -entry.holder.position.z) : new THREE.Plane(new THREE.Vector3(1, 0, 0), -entry.holder.position.x);
    const localRay = raycaster.ray.clone().applyMatrix4(new THREE.Matrix4().copy(activeParent.matrixWorld).invert());
    const point = new THREE.Vector3(); if (!localRay.intersectPlane(plane, point)) return;
    dragPdu = { entry, plane, activeParent, point: point.clone(), original: entry.holder.position.clone(), moved: false };
    controls.enabled = false; renderer.domElement.setPointerCapture(event.pointerId); event.stopImmediatePropagation(); event.preventDefault();
  };
  const onPduMove = (event) => {
    if (!dragPdu) return;
    rayAt(event); const localRay = raycaster.ray.clone().applyMatrix4(new THREE.Matrix4().copy(dragPdu.activeParent.matrixWorld).invert());
    const point = new THREE.Vector3(); if (!localRay.intersectPlane(dragPdu.plane, point)) return;
    const { entry, original } = dragPdu, delta = point.sub(dragPdu.point);
    if (entry.isU) {
      const start = THREE.MathUtils.clamp(Math.round((original.y + delta.y - bottom - entry.height / 2) / unitHeight) + 1, 1, capacity - Number(entry.item.uHeight) + 1);
      entry.holder.position.y = bottom + (start - 1) * unitHeight + entry.height / 2; dragPdu.uPosition = start;
    } else {
      entry.holder.position.y = THREE.MathUtils.clamp(original.y + delta.y, entry.height / 2, 2.3 - entry.height / 2);
      if (entry.item.mountType === "RACK_REAR") entry.holder.position.x = THREE.MathUtils.clamp(original.x + delta.x, -.48, .48);
      else entry.holder.position.z = THREE.MathUtils.clamp(original.z + delta.z, -.48, .48);
    }
    if (entry.movingHolder) entry.movingHolder.position.copy(entry.holder.position);
    dragPdu.moved ||= entry.holder.position.distanceTo(original) > .001;
    event.stopImmediatePropagation(); event.preventDefault();
  };
  const onPduUp = async (event) => {
    if (!dragPdu) return;
    const drag = dragPdu; dragPdu = null; controls.enabled = true;
    if (renderer.domElement.hasPointerCapture(event.pointerId)) renderer.domElement.releasePointerCapture(event.pointerId);
    event.stopImmediatePropagation(); event.preventDefault();
    if (!drag.moved || event.type === "pointercancel") { drag.entry.holder.position.copy(drag.original); if (drag.entry.movingHolder) drag.entry.movingHolder.position.copy(drag.original); return; }
    const pos = drag.entry.holder.position;
    const position = drag.entry.isU ? { uPosition: drag.uPosition } : { positionX: (pos.x + .48) / .96, positionY: pos.y / 2.3, positionZ: (pos.z + .48) / .96 };
    try { await options.onPduMove(drag.entry.item, position); }
    catch (error) { if (!disposed) { drag.entry.holder.position.copy(drag.original); if (drag.entry.movingHolder) drag.entry.movingHolder.position.copy(drag.original); } options.onError?.(error.message || "PDU 위치를 저장하지 못했습니다."); }
  };
  renderer.domElement.addEventListener("pointerdown", onPduDown, true); renderer.domElement.addEventListener("pointermove", onPduMove, true);
  renderer.domElement.addEventListener("pointerup", onPduUp, true); renderer.domElement.addEventListener("pointercancel", onPduUp, true);
  const click = (event) => {
    const rect = renderer.domElement.getBoundingClientRect(); pointer.set((event.clientX - rect.left) / rect.width * 2 - 1, -(event.clientY - rect.top) / rect.height * 2 + 1);
    raycaster.setFromCamera(pointer, camera); const hit = raycaster.intersectObjects(hitAreas)[0]; if (hit) onUnitSelected(hit.object.userData.u);
  };
  renderer.domElement.addEventListener("dblclick", click);
  const resize = () => { const r = container.getBoundingClientRect(); if (r.width && r.height) { renderer.setSize(r.width, r.height, false); camera.aspect = r.width / r.height; camera.updateProjectionMatrix(); } };
  const observer = new ResizeObserver(resize); observer.observe(container); resize();
  let raf; const tick = (now = performance.now()) => {
    if (disposed) return;
    const dt = doorLastFrame == null ? 0 : Math.min(.05, Math.max(0, (now - doorLastFrame) / 1000));
    doorLastFrame = now;
    const target = doorOpen ? openDoorAngle : 0;
    doorAngle += (target - doorAngle) * (1 - Math.exp(-7 * dt));
    if (Math.abs(target - doorAngle) < .001) doorAngle = target;
    hinge.rotation.y = doorAngle; for (const door of actualDoorHinges) door.pivot.rotation.y = door.direction * doorAngle;
    updateDoorAttachmentTransform(doorAttachments, actualDoorHinge?.position || hinge.position, doorAngle);
    if (selectedPlacement) selectedBounds.box.setFromObject(selectedPlacement.holder);
    controls.update(); renderer.render(scene, camera); raf = requestAnimationFrame(tick);
  }; tick();
  return { setDoorState(open) { if (typeof open === "boolean") doorOpen = open; }, toggleDoor() { doorOpen = !doorOpen; return doorOpen; }, applyPlacement(item) { applyPlacement(item); }, previewPlacement(item) { applyPlacement(item, false); }, selectPlacement(item) { const entry = selectablePlacements.find((candidate) => samePlacement(candidate.item, item)); if (!entry) return false; selectPlacement(entry); return true; }, clearSelection() { selectPlacement(null); },
    dispose() { disposed = true; cancelAnimationFrame(raf); observer.disconnect(); controls.dispose(); renderer.domElement.removeEventListener("dblclick", click); renderer.domElement.removeEventListener("pointerdown", onPduDown, true); renderer.domElement.removeEventListener("pointermove", onPduMove, true); renderer.domElement.removeEventListener("pointerup", onPduUp, true); renderer.domElement.removeEventListener("pointercancel", onPduUp, true); scene.traverse((node) => { if (node.userData.rackSharedModel) return; node.geometry?.dispose(); if (node.material && node.material !== deviceMaterial) { node.material.map?.dispose(); node.material.dispose(); } }); deviceMaterial.dispose(); renderer.dispose(); container.replaceChildren(); } };
}

function mountPanelDetail(container) {
  const renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 2)); renderer.setClearColor(0x0b1724);
  renderer.outputColorSpace = THREE.SRGBColorSpace; container.replaceChildren(renderer.domElement);
  const scene = new THREE.Scene(), camera = new THREE.PerspectiveCamera(38, 1, .01, 100);
  camera.position.set(2.9, 2.2, 4.4);
  scene.add(new THREE.HemisphereLight(0xe0efff, 0x1f2c3a, 2.2));
  const light = new THREE.DirectionalLight(0xffffff, 2.4); light.position.set(3, 6, 5); scene.add(light);
  const controls = new OrbitControls(camera, renderer.domElement); controls.target.set(0, 1.1, 0); controls.enableDamping = true;
  const materials = [0x202b30,0x101820,0xd0d2c7,0x687575,0xb57047,0x4265a4,0x5dbb6f,0x7ca6ad]
    .map((color, index) => new THREE.MeshStandardMaterial({ color, metalness: index === 4 ? .65 : .2, roughness: .57 }));
  const add = (w,h,d,x,y,z,mat=0,parent=scene) => { const mesh = new THREE.Mesh(new THREE.BoxGeometry(w,h,d),materials[mat]); mesh.position.set(x,y,z); parent.add(mesh); return mesh; };
  add(1.2,2.2,.12,0,1.1,-.3,1); add(.07,2.2,.65,-.56,1.1,0); add(.07,2.2,.65,.56,1.1,0);
  add(1.18,.07,.65,0,2.17,0); add(1.18,.07,.65,0,.035,0);
  // Main breaker, meter, busbars and two rows of circuit breakers are illustrative.
  add(.37,.31,.13,-.20,1.53,-.16,2); add(.09,.16,.06,-.20,1.53,-.065,3);
  add(.36,.23,.12,.28,1.83,-.16,0); add(.28,.15,.02,.28,1.84,-.09,7);
  for (let i=0;i<4;i++) { const x=-.40+i*.12; add(.065,.29,.04,x,1.94,-.19,4); add(.035,.08,.035,x,1.95,-.15,3); }
  for (let i=0;i<4;i++) { const x=-.40+i*.12; add(.06,.30,.04,x,1.14,-.19,4); }
  for (let side=0;side<2;side++) for (let i=0;i<5;i++) {
    const x=side ? .25 : -.25, y=.23+i*.17;
    add(.34,.14,.12,x,y,-.15,2); add(.10,.06,.05,x,y,-.065,3);
    add(.025,.015,.015,x+.03,y,-.035,i%2 ? 6 : 4);
  }
  const cable = (points,color) => { const curve=new THREE.CatmullRomCurve3(points.map((p)=>new THREE.Vector3(...p))); const mesh=new THREE.Mesh(new THREE.TubeGeometry(curve,24,.012,8,false),materials[color]); scene.add(mesh); };
  for (let i=0;i<4;i++) cable([[-.38+i*.12,2.14,-.21],[-.38+i*.12,2.02,-.16],[-.38+i*.12,1.84,-.15]], i===3 ? 5 : 3);
  for (let i=0;i<5;i++) cable([[-.43,.23+i*.17,-.1],[-.52,.23+i*.17,.02],[-.53,.18+i*.17,.23]], i%2 ? 7 : 4);
  const hinge = new THREE.Group(); hinge.position.set(.59,0,.33); scene.add(hinge);
  add(1.15,2.16,.07,-.58,1.1,0,0,hinge); add(.025,.20,.04,-1.06,1.10,.055,3,hinge);
  let open=true, disposed=false; hinge.rotation.y=-1.32;
  const resize=()=>{const r=container.getBoundingClientRect();if(r.width&&r.height){renderer.setSize(r.width,r.height,false);camera.aspect=r.width/r.height;camera.updateProjectionMatrix();}};
  const observer=new ResizeObserver(resize); observer.observe(container); resize();
  let raf; const animate=()=>{if(disposed)return;controls.update();renderer.render(scene,camera);raf=requestAnimationFrame(animate);}; animate();
  return { toggleDoor(){open=!open;hinge.rotation.y=open?-1.32:0;return open;}, dispose(){disposed=true;cancelAnimationFrame(raf);observer.disconnect();controls.dispose();scene.traverse((node)=>node.geometry?.dispose());materials.forEach((material)=>material.dispose());renderer.dispose();container.replaceChildren();} };
}

window.DcimRoomScene3D = { mount, mountRackDetail, mountPanelDetail, renderModelThumbnails, renderBuiltinThumbnails,
  setBuiltinAssets(assets) {
    builtinAssetLabels.clear();
    for (const asset of assets || []) builtinAssetLabels.set(asset.key, asset.label);
  },
};
window.dispatchEvent(new Event("dcim:scene3d-ready"));

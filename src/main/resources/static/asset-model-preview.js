import * as THREE from "/vendor/three/three.module.js?v=20261022d";
import { OrbitControls } from "/vendor/three/controls/OrbitControls.js";
import { GLTFLoader } from "/vendor/three/loaders/GLTFLoader.js";

const previews = new WeakMap();

function parseObj(source) {
  const vertices = [];
  const triangles = [];
  for (const line of source.split(/\r?\n/)) {
    const parts = line.trim().split(/\s+/);
    if (parts[0] === "v" && parts.length >= 4) {
      vertices.push([Number(parts[1]), Number(parts[2]), Number(parts[3])]);
    } else if (parts[0] === "f" && parts.length >= 4) {
      const face = parts.slice(1).map((part) => {
        const index = Number(part.split("/")[0]);
        return index > 0 ? index - 1 : vertices.length + index;
      });
      for (let i = 1; i < face.length - 1; i++) triangles.push([face[0], face[i], face[i + 1]]);
      if (triangles.length > 250000) throw new Error("OBJ 면이 너무 많아 미리보기할 수 없습니다.");
    }
  }
  if (!vertices.length || !triangles.length) throw new Error("OBJ 파일에서 정점/면을 찾지 못했습니다.");
  const positions = [];
  for (const triangle of triangles) for (const index of triangle) {
    const vertex = vertices[index];
    if (!vertex || vertex.some((value) => !Number.isFinite(value))) throw new Error("OBJ 면 인덱스가 올바르지 않습니다.");
    positions.push(...vertex);
  }
  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute("position", new THREE.Float32BufferAttribute(positions, 3));
  geometry.computeVertexNormals();
  geometry.computeBoundingBox();
  const center = geometry.boundingBox.getCenter(new THREE.Vector3());
  geometry.translate(-center.x, -center.y, -center.z);
  return geometry;
}

export function mount(container, sourceUrl, originalName = "") {
  previews.get(container)?.dispose();
  container.replaceChildren();
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
  renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 2));
  renderer.setClearColor(0x101b29, 1);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  container.appendChild(renderer.domElement);
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x101b29);
  const camera = new THREE.PerspectiveCamera(38, 1, .01, 10000);
  camera.position.set(3.2, 2.6, 4.2);
  scene.add(new THREE.HemisphereLight(0xe7f5ff, 0x263443, 2.1));
  const light = new THREE.DirectionalLight(0xffffff, 3);
  light.position.set(4, 8, 6); scene.add(light);
  const controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  let frame = 0, disposed = false;
  let geometry = null, material = null, modelRoot = null;
  const resize = () => {
    const rect = container.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    renderer.setSize(rect.width, rect.height, false);
    camera.aspect = rect.width / rect.height;
    camera.updateProjectionMatrix();
  };
  const observer = new ResizeObserver(resize); observer.observe(container); resize();
  fetch(sourceUrl).then(async (response) => {
    if (!response.ok) throw new Error("3D 파일을 불러오지 못했습니다.");
    if (/\.glb$/i.test(originalName)) return (await new GLTFLoader().parseAsync(await response.arrayBuffer(), "")).scene;
    const source = await response.text();
    geometry = parseObj(source);
    material = new THREE.MeshStandardMaterial({ color: 0x42b9c7, metalness: .35, roughness: .42 });
    return new THREE.Mesh(geometry, material);
  }).then((object) => {
    if (disposed) return;
    modelRoot = object; scene.add(object);
    const bounds = new THREE.Box3().setFromObject(object);
    if (bounds.isEmpty()) throw new Error("3D 모델에 표시할 형상이 없습니다.");
    const size = bounds.getSize(new THREE.Vector3()), center = bounds.getCenter(new THREE.Vector3());
    const radius = Math.max(size.x, size.y, size.z, .2);
    camera.position.copy(center).add(new THREE.Vector3(radius * 1.9, radius * 1.4, radius * 2.3));
    camera.near = Math.max(.01, radius / 1000); camera.far = radius * 100; camera.updateProjectionMatrix();
    controls.target.copy(center); controls.minDistance = radius * .7; controls.maxDistance = radius * 12; controls.update();
  }).catch((error) => {
    if (!disposed) { const message = document.createElement("div"); message.className = "asset-model-error"; message.textContent = error.message; container.append(message); }
  });
  const animate = () => {
    if (disposed) return;
    controls.update(); renderer.render(scene, camera); frame = requestAnimationFrame(animate);
  };
  animate();
  const api = { dispose() { disposed = true; cancelAnimationFrame(frame); observer.disconnect(); controls.dispose();
    modelRoot?.traverse((node) => { node.geometry?.dispose(); for (const mat of Array.isArray(node.material) ? node.material : [node.material]) {
      if (!mat) continue; for (const value of Object.values(mat)) if (value?.isTexture) value.dispose(); mat.dispose();
    }}); renderer.dispose(); container.replaceChildren(); } };
  previews.set(container, api);
  return api;
}

"""Generate the example rack and vertical PDU models as self-contained GLB files."""
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/static/scene-assets/showroom"

# A unit cube with face normals; each component references one of these meshes.
FACES = [
    ((0, 0, 1), [(-.5,-.5,.5),(.5,-.5,.5),(.5,.5,.5),(-.5,.5,.5)]),
    ((0, 0,-1),[(.5,-.5,-.5),(-.5,-.5,-.5),(-.5,.5,-.5),(.5,.5,-.5)]),
    ((1, 0, 0),[(.5,-.5,.5),(.5,-.5,-.5),(.5,.5,-.5),(.5,.5,.5)]),
    ((-1,0, 0),[(-.5,-.5,-.5),(-.5,-.5,.5),(-.5,.5,.5),(-.5,.5,-.5)]),
    ((0, 1, 0),[(-.5,.5,.5),(.5,.5,.5),(.5,.5,-.5),(-.5,.5,-.5)]),
    ((0,-1, 0),[(-.5,-.5,-.5),(.5,-.5,-.5),(.5,-.5,.5),(-.5,-.5,.5)]),
]
PALETTE = [
    ("housing", [.025,.032,.042,1]), ("front panel", [.055,.065,.075,1]),
    ("socket surround", [.11,.12,.13,1]), ("socket opening", [.008,.012,.017,1]),
    ("metal", [.35,.38,.39,1]), ("display", [.43,.79,.88,1]),
    ("button", [.19,.21,.22,1]), ("indicator", [.15,.75,.43,1]),
    ("network port", [.47,.43,.28,1]), ("print", [.62,.67,.68,1]),
    ("server display", [.035,.23,.93,1]),
]

def export(filename, parts):
    positions, normals, indices = [], [], []
    for normal, corners in FACES:
        base = len(positions) // 3
        for corner in corners:
            positions.extend(corner); normals.extend(normal)
        indices.extend([base,base+1,base+2,base,base+2,base+3])
    pos = struct.pack('<%sf' % len(positions), *positions)
    nor = struct.pack('<%sf' % len(normals), *normals)
    ind = struct.pack('<%sH' % len(indices), *indices)
    binary = pos + nor + ind
    while len(binary) % 4: binary += b'\0'
    materials = [{"name": name, "pbrMetallicRoughness": {"baseColorFactor": color,
        "metallicFactor": .38 if name in ("housing", "metal") else .12, "roughnessFactor": .57}}
        for name, color in PALETTE]
    meshes = [{"name": name, "primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1}, "indices": 2, "material": i}]}
        for i, (name, _) in enumerate(PALETTE)]
    nodes = [{"name": name, "mesh": material, "translation": list(center), "scale": list(size)}
        for name, center, size, material in parts]
    document = {"asset": {"version": "2.0", "generator": "DCIM procedural PDU examples"},
        "scene": 0, "scenes": [{"nodes": list(range(len(nodes)))}], "nodes": nodes,
        "meshes": meshes, "materials": materials, "buffers": [{"byteLength": len(binary)}],
        "bufferViews": [{"buffer":0,"byteOffset":0,"byteLength":len(pos),"target":34962},
            {"buffer":0,"byteOffset":len(pos),"byteLength":len(nor),"target":34962},
            {"buffer":0,"byteOffset":len(pos)+len(nor),"byteLength":len(ind),"target":34963}],
        "accessors": [{"bufferView":0,"componentType":5126,"count":24,"type":"VEC3","min":[-.5,-.5,-.5],"max":[.5,.5,.5]},
            {"bufferView":1,"componentType":5126,"count":24,"type":"VEC3"},
            {"bufferView":2,"componentType":5123,"count":36,"type":"SCALAR"}]}
    raw = json.dumps(document, separators=(',', ':')).encode()
    raw += b' ' * (-len(raw) % 4)
    result = struct.pack('<4sII',b'glTF',2,12+8+len(raw)+8+len(binary))
    result += struct.pack('<I4s',len(raw),b'JSON')+raw+struct.pack('<I4s',len(binary),b'BIN\0')+binary
    ROOT.mkdir(parents=True, exist_ok=True)
    (ROOT / filename).write_bytes(result)

def add(parts, name, center, size, material): parts.append((name, center, size, material))

def horizontal():
    p=[]
    add(p,'rack PDU chassis',(0,.055,0),(.44,.105,.24),0)
    add(p,'front bezel',(0,.055,.124),(.482,.105,.009),1)
    for x in (-.23,.23):
        add(p,'rack mounting ear',(x,.055,.13),(.021,.105,.007),0)
        for y in (.018,.092): add(p,'mounting screw',(x,y,.135),(.005,.005,.003),4)
    add(p,'controller recess',(-.105,.058,.131),(.195,.072,.005),0)
    add(p,'LCD frame',(-.085,.060,.136),(.055,.052,.006),4)
    add(p,'LCD',(-.085,.060,.140),(.047,.043,.003),5)
    for x in (-.18,-.157):
        for y in (.041,.070):
            add(p,'network port rim',(x,y,.137),(.017,.021,.004),8)
            add(p,'network port',(x,y,.140),(.012,.015,.003),3)
    for x in (-.048,-.035):
        for y in (.044,.061,.078): add(p,'control button',(x,y,.138),(.006,.006,.004),6)
    for col in range(3):
        for row in range(4):
            x=.01+col*.078; y=.020+row*.023
            add(p,'outlet bezel',(x,y,.133),(.055,.019,.004),2)
            add(p,'outlet',(x-.008,y,.136),(.031,.014,.003),3)
            for dx in (-.014,-.003): add(p,'outlet contact',(x+dx,y,.138),(.003,.006,.002),4)
            add(p,'outlet number',(x+.020,y,.137),(.007,.0015,.002),9)
    return p

def vertical():
    p=[]
    add(p,'vertical PDU housing',(0,.8,0),(.058,1.6,.068),0)
    add(p,'front face',(0,.8,.036),(.054,1.58,.006),1)
    for y in (.02,1.58): add(p,'mounting tab',(0,y,-.04),(.085,.035,.008),4)
    add(p,'controller panel',(0,1.37,.041),(.045,.32,.005),0)
    for i in range(3): add(p,'status indicator',(-.014,1.43-i*.035,.045),(.004,.006,.003),7)
    for i in range(18):
        y=.10+i*.061
        add(p,'outlet surround',(0,y,.041),(.043,.045,.005),2)
        add(p,'outlet opening',(0,y,.045),(.033,.034,.004),3)
        for x in (-.008,.008):
            for yy in (-.008,.008): add(p,'outlet contact',(x,y+yy,.048),(.004,.006,.003),4)
        add(p,'outlet index',(.021,y,.047),(.002,.012,.002),9)
    for i in range(4): add(p,'network port',(0,1.20+i*.035,.043),(.025,.022,.004),3)
    return p

def gpu_server():
    p=[]
    add(p,'GPU server chassis',(0,.089,0),(.44,.172,.72),0)
    add(p,'front panel',(0,.089,.365),(.482,.174,.012),1)
    for x in (-.232,.232):
        add(p,'rack ear',(x,.089,.373),(.018,.175,.006),0)
        for y in (.025,.151): add(p,'mount screw',(x,y,.378),(.006,.007,.003),4)
    add(p,'vent surround',(-.063,.091,.374),(.30,.146,.005),3)
    for i in range(42):
        x=-.208+i*.007
        add(p,'vent fin',(x,.091,.379),(.0025,.132,.004),2)
    for i in range(18): add(p,'blue status light',(-.20+i*.016,.151,.381),(.004,.003,.002),10)
    add(p,'control panel',(.144,.09,.375),(.10,.146,.006),0)
    add(p,'LCD border',(.14,.125,.381),(.045,.025,.003),4)
    add(p,'LCD screen',(.14,.125,.384),(.039,.020,.003),10)
    for y in (.09,.074,.058): add(p,'USB port',(.124,y,.382),(.013,.005,.003),4)
    add(p,'power switch',(.16,.071,.383),(.012,.012,.004),6)
    return p

if __name__ == '__main__':
    export('pdu_rack_horizontal.glb', horizontal())
    export('pdu_rack_vertical.glb', vertical())
    export('gpu_server_rack.glb', gpu_server())

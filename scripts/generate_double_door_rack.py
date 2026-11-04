"""Generate a rack with two independent, perforated front doors."""
import json
import struct
from generate_pdu_models import ROOT, add, export

parts = []
for x in (-.435, .435):
    add(parts, 'cabinet side', (x, 1.175, 0), (.03, 2.35, .9), 1)
for y in (.025, 2.325):
    add(parts, 'cabinet top or base', (0, y, 0), (.9, .05, .9), 0)
add(parts, 'rear panel', (0, 1.175, -.44), (.86, 2.29, .025), 1)
for x in (-.37, .37):
    add(parts, 'rack rail', (x, 1.175, .24), (.035, 2.24, .035), 4)
for side, x in [('left', -.218), ('right', .218)]:
    prefix = 'door_' + side + '_'
    # The ventilation area is real open geometry: no solid panel behind it.
    # This keeps installed equipment visible even when the doors are closed.
    for index in range(49):
        add(parts, prefix+'vent_horizontal', (x, .20 + index*.04, .479), (.32, .003, .006), 2)
    for index in range(17):
        add(parts, prefix+'vent_vertical', (x - .16 + index*.02, 1.16, .479), (.003, 1.92, .006), 2)
    for y in (.12, 2.23):
        add(parts, prefix+'solid_trim', (x, y, .47), (.425, .12, .025), 1)
    for dx in (-.207, .207):
        add(parts, prefix+'frame', (x+dx, 1.175, .48), (.012, 2.25, .018), 0)
    for y in (.055, 2.295):
        add(parts, prefix+'frame', (x, y, .48), (.425, .012, .018), 0)
    add(parts, prefix+'handle', (x + (.15 if side == 'left' else -.15), 1.15, .492), (.018, .18, .018), 4)

path = ROOT / 'rack_double_door.glb'
export(path.name, parts)
raw = path.read_bytes()
length = struct.unpack_from('<I', raw, 12)[0]
doc = json.loads(raw[20:20+length])
binary = raw[20+length:]
roots = []
for side in ('left', 'right'):
    children = [i for i, node in enumerate(doc['nodes']) if node['name'].startswith('door_'+side+'_')]
    roots.append(len(doc['nodes']))
    doc['nodes'].append({'name':'front_door_'+side, 'children':children})
doc['scenes'][0]['nodes'] = [i for i, node in enumerate(doc['nodes'][:-2]) if not node['name'].startswith(('door_left_', 'door_right_'))] + roots
doc['asset']['generator'] = 'DCIM double door rack generator'
encoded = json.dumps(doc, separators=(',', ':')).encode()
encoded += b' ' * (-len(encoded) % 4)
path.write_bytes(struct.pack('<4sII', b'glTF', 2, 20 + len(encoded) + len(binary)) + struct.pack('<I4s', len(encoded), b'JSON') + encoded + binary)

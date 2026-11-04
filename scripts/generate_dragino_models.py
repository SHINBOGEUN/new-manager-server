"""Generate simplified, locally stored sensor models from reference photos."""
from generate_pdu_models import PALETTE, add, export
PALETTE.extend([('white plastic',[.86,.91,.92,1]),('blue',[.02,.38,.65,1]),('label',[.72,.81,.83,1]),('steel',[.50,.54,.55,1])])
def temperature():
 p=[]
 add(p,'white enclosure',(0,.055,0),(.068,.106,.026),11)
 add(p,'front label',(0,.057,.0135),(.056,.088,.002),13)
 add(p,'blue logo',(-.012,.083,.015),(.013,.009,.001),12)
 add(p,'sensor marking',(.008,.083,.015),(.027,.004,.001),1)
 add(p,'label text',(0,.071,.015),(.04,.003,.001),1)
 add(p,'label barcode',(0,.053,.015),(.020,.020,.001),0)
 for x in (-.016,0,.016): add(p,'indicator',(x,.032,.016),(.009,.009,.002),4)
 add(p,'blue connector',(0,-.004,0),(.016,.014,.018),12)
 return p
def door():
 p=[]
 add(p,'mounting plate',(0,.04,0),(.13,.08,.004),14)
 for x in (-.054,.054):
  for y in (.017,.063): add(p,'mounting screw',(x,y,.003),(.008,.008,.002),0)
 add(p,'wired contact',(-.011,.04,.014),(.021,.069,.023),13)
 add(p,'magnet',(.011,.04,.014),(.020,.069,.023),13)
 add(p,'contact gap',(0,.04,.027),(.002,.068,.002),0)
 add(p,'wire',(-.011,.083,.014),(.005,.020,.005),11)
 return p
if __name__ == '__main__':
 export('dragino_temperature_humidity.glb',temperature())
 export('dragino_door_contact.glb',door())

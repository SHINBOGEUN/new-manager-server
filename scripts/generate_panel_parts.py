"""Generate editable illustrative components for the default panel layout."""
from generate_pdu_models import add, export

def main_breaker():
    p=[]
    add(p,'housing',(0,.155,0),(.37,.31,.13),2)
    add(p,'switch',(0,.155,.072),(.09,.16,.03),6)
    return p

def meter():
    p=[]
    add(p,'housing',(0,.115,0),(.36,.23,.12),0)
    add(p,'display',(0,.115,.065),(.28,.15,.01),5)
    return p

def busbar():
    p=[]
    add(p,'copper bar',(0,.145,0),(.06,.29,.04),8)
    add(p,'terminal',(0,.145,.025),(.035,.08,.01),4)
    return p

def wire():
    p=[]
    add(p,'vertical lead',(-.04,.065,0),(.012,.11,.012),8)
    add(p,'horizontal lead',(0,.12,0),(.08,.012,.012),8)
    return p

if __name__ == '__main__':
    for filename,parts in [('panel_main_breaker.glb',main_breaker()),('panel_meter.glb',meter()),('panel_busbar.glb',busbar()),('panel_wire.glb',wire())]:
        export(filename,parts)

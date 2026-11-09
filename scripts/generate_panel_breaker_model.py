"""Create an illustrative molded-case circuit breaker for panel layouts."""
from generate_pdu_models import add, export


def breaker():
    p = []
    add(p, 'breaker housing', (0, .055, 0), (.095, .11, .075), 2)
    add(p, 'front cover', (0, .055, .039), (.091, .104, .004), 9)
    add(p, 'control recess', (0, .055, .042), (.04, .065, .004), 1)
    add(p, 'switch handle', (0, .055, .047), (.022, .036, .008), 6)
    add(p, 'status indicator', (0, .037, .052), (.018, .004, .002), 7)
    for x in (-.03, 0, .03):
        for y in (.006, .104):
            add(p, 'terminal screw', (x, y, .044), (.008, .008, .004), 4)
    return p


if __name__ == '__main__':
    export('panel_breaker_3p.glb', breaker())

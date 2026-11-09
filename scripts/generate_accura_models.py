"""Create illustrative rack-panel ACCURA 2350 meter models."""
from generate_pdu_models import add, export


def meter(three_phase):
    parts = []
    width = .060 if three_phase else .045
    h, depth = .105, .045
    add(parts, 'meter body', (0, h / 2, 0), (width, h, depth), 2)
    add(parts, 'front face', (0, h / 2, depth / 2 + .001), (width * .9, h * .94, .003), 9)
    add(parts, 'type label', (0, .091, .025), (width * .84, .015, .003), 10 if three_phase else 7)
    add(parts, 'display frame', (0, .055, .025), (width * .62, .028, .003), 4)
    add(parts, 'LCD', (0, .055, .027), (width * .54, .022, .002), 5)
    for x in (-.010, -.005, 0, .005, .010):
        add(parts, 'LCD segment', (x, .057, .029), (.002, .011, .001), 3)
    for y in (.024, .081):
        add(parts, 'network socket rim', (width * .20, y, .025), (width * .34, .019, .004), 4)
        add(parts, 'network socket', (width * .20, y, .028), (width * .23, .013, .002), 3)
    add(parts, 'button', (-width * .22, .021, .028), (.008, .008, .003), 10 if three_phase else 7)
    return parts


if __name__ == '__main__':
    export('accura_2350_1p.glb', meter(False))
    export('accura_2350_3p.glb', meter(True))

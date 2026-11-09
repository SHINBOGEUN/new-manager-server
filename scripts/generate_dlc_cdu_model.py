"""Build the rack-mounted DLC CDU example with its front facing +Z."""
from generate_pdu_models import add, export


def cdu():
    p = []
    add(p, 'chassis', (0, .089, 0), (.438, .172, .64), 0)
    add(p, 'front bezel', (0, .089, .323), (.482, .172, .009), 1)
    add(p, 'front inner panel', (0, .089, .329), (.421, .139, .004), 0)
    # Rack ears, silver handles and fasteners on both sides.
    for x in (-.23, .23):
        add(p, 'mounting ear', (x, .089, .33), (.022, .168, .008), 0)
        for y in (.025, .153):
            add(p, 'mounting screw', (x, y, .336), (.007, .007, .003), 4)
        handle_x = x * .90
        add(p, 'handle', (handle_x, .089, .348), (.009, .115, .009), 4)
        for y in (.031, .147):
            add(p, 'handle end', (handle_x, y, .341), (.014, .012, .014), 4)
    # Thin bright border around the control panel, interrupted by the grille.
    for y in (.021, .157):
        add(p, 'silver trim', (0, y, .335), (.405, .003, .003), 4)
    add(p, 'screen frame', (-.108, .091, .338), (.122, .088, .011), 4)
    add(p, 'screen bezel', (-.108, .091, .345), (.112, .078, .004), 0)
    add(p, 'touch screen', (-.108, .091, .348), (.097, .061, .003), 5)
    for x in (-.14, -.122, -.104, -.086):
        add(p, 'screen indicator', (x, .092, .350), (.008, .042, .002), 10)
    add(p, 'screen status strip', (-.108, .069, .351), (.084, .003, .002), 9)
    add(p, 'power indicator', (-.025, .091, .339), (.012, .012, .005), 10)
    add(p, 'vent backing', (.102, .091, .336), (.184, .116, .004), 3)
    # Individual openings remain part of the model, not a flat dark rectangle.
    for row in range(11):
        for col in range(20):
            x = .016 + col * .009 + (row % 2) * .0045
            y = .039 + row * .0103
            add(p, 'vent perforation', (x, y, .340), (.0047, .0047, .002), 2)
    add(p, 'bottom maker plate', (-.1, .028, .339), (.09, .012, .003), 2)
    for x in (-.18, .18):
        for y in (.025, .153):
            add(p, 'bezel fastener', (x, y, .34), (.006, .006, .003), 4)
    return p


if __name__ == '__main__':
    export('DLC_CDU_CHx200.glb', cdu())

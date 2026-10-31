"""
VERSION 1 (saved 2026-10-31): the trunk forks into limbs that leave its sides at different heights.
To go back to it, copy this file over ../tree_preview.py
(from the project folder:  cp art/tree_preview/versions/tree_preview_v1.py art/tree_preview/tree_preview.py).

Preview of the habit tree, drawn outside the app so you can change it and see it in seconds.

HOW TO USE
  1. Change numbers in the SETTINGS block below (every setting says what it does).
  2. Run it, from the project folder:

       python art/tree_preview/tree_preview.py                 the 14 pictures (seed + stages 1-13), seed 31
       python art/tree_preview/tree_preview.py --seed 8        another habit's tree (any whole number)
       python art/tree_preview/tree_preview.py --stage 13      one big picture of a single stage (0 to 13)
       python art/tree_preview/tree_preview.py --grid          12 different trees at the final stage
       python art/tree_preview/tree_preview.py --grid --stage 10   12 different trees at stage 10

  3. Open the png it writes, next to this file: tree_preview.png

It needs the Pillow package once:   python -m pip install pillow

This is a drawing of the same idea as the app, not the app itself: the app's trees have the same style and
rules but a different random sequence, so a given seed does not look exactly the same there. When you like
what you see, tell me the numbers you changed and I put them in the app (domain/habit/TreeShape.kt).
"""
import argparse
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

# ============================================================================================
# SETTINGS: everything to play with. Lengths are parts of the tree's height (the tree is 1 tall).
# ============================================================================================

# --- the trunk and the six limbs ------------------------------------------------------------
TRUNK_HEIGHT = 0.38          # how tall the bare trunk is at first
TRUNK_WIDTH = 0.07           # how thick it is at the foot
LIMB_FAN = [-1.0, -0.58, -0.10, 0.10, 0.58, 1.0]   # the angle of each limb from straight up (radians; 1.0 is about 57 degrees)
LIMB_LENGTHS = [0.60, 0.68, 0.74, 0.74, 0.68, 0.60]  # how long each limb is, compared with the trunk
LIMB_LENGTH_SCALE = 1.35     # one number to make all limbs longer or shorter
TAPER = 0.62                 # a branch ends this thick compared with where it starts (smaller = thinner tips)
LIMB_ATTACH = [0.80, 0.90, 1.0, 1.0, 0.90, 0.80]   # how far up the trunk each limb leaves it (1.0 = the very top; lower = a limb that peels off the side)
LIMB_SIDE = 0.35             # how far toward the side of the trunk a limb leaves from (0 = from the middle of the trunk)
LIMB_WIDTH = 0.95            # a limb starts this thick compared with the trunk where it leaves (smaller = thinner limbs)
ROOT_ARMS = [(-1.0, 0.17), (1.0, 0.16), (-0.5, 0.10), (0.55, 0.11)]   # the roots: (which way, how far they reach)
ROOT_WIDTH = 0.65            # a root starts this thick compared with the foot of the trunk

# --- the crown ---------------------------------------------------------------------------------
# Branches are kept inside this ellipse, so the crown is round: (height of its centre, half width, half height)
CROWN = (0.68, 0.50, 0.35)
MAX_DEPTH = 5                # how many times branches fork above the trunk (more = more twigs, and leaves)
LEADER_LENGTH = (0.64, 0.74)  # a branch's continuing leader is this much of its length
SIDE_BRANCH_LENGTH = (0.52, 0.68)  # a side branch is this much of its parent's length
INNER_LIMB_LEAN = 0.45       # limbs leaning less than this from vertical count as "inner" and close in on the middle

# --- leaves ------------------------------------------------------------------------------------
# Where leaves sit on a branch: (how far along it, how far the leaf turns). More entries = more leaves.
LEAVES_ON_LIMB = [(1.0, 0.0), (0.72, 0.6), (0.46, 0.6)]
LEAVES_ON_BRANCH = [(1.0, 0.0), (0.74, 0.6), (0.5, 0.6), (0.28, 0.6)]
LEAVES_ON_TWIG = [(1.0, 0.0), (0.8, 0.6), (0.62, -0.6), (0.44, 0.7), (0.28, -0.7)]
LEAF_SIZE = 0.040            # leaf length when the tree is young
LEAF_SIZE_GROWTH = 0.012     # how much longer leaves get as the tree grows
LEAF_SIZE_STRETCH = 0.016    # how much longer again while the crown grows taller (stages 11-13)
LEAF_FULL_BY = 0.50          # the tree has all its leaves by this share of the way to stage 10 (smaller = leaves come sooner)
LEAF_CURVE = 0.7             # below 1 = leaves come early and slow down; above 1 = they come late

# --- how the tree grows over the stages -----------------------------------------------------
# When each level of branches grows (share of the way to the tree with its full trunk: 0 to 1).
# Level 0 is the trunk, 1 the limbs, 2 and up the branches and twigs.
GROWTH_WINDOWS = [(0.0, 0.04), (0.02, 0.11), (0.06, 0.19), (0.12, 0.27), (0.19, 0.34), (0.26, 0.41)]
# Stage -> the look it has. Stages 8, 9, 10 look like 9, 11, 13 of an even run, then the trunk stays as it is.
STAGE_LOOK = [(0, 0), (7, 7), (8, 9), (9, 11), (10, 13), (13, 13)]
TRUNK_THICKER_FROM_STAGE = 5   # the trunk starts getting thicker at this stage
TRUNK_THICKER_BY = 2.0         # and ends up this much thicker (2.0 = three times as thick at the foot)
TRUNK_TALLER_FROM_STAGE = 10   # the trunk starts growing taller at this stage
TRUNK_TALLER_BY = 0.70         # and ends this much taller than at first (0.70 = 70% longer)
CROWN_STRETCH = 0.42           # after stage 10 the crown grows this much taller by stage 13 (0.42 = 42%)

# --- colours (red, green, blue) ----------------------------------------------------------------
LEAF_COLOR = (102, 168, 92)
TRUNK_COLOR = (138, 106, 74)
BACKGROUND = (14, 20, 26)

# ============================================================================================
# The drawing code. You do not need to change anything below.
# ============================================================================================

STAGES = 13


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def make(seed):
    """The tree of this seed: its branches and the places where its leaves sit."""
    rnd = random.Random(seed)
    branches = []

    def bez(b, t):
        u = 1 - t
        return (u * u * b['x0'] + 2 * u * t * b['cx'] + t * t * b['x1'],
                u * u * b['y0'] + 2 * u * t * b['cy'] + t * t * b['y1'])

    def tang(b, t):
        dx = 2 * (1 - t) * (b['cx'] - b['x0']) + 2 * t * (b['x1'] - b['cx'])
        dy = 2 * (1 - t) * (b['cy'] - b['y0']) + 2 * t * (b['y1'] - b['cy'])
        return math.atan2(dx, dy)

    def width_at(b, t):
        return b['w'] + (b['w'] * TAPER - b['w']) * t

    crown_y, crown_rx, crown_ry = CROWN

    def fit(x, y, dx, dy, length):
        """The length, no longer than asked, that keeps the tip inside the crown ellipse."""
        lo, hi = 0.0, length

        def inside(s):
            return ((x + dx * s) / crown_rx) ** 2 + ((y + dy * s - crown_y) / crown_ry) ** 2 <= 1.0

        if inside(hi):
            return hi
        for _ in range(24):
            mid = (lo + hi) / 2
            if inside(mid):
                lo = mid
            else:
                hi = mid
        return lo

    def grow(x, y, ang, length, w, depth, rnd, mirror):
        dx, dy = math.sin(ang), math.cos(ang)
        if depth > 0:
            length = max(fit(x, y, dx, dy, length), length * 0.25)
        ex, ey = x + dx * length, y + dy * length
        bend = (rnd.random() - 0.5) * (0.14 if depth == 0 else 0.6) * mirror
        if depth == 1:
            bend = -math.copysign(abs(bend), ang)  # the limbs bow upward
        nx, ny = math.cos(ang), -math.sin(ang)
        cx = (x + ex) / 2 + nx * bend * length
        cy = (y + ey) / 2 + ny * bend * length
        b = dict(depth=depth, x0=x, y0=y, cx=cx, cy=cy, x1=ex, y1=ey, w=w)
        branches.append(b)
        if depth == MAX_DEPTH:
            return
        end_width = w * TAPER
        end_ang = tang(b, 1.0)
        if depth == 0:
            # six limbs in three matching pairs: a limb and its partner are mirror images
            pair_seeds = [rnd.getrandbits(30) for _ in range(3)]
            for k in range(6):
                first = k < 3
                pr = random.Random(pair_seeds[k if first else 5 - k])
                sign = 1.0 if first else -1.0
                turn = (pr.random() - 0.5) * 0.18 * sign
                size = 0.92 + pr.random() * 0.16
                partner = 1.0 if first else 1.0 + (rnd.random() - 0.5) * 0.1
                # the limb leaves the trunk at a point along it, toward its own side: the outer limbs lower down
                t_attach = LIMB_ATTACH[k]
                trunk_width = width_at(b, t_attach)
                a_t = tang(b, t_attach)
                side_dir = 1.0 if LIMB_FAN[k] > 0 else -1.0
                px, py = bez(b, t_attach)
                bx = px + math.cos(a_t) * trunk_width * LIMB_SIDE * side_dir
                by = py - math.sin(a_t) * trunk_width * LIMB_SIDE * side_dir
                limb = len(branches)
                grow(bx, by, a_t * 0.25 + LIMB_FAN[k] + turn,
                     length * LIMB_LENGTHS[k] * size * partner * LIMB_LENGTH_SCALE, trunk_width * LIMB_WIDTH, 1, pr, sign)
                branches[limb]['attach'] = t_attach
            return
        magnitude = 0.32 + rnd.random() * 0.3
        flip = 1 if rnd.random() < 0.5 else -1
        if depth == 1:
            # the inner limbs close in on the middle of the crown; the outer ones only rise a little
            lean = -math.copysign(magnitude if abs(end_ang) < INNER_LIMB_LEAN else magnitude * 0.35, end_ang)
        else:
            lean = magnitude * flip * mirror
        lo, hi = LEADER_LENGTH
        grow(ex, ey, end_ang + lean, length * (lo + rnd.random() * (hi - lo)), end_width, depth + 1, rnd, mirror)
        count = {1: rnd.choice([1, 2, 2]), 2: rnd.choice([1, 2, 2]), 3: rnd.choice([1, 1, 2]), 4: rnd.choice([0, 1, 1])}.get(depth, 0)
        side = (1 if rnd.random() < 0.5 else -1) * mirror
        places = sorted(0.42 + rnd.random() * 0.5 for _ in range(count))
        lo, hi = SIDE_BRANCH_LENGTH
        for t in places:
            px, py = bez(b, t)
            a = tang(b, t) + side * (0.5 + rnd.random() * 0.45) + side * 0.05 * depth
            grow(px, py, a, length * (lo + rnd.random() * (hi - lo)), width_at(b, t) * 0.68, depth + 1, rnd, mirror)
            side = -side

    grow(0, 0, (rnd.random() - 0.5) * 0.06, TRUNK_HEIGHT, TRUNK_WIDTH, 0, rnd, 1.0)

    # fit the tree to a unit height and centre it
    pts = [bez(b, i / 6) for b in branches for i in range(7)]
    top = max(p[1] for p in pts)
    mid = (min(p[0] for p in pts) + max(p[0] for p in pts)) / 2
    scale = 1 / top
    for b in branches:
        for k in ('x0', 'cx', 'x1'):
            b[k] = (b[k] - mid) * scale
        for k in ('y0', 'cy', 'y1'):
            b[k] = b[k] * scale
        b['w'] *= scale

    spots = []
    for i, b in enumerate(branches):
        d = b['depth']
        if d == 0:
            plan = []
        elif d == MAX_DEPTH:
            plan = LEAVES_ON_TWIG
        elif d >= 2:
            plan = LEAVES_ON_BRANCH
        else:
            plan = LEAVES_ON_LIMB
        for t, turn in plan:
            if turn == 0.6:
                turn *= rnd.choice([-1, 1])
            spots.append(dict(branch=i, t=t, ang=tang(b, t) + turn + (rnd.random() - 0.5) * 0.4,
                              size=0.85 + rnd.random() * 0.3, shade=rnd.random(), rank=rnd.random() * 0.92))
    return branches, spots, bez, tang


def look_of(stage):
    """The stage whose trunk the tree has: the trunk reaches its full form at stage 10 and stays."""
    for (a, la), (b, lb) in zip(STAGE_LOOK, STAGE_LOOK[1:]):
        if stage <= b:
            return la + (lb - la) * (stage - a) / (b - a)
    return float(STAGES)


def render(seed, stage, W=250, H=400):
    """The tree of [seed] at [stage] (0 to 13, may be in between), W by H pixels."""
    stretch = CROWN_STRETCH * max(0.0, min(1.0, (stage - 10) / 3))
    f = look_of(stage) / STAGES          # how far along the tree is, 0 to 1
    base, spots, bez, tang = make(seed)
    thicker_from = TRUNK_THICKER_FROM_STAGE / STAGES
    taller_from = TRUNK_TALLER_FROM_STAGE / STAGES
    lift_max = TRUNK_TALLER_BY * TRUNK_HEIGHT

    # from the "taller" stage the trunk grows, and lifts the crown with it (a limb rides up with the part of the
    # trunk it leaves); then each limb grows taller from where it leaves the trunk
    lift = lift_max * max(0.0, min(1.0, (f - taller_from) / (1 - taller_from)))
    branches = []
    owner = {}       # the limb each branch belongs to
    limb_base = {}   # where each limb leaves the trunk, lifted
    attach = {}
    current = None
    for i, b in enumerate(base):
        c = dict(b)
        if i == 0:
            c['y1'] += lift
            c['cy'] += lift / 2
        else:
            if b['depth'] == 1:
                current = i
                attach[i] = b['attach']
            owner[i] = current
            shift = lift * attach[current]
            for k in ('y0', 'cy', 'y1'):
                c[k] += shift
        branches.append(c)
    for i, c in enumerate(branches):
        if i == 0:
            continue
        pivot = branches[owner[i]]['y0']
        for k in ('y0', 'cy', 'y1'):
            c[k] = pivot + (c[k] - pivot) * (1 + stretch)

    thick = max(0.0, (f - thicker_from) / (1 - thicker_from))

    def girth(depth):
        return 1 + TRUNK_THICKER_BY * thick * {0: 1.0, 1: 0.5, 2: 0.2}.get(depth, 0.0)

    def growth(depth):
        a, b = GROWTH_WINDOWS[depth]
        t = smooth((f - a) / (b - a))
        return max(t, 0.12) if depth == 0 else t

    SS = 3  # drawn this much larger, then shrunk, to smooth the edges
    img = Image.new('RGB', (W * SS, H * SS), BACKGROUND)
    dr = ImageDraw.Draw(img, 'RGBA')
    ground = H * 0.97 * SS
    centre = W / 2 * SS
    half = max(max(abs(b['x0']), abs(b['x1'])) for b in branches) + 0.07
    room = 1 + lift_max + CROWN_STRETCH * 0.62
    sc = min(H * 0.90 / room, W * 0.47 / half) * SS * (0.70 + 0.30 * smooth(min(1.0, f / 0.9)))

    def P(x, y):
        return (centre + x * sc, ground - y * sc)

    # the ground, tufts of grass, roots
    dr.ellipse([centre - sc * 0.45, ground - 9 * SS, centre + sc * 0.45, ground + 11 * SS], fill=TRUNK_COLOR + (50,))
    rr = random.Random(seed + 1)
    for _ in range(32):
        gx = centre + (rr.random() - 0.5) * sc * 0.85
        h = (8 + rr.random() * 14) * SS
        bend = (rr.random() - 0.5) * 8 * SS
        dr.line([(gx, ground + 3 * SS), (gx + bend, ground + 3 * SS - h)], fill=(72, 148, 62, 200), width=2 * SS)
    root_grow = 0.45 + 0.55 * min(1, f * 6)
    for side, reach in ROOT_ARMS:
        top_edge, bottom_edge = [], []
        for i in range(11):
            t = i / 10
            x = side * reach * root_grow * t
            y = 0.035 * (1 - t) ** 1.5 * (0.6 + 0.4 * root_grow) - 0.004 * t
            w = branches[0]['w'] * ROOT_WIDTH * girth(0) * (1 - t) ** 0.9 * root_grow + 0.0015
            top_edge.append(P(x, y + w / 2))
            bottom_edge.append(P(x, y - w / 2))
        dr.polygon(top_edge + bottom_edge[::-1], fill=TRUNK_COLOR + (255,))

    # the trunk and the branches
    for i, b in enumerate(branches):
        g = growth(b['depth'])
        if g <= 0.001:
            continue
        steps = 14
        pts = []
        for k in range(steps + 1):
            t = g * k / steps
            x, y = bez(b, t)
            w = b['w'] + (b['w'] * TAPER - b['w']) * t
            if i == 0:
                w *= 1 + 1.0 * max(0, 1 - t / 0.2) ** 2   # the foot flares out
            w *= girth(b['depth'])
            pts.append((x, y, w * (0.5 + 0.5 * g)))
        left, right = [], []
        for k, (x, y, w) in enumerate(pts):
            a = tang(b, g * k / steps)
            nx, ny = math.cos(a), -math.sin(a)
            left.append(P(x - nx * w / 2, y - ny * w / 2))
            right.append(P(x + nx * w / 2, y + ny * w / 2))
        dr.polygon(left + right[::-1], fill=TRUNK_COLOR + (255,))
        shade = [(l[0] * 0.35 + r[0] * 0.65, l[1] * 0.35 + r[1] * 0.65) for l, r in zip(left, right)]
        dr.polygon(shade + right[::-1], fill=(40, 28, 18, 70))
        if b['depth'] >= 2:
            x, y, w = pts[0]
            px, py = P(x, y)
            dr.ellipse([px - w * sc / 2, py - w * sc / 2, px + w * sc / 2, py + w * sc / 2], fill=TRUNK_COLOR + (255,))

    # the leaves
    leaf_len = sc * (LEAF_SIZE + LEAF_SIZE_GROWTH * smooth(min(1.0, f / 0.9)) + LEAF_SIZE_STRETCH * (stretch / CROWN_STRETCH))
    leaf_share = min(1.0, max(0.0, (f - 0.01) / LEAF_FULL_BY)) ** LEAF_CURVE
    placed = []
    for sp in spots:
        b = branches[sp['branch']]
        g = growth(b['depth'])
        lg = max(0.0, min(1.0, (leaf_share - sp['rank']) / 0.06)) * max(0.0, min(1.0, (g - sp['t']) / 0.12))
        if lg > 0:
            x, y = bez(b, sp['t'] * g)
            placed.append((P(x, y), sp, lg))

    def draw_leaf(px, py, length, angle, color, midrib=True):
        half_w = length * 0.36
        pts = [(math.sin(math.pi * (k / 8) ** 0.8) * half_w, -length * k / 8) for k in range(9)]
        pts += [(-math.sin(math.pi * (k / 8) ** 0.8) * half_w * 0.85, -length * k / 8) for k in range(8, -1, -1)]
        ca, sa = math.cos(angle), math.sin(angle)
        dr.polygon([(px + x * ca - y * sa, py + x * sa + y * ca) for x, y in pts], fill=color + (255,))
        if midrib:
            dr.line([(px, py), (px + length * 0.92 * sa, py - length * 0.92 * ca)], fill=(30, 70, 30, 120), width=max(1, SS))

    for (px, py), sp, lg in placed:
        r = leaf_len * 1.1 * lg
        dr.ellipse([px - r, py - r, px + r, py + r], fill=LEAF_COLOR + (6,))
    for (px, py), sp, lg in placed:
        k = 0.78 + 0.3 * sp['shade']
        draw_leaf(px, py, leaf_len * sp['size'] * lg, sp['ang'], tuple(int(c * k) for c in LEAF_COLOR))
    if f < 0.04:  # the seed: a sprout with two small leaves
        x, y = bez(branches[0], growth(0))
        px, py = P(x, y)
        for a in (-0.7, 0.7):
            draw_leaf(px, py, leaf_len * 0.9, a, LEAF_COLOR, midrib=False)
    return img.resize((W, H), Image.LANCZOS)


def sheet_of_stages(seed, out):
    W, H = 250, 400
    base, spots, _, _ = make(seed)
    print('seed', seed, '-', len(base), 'branches,', len(spots), 'leaf places')
    sheet = Image.new('RGB', (W * 7, H * 2), BACKGROUND)
    d = ImageDraw.Draw(sheet)
    for st in range(STAGES + 1):
        sheet.paste(render(seed, st, W, H), ((st % 7) * W, (st // 7) * H))
        d.text(((st % 7) * W + 10, (st // 7) * H + 8), 'seed' if st == 0 else 'stage %d' % st, fill=(200, 210, 220))
    sheet.save(out)


def one_stage(seed, stage, out):
    render(seed, stage, 500, 800).save(out)


def grid_of_trees(stage, out):
    W, H = 250, 400
    seeds = [31, 8, 5, 77, 101, 202, 303, 404, 505, 606, 707, 808]
    sheet = Image.new('RGB', (W * 6, H * 2), BACKGROUND)
    d = ImageDraw.Draw(sheet)
    for i, s in enumerate(seeds):
        sheet.paste(render(s, stage, W, H), ((i % 6) * W, (i // 6) * H))
        d.text(((i % 6) * W + 10, (i // 6) * H + 8), 'seed %d' % s, fill=(200, 210, 220))
    sheet.save(out)
    print('seeds', seeds)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Preview of the habit tree.')
    parser.add_argument('--seed', type=int, default=31, help='which habit\'s tree (any whole number)')
    parser.add_argument('--stage', type=float, default=None, help='one stage, 0 to 13')
    parser.add_argument('--grid', action='store_true', help='12 different trees side by side (at --stage, default 13)')
    args = parser.parse_args()
    out = Path(__file__).with_name('tree_preview.png')
    if args.grid:
        grid_of_trees(13 if args.stage is None else args.stage, out)
    elif args.stage is not None:
        one_stage(args.seed, args.stage, out)
    else:
        sheet_of_stages(args.seed, out)
    print('written', out)

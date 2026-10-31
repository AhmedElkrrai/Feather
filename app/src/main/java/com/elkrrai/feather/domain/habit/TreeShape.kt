package com.elkrrai.feather.domain.habit

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The tree of a habit grows in stages: one stage for every [TREE_WEEKS_PER_STAGE] weeks that met the
 * goal, [TREE_STAGES] stages in all (a year of met weeks). It never shrinks: a week that was not met
 * adds nothing, and takes nothing away.
 */
const val TREE_STAGES = 13
const val TREE_WEEKS_PER_STAGE = 4

/** The stage after [metWeeks] weeks that met the goal: 0 is the seed, 1 starts with the first met week. */
fun treeStage(metWeeks: Int): Int =
    if (metWeeks <= 0) 0 else min(TREE_STAGES, (metWeeks - 1) / TREE_WEEKS_PER_STAGE + 1)

/** The met weeks still needed for the next stage, or null once the tree is fully grown. */
fun weeksToNextTreeStage(metWeeks: Int): Int? {
    val stage = treeStage(metWeeks)
    return if (stage >= TREE_STAGES) null else stage * TREE_WEEKS_PER_STAGE + 1 - maxOf(metWeeks, 0)
}

/**
 * The fixed shape of one tree (the same seed always grows the same tree), in a unit drawing: the tree is
 * 1 tall, stands with its foot at (0, 0) with y rising, and is centred on x = 0. How much of it has grown
 * is up to [TreeLook]; the screen scales the whole drawing.
 */
class TreeShape(val branches: List<TreeBranch>, val spots: List<TreeSpot>) {
    /** How far the crown reaches to either side of the trunk, with room for the leaves. */
    val halfWidth: Float = branches.maxOf { max(abs(it.x0), abs(it.x1)) } + 0.07f
}

/** A branch as a curve from its foot to its tip with one control point. [depth] 0 is the trunk. */
class TreeBranch(
    val depth: Int,
    val x0: Float,
    val y0: Float,
    val cx: Float,
    val cy: Float,
    val x1: Float,
    val y1: Float,
    /** How thick the branch is at its foot, as a part of the tree's height. */
    val width: Float,
    /** The index of the limb this branch is on (a limb is on itself), or -1 for the trunk. */
    val limb: Int = -1,
    /** For a limb: how far up the trunk it leaves it, 0 to 1. */
    val attach: Float = 1f,
) {
    fun x(t: Float): Float = (1 - t) * (1 - t) * x0 + 2 * (1 - t) * t * cx + t * t * x1

    fun y(t: Float): Float = (1 - t) * (1 - t) * y0 + 2 * (1 - t) * t * cy + t * t * y1

    /** The direction of the branch at [t], in radians from straight up. */
    fun angle(t: Float): Float {
        val dx = 2 * (1 - t) * (cx - x0) + 2 * t * (x1 - cx)
        val dy = 2 * (1 - t) * (cy - y0) + 2 * t * (y1 - cy)
        return atan2(dx, dy)
    }

    /** The width at [t] along the branch: it thins toward the tip. */
    fun widthAt(t: Float): Float = width + (width * TREE_TAPER - width) * t
}

/**
 * Where a leaf sits: [along] the branch of index [branch] (0 at its foot, 1 at its tip), turned [angle]
 * radians from straight up. A leaf comes out when the branch has grown to it and the tree's share of
 * leaves has passed its [rank].
 */
class TreeSpot(val branch: Int, val along: Float, val angle: Float, val size: Float, val shade: Float, val rank: Float)

const val TREE_TAPER = 0.62f

/** The deepest fork: the trunk, and five forks above it. */
private const val MAX_DEPTH = 5

/** The trunk's height, as a part of the tree's height before it grows taller. */
private const val TRUNK = 0.38f

/** How far up the trunk each of the six limbs leaves it (1 is the very top): the outer ones peel off lower down. */
private val LIMB_ATTACH = floatArrayOf(0.80f, 0.90f, 1.0f, 1.0f, 0.90f, 0.80f)

/** How far toward the side of the trunk a limb leaves from, and how thick it starts compared with the trunk there. */
private const val LIMB_SIDE = 0.35f
private const val LIMB_WIDTH = 0.95f

/** The roots: which way each reaches (-1 left to 1 right) and how far, as a part of the tree's height. */
val TREE_ROOTS: List<Pair<Float, Float>> = listOf(-1.0f to 0.17f, 1.0f to 0.16f, -0.5f to 0.10f, 0.55f to 0.11f)

/** A root starts this thick compared with the foot of the trunk. */
const val TREE_ROOT_WIDTH = 0.65f

/** The crown is kept inside this ellipse (centre height, half width, half height), so it is round and even. */
private const val CROWN_Y = 0.68f
private const val CROWN_RX = 0.50f
private const val CROWN_RY = 0.35f

/** The tree for [seed]. */
fun treeShape(seed: Int): TreeShape {
    val random = TreeRandom(seed)
    val raw = ArrayList<TreeBranch>()
    val attachOf = HashMap<Int, Float>()

    fun fit(x: Float, y: Float, dx: Float, dy: Float, length: Float): Float {
        fun inside(s: Float): Boolean {
            val px = (x + dx * s) / CROWN_RX
            val py = (y + dy * s - CROWN_Y) / CROWN_RY
            return px * px + py * py <= 1f
        }
        if (inside(length)) return length
        var lo = 0f
        var hi = length
        repeat(24) {
            val mid = (lo + hi) / 2
            if (inside(mid)) lo = mid else hi = mid
        }
        return lo
    }

    /**
     * Grows a branch and all that is on it. [rnd] makes the branch's choices and [mirror] (1 or -1) says which
     * way they lean: a limb grown again from the same [rnd] with -1 is the mirror image of the first.
     */
    fun grow(x: Float, y: Float, angle: Float, length0: Float, width: Float, depth: Int, rnd: TreeRandom, mirror: Float) {
        val dx = sin(angle)
        val dy = cos(angle)
        val length = if (depth > 0) max(fit(x, y, dx, dy, length0), length0 * 0.25f) else length0
        val ex = x + dx * length
        val ey = y + dy * length
        var bend = (rnd.nextFloat() - 0.5f) * (if (depth == 0) 0.14f else 0.6f) * mirror
        // the limbs bow upward, never down
        if (depth == 1) bend = -abs(bend) * (if (angle >= 0f) 1f else -1f)
        val cx = (x + ex) / 2 + cos(angle) * bend * length
        val cy = (y + ey) / 2 - sin(angle) * bend * length
        val branch = TreeBranch(depth, x, y, cx, cy, ex, ey, width)
        raw += branch
        if (depth == MAX_DEPTH) return
        val endWidth = width * TREE_TAPER
        val endAngle = branch.angle(1f)
        if (depth == 0) {
            // the trunk forks into six limbs of about the same length, fanned out: there is no leader. The
            // limbs on the two sides come in matching pairs, so the crown is the same on both sides.
            val fan = floatArrayOf(-1.0f, -0.58f, -0.10f, 0.10f, 0.58f, 1.0f)
            val lengths = floatArrayOf(0.60f, 0.68f, 0.74f, 0.74f, 0.68f, 0.60f)
            val pairSeeds = IntArray(3) { rnd.nextBits30() }
            for (k in fan.indices) {
                val first = k < 3
                val pair = TreeRandom(pairSeeds[if (first) k else 5 - k])
                val sign = if (first) 1f else -1f
                val turn = (pair.nextFloat() - 0.5f) * 0.18f * sign
                val size = 0.92f + pair.nextFloat() * 0.16f
                // the partner is a touch different in size, so the two sides are alike but not stamped
                val partner = if (first) 1f else 1f + (rnd.nextFloat() - 0.5f) * 0.1f
                // the limb leaves the trunk at a point along it, toward its own side: the outer limbs lower down
                val attach = LIMB_ATTACH[k]
                val trunkWidth = branch.widthAt(attach)
                val trunkAngle = branch.angle(attach)
                val sideDirection = if (fan[k] > 0f) 1f else -1f
                val startX = branch.x(attach) + cos(trunkAngle) * trunkWidth * LIMB_SIDE * sideDirection
                val startY = branch.y(attach) - sin(trunkAngle) * trunkWidth * LIMB_SIDE * sideDirection
                attachOf[raw.size] = attach
                grow(
                    startX, startY, trunkAngle * 0.25f + fan[k] + turn,
                    length * lengths[k] * size * partner * 1.35f, trunkWidth * LIMB_WIDTH, 1, pair, sign,
                )
            }
            return
        }
        // a leader that leans instead of running straight up. A limb leans toward the middle of the crown, the
        // inner two a lot, which fills the middle; the outer ones only rise a little. The twigs lean either way.
        val magnitude = 0.32f + rnd.nextFloat() * 0.3f
        val flip = if (rnd.nextDouble() < 0.5) 1f else -1f
        val lean = if (depth == 1) {
            -(if (endAngle >= 0f) 1f else -1f) * (if (abs(endAngle) < 0.45f) magnitude else magnitude * 0.35f)
        } else {
            magnitude * flip * mirror
        }
        grow(ex, ey, endAngle + lean, length * (0.64f + rnd.nextFloat() * 0.1f), endWidth, depth + 1, rnd, mirror)
        val count = when (depth) {
            1, 2 -> rnd.pick(intArrayOf(1, 2, 2))
            3 -> rnd.pick(intArrayOf(1, 1, 2))
            else -> rnd.pick(intArrayOf(0, 1, 1))
        }
        var side = (if (rnd.nextDouble() < 0.5) 1f else -1f) * mirror
        val places = List(count) { 0.42f + rnd.nextFloat() * 0.5f }.sorted()
        for (t in places) {
            val a = branch.angle(t) + side * (0.5f + rnd.nextFloat() * 0.45f) + side * 0.05f * depth
            grow(branch.x(t), branch.y(t), a, length * (0.52f + rnd.nextFloat() * 0.16f), branch.widthAt(t) * 0.68f, depth + 1, rnd, mirror)
            side = -side
        }
    }

    grow(0f, 0f, (random.nextFloat() - 0.5f) * 0.06f, TRUNK, 0.07f, 0, random, 1f)

    // fit the tree to a unit height and centre it
    var top = 0f
    var left = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    for (b in raw) {
        for (i in 0..6) {
            val t = i / 6f
            top = max(top, b.y(t))
            left = min(left, b.x(t))
            right = max(right, b.x(t))
        }
    }
    val scale = 1f / top
    val centre = (left + right) / 2
    var currentLimb = -1
    val branches = raw.mapIndexed { index, it ->
        if (it.depth == 1) currentLimb = index
        TreeBranch(
            it.depth,
            (it.x0 - centre) * scale, it.y0 * scale,
            (it.cx - centre) * scale, it.cy * scale,
            (it.x1 - centre) * scale, it.y1 * scale,
            it.width * scale,
            limb = if (it.depth == 0) -1 else currentLimb,
            attach = attachOf[index] ?: 1f,
        )
    }

    val spots = ArrayList<TreeSpot>()
    branches.forEachIndexed { index, b ->
        // (how far along, how far the leaf turns from the branch); a turn of 0.6 goes to either side, by chance
        val plan: List<Pair<Float, Float>> = when {
            b.depth == 0 -> emptyList()
            b.depth == MAX_DEPTH -> listOf(1f to 0f, 0.8f to 0.6f, 0.62f to -0.6f, 0.44f to 0.7f, 0.28f to -0.7f)
            b.depth >= 2 -> listOf(1f to 0f, 0.74f to 0.6f, 0.5f to 0.6f, 0.28f to 0.6f)
            else -> listOf(1f to 0f, 0.72f to 0.6f, 0.46f to 0.6f)
        }
        for ((along, planned) in plan) {
            val turn = if (planned == 0.6f) planned * (if (random.nextDouble() < 0.5) -1f else 1f) else planned
            spots += TreeSpot(
                branch = index,
                along = along,
                angle = b.angle(along) + turn + (random.nextFloat() - 0.5f) * 0.4f,
                size = 0.85f + random.nextFloat() * 0.3f,
                shade = random.nextFloat(),
                rank = random.nextFloat() * 0.92f,
            )
        }
    }
    return TreeShape(branches, spots)
}

/** From stage 10 on the trunk stands tall; the most it grows, as a part of the tree's height. */
const val TREE_LIFT_MAX = 0.70f * TRUNK

/** After stage 10 the crown grows taller, stretched upward from the top of the trunk, by this much at the end. */
const val TREE_CROWN_STRETCH = 0.42f

/** The height the picture has to leave room for, as a multiple of the tree's own height. */
const val TREE_HEIGHT_ROOM = 1f + TREE_LIFT_MAX + TREE_CROWN_STRETCH * 0.62f

/** When each depth of branches grows, as a share of the way to the full tree. */
private val WINDOWS = arrayOf(
    0.00f to 0.04f, 0.02f to 0.11f, 0.06f to 0.19f, 0.12f to 0.27f, 0.19f to 0.34f, 0.26f to 0.41f,
)

private const val STAGE_THICKER = 5f / TREE_STAGES
private const val STAGE_TALLER = 10f / TREE_STAGES

private fun smooth(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3 - 2 * c)
}

/**
 * How much of the tree has grown at [stage] (0 to 13, in between while it grows): which branches are
 * out, how many leaves, how thick and tall the trunk is.
 *
 * Stages 1 to 7 grow the whole tree. Stages 8, 9 and 10 take the trunk to its full, tall and thick form
 * (they look as the tree did at 9, 11 and 13 of an even run of stages); after stage 10 the trunk stays
 * as it is and the crown grows taller.
 */
class TreeLook(stage: Float) {
    /** The share of the way to the tree with its full trunk, 0 to 1. */
    val share: Float = trunkStage(stage) / TREE_STAGES

    /** How far the roots have spread, 0 to 1: they come early. */
    val roots: Float = min(1f, share * 6f)

    /** How far up the whole crown is lifted by the trunk growing tall. */
    val lift: Float = TREE_LIFT_MAX * ((share - STAGE_TALLER) / (1 - STAGE_TALLER)).coerceIn(0f, 1f)

    /** How much taller than before the crown has grown, as a share of its height above the trunk. */
    val stretch: Float = TREE_CROWN_STRETCH * ((stage - 10f) / 3f).coerceIn(0f, 1f)

    /** How big the whole picture is drawn, relative to the full size. */
    val size: Float = 0.70f + 0.30f * smooth(min(1f, share / 0.9f))

    /** The leaf length, as a part of the picture's scale: a little larger as the crown grows taller, to keep it full. */
    val leafSize: Float = 0.040f + 0.012f * smooth(min(1f, share / 0.9f)) + 0.016f * (stretch / TREE_CROWN_STRETCH)

    /** The share of the leaves that are out, 0 to 1. */
    val leafShare: Float = ((share - 0.01f) / 0.50f).coerceIn(0f, 1f).pow(0.7f)

    private val thick = max(0f, (share - STAGE_THICKER) / (1 - STAGE_THICKER))

    /** How much thicker than at first a branch of [depth] is: the trunk most, the limbs a little, the twigs not at all. */
    fun girth(depth: Int): Float = 1f + 2.0f * thick * when (depth) {
        0 -> 1f
        1 -> 0.5f
        2 -> 0.2f
        else -> 0f
    }

    /** How far a branch of [depth] has grown, 0 to 1 (the trunk is never less than a sprout). */
    fun branchGrowth(depth: Int): Float {
        val (from, to) = WINDOWS[depth.coerceIn(0, MAX_DEPTH)]
        val t = smooth((share - from) / (to - from))
        return if (depth == 0) max(t, 0.12f) else t
    }

    /** How far leaf [spot] has come out (0 to 1): its branch has to have grown to it. */
    fun leafGrowth(spot: TreeSpot, branchGrowth: Float): Float =
        ((leafShare - spot.rank) / 0.06f).coerceIn(0f, 1f) * ((branchGrowth - spot.along) / 0.12f).coerceIn(0f, 1f)

    /**
     * The trunk and branches as they stand now. The taller trunk lifts each limb by the part of the trunk it
     * leaves from, and after stage 10 each limb grows taller from where it leaves the trunk.
     */
    fun place(shape: TreeShape): List<TreeBranch> {
        val lifted = shape.branches.mapIndexed { i, b ->
            if (i == 0) {
                TreeBranch(0, b.x0, b.y0, b.cx, b.cy + lift / 2, b.x1, b.y1 + lift, b.width)
            } else {
                val up = lift * shape.branches[b.limb].attach
                TreeBranch(b.depth, b.x0, b.y0 + up, b.cx, b.cy + up, b.x1, b.y1 + up, b.width, b.limb, b.attach)
            }
        }
        return lifted.mapIndexed { i, b ->
            if (i == 0) {
                b
            } else {
                val pivot = lifted[b.limb].y0
                fun grow(y: Float) = pivot + (y - pivot) * (1 + stretch)
                TreeBranch(b.depth, b.x0, grow(b.y0), b.cx, grow(b.cy), b.x1, grow(b.y1), b.width, b.limb, b.attach)
            }
        }
    }

    companion object {
        /** The stage whose trunk the tree has: 8, 9 and 10 have the trunk of 9, 11 and 13, and it stays so. */
        fun trunkStage(stage: Float): Float {
            val points = arrayOf(0f to 0f, 7f to 7f, 8f to 9f, 9f to 11f, 10f to 13f, 13f to 13f)
            for (i in 0 until points.size - 1) {
                val (a, la) = points[i]
                val (b, lb) = points[i + 1]
                if (stage <= b) return la + (lb - la) * (stage - a) / (b - a)
            }
            return 13f
        }
    }
}

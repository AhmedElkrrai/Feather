package com.elkrrai.feather.domain.habit

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The tree of a habit: one leaf for each week the goal was met. The tree is a fixed shape (made from a
 * seed, so a habit always grows the same tree) and the number of met weeks decides how much of it has
 * grown. It never shrinks: a week that was not met adds nothing, and takes nothing away.
 *
 * Everything is in a unit drawing: the tree is 1 tall, stands with its foot at (0, 0) and y rising, and
 * is centred on x = 0. The screen scales it.
 */
class TreeShape(val branches: List<TreeBranch>, val spots: List<TreeSpot>) {
    /** The widest the tree reaches to either side of its trunk. */
    val halfWidth: Float = branches.maxOf { maxOf(abs(it.x0), abs(it.x1)) }
}

/** A branch from its foot to its tip. [depth] 0 is the trunk; each fork is one deeper. */
class TreeBranch(
    val depth: Int,
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    /** How thick the branch is when fully grown, as a part of the tree's height. */
    val width: Float,
)

/**
 * Where a leaf can sit: [along] the branch of index [branch] (0 at its foot, 1 at its tip), turned to
 * [angle] radians from straight up. Spots are in the order leaves come: the first met week takes the
 * first spot.
 */
class TreeSpot(val branch: Int, val along: Float, val angle: Float)

/** The deepest fork: the trunk, and five forks above it. */
const val TREE_DEPTH = 5

/** The leaves a tree has room for. More met weeks than this still count, in the numbers and in the girth. */
const val TREE_MAX_LEAVES = 126

/** The leaf spots of the branches of each depth, added up before it: the first leaf of a depth is this one. */
private val CAP_START = intArrayOf(0, 2, 6, 14, 30, 62)

/** Branches of a depth grow while the met weeks go from here to there; the next depth starts where this ends. */
private val GROW_FROM = intArrayOf(0, 1, 2, 6, 14, 30)
private val GROW_TO = intArrayOf(1, 2, 6, 14, 30, 62)

/** How far a branch of [depth] has grown (0 to 1) once [weeks] weeks were met. The trunk is never less than a sprout. */
fun treeBranchGrowth(depth: Int, weeks: Float): Float {
    val d = depth.coerceIn(0, TREE_DEPTH)
    val t = ((weeks - GROW_FROM[d]) / (GROW_TO[d] - GROW_FROM[d])).coerceIn(0f, 1f)
    val eased = t * t * (3f - 2f * t)
    return if (d == 0) maxOf(eased, SPROUT) else eased
}

/** The trunk at the very first, before any week was met. */
private const val SPROUT = 0.14f

/** How far leaf number [index] (0 is the first) has come out once [weeks] weeks were met: 0 to 1. */
fun treeLeafGrowth(index: Int, weeks: Float): Float = (weeks - index).coerceIn(0f, 1f)

/** A multiplier for the thickness of the branches: a tree of many years has a thicker trunk and limbs. */
fun treeGirth(weeks: Float): Float = 1f + 0.7f * ((weeks - 30f) / 200f).coerceIn(0f, 1f)

/** The tree for [seed] (the same seed always makes the same tree). */
fun treeShape(seed: Int): TreeShape {
    val random = Random(seed)
    val raw = ArrayList<TreeBranch>()
    fun grow(x: Float, y: Float, angle: Float, length: Float, width: Float, depth: Int) {
        val x1 = x + sin(angle) * length
        val y1 = y + cos(angle) * length
        raw += TreeBranch(depth, x, y, x1, y1, width)
        if (depth == TREE_DEPTH) return
        val spread = 0.42f + random.nextFloat() * 0.22f
        val childLength = length * (0.70f + random.nextFloat() * 0.08f)
        val childWidth = width * 0.66f
        grow(x1, y1, angle - spread + (random.nextFloat() - 0.5f) * 0.2f, childLength * (0.92f + random.nextFloat() * 0.16f), childWidth, depth + 1)
        grow(x1, y1, angle + spread + (random.nextFloat() - 0.5f) * 0.2f, childLength * (0.92f + random.nextFloat() * 0.16f), childWidth, depth + 1)
    }
    grow(0f, 0f, (random.nextFloat() - 0.5f) * 0.08f, 0.30f, 0.045f, 0)

    // fit the tree to a unit height and centre it
    val top = raw.maxOf { maxOf(it.y0, it.y1) }
    val left = raw.minOf { minOf(it.x0, it.x1) }
    val right = raw.maxOf { maxOf(it.x0, it.x1) }
    val scale = 1f / top
    val centre = (left + right) / 2f
    val branches = raw.map {
        TreeBranch(it.depth, (it.x0 - centre) * scale, it.y0 * scale, (it.x1 - centre) * scale, it.y1 * scale, it.width * scale)
    }

    // two spots on each branch, the leaves of the lower forks coming first and, within a fork, in no set order
    val spots = branches.indices
        .flatMap { index ->
            val b = branches[index]
            val direction = atan2(b.x1 - b.x0, b.y1 - b.y0)
            listOf(
                TreeSpot(index, 0.55f, direction + (if (random.nextBoolean()) 1f else -1f) * (0.5f + random.nextFloat() * 0.5f)),
                TreeSpot(index, 1f, direction + (random.nextFloat() - 0.5f) * 0.5f),
            )
        }
        .shuffled(random)
        .sortedBy { branches[it.branch].depth }
    return TreeShape(branches, spots)
}

/** The first leaf spot of the branches at [depth]; leaf spots before it belong to lower forks. */
fun treeFirstSpotOfDepth(depth: Int): Int = CAP_START[depth.coerceIn(0, TREE_DEPTH)]

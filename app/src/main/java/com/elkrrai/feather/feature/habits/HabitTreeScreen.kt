package com.elkrrai.feather.feature.habits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.TREE_HEIGHT_ROOM
import com.elkrrai.feather.domain.habit.TREE_ROOTS
import com.elkrrai.feather.domain.habit.TREE_ROOT_WIDTH
import com.elkrrai.feather.domain.habit.TREE_STAGES
import com.elkrrai.feather.domain.habit.TreeLook
import com.elkrrai.feather.domain.habit.TreeRandom
import com.elkrrai.feather.domain.habit.TreeShape
import com.elkrrai.feather.domain.habit.overview
import com.elkrrai.feather.domain.habit.treeShape
import com.elkrrai.feather.domain.habit.treeStage
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.habit.weeksToNextTreeStage
import com.elkrrai.feather.ui.firstDayOfWeek
import com.elkrrai.feather.ui.rememberLowEndMode
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.theme.leafColor
import com.elkrrai.feather.ui.theme.rememberSeason
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The tree of one habit. It grows in stages, one for every four weeks that met the goal, and grows from
 * a seed up to its stage each time the page opens.
 */
@Composable
fun HabitTreeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitTreeViewModel = hiltViewModel(),
) {
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    HabitTreeContent(habit = habit, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HabitTreeContent(
    habit: HabitState?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val firstDay = firstDayOfWeek(locale)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(habit?.habit?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (habit == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val all = remember(habit, today, firstDay) { habit.overview(weekStartOf(today, firstDay), firstDay) }
        val stage = treeStage(all.weeksMet)
        val shape = remember(habit.habit.id) { treeShape(habit.habit.id.hashCode()) }
        val met = pluralStringResource(R.plurals.habit_tree_met, all.weeksMet, all.weeksMet)

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Tree(
                shape = shape,
                stage = stage,
                seed = habit.habit.id.hashCode(),
                description = met,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Text(text = met, style = MaterialTheme.typography.titleMedium)
            if (stage > 0) {
                Text(
                    text = stringResource(R.string.habit_tree_stage, stage, TREE_STAGES),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (all.inARow >= 2) {
                Text(
                    text = stringResource(R.string.habit_weeks_in_a_row, all.inARow),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val next = weeksToNextTreeStage(all.weeksMet)
            Text(
                text = when {
                    all.weeksMet == 0 -> stringResource(R.string.habit_tree_empty)
                    next == null -> stringResource(R.string.habit_tree_full)
                    else -> pluralStringResource(R.plurals.habit_tree_next, next, next)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.x1, bottom = Space.x2),
            )
        }
    }
}

/** How long the tree takes to grow from the seed to its stage, when the page opens. */
private const val GROW_MILLIS = 2600

private val Trunk = Color(0xFF8A6A4A)
private val TrunkShade = Color(40, 28, 18, 70)
private val Midrib = Color(0x781E461E)

@Composable
private fun Tree(
    shape: TreeShape,
    stage: Int,
    seed: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    val leaf = MaterialTheme.featherColors.leafColor(rememberSeason())
    val still = rememberReduceMotion()
    // a phone that is struggling shows half of the leaves, a little larger
    val thin = rememberLowEndMode()

    // 0 to 1: how far the tree has grown toward its stage
    val grown = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(stage, still) {
        if (still) grown.snapTo(1f) else grown.animateTo(1f, tween(GROW_MILLIS, easing = FastOutSlowInEasing))
    }

    Canvas(modifier.semantics { contentDescription = description }) {
        drawTree(shape, TreeLook(stage * grown.value), seed, leaf, thin)
    }
}

/** One leaf, 1 long, its foot at the origin and its tip straight up (y is negative). */
private val LeafPath: Path = Path().apply {
    val half = 0.36f
    for (k in 0..8) {
        val t = k / 8f
        val x = sin(PI.toFloat() * t.pow(0.8f)) * half
        if (k == 0) moveTo(x, -t) else lineTo(x, -t)
    }
    for (k in 8 downTo 0) {
        val t = k / 8f
        lineTo(-sin(PI.toFloat() * t.pow(0.8f)) * half * 0.85f, -t)
    }
    close()
}

private fun DrawScope.drawTree(shape: TreeShape, look: TreeLook, seed: Int, leaf: Color, thin: Boolean) {
    // sizes below are in the units of a 250 wide drawing; this is one of them
    val u = size.width / 250f
    val centreX = size.width / 2f
    val groundY = size.height * 0.97f
    val scale = min(size.height * 0.90f / TREE_HEIGHT_ROOM, size.width * 0.47f / shape.halfWidth) * look.size
    fun px(x: Float) = centreX + x * scale
    fun py(y: Float) = groundY - y * scale

    val branches = look.place(shape)

    // the ground, and tufts of grass
    drawOval(
        Trunk.copy(alpha = 0.2f),
        topLeft = Offset(centreX - scale * 0.45f, groundY - 9 * u),
        size = Size(scale * 0.9f, 20 * u),
    )
    val grass = Color(leaf.red * 0.7f, leaf.green * 0.85f, leaf.blue * 0.7f, 0.8f)
    val random = TreeRandom(seed + 1)
    repeat(32) {
        val x = centreX + (random.nextFloat() - 0.5f) * scale * 0.85f
        val height = (8 + random.nextFloat() * 14) * u
        val bend = (random.nextFloat() - 0.5f) * 8 * u
        drawLine(grass, Offset(x, groundY + 3 * u), Offset(x + bend, groundY + 3 * u - height), strokeWidth = 2 * u, cap = StrokeCap.Round)
    }

    // the roots: a few thick arms that thin to a point, flowing out of the foot of the trunk
    val rootGrow = 0.45f + 0.55f * look.roots
    for ((side, reach) in TREE_ROOTS) {
        val outline = Path()
        val steps = 10
        for (k in 0..steps) {
            val t = k / steps.toFloat()
            val x = side * reach * rootGrow * t
            val y = 0.035f * (1 - t).pow(1.5f) * (0.6f + 0.4f * rootGrow) - 0.004f * t
            val w = branches[0].width * TREE_ROOT_WIDTH * look.girth(0) * (1 - t).pow(0.9f) * rootGrow + 0.0015f
            if (k == 0) outline.moveTo(px(x), py(y + w / 2)) else outline.lineTo(px(x), py(y + w / 2))
        }
        for (k in steps downTo 0) {
            val t = k / steps.toFloat()
            val x = side * reach * rootGrow * t
            val y = 0.035f * (1 - t).pow(1.5f) * (0.6f + 0.4f * rootGrow) - 0.004f * t
            val w = branches[0].width * TREE_ROOT_WIDTH * look.girth(0) * (1 - t).pow(0.9f) * rootGrow + 0.0015f
            outline.lineTo(px(x), py(y - w / 2))
        }
        outline.close()
        drawPath(outline, Trunk)
    }

    // the trunk and the branches, each a curved strip that thins toward its tip
    val steps = 14
    val left = FloatArray((steps + 1) * 2)
    val right = FloatArray((steps + 1) * 2)
    branches.forEachIndexed { index, b ->
        val g = look.branchGrowth(b.depth)
        if (g <= 0.001f) return@forEachIndexed
        for (k in 0..steps) {
            val t = g * k / steps
            var width = b.widthAt(t)
            if (index == 0) {
                val flare = max(0f, 1f - t / 0.2f)
                width *= 1f + flare * flare
            }
            width *= look.girth(b.depth) * (0.5f + 0.5f * g)
            val a = b.angle(t)
            val nx = kotlin.math.cos(a)
            val ny = -sin(a)
            val x = b.x(t)
            val y = b.y(t)
            left[k * 2] = px(x - nx * width / 2)
            left[k * 2 + 1] = py(y - ny * width / 2)
            right[k * 2] = px(x + nx * width / 2)
            right[k * 2 + 1] = py(y + ny * width / 2)
        }
        val body = Path()
        val shade = Path()
        for (k in 0..steps) {
            if (k == 0) body.moveTo(left[0], left[1]) else body.lineTo(left[k * 2], left[k * 2 + 1])
        }
        for (k in steps downTo 0) body.lineTo(right[k * 2], right[k * 2 + 1])
        body.close()
        drawPath(body, Trunk)
        // the side away from the light is a little darker
        for (k in 0..steps) {
            val x = left[k * 2] * 0.35f + right[k * 2] * 0.65f
            val y = left[k * 2 + 1] * 0.35f + right[k * 2 + 1] * 0.65f
            if (k == 0) shade.moveTo(x, y) else shade.lineTo(x, y)
        }
        for (k in steps downTo 0) shade.lineTo(right[k * 2], right[k * 2 + 1])
        shade.close()
        drawPath(shade, TrunkShade)
        if (b.depth >= 2) {
            val footWidth = b.widthAt(0f) * look.girth(b.depth) * (0.5f + 0.5f * g)
            drawCircle(Trunk, radius = footWidth * scale / 2, center = Offset(px(b.x(0f)), py(b.y(0f))))
        }
    }

    // the leaves
    val leafLength = scale * look.leafSize * (if (thin) 1.3f else 1f)
    shape.spots.forEachIndexed { index, spot ->
        if (thin && index % 2 == 1) return@forEachIndexed
        val b = branches[spot.branch]
        val g = look.branchGrowth(b.depth)
        val growth = look.leafGrowth(spot, g)
        if (growth <= 0f) return@forEachIndexed
        val length = leafLength * spot.size * growth
        val k = 0.78f + 0.3f * spot.shade
        drawLeaf(
            Offset(px(b.x(spot.along * g)), py(b.y(spot.along * g))),
            length,
            spot.angle,
            Color(min(1f, leaf.red * k), min(1f, leaf.green * k), min(1f, leaf.blue * k)),
        )
    }

    // before the first week: a sprout with two small leaves
    if (look.share < 0.04f) {
        val trunk = branches[0]
        val g = look.branchGrowth(0)
        val tip = Offset(px(trunk.x(g)), py(trunk.y(g)))
        for (turn in floatArrayOf(-0.7f, 0.7f)) drawLeaf(tip, leafLength * 0.9f, turn, leaf)
    }
}

/** A leaf [length] long with its foot at [at], turned [angle] radians clockwise from straight up. */
private fun DrawScope.drawLeaf(at: Offset, length: Float, angle: Float, color: Color) {
    withTransform({
        translate(at.x, at.y)
        rotate(angle * 180f / PI.toFloat(), Offset.Zero)
        scale(length, length, Offset.Zero)
    }) {
        drawPath(LeafPath, color)
        drawLine(Midrib, Offset.Zero, Offset(0f, -0.92f), strokeWidth = 0.05f)
    }
}

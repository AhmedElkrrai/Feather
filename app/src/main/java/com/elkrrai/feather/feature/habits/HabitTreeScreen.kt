package com.elkrrai.feather.feature.habits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
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
import com.elkrrai.feather.domain.habit.TREE_MAX_LEAVES
import com.elkrrai.feather.domain.habit.TreeShape
import com.elkrrai.feather.domain.habit.WeekProgress
import com.elkrrai.feather.domain.habit.overview
import com.elkrrai.feather.domain.habit.treeBranchGrowth
import com.elkrrai.feather.domain.habit.treeGirth
import com.elkrrai.feather.domain.habit.treeLeafGrowth
import com.elkrrai.feather.domain.habit.treeShape
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.ui.firstDayOfWeek
import com.elkrrai.feather.ui.rememberLowEndMode
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.theme.leafColor
import com.elkrrai.feather.ui.theme.rememberSeason
import java.time.LocalDate
import kotlin.math.hypot

/**
 * The tree of one habit: a leaf for each week the goal was met, and the tree grows as the weeks add up.
 * It grows from a seed when the page opens. A leaf opens its week on the weekly page.
 */
@Composable
fun HabitTreeScreen(
    onBack: () -> Unit,
    onOpenWeek: (habitId: String, weekStart: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitTreeViewModel = hiltViewModel(),
) {
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    HabitTreeContent(
        habit = habit,
        onBack = onBack,
        onOpenWeek = { week -> habit?.let { onOpenWeek(it.habit.id, week) } },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HabitTreeContent(
    habit: HabitState?,
    onBack: () -> Unit,
    onOpenWeek: (LocalDate) -> Unit,
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
        val currentWeek = weekStartOf(today, firstDay)
        val all = remember(habit, currentWeek, firstDay) { habit.overview(currentWeek, firstDay) }
        // the leaves: the weeks that were met, oldest first (the newest when there are more than the tree has room for)
        val metWeeks = remember(all) { all.weeks.filter { it.met }.takeLast(TREE_MAX_LEAVES) }
        val shape = remember(habit.habit.id) { treeShape(habit.habit.id.hashCode()) }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Tree(
                shape = shape,
                metWeeks = metWeeks,
                totalMet = all.weeksMet,
                currentWeek = currentWeek,
                onOpenWeek = onOpenWeek,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Spacer(Modifier.height(Space.x2))
            Text(
                text = pluralStringResource(R.plurals.habit_tree_met, all.weeksMet, all.weeksMet),
                style = MaterialTheme.typography.titleMedium,
            )
            if (all.inARow >= 2) {
                Text(
                    text = stringResource(R.string.habit_weeks_in_a_row, all.inARow),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(if (all.weeksMet == 0) R.string.habit_tree_empty else R.string.habit_tree_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.x2),
            )
            Spacer(Modifier.height(Space.x4))
        }
    }
}

/** How close a tap has to be to a leaf to open it. */
private val TAP_RADIUS = 30.dp

/** How long the tree takes to grow from nothing to what it is, when the page opens. */
private const val GROW_MILLIS = 1800

private val Trunk = Color(0xFF8A6A4A)

@Composable
private fun Tree(
    shape: TreeShape,
    metWeeks: List<WeekProgress>,
    totalMet: Int,
    currentWeek: LocalDate,
    onOpenWeek: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaf = MaterialTheme.featherColors.leafColor(rememberSeason())
    val still = rememberReduceMotion() || rememberLowEndMode()

    // 0 to 1: how far the tree has grown toward what it is now
    val grown = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(totalMet, still) {
        if (still) grown.snapTo(1f) else grown.animateTo(1f, tween(GROW_MILLIS, easing = FastOutSlowInEasing))
    }
    val transition = rememberInfiniteTransition(label = "treeBreath")
    val breathing by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val breath = if (still) 0.5f else breathing
    val description = pluralStringResource(R.plurals.habit_tree_met, totalMet, totalMet)

    Canvas(
        modifier
            .semantics { contentDescription = description }
            .pointerInput(shape, metWeeks, totalMet) {
                detectTapGestures { tap ->
                    val place = TreePlace(size.width.toFloat(), size.height.toFloat(), shape)
                    val full = totalMet.toFloat()
                    var best = -1
                    var bestDistance = TAP_RADIUS.toPx()
                    for (i in 0 until minOf(metWeeks.size, shape.spots.size)) {
                        val distance = hypot(tap.x - place.leafAt(i, full).x, tap.y - place.leafAt(i, full).y)
                        if (distance <= bestDistance) {
                            best = i
                            bestDistance = distance
                        }
                    }
                    if (best >= 0) onOpenWeek(metWeeks[best].weekStart)
                }
            },
    ) {
        val place = TreePlace(size.width, size.height, shape)
        val weeks = totalMet * grown.value

        // the ground the tree stands on
        drawOval(
            Trunk.copy(alpha = 0.22f),
            topLeft = Offset(place.centreX - place.scale * 0.32f, place.groundY - 5.dp.toPx()),
            size = Size(place.scale * 0.64f, 10.dp.toPx()),
        )

        val girth = treeGirth(weeks)
        shape.branches.forEach { branch ->
            val growth = treeBranchGrowth(branch.depth, weeks)
            if (growth <= 0f) return@forEach
            val from = place.point(branch.x0, branch.y0)
            val to = place.point(branch.x0 + (branch.x1 - branch.x0) * growth, branch.y0 + (branch.y1 - branch.y0) * growth)
            drawLine(
                color = Trunk,
                start = from,
                end = to,
                strokeWidth = (branch.width * place.scale * girth * (0.4f + 0.6f * growth)).coerceAtLeast(1.5.dp.toPx()),
                cap = StrokeCap.Round,
            )
        }

        val leafLength = maxOf(place.scale * LEAF_SIZE, 10.dp.toPx())
        val count = minOf(metWeeks.size, shape.spots.size)
        for (i in 0 until count) {
            val growth = treeLeafGrowth(i, weeks)
            if (growth <= 0f) continue
            val at = place.leafAt(i, weeks)
            val tint = leaf.copy(alpha = 0.78f + 0.2f * ((i * 37) % 10) / 10f)
            // the leaf of this week, if it was met, glows, slowly
            val thisWeek = metWeeks[i].weekStart == currentWeek
            if (thisWeek && growth >= 1f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(leaf.copy(alpha = 0.25f + 0.3f * breath), Color.Transparent),
                        center = at,
                        radius = leafLength * 1.4f,
                    ),
                    radius = leafLength * 1.4f,
                    center = at,
                )
            }
            drawLeaf(at, leafLength * growth, Math.toDegrees(shape.spots[i].angle.toDouble()).toFloat(), tint)
        }

        // before the first met week: a sprout with two small leaves
        if (totalMet == 0) {
            val trunk = shape.branches[0]
            val sprout = treeBranchGrowth(0, 0f)
            val tip = place.point(trunk.x0 + (trunk.x1 - trunk.x0) * sprout, trunk.y0 + (trunk.y1 - trunk.y0) * sprout)
            drawLeaf(tip, leafLength * 0.7f, -40f, leaf.copy(alpha = 0.8f))
            drawLeaf(tip, leafLength * 0.7f, 40f, leaf.copy(alpha = 0.8f))
        }
    }
}

/** A leaf is this part of the tree's height long. */
private const val LEAF_SIZE = 0.055f

/** Where the unit tree stands on a canvas [width] by [height]: its foot, and how many pixels one unit is. */
private class TreePlace(width: Float, height: Float, private val shape: TreeShape) {
    val centreX: Float = width / 2f
    val groundY: Float = height * 0.94f
    val scale: Float = minOf(height * 0.86f, width * 0.46f / shape.halfWidth)

    fun point(x: Float, y: Float) = Offset(centreX + x * scale, groundY - y * scale)

    /** Where leaf number [index] sits once [weeks] weeks were met. */
    fun leafAt(index: Int, weeks: Float): Offset {
        val spot = shape.spots[index]
        val branch = shape.branches[spot.branch]
        val growth = treeBranchGrowth(branch.depth, weeks) * spot.along
        return point(branch.x0 + (branch.x1 - branch.x0) * growth, branch.y0 + (branch.y1 - branch.y0) * growth)
    }
}

/** One leaf, its foot at [at], [length] long, turned [degrees] clockwise from pointing up. */
private fun DrawScope.drawLeaf(at: Offset, length: Float, degrees: Float, color: Color) {
    val half = length * 0.34f
    val shapePath = Path().apply {
        moveTo(0f, 0f)
        quadraticTo(half * 1.6f, -length * 0.45f, 0f, -length)
        quadraticTo(-half * 1.6f, -length * 0.45f, 0f, 0f)
        close()
    }
    withTransform({
        translate(at.x, at.y)
        rotate(degrees, Offset.Zero)
    }) {
        drawPath(shapePath, color)
    }
}

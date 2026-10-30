package com.elkrrai.feather.feature.habits

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitOverview
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.WeekProgress
import com.elkrrai.feather.domain.habit.overview
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.ui.firstDayOfWeek
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.theme.leafColor
import com.elkrrai.feather.ui.theme.rememberSeason
import java.time.LocalDate
import java.time.ZoneId

/** Every week of one habit at once: a few quiet numbers, and a small leaf for each week. */
@Composable
fun HabitOverviewScreen(
    onBack: () -> Unit,
    onOpenWeek: (habitId: String, weekStart: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitOverviewViewModel = hiltViewModel(),
) {
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    HabitOverviewContent(
        habit = habit,
        onBack = onBack,
        onOpenWeek = { week -> habit?.let { onOpenWeek(it.habit.id, week) } },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HabitOverviewContent(
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
        val all = remember(habit, today, firstDay) { habit.overview(weekStartOf(today, firstDay), firstDay) }
        Garden(habit = habit, overview = all, onOpenWeek = onOpenWeek, contentPadding = padding)
    }
}

@Composable
private fun Garden(
    habit: HabitState,
    overview: HabitOverview,
    onOpenWeek: (LocalDate) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val leaf = MaterialTheme.featherColors.leafColor(rememberSeason())
    val scheme = MaterialTheme.colorScheme
    val unit = habit.habit.unit

    LazyVerticalGrid(
        columns = GridCells.Adaptive(44.dp),
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = Space.gutter,
            end = Space.gutter,
            top = contentPadding.calculateTopPadding() + Space.x2,
            bottom = Space.x8,
        ),
        horizontalArrangement = Arrangement.spacedBy(Space.x2),
        verticalArrangement = Arrangement.spacedBy(Space.x2),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.x1)) {
                Text(
                    text = stringResource(R.string.habit_overview_met, overview.weeksMet, overview.weeksSoFar),
                    style = MaterialTheme.typography.titleLarge,
                )
                val weeks = overview.weeks.size
                Text(
                    text = if (habit.habit.kind == HabitKind.DAYS) {
                        stringResource(R.string.habit_overview_total_days, overview.total)
                    } else {
                        stringResource(
                            R.string.habit_overview_total_amount,
                            overview.total,
                            unit,
                            if (weeks > 0) overview.total / weeks else 0,
                        ).replace("  ", " ").trim()
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
                overview.best?.let { best ->
                    val range = weekRange(context, best.weekStart, zone)
                    Text(
                        text = if (habit.habit.kind == HabitKind.DAYS) {
                            stringResource(R.string.habit_overview_best_days, best.total, range)
                        } else {
                            stringResource(R.string.habit_overview_best_amount, best.total, unit, range).replace("  ", " ")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                if (overview.inARow >= 2) {
                    Text(
                        text = stringResource(R.string.habit_weeks_in_a_row, overview.inARow),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Space.x3))
                Text(
                    text = stringResource(R.string.habit_garden_legend),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.x2))
            }
        }

        itemsIndexed(overview.weeks, key = { _, week -> week.weekStart.toString() }) { index, week ->
            val before = overview.weeks.getOrNull(index - 1)?.goal
            val changed = before != null && week.goal != null &&
                (before.target != week.goal.target || before.dailyTarget != week.goal.dailyTarget)
            val range = weekRange(context, week.weekStart, zone)
            MiniLeaf(
                week = week,
                goalChanged = changed,
                leaf = leaf,
                dot = scheme.onSurface,
                description = if (habit.habit.kind == HabitKind.DAYS) {
                    stringResource(R.string.habit_garden_week_days, range, week.total, week.target)
                } else {
                    stringResource(R.string.habit_garden_week_amount, range, week.total, week.target, unit).replace("  ", " ")
                },
                onClick = { onOpenWeek(week.weekStart) },
            )
        }
    }
}

private fun weekRange(context: android.content.Context, start: LocalDate, zone: ZoneId): String =
    DateUtils.formatDateRange(
        context,
        start.atStartOfDay(zone).toInstant().toEpochMilli(),
        start.plusDays(6).atStartOfDay(zone).toInstant().toEpochMilli(),
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH,
    )

/**
 * One week as a small leaf. A week that reached its goal is a full leaf with a soft light; a week that
 * did not is a leaf filled as far as it got, and never red. A small dot says the goal changed that week.
 * Tapping opens the week on the weekly page.
 */
@Composable
private fun MiniLeaf(
    week: WeekProgress,
    goalChanged: Boolean,
    leaf: Color,
    dot: Color,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val height = size.height * 0.84f
            val top = size.height * 0.04f
            val base = top + height
            val half = size.width * 0.40f
            val cx = size.width / 2f
            val outline = Path().apply {
                val steps = 32
                for (step in 0..steps) {
                    val t = step / steps.toFloat()
                    val point = Offset(cx + half * leafWidth(t), base - height * t)
                    if (step == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                for (step in steps downTo 0) {
                    val t = step / steps.toFloat()
                    lineTo(cx - half * leafWidth(t), base - height * t)
                }
                close()
            }
            if (week.goal != null) {
                if (week.met) {
                    // a soft light round a week that reached its goal
                    for (layer in 1..3) {
                        drawPath(outline, leaf.copy(alpha = 0.10f * (1f - layer / 4f)), style = Stroke(width = (layer * 4).dp.toPx()))
                    }
                }
                val fill = if (week.met) 0.85f else 0.10f + 0.50f * week.fraction
                drawPath(
                    outline,
                    Brush.verticalGradient(listOf(leaf.copy(alpha = fill * 0.7f), leaf.copy(alpha = fill)), startY = base - height, endY = base),
                )
                drawPath(outline, leaf.copy(alpha = if (week.met) 0.95f else 0.45f), style = Stroke(width = 1.5.dp.toPx()))
                drawLine(leaf.copy(alpha = 0.6f), Offset(cx, base), Offset(cx, top + height * 0.08f), 1.dp.toPx())
                drawLine(leaf.copy(alpha = 0.8f), Offset(cx, base), Offset(cx + 2.dp.toPx(), size.height), 1.5.dp.toPx())
                if (goalChanged) {
                    drawCircle(dot, radius = 3.dp.toPx(), center = Offset(size.width - 5.dp.toPx(), top + 4.dp.toPx()))
                }
            }
        }
    }
}

package com.elkrrai.feather.feature.habits

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.graphics.drawscope.withTransform
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
import com.elkrrai.feather.domain.habit.DAYS_IN_WEEK
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
    // each week's small leaf has the shape of the habit's own leaf; the art is made once, not for each week
    val style = leafStyleFor(habit.habit)
    val art = remember(style) { leafArtFor(style) }

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
                style = style,
                art = art,
                onClick = { onOpenWeek(week.weekStart) },
            )
        }
    }
}

private fun weekRange(context: android.content.Context, start: LocalDate, zone: ZoneId): String =
    DateUtils.formatDateRange(
        context,
        start.atStartOfDay(zone).toInstant().toEpochMilli(),
        // the last moment of the last day: a range that ends at midnight is shown without that day
        start.plusDays(DAYS_IN_WEEK.toLong()).atStartOfDay(zone).toInstant().toEpochMilli() - 1,
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
    style: LeafStyle,
    art: LeafArt?,
    onClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (week.goal != null) {
            // The weekly leaf itself, made on its own 328 by 470 board and scaled down to the cell, so the small leaf is a true miniature:
            // the same shape, veins and fill. A week that met its goal is at full strength and glows; a week that did not is the fainter
            // the less of its goal was reached, and never red.
            val fit = minOf(maxWidth / 328.dp, maxHeight / 470.dp)
            val strength = if (week.met) 1f else 0.30f + 0.45f * week.fraction.coerceIn(0f, 1f)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WeekLeaf(
                    week = week,
                    today = LocalDate.now(),
                    modifier = Modifier
                        .requiredSize(328.dp, 470.dp)
                        .graphicsLayer {
                            scaleX = fit
                            scaleY = fit
                            alpha = strength
                        },
                    style = style,
                    preview = true,
                    lineBoost = 3.5f,
                    precomputedArt = art,
                )
            }
            if (goalChanged) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(dot, radius = 3.dp.toPx(), center = Offset(size.width - 5.dp.toPx(), size.height * 0.04f + 4.dp.toPx()))
                }
            }
        }
    }
}

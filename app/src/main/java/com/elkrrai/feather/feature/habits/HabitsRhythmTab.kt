package com.elkrrai.feather.feature.habits

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.history
import com.elkrrai.feather.domain.habit.weeksMetInARow
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.components.EmptyState
import com.elkrrai.feather.ui.theme.Space
import java.time.LocalDate

/** How many weeks the Habits tab of Your rhythm looks back. */
private const val WEEKS_SHOWN = 8

/**
 * The Habits tab of Your rhythm: for each habit the last eight weeks, one bar a week that rises with
 * how much of that week's goal was reached (a week that met it is the full colour), and a quiet line
 * about weeks in a row. A week that was not met is only a shorter bar: no red, no "you missed".
 */
@Composable
fun HabitsRhythmTab(
    habits: List<HabitState>?,
    weekStart: LocalDate,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    when {
        habits == null -> Box(modifier.fillMaxSize().padding(contentPadding))

        habits.isEmpty() -> Box(modifier.fillMaxSize().padding(contentPadding)) {
            EmptyState(
                title = stringResource(R.string.habits_rhythm_empty_title),
                body = stringResource(R.string.habits_rhythm_empty_body),
                mood = BirdMood.IDLE,
            )
        }

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.gutter,
                end = Space.gutter,
                top = contentPadding.calculateTopPadding() + Space.x2,
                bottom = contentPadding.calculateBottomPadding() + Space.x8,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            items(habits, key = { it.habit.id }) { state -> HabitHistoryCard(state, weekStart) }
        }
    }
}

@Composable
private fun HabitHistoryCard(state: HabitState, weekStart: LocalDate) {
    val scheme = MaterialTheme.colorScheme
    val weeks = state.history(weekStart, WEEKS_SHOWN)
    val existed = weeks.filter { it.goal != null }
    val met = existed.count { it.met }
    val inARow = state.weeksMetInARow(weekStart)
    val summary = stringResource(R.string.habit_history_summary, met, existed.size)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier.border(androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant), RoundedCornerShape(20.dp)),
    ) {
        Column(Modifier.padding(Space.x4)) {
            Text(text = state.habit.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.x3))
            val track = scheme.surfaceVariant
            val full = scheme.primary
            val partial = scheme.primary.copy(alpha = 0.45f)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = summary },
            ) {
                val gap = 6.dp.toPx()
                val barWidth = (size.width - gap * (weeks.size - 1)) / weeks.size
                val radius = CornerRadius(barWidth / 3f)
                weeks.forEachIndexed { index, week ->
                    val x = index * (barWidth + gap)
                    if (week.goal == null) return@forEachIndexed
                    drawRoundRect(track, topLeft = Offset(x, 0f), size = Size(barWidth, size.height), cornerRadius = radius)
                    if (week.fraction > 0f) {
                        val height = maxOf(size.height * week.fraction, 4.dp.toPx())
                        drawRoundRect(
                            color = if (week.met) full else partial,
                            topLeft = Offset(x, size.height - height),
                            size = Size(barWidth, height),
                            cornerRadius = radius,
                        )
                    }
                }
            }
            if (inARow >= 2) {
                Spacer(Modifier.height(Space.x3))
                Text(
                    text = stringResource(R.string.habit_weeks_in_a_row, inARow),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

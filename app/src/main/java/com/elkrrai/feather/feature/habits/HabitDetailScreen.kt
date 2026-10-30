package com.elkrrai.feather.feature.habits

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.coveringFrom
import com.elkrrai.feather.domain.habit.firstWeekStart
import com.elkrrai.feather.domain.habit.week
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.ui.firstDayOfWeek
import com.elkrrai.feather.ui.theme.Space
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One habit looked back on week by week: a page for each week, each a leaf of seven days. */
@Composable
fun HabitDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    HabitDetailContent(habit = habit, initialWeek = viewModel.initialWeek, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HabitDetailContent(
    habit: HabitState?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialWeek: LocalDate? = null,
    today: LocalDate = LocalDate.now(),
) {
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
        WeeksPager(habit = habit, today = today, initialWeek = initialWeek, modifier = Modifier.padding(padding))
    }
}

@Composable
private fun WeeksPager(habit: HabitState, today: LocalDate, initialWeek: LocalDate?, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val firstDay = firstDayOfWeek(locale)
    val currentStart = weekStartOf(today, firstDay)

    // From the week the habit began to this week; a goal counts from the first page on.
    val firstStart = remember(habit, firstDay) { habit.firstWeekStart(currentStart, firstDay) }
    val pageCount = (ChronoUnit.WEEKS.between(firstStart, currentStart) + 1).toInt().coerceAtLeast(1)
    // This week, or the week a tap in the all-time view asked for.
    val startPage = initialWeek
        ?.let { ChronoUnit.WEEKS.between(firstStart, weekStartOf(it, firstDay)).toInt().coerceIn(0, pageCount - 1) }
        ?: (pageCount - 1)
    val pager = rememberPagerState(initialPage = startPage) { pageCount }
    // The first goal also covers a first page that starts a little before it (a changed week start).
    val shown = remember(habit, firstStart) { habit.coveringFrom(firstStart) }
    val zone = remember { ZoneId.systemDefault() }

    HorizontalPager(state = pager, modifier = modifier.fillMaxSize()) { page ->
        val weekStart = firstStart.plusWeeks(page.toLong())
        val progress = shown.week(weekStart)
        val range = DateUtils.formatDateRange(
            context,
            weekStart.atStartOfDay(zone).toInstant().toEpochMilli(),
            weekStart.plusDays(6).atStartOfDay(zone).toInstant().toEpochMilli(),
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(start = Space.gutter, end = Space.gutter, bottom = Space.x6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.x2),
        ) {
            Spacer(Modifier.height(Space.x4))
            Text(text = range, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                text = if (weekStart == currentStart) stringResource(R.string.habit_this_week) else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(20.dp),
            )
            Spacer(Modifier.height(Space.x2))
            // the leaf takes all the room between the header and the summary
            WeekLeaf(week = progress, today = today, modifier = Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(Space.x2))

            val goal = progress.goal
            Text(
                text = if (habit.habit.kind == HabitKind.DAYS) {
                    stringResource(R.string.habit_progress_days, progress.total, progress.target)
                } else {
                    stringResource(R.string.habit_progress_amount, progress.total, progress.target, habit.habit.unit).trim()
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (goal != null) {
                Text(
                    text = when {
                        habit.habit.kind == HabitKind.AMOUNT ->
                            stringResource(R.string.habit_goal_amount, goal.target, habit.habit.unit).trim()
                        goal.dailyTarget > 1 -> stringResource(R.string.habit_goal_days_times, goal.target, goal.dailyTarget)
                        else -> stringResource(R.string.habit_goal_days, goal.target)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (progress.met) {
                Text(
                    text = stringResource(R.string.habit_week_met),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

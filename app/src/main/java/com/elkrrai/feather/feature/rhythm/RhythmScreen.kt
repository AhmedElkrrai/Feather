package com.elkrrai.feather.feature.rhythm

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.rhythm.RhythmStats
import com.elkrrai.feather.domain.rhythm.SteadyTask
import com.elkrrai.feather.domain.rhythm.TimeOfDay
import com.elkrrai.feather.domain.rhythm.rhythmStats
import com.elkrrai.feather.ui.brand.BirdIllustration
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.firstDayOfWeek
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.theme.DayPhase
import com.elkrrai.feather.ui.theme.Ease
import com.elkrrai.feather.ui.theme.LightFeatherColors
import com.elkrrai.feather.ui.theme.NestShapes
import com.elkrrai.feather.ui.theme.NightSkyText
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.today.dayPhaseFor
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.feature.habits.HabitsRhythmTab
import com.elkrrai.feather.feature.habits.HabitsViewModel
import com.elkrrai.feather.domain.rhythm.WeeklyFeather
import com.elkrrai.feather.domain.rhythm.isFeatherDay
import com.elkrrai.feather.domain.rhythm.weeklyFeather

@Composable
fun RhythmScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RhythmViewModel = hiltViewModel(),
    habitsViewModel: HabitsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val habits by habitsViewModel.habits.collectAsStateWithLifecycle()
    RhythmContent(
        state = state,
        habits = habits,
        onBack = onBack,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        modifier = modifier,
    )
}

/**
 * Your rhythm: a gentle look back, in the tone of a journal. There are no percentages against last
 * week, no red, no "you missed" and no streak that can break; a day with nothing is a day of
 * rest. Everything is worked out on the phone from the record of finished tasks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhythmContent(
    state: RhythmUiState,
    onBack: () -> Unit,
    onPause: (days: Int) -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    /** The habits for the second tab; null while they load. */
    habits: List<HabitState>? = emptyList(),
) {
    // 0 is the tasks (everything this screen was), 1 the habits.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember { ZoneId.systemDefault() }
    val firstDay = firstDayOfWeek(locale)
    val stats = remember(state.records, state.pausedDays, today, firstDay) {
        rhythmStats(state.records, today, zone, firstDay, state.pausedDays)
    }
    val feather = remember(state.records, state.weeklyFeather, today, firstDay) {
        if (state.weeklyFeather && isFeatherDay(today, firstDay)) weeklyFeather(state.records, today, zone, firstDay) else null
    }
    val entry = rememberEntry()
    val haptics = rememberHaptics()
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var showPause by remember { mutableStateOf(false) }
    val paused = state.pausedDays.any { !it.isBefore(today) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.rhythm_title), style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                        }
                    },
                )
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.rhythm_tab_tasks)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.rhythm_tab_habits)) })
                }
            }
        },
    ) { padding ->
        if (tab == 1) {
            HabitsRhythmTab(habits = habits, weekStart = weekStartOf(today, firstDay), contentPadding = padding)
            return@Scaffold
        }
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.gutter,
                end = Space.gutter,
                top = padding.calculateTopPadding() + Space.x2,
                bottom = padding.calculateBottomPadding() + Space.x8,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            item(key = "hero") { RhythmHero(stats, entry) }

            // On the first day of the week: the feather made from the week that just ended.
            if (feather != null) {
                item(key = "feather") { WeeklyFeatherCard(feather) }
            }

            if (stats.isNew) {
                item(key = "new") {
                    SectionCard(title = stringResource(R.string.rhythm_new_title)) {
                        Text(
                            text = stringResource(R.string.rhythm_new_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Space.x4),
                        )
                    }
                }
            } else {
                item(key = "week") {
                    SectionCard(title = stringResource(R.string.rhythm_last_seven)) {
                        val resources = LocalContext.current.resources
                        val summary = stringResource(
                            R.string.rhythm_last_seven_summary,
                            stats.lastSeven.joinToString(", ") {
                                resources.getString(
                                    R.string.rhythm_day_count,
                                    it.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                                    it.count,
                                )
                            },
                        )
                        Column(
                            Modifier.padding(horizontal = Space.x4).semantics { contentDescription = summary },
                        ) {
                            WeekBars(stats.lastSeven, entry)
                            WeekdayLabels(stats.lastSeven.map { it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) })
                        }
                    }
                }

                item(key = "dots") {
                    SectionCard(title = stringResource(R.string.rhythm_dots_title)) {
                        val headline = stringResource(R.string.rhythm_dots_headline, stats.dotsShowedUp, stats.dots.size)
                        Column(Modifier.padding(horizontal = Space.x4)) {
                            Text(
                                text = headline,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(Space.x2))
                            RhythmDots(stats.dots, entry, Modifier.semantics { contentDescription = headline })
                            Text(
                                text = stringResource(R.string.rhythm_dots_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (paused) {
                                Text(
                                    text = stringResource(R.string.rhythm_pause_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Space.x2),
                                )
                            }
                            OutlinedButton(
                                onClick = { if (paused) onResume() else showPause = true },
                                modifier = Modifier.padding(top = Space.x3).heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(if (paused) R.string.rhythm_resume else R.string.rhythm_pause))
                            }
                        }
                    }
                }

                item(key = "month") {
                    SectionCard(title = monthTitle(stats.month.month, locale)) {
                        MonthSection(stats, entry, locale, firstDay) { day ->
                            haptics.tick()
                            selectedDay = day
                        }
                    }
                }

                item(key = "when") {
                    SectionCard(title = stringResource(R.string.rhythm_when_title)) {
                        WhenSection(stats, entry, locale)
                    }
                }

                if (stats.steady.isNotEmpty()) {
                    item(key = "steady") {
                        SectionCard(title = stringResource(R.string.rhythm_steady_title)) {
                            Column(Modifier.padding(horizontal = Space.x4), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
                                Text(
                                    text = stringResource(R.string.rhythm_steady_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                stats.steady.forEach { SteadyRow(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedDay?.let { day ->
        DaySheet(day, stats, locale, onDismiss = { selectedDay = null })
    }
    if (showPause) {
        PauseDialog(
            onPick = { days ->
                showPause = false
                onPause(days)
            },
            onDismiss = { showPause = false },
        )
    }
}

private fun monthTitle(month: java.time.YearMonth, locale: Locale): String =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "LLLLyyyy"), locale).format(month)

/** A rounded card on the paper with a title, the way every chart on this screen sits. */
@Composable
private fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NestShapes.large)
            .background(MaterialTheme.featherColors.card)
            .padding(vertical = Space.x4),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = Space.x4, end = Space.x4, bottom = Space.x3)
                .semantics { heading() },
        )
        content()
    }
}

/**
 * The top of the screen: how many tasks were finished in the last seven days, one kind sentence
 * and the bird, on a sky that follows the time of day. For someone new, a dashed egg waits where
 * the numbers will be.
 */
@Composable
private fun RhythmHero(stats: RhythmStats, entry: Animatable<Float, AnimationVector1D>) {
    val feather = MaterialTheme.featherColors
    val phase = remember { dayPhaseFor(LocalTime.now()) }
    val (skyTop, skyBottom) = feather.sky.getValue(phase)
    val ink = if (phase == DayPhase.Night && feather.skyText == LightFeatherColors.skyText) NightSkyText else feather.skyText
    val locale = LocalConfiguration.current.locales[0]
    val numbers = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val shown = (stats.weekTotal * Ease.Settle.transform(segment(entry.value, 0f, 600f))).roundToInt()
    val sentence = stringResource(
        when {
            stats.weekTotal == 0 -> R.string.rhythm_hero_none
            stats.weekTotal < 5 -> R.string.rhythm_hero_few
            stats.weekTotal < 15 -> R.string.rhythm_hero_steady
            else -> R.string.rhythm_hero_busy
        }
    )
    val label = stringResource(R.string.rhythm_hero_label)
    val summary = "${numbers.format(stats.weekTotal)} $label. $sentence"

    Box(
        Modifier
            .fillMaxWidth()
            .clip(NestShapes.extraLarge)
            .background(Brush.verticalGradient(listOf(skyTop, skyBottom)))
            .semantics(mergeDescendants = true) { contentDescription = summary },
    ) {
        Row(Modifier.padding(Space.x6), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = numbers.format(shown),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp, lineHeight = 66.sp, fontFeatureSettings = "tnum"),
                    color = ink,
                    maxLines = 1,
                )
                Text(text = label, style = MaterialTheme.typography.labelLarge, color = ink.copy(alpha = 0.85f))
                Text(
                    text = sentence,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ink,
                    modifier = Modifier.padding(top = Space.x2),
                )
            }
            Spacer(Modifier.width(Space.x3))
            if (stats.isNew) {
                DashedEgg(ink.copy(alpha = 0.6f))
            } else {
                BirdIllustration(Modifier.size(92.dp), mood = BirdMood.IDLE)
            }
        }
    }
}

/** An egg drawn with a dashed line: the place where the first one will appear. */
@Composable
private fun DashedEgg(color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(56.dp, 72.dp).semantics { }) {
        drawOval(
            color = color,
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))),
        )
    }
}

/** The feather of the week that just ended, with a sentence about it. */
@Composable
private fun WeeklyFeatherCard(feather: WeeklyFeather) {
    val color = MaterialTheme.featherColors.amberFill
    val body = stringResource(R.string.weekly_feather_body_plain, feather.total, feather.restDays)
    SectionCard(title = stringResource(R.string.weekly_feather_title)) {
        Row(
            modifier = Modifier.padding(horizontal = Space.x4).semantics(mergeDescendants = true) { contentDescription = body },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.x4),
        ) {
            WeeklyFeatherView(feather, color)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthSection(
    stats: RhythmStats,
    entry: Animatable<Float, AnimationVector1D>,
    locale: Locale,
    firstDay: DayOfWeek,
    onDay: (LocalDate) -> Unit,
) {
    val grid = stats.month
    val monthName = monthTitle(grid.month, locale)
    val daysWithTasks = grid.cells.count { it.count > 0 }
    val finished = grid.cells.sumOf { it.count }
    val summary = stringResource(R.string.rhythm_month_summary, monthName, daysWithTasks, finished)
    val resources = LocalContext.current.resources
    val dayLabel = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMd"), locale) }
    val actions = grid.cells.filter { it.count > 0 && !it.isFuture }.map { cell ->
        val text = dayLabel.format(cell.date) + ", " +
            resources.getQuantityString(R.plurals.rhythm_finished_on_day, cell.count, cell.count)
        CustomAccessibilityAction(text) {
            onDay(cell.date)
            true
        }
    }
    val labels = (0 until 7).map { offset ->
        DayOfWeek.of((firstDay.value - 1 + offset) % 7 + 1).getDisplayName(TextStyle.NARROW, locale)
    }
    Column(
        Modifier
            .padding(horizontal = Space.x2)
            .semantics {
                contentDescription = summary
                customActions = actions
            },
    ) {
        WeekdayLabels(labels)
        MonthNest(grid, entry, onDay)
    }
}

@Composable
private fun WhenSection(stats: RhythmStats, entry: Animatable<Float, AnimationVector1D>, locale: Locale) {
    val labels = listOf(
        stringResource(R.string.rhythm_time_morning),
        stringResource(R.string.rhythm_time_afternoon),
        stringResource(R.string.rhythm_time_evening),
        stringResource(R.string.rhythm_time_night),
    )
    val busiest = stats.busiestWeekday
    val best = stats.bestTime
    Column(Modifier.padding(horizontal = Space.x4), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
        if (busiest != null) {
            Text(
                text = stringResource(R.string.rhythm_busiest, busiest.getDisplayName(TextStyle.FULL, locale)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (best != null) {
            val part = labels[TimeOfDay.entries.indexOf(best)].lowercase(locale)
            val sentence = stringResource(R.string.rhythm_best_time, part)
            Text(text = sentence, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeOfDayBars(
                times = stats.timeOfDay,
                best = best,
                labels = labels,
                entry = entry,
                modifier = Modifier.padding(top = Space.x2).semantics { contentDescription = sentence },
            )
        }
    }
}

@Composable
private fun SteadyRow(task: SteadyTask) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        LeafDays(task.lastSeven, Modifier.width(124.dp))
    }
}

/** What was finished on one day of the month, in a sheet from the bottom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(day: LocalDate, stats: RhythmStats, locale: Locale, onDismiss: () -> Unit) {
    val records = stats.byDay[day].orEmpty()
    val title = remember(day, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale).format(day)
    }
    val resources = LocalContext.current.resources
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Space.gutter, end = Space.gutter, bottom = Space.x6),
            verticalArrangement = Arrangement.spacedBy(Space.x2),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            if (records.isEmpty()) {
                Text(
                    text = stringResource(R.string.rhythm_day_sheet_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = resources.getQuantityString(R.plurals.rhythm_finished_on_day, records.size, records.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                records.forEach { record ->
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = Space.x1),
                    )
                }
            }
        }
    }
}

@Composable
private fun PauseDialog(onPick: (days: Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rhythm_pause_title)) },
        text = {
            Column {
                listOf(
                    R.string.rhythm_pause_today to 1,
                    R.string.rhythm_pause_three to 3,
                    R.string.rhythm_pause_week to 7,
                ).forEach { (label, days) ->
                    TextButton(onClick = { onPick(days) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(label))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

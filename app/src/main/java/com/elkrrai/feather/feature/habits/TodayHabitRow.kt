package com.elkrrai.feather.feature.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.StageMode
import com.elkrrai.feather.domain.habit.StageState
import com.elkrrai.feather.domain.habit.TodayHabit
import com.elkrrai.feather.ui.components.MetaChip
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Pebble
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.theme.leafColor
import com.elkrrai.feather.ui.theme.rememberSeason
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a card is held to finish the day. */
private const val HOLD_MILLIS = 700

/** How long a finished row stays in the list, done, before it leaves. */
private const val LINGER_MILLIS = 1_600L

/**
 * The habits of Today, in a block above the tasks. A habit is finished by pressing and holding its card, never by a tap, so a
 * stray touch cannot do it. A row that was just finished stays a moment, done, then leaves.
 */
@Composable
fun TodayHabitsBlock(
    ui: TodayHabitsUi,
    onFinish: (TodayHabit) -> Unit,
    onToggleStage: (TodayHabit, String) -> Unit,
    onSetAmount: (TodayHabit, Int) -> Unit,
    modifier: Modifier = Modifier,
    /** A day was finished (by holding, by the last tick, or by an amount that reaches the goal): where on the screen the ring is. */
    onCelebrate: (origin: Offset) -> Unit = {},
    today: LocalDate = LocalDate.now(),
) {
    val scope = rememberCoroutineScope()
    val lingering = remember { mutableStateListOf<String>() }
    val open = remember { mutableSetOf<String>() }
    // a row is kept after it turns done only if it was seen open: a day that was finished earlier does not come back as a row
    LaunchedEffect(ui.rows) {
        for (row in ui.rows) {
            if (!row.doneToday) {
                open += row.id
            } else if (open.remove(row.id)) {
                scope.launch {
                    lingering += row.id
                    delay(LINGER_MILLIS)
                    lingering -= row.id
                }
            }
        }
    }
    val visible = ui.rows.filter { !it.doneToday || it.id in lingering }
    if (visible.isEmpty()) return

    val locale = LocalConfiguration.current.locales[0]
    var amountFor by remember { mutableStateOf<String?>(null) }
    Column(modifier.padding(horizontal = Space.gutter)) {
        Text(
            text = stringResource(R.string.today_habits_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = Space.x2).semantics { heading() },
        )
        visible.forEachIndexed { index, row ->
            // each row keeps its own hold and bloom when the rows change places
            key(row.id) {
                HabitTodayRow(
                    row = row,
                    showHint = ui.showHoldHint && index == 0,
                    onFinish = { onFinish(row) },
                    onToggleStage = { onToggleStage(row, it) },
                    onPickAmount = { amountFor = row.id },
                    onCelebrate = onCelebrate,
                )
                Spacer(Modifier.height(Space.x2))
            }
        }
    }
    visible.firstOrNull { it.id == amountFor }?.let { row ->
        AmountDialog(
            title = stringResource(
                R.string.habit_amount_title,
                row.unit.ifBlank { row.name },
                today.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
            ),
            initial = row.todayAmount,
            onSet = { amount ->
                onSetAmount(row, amount)
                amountFor = null
            },
            onDismiss = { amountFor = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitTodayRow(
    row: TodayHabit,
    showHint: Boolean,
    onFinish: () -> Unit,
    onToggleStage: (stageId: String) -> Unit,
    onPickAmount: () -> Unit,
    onCelebrate: (Offset) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val leaf = MaterialTheme.featherColors.leafColor(rememberSeason())
    var ringCenter by remember { mutableStateOf(Offset.Zero) }
    val celebrate by rememberUpdatedState(onCelebrate)
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val still = rememberReduceMotion()
    val done = row.doneToday
    val amountHabit = row.kind == HabitKind.AMOUNT

    // how far the card has been held (0 to 1), how the leaf blooms and how the burst of small leaves spreads
    val progress = remember { Animatable(if (done) 1f else 0f) }
    val bloom = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    var tip by remember { mutableStateOf(false) }
    LaunchedEffect(tip) {
        if (tip) {
            delay(1_500)
            tip = false
        }
    }
    LaunchedEffect(done) {
        if (done) {
            // however the day was finished, the bird and the feathers celebrate, as for a task
            celebrate(ringCenter)
            if (!still) {
                scope.launch { burst.snapTo(0f); burst.animateTo(1f, tween(650)) }
                bloom.snapTo(0.6f)
                scope.launch { progress.animateTo(1f, tween(150)) }
                bloom.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f))
            } else {
                progress.snapTo(1f)
            }
        } else if (progress.value > 0f) {
            progress.animateTo(0f, tween(250))
        }
    }

    // the hold runs in a gesture that outlives recompositions: it must call the latest finish, with the row as it is now
    val finish by rememberUpdatedState(onFinish)
    val markDone = stringResource(R.string.habit_today_mark_done)
    val gesture = if (amountHabit) {
        Modifier.clickable(onClick = onPickAmount)
    } else {
        Modifier.pointerInput(row.id, done) {
            if (done) return@pointerInput
            awaitEachGesture {
                // a child that takes the touch (a stage chip) keeps it: only the card itself starts a hold
                awaitFirstDown()
                val started = System.currentTimeMillis()
                val hold = scope.launch {
                    var nextTick = 0.25f
                    progress.animateTo(1f, tween(HOLD_MILLIS, easing = LinearEasing)) {
                        if (value >= nextTick) {
                            haptics.tick()
                            nextTick += 0.25f
                        }
                    }
                    haptics.confirm()
                    finish()
                }
                val up = waitForUpOrCancellation()
                if (hold.isActive) {
                    // let go early, or the finger slid away to scroll: everything eases back
                    hold.cancel()
                    val quick = System.currentTimeMillis() - started < 220
                    scope.launch {
                        if (up != null && quick) progress.animateTo(0.24f, tween(110))
                        progress.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f))
                    }
                    // a tap or a short press teaches the gesture
                    if (up != null) tip = true
                }
            }
        }
    }

    val title = if (row.mode == StageMode.ORDERED && row.current != null) "${row.name}: ${row.current.stage.name}" else row.name
    val holdDescription = stringResource(R.string.habit_today_row_desc, title)
    Box {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = leaf.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, leaf.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val scale = 1f - 0.018f * progress.value
                    scaleX = scale
                    scaleY = scale
                }
                .alpha(if (done) 0.82f else 1f)
                .semantics {
                    contentDescription = if (amountHabit || done) title else holdDescription
                    if (!amountHabit && !done) customActions = listOf(CustomAccessibilityAction(markDone) { onFinish(); true })
                }
                .then(gesture),
        ) {
            Box(
                Modifier.drawBehind {
                    // the wash spreads from the leaf while the card is held
                    if (progress.value > 0f) {
                        drawCircle(
                            leaf.copy(alpha = 0.30f),
                            radius = progress.value * (size.width + size.height),
                            center = Offset(32.dp.toPx(), 31.dp.toPx()),
                        )
                    }
                },
            ) {
                Row(
                    Modifier.padding(horizontal = Space.x3, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(Space.x3),
                ) {
                    HoldRing(
                        progress = { progress.value },
                        bloom = { bloom.value },
                        burst = { burst.value },
                        done = done,
                        leaf = leaf,
                        modifier = Modifier.onGloballyPositioned { ringCenter = it.boundsInRoot().center },
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.x1)) {
                        Text(title, style = MaterialTheme.typography.bodyLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (row.current?.state == StageState.NOW) {
                                MetaChip(stringResource(R.string.habit_chip_now), leaf, Color(0xFF0B1409))
                            }
                            if (row.time.isSet) MetaChip(describeTime(row.time), scheme.surfaceContainerHigh, scheme.onSurfaceVariant)
                            MetaChip(progressText(row), scheme.surfaceContainerHigh, scheme.onSurfaceVariant)
                        }
                        if (row.stages.isNotEmpty() && !done) {
                            if (row.mode == StageMode.ALL) {
                                Column {
                                    row.stages.forEach { stage ->
                                        ChecklistLine(stage.stage.name, describeTime(stage.stage.time).takeIf { stage.stage.time.isSet }, stage.state == StageState.DONE, leaf) {
                                            onToggleStage(stage.stage.id)
                                        }
                                    }
                                }
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(top = Space.x1),
                                ) {
                                    row.stages.forEach { stage ->
                                        StageChip(stage.stage.name, stage.state, leaf) { onToggleStage(stage.stage.id) }
                                    }
                                }
                            }
                        }
                        if (showHint && !done) {
                            Text(
                                text = stringResource(R.string.habit_today_hold_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Space.x1),
                            )
                        }
                        if (done) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                androidx.compose.material3.Icon(
                                    painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = leaf,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    stringResource(R.string.habit_today_done),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = leaf,
                                )
                            }
                        }
                    }
                }
            }
        }
        // a small label that says how to finish, after a tap or a hold that was let go too soon
        AnimatedVisibility(
            visible = tip && !done,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        ) {
            Surface(shape = CircleShape, color = leaf) {
                Text(
                    stringResource(R.string.habit_today_hold_tip),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0B1409),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/** The ring around the leaf: its track says "this can be filled", the arc is the hold, and the leaf blooms when the day is done. */
@Composable
private fun HoldRing(
    progress: () -> Float,
    bloom: () -> Float,
    burst: () -> Float,
    done: Boolean,
    leaf: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(40.dp)) {
        val stroke = 2.5.dp.toPx()
        val radius = size.minDimension / 2 - stroke
        drawCircle(leaf.copy(alpha = 0.30f), radius, style = Stroke(stroke))
        val held = progress()
        if (held > 0f && !done) {
            drawArc(
                color = leaf,
                startAngle = -90f,
                sweepAngle = 360f * held,
                useCenter = false,
                topLeft = Offset(stroke, stroke),
                size = Size(radius * 2, radius * 2),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        // the leaf: the same size done or not, filled when done
        val scale = bloom()
        val w = 18.dp.toPx() * scale
        val h = 22.dp.toPx() * scale
        val c = center
        val path = Path().apply {
            moveTo(c.x - w / 2, c.y + h / 2)
            quadraticTo(c.x - w / 2, c.y - h / 2, c.x + w / 2, c.y - h / 2)
            quadraticTo(c.x + w / 2, c.y + h / 2, c.x - w / 2, c.y + h / 2)
            close()
        }
        if (done) drawPath(path, leaf) else drawPath(path, leaf, style = Stroke(2.dp.toPx()))
        val spread = burst()
        if (spread < 1f) {
            repeat(8) { i ->
                val angle = i * (2 * PI / 8).toFloat()
                val distance = size.minDimension / 2 + 24.dp.toPx() * spread
                drawCircle(
                    leaf.copy(alpha = 1f - spread),
                    radius = 2.5.dp.toPx(),
                    center = Offset(c.x + cos(angle) * distance, c.y + sin(angle) * distance),
                )
            }
        }
    }
}

/** One stage as a small chip: a dot (the same size in every state), the name; tapping ticks or unticks it. */
@Composable
private fun StageChip(name: String, state: StageState, leaf: Color, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val amber = MaterialTheme.featherColors.amberFill
    val (container, line, content) = when (state) {
        StageState.DONE -> Triple(leaf.copy(alpha = 0.20f), leaf.copy(alpha = 0.55f), scheme.onSurface)
        StageState.MISSED -> Triple(Color.Transparent, amber.copy(alpha = 0.6f), amber)
        StageState.NOW -> Triple(Color.Transparent, leaf, scheme.onSurface)
        StageState.NEXT -> Triple(Color.Transparent, scheme.outlineVariant, scheme.onSurfaceVariant)
    }
    val description = stringResource(if (state == StageState.DONE) R.string.habit_stage_state_done else R.string.habit_stage_state_open, name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(Pebble)
            .background(container)
            .drawBehind {
                // a missed stage has a dashed outline: it is not wrong, only past
                drawRoundRect(
                    color = line,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    style = Stroke(
                        1.dp.toPx(),
                        pathEffect = if (state == StageState.MISSED) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null,
                    ),
                )
            }
            .semantics { contentDescription = description }
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Dot(filled = state == StageState.DONE, color = if (state == StageState.DONE) leaf else content)
        Text(name, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

/** A round mark of one size, filled or an outline, so a done stage is as big as one that is not. */
@Composable
private fun Dot(filled: Boolean, color: Color) {
    Box(
        Modifier
            .size(9.dp)
            .then(if (filled) Modifier.background(color, CircleShape) else Modifier.border(1.5.dp, color, CircleShape)),
    )
}

/** A stage as a line of a checklist: a box, the name, and its time. */
@Composable
private fun ChecklistLine(name: String, time: String?, checked: Boolean, leaf: Color, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(5.dp))
                .then(if (checked) Modifier.background(leaf) else Modifier.border(1.5.dp, leaf, RoundedCornerShape(5.dp))),
        )
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (time != null) Text(time, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun progressText(row: TodayHabit): String = when {
    row.stages.isNotEmpty() -> stringResource(R.string.habit_today_progress_stages, row.doneCount, row.totalCount)
    row.kind == HabitKind.AMOUNT -> stringResource(R.string.habit_today_progress_amount, row.weekTotal, row.weekTarget, row.unit).trim()
    row.totalCount > 1 -> stringResource(R.string.habit_today_progress_times, row.doneCount, row.totalCount)
    else -> stringResource(R.string.habit_today_progress_days, row.weekTotal, row.weekTarget)
}

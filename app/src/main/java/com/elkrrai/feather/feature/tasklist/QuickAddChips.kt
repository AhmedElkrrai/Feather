package com.elkrrai.feather.feature.tasklist

import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.quickadd.QuickAddField
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Pebble

/** What a recognized part of the quick-add text shows as: which part it is, and its friendly words. */
internal data class QuickAddChip(val field: QuickAddField, val label: String)

/** The colors a recognized part is tinted with, in the chip and in the text field. */
internal class FieldTint(val container: Color, val content: Color)

internal fun fieldTint(field: QuickAddField, scheme: ColorScheme): FieldTint = when (field) {
    QuickAddField.DATE, QuickAddField.TIME, QuickAddField.REPEAT ->
        FieldTint(scheme.secondaryContainer, scheme.onSecondaryContainer)

    QuickAddField.PRIORITY -> FieldTint(scheme.tertiaryContainer, scheme.onTertiaryContainer)
    QuickAddField.TAGS, QuickAddField.LIST -> FieldTint(scheme.primaryContainer, scheme.onPrimaryContainer)
    QuickAddField.COUNTER -> FieldTint(scheme.tertiaryContainer, scheme.onTertiaryContainer)
}

@DrawableRes
private fun QuickAddField.iconRes(): Int? = when (this) {
    QuickAddField.DATE -> R.drawable.ic_calendar
    QuickAddField.TIME -> R.drawable.ic_clock
    QuickAddField.REPEAT -> R.drawable.ic_repeat
    QuickAddField.PRIORITY -> R.drawable.ic_flag
    QuickAddField.TAGS -> R.drawable.ic_tag
    QuickAddField.LIST -> null
    QuickAddField.COUNTER -> R.drawable.ic_counter
}

private const val CHIP_STAGGER_MILLIS = 40
private const val CHIP_IN_MILLIS = 160
private const val CHIP_OUT_MILLIS = 120
private const val REDUCED_MILLIS = 80

/** A little overshoot at the end, so a chip pops into place. */
private val PopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** Chips closer together than this do not each get a tick. */
internal const val TICK_GAP_MILLIS = 400L

/**
 * Whether a new chip earns a light tick: there are more chips than before and the last tick was at
 * least [TICK_GAP_MILLIS] ago.
 */
internal fun chipTickDue(previousCount: Int, count: Int, now: Long, lastTick: Long): Boolean =
    count > previousCount && now - lastTick >= TICK_GAP_MILLIS

/** How long a chip waits before popping in, so several arriving together come one after another. */
internal fun chipDelayMillis(order: Int): Int = order.coerceAtLeast(0) * CHIP_STAGGER_MILLIS

private class Remembered<T>(var value: T)

/**
 * The row of chips under the text: one for each part of the text that was understood. A chip
 * pops in when its part is recognized and shrinks away when it stops being recognized; tapping it
 * leaves those words in the title ([onIgnore]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickAddChips(
    chips: List<QuickAddChip>,
    onIgnore: (QuickAddField) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val haptics = rememberHaptics()
    var previousCount by remember { mutableIntStateOf(0) }
    var lastTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(chips.map { it.field }) {
        val now = SystemClock.elapsedRealtime()
        if (chipTickDue(previousCount, chips.size, now, lastTick)) {
            haptics.tick()
            lastTick = now
        }
        previousCount = chips.size
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickAddField.entries.forEach { field ->
            val chip = chips.firstOrNull { it.field == field }
            ChipSlot(
                chip = chip,
                order = chips.indexOfFirst { it.field == field },
                reduceMotion = reduceMotion,
                onIgnore = onIgnore,
            )
        }
    }
}

@Composable
private fun ChipSlot(
    chip: QuickAddChip?,
    order: Int,
    reduceMotion: Boolean,
    onIgnore: (QuickAddField) -> Unit,
) {
    // The chip keeps its words while it shrinks away, after its part has stopped being recognized.
    val last = remember { Remembered<QuickAddChip?>(null) }
    if (chip != null) last.value = chip

    AnimatedVisibility(
        visible = chip != null,
        enter = if (reduceMotion) {
            fadeIn(tween(REDUCED_MILLIS))
        } else {
            val delay = chipDelayMillis(order)
            fadeIn(tween(CHIP_IN_MILLIS, delayMillis = delay)) +
                scaleIn(tween(CHIP_IN_MILLIS, delayMillis = delay, easing = PopEasing), initialScale = 0.7f)
        },
        exit = if (reduceMotion) {
            fadeOut(tween(REDUCED_MILLIS))
        } else {
            fadeOut(tween(CHIP_OUT_MILLIS)) + scaleOut(tween(CHIP_OUT_MILLIS), targetScale = 0.8f)
        },
    ) {
        last.value?.let { TokenChip(it, onIgnore) }
    }
}

/** A 36dp pill: an icon, the friendly words, and a small cross; tapping it ignores that part. */
@Composable
private fun TokenChip(chip: QuickAddChip, onIgnore: (QuickAddField) -> Unit) {
    val tint = fieldTint(chip.field, MaterialTheme.colorScheme)
    val ignoreLabel = stringResource(R.string.quick_add_ignore, chip.label)
    Row(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(Pebble)
            .background(tint.container)
            .clickable(onClickLabel = ignoreLabel, role = Role.Button) { onIgnore(chip.field) }
            .padding(start = 12.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chip.field.iconRes()?.let { icon ->
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = tint.content,
            )
        }
        Text(
            text = chip.label,
            style = MaterialTheme.typography.labelLarge,
            color = tint.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = ignoreLabel,
            modifier = Modifier.size(16.dp),
            tint = tint.content,
        )
    }
}

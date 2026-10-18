package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.components.moonShape
import com.elkrrai.feather.ui.theme.Nest
import com.elkrrai.feather.ui.theme.NestShapes
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.today.DayCard
import com.elkrrai.feather.ui.today.DayCardKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The small card at the top of Today: in the morning a look at the day ("Your day", what is
 * planned, what is waiting, what comes first) and after 7 PM a wind-down ("You finished 4 things
 * today. Two are still waiting. Nothing is lost. Want them tomorrow?"). It can be put away, and it
 * shows once a day.
 */
@Composable
fun DayCardView(
    card: DayCard,
    onDismiss: () -> Unit,
    onMoveToTomorrow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val morning = card.kind == DayCardKind.Morning
    val title = stringResource(if (morning) R.string.day_card_morning_title else R.string.day_card_evening_title)
    val body = buildString {
        if (morning) {
            append(resources.getQuantityString(R.plurals.day_card_morning_count, card.left, card.left))
            if (card.waiting > 0) append(' ').append(resources.getQuantityString(R.plurals.day_card_waiting, card.waiting, card.waiting))
            card.firstTitle?.let { append(' ').append(resources.getString(R.string.day_card_first_up, it)) }
        } else {
            if (card.done > 0) append(resources.getQuantityString(R.plurals.day_card_evening_done, card.done, card.done))
            if (card.left > 0) {
                if (isNotEmpty()) append(' ')
                append(resources.getQuantityString(R.plurals.day_card_evening_left, card.left, card.left))
            } else {
                if (isNotEmpty()) append(' ')
                append(resources.getString(R.string.day_card_evening_rest))
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x4, vertical = Space.x2)
            .clip(NestShapes.large)
            .background(MaterialTheme.featherColors.card)
            .padding(start = Space.x4, top = Space.x4, end = Space.x2, bottom = Space.x2),
        horizontalArrangement = Arrangement.spacedBy(Space.x3),
    ) {
        DayCardIcon(morning)
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Space.x1),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!morning && card.left > 0) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.day_card_leave_them))
                    }
                    TextButton(onClick = onMoveToTomorrow, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.day_card_move_to_tomorrow))
                    }
                } else {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(if (morning) R.string.day_card_got_it else R.string.day_card_thanks))
                    }
                }
            }
        }
    }
}

/** A little sun for the morning and a moon for the evening. Decoration only. */
@Composable
private fun DayCardIcon(morning: Boolean) {
    val moon = remember { moonShape() }
    val moonColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(36.dp).clearAndSetSemantics { }) {
        if (morning) {
            scale(size.width / 36f, pivot = Offset.Zero) {
                val center = Offset(18f, 18f)
                drawCircle(Nest.Amber, radius = 8f, center = center)
                for (ray in 0 until 8) {
                    val angle = ray * PI / 4
                    drawLine(
                        color = Nest.Amber,
                        start = Offset(center.x + (cos(angle) * 12f).toFloat(), center.y + (sin(angle) * 12f).toFloat()),
                        end = Offset(center.x + (cos(angle) * 16f).toFloat(), center.y + (sin(angle) * 16f).toFloat()),
                        strokeWidth = 2.4f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        } else {
            scale(size.width, pivot = Offset.Zero) { drawPath(moon, moonColor) }
        }
    }
}

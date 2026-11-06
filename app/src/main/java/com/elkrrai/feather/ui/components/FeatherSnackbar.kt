package com.elkrrai.feather.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.rememberReduceMotion

/** What a message is about: it picks the small icon, and whether the icon wears the amber of a warning or the blue of a plain message. */
enum class SnackKind(@DrawableRes val icon: Int, val warm: Boolean = false, val compact: Boolean = false) {
    Done(R.drawable.ic_check),
    Trash(R.drawable.ic_delete, warm = true),
    Repeat(R.drawable.ic_repeat),
    Moved(R.drawable.ic_calendar),
    /** A habit finished by holding its card: that already celebrated, so the message is smaller. */
    Habit(R.drawable.ic_feather, compact = true),
    Cloud(R.drawable.ic_cloud_upload),
    Alert(R.drawable.ic_alert, warm = true),
}

class FeatherSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
    val kind: SnackKind,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
}

/** Shows a message in the app's own snackbar; returns how it ended (the action pressed, or not). */
suspend fun SnackbarHostState.showFeather(
    message: String,
    kind: SnackKind = SnackKind.Done,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
): SnackbarResult = showSnackbar(FeatherSnackbarVisuals(message, actionLabel, duration, kind))

/** The place messages appear. Put it in the `snackbarHost` of a Scaffold, which lifts it above the add button. */
@Composable
fun FeatherSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = state, modifier = modifier) { data -> FeatherSnackbar(data) }
}

/**
 * A rounded card with a small icon, the message, and an action (Undo) in amber. A thin line along the bottom edge shrinks while the
 * message stays; a tap anywhere on the card closes it early. With reduced motion the line is left out.
 */
@Composable
fun FeatherSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val visuals = data.visuals
    val kind = (visuals as? FeatherSnackbarVisuals)?.kind ?: SnackKind.Done
    val scheme = MaterialTheme.colorScheme
    val tone = if (kind.warm) scheme.secondary else scheme.primary
    val shape = RoundedCornerShape(16.dp)
    val millis = when (visuals.duration) {
        SnackbarDuration.Short -> 4000
        SnackbarDuration.Long -> 10000
        SnackbarDuration.Indefinite -> 0
    }
    val showLine = millis > 0 && !rememberReduceMotion()
    val remaining = remember(data) { Animatable(1f) }
    if (showLine) {
        LaunchedEffect(data) { remaining.animateTo(0f, tween(millis, easing = LinearEasing)) }
    }

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = data::dismiss),
    ) {
        Box {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = if (kind.compact) 9.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val tile = if (kind.compact) 26.dp else 32.dp
                Box(
                    modifier = Modifier
                        .size(tile)
                        .clip(RoundedCornerShape(if (kind.compact) 8.dp else 10.dp))
                        .background(tone.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(kind.icon),
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(if (kind.compact) 15.dp else 18.dp),
                    )
                }
                Text(
                    text = visuals.message,
                    style = if (kind.compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                visuals.actionLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = data::performAction)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }
            if (showLine) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(remaining.value)
                        .height(3.dp)
                        .background(scheme.secondary, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)),
                )
            }
        }
    }
}

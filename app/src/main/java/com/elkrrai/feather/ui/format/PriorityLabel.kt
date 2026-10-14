package com.elkrrai.feather.ui.format

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.ui.theme.featherColors

@Composable
fun Priority.label(): String = stringResource(
    when (this) {
        Priority.NONE -> R.string.priority_none
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
    }
)

/** The color of the completion ring: neutral without a priority, sky, amber or coral with one. */
@Composable
fun Priority.ringColor(): Color {
    val colors = MaterialTheme.featherColors
    return when (this) {
        Priority.NONE -> colors.prioNone
        Priority.LOW -> colors.prioLow
        Priority.MEDIUM -> colors.prioMed
        Priority.HIGH -> colors.prioHigh
    }
}

/** Marker color for a priority, or null when there is nothing to show. */
@Composable
fun Priority.markerColor(): Color? = when (this) {
    Priority.NONE -> null
    Priority.LOW -> MaterialTheme.colorScheme.secondary
    Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
    Priority.HIGH -> MaterialTheme.colorScheme.error
}

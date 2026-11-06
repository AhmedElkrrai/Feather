package com.elkrrai.feather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** A few options in one rounded switch, as wide as its space; the picked one is lit in blue. For a handful of equal views of one screen. */
@Composable
fun SegmentedSwitch(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(scheme.surfaceContainer)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                color = if (on) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (on) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(vertical = 8.dp),
            )
        }
    }
}

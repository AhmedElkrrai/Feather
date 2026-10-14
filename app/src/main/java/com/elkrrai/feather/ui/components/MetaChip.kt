package com.elkrrai.feather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.theme.Pebble

/**
 * A small pill of information on a task: a date, a list, a repeat. 26dp tall and not a touch target
 * (the whole row is). [leading] is an optional 12dp icon or shape before the text.
 */
@Composable
fun MetaChip(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .heightIn(min = 26.dp)
            .background(container, Pebble)
            .padding(start = if (leading != null) 8.dp else 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

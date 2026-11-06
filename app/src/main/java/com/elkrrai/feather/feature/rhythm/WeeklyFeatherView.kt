package com.elkrrai.feather.feature.rhythm

import androidx.compose.ui.unit.Dp
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.rhythm.WeeklyFeather

/** The feather that [activeDays] days with a finished task earn: a quiet week (0) has the bare quill, a week of seven the fullest feather. */
@DrawableRes
internal fun featherDrawable(activeDays: Int): Int = when (activeDays.coerceIn(0, 7)) {
    0 -> R.drawable.feather_days_0
    1 -> R.drawable.feather_days_1
    2 -> R.drawable.feather_days_2
    3 -> R.drawable.feather_days_3
    4 -> R.drawable.feather_days_4
    5 -> R.drawable.feather_days_5
    6 -> R.drawable.feather_days_6
    else -> R.drawable.feather_days_7
}

/**
 * The weekly feather: one of the owl's eight feathers, by how many days of last week had a finished task. It keeps its own colors in every
 * season. The feathers are dark, so a light badge sits behind them, which keeps them in view on the dark card. Decoration; the words next
 * to it carry the meaning.
 */
@Composable
internal fun WeeklyFeatherView(
    feather: WeeklyFeather,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFFE8E3D8), RoundedCornerShape(size / 5))
            .padding(size / 10)
            .clearAndSetSemantics { },
    ) {
        Image(
            painter = painterResource(featherDrawable(feather.activeDays)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

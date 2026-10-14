package com.elkrrai.feather.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Dur
import com.elkrrai.feather.ui.theme.Ease
import kotlinx.coroutines.delay

/** The strike starts a moment after the ring is tapped, so the check lands first. */
private const val STRIKE_DELAY = 100L

/**
 * Where the strike-through is on each line when it has grown to [progress] (0 to 1) of the whole
 * title. Lines are struck one after the other, each from its reading-direction start: the left
 * edge in left-to-right text and the right edge in right-to-left text. Pure geometry, so it is
 * tested without a screen.
 *
 * @param widths the width of each line of text.
 * @return for each line, the fraction of its width that is struck, from 0 to 1.
 */
internal fun strikeFractions(widths: List<Float>, progress: Float): List<Float> {
    val total = widths.sum()
    if (total <= 0f) return widths.map { 0f }
    var before = 0f
    val reach = progress.coerceIn(0f, 1f) * total
    return widths.map { width ->
        val fraction = if (width <= 0f) 0f else ((reach - before) / width).coerceIn(0f, 1f)
        before += width
        fraction
    }
}

/**
 * Text that is struck through with a line that grows across it when [struck] becomes true, and
 * whose color settles from [activeColor] to [doneColor]. Without motion (reduced motion) the change
 * is instant. A title that is already struck when it first appears is drawn struck, without any
 * animation.
 */
@Composable
fun StrikeText(
    text: String,
    struck: Boolean,
    activeColor: Color,
    doneColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(if (struck) 1f else 0f) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(struck, reduceMotion) {
        val target = if (struck) 1f else 0f
        if (progress.value == target) return@LaunchedEffect
        when {
            reduceMotion -> progress.snapTo(target)
            struck -> {
                delay(STRIKE_DELAY)
                progress.animateTo(1f, tween(Dur.Short2, easing = Ease.Soft))
            }
            else -> progress.animateTo(0f, tween(Dur.Micro, easing = Ease.Leave))
        }
    }

    val color by animateColorAsState(
        targetValue = if (struck) doneColor else activeColor,
        animationSpec = if (reduceMotion) snap() else tween(Dur.Short2, delayMillis = STRIKE_DELAY.toInt()),
        label = "titleColor",
    )

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            drawContent()
            val result = layout ?: return@drawWithContent
            if (progress.value <= 0f) return@drawWithContent
            val lineCount = result.lineCount
            val widths = (0 until lineCount).map { line -> result.getLineRight(line) - result.getLineLeft(line) }
            val fractions = strikeFractions(widths, progress.value)
            val thickness = 1.5.dp.toPx()
            for (line in 0 until lineCount) {
                val fraction = fractions[line]
                if (fraction <= 0f) continue
                val left = result.getLineLeft(line)
                val right = result.getLineRight(line)
                val rightToLeft = result.getParagraphDirection(result.getLineStart(line)) == ResolvedTextDirection.Rtl
                val top = result.getLineTop(line)
                val bottom = result.getLineBottom(line)
                // A little above the middle of the line, about half way up the lower case letters.
                val y = result.getLineBaseline(line) - (bottom - top) * 0.22f
                val from = if (rightToLeft) right else left
                val to = if (rightToLeft) right - (right - left) * fraction else left + (right - left) * fraction
                drawLine(
                    color = color,
                    start = Offset(from, y),
                    end = Offset(to, y),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round,
                )
            }
        },
        color = color,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { layout = it },
    )
}

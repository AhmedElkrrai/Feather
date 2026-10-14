package com.elkrrai.feather.feature.tasklist

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.brand.BirdIllustration
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton
import com.elkrrai.feather.ui.components.drawLeaf
import com.elkrrai.feather.ui.components.leafShape
import com.elkrrai.feather.ui.components.moonShape
import com.elkrrai.feather.ui.theme.DayPhase
import com.elkrrai.feather.ui.theme.LightFeatherColors
import com.elkrrai.feather.ui.theme.Nest
import com.elkrrai.feather.ui.theme.NightSkyText
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.today.Greeting
import com.elkrrai.feather.ui.today.TodayStatus
import com.elkrrai.feather.ui.today.dayPhaseFor
import com.elkrrai.feather.ui.today.greetingFor
import com.elkrrai.feather.ui.today.leavesFilled
import com.elkrrai.feather.ui.today.showsMoon
import com.elkrrai.feather.ui.today.todayBirdMood
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The header is this tall (below the status bar) when open, and this tall when scrolled away. */
private val HeaderOpenHeight = 236.dp
private val HeaderClosedHeight = 64.dp

/** How far the header can be pushed up. */
val HeaderRange: Dp = HeaderOpenHeight - HeaderClosedHeight

private val NestLip = 24.dp
private const val LEAF_COUNT = 5

/**
 * How far the Today header has been pushed up by scrolling the list: 0 when open, down to
 * `-rangePx` when closed. The header and the nested scroll connection share it.
 */
@Stable
class HeaderCollapseState(val rangePx: Float) {
    var offset by mutableFloatStateOf(0f)

    /** 0 when open, 1 when closed. */
    val fraction: Float get() = if (rangePx <= 0f) 0f else (-offset / rangePx).coerceIn(0f, 1f)
}

/** The state and the scroll connection that drives it; put the connection on the header's parent. */
class HeaderCollapse(val state: HeaderCollapseState, val connection: NestedScrollConnection)

@Composable
fun rememberHeaderCollapse(): HeaderCollapse {
    val rangePx = with(LocalDensity.current) { HeaderRange.toPx() }
    val state = remember(rangePx) { HeaderCollapseState(rangePx) }
    val connection = remember(state) {
        object : NestedScrollConnection {
            // Scrolling the list up: the header closes first, and the list moves with what is left over.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val before = state.offset
                state.offset = (before + available.y).coerceIn(-state.rangePx, 0f)
                return Offset(0f, state.offset - before)
            }

            // Pulling down once the list is at its top: the header opens again.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val before = state.offset
                state.offset = (before + available.y).coerceIn(-state.rangePx, 0f)
                return Offset(0f, state.offset - before)
            }
        }
    }
    return remember(state, connection) { HeaderCollapse(state, connection) }
}

/**
 * The top of the Today view: a sky that follows the time of day, a greeting, the date, how many
 * things are left, five leaves for how much is done, and the bird on its branch. Its bottom edge
 * is a shallow scoop, the nest's lip. It closes to a 64dp bar as the list scrolls, leaving only the
 * title and the menu, search and sort buttons.
 */
@Composable
fun TodayHeader(
    status: TodayStatus,
    date: LocalDate,
    time: LocalTime,
    collapse: HeaderCollapseState,
    title: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    sortActions: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val feather = MaterialTheme.featherColors
    val phase = remember(time.hour) { dayPhaseFor(time) }
    val (skyTop, skyBottom) = feather.sky.getValue(phase)
    // The night wash is dark even in the light theme, so its text is cream there.
    val ink = if (phase == DayPhase.Night && feather.skyText == LightFeatherColors.skyText) NightSkyText else feather.skyText

    val insetTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val openHeightPx = with(LocalDensity.current) { (insetTop + HeaderOpenHeight).toPx() }
    val lipPx = with(LocalDensity.current) { NestLip.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val height = (openHeightPx + collapse.offset).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .drawBehind {
                // A flat wash with a scooped bottom edge: the middle of the edge is 24dp higher than the corners.
                val lip = lipPx * (1f - collapse.fraction)
                val wash = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    quadraticTo(size.width / 2f, size.height - 2f * lip, 0f, size.height)
                    close()
                }
                drawPath(wash, Brush.verticalGradient(listOf(skyTop, skyBottom)))
            }
            .clipToBounds(),
    ) {
        CompositionLocalProvider(LocalContentColor provides ink) {
            // The bird's scene drifts up at 0.4 times the scroll, shrinks to 0.6 and fades out.
            TodayScene(
                status = status,
                phase = phase,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Space.gutter, bottom = NestLip + Space.x1)
                    .graphicsLayer {
                        val fraction = collapse.fraction
                        translationY = collapse.offset * 0.4f
                        val scale = 1f - 0.4f * fraction
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        alpha = (1f - fraction * 1.25f).coerceIn(0f, 1f)
                    },
            )

            Column(
                modifier = Modifier
                    .padding(start = Space.gutter, top = insetTop + 56.dp)
                    .widthIn(max = 230.dp)
                    .graphicsLayer { alpha = (1f - collapse.fraction * 2f).coerceIn(0f, 1f) },
            ) {
                Text(
                    text = stringResource(greetingFor(time).label()),
                    style = MaterialTheme.typography.displaySmall,
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = rememberDateLine(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.8f),
                )
                Text(
                    text = todayCountLine(status),
                    style = MaterialTheme.typography.titleSmall,
                    color = ink,
                    modifier = Modifier.padding(top = Space.x2),
                )
                DecorativeLeaves(
                    filled = leavesFilled(status, LEAF_COUNT),
                    color = MaterialTheme.colorScheme.primary,
                    outline = ink.copy(alpha = 0.45f),
                    modifier = Modifier.padding(top = Space.x2),
                )
            }

            // The bar: menu, the title (it fades in as the greeting fades out), search, sort.
            Row(
                modifier = Modifier
                    .padding(top = insetTop)
                    .height(56.dp)
                    .fillMaxWidth()
                    .padding(horizontal = Space.x1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.action_open_menu),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { alpha = ((collapse.fraction - 0.5f) * 2f).coerceIn(0f, 1f) },
                )
                IconButton(onClick = onSearchClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.action_search),
                    )
                }
                OverflowButton(actions = sortActions)
            }
        }
    }
}

private fun Greeting.label(): Int = when (this) {
    Greeting.Morning -> R.string.greeting_morning
    Greeting.Afternoon -> R.string.greeting_afternoon
    Greeting.Evening -> R.string.greeting_evening
    Greeting.Night -> R.string.greeting_night
}

/** "Wednesday, October 14" in the app's language and the locale's own order. */
@Composable
private fun rememberDateLine(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd")
        DateTimeFormatter.ofPattern(pattern, locale).format(date)
    }
}

/** "3 things left · 2 done", "All done." or "Nothing planned today". */
@Composable
private fun todayCountLine(status: TodayStatus): String {
    val resources = LocalContext.current.resources
    return when {
        status.planned == 0 -> stringResource(R.string.today_nothing_planned)
        status.left == 0 -> stringResource(R.string.today_all_done)
        status.done == 0 -> resources.getQuantityString(R.plurals.today_left, status.left, status.left)
        else -> resources.getQuantityString(R.plurals.today_left, status.left, status.left) +
            " · " + stringResource(R.string.today_done_count, status.done)
    }
}

/** Five leaves: filled for the share of today that is done. Decoration only. */
@Composable
private fun DecorativeLeaves(filled: Int, color: Color, outline: Color, modifier: Modifier = Modifier) {
    val leaf = remember { leafShape() }
    Row(
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(LEAF_COUNT) { index ->
            Canvas(Modifier.size(16.dp)) {
                scale(this.size.width, pivot = Offset.Zero) {
                    drawLeaf(leaf, if (index < filled) color else outline, filled = index < filled)
                }
            }
        }
    }
}

private val SceneWidth = 150.dp
private val SceneHeight = 110.dp

/**
 * The bird on a branch with a sun or a moon, and eggs on the branch when everything is done. The
 * picture is not mirrored in right-to-left languages (only where it sits is). Decoration only.
 */
@Composable
private fun TodayScene(status: TodayStatus, phase: DayPhase, modifier: Modifier = Modifier) {
    val feather = MaterialTheme.featherColors
    val mood = todayBirdMood(status, phase)
    val moon = showsMoon(phase)
    val moonPath = remember { moonShape() }
    val leaf = remember { leafShape() }
    val eggs = if (status.allDone) 3 else 0

    Box(
        modifier = modifier
            .size(SceneWidth, SceneHeight)
            .clearAndSetSemantics { },
    ) {
        Canvas(Modifier.size(SceneWidth, SceneHeight)) {
            // Everything below is drawn in dp units on a 150 by 110 board.
            scale(size.width / 150f, pivot = Offset.Zero) {
                if (moon) {
                    translateAndScale(124f, 12f, 26f) { drawPath(moonPath, Nest.Cream) }
                } else {
                    drawSun(Offset(128f, 24f), 12f)
                }
                // the branch, a twig and a leaf
                drawLine(feather.twig, Offset(6f, 96f), Offset(142f, 92f), strokeWidth = 7f, cap = StrokeCap.Round)
                drawLine(feather.twig, Offset(104f, 94f), Offset(128f, 80f), strokeWidth = 4f, cap = StrokeCap.Round)
                translateAndScale(124f, 66f, 16f) { drawLeaf(leaf, feather.listColors[3], filled = true) }
                // eggs on the branch
                repeat(eggs) { index ->
                    val x = 10f + index * 13f
                    drawEgg(Offset(x, 84f))
                }
            }
        }
        BirdIllustration(
            modifier = Modifier
                .size(84.dp)
                .offset(x = 36.dp, y = 17.dp),
            mood = mood,
        )
    }
}

/** Draws with the origin moved to ([x], [y]) and a unit box scaled to [size]. */
private fun DrawScope.translateAndScale(x: Float, y: Float, size: Float, block: DrawScope.() -> Unit) {
    androidx.compose.ui.graphics.drawscope.translate(left = x, top = y) {
        scale(size, pivot = Offset.Zero, block = block)
    }
}

private fun DrawScope.drawSun(center: Offset, radius: Float) {
    drawCircle(Nest.Amber, radius = radius, center = center)
    for (ray in 0 until 8) {
        val angle = ray * PI / 4
        drawLine(
            color = Nest.Amber,
            start = Offset(center.x + (cos(angle) * (radius + 4f)).toFloat(), center.y + (sin(angle) * (radius + 4f)).toFloat()),
            end = Offset(center.x + (cos(angle) * (radius + 9f)).toFloat(), center.y + (sin(angle) * (radius + 9f)).toFloat()),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawEgg(topLeft: Offset) {
    drawOval(Nest.Cream, topLeft = topLeft, size = androidx.compose.ui.geometry.Size(11f, 14f))
    drawCircle(Nest.Amber.copy(alpha = 0.5f), radius = 0.9f, center = Offset(topLeft.x + 4f, topLeft.y + 5f))
    drawCircle(Nest.Coral.copy(alpha = 0.5f), radius = 0.8f, center = Offset(topLeft.x + 7f, topLeft.y + 9f))
}

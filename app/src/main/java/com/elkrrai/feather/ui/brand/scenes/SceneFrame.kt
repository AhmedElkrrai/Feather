package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.brand.BirdIllustration
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.brand.OwlDress
import com.elkrrai.feather.ui.brand.rememberBirdMotionAllowed
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Ease
import kotlinx.coroutines.delay
import com.elkrrai.feather.ui.brand.rememberAmbientPhase

/** The scenes of the illustration kit (see `Illustration`). */
enum class SceneKind {
    EmptyList,
    ClearSkies,
    NothingDue,
    NoResults,
    BackupSaved,
    RemindersBlocked,
    FirstLaunch,
    TrashEmpty,
}

/** What a scene's drawing needs: the scale, the colors and the two animated values. */
internal class SceneScope(
    /** How many dp one unit of the 200-unit-wide scene board is. */
    val unit: Dp,
    val palette: ScenePalette,
    /** The sway of leaves and feathers, in degrees; 0 when nothing moves. */
    val sway: () -> Float,
    /** How far the first-launch sun has climbed, in board units. */
    val sunRise: () -> Float,
)

private const val ENTRANCE_MILLIS = 320
private const val SWAY_MILLIS = 5000
private const val SUN_DELAY_MILLIS = 200L
private const val SUN_MILLIS = 600
private val ENTRANCE_RISE = 12.dp

/**
 * The frame every scene sits in: [size] wide and [height] high, faded in and lifted 12dp over
 * 320ms (a plain 160ms fade with reduced motion), with the sway and sunrise running when motion is
 * allowed. Scenes are decoration: their sentence underneath says what they mean.
 */
@Composable
internal fun SceneFrame(
    size: Dp,
    modifier: Modifier = Modifier,
    height: Dp = size,
    risingSun: Boolean = false,
    content: @Composable SceneScope.() -> Unit,
) {
    val palette = rememberScenePalette()
    val reduceMotion = rememberReduceMotion()
    val moving = rememberBirdMotionAllowed()

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        entrance.animateTo(1f, tween(if (reduceMotion) 160 else ENTRANCE_MILLIS, easing = Ease.Settle))
    }

    // The sway, like the bird, goes on while motion is allowed.
    val swayPhase = rememberAmbientPhase(SWAY_MILLIS, enabled = moving)

    val sunProgress = remember { Animatable(if (risingSun) 0f else 1f) }
    LaunchedEffect(risingSun, reduceMotion) {
        if (risingSun && !reduceMotion) {
            sunProgress.snapTo(0f)
            delay(SUN_DELAY_MILLIS)
            sunProgress.animateTo(1f, tween(SUN_MILLIS, easing = Ease.Settle))
        } else {
            sunProgress.snapTo(1f)
        }
    }

    val scope = remember(size, palette, swayPhase) {
        SceneScope(
            unit = size / SCENE_BOARD,
            palette = palette,
            sway = { swayDegrees(swayPhase.value) },
            sunRise = { sunRise(sunProgress.value) },
        )
    }

    Box(
        modifier = modifier
            .size(width = size, height = height)
            .graphicsLayer {
                alpha = entrance.value
                translationY = (1f - entrance.value) * ENTRANCE_RISE.toPx()
            }
            .clearAndSetSemantics { },
    ) {
        scope.content()
    }
}

/** A drawing layer over the whole scene; [draw] works in board units. */
@Composable
internal fun SceneScope.Layer(draw: DrawScope.() -> Unit) {
    Canvas(Modifier.fillMaxSize()) {
        scale(size.width / SCENE_BOARD, pivot = Offset.Zero) { draw() }
    }
}

/** The live, animated bird, placed on the board by [at]. */
@Composable
internal fun SceneScope.SceneBird(mood: BirdMood, at: BirdPlacement, dress: OwlDress = OwlDress.None) {
    BirdIllustration(
        modifier = Modifier
            .offset(x = unit * at.x, y = unit * at.y)
            .size(unit * at.side)
            .graphicsLayer { rotationZ = at.tilt },
        mood = mood,
        dress = dress,
    )
}

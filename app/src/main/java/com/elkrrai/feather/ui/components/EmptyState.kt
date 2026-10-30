package com.elkrrai.feather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.brand.BirdIllustration
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.brand.scenes.ClearSkiesScene
import com.elkrrai.feather.ui.brand.scenes.Illustration
import com.elkrrai.feather.ui.brand.scenes.SceneKind
import com.elkrrai.feather.ui.brand.scenes.sceneSizeDp

/**
 * Centered empty-state layout: one picture, one sentence ([title]) and, if it helps, a line of
 * detail ([body]). The picture is a [scene] from the illustration kit, shown at a size that
 * follows the font size and left out when the screen is too short for it; without a scene it is
 * the bird in [mood].
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    mood: BirdMood = BirdMood.IDLE,
    scene: SceneKind? = null,
) {
    if (scene == SceneKind.ClearSkies) {
        // The sky fills the screen, with the sentence written on it near the top.
        BoxWithConstraints(modifier.fillMaxSize().padding(SKY_MARGIN)) {
            if (maxHeight >= SKY_MIN_HEIGHT) ClearSkiesScene(width = maxWidth, height = maxHeight)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 36.dp, start = 28.dp, end = 28.dp),
            )
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp, start = 28.dp, end = 28.dp),
                )
            }
        }
        return
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(if (scene == SceneKind.HungryChick) WIDE_SCENE_MARGIN else 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        val fontScale = LocalDensity.current.fontScale
        val fits = scene?.let { sceneSizeDp(maxHeight.value, fontScale) }
        // The nest of hungry chicks uses the whole width, as far as the height allows room for its words.
        val sceneSize = if (scene == SceneKind.HungryChick && fits != null) {
            minOf(maxWidth.value, maxHeight.value - WIDE_SCENE_WORDS_ROOM.value)
        } else {
            fits
        }
        Column(
            modifier = if (scene == SceneKind.EmptyList) Modifier.offset(y = (-50).dp) else Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                scene == null -> {
                    BirdIllustration(mood = mood)
                    Spacer(Modifier.height(24.dp))
                }

                sceneSize != null -> {
                    Illustration(scene, size = sceneSize.dp)
                    Spacer(Modifier.height(24.dp))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            if (body != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The sky of an empty Overdue view leaves this much of the screen's edge, and is left out when shorter than this. */
private val SKY_MARGIN = 10.dp
private val SKY_MIN_HEIGHT = 320.dp

/** The nest of hungry chicks leaves this much at the sides, and keeps this much height for its words. */
private val WIDE_SCENE_MARGIN = 2.dp
private val WIDE_SCENE_WORDS_ROOM = 120.dp

package com.elkrrai.feather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        val fontScale = LocalDensity.current.fontScale
        val sceneSize = scene?.let { sceneSizeDp(maxHeight.value, fontScale) }
        Column(
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

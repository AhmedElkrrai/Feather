package com.elkrrai.feather.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.brand.BirdCanvas
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.brand.birdPose
import com.elkrrai.feather.ui.brand.periodMillis
import com.elkrrai.feather.ui.theme.FeatherTheme

/**
 * A debug-only screen for looking at the bird: every mood at once on one clock, a switch to
 * stop the clock and scrub a single phase by hand, a speed choice, light or dark ground, and the
 * bird at the sizes the icon and notification use. Start it with
 * `adb shell am start -n com.elkrrai.feather/.debug.BirdLabActivity`.
 */
class BirdLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by remember { mutableStateOf(true) }
            FeatherTheme(darkTheme = dark, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                    BirdLab(dark = dark, onDarkChange = { dark = it })
                }
            }
        }
    }
}

private val speeds = listOf(0.1f, 0.25f, 0.5f, 1f)

@Composable
private fun BirdLab(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    var playing by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    var scrub by remember { mutableFloatStateOf(0f) }
    var clockMillis by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playing, speed) {
        if (!playing) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) clockMillis += (now - last) / 1_000_000f * speed
                last = now
            }
        }
    }

    // Playing: each mood runs on its own period. Paused: every mood shows the scrubbed phase.
    fun phaseOf(mood: BirdMood): Float =
        if (playing) (clockMillis / periodMillis(mood)) % 1f else scrub

    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Bird lab", style = MaterialTheme.typography.headlineSmall)

        ControlRow("Dark ground") { Switch(checked = dark, onCheckedChange = onDarkChange) }
        ControlRow("Playing") { Switch(checked = playing, onCheckedChange = { playing = it }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in speeds) {
                FilterChip(
                    selected = speed == option,
                    onClick = { speed = option },
                    label = { Text("${option}x") },
                )
            }
        }
        if (!playing) {
            Text("Phase ${"%.2f".format(scrub)}", style = MaterialTheme.typography.labelLarge)
            Slider(value = scrub, onValueChange = { scrub = it })
        }

        for (pair in BirdMood.entries.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (mood in pair) {
                    MoodCard(mood, phase = { phaseOf(mood) }, modifier = Modifier.weight(1f))
                }
            }
        }

        Text("Small sizes (idle, still)", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (size in listOf(96, 64, 48, 32, 24)) {
                BirdCanvas(mood = BirdMood.IDLE, phase = { 0f }, modifier = Modifier.size(size.dp))
            }
        }

        Text("On the launch-screen sky", style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFCFE9F2), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            BirdCanvas(mood = BirdMood.FLYING, phase = { phaseOf(BirdMood.FLYING) }, modifier = Modifier.size(120.dp))
        }
    }
}

@Composable
private fun ControlRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        control()
    }
}

@Composable
private fun MoodCard(mood: BirdMood, phase: () -> Float, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BirdCanvas(mood = mood, phase = phase, modifier = Modifier.size(140.dp))
            Text(mood.name, style = MaterialTheme.typography.titleSmall)
            val pose = birdPose(mood, phase())
            Text(
                "wing %.0f deg  y %.1f  eye %.2f".format(pose.wingAngle, pose.bodyOffsetY, pose.eyeOpen),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

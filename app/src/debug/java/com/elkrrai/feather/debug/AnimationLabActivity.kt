package com.elkrrai.feather.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.domain.nest.NestItem
import com.elkrrai.feather.domain.rhythm.DayCount
import com.elkrrai.feather.domain.rhythm.RhythmDot
import com.elkrrai.feather.feature.rhythm.RhythmDots
import com.elkrrai.feather.feature.rhythm.WeekBars
import com.elkrrai.feather.feature.rhythm.rememberEntry
import com.elkrrai.feather.feature.tasklist.TodayHeader
import com.elkrrai.feather.feature.tasklist.rememberHeaderCollapse
import com.elkrrai.feather.ui.brand.BirdIllustration
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.brand.CompletionBird
import com.elkrrai.feather.ui.brand.scenes.Illustration
import com.elkrrai.feather.ui.brand.scenes.NestScene
import com.elkrrai.feather.ui.brand.scenes.SceneKind
import com.elkrrai.feather.ui.components.CompletionRing
import com.elkrrai.feather.ui.components.ConfettiController
import com.elkrrai.feather.ui.components.FeatherConfettiOverlay
import com.elkrrai.feather.ui.components.rememberConfettiController
import com.elkrrai.feather.ui.LOW_END_FEATHERS
import com.elkrrai.feather.ui.theme.FeatherTheme
import com.elkrrai.feather.ui.theme.Nest
import com.elkrrai.feather.ui.today.TodayStatus
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * A debug-only screen to pick any animation from a bottom sheet and watch it: the bird's moods
 * (with the idle head tilt and tail flick), the completion ring and hop, every kind of confetti,
 * the Today header's sky and all-done celebration, the scenes, the nest and the Your rhythm
 * charts. Replay starts the chosen one again. It shows the animations as they are, but, like the
 * app, it stays still if the phone's animation scale is zero. It has its own launcher icon in debug
 * builds ("Animation lab"), or start it with
 * `adb shell am start -n com.elkrrai.feather/.debug.AnimationLabActivity`.
 */
class AnimationLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by rememberSaveable { mutableStateOf(false) }
            FeatherTheme(darkTheme = dark, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AnimationLab(dark = dark, onDarkChange = { dark = it })
                }
            }
        }
    }
}

/** What an animation's preview is given: how many times Replay was pressed, and the shared helpers. */
private class LabScope(
    val replay: Int,
    val confetti: ConfettiController,
    /** The middle of the preview area in window pixels, where confetti starts. */
    val center: () -> Offset,
)

private class LabItem(
    val group: String,
    val title: String,
    val note: String,
    val content: @Composable LabScope.() -> Unit,
)

private val Items: List<LabItem> = buildList {
    // --- the bird ---
    for (mood in BirdMood.entries) {
        val note = when (mood) {
            BirdMood.IDLE -> "Breathes, blinks, tilts its head and preens now and then."
            BirdMood.SLEEPING -> "Eyes closed, breathing slowly, the glow pulsing, floating z letters."
            BirdMood.HAPPY -> "Wings spread, one hop with flapping. Only the completion hop uses it."
            BirdMood.FLYING -> "Wings spread, flapping on the spot."
        }
        add(LabItem("Bird", "Bird: ${mood.name.lowercase()}", note) { BirdLoop(mood) })
    }
    add(LabItem("Bird", "Bird: flight and land", "Crouches, flies one lap in the air, lands with a squint and starts the idle loop again. 2.2 s.") {
        Box(Modifier.fillMaxSize().padding(bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
            BirdIllustration(Modifier.size(120.dp), flightTrigger = replay)
        }
    })
    add(LabItem("Bird", "Completion hop", "The bird that hops across the screen when a task is finished.") {
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) { CompletionBird(trigger = replay) }
    })

    // --- finishing a task ---
    add(LabItem("Finishing", "Completion ring", "Squeeze, fill, pop and draw the check; then undo. Tap a ring to try it yourself. Dashes are the priority.") {
        key(replay) { RingsDemo(confetti) }
    })
    add(LabItem("Finishing", "Confetti: task", "28 feathers thrown in random directions from the ring.") {
        ConfettiOnReplay(this, scale = 1f, count = 28, warm = false)
    })
    add(LabItem("Finishing", "Confetti: subtask", "The smaller burst for a subtask (0.6 size).") {
        ConfettiOnReplay(this, scale = 0.6f, count = 28, warm = false)
    })
    add(LabItem("Finishing", "Confetti: all done (warm)", "12 amber and coral feathers for the once-a-day last task.") {
        ConfettiOnReplay(this, scale = 1f, count = 12, warm = true)
    })
    add(LabItem("Finishing", "Confetti: low-end phone", "$LOW_END_FEATHERS feathers, as on a low-memory phone or in battery saver.") {
        ConfettiOnReplay(this, scale = 1f, count = LOW_END_FEATHERS, warm = false)
    })
    add(LabItem("Finishing", "Repeating task flight", "A leaf-feather flies to the top corner where Upcoming lives.") {
        LaunchedEffect(replay) {
            delay(150)
            confetti.fly(center(), null)
        }
        Hint("Watch the top-left corner.")
    })

    // --- Today ---
    add(LabItem("Today", "Header: all done", "The warm flourish: a halo and warm feathers at take-off, the bird flies a lap and lands, and the headline turns to All done.") {
        HeaderDemo(this, TodayStatus(left = 0, done = 5), hours = listOf(10), celebrate = true)
    })
    add(LabItem("Today", "Header: sky through the day", "The sky cross-fades when the part of the day changes. Replay steps morning, afternoon, evening, night.") {
        HeaderDemo(this, TodayStatus(left = 3, done = 2), hours = listOf(8, 14, 19, 23), celebrate = false)
    })
    add(LabItem("Today", "Header: asleep", "Nothing planned: the bird sleeps.") {
        HeaderDemo(this, TodayStatus(left = 0, done = 0), hours = listOf(10), celebrate = false)
    })

    // --- scenes ---
    for (kind in SceneKind.entries) {
        add(LabItem("Scenes", "Scene: ${kind.name}", "Fades in and lifts over 320 ms, then the leaf sways.") {
            key(replay) { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Illustration(kind, size = 280.dp) } }
        })
    }
    add(LabItem("Scenes", "Your nest: everything", "The nest with every gift.") {
        key(replay) { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { NestScene(NestItem.entries.toSet(), size = 300.dp) } }
    })
    add(LabItem("Scenes", "Your nest: empty", "The nest before anything is earned.") {
        key(replay) { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { NestScene(emptySet(), size = 300.dp) } }
    })

    // --- Your rhythm ---
    add(LabItem("Your rhythm", "Week bars", "The seven bars grow in one after another; a quiet day is a curled leaf.") {
        key(replay) {
            val entry = rememberEntry()
            WeekBars(
                days = demoDays(),
                entry = entry,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    })
    add(LabItem("Your rhythm", "Rhythm dots", "Fourteen dots pop in one after another.") {
        key(replay) {
            val entry = rememberEntry()
            RhythmDots(
                dots = demoDots(),
                entry = entry,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    })
}

private fun demoDays(): List<DayCount> {
    val today = LocalDate.now()
    val counts = listOf(3, 0, 5, 2, 7, 1, 4)
    return counts.mapIndexed { index, count -> DayCount(today.minusDays((counts.lastIndex - index).toLong()), count) }
}

private fun demoDots(): List<RhythmDot> {
    val today = LocalDate.now()
    val shows = listOf(true, true, false, true, false, true, true, true, false, false, true, true, true, true)
    return shows.mapIndexed { index, showed -> RhythmDot(today.minusDays((shows.lastIndex - index).toLong()), showed) }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** The owl in [mood], bigger than the Today header's. */
@Composable
private fun BirdLoop(mood: BirdMood) {
    BirdIllustration(Modifier.size(240.dp), mood = mood)
}

@Composable
private fun ConfettiOnReplay(scope: LabScope, scale: Float, count: Int, warm: Boolean) {
    LaunchedEffect(scope.replay) {
        delay(150)
        scope.confetti.celebrate(scope.center(), scale, count, warm)
    }
    Hint("Feathers start from the middle of this area.")
}

/** The four priority rings, each ticked after a moment and unticked again. */
@Composable
private fun RingsDemo(confetti: ConfettiController) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        val colors = listOf(
            MaterialTheme.colorScheme.outline,
            Nest.SkyWing,
            Nest.Amber,
            Nest.Coral,
        )
        colors.forEachIndexed { barbs, color ->
            var checked by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(450L + barbs * 120L)
                checked = true
                delay(2200)
                checked = false
            }
            CompletionRing(
                checked = checked,
                onCheckedChange = { checked = it },
                ringColor = color,
                contentDescription = "Demo task",
                barbs = barbs,
                onCelebrate = { origin -> confetti.celebrate(origin, 1f) },
            )
        }
    }
}

@Composable
private fun HeaderDemo(scope: LabScope, status: TodayStatus, hours: List<Int>, celebrate: Boolean) {
    val collapse = rememberHeaderCollapse().state
    val time = LocalTime.of(hours[scope.replay % hours.size], 0)
    TodayHeader(
        status = status,
        date = LocalDate.now(),
        time = time,
        collapse = collapse,
        title = "Today",
        onMenuClick = {},
        onSearchClick = {},
        sortActions = emptyList(),
        celebration = if (celebrate) scope.replay else 0,
        onWarmFeathers = { origin -> scope.confetti.celebrate(origin, 1f, 12, true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimationLab(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var replay by rememberSaveable { mutableIntStateOf(1) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var previewCenter by remember { mutableStateOf(Offset.Zero) }
    val confetti = rememberConfettiController()
    val current = Items[selected]

    fun select(index: Int) {
        selected = index.mod(Items.size)
        replay = 1
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Animation lab", style = MaterialTheme.typography.headlineSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dark", style = MaterialTheme.typography.labelLarge)
                    Switch(checked = dark, onCheckedChange = onDarkChange)
                }
            }
            Column {
                Text(current.group.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(current.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(current.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .onGloballyPositioned {
                        previewCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                // A new item starts from scratch (replay is reset to 1 when one is chosen).
                key(selected) {
                    val scope = LabScope(replay, confetti) { previewCenter }
                    current.content(scope)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { sheetOpen = true }, modifier = Modifier.weight(1f)) { Text("Choose animation") }
                FilledTonalButton(onClick = { replay += 1 }, modifier = Modifier.weight(1f)) { Text("Replay") }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { select(selected - 1) }) { Text("Previous") }
                Text("${selected + 1} / ${Items.size}", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { select(selected + 1) }) { Text("Next") }
            }
        }

        FeatherConfettiOverlay(confetti)
    }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }, sheetState = sheetState) {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                var lastGroup = ""
                Items.forEachIndexed { index, entry ->
                    if (entry.group != lastGroup) {
                        lastGroup = entry.group
                        val group = entry.group
                        item(key = "group-$group") {
                            Text(
                                group,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
                            )
                        }
                    }
                    item(key = "item-$index") {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    select(index)
                                    sheetOpen = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

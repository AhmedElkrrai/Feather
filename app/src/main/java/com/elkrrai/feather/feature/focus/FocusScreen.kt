package com.elkrrai.feather.feature.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.focus.BREAK_MINUTES
import com.elkrrai.feather.domain.focus.FocusPhase
import com.elkrrai.feather.domain.focus.MILLIS_PER_MINUTE
import com.elkrrai.feather.domain.focus.focusClock
import com.elkrrai.feather.reminders.SettingsIntents
import com.elkrrai.feather.reminders.startActivitySafely
import com.elkrrai.feather.ui.brand.BirdIllustration
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.theme.Space

/** The focus timer of one task: pick the length, start, and the owl keeps you company. */
@Composable
fun FocusScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.missing) { if (state.missing) onBack() }

    // Without the notification permission nothing tells you the time is up while the app is closed:
    // it is asked for when a session starts, and the session starts whatever the answer.
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.start()
    }
    val start = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.start()
        }
    }

    FocusContent(
        state = state,
        notificationsOn = notificationsOn,
        onNotificationSettings = { context.startActivitySafely(SettingsIntents.notifications(context), SettingsIntents.appDetails(context)) },
        onBack = onBack,
        onShorter = viewModel::onShorter,
        onLonger = viewModel::onLonger,
        onStart = start,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onStop = viewModel::stop,
        onBreak = viewModel::startBreak,
        modifier = modifier,
    )
}

/** What the screen is doing, from the session. */
private enum class FocusMode { Idle, Running, Paused, Over }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusContent(
    state: FocusUiState,
    notificationsOn: Boolean,
    onNotificationSettings: () -> Unit,
    onBack: () -> Unit,
    onShorter: () -> Unit,
    onLonger: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onBreak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session
    val mode = when {
        session == null -> FocusMode.Idle
        session.isOver(state.now) -> FocusMode.Over
        session.isPaused -> FocusMode.Paused
        else -> FocusMode.Running
    }
    val onBreakPhase = session?.phase == FocusPhase.BREAK
    val locale = LocalConfiguration.current.locales[0]

    // The screen stays on while a session runs, so the countdown can be watched.
    val view = LocalView.current
    DisposableEffect(mode) {
        view.keepScreenOn = mode == FocusMode.Running
        onDispose { view.keepScreenOn = false }
    }

    // The owl lies still while you focus and takes a lap when the time is up.
    var laps by remember { mutableIntStateOf(0) }
    LaunchedEffect(mode, onBreakPhase) { if (mode == FocusMode.Over && !onBreakPhase) laps++ }
    val mood = if (mode == FocusMode.Running && !onBreakPhase) BirdMood.SLEEPING else BirdMood.IDLE

    val left = when (mode) {
        FocusMode.Idle -> state.minutes * MILLIS_PER_MINUTE
        else -> session?.remaining(state.now) ?: 0L
    }
    val fraction = if (session == null) 1f else session.fractionLeft(state.now)
    val label = when {
        mode == FocusMode.Paused -> R.string.focus_label_paused
        onBreakPhase -> R.string.focus_label_break
        else -> R.string.focus_label_focus
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.focus_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter, vertical = Space.x4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.x3, Alignment.CenterVertically),
        ) {
            BirdIllustration(
                modifier = Modifier.size(120.dp),
                mood = mood,
                flightTrigger = laps,
            )
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            CountdownRing(
                fraction = fraction,
                breakPhase = onBreakPhase,
                time = focusClock(left, locale),
                label = stringResource(label),
            )

            when (mode) {
                FocusMode.Idle -> {
                    LengthPicker(minutes = state.minutes, onShorter = onShorter, onLonger = onLonger)
                    if (state.otherTitle != null) {
                        Text(
                            text = stringResource(R.string.focus_replaces, state.otherTitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (!notificationsOn) {
                        Text(
                            text = stringResource(R.string.focus_notifications_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = onNotificationSettings) {
                            Text(stringResource(R.string.focus_notifications_settings))
                        }
                    }
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth(0.7f)) {
                        Text(stringResource(R.string.focus_start))
                    }
                }

                FocusMode.Running -> Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.x2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onPause) { Text(stringResource(R.string.focus_pause)) }
                    TextButton(onClick = onStop) { Text(stringResource(R.string.focus_stop)) }
                }

                FocusMode.Paused -> Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.x2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onResume) { Text(stringResource(R.string.focus_resume)) }
                    TextButton(onClick = onStop) { Text(stringResource(R.string.focus_stop)) }
                }

                FocusMode.Over -> {
                    Text(
                        text = stringResource(if (onBreakPhase) R.string.focus_break_over_title else R.string.focus_over_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    if (!onBreakPhase) {
                        FilledTonalButton(onClick = onBreak) {
                            Text(stringResource(R.string.focus_take_break, BREAK_MINUTES))
                        }
                    }
                    OutlinedButton(onClick = onStart) { Text(stringResource(R.string.focus_another)) }
                    TextButton(onClick = onStop) { Text(stringResource(R.string.focus_done)) }
                }
            }
            Spacer(Modifier.height(Space.x2))
        }
    }
}

/** A ring that empties as the time goes, with the time and what it is in the middle. */
@Composable
private fun CountdownRing(fraction: Float, breakPhase: Boolean, time: String, label: String) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val color = if (breakPhase) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(236.dp)
            .semantics { contentDescription = "$label, $time" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2f
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arc,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = time, style = MaterialTheme.typography.displayMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The length of the next stretch: shorter and longer by five minutes. */
@Composable
private fun LengthPicker(minutes: Int, onShorter: () -> Unit, onLonger: () -> Unit) {
    val shorter = stringResource(R.string.focus_shorter)
    val longer = stringResource(R.string.focus_longer)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.x3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onShorter, modifier = Modifier.semantics { contentDescription = shorter }) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = stringResource(R.string.focus_minutes, minutes),
            style = MaterialTheme.typography.titleMedium,
        )
        FilledTonalIconButton(onClick = onLonger, modifier = Modifier.semantics { contentDescription = longer }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

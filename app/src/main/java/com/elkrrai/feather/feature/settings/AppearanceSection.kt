package com.elkrrai.feather.feature.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.ThemeMode
import com.elkrrai.feather.domain.settings.TimeFormat
import com.elkrrai.feather.domain.settings.WeekStart
import java.time.DayOfWeek
import java.time.format.TextStyle

/** What the appearance rows of the settings screen can change; the stateful screen supplies them. */
class AppearanceActions(
    val onThemeChange: (ThemeMode) -> Unit = {},
    val onPureBlackChange: (Boolean) -> Unit = {},
    val onDynamicColorChange: (Boolean) -> Unit = {},
    val onTimeFormatChange: (TimeFormat) -> Unit = {},
    val onWeekStartChange: (WeekStart) -> Unit = {},
    val onHapticsChange: (Boolean) -> Unit = {},
    val onBirdAnimationsChange: (Boolean) -> Unit = {},
    val onReduceMotionChange: (Boolean) -> Unit = {},
    val onCelebrationsChange: (Boolean) -> Unit = {},
)

@StringRes
internal fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
internal fun TimeFormat.labelRes(): Int = when (this) {
    TimeFormat.SYSTEM -> R.string.format_system
    TimeFormat.H12 -> R.string.time_format_12
    TimeFormat.H24 -> R.string.time_format_24
}

private fun WeekStart.day(): DayOfWeek? = when (this) {
    WeekStart.SYSTEM -> null
    WeekStart.MONDAY -> DayOfWeek.MONDAY
    WeekStart.SATURDAY -> DayOfWeek.SATURDAY
    WeekStart.SUNDAY -> DayOfWeek.SUNDAY
}

/** The name of a first-weekday choice: the weekday in the app language, or "Follow system". */
@Composable
internal fun WeekStart.label(): String {
    val locale = LocalConfiguration.current.locales[0]
    return day()?.getDisplayName(TextStyle.FULL, locale) ?: stringResource(R.string.format_system)
}

/** The rows of the "Appearance and formats" section. */
@Composable
internal fun AppearanceRows(
    settings: AppSettings,
    actions: AppearanceActions,
    onPickTheme: () -> Unit,
    onPickTimeFormat: () -> Unit,
    onPickWeekStart: () -> Unit,
) {
    SectionTitle(stringResource(R.string.settings_section_appearance))
    SettingsRow(
        title = stringResource(R.string.settings_theme),
        summary = stringResource(settings.themeMode.labelRes()),
        supporting = null,
        onClick = onPickTheme,
    )
    SettingsRow(
        title = stringResource(R.string.settings_pure_black),
        summary = null,
        supporting = stringResource(R.string.settings_pure_black_summary),
        onClick = { actions.onPureBlackChange(!settings.pureBlack) },
        trailing = { Switch(checked = settings.pureBlack, onCheckedChange = actions.onPureBlackChange) },
    )
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    SettingsRow(
        title = stringResource(R.string.settings_dynamic_color),
        summary = null,
        supporting = stringResource(R.string.settings_dynamic_color_summary),
        enabled = dynamicAvailable,
        onClick = { actions.onDynamicColorChange(!settings.dynamicColor) },
        trailing = {
            Switch(
                checked = settings.dynamicColor && dynamicAvailable,
                onCheckedChange = actions.onDynamicColorChange,
                enabled = dynamicAvailable,
            )
        },
    )
    SettingsRow(
        title = stringResource(R.string.settings_time_format),
        summary = stringResource(settings.timeFormat.labelRes()),
        supporting = null,
        onClick = onPickTimeFormat,
    )
    SettingsRow(
        title = stringResource(R.string.settings_week_start),
        summary = settings.weekStart.label(),
        supporting = null,
        onClick = onPickWeekStart,
    )
    SettingsRow(
        title = stringResource(R.string.settings_haptics),
        summary = null,
        supporting = stringResource(R.string.settings_haptics_summary),
        onClick = { actions.onHapticsChange(!settings.haptics) },
        trailing = { Switch(checked = settings.haptics, onCheckedChange = actions.onHapticsChange) },
    )
    SettingsRow(
        title = stringResource(R.string.settings_bird_animations),
        summary = null,
        supporting = stringResource(R.string.settings_bird_animations_summary),
        onClick = { actions.onBirdAnimationsChange(!settings.birdAnimations) },
        trailing = {
            Switch(checked = settings.birdAnimations, onCheckedChange = actions.onBirdAnimationsChange)
        },
    )
    SettingsRow(
        title = stringResource(R.string.settings_celebrations),
        summary = null,
        supporting = stringResource(R.string.settings_celebrations_summary),
        onClick = { actions.onCelebrationsChange(!settings.celebrations) },
        trailing = { Switch(checked = settings.celebrations, onCheckedChange = actions.onCelebrationsChange) },
    )
    SettingsRow(
        title = stringResource(R.string.settings_reduce_motion),
        summary = null,
        supporting = stringResource(R.string.settings_reduce_motion_summary),
        onClick = { actions.onReduceMotionChange(!settings.reduceMotion) },
        trailing = { Switch(checked = settings.reduceMotion, onCheckedChange = actions.onReduceMotionChange) },
    )
}

/** A short list of radio choices in a dialog; picking one applies it and closes the dialog. */
@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = { onSelect(value) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

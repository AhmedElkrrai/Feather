package com.elkrrai.feather.feature.settings

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.elkrrai.feather.domain.settings.TimeFormat
import com.elkrrai.feather.domain.settings.VoiceLanguage
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.elkrrai.feather.R

/** Languages the app is translated into. [tag] is empty for "follow the system". */
enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    ARABIC("ar", R.string.language_arabic);

    companion object {
        /** The language currently chosen through per-app locales. */
        fun current(): AppLanguage {
            val language = AppCompatDelegate.getApplicationLocales()[0]?.language.orEmpty()
            return entries.firstOrNull { it.tag == language } ?: SYSTEM
        }
    }

    /**
     * Applies this language app-wide. AppCompat persists the choice and recreates the activity,
     * and Compose then picks up the new locale and layout direction.
     */
    fun apply() {
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

/** Language, time format and the language of the microphone in one dialog: groups of radio buttons that apply as soon as one is picked. */
@Composable
fun LanguageTimeDialog(
    timeFormat: TimeFormat,
    onTimeFormatChange: (TimeFormat) -> Unit,
    voiceLanguage: VoiceLanguage,
    onVoiceLanguageChange: (VoiceLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    var language by remember { mutableStateOf(AppLanguage.current()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_section_language_time)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DialogSection(stringResource(R.string.settings_language))
                Column(Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { option ->
                        RadioLine(
                            label = stringResource(option.labelRes),
                            selected = option == language,
                            onClick = {
                                if (option != language) {
                                    language = option
                                    // the app restarts its screen in the new language; this dialog comes back with it
                                    option.apply()
                                }
                            },
                        )
                    }
                }
                DialogSection(stringResource(R.string.settings_time_format))
                Column(Modifier.selectableGroup()) {
                    TimeFormat.entries.forEach { option ->
                        RadioLine(
                            label = stringResource(option.labelRes()),
                            selected = option == timeFormat,
                            onClick = { onTimeFormatChange(option) },
                        )
                    }
                }
                DialogSection(stringResource(R.string.settings_voice_input))
                Column(Modifier.selectableGroup()) {
                    VoiceLanguage.entries.forEach { option ->
                        RadioLine(
                            label = stringResource(option.labelRes()),
                            selected = option == voiceLanguage,
                            onClick = { onVoiceLanguageChange(option) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_voice_input_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 40.dp, bottom = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        },
    )
}

@Composable
private fun DialogSection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 2.dp)
            .semantics { heading() },
    )
}

@Composable
private fun RadioLine(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text(label)
    }
}

@StringRes
private fun VoiceLanguage.labelRes(): Int = when (this) {
    VoiceLanguage.EN_US -> R.string.voice_language_en_us
    VoiceLanguage.AR_EG -> R.string.voice_language_ar_eg
}

package com.elkrrai.feather.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Light vibrations for finishing, selecting and moving tasks; silent when switched off in Settings. */
class Haptics(private val feedback: HapticFeedback) {

    /** A task was finished. */
    fun confirm() = perform(HapticFeedbackType.Confirm)

    /** A long press took effect: selecting, or starting a drag. */
    fun longPress() = perform(HapticFeedbackType.LongPress)

    /** A dragged task passed over a neighbor. */
    fun tick() = perform(HapticFeedbackType.SegmentTick)

    /** A task was sent from quick add: a soft thud. */
    fun thud() = perform(HapticFeedbackType.VirtualKey)

    private fun perform(type: HapticFeedbackType) {
        if (DisplayPrefs.haptics) feedback.performHapticFeedback(type)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}

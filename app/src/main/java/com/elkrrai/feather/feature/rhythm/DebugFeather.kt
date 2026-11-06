package com.elkrrai.feather.feature.rhythm

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Debug builds only: forces the weekly feather card on Progress to show, whatever day it is, as if that many days of last week had a
 * finished task (0 to 7). Null is off. It is set from the debug dialog in Settings and lasts until the app is closed.
 */
object DebugFeather {
    val days = MutableStateFlow<Int?>(null)
}

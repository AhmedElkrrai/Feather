package com.elkrrai.feather.feature.focus

import androidx.lifecycle.ViewModel
import com.elkrrai.feather.domain.focus.FocusSession
import com.elkrrai.feather.focus.FocusController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Lets the task list know which task has a focus session, to show its time left on the card. */
@HiltViewModel
class FocusBadgeViewModel @Inject constructor(
    controller: FocusController,
) : ViewModel() {
    val session: StateFlow<FocusSession?> = controller.session
}

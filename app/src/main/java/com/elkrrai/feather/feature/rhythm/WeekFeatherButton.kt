package com.elkrrai.feather.feature.rhythm

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.rhythm.CompletionRecord
import com.elkrrai.feather.domain.rhythm.RhythmRepository
import com.elkrrai.feather.domain.rhythm.WeeklyFeather
import com.elkrrai.feather.domain.rhythm.thisWeekActiveDays
import com.elkrrai.feather.ui.firstDayOfWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class WeekFeatherViewModel @Inject constructor(rhythm: RhythmRepository) : ViewModel() {
    val records: StateFlow<List<CompletionRecord>> =
        rhythm.observeCompletions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * The button that adds something (a task, a habit): round, with this week's feather where a plus would be. The feather is one of the
 * owl's eight and grows fuller with each day of the week that something was finished; the same feathers as the weekly one in Progress.
 * [description] is what a screen reader says, since the picture itself says nothing.
 */
@Composable
fun WeekFeatherButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeekFeatherViewModel = hiltViewModel(),
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val days = remember(records, today) {
        thisWeekActiveDays(records, today, ZoneId.systemDefault(), firstDayOfWeek(Locale.getDefault()))
    }
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        // the weekly feather view takes a week of days: one finished task on each of the first [days] days gives the same feather
        WeeklyFeatherView(WeeklyFeather(today, List(7) { if (it < days) 1 else 0 }), size = 38.dp)
    }
}

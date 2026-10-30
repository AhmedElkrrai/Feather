package com.elkrrai.feather.data.nest

import com.elkrrai.feather.domain.nest.NestFacts
import com.elkrrai.feather.domain.nest.NestItem
import com.elkrrai.feather.domain.nest.earnedNestItems
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.rhythm.RhythmRepository
import com.elkrrai.feather.domain.rhythm.keptRepeaterFourWeeks
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.SettingsRepository
import java.time.Clock
import com.elkrrai.feather.domain.time.FIRST_DAY_OF_WEEK
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Keeps score for the bird's nest. It works out what has been done (tasks finished, a backup made,
 * tags and reminders) and, whenever that earns something new, saves it: an earned item stays for
 * good, even if the tags or reminders that earned it are deleted later. Nothing here is ever
 * time-limited, random or paid.
 */
@Singleton
class NestTracker @Inject constructor(
    private val rhythm: RhythmRepository,
    private val tasks: TaskRepository,
    private val tagRepository: TagRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    /** What has been done so far, updated as the data changes. */
    fun facts(firstDay: DayOfWeek = FIRST_DAY_OF_WEEK): Flow<NestFacts> = combine(
        rhythm.observeCompletions(),
        tasks.observeTasks(),
        tagRepository.observeTags(),
        settings.settings,
    ) { records, openAndDone, allTags, current ->
        val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        NestFacts(
            tasksFinished = records.size,
            backedUp = current.backedUp || current.lastBackupAt != null,
            keptRepeaterFourWeeks = keptRepeaterFourWeeks(records, today, ZoneId.systemDefault(), firstDay),
            remindersSet = openAndDone.sumOf { it.reminders.size },
            tagsMade = allTags.size,
            voiceUsed = current.voiceUsed,
        )
    }.distinctUntilChanged()

    /** Starts saving what gets earned. Called once, when the app starts. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(facts(), settings.settings) { facts, current -> facts to current }
                .collect { (facts, current) -> saveNew(facts, current) }
        }
    }

    private suspend fun saveNew(facts: NestFacts, current: AppSettings) {
        val earned = earnedNestItems(facts).map(NestItem::id).toSet()
        if (!current.nestUnlocked.containsAll(earned)) {
            settings.setNestUnlocked(current.nestUnlocked + earned)
        }
    }
}

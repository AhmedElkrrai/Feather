package com.elkrrai.feather.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.repository.SavedFilterRepositoryImpl
import com.elkrrai.feather.domain.model.DueWindow
import com.elkrrai.feather.domain.model.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedFilterRepositoryTest {

    private lateinit var db: FeatherDatabase
    private lateinit var clock: TestClock
    private lateinit var filters: SavedFilterRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FeatherDatabase::class.java).build()
        clock = TestClock()
        filters = SavedFilterRepositoryImpl(db.savedFilterDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun create(name: String = "Important") = filters.createFilter(
        name = name,
        listId = "list-1",
        tagId = "tag-1",
        minPriority = Priority.MEDIUM,
        due = DueWindow.NEXT_7_DAYS,
        hideCompleted = false,
    )

    @Test
    fun aFilterIsStoredWithAllItsConditions() = runTest {
        val created = create("  Important  ")

        val stored = filters.observeFilters().first().single()

        assertEquals(created, stored)
        assertEquals("Important", stored.name)
        assertEquals("list-1", stored.listId)
        assertEquals("tag-1", stored.tagId)
        assertEquals(Priority.MEDIUM, stored.minPriority)
        assertEquals(DueWindow.NEXT_7_DAYS, stored.due)
        assertEquals(false, stored.hideCompleted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aBlankNameIsRefused() = runTest {
        create("   ")
    }

    @Test
    fun updatingChangesTheConditionsInPlace() = runTest {
        val created = create()
        clock.advance()

        filters.updateFilter(created.copy(name = "Renamed", listId = null, due = DueWindow.TODAY))

        val stored = filters.observeFilters().first().single()
        assertEquals(created.id, stored.id)
        assertEquals("Renamed", stored.name)
        assertEquals(null, stored.listId)
        assertEquals(DueWindow.TODAY, stored.due)
        assertEquals("tag-1", stored.tagId)
    }

    @Test
    fun deletedFiltersDisappearAndStayDeleted() = runTest {
        val created = create()

        filters.deleteFilter(created.id)
        filters.updateFilter(created.copy(name = "Back again"))

        assertTrue(filters.observeFilters().first().isEmpty())
    }

    @Test
    fun filtersKeepTheOrderTheyWereMadeIn() = runTest {
        create("First")
        clock.advance()
        create("Second")

        assertEquals(listOf("First", "Second"), filters.observeFilters().first().map { it.name })
    }
}

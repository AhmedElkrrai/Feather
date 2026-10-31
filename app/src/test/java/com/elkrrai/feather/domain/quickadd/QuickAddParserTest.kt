package com.elkrrai.feather.domain.quickadd

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Now" is Wednesday 14 October 2026, 10:00, so Friday is the 16th, next Monday the 19th and
 * the coming Saturday the 17th.
 */
class QuickAddParserTest {

    private val now = LocalDateTime.of(2026, 10, 14, 10, 0)

    private fun parse(
        text: String,
        lists: List<String> = emptyList(),
        disabled: Set<QuickAddField> = emptySet(),
    ) = QuickAddParser.parse(text, now, lists, disabled)

    private data class Case(
        val text: String,
        val title: String,
        val date: String? = null,
        val time: String? = null,
        val implied: Boolean = false,
    )

    private fun check(cases: List<Case>) {
        for (case in cases) {
            val parsed = parse(case.text)
            assertEquals("title of '${case.text}'", case.title, parsed.title)
            assertEquals("date of '${case.text}'", case.date?.let(LocalDate::parse), parsed.dueDate)
            assertEquals("time of '${case.text}'", case.time?.let(LocalTime::parse), parsed.dueTime)
            assertEquals("implied date of '${case.text}'", case.implied, parsed.dateIsImplied)
        }
    }

    // --- Plain text ---

    @Test
    fun textWithoutAnythingToRecognizeIsJustATitle() {
        check(
            listOf(
                Case("Buy milk", "Buy milk"),
                Case("Pay 5 apples", "Pay 5 apples"),
                Case("Read chapter 3", "Read chapter 3"),
                Case("May the force be with you", "May the force be with you"),
                Case("Feb 30 party", "Feb 30 party"),
                Case("Wow!", "Wow!"),
            )
        )
    }

    @Test
    fun blankTextGivesAnEmptyTask() {
        val parsed = parse("   ")
        assertEquals("", parsed.title)
        assertTrue(parsed.fields.isEmpty())
    }

    @Test
    fun whenNothingIsLeftForATitleTheWholeTextIsTheTitle() {
        val parsed = parse("tomorrow")
        assertEquals("tomorrow", parsed.title)
        assertEquals(LocalDate.of(2026, 10, 15), parsed.dueDate)
        assertEquals("#home", parse("#home").title)
    }

    // --- English dates ---

    @Test
    fun englishRelativeDays() {
        check(
            listOf(
                Case("Call mom today", "Call mom", "2026-10-14"),
                Case("Call mom tomorrow", "Call mom", "2026-10-15"),
                Case("Call mom tmrw", "Call mom", "2026-10-15"),
                Case("Call mom day after tomorrow", "Call mom", "2026-10-16"),
                Case("Submit by tomorrow", "Submit", "2026-10-15"),
                Case("Pay rent in 3 days", "Pay rent", "2026-10-17"),
                Case("Pay rent in two weeks", "Pay rent", "2026-10-28"),
                Case("Pay rent in a month", "Pay rent", "2026-11-14"),
                Case("Plan next week", "Plan", "2026-10-17"), // the week starts on Saturday
                Case("Plan next month", "Plan", "2026-11-01"),
                Case("Hike this weekend", "Hike", "2026-10-17"),
            )
        )
    }

    @Test
    fun englishWeekdays() {
        check(
            listOf(
                Case("Dentist friday", "Dentist", "2026-10-16"),
                Case("Dentist this friday", "Dentist", "2026-10-16"),
                Case("Report next friday", "Report", "2026-10-23"),
                Case("Meeting on monday", "Meeting", "2026-10-19"),
                Case("Meeting wednesday", "Meeting", "2026-10-21"), // today is a Wednesday
                Case("Submit by friday", "Submit", "2026-10-16"),
                Case("Class on tue", "Class", "2026-10-20"),
                Case("Class next mon", "Class", "2026-10-19"),
                Case("Class next sun", "Class", "2026-10-18"), // the Sunday of next week (it starts on Saturday the 17th)
                Case("Class sat", "Class sat"), // an abbreviation alone is just a word
            )
        )
    }

    @Test
    fun englishCalendarDates() {
        check(
            listOf(
                Case("Party on oct 20", "Party", "2026-10-20"),
                Case("Party 20 october", "Party", "2026-10-20"),
                Case("Party october 20th", "Party", "2026-10-20"),
                Case("Party the 20th of october", "Party", "2026-10-20"),
                Case("Party jan 5", "Party", "2027-01-05"),
                Case("Party oct 20, 2027", "Party", "2027-10-20"),
                Case("Rent on the 1st", "Rent", "2026-11-01"),
                Case("Rent the 20th", "Rent", "2026-10-20"),
                Case("Review 2026-12-01", "Review", "2026-12-01"),
            )
        )
    }

    // --- English times ---

    @Test
    fun englishClockTimes() {
        check(
            listOf(
                Case("Call mom tomorrow 5pm", "Call mom", "2026-10-15", "17:00"),
                Case("Call mom tomorrow at 5:30 pm", "Call mom", "2026-10-15", "17:30"),
                Case("Call mom tomorrow at 5", "Call mom", "2026-10-15", "17:00"),
                Case("Dentist friday at 3:30 pm", "Dentist", "2026-10-16", "15:30"),
                Case("Stand-up 09:15 tomorrow", "Stand-up", "2026-10-15", "09:15"),
                Case("Flight tomorrow 6 a.m.", "Flight", "2026-10-15", "06:00"),
                Case("Meet at 5pm on friday", "Meet", "2026-10-16", "17:00"),
                Case("Meeting tomorrow morning", "Meeting", "2026-10-15", "09:00"),
                Case("Meeting friday afternoon", "Meeting", "2026-10-16", "15:00"),
            )
        )
    }

    @Test
    fun aTimeAloneMeansTodayIfItIsAheadOtherwiseTomorrow() {
        check(
            listOf(
                Case("Lunch at noon", "Lunch", "2026-10-14", "12:00", implied = true),
                Case("Call at 5", "Call", "2026-10-14", "17:00", implied = true),
                Case("Meet at 5pm.", "Meet", "2026-10-14", "17:00", implied = true),
                Case("Gym at 7", "Gym", "2026-10-15", "07:00", implied = true),
                Case("Wake up at 6:30", "Wake up", "2026-10-15", "06:30", implied = true),
                Case("Sync at midnight", "Sync", "2026-10-15", "00:00", implied = true),
                Case("Dinner tonight", "Dinner", "2026-10-14", "20:00", implied = true),
                Case("Walk this evening", "Walk", "2026-10-14", "18:00", implied = true),
            )
        )
    }

    @Test
    fun englishRelativeTimes() {
        check(
            listOf(
                Case("Drink water in 2 hours", "Drink water", "2026-10-14", "12:00", implied = true),
                Case("Call back in 30 minutes", "Call back", "2026-10-14", "10:30", implied = true),
                Case("Call back in half an hour", "Call back", "2026-10-14", "10:30", implied = true),
                Case("Call back in an hour", "Call back", "2026-10-14", "11:00", implied = true),
            )
        )
    }

    // --- Arabic ---

    @Test
    fun arabicDates() {
        check(
            listOf(
                Case("اتصل بأمي غدا", "اتصل بأمي", "2026-10-15"),
                Case("زيارة اليوم", "زيارة", "2026-10-14"),
                Case("ادفع بعد غد", "ادفع", "2026-10-16"),
                Case("ادفع الايجار بعد ٣ ايام", "ادفع الايجار", "2026-10-17"),
                Case("ادفع بعد يومين", "ادفع", "2026-10-16"),
                Case("ادفع بعد اسبوعين", "ادفع", "2026-10-28"),
                Case("الاجتماع الجمعة القادمة", "الاجتماع", "2026-10-23"),
                Case("الاجتماع يوم الجمعة", "الاجتماع", "2026-10-16"),
                Case("الاجتماع الأربعاء", "الاجتماع", "2026-10-21"),
                Case("مراجعة الاسبوع القادم", "مراجعة", "2026-10-17"),
                Case("مراجعة الشهر المقبل", "مراجعة", "2026-11-01"),
                Case("حفلة 20 اكتوبر", "حفلة", "2026-10-20"),
                Case("حفلة ٢٠ أكتوبر", "حفلة", "2026-10-20"),
                Case("حفلة 20 من اكتوبر 2027", "حفلة", "2027-10-20"),
            )
        )
    }

    @Test
    fun arabicTimes() {
        check(
            listOf(
                Case("اتصل بأمي غداً الساعة ٥ مساءً", "اتصل بأمي", "2026-10-15", "17:00"),
                Case("موعد الطبيب بعد غد الساعة 4 عصرا", "موعد الطبيب", "2026-10-16", "16:00"),
                Case("اجتماع الساعة 5:30", "اجتماع", "2026-10-15", "05:30", implied = true),
                Case("اجتماع الساعة ٧ صباحا", "اجتماع", "2026-10-15", "07:00", implied = true),
                Case("اجتماع الساعة 5 ونصف", "اجتماع", "2026-10-14", "17:30", implied = true),
                Case("اجتماع الساعة 5 الا ربع", "اجتماع", "2026-10-14", "16:45", implied = true),
                Case("اجتماع منتصف الليل", "اجتماع", "2026-10-15", "00:00", implied = true),
                Case("عشاء الليلة", "عشاء", "2026-10-14", "20:00", implied = true),
                Case("اتصل بعد ساعتين", "اتصل", "2026-10-14", "12:00", implied = true),
                Case("اتصل بعد نصف ساعة", "اتصل", "2026-10-14", "10:30", implied = true),
            )
        )
    }

    @Test
    fun theTitleKeepsTheUsersOwnArabicSpelling() {
        // The hamza and tanween are untouched; only the recognized word is taken out.
        val parsed = parse("أكتب تقريرًا غدًا")
        assertEquals("أكتب تقريرًا", parsed.title)
        assertEquals(LocalDate.of(2026, 10, 15), parsed.dueDate)
    }

    @Test
    fun englishAndArabicCanBeMixed() {
        val parsed = parse("Call mom غدا at 5pm")
        assertEquals("Call mom", parsed.title)
        assertEquals(LocalDate.of(2026, 10, 15), parsed.dueDate)
        assertEquals(LocalTime.of(17, 0), parsed.dueTime)
    }

    // --- Repeats ---

    private fun weekly(vararg days: DayOfWeek, interval: Int = 1) =
        Recurrence(Frequency.WEEKLY, interval, weekdays = days.toSet())

    @Test
    fun englishRepeats() {
        fun repeatOf(text: String) = parse(text).recurrence

        assertEquals(weekly(DayOfWeek.MONDAY), repeatOf("Take out trash every monday"))
        assertEquals(weekly(DayOfWeek.MONDAY), repeatOf("Gym mondays"))
        assertEquals(weekly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), repeatOf("Gym every mon and wed"))
        assertEquals(weekly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), repeatOf("Gym every monday, friday"))
        assertEquals(weekly(DayOfWeek.WEDNESDAY, interval = 2), repeatOf("Water plants every 2 weeks"))
        assertEquals(weekly(DayOfWeek.WEDNESDAY, interval = 2), repeatOf("Mow lawn every other week"))
        assertEquals(Recurrence(Frequency.DAILY), repeatOf("Journal daily"))
        assertEquals(Recurrence(Frequency.DAILY), repeatOf("Journal every day"))
        assertEquals(Recurrence(Frequency.DAILY, 3), repeatOf("Do taxes every 3 days"))
        assertEquals(Recurrence(Frequency.MONTHLY, monthDay = 14), repeatOf("Pay rent every month"))
        assertEquals(Recurrence(Frequency.MONTHLY, monthDay = 14), repeatOf("Pay rent monthly"))
        assertEquals(Recurrence(Frequency.YEARLY, month = 10, monthDay = 14), repeatOf("Birthday every year"))
        assertEquals(
            weekly(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            repeatOf("Check email every weekday"),
        )
    }

    @Test
    fun aRepeatWithoutADateStartsOnItsFirstMatchingDay() {
        val monday = parse("Take out trash every monday")
        assertEquals("Take out trash", monday.title)
        assertEquals(LocalDate.of(2026, 10, 19), monday.dueDate)
        assertTrue(monday.dateIsImplied)

        // Today is a Wednesday and the rule includes it.
        assertEquals(LocalDate.of(2026, 10, 14), parse("Gym every mon and wed").dueDate)

        val standUp = parse("Stand-up every weekday at 9:30")
        assertEquals(LocalTime.of(9, 30), standUp.dueTime)
        assertEquals(LocalDate.of(2026, 10, 15), standUp.dueDate) // 9:30 has passed today
    }

    @Test
    fun aRepeatKeepsAnExplicitDate() {
        val parsed = parse("Call mom tomorrow every week")
        assertEquals(LocalDate.of(2026, 10, 15), parsed.dueDate)
        assertEquals(weekly(DayOfWeek.THURSDAY), parsed.recurrence)
        assertEquals(false, parsed.dateIsImplied)
    }

    @Test
    fun arabicRepeats() {
        fun repeatOf(text: String) = parse(text).recurrence

        assertEquals(weekly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), repeatOf("تمرين كل اثنين وخميس"))
        assertEquals(weekly(DayOfWeek.FRIDAY), repeatOf("صلاة كل يوم الجمعة"))
        assertEquals(Recurrence(Frequency.DAILY), repeatOf("اسقي النباتات كل يوم"))
        assertEquals(Recurrence(Frequency.DAILY), repeatOf("ورد يوميا"))
        assertEquals(weekly(DayOfWeek.WEDNESDAY, interval = 2), repeatOf("مراجعة كل ٢ اسبوع"))
        assertEquals(weekly(DayOfWeek.WEDNESDAY, interval = 2), repeatOf("زيارة كل اسبوعين"))
        assertEquals(Recurrence(Frequency.MONTHLY, monthDay = 14), repeatOf("ايجار شهريا"))

        val parsed = parse("تمرين كل اثنين وخميس")
        assertEquals("تمرين", parsed.title)
        assertEquals(LocalDate.of(2026, 10, 15), parsed.dueDate)
    }

    // --- Priority, tags and lists ---

    @Test
    fun priorities() {
        assertEquals(Priority.HIGH, parse("Fix bug !high").priority)
        assertEquals(Priority.HIGH, parse("Fix bug !1").priority)
        assertEquals(Priority.HIGH, parse("Fix bug p1").priority)
        assertEquals(Priority.HIGH, parse("Fix bug !!!").priority)
        assertEquals(Priority.MEDIUM, parse("Fix bug !medium").priority)
        assertEquals(Priority.MEDIUM, parse("Fix bug !2").priority)
        assertEquals(Priority.LOW, parse("Fix bug !low").priority)
        assertEquals(Priority.LOW, parse("Fix bug p3").priority)
        assertEquals(Priority.NONE, parse("Fix bug!").priority)
        assertEquals("Fix bug", parse("Fix bug !high").title)
        assertEquals(Priority.HIGH, parse("اصلح الخطأ !عالية").priority)
        assertEquals(Priority.MEDIUM, parse("اصلح الخطأ !متوسطة").priority)
        assertEquals(Priority.LOW, parse("اصلح الخطأ !منخفضة").priority)
    }

    @Test
    fun tags() {
        val parsed = parse("Buy milk #groceries #home")
        assertEquals("Buy milk", parsed.title)
        assertEquals(listOf("groceries", "home"), parsed.tags)

        assertEquals(listOf("Home"), parse("Clean #Home #home").tags)
        assertEquals(emptyList<String>(), parse("Item #5").tags)
        assertEquals("Item #5", parse("Item #5").title)
        assertEquals(listOf("البقالة"), parse("اشتري حليب #البقالة").tags)
        assertEquals("اشتري حليب", parse("اشتري حليب #البقالة").title)
    }

    @Test
    fun listsMustExist() {
        val lists = listOf("Inbox", "Work", "Side Project")

        val work = parse("Draft report @work", lists)
        assertEquals("Work", work.listName)
        assertEquals("Draft report", work.title)

        assertEquals("Side Project", parse("Plan @side_project", lists).listName)

        val unknown = parse("Draft @nothing", lists)
        assertNull(unknown.listName)
        assertEquals("Draft @nothing", unknown.title)

        assertNull(parse("Draft @work").listName)
    }

    @Test
    fun everythingTogether() {
        val parsed = parse("Call mom tomorrow 5pm !high #family @Home", listOf("Home"))

        assertEquals("Call mom", parsed.title)
        assertEquals(LocalDate.of(2026, 10, 15), parsed.dueDate)
        assertEquals(LocalTime.of(17, 0), parsed.dueTime)
        assertEquals(Priority.HIGH, parsed.priority)
        assertEquals(listOf("family"), parsed.tags)
        assertEquals("Home", parsed.listName)
        assertEquals(
            setOf(
                QuickAddField.DATE,
                QuickAddField.TIME,
                QuickAddField.PRIORITY,
                QuickAddField.TAGS,
                QuickAddField.LIST,
            ),
            parsed.fields,
        )
    }

    @Test
    fun anImpliedDateHasNoChipOfItsOwn() {
        assertEquals(setOf(QuickAddField.TIME), parse("Lunch at noon").fields)
        assertEquals(setOf(QuickAddField.REPEAT), parse("Journal daily").fields)
    }

    // --- Switching fields off ---

    @Test
    fun aSwitchedOffFieldKeepsItsWordsInTheTitle() {
        val noDate = parse("Call mom tomorrow", disabled = setOf(QuickAddField.DATE))
        assertEquals("Call mom tomorrow", noDate.title)
        assertNull(noDate.dueDate)

        val noTime = parse("Dinner tomorrow at 5pm", disabled = setOf(QuickAddField.TIME))
        assertEquals("Dinner at 5pm", noTime.title)
        assertEquals(LocalDate.of(2026, 10, 15), noTime.dueDate)
        assertNull(noTime.dueTime)

        val noPriority = parse("Fix bug !high", disabled = setOf(QuickAddField.PRIORITY))
        assertEquals("Fix bug !high", noPriority.title)
        assertEquals(Priority.NONE, noPriority.priority)

        val noTags = parse("Buy milk #home", disabled = setOf(QuickAddField.TAGS))
        assertEquals("Buy milk #home", noTags.title)
        assertTrue(noTags.tags.isEmpty())

        val noList = parse("Draft @work", listOf("Work"), disabled = setOf(QuickAddField.LIST))
        assertEquals("Draft @work", noList.title)
        assertNull(noList.listName)

        assertNull(parse("Journal daily", disabled = setOf(QuickAddField.REPEAT)).recurrence)
    }

    // --- Text handling ---

    @Test
    fun normalizationUnifiesArabicSpellings() {
        assertEquals("الاحد", QuickText.normalize("الأحد"))
        assertEquals("34", QuickText.normalize("٣٤"))
        assertEquals("مدرسه", QuickText.normalize("مدرسةٌ"))
        assertEquals("hello", QuickText.normalize("HeLLo"))
    }

    @Test
    fun spacesAndSeparatorsAroundRemovedWordsAreTidied() {
        assertEquals("Call mom", parse("Call mom, tomorrow").title)
        assertEquals("Call mom please", parse("Call mom tomorrow please").title)
        assertEquals("Call mom", parse("  Call   mom   tomorrow  ").title)
    }
}

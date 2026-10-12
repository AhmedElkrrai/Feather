package com.elkrrai.feather.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock the test moves by hand so timestamps and ordering are deterministic. */
class TestClock(var millis: Long = 1_000_000L) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(millis)
    override fun millis(): Long = millis

    fun advance(byMillis: Long = 1_000L) {
        millis += byMillis
    }
}

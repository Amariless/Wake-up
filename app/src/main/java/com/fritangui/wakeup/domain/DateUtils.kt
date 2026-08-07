package com.fritangui.wakeup.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** Día de hoy como epoch-day (días desde 1970-01-01), la unidad que usan las tablas de uso diario. */
fun todayEpochDay(): Long = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()

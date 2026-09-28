package com.callankan.poloapp.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DeadlineState { OK, SOON, URGENT, EXPIRED, UNKNOWN }

/** Un vencimiento con cuenta atrás (ITV, seguro, documentos). */
data class Deadline(val date: LocalDate?, val daysLeft: Long?, val state: DeadlineState) {
    companion object {
        val UNKNOWN = Deadline(null, null, DeadlineState.UNKNOWN)

        fun of(date: LocalDate?, today: LocalDate, soonDays: Int = 30, urgentDays: Int = 7): Deadline {
            if (date == null) return UNKNOWN
            val days = ChronoUnit.DAYS.between(today, date)
            val state = when {
                days < 0 -> DeadlineState.EXPIRED
                days <= urgentDays -> DeadlineState.URGENT
                days <= soonDays -> DeadlineState.SOON
                else -> DeadlineState.OK
            }
            return Deadline(date, days, state)
        }
    }
}

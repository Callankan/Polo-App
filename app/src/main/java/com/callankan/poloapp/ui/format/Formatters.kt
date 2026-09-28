package com.callankan.poloapp.ui.format

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** Formato español (es-ES) para km, euros, litros y fechas. */
object Fmt {
    val locale: Locale = Locale.forLanguageTag("es-ES")

    private fun decimals(min: Int, max: Int) = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = min
        maximumFractionDigits = max
        isGroupingUsed = true
    }

    private val integer = decimals(0, 0)
    private val oneDecimal = decimals(1, 1)
    private val twoDecimals = decimals(2, 2)
    private val threeDecimals = decimals(3, 3)
    private val upToTwo = decimals(0, 2)

    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
    private val longDate = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", locale)
    private val numeric = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val shortMonth = DateTimeFormatter.ofPattern("MMM", locale)
    private val shortMonthYear = DateTimeFormatter.ofPattern("MMM yy", locale)

    fun number(value: Int): String = integer.format(value)
    fun number(value: Long): String = integer.format(value)
    fun km(value: Int?): String = value?.let { "${integer.format(it)} km" } ?: "—"
    fun kmPlain(value: Int?): String = value?.let { integer.format(it) } ?: "—"
    fun money(value: Double?): String = value?.let { "${twoDecimals.format(it)} €" } ?: "—"
    fun moneyRound(value: Double?): String = value?.let { "${integer.format(Math.round(it))} €" } ?: "—"
    fun moneyPlain(value: Double?): String = value?.let { twoDecimals.format(it) } ?: "—"
    fun liters(value: Double?): String = value?.let { "${twoDecimals.format(it)} l" } ?: "—"
    fun pricePerLiter(value: Double?): String = value?.let { "${threeDecimals.format(it)} €/l" } ?: "—"
    fun consumption(value: Double?): String = value?.let { "${twoDecimals.format(it)} l/100" } ?: "—"
    fun oneDecimal(value: Double?): String = value?.let { oneDecimal.format(it) } ?: "—"
    fun twoDecimals(value: Double?): String = value?.let { twoDecimals.format(it) } ?: "—"
    fun threeDecimals(value: Double?): String = value?.let { threeDecimals.format(it) } ?: "—"
    fun bar(value: Double?): String = value?.let { "${decimals(1, 2).format(it)} bar" } ?: "—"
    fun percent(fraction: Float): String = "${integer.format(Math.round(fraction * 100))} %"

    /** Valor para prellenar campos editables (sin separador de miles). */
    fun input(value: Double?): String = value?.let {
        NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            maximumFractionDigits = 3
        }.format(it)
    } ?: ""

    fun input(value: Int?): String = value?.toString() ?: ""
    fun inputMoney(value: Double?): String = value?.let { upToTwo.format(it).replace(".", "") } ?: ""

    fun date(date: LocalDate?): String = date?.format(dayMonthYear)?.replace(".", "") ?: "—"
    fun dayMonth(date: LocalDate?): String = date?.format(dayMonth)?.replace(".", "") ?: "—"
    fun longDate(date: LocalDate?): String = date?.format(longDate) ?: "—"
    fun numericDate(date: LocalDate?): String = date?.format(numeric) ?: "—"
    fun monthYear(month: YearMonth?): String = month?.format(monthYear)?.replaceFirstChar { it.uppercase(locale) } ?: "—"
    fun shortMonth(month: YearMonth): String = month.format(shortMonth).replace(".", "").replaceFirstChar { it.uppercase(locale) }
    fun shortMonthYear(month: YearMonth): String = month.format(shortMonthYear).replace(".", "").replaceFirstChar { it.uppercase(locale) }
    fun time(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

    /** "hoy", "mañana", "en 12 días", "hace 3 días"... */
    fun relativeDays(days: Long?): String = when {
        days == null -> "—"
        days == 0L -> "hoy"
        days == 1L -> "mañana"
        days == -1L -> "ayer"
        days > 0 && days < 60 -> "en $days días"
        days > 0 -> "en ${monthsText(days)}"
        abs(days) < 60 -> "hace ${abs(days)} días"
        else -> "hace ${monthsText(abs(days))}"
    }

    fun since(date: LocalDate?, today: LocalDate = LocalDate.now()): String =
        date?.let { relativeDays(ChronoUnit.DAYS.between(today, it)) } ?: "—"

    private fun monthsText(days: Long): String {
        val months = Math.round(days / 30.44)
        return if (months >= 24) "${months / 12} años" else if (months == 1L) "1 mes" else "$months meses"
    }

    fun duration(months: Int): String {
        val years = months / 12
        val rest = months % 12
        return buildList {
            if (years > 0) add(if (years == 1) "1 año" else "$years años")
            if (rest > 0) add(if (rest == 1) "1 mes" else "$rest meses")
        }.joinToString(" y ").ifEmpty { "0 meses" }
    }

    fun greeting(hour: Int): String = when (hour) {
        in 6..13 -> "Buenos días"
        in 14..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

/** Interpreta números escritos con coma o punto decimal ("1,459" o "1.459"). */
object NumberInput {
    fun parseDouble(text: String): Double? {
        val clean = text.trim().replace(" ", "").replace("€", "").replace(" ", "")
        if (clean.isEmpty()) return null
        val normalized = if (clean.contains(',') && clean.contains('.')) {
            clean.replace(".", "").replace(',', '.')
        } else clean.replace(',', '.')
        return normalized.toDoubleOrNull()
    }

    fun parseInt(text: String): Int? = text.trim().replace(".", "").replace(" ", "").toIntOrNull()

    /** Filtra la entrada de un campo decimal. */
    fun sanitizeDecimal(text: String): String {
        val filtered = text.filter { it.isDigit() || it == ',' || it == '.' }
        val firstSep = filtered.indexOfFirst { it == ',' || it == '.' }
        if (firstSep < 0) return filtered
        return filtered.substring(0, firstSep + 1) + filtered.substring(firstSep + 1).filter { it.isDigit() }
    }

    fun sanitizeInt(text: String): String = text.filter { it.isDigit() }
}

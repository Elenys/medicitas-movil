package com.medicitas.movil.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Las fechas viajan en UTC (ISO-8601) y se muestran en la hora de Colombia, como la web
 * (04-api §1.1: las fechas sin hora se interpretan en America/Bogota).
 */
object DateFormats {
    val ZONE: ZoneId = ZoneId.of("America/Bogota")
    private val ES = Locale.forLanguageTag("es-CO")

    private val dateLong = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES)
    private val dateShort = DateTimeFormatter.ofPattern("EEE d MMM", ES)
    private val time = DateTimeFormatter.ofPattern("h:mm a", ES)
    private val monthTitle = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES)
    private val dateTime = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", ES)

    fun longDate(instant: Instant): String = dateLong.format(instant.atZone(ZONE)).replaceFirstChar { it.uppercase() }
    fun longDate(date: LocalDate): String = dateLong.format(date).replaceFirstChar { it.uppercase() }
    fun shortDate(date: LocalDate): String = dateShort.format(date)
    fun time(instant: Instant): String = normalizeAmPm(time.format(instant.atZone(ZONE)))
    fun timeRange(start: Instant, end: Instant): String = "${time(start)} – ${time(end)}"
    fun month(month: YearMonth): String = monthTitle.format(month).replaceFirstChar { it.uppercase() }
    fun dateTime(instant: Instant): String = normalizeAmPm(dateTime.format(instant.atZone(ZONE)))
    fun today(): LocalDate = LocalDate.now(ZONE)

    /** Según la versión de Android, es-CO produce "a. m." o "a. m."; se unifica. */
    private fun normalizeAmPm(text: String) = text.replace(' ', ' ').replace(' ', ' ')
}

package com.medicitas.movil.ui

import com.medicitas.movil.util.DateFormats
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DateFormatsTest {
    @Test
    fun `las horas UTC de la API se muestran en hora de Colombia`() {
        // 19:00 UTC = 2:00 p. m. en Bogotá (UTC-5, sin horario de verano).
        val start = Instant.parse("2026-10-27T19:00:00Z")
        val end = Instant.parse("2026-10-27T19:30:00Z")
        assertEquals("Martes 27 de octubre de 2026", DateFormats.longDate(start))
        val range = DateFormats.timeRange(start, end).lowercase().replace(".", "").replace(" ", "")
        assertEquals("2:00pm–2:30pm", range)
    }

    @Test
    fun `una cita a medianoche UTC cae el dia anterior en Bogota`() {
        assertEquals("Lunes 26 de octubre de 2026", DateFormats.longDate(Instant.parse("2026-10-27T03:00:00Z")))
    }
}

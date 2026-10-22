package com.medicitas.movil.ui

import com.medicitas.movil.fakes.FakeAppointmentRepository
import com.medicitas.movil.fakes.FakeCatalogRepository
import com.medicitas.movil.fakes.MainDispatcherRule
import com.medicitas.movil.fakes.apiError
import com.medicitas.movil.ui.booking.BookingStep
import com.medicitas.movil.ui.booking.BookingViewModel
import com.medicitas.movil.domain.model.AvailabilityDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BookingViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val catalog = FakeCatalogRepository()
    private val appointments = FakeAppointmentRepository()
    private fun vm() = BookingViewModel(catalog, appointments, today = { today })

    @Test
    fun `recorrido completo especialidad, medico, dia, hora y reserva`() {
        val vm = vm()
        assertEquals(BookingStep.SPECIALTY, vm.state.value.step)
        assertEquals(listOf(catalog.general), (vm.state.value.specialties as UiState.Success).data)

        vm.selectSpecialty(catalog.general)
        assertEquals(BookingStep.DOCTOR, vm.state.value.step)

        vm.selectDoctor(catalog.doctor)
        // El calendario abre en el mes del próximo cupo del médico (noviembre).
        assertEquals(YearMonth.of(2026, 11), vm.state.value.month)
        assertEquals(BookingStep.DAY, vm.state.value.step)

        vm.selectDate(AvailabilityDay(LocalDate.of(2026, 11, 3), 2))
        assertEquals(BookingStep.SLOT, vm.state.value.step)

        vm.selectSlot(catalog.taken) // ocupado: se ignora
        assertEquals(BookingStep.SLOT, vm.state.value.step)
        vm.selectSlot(catalog.slot)
        assertEquals(BookingStep.CONFIRM, vm.state.value.step)

        vm.onReasonChange("Control anual")
        vm.submit()
        assertEquals(BookingStep.DONE, vm.state.value.step)
        assertNotNull(vm.state.value.booked)
        assertEquals(Triple(9, catalog.slot.startsAt, "Control anual"), appointments.lastBooking)
    }

    @Test
    fun `dia sin cupos no se puede elegir`() {
        val vm = vm()
        vm.selectSpecialty(catalog.general)
        vm.selectDoctor(catalog.doctor)
        vm.selectDate(AvailabilityDay(LocalDate.of(2026, 11, 2), 0))
        assertEquals(BookingStep.DAY, vm.state.value.step)
        assertNull(vm.state.value.date)
    }

    @Test
    fun `el calendario no pasa de la ventana de 60 dias`() {
        val vm = vm()
        vm.selectSpecialty(catalog.general)
        vm.selectDoctor(catalog.doctor) // noviembre
        assertTrue(vm.state.value.canGoNextMonth)
        vm.nextMonth() // diciembre (22 oct + 60 días = 21 dic)
        assertEquals(YearMonth.of(2026, 12), vm.state.value.month)
        assertFalse(vm.state.value.canGoNextMonth)
        vm.nextMonth()
        assertEquals(YearMonth.of(2026, 12), vm.state.value.month)
        vm.prevMonth(); vm.prevMonth()
        assertEquals(YearMonth.of(2026, 10), vm.state.value.month)
        assertFalse(vm.state.value.canGoPrevMonth)
    }

    @Test
    fun `cupo tomado por otra persona vuelve a las horas y las recarga`() {
        appointments.bookResult = apiError("SLOT_TAKEN", "Ese horario acaba de ser reservado por otra persona. Elija otra franja.")
        val vm = vm()
        vm.selectSpecialty(catalog.general)
        vm.selectDoctor(catalog.doctor)
        vm.selectDate(AvailabilityDay(LocalDate.of(2026, 11, 3), 2))
        vm.selectSlot(catalog.slot)
        val loadsBefore = catalog.slotLoads
        vm.submit()
        val s = vm.state.value
        assertEquals(BookingStep.SLOT, s.step)
        assertNull(s.slot)
        assertEquals("Ese horario acaba de ser reservado por otra persona. Elija otra franja.", s.error)
        assertEquals(loadsBefore + 1, catalog.slotLoads)
    }

    @Test
    fun `atras retrocede un paso y en el primero devuelve false`() {
        val vm = vm()
        vm.selectSpecialty(catalog.general)
        vm.selectDoctor(catalog.doctor)
        assertTrue(vm.back())
        assertEquals(BookingStep.DOCTOR, vm.state.value.step)
        assertTrue(vm.back())
        assertFalse(vm.back())
    }
}

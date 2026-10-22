package com.medicitas.movil.ui

import com.medicitas.movil.domain.model.AppointmentScope
import com.medicitas.movil.domain.model.AppointmentStatus
import com.medicitas.movil.domain.model.Notification
import com.medicitas.movil.domain.model.NotificationPage
import com.medicitas.movil.fakes.FakeAppointmentRepository
import com.medicitas.movil.fakes.FakeNotificationRepository
import com.medicitas.movil.fakes.MainDispatcherRule
import com.medicitas.movil.fakes.apiError
import com.medicitas.movil.ui.appointments.AppointmentsViewModel
import com.medicitas.movil.ui.detail.AppointmentDetailViewModel
import com.medicitas.movil.ui.notifications.NotificationsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class AppointmentViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `panel carga las proximas y cambia a historial`() {
        val repo = FakeAppointmentRepository()
        val vm = AppointmentsViewModel(repo)
        vm.load()
        assertEquals(AppointmentScope.UPCOMING, repo.lastScope)
        assertEquals(1, (vm.state.value as UiState.Success).data.size)
        vm.selectScope(AppointmentScope.HISTORY)
        assertEquals(AppointmentScope.HISTORY, repo.lastScope)
    }

    @Test
    fun `panel muestra el error de la API`() {
        val repo = FakeAppointmentRepository().apply {
            listResult = apiError("UNAUTHENTICATED", "Su sesión venció. Inicie sesión de nuevo.")
        }
        val vm = AppointmentsViewModel(repo)
        vm.load()
        assertEquals(UiState.Error("Su sesión venció. Inicie sesión de nuevo."), vm.state.value)
    }

    @Test
    fun `confirmar actualiza el estado y avisa`() {
        val vm = AppointmentDetailViewModel(70, FakeAppointmentRepository())
        vm.confirm()
        val appt = (vm.state.value as UiState.Success).data
        assertEquals(AppointmentStatus.CONFIRMED, appt.status)
        assertFalse(appt.canConfirm)
        assertEquals("Su asistencia quedó confirmada.", vm.action.value.message)
    }

    @Test
    fun `cancelar exige motivo de al menos 3 caracteres`() {
        val repo = FakeAppointmentRepository()
        val vm = AppointmentDetailViewModel(70, repo)
        assertFalse(vm.cancel(" a "))
        assertEquals("Indique el motivo de la cancelación (mínimo 3 caracteres).", vm.action.value.error)
        assertNull(repo.lastCancelReason)

        assertTrue(vm.cancel("Viaje de trabajo"))
        assertEquals("Viaje de trabajo", repo.lastCancelReason)
        assertEquals(AppointmentStatus.CANCELLED, (vm.state.value as UiState.Success).data.status)
    }

    @Test
    fun `avisos marca como leido y baja el contador`() {
        val n1 = Notification(1, "APPOINTMENT_BOOKED", "Reservada", 70, false, Instant.EPOCH)
        val n2 = Notification(2, "APPOINTMENT_CONFIRMED", "Confirmada", 70, false, Instant.EPOCH)
        val repo = FakeNotificationRepository(NotificationPage(listOf(n1, n2), unreadCount = 2, total = 2))
        val vm = NotificationsViewModel(repo)
        vm.open(n1)
        assertEquals(listOf(1), repo.readIds)
        assertEquals(1, (vm.state.value as UiState.Success).data.unreadCount)
        vm.markAllRead()
        val page = (vm.state.value as UiState.Success).data
        assertEquals(0, page.unreadCount)
        assertTrue(page.items.all { it.isRead })
    }
}

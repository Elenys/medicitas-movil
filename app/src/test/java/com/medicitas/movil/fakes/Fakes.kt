package com.medicitas.movil.fakes

import com.medicitas.movil.data.session.ServerUrlStore
import com.medicitas.movil.domain.model.ApiException
import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.model.AppointmentScope
import com.medicitas.movil.domain.model.AppointmentStatus
import com.medicitas.movil.domain.model.AvailabilityDay
import com.medicitas.movil.domain.model.Doctor
import com.medicitas.movil.domain.model.NotificationPage
import com.medicitas.movil.domain.model.Role
import com.medicitas.movil.domain.model.Slot
import com.medicitas.movil.domain.model.Specialty
import com.medicitas.movil.domain.model.User
import com.medicitas.movil.domain.repository.AppointmentRepository
import com.medicitas.movil.domain.repository.AuthRepository
import com.medicitas.movil.domain.repository.CatalogRepository
import com.medicitas.movil.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Reemplaza Dispatchers.Main (viewModelScope) por un despachador de pruebas. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = kotlinx.coroutines.Dispatchers.resetMain()
}

class MemoryUrlStore(var value: String? = null) : ServerUrlStore {
    override fun read() = value
    override fun write(url: String) { value = url }
}

val patient = User(32, Role.PATIENT, "Sofía", "Herrera", "paciente@correo.com")

fun appointment(
    id: Int = 70,
    status: AppointmentStatus = AppointmentStatus.BOOKED,
    canConfirm: Boolean = true,
    canCancel: Boolean = true,
) = Appointment(
    id = id,
    startsAt = Instant.parse("2026-10-27T19:00:00Z"),
    endsAt = Instant.parse("2026-10-27T19:30:00Z"),
    status = status,
    reason = "Control",
    cancellationReason = null,
    doctorName = "Jorge Salazar",
    specialty = "Cardiología",
    canConfirm = canConfirm,
    canCancel = canCancel,
)

class FakeAuthRepository(var result: Result<User> = Result.success(patient)) : AuthRepository {
    override var currentUser: User? = null
    var calls = 0
    override suspend fun login(email: String, password: String): Result<User> {
        calls++
        return result.onSuccess { currentUser = it }
    }
    override suspend fun logout() { currentUser = null }
}

class FakeAppointmentRepository : AppointmentRepository {
    var listResult: Result<List<Appointment>> = Result.success(listOf(appointment()))
    var bookResult: Result<Appointment> = Result.success(appointment())
    var lastScope: AppointmentScope? = null
    var lastCancelReason: String? = null
    var lastBooking: Triple<Int, Instant, String?>? = null

    override suspend fun list(scope: AppointmentScope) = listResult.also { lastScope = scope }
    override suspend fun detail(id: Int) = Result.success(appointment(id))
    override suspend fun confirm(id: Int) =
        Result.success(appointment(id, AppointmentStatus.CONFIRMED, canConfirm = false))
    override suspend fun cancel(id: Int, reason: String): Result<Appointment> {
        lastCancelReason = reason
        return Result.success(appointment(id, AppointmentStatus.CANCELLED, canConfirm = false, canCancel = false))
    }
    override suspend fun book(doctorId: Int, startsAt: Instant, reason: String?): Result<Appointment> {
        lastBooking = Triple(doctorId, startsAt, reason)
        return bookResult
    }
}

class FakeCatalogRepository : CatalogRepository {
    val general = Specialty(7, "Medicina General", "Atención primaria")
    val doctor = Doctor(9, "Laura Medina", "Medicina General", 30, LocalDate.of(2026, 11, 3))
    val slot = Slot(Instant.parse("2026-11-03T13:00:00Z"), Instant.parse("2026-11-03T13:30:00Z"), true)
    val taken = Slot(Instant.parse("2026-11-03T12:30:00Z"), Instant.parse("2026-11-03T13:00:00Z"), false)
    val requestedMonths = mutableListOf<YearMonth>()
    var slotLoads = 0

    override suspend fun specialties() = Result.success(listOf(general))
    override suspend fun doctors(specialtyId: Int) = Result.success(listOf(doctor))
    override suspend fun availability(doctorId: Int, month: YearMonth): Result<List<AvailabilityDay>> {
        requestedMonths += month
        return Result.success(listOf(AvailabilityDay(LocalDate.of(2026, 11, 2), 0), AvailabilityDay(LocalDate.of(2026, 11, 3), 2)))
    }
    override suspend fun slots(doctorId: Int, date: LocalDate): Result<List<Slot>> {
        slotLoads++
        return Result.success(listOf(taken, slot))
    }
}

class FakeNotificationRepository(var page: NotificationPage) : NotificationRepository {
    val readIds = mutableListOf<Int>()
    override suspend fun list() = Result.success(page)
    override suspend fun markRead(id: Int): Result<Unit> { readIds += id; return Result.success(Unit) }
    override suspend fun markAllRead() = Result.success(Unit)
}

fun apiError(code: String, message: String) = Result.failure<Nothing>(ApiException(code, message))

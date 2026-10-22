package com.medicitas.movil.data.repository

import com.medicitas.movil.data.remote.MediCitasApi
import com.medicitas.movil.data.remote.apiCall
import com.medicitas.movil.data.remote.dto.CancelRequest
import com.medicitas.movil.data.remote.dto.CreateAppointmentRequest
import com.medicitas.movil.data.remote.dto.LoginRequest
import com.medicitas.movil.data.remote.orThrow
import com.medicitas.movil.data.session.SessionCookieJar
import com.medicitas.movil.domain.model.ApiException
import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.model.AppointmentScope
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class RemoteAuthRepository(
    private val api: MediCitasApi,
    private val jar: SessionCookieJar,
) : AuthRepository {

    @Volatile
    override var currentUser: User? = null
        private set

    override suspend fun login(email: String, password: String): Result<User> =
        apiCall { api.login(LoginRequest(email.trim(), password)).toDomain() }
            .mapCatching { user ->
                // Esta app es el módulo del PACIENTE. Médicos y administradores usan la web.
                if (user.role != Role.PATIENT) {
                    runCatching { api.logout() }
                    jar.clear()
                    throw ApiException(
                        code = "ROLE_NOT_SUPPORTED",
                        message = "MediCitas Móvil es para pacientes. Médicos y administradores ingresan por la versión web.",
                    )
                }
                user
            }
            .onSuccess { currentUser = it }

    override suspend fun logout() {
        currentUser = null
        // POST /auth/logout nunca falla en el servidor; aunque no haya red, la sesión local se borra.
        runCatching { api.logout() }
        jar.clear()
    }
}

class RemoteAppointmentRepository(private val api: MediCitasApi) : AppointmentRepository {

    override suspend fun list(scope: AppointmentScope): Result<List<Appointment>> =
        apiCall { api.appointments(scope.query).items.map { it.toDomain() } }

    override suspend fun detail(id: Int): Result<Appointment> =
        apiCall { api.appointment(id).toDomain() }

    override suspend fun confirm(id: Int): Result<Appointment> =
        apiCall { api.confirm(id).toDomain() }

    override suspend fun cancel(id: Int, reason: String): Result<Appointment> =
        apiCall { api.cancel(id, CancelRequest(reason.trim())).toDomain() }

    override suspend fun book(doctorId: Int, startsAt: Instant, reason: String?): Result<Appointment> =
        apiCall {
            api.createAppointment(
                CreateAppointmentRequest(
                    doctorId = doctorId,
                    startsAt = startsAt.toString(),
                    reason = reason?.trim()?.takeIf { it.isNotEmpty() },
                ),
            ).toDomain()
        }
}

class RemoteCatalogRepository(private val api: MediCitasApi) : CatalogRepository {

    override suspend fun specialties(): Result<List<Specialty>> =
        apiCall { api.specialties().map { it.toDomain() } }

    override suspend fun doctors(specialtyId: Int): Result<List<Doctor>> =
        apiCall { api.doctors(specialtyId).items.map { it.toDomain() } }

    override suspend fun availability(doctorId: Int, month: YearMonth): Result<List<AvailabilityDay>> =
        apiCall { api.availability(doctorId, month.toString()).days.map { it.toDomain() } }

    override suspend fun slots(doctorId: Int, date: LocalDate): Result<List<Slot>> =
        apiCall { api.availableSlots(doctorId, date.toString()).slots.map { it.toDomain() } }
}

class RemoteNotificationRepository(private val api: MediCitasApi) : NotificationRepository {

    override suspend fun list(): Result<NotificationPage> =
        apiCall { api.notifications().toDomain() }

    override suspend fun markRead(id: Int): Result<Unit> =
        apiCall { api.markNotificationRead(id).orThrow() }

    override suspend fun markAllRead(): Result<Unit> =
        apiCall { api.markAllNotificationsRead().orThrow() }
}

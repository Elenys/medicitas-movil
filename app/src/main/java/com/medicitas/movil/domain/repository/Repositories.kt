package com.medicitas.movil.domain.repository

import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.model.AppointmentScope
import com.medicitas.movil.domain.model.AvailabilityDay
import com.medicitas.movil.domain.model.Doctor
import com.medicitas.movil.domain.model.NotificationPage
import com.medicitas.movil.domain.model.Slot
import com.medicitas.movil.domain.model.Specialty
import com.medicitas.movil.domain.model.User
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/*
 * Contratos de la capa de dominio. La interfaz de usuario depende de estas interfaces y no de
 * Retrofit: así los ViewModel se prueban con implementaciones falsas (ver src/test).
 * Todas las operaciones devuelven Result; el fallo siempre es una ApiException.
 */

interface AuthRepository {
    /** Usuario de la sesión activa, o null si no hay sesión. */
    val currentUser: User?
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
}

interface AppointmentRepository {
    suspend fun list(scope: AppointmentScope): Result<List<Appointment>>
    suspend fun detail(id: Int): Result<Appointment>
    suspend fun confirm(id: Int): Result<Appointment>
    suspend fun cancel(id: Int, reason: String): Result<Appointment>
    suspend fun book(doctorId: Int, startsAt: Instant, reason: String?): Result<Appointment>
}

interface CatalogRepository {
    suspend fun specialties(): Result<List<Specialty>>
    suspend fun doctors(specialtyId: Int): Result<List<Doctor>>
    suspend fun availability(doctorId: Int, month: YearMonth): Result<List<AvailabilityDay>>
    suspend fun slots(doctorId: Int, date: LocalDate): Result<List<Slot>>
}

interface NotificationRepository {
    suspend fun list(): Result<NotificationPage>
    suspend fun markRead(id: Int): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
}

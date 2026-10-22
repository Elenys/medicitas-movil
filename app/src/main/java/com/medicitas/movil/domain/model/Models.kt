package com.medicitas.movil.domain.model

import java.time.Instant
import java.time.LocalDate

/** Rol del usuario autenticado (users.role). */
enum class Role { PATIENT, DOCTOR, ADMIN }

data class User(
    val id: Int,
    val role: Role,
    val firstName: String,
    val lastName: String,
    val email: String,
) {
    val fullName: String get() = "$firstName $lastName"
}

/** Estados de cita (RN-23). La etiqueta en español es la misma de la web (06 §1.2). */
enum class AppointmentStatus(val label: String) {
    BOOKED("Reservada"),
    CONFIRMED("Confirmada"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada"),
}

data class Appointment(
    val id: Int,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: AppointmentStatus,
    val reason: String?,
    val cancellationReason: String?,
    val doctorName: String,
    val specialty: String,
    /** Los permisos los calcula el servidor (RN-23 a RN-26); la app solo los lee. */
    val canConfirm: Boolean,
    val canCancel: Boolean,
)

enum class AppointmentScope(val query: String) { UPCOMING("upcoming"), HISTORY("history") }

data class Specialty(val id: Int, val name: String, val description: String?)

data class Doctor(
    val id: Int,
    val fullName: String,
    val specialtyName: String,
    val durationMinutes: Int,
    val nextAvailableDate: LocalDate?,
)

data class AvailabilityDay(val date: LocalDate, val availableSlots: Int) {
    val isSelectable: Boolean get() = availableSlots > 0
}

data class Slot(val startsAt: Instant, val endsAt: Instant, val available: Boolean)

data class Notification(
    val id: Int,
    val type: String,
    val message: String,
    val appointmentId: Int?,
    val isRead: Boolean,
    val createdAt: Instant,
)

data class NotificationPage(val items: List<Notification>, val unreadCount: Int, val total: Int)

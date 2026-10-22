package com.medicitas.movil.data.repository

import com.medicitas.movil.data.remote.dto.AppointmentDto
import com.medicitas.movil.data.remote.dto.AvailabilityDayDto
import com.medicitas.movil.data.remote.dto.DoctorDto
import com.medicitas.movil.data.remote.dto.NotificationDto
import com.medicitas.movil.data.remote.dto.NotificationPageDto
import com.medicitas.movil.data.remote.dto.SlotDto
import com.medicitas.movil.data.remote.dto.SpecialtyDto
import com.medicitas.movil.data.remote.dto.UserDto
import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.model.AppointmentStatus
import com.medicitas.movil.domain.model.AvailabilityDay
import com.medicitas.movil.domain.model.Doctor
import com.medicitas.movil.domain.model.Notification
import com.medicitas.movil.domain.model.NotificationPage
import com.medicitas.movil.domain.model.Role
import com.medicitas.movil.domain.model.Slot
import com.medicitas.movil.domain.model.Specialty
import com.medicitas.movil.domain.model.User
import java.time.Instant
import java.time.LocalDate

/* Traducción DTO (JSON de la API) → modelo de dominio. */

fun UserDto.toDomain() = User(id, Role.valueOf(role), firstName, lastName, email)

fun AppointmentDto.toDomain() = Appointment(
    id = id,
    startsAt = Instant.parse(startsAt),
    endsAt = Instant.parse(endsAt),
    status = AppointmentStatus.valueOf(status),
    reason = reason,
    cancellationReason = cancellationReason,
    doctorName = doctor.fullName,
    specialty = doctor.specialty,
    canConfirm = canConfirm,
    canCancel = canCancel,
)

fun SpecialtyDto.toDomain() = Specialty(id, name, description)

fun DoctorDto.toDomain() = Doctor(
    id = id,
    fullName = fullName,
    specialtyName = specialty.name,
    durationMinutes = appointmentDurationMinutes,
    nextAvailableDate = nextAvailableDate?.let(LocalDate::parse),
)

fun AvailabilityDayDto.toDomain() = AvailabilityDay(LocalDate.parse(date), availableSlots)

fun SlotDto.toDomain() = Slot(Instant.parse(startsAt), Instant.parse(endsAt), available)

fun NotificationDto.toDomain() = Notification(
    id = id,
    type = type,
    message = message,
    appointmentId = appointmentId,
    isRead = readAt != null,
    createdAt = Instant.parse(createdAt),
)

fun NotificationPageDto.toDomain() = NotificationPage(items.map { it.toDomain() }, unreadCount, total)

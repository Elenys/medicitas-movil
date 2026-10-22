package com.medicitas.movil.data.remote.dto

import kotlinx.serialization.Serializable

/*
 * Objetos de transferencia: reflejan EXACTAMENTE el JSON de la API de MediCitas
 * (especificacion/openapi.json). Los campos que la app no usa se ignoran al deserializar.
 */

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class UserDto(
    val id: Int,
    val role: String,
    val firstName: String,
    val lastName: String,
    val email: String,
)

@Serializable
data class PersonRefDto(val id: Int, val fullName: String)

@Serializable
data class AppointmentDoctorDto(val id: Int, val fullName: String, val specialty: String)

@Serializable
data class AppointmentDto(
    val id: Int,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val reason: String? = null,
    val cancellationReason: String? = null,
    val patient: PersonRefDto? = null,
    val doctor: AppointmentDoctorDto,
    val canConfirm: Boolean = false,
    val canCancel: Boolean = false,
)

@Serializable
data class PageDto<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class CreateAppointmentRequest(val doctorId: Int, val startsAt: String, val reason: String? = null)

@Serializable
data class CancelRequest(val reason: String)

@Serializable
data class SpecialtyDto(val id: Int, val name: String, val description: String? = null)

@Serializable
data class SpecialtyRefDto(val id: Int, val name: String)

@Serializable
data class DoctorDto(
    val id: Int,
    val fullName: String,
    val specialty: SpecialtyRefDto,
    val appointmentDurationMinutes: Int,
    val nextAvailableDate: String? = null,
)

@Serializable
data class AvailabilityDayDto(val date: String, val availableSlots: Int)

@Serializable
data class AvailabilityDto(val month: String, val days: List<AvailabilityDayDto>)

@Serializable
data class SlotDto(val startsAt: String, val endsAt: String, val available: Boolean)

@Serializable
data class SlotsDto(val date: String, val durationMinutes: Int, val slots: List<SlotDto>)

@Serializable
data class NotificationDto(
    val id: Int,
    val type: String,
    val message: String,
    val appointmentId: Int? = null,
    val readAt: String? = null,
    val createdAt: String,
)

@Serializable
data class NotificationPageDto(
    val items: List<NotificationDto>,
    val unreadCount: Int,
    val total: Int,
)

@Serializable
data class ErrorEnvelopeDto(val error: ErrorDto)

@Serializable
data class ErrorDto(
    val code: String,
    val message: String,
    val details: List<ErrorDetailDto> = emptyList(),
)

@Serializable
data class ErrorDetailDto(val field: String = "", val message: String)

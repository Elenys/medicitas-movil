package com.medicitas.movil.data.remote

import com.medicitas.movil.data.remote.dto.AppointmentDto
import com.medicitas.movil.data.remote.dto.AvailabilityDto
import com.medicitas.movil.data.remote.dto.CancelRequest
import com.medicitas.movil.data.remote.dto.CreateAppointmentRequest
import com.medicitas.movil.data.remote.dto.DoctorDto
import com.medicitas.movil.data.remote.dto.LoginRequest
import com.medicitas.movil.data.remote.dto.NotificationPageDto
import com.medicitas.movil.data.remote.dto.PageDto
import com.medicitas.movil.data.remote.dto.SlotsDto
import com.medicitas.movil.data.remote.dto.SpecialtyDto
import com.medicitas.movil.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints de la API REST de MediCitas que usa el módulo del paciente
 * (numeración de especificacion/04-api.md §2). La sesión viaja en la cookie
 * `medicitas_session`, que gestiona [com.medicitas.movil.data.session.SessionCookieJar].
 */
interface MediCitasApi {

    /** #2 */
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): UserDto

    /** #3 — 204 sin cuerpo. */
    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    /** #8 */
    @GET("api/v1/specialties")
    suspend fun specialties(): List<SpecialtyDto>

    /** #9 — solo médicos activos con horario (RN-16). */
    @GET("api/v1/doctors")
    suspend fun doctors(
        @Query("specialtyId") specialtyId: Int,
        @Query("pageSize") pageSize: Int = 50,
    ): PageDto<DoctorDto>

    /** #11 — month = YYYY-MM. */
    @GET("api/v1/doctors/{id}/availability")
    suspend fun availability(@Path("id") doctorId: Int, @Query("month") month: String): AvailabilityDto

    /** #12 — date = YYYY-MM-DD. */
    @GET("api/v1/doctors/{id}/available-slots")
    suspend fun availableSlots(@Path("id") doctorId: Int, @Query("date") date: String): SlotsDto

    /** #16 — scope = upcoming | history. */
    @GET("api/v1/appointments")
    suspend fun appointments(
        @Query("scope") scope: String,
        @Query("pageSize") pageSize: Int = 50,
    ): PageDto<AppointmentDto>

    /** #17 — el cliente no envía patientId ni endsAt (RN-20, RN-21.4). */
    @POST("api/v1/appointments")
    suspend fun createAppointment(@Body body: CreateAppointmentRequest): AppointmentDto

    /** #18 */
    @GET("api/v1/appointments/{id}")
    suspend fun appointment(@Path("id") id: Int): AppointmentDto

    /** #19 */
    @PATCH("api/v1/appointments/{id}/confirm")
    suspend fun confirm(@Path("id") id: Int): AppointmentDto

    /** #20 — reason obligatorio, 3 a 255 caracteres (RN-24.1). */
    @PATCH("api/v1/appointments/{id}/cancel")
    suspend fun cancel(@Path("id") id: Int, @Body body: CancelRequest): AppointmentDto

    /** #26 */
    @GET("api/v1/notifications")
    suspend fun notifications(@Query("pageSize") pageSize: Int = 50): NotificationPageDto

    /** #27 — 204 sin cuerpo. */
    @PATCH("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): Response<Unit>

    /** #28 */
    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>
}

package com.medicitas.movil.data

import com.medicitas.movil.data.repository.RemoteAppointmentRepository
import com.medicitas.movil.data.repository.RemoteAuthRepository
import com.medicitas.movil.data.repository.RemoteCatalogRepository
import com.medicitas.movil.data.repository.RemoteNotificationRepository
import com.medicitas.movil.data.session.SessionCookieJar
import com.medicitas.movil.data.session.SessionEvents
import com.medicitas.movil.di.AppContainer
import com.medicitas.movil.domain.model.ApiException
import com.medicitas.movil.domain.model.AppointmentScope
import com.medicitas.movil.domain.model.AppointmentStatus
import com.medicitas.movil.domain.model.Role
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Prueba la capa de datos completa (Retrofit + OkHttp + CookieJar + parseo) contra un servidor
 * HTTP simulado que responde con JSON copiado de la API real de MediCitas.
 */
class RemoteRepositoriesTest {

    private lateinit var server: MockWebServer
    private lateinit var jar: SessionCookieJar
    private lateinit var events: SessionEvents
    private lateinit var auth: RemoteAuthRepository
    private lateinit var appointments: RemoteAppointmentRepository
    private lateinit var catalog: RemoteCatalogRepository
    private lateinit var notifications: RemoteNotificationRepository

    private val appointmentJson = """{"id":70,"startsAt":"2026-10-27T19:00:00.000Z","endsAt":"2026-10-27T19:30:00.000Z",
        "status":"CONFIRMED","reason":"Dolor en el pecho al caminar","notes":null,"cancellationReason":null,"cancelledBy":null,
        "createdAt":"2026-10-20T20:36:50.762Z","patient":{"id":14,"fullName":"Sofía Herrera"},
        "doctor":{"id":12,"fullName":"Jorge Salazar","specialty":"Cardiología","appointmentDurationMinutes":30},
        "canConfirm":false,"canCancel":true,"canComplete":false}"""

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        jar = SessionCookieJar(allowSecureCookiesOverHttp = true)
        events = SessionEvents()
        val client = AppContainer.createHttpClient(jar, events, serverConfig = null, logBodies = false)
        val api = AppContainer.createApi(server.url("/").toString(), client)
        auth = RemoteAuthRepository(api, jar)
        appointments = RemoteAppointmentRepository(api)
        catalog = RemoteCatalogRepository(api)
        notifications = RemoteNotificationRepository(api)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(body: String, code: Int = 200) = MockResponse().setResponseCode(code)
        .setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun `login guarda la cookie y la siguiente peticion la envia`() = runTest {
        server.enqueue(
            json("""{"id":32,"role":"PATIENT","firstName":"Sofía","lastName":"Herrera","email":"paciente@correo.com"}""")
                .addHeader("Set-Cookie", "medicitas_session=abc.def.ghi; Path=/; Max-Age=28800; Secure; HttpOnly; SameSite=lax"),
        )
        server.enqueue(json("""{"items":[$appointmentJson],"page":1,"pageSize":50,"total":1,"totalPages":1}"""))

        val user = auth.login(" paciente@correo.com ", "Medicitas2026").getOrThrow()
        assertEquals(Role.PATIENT, user.role)
        assertEquals("Sofía Herrera", user.fullName)
        assertEquals(user, auth.currentUser)

        val loginRequest = server.takeRequest()
        assertEquals("POST", loginRequest.method)
        assertEquals("/api/v1/auth/login", loginRequest.path)
        assertEquals("""{"email":"paciente@correo.com","password":"Medicitas2026"}""", loginRequest.body.readUtf8())

        val list = appointments.list(AppointmentScope.UPCOMING).getOrThrow()
        val listRequest = server.takeRequest()
        assertEquals("/api/v1/appointments?scope=upcoming&pageSize=50", listRequest.path)
        assertEquals("medicitas_session=abc.def.ghi", listRequest.getHeader("Cookie"))
        assertEquals(1, list.size)
        assertEquals(AppointmentStatus.CONFIRMED, list[0].status)
        assertEquals(Instant.parse("2026-10-27T19:00:00Z"), list[0].startsAt)
        assertTrue(list[0].canCancel)
        assertEquals("Cardiología", list[0].specialty)
    }

    @Test
    fun `credenciales invalidas devuelven el mensaje del servidor`() = runTest {
        server.enqueue(json("""{"error":{"code":"INVALID_CREDENTIALS","message":"Correo o contraseña incorrectos."}}""", 401))
        val e = auth.login("x@y.com", "mala").exceptionOrNull() as ApiException
        assertEquals("INVALID_CREDENTIALS", e.code)
        assertEquals("Correo o contraseña incorrectos.", e.message)
        assertNull(auth.currentUser)
    }

    @Test
    fun `un medico no puede usar la app de pacientes`() = runTest {
        server.enqueue(json("""{"id":9,"role":"DOCTOR","firstName":"Laura","lastName":"Medina","email":"medico@medicitas.com"}"""))
        server.enqueue(MockResponse().setResponseCode(204)) // logout
        val e = auth.login("medico@medicitas.com", "Medicitas2026").exceptionOrNull() as ApiException
        assertEquals("ROLE_NOT_SUPPORTED", e.code)
        server.takeRequest()
        assertEquals("/api/v1/auth/logout", server.takeRequest().path)
    }

    @Test
    fun `reservar envia doctorId, startsAt ISO y motivo, sin patientId ni endsAt`() = runTest {
        server.enqueue(json(appointmentJson.replace("CONFIRMED", "BOOKED"), 201))
        val result = appointments.book(12, Instant.parse("2026-10-27T19:00:00Z"), "  Control  ").getOrThrow()
        assertEquals(AppointmentStatus.BOOKED, result.status)
        val req = server.takeRequest()
        assertEquals("/api/v1/appointments", req.path)
        assertEquals("""{"doctorId":12,"startsAt":"2026-10-27T19:00:00Z","reason":"Control"}""", req.body.readUtf8())
    }

    @Test
    fun `cupo ocupado se traduce a SLOT_TAKEN`() = runTest {
        server.enqueue(json("""{"error":{"code":"SLOT_TAKEN","message":"Ese horario acaba de ser reservado por otra persona. Elija otra franja."}}""", 409))
        val e = appointments.book(12, Instant.parse("2026-10-27T19:00:00Z"), null).exceptionOrNull() as ApiException
        assertEquals("SLOT_TAKEN", e.code)
        assertEquals(409, e.httpStatus)
    }

    @Test
    fun `cancelar envia el motivo y lee CANCELLATION_WINDOW_CLOSED`() = runTest {
        server.enqueue(json("""{"error":{"code":"CANCELLATION_WINDOW_CLOSED","message":"Solo puede cancelar hasta 24 horas antes."}}""", 403))
        val e = appointments.cancel(70, " Viaje ").exceptionOrNull() as ApiException
        assertEquals("CANCELLATION_WINDOW_CLOSED", e.code)
        val req = server.takeRequest()
        assertEquals("PATCH", req.method)
        assertEquals("/api/v1/appointments/70/cancel", req.path)
        assertEquals("""{"reason":"Viaje"}""", req.body.readUtf8())
    }

    @Test
    fun `catalogo, disponibilidad y cupos se leen con sus parametros`() = runTest {
        server.enqueue(json("""[{"id":7,"name":"Medicina General","description":"Atención primaria y valoración inicial"}]"""))
        server.enqueue(json("""{"items":[{"id":9,"fullName":"Laura Medina","specialty":{"id":7,"name":"Medicina General"},"appointmentDurationMinutes":30,"nextAvailableDate":"2026-10-22"}],"page":1,"pageSize":50,"total":1,"totalPages":1}"""))
        server.enqueue(json("""{"month":"2026-10","days":[{"date":"2026-10-24","availableSlots":0},{"date":"2026-10-26","availableSlots":17}]}"""))
        server.enqueue(json("""{"date":"2026-10-26","durationMinutes":30,"slots":[{"startsAt":"2026-10-26T12:00:00.000Z","endsAt":"2026-10-26T12:30:00.000Z","available":false}]}"""))

        assertEquals("Medicina General", catalog.specialties().getOrThrow().single().name)
        val doctor = catalog.doctors(7).getOrThrow().single()
        assertEquals(LocalDate.of(2026, 10, 22), doctor.nextAvailableDate)
        val days = catalog.availability(9, YearMonth.of(2026, 10)).getOrThrow()
        assertFalse(days[0].isSelectable)
        assertTrue(days[1].isSelectable)
        val slots = catalog.slots(9, LocalDate.of(2026, 10, 26)).getOrThrow()
        assertFalse(slots.single().available)

        assertEquals("/api/v1/specialties", server.takeRequest().path)
        assertEquals("/api/v1/doctors?specialtyId=7&pageSize=50", server.takeRequest().path)
        assertEquals("/api/v1/doctors/9/availability?month=2026-10", server.takeRequest().path)
        assertEquals("/api/v1/doctors/9/available-slots?date=2026-10-26", server.takeRequest().path)
    }

    @Test
    fun `notificaciones con contador y marcado como leidas`() = runTest {
        server.enqueue(json("""{"items":[{"id":150,"type":"APPOINTMENT_BOOKED","message":"Su cita con Dr(a). Laura Medina quedó reservada.","appointmentId":76,"readAt":null,"createdAt":"2026-10-20T20:36:50.769Z"}],"unreadCount":3,"page":1,"pageSize":50,"total":13,"totalPages":1}"""))
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(json("""{"updated":3}"""))
        val page = notifications.list().getOrThrow()
        assertEquals(3, page.unreadCount)
        assertFalse(page.items.single().isRead)
        assertTrue(notifications.markRead(150).isSuccess)
        assertTrue(notifications.markAllRead().isSuccess)
        server.takeRequest()
        assertEquals("/api/v1/notifications/150/read", server.takeRequest().path)
        assertEquals("/api/v1/notifications/read-all", server.takeRequest().path)
    }

    @Test
    fun `un 401 en una peticion protegida borra la sesion y avisa`() = runTest {
        jar.saveFromResponse(server.url("/"), listOf(okhttp3.Cookie.parse(server.url("/"), "medicitas_session=x; Path=/; Max-Age=60")!!))
        server.enqueue(json("""{"error":{"code":"UNAUTHENTICATED","message":"Su sesión venció. Inicie sesión de nuevo."}}""", 401))
        val e = appointments.list(AppointmentScope.UPCOMING).exceptionOrNull() as ApiException
        assertTrue(e.isUnauthenticated)
        assertFalse(jar.hasSession())
    }

    @Test
    fun `sin servidor responde con mensaje de red`() = runTest {
        server.shutdown()
        val e = catalog.specialties().exceptionOrNull() as ApiException
        assertEquals(ApiException.NETWORK_ERROR, e.code)
    }
}

package com.medicitas.movil.ui

import com.medicitas.movil.data.session.ServerConfig
import com.medicitas.movil.fakes.FakeAuthRepository
import com.medicitas.movil.fakes.MainDispatcherRule
import com.medicitas.movil.fakes.MemoryUrlStore
import com.medicitas.movil.fakes.apiError
import com.medicitas.movil.fakes.patient
import com.medicitas.movil.ui.login.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val store = MemoryUrlStore()
    private fun vm(auth: FakeAuthRepository = FakeAuthRepository()) =
        LoginViewModel(auth, ServerConfig("http://10.0.2.2:3000/", store))

    @Test
    fun `login correcto deja el usuario y limpia la contrasena`() {
        val vm = vm()
        vm.onEmailChange("paciente@correo.com")
        vm.onPasswordChange("Medicitas2026")
        vm.submit()
        val s = vm.state.value
        assertEquals(patient, s.loggedInUser)
        assertEquals("", s.password)
        assertFalse(s.loading)
        assertNull(s.error)
    }

    @Test
    fun `campos vacios no llaman a la API`() {
        val auth = FakeAuthRepository()
        val vm = vm(auth)
        vm.submit()
        assertEquals("Ingrese su correo y su contraseña.", vm.state.value.error)
        assertEquals(0, auth.calls)
    }

    @Test
    fun `correo mal escrito se avisa en el cliente`() {
        val vm = vm()
        vm.onEmailChange("paciente.correo.com")
        vm.onPasswordChange("x")
        vm.submit()
        assertEquals("Ingrese un correo electrónico válido.", vm.state.value.error)
    }

    @Test
    fun `muestra el mensaje en espanol que manda el servidor`() {
        val vm = vm(FakeAuthRepository(apiError("INVALID_CREDENTIALS", "Correo o contraseña incorrectos.")))
        vm.onEmailChange("paciente@correo.com")
        vm.onPasswordChange("mala")
        vm.submit()
        assertEquals("Correo o contraseña incorrectos.", vm.state.value.error)
        assertNull(vm.state.value.loggedInUser)
    }

    @Test
    fun `la URL del servidor se valida y se guarda`() {
        val vm = vm()
        vm.onEmailChange("paciente@correo.com")
        vm.onPasswordChange("Medicitas2026")
        vm.onServerUrlChange("no es una url")
        vm.submit()
        assertEquals("La dirección del servidor no es válida (ejemplo: http://10.0.2.2:3000/).", vm.state.value.error)

        vm.onServerUrlChange("http://192.168.1.50:3000")
        vm.submit()
        assertEquals("http://192.168.1.50:3000/", store.value)
        assertEquals(patient, vm.state.value.loggedInUser)
    }
}

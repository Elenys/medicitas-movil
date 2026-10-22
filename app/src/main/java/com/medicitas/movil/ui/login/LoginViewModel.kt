package com.medicitas.movil.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medicitas.movil.data.session.ServerConfig
import com.medicitas.movil.domain.model.User
import com.medicitas.movil.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val serverUrl: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedInUser: User? = null,
)

class LoginViewModel(
    private val auth: AuthRepository,
    private val serverConfig: ServerConfig,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState(serverUrl = serverConfig.display))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onServerUrlChange(value: String) = _state.update { it.copy(serverUrl = value, error = null) }

    fun submit() {
        val s = _state.value
        // Validación mínima en el cliente; la definitiva la hace el servidor (Zod).
        val localError = when {
            s.email.isBlank() || s.password.isBlank() -> "Ingrese su correo y su contraseña."
            !EMAIL.matches(s.email.trim()) -> "Ingrese un correo electrónico válido."
            !serverConfig.update(s.serverUrl) -> "La dirección del servidor no es válida (ejemplo: http://10.0.2.2:3000/)."
            else -> null
        }
        if (localError != null) {
            _state.update { it.copy(error = localError) }
            return
        }
        _state.update { it.copy(loading = true, error = null, serverUrl = serverConfig.display) }
        viewModelScope.launch {
            auth.login(s.email, s.password)
                .onSuccess { user -> _state.update { it.copy(loading = false, loggedInUser = user, password = "") } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    companion object {
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

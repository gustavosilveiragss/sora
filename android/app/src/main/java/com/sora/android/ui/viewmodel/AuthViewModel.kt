package com.sora.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.domain.model.LoginRequest
import com.sora.android.domain.model.RegisterRequest
import com.sora.android.domain.repository.AuthRepository
import com.sora.android.core.error.ErrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val isRegisterSuccess: Boolean = false
)

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isValid: Boolean = false
)

data class RegisterFormState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val bio: String = "",
    val usernameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val isValid: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val errorManager: ErrorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _loginForm = MutableStateFlow(LoginFormState())
    val loginForm: StateFlow<LoginFormState> = _loginForm.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            authRepository.isLoggedIn().collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                _isInitialized.value = true
            }
        }
    }

    fun updateLoginEmail(email: String) {
        val emailError = validateEmail(email)
        _loginForm.value = _loginForm.value.copy(
            email = email,
            emailError = emailError,
            isValid = emailError == null && _loginForm.value.passwordError == null && _loginForm.value.password.isNotEmpty()
        )
    }

    fun updateLoginPassword(password: String) {
        val passwordError = validatePassword(password)
        _loginForm.value = _loginForm.value.copy(
            password = password,
            passwordError = passwordError,
            isValid = passwordError == null && _loginForm.value.emailError == null && _loginForm.value.email.isNotEmpty()
        )
    }

    fun updateRegisterUsername(username: String) {
        val usernameError = validateUsername(username)
        _registerForm.value = _registerForm.value.copy(
            username = username,
            usernameError = usernameError
        )
        validateRegisterForm()
    }

    fun updateRegisterEmail(email: String) {
        val emailError = validateEmail(email)
        _registerForm.value = _registerForm.value.copy(
            email = email,
            emailError = emailError
        )
        validateRegisterForm()
    }

    fun updateRegisterPassword(password: String) {
        val passwordError = validatePassword(password)
        val confirmPasswordError = if (_registerForm.value.confirmPassword.isNotEmpty() &&
            _registerForm.value.confirmPassword != password) {
            "As senhas não coincidem"
        } else null

        _registerForm.value = _registerForm.value.copy(
            password = password,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )
        validateRegisterForm()
    }

    fun updateRegisterConfirmPassword(confirmPassword: String) {
        val confirmPasswordError = if (confirmPassword != _registerForm.value.password) {
            "As senhas não coincidem"
        } else null

        _registerForm.value = _registerForm.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = confirmPasswordError
        )
        validateRegisterForm()
    }

    fun updateRegisterFirstName(firstName: String) {
        val firstNameError = validateName(firstName, "Nome")
        _registerForm.value = _registerForm.value.copy(
            firstName = firstName,
            firstNameError = firstNameError
        )
        validateRegisterForm()
    }

    fun updateRegisterLastName(lastName: String) {
        val lastNameError = validateName(lastName, "Sobrenome")
        _registerForm.value = _registerForm.value.copy(
            lastName = lastName,
            lastNameError = lastNameError
        )
        validateRegisterForm()
    }

    fun updateRegisterBio(bio: String) {
        _registerForm.value = _registerForm.value.copy(bio = bio)
    }

    private fun validateRegisterForm() {
        val form = _registerForm.value
        val isValid = form.usernameError == null && form.username.isNotEmpty() &&
                form.emailError == null && form.email.isNotEmpty() &&
                form.passwordError == null && form.password.isNotEmpty() &&
                form.confirmPasswordError == null && form.confirmPassword.isNotEmpty() &&
                form.firstNameError == null && form.firstName.isNotEmpty() &&
                form.lastNameError == null && form.lastName.isNotEmpty()

        _registerForm.value = form.copy(isValid = isValid)
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isEmpty() -> "Email é obrigatório"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email inválido"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "Senha é obrigatória"
            password.length < 6 -> "Senha deve ter pelo menos 6 caracteres"
            !password.any { it.isUpperCase() } -> "Senha deve conter pelo menos uma letra maiúscula"
            !password.any { it.isLowerCase() } -> "Senha deve conter pelo menos uma letra minúscula"
            !password.any { it.isDigit() } -> "Senha deve conter pelo menos um número"
            !password.any { !it.isLetterOrDigit() } -> "Senha deve conter pelo menos um caractere especial"
            else -> null
        }
    }

    private fun validateUsername(username: String): String? {
        return when {
            username.isEmpty() -> "Username é obrigatório"
            username.length < 3 -> "Username deve ter pelo menos 3 caracteres"
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Username pode conter apenas letras, números e underscore"
            else -> null
        }
    }

    private fun validateName(name: String, fieldName: String): String? {
        return when {
            name.isEmpty() -> "$fieldName é obrigatório"
            name.length < 2 -> "$fieldName deve ter pelo menos 2 caracteres"
            else -> null
        }
    }

    fun login() {
        if (!_loginForm.value.isValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val request = LoginRequest(
                email = _loginForm.value.email,
                password = _loginForm.value.password
            )

            authRepository.login(request).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    viewModelScope.launch {
                        errorManager.showErrorSnackbar(error)
                    }
                }
            )
        }
    }

    fun register() {
        if (!_registerForm.value.isValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val request = RegisterRequest(
                username = _registerForm.value.username,
                email = _registerForm.value.email,
                password = _registerForm.value.password,
                firstName = _registerForm.value.firstName,
                lastName = _registerForm.value.lastName,
                bio = _registerForm.value.bio.ifEmpty { null }
            )

            authRepository.register(request).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegisterSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao criar conta"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetSuccessStates() {
        _uiState.value = _uiState.value.copy(
            isLoginSuccess = false,
            isRegisterSuccess = false
        )
    }
}
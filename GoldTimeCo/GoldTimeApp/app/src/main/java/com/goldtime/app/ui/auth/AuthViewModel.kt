package com.goldtime.app.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldtime.app.data.AuthRepository
import com.goldtime.app.data.toFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }

    private fun fail(message: String) = _state.update { it.copy(error = message, info = null) }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        val e = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) return fail("Enter a valid email address.")
        if (password.isEmpty()) return fail("Enter your password.")

        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            try {
                AuthRepository.signIn(e, password)
                _state.value = AuthUiState()
                onSuccess()
            } catch (ex: Exception) {
                _state.value = AuthUiState(error = ex.toFriendlyMessage())
            }
        }
    }

    fun register(
        firstName: String,
        surname: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean,
        onSuccess: () -> Unit
    ) {
        val e = email.trim()
        val p = phone.filter { it.isDigit() || it == '+' }
        when {
            firstName.isBlank() -> return fail("Enter your first name.")
            surname.isBlank() -> return fail("Enter your surname.")
            !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> return fail("Enter a valid email address.")
            p.count { it.isDigit() } < 9 -> return fail("Enter a valid phone number.")
            password.length < 8 -> return fail("Password must be at least 8 characters.")
            password != confirmPassword -> return fail("Passwords do not match.")
            !acceptedTerms -> return fail("Please accept the Terms & Conditions and Privacy Notice.")
        }

        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            try {
                AuthRepository.register(firstName.trim(), surname.trim(), e, p, password)
                _state.value = AuthUiState()
                onSuccess()
            } catch (ex: Exception) {
                _state.value = AuthUiState(error = ex.toFriendlyMessage())
            }
        }
    }

    fun resetPassword(email: String) {
        val e = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            return fail("Enter your email above, then tap Forgot password.")
        }
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            try {
                AuthRepository.sendPasswordReset(e)
                _state.value = AuthUiState(info = "Password reset email sent. Check your inbox.")
            } catch (ex: Exception) {
                _state.value = AuthUiState(error = ex.toFriendlyMessage())
            }
        }
    }
}

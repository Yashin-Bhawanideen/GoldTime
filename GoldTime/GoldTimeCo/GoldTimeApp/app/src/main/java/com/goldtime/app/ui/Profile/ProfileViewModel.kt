package com.goldtime.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldtime.app.data.ApiClient
import com.goldtime.app.data.AuthRepository
import com.goldtime.app.data.SavedOrder
import com.goldtime.app.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val profile: UserProfile? = null,
    val orders: List<SavedOrder> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _state =
        MutableStateFlow(ProfileState())

    val state: StateFlow<ProfileState> =
        _state.asStateFlow()

    init {
        load()
    }

    fun load() {

        viewModelScope.launch {

            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null
                )

            try {

                val token =
                    AuthRepository.idToken()


                val profile =
                    ApiClient.getProfile(token)

                _state.value =
                    _state.value.copy(
                        profile = profile
                    )

                try {

                    val orders =
                        ApiClient.getOrderHistory(token)

                    _state.value =
                        _state.value.copy(
                            orders = orders,
                            loading = false,
                            error = null
                        )

                } catch (orderError: Exception) {


                    _state.value =
                        _state.value.copy(
                            orders = emptyList(),
                            loading = false,
                            error =
                                orderError.message
                                    ?: "Could not load order history."
                        )
                }

            } catch (profileError: Exception) {

                _state.value =
                    _state.value.copy(
                        loading = false,
                        error =
                            profileError.message
                                ?: "Could not load your profile."
                    )
            }
        }
    }
}
package com.goldtime.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldtime.app.data.ApiClient
import com.goldtime.app.data.AuthRepository
import com.goldtime.app.data.HomeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val data: HomeData = HomeData.Default,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val data = ApiClient.getHome(AuthRepository.idToken())
                _state.update { it.copy(loading = false, data = data) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = "Couldn't reach the server. Tap to retry.")
                }
            }
        }
    }
}

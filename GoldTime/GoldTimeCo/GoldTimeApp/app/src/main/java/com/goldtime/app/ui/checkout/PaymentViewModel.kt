package com.goldtime.app.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldtime.app.data.ApiClient
import com.goldtime.app.data.AuthRepository
import com.goldtime.app.data.SavedOrder
import kotlinx.coroutines.launch

class PaymentViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val session = PaymentSession(savedState["paymentOrderId"],
        createForm = { ApiClient.paymentForm(AuthRepository.idToken(), it) },
        readOrder = { ApiClient.getOrder(AuthRepository.idToken(), it) },
        saveOrderId = { savedState["paymentOrderId"] = it })
    val state = session.state
    val pageOpened: Boolean get() = savedState["paymentPageOpened"] ?: false

    init {
        //restores the order check, never automatically resubmits a payment after process recreation
        if (state.value.orderId != null) check()
    }

    fun start(order: SavedOrder) {
        savedState["paymentPageOpened"] = false
        viewModelScope.launch { session.start(order) }
    }
    fun opened() { savedState["paymentPageOpened"] = true }
    fun check() { viewModelScope.launch { session.check() } }
    fun leave() = session.leave()
}

/* REFERENCE LIST
Android Developers. n.d. Saved State module for ViewModel. [Online]. Available at:
https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
[Accessed 1 October 2026].
*/

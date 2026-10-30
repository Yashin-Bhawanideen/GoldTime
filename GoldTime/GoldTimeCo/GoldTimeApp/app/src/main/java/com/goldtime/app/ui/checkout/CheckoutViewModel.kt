package com.goldtime.app.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldtime.app.data.ApiClient
import com.goldtime.app.data.AuthRepository
import com.goldtime.app.data.OrderApiException
import com.goldtime.app.data.OrderDraft
import kotlinx.coroutines.launch

class CheckoutViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val submission = OrderSubmission(
        create = { draft, requestId ->
            if (!AuthRepository.isLoggedIn) throw OrderApiException(401, "Please sign in before continuing.")
            ApiClient.createOrder(AuthRepository.idToken(), draft, requestId)
        },
        previousRequest = { savedState.get<ArrayList<String>>("checkoutRequest") },
        //stores only the request hash and ID for restored screens (Android Developers, n.d.)
        saveRequest = { savedState["checkoutRequest"] = ArrayList(it) }
    )
    val state = submission.state

    fun submit(order: OrderReview, delivery: DeliveryDetails, method: PaymentMethod) {
        viewModelScope.launch { submission.submit(OrderDraft(order, delivery, method)) }
    }

    fun edit() = submission.edit()
}

/* REFERENCE LIST
Android Developers. n.d. Saved State module for ViewModel. [Online]. Available at:
https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
[Accessed 1 October 2026].
*/

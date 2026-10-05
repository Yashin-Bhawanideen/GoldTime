package com.goldtime.app.ui.checkout

import com.goldtime.app.data.OrderApiException
import com.goldtime.app.data.OrderDraft
import com.goldtime.app.data.SavedOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

data class OrderSubmissionState(
    val loading: Boolean = false,
    val error: String? = null,
    val savedOrder: SavedOrder? = null
)

class OrderSubmission(
    private val create: suspend (OrderDraft, String) -> SavedOrder,
    private val previousRequest: () -> List<String>?,
    private val saveRequest: (List<String>) -> Unit
) {
    private val mutableState = MutableStateFlow(OrderSubmissionState())
    val state = mutableState.asStateFlow()
    private val submissionLock = Mutex()

    fun edit() {
        if (!mutableState.value.loading) mutableState.value = OrderSubmissionState()
    }

    suspend fun submit(draft: OrderDraft) {
        //ignores another tap while the first request is still running
        if (!submissionLock.tryLock()) return
        try {
            val error = paymentSelectionError(draft.order, draft.delivery, draft.method)
            val invalidItems = draft.order.items.size !in 1..50 || draft.order.items.any {
                !Regex("[a-zA-Z0-9_-]{1,100}").matches(it.productId) || it.quantity !in 1..99
            } || draft.order.items.map { it.productId }.distinct().size != draft.order.items.size
            if (error != null || invalidItems) {
                mutableState.value = OrderSubmissionState(error = error ?: "Check the products and quantities in your cart.")
                return
            }
            mutableState.value = OrderSubmissionState(loading = true)
            val fingerprint = draft.fingerprint()
            val previous = previousRequest()
            val requestId = if (previous?.size == 2 && previous[0] == fingerprint) previous[1]
                else UUID.randomUUID().toString()
            //keeps the same ID after a lost response; changed checkout details get a new ID
            saveRequest(listOf(fingerprint, requestId))
            val saved = create(draft, requestId)
            require(saved.method == draft.method && saved.delivery.trimmed() == draft.delivery.trimmed())
            require(saved.order.items.associate { it.productId to it.quantity } ==
                draft.order.items.associate { it.productId to it.quantity })
            mutableState.value = OrderSubmissionState(savedOrder = saved)
        } catch (ex: CancellationException) {
            mutableState.value = OrderSubmissionState()
            throw ex
        } catch (ex: Exception) {
            mutableState.value = OrderSubmissionState(error = when (ex) {
                is OrderApiException -> ex.message
                else -> "Could not confirm your order. Check your connection and retry. Your cart has not been cleared."
            })
        } finally {
            submissionLock.unlock()
        }
    }
}

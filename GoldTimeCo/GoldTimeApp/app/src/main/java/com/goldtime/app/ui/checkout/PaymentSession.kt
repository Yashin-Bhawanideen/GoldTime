package com.goldtime.app.ui.checkout

import com.goldtime.app.data.OrderApiException
import com.goldtime.app.data.PayFastForm
import com.goldtime.app.data.SavedOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

data class PaymentState(
    val orderId: String? = null,
    val loading: Boolean = false,
    val form: PayFastForm? = null,
    val order: SavedOrder? = null,
    val error: String? = null
)

class PaymentSession(
    restoredOrderId: String?,
    private val createForm: suspend (SavedOrder) -> PayFastForm,
    private val readOrder: suspend (String) -> SavedOrder,
    private val saveOrderId: (String?) -> Unit
) {
    private val mutableState = MutableStateFlow(PaymentState(orderId = restoredOrderId))
    val state = mutableState.asStateFlow()
    private val lock = Mutex()

    suspend fun start(order: SavedOrder) {
        if (!lock.tryLock()) return
        try {
            mutableState.value = PaymentState(orderId = order.id, loading = true, order = order)
            require(order.status == "pending_payment")
            saveOrderId(order.id)
            mutableState.value = PaymentState(orderId = order.id, order = order, form = createForm(order))
        } catch (ex: CancellationException) { throw ex }
        catch (ex: Exception) { fail(ex) }
        finally { mutableState.value = mutableState.value.copy(loading = false); lock.unlock() }
    }

    suspend fun check() {
        val id = mutableState.value.orderId ?: return
        if (!lock.tryLock()) return
        try {
            //browser completion only starts a check; the API decides whether payment was verified
            mutableState.value = PaymentState(orderId = id, loading = true)
            val order = readOrder(id)
            require(order.id == id)
            require(order.status != "sandbox_paid" || order.sandboxPaymentConfirmed)
            mutableState.value = PaymentState(orderId = id, order = order)
        } catch (ex: CancellationException) { throw ex }
        catch (ex: Exception) { fail(ex) }
        finally { mutableState.value = mutableState.value.copy(loading = false); lock.unlock() }
    }

    fun leave() {
        if (mutableState.value.loading) return
        saveOrderId(null)
        mutableState.value = PaymentState()
    }

    private fun fail(ex: Exception) {
        mutableState.value = mutableState.value.copy(form = null, order = null, error =
            if (ex is OrderApiException) ex.message else "Could not verify the payment. Check again before starting another payment.")
    }
}

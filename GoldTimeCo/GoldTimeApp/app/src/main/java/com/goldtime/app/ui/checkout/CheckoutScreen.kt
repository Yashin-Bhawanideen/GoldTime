package com.goldtime.app.ui.checkout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldtime.app.data.SavedOrder
import com.goldtime.app.ui.theme.GoldColors

@Composable
fun CheckoutScreen(
    order: OrderReview,
    onBackToCart: () -> Unit,
    onOrderConfirmed: ((SavedOrder) -> Unit)? = null,
    vm: CheckoutViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf("delivery") }
    var details by rememberSaveable(stateSaver = checkoutAddressSaver) { mutableStateOf(DeliveryDetails()) }
    var methodName by rememberSaveable { mutableStateOf<String?>(null) }
    val method = methodName?.let { PaymentMethod.valueOf(it) }
    val back = {
        if (!state.loading) {
            if (state.savedOrder != null) vm.edit()
            else when (step) {
                "payment" -> { vm.edit(); step = "review" }
                "review" -> step = "delivery"
                else -> onBackToCart()
            }
        }
    }
    BackHandler(onBack = back)

    Column(Modifier.fillMaxSize().background(GoldColors.Background)) {
        if (state.error != null) {
            Text(state.error.orEmpty(), color = GoldColors.Error, modifier = Modifier
                .statusBarsPadding().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite })
        }
        Box(Modifier.weight(1f)) {
            val saved = state.savedOrder
            when {
                state.loading -> Column(Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = GoldColors.Gold)
                    Spacer(Modifier.height(16.dp))
                    Text("Checking prices and saving your order…", color = GoldColors.TextPrimary)
                }
                saved != null -> ReviewOrderScreen(saved.order, saved.delivery,
                    onEditDelivery = { vm.edit(); step = "delivery" },
                    onProceedToPayment = { onOrderConfirmed?.invoke(saved) },
                    heading = "Confirm Total", stepLabel = if (saved.status == "pending_payment")
                        "ORDER SAVED — NOT PAID" else "ORDER STATUS: ${saved.status}",
                    continueLabel = if (onOrderConfirmed == null) "Payment not available" else "Continue to PayFast",
                    allowContinue = onOrderConfirmed != null && saved.status == "pending_payment")
                step == "payment" -> PaymentMethodScreen(order, details, method,
                    onMethodSelected = { methodName = it.name; vm.edit() }, onBack = back,
                    onContinue = { vm.submit(order, details, it) })
                step == "review" -> ReviewOrderScreen(order, details,
                    onEditDelivery = { step = "delivery" }, onProceedToPayment = { step = "payment" })
                else -> DeliveryDetailsScreen(onBack = onBackToCart, initialDetails = details,
                    onContinue = { details = it; step = "review" })
            }
        }
    }
}

//restores delivery fields when returning between checkout steps (Android Developers, n.d.)
private val checkoutAddressSaver = listSaver<DeliveryDetails, String>(
    save = { listOf(it.fullName, it.phone, it.streetAddress, it.city, it.province, it.postalCode) },
    restore = { DeliveryDetails(it[0], it[1], it[2], it[3], it[4], it[5]) }
)

/* REFERENCE LIST
Android Developers. n.d. Save UI state in Compose. [Online]. Available at:
https://developer.android.com/develop/ui/compose/state-saving [Accessed 1 October 2026].
*/

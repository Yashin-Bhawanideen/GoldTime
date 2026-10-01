package com.goldtime.app.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldtime.app.data.SavedOrder
import com.goldtime.app.ui.components.GoldButton
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.GoldTimeTheme
import java.math.BigDecimal

@Composable
fun PaymentStatusScreen(state: PaymentState, onCheckAgain: () -> Unit, onBackToCart: () -> Unit) {
    val confirmed = state.order?.sandboxPaymentConfirmed == true
    Box(Modifier.fillMaxSize().background(GoldColors.Background).systemBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("PAYFAST SANDBOX", color = GoldColors.Gold)
            Text(when {
                state.loading -> "Checking payment"
                confirmed -> "Sandbox payment confirmed"
                state.error != null -> "Unable to verify payment"
                else -> "Payment not confirmed"
            }, color = GoldColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (state.loading) CircularProgressIndicator(color = GoldColors.Gold)
            Text(when {
                confirmed -> "Your test payment was verified by the server. No real money was charged and no delivery will be arranged."
                state.loading -> "Please wait while the order status is checked."
                else -> "A payment notification may still be on its way. Check again before starting another payment. Your cart has been kept."
            }, color = GoldColors.TextPrimary)
            state.error?.let { Text(it, color = GoldColors.Error) }
            state.orderId?.let { Text("Order: $it", color = GoldColors.TextMuted) }
            state.order?.let { Text("Total: ${it.order.total.asRand()}", color = GoldColors.Gold, fontSize = 22.sp) }
            if (confirmed) Text("Payment reference: ${state.order?.paymentId}", color = GoldColors.TextPrimary)
            if (!confirmed) GoldButton("Check again", onCheckAgain, enabled = !state.loading)
            GoldButton("Back to cart", onBackToCart, enabled = !state.loading)
        }
    }
}

@Preview(name = "Sandbox confirmation - sample", widthDp = 380, heightDp = 820)
@Composable
fun SandboxConfirmationPreview() {
    GoldTimeTheme {
        Column(Modifier.fillMaxSize().background(GoldColors.Background)) {
            Text("PREVIEW — SAMPLE PAYMENT", color = GoldColors.Gold, modifier = Modifier.padding(12.dp))
            PaymentStatusScreen(PaymentState(orderId = "sample-order", order = SavedOrder("sample-order",
                OrderReview(listOf(CheckoutItem("sample", "Sample item", 1, BigDecimal("100.00"))), BigDecimal("150.00")),
                DeliveryDetails(), PaymentMethod.CARD, "sandbox_paid", "sandbox", "12345", "2026-10-01T00:00:00Z")), {}, {})
        }
    }
}

@Preview(name = "Payment pending - sample", widthDp = 380, heightDp = 820)
@Composable
fun PendingPaymentPreview() {
    GoldTimeTheme { PaymentStatusScreen(PaymentState(orderId = "sample-order"), {}, {}) }
}

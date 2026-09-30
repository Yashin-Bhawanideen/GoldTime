package com.goldtime.app.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldtime.app.ui.components.GoldButton
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.GoldTimeTheme
import com.goldtime.app.ui.theme.HeadingFont
import java.math.BigDecimal

@Composable
fun ReviewOrderScreen(
    order: OrderReview,
    details: DeliveryDetails,
    onEditDelivery: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    val validDelivery = validateDelivery(details).isEmpty()
    Box(Modifier.fillMaxSize().background(GoldColors.Background).systemBarsPadding(),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditDelivery) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to delivery details",
                        tint = GoldColors.Gold)
                }
                Column {
                    Text("STEP 2 OF 3", color = GoldColors.Gold, fontSize = 11.sp)
                    Text("Review Order", fontFamily = HeadingFont, fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold, color = GoldColors.TextPrimary)
                }
            }
            if (order.items.isEmpty()) {
                Text("Your cart is empty. Add an item before continuing.", color = GoldColors.Error)
            }
            order.items.forEach { item ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = GoldColors.Surface, contentColor = GoldColors.TextPrimary)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                        Text("Quantity: ${item.quantity}")
                        Text("Unit price: ${item.unitPrice.asRand()}")
                        Text("Item total: ${item.lineTotal.asRand()}", color = GoldColors.Gold)
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = GoldColors.Surface, contentColor = GoldColors.TextPrimary)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Deliver to", fontWeight = FontWeight.SemiBold)
                    //does not leave a large blank address card before details have been entered
                    val addressLines = listOf(details.fullName, details.phone, details.streetAddress,
                        details.city, details.province, details.postalCode).filter { it.isNotBlank() }
                    if (addressLines.isEmpty()) {
                        Text("No delivery details entered yet.", color = GoldColors.TextMuted)
                    } else {
                        Text(addressLines.joinToString("\n"))
                    }
                    TextButton(onClick = onEditDelivery) { Text("Edit delivery details") }
                }
            }
            if (!validDelivery) {
                Text("Please complete your delivery details before paying.", color = GoldColors.Error)
            }
            HorizontalDivider()
            ReviewAmount("Subtotal", order.subtotal)
            ReviewAmount("Delivery", order.delivery)
            ReviewAmount("Total", order.total)
            //the API must confirm product prices before creating a payment
            GoldButton("Proceed to payment", onProceedToPayment,
                enabled = order.canContinue && validDelivery)
        }
    }
}

@Composable
private fun ReviewAmount(label: String, amount: BigDecimal) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), color = GoldColors.TextPrimary)
        Text(amount.asRand(), color = GoldColors.Gold, fontWeight = FontWeight.SemiBold)
    }
}

//saves the small delivery values when moving between preview steps (Android Developers, n.d.)
private val deliverySaver = listSaver<DeliveryDetails, String>(
    save = { listOf(it.fullName, it.phone, it.streetAddress, it.city, it.province, it.postalCode) },
    restore = { DeliveryDetails(it[0], it[1], it[2], it[3], it[4], it[5]) }
)

@Preview(name = "Checkout flow - sample data", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
fun CheckoutFlowPreview() {
    //sample items belong to this preview only; no order or payment is submitted
    val order = OrderReview(listOf(
        CheckoutItem("preview-gold", "Gold Krugerrand", 2, BigDecimal("58995.00")),
        CheckoutItem("preview-silver", "Silver Bar", 1, BigDecimal("21450.00"))
    ), BigDecimal("150.00"))
    var details by rememberSaveable(stateSaver = deliverySaver) { mutableStateOf(DeliveryDetails()) }
    var step by rememberSaveable { mutableStateOf("delivery") }
    //keeps the selection when returning to review or editing the delivery address
    var methodName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMethod = methodName?.let { PaymentMethod.valueOf(it) }
    var paymentMessage by remember { mutableStateOf(false) }
    GoldTimeTheme {
        Column(Modifier.fillMaxSize().background(GoldColors.Background)) {
            Text("PREVIEW — SAMPLE CART", color = GoldColors.Gold, modifier = Modifier.padding(8.dp))
            Box(Modifier.weight(1f)) {
                when (step) {
                    "review" -> ReviewOrderScreen(order, details, onEditDelivery = { step = "delivery" },
                        onProceedToPayment = { step = "payment" })
                    "payment" -> PaymentMethodScreen(order, details, selectedMethod,
                        onMethodSelected = { methodName = it.name },
                        onBack = { step = "review" },
                        onContinue = { paymentMessage = true }, previewOnly = true)
                    else -> DeliveryDetailsScreen(onBack = {}, initialDetails = details,
                        onContinue = { details = it; step = "review" })
                }
            }
        }
        if (paymentMessage) {
            AlertDialog(onDismissRequest = { paymentMessage = false },
                title = { Text("Preview: method selected") },
                text = { Text("${selectedMethod?.label} selected. No order or payment has been submitted. PayFast is not connected yet.") },
                confirmButton = { TextButton(onClick = { paymentMessage = false }) { Text("OK") } })
        }
    }
}

@Preview(name = "Empty cart", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
fun EmptyOrderPreview() {
    var editing by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable(stateSaver = deliverySaver) { mutableStateOf(DeliveryDetails()) }
    GoldTimeTheme {
        if (editing) {
            DeliveryDetailsScreen(initialDetails = details, onBack = { editing = false },
                onContinue = { details = it; editing = false })
        } else {
            ReviewOrderScreen(OrderReview(emptyList(), BigDecimal("150.00")),
                details, onEditDelivery = { editing = true }, onProceedToPayment = {})
        }
    }
}

/*
REFERENCE LIST
Android Developers. n.d. Save UI state in Compose. [Online].
Available at: https://developer.android.com/develop/ui/compose/state-saving
[Accessed 30 September 2026].

Android Developers. n.d. Preview your UI with composable previews. [Online].
Available at: https://developer.android.com/develop/ui/compose/tooling/previews
[Accessed 30 September 2026].
*/

package com.goldtime.app.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldtime.app.ui.components.GoldButton
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont

@Composable
fun PaymentMethodScreen(
    order: OrderReview,
    details: DeliveryDetails,
    selectedMethod: PaymentMethod?,
    onMethodSelected: (PaymentMethod) -> Unit,
    onBack: () -> Unit,
    onContinue: (PaymentMethod) -> Unit,
    previewOnly: Boolean = false
) {
    var submitted by rememberSaveable { mutableStateOf(false) }
    val error = if (submitted) paymentSelectionError(order, details, selectedMethod) else null
    Surface(color = GoldColors.Background, contentColor = GoldColors.TextPrimary,
        modifier = Modifier.fillMaxSize()) {
        Box(Modifier.systemBarsPadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to review order",
                            tint = GoldColors.Gold)
                    }
                    Column {
                        Text("STEP 3 OF 3", color = GoldColors.Gold, fontSize = 11.sp)
                        Text("Payment Method", fontFamily = HeadingFont, fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = GoldColors.Surface, contentColor = GoldColors.TextPrimary)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Order total", color = GoldColors.Gold)
                        Text(order.total.asRand(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        if (previewOnly) {
                            Text("Preview only. No payment will be processed.")
                        }
                    }
                }
                Text("Choose a payment method")
                //makes each labelled row one accessible radio option (Android Developers, 2026)
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(GoldColors.Surface)
                            .selectable(selected = selectedMethod == method, role = Role.RadioButton,
                                onClick = { onMethodSelected(method) })
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(method.label, modifier = Modifier.weight(1f))
                            RadioButton(selected = selectedMethod == method, onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = GoldColors.Gold,
                                    unselectedColor = GoldColors.TextMuted))
                        }
                    }
                }
                if (error != null) {
                    Text(error, color = GoldColors.Error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                GoldButton(if (previewOnly) "Check selection" else "Continue", onClick = {
                    submitted = true
                    //does not call the next step until the method, cart and address are valid
                    if (paymentSelectionError(order, details, selectedMethod) == null) {
                        selectedMethod?.let(onContinue)
                    }
                })
            }
        }
    }
}

/*
REFERENCE LIST
Android Developers. 2026. Radio button. [Online].
Available at: https://developer.android.com/develop/ui/compose/components/radio-button
[Accessed 1 October 2026].
*/

package com.goldtime.app.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldtime.app.ui.components.GoldButton
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.GoldTimeTheme
import com.goldtime.app.ui.theme.HeadingFont

@Composable
fun DeliveryDetailsScreen(
    onBack: () -> Unit,
    onContinue: (DeliveryDetails) -> Unit,
    initialDetails: DeliveryDetails = DeliveryDetails()
) {
    // Keep the small form values through rotation and saved-state restoration.
    var fullName by rememberSaveable { mutableStateOf(initialDetails.fullName) }
    var phone by rememberSaveable { mutableStateOf(initialDetails.phone) }
    var street by rememberSaveable { mutableStateOf(initialDetails.streetAddress) }
    var city by rememberSaveable { mutableStateOf(initialDetails.city) }
    var province by rememberSaveable { mutableStateOf(initialDetails.province) }
    var postalCode by rememberSaveable { mutableStateOf(initialDetails.postalCode) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val details = DeliveryDetails(fullName, phone, street, city, province, postalCode)
    val errors = if (submitted) validateDelivery(details) else emptyMap()
    val focus = LocalFocusManager.current
    val submit = {
        submitted = true
        if (validateDelivery(details).isEmpty()) {
            focus.clearFocus()
            onContinue(details.trimmed())
        }
    }

    Box(
        Modifier.fillMaxSize().background(GoldColors.Background)
            .systemBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = GoldColors.Gold)
                }
                Column {
                    Text("STEP 1 OF 3", color = GoldColors.Gold, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text("Delivery Details", color = GoldColors.TextPrimary,
                        fontFamily = HeadingFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
                }
            }
            DeliveryField("Full name", fullName, { fullName = it }, errors["fullName"])
            DeliveryField("Phone", phone, { phone = it }, errors["phone"], KeyboardType.Phone)
            DeliveryField("Street address", street, { street = it }, errors["streetAddress"])
            DeliveryField("City", city, { city = it }, errors["city"])
            DeliveryField("Province", province, { province = it }, errors["province"])
            DeliveryField("Postal code", postalCode, { postalCode = it }, errors["postalCode"],
                KeyboardType.Number, ImeAction.Done, KeyboardActions(onDone = { submit() }))
            if (errors.isNotEmpty()) {
                Text("Please correct the highlighted fields.", color = GoldColors.Error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            GoldButton(text = "Continue", onClick = { submit() })
        }
    }
}

@Composable
private fun DeliveryField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        isError = error != null,
        supportingText = if (error != null) ({ Text(error) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = GoldColors.Field,
            unfocusedContainerColor = GoldColors.Field,
            errorContainerColor = GoldColors.Field,
            focusedBorderColor = GoldColors.Gold,
            unfocusedBorderColor = GoldColors.FieldBorder
        )
    )
}

@Preview(name = "Delivery details", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
fun DeliveryDetailsPreview() {
    GoldTimeTheme {
        var accepted by remember { mutableStateOf(false) }
        DeliveryDetailsScreen(onBack = {}, onContinue = { accepted = true })
        if (accepted) {
            AlertDialog(
                onDismissRequest = { accepted = false },
                title = { Text("Preview: details valid") },
                text = { Text("Validation passed. Order review will be connected in the next feature.") },
                confirmButton = { TextButton(onClick = { accepted = false }) { Text("OK") } }
            )
        }
    }
}

package com.goldtime.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldtime.app.ui.components.GoldButton
import com.goldtime.app.ui.components.GoldTextField
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont

@Composable
fun RegisterScreen(
    vm: AuthViewModel,
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var firstName by rememberSaveable { mutableStateOf("") }
    var surname by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    LaunchedEffect(Unit) { vm.clearMessages() }

    val submit = {
        focus.clearFocus()
        vm.register(firstName, surname, email, phone, password, confirm, accepted, onRegistered)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldColors.Background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GoldColors.Field)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldColors.Gold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("GOLD TIME CO.", color = GoldColors.Gold, fontSize = 10.sp, letterSpacing = 2.sp)
                Text(
                    "Create Account",
                    color = GoldColors.TextPrimary,
                    fontFamily = HeadingFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        GoldTextField("First name", firstName, { firstName = it }, placeholder = "Mishal")
        Spacer(Modifier.height(14.dp))
        GoldTextField("Surname", surname, { surname = it }, placeholder = "Bhikha")
        Spacer(Modifier.height(14.dp))
        GoldTextField(
            "Email", email, { email = it },
            placeholder = "name@email.co.za", keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(14.dp))
        GoldTextField(
            "Phone", phone, { phone = it },
            placeholder = "+27 82 000 0000", keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(14.dp))
        GoldTextField("Password", password, { password = it }, placeholder = "••••••••", isPassword = true)
        Spacer(Modifier.height(14.dp))
        GoldTextField(
            "Confirm password", confirm, { confirm = it },
            placeholder = "••••••••", isPassword = true,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() })
        )

        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.clickable { accepted = !accepted }
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = GoldColors.Gold,
                    uncheckedColor = GoldColors.TextMuted,
                    checkmarkColor = Color.Black
                )
            )
            Text(
                "I accept the Terms & Conditions and Privacy Notice.",
                color = GoldColors.TextMuted,
                fontSize = 12.sp
            )
        }

        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = GoldColors.Error, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(12.dp))
        GoldButton(text = "Create Account", onClick = { submit() }, loading = state.loading)
        Spacer(Modifier.height(24.dp))
    }
}

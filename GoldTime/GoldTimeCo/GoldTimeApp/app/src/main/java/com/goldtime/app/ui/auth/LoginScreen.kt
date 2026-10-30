package com.goldtime.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldtime.app.ui.components.GoldButton
import com.goldtime.app.ui.components.GoldOutlinedButton
import com.goldtime.app.ui.components.GoldTextField
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont

@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    LaunchedEffect(Unit) { vm.clearMessages() }

    val submit = {
        focus.clearFocus()
        vm.signIn(email, password, onLoggedIn)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldColors.Background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(1.dp, GoldColors.Gold.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Storefront,
                contentDescription = null,
                tint = GoldColors.Gold,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("GOLD TIME CO.", color = GoldColors.Gold, fontSize = 11.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Welcome Back",
            color = GoldColors.TextPrimary,
            fontFamily = HeadingFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Secure access to your Gold Time Co. account",
            color = GoldColors.TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))
        GoldTextField(
            label = "Email address",
            value = email,
            onValueChange = { email = it },
            placeholder = "name@email.co.za",
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(16.dp))
        GoldTextField(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            placeholder = "••••••••",
            isPassword = true,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() })
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = GoldColors.Error, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        state.info?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = GoldColors.Gold, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))
        GoldButton(text = "Sign In", onClick = { submit() }, loading = state.loading)

        Spacer(Modifier.height(20.dp))
        Text(
            "Forgot password?",
            color = GoldColors.Gold,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { vm.resetPassword(email) }
        )

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = GoldColors.Divider)
            Text(
                "OR",
                color = GoldColors.TextMuted,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = GoldColors.Divider)
        }

        Spacer(Modifier.height(20.dp))
        GoldOutlinedButton(text = "Create Account", onClick = onCreateAccount)
    }
}

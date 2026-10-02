package com.goldtime.app.ui.quote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldtime.app.ui.components.GoldButton
import com.goldtime.app.ui.components.GoldTextField
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont

@Composable
fun RequestQuoteScreen(
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldColors.TextPrimary
                )
            }

            Text(
                text = "REQUEST A QUOTE",
                color = GoldColors.Gold,
                fontFamily = HeadingFont,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "GET A QUOTE",
            color = GoldColors.TextPrimary,
            fontFamily = HeadingFont,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Tell us what you're interested in and we'll get back to you.",
            color = GoldColors.TextMuted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))

        GoldTextField(
            label = "Full Name",
            value = name,
            onValueChange = { name = it },
            placeholder = "Your name"
        )

        Spacer(Modifier.height(16.dp))

        GoldTextField(
            label = "Email",
            value = email,
            onValueChange = { email = it },
            placeholder = "your@email.com"
        )

        Spacer(Modifier.height(16.dp))

        GoldTextField(
            label = "Phone",
            value = phone,
            onValueChange = { phone = it },
            placeholder = "Your phone number"
        )

        Spacer(Modifier.height(16.dp))

        GoldTextField(
            label = "Message",
            value = message,
            onValueChange = { message = it },
            placeholder = "What would you like a quote for?"
        )

        Spacer(Modifier.height(28.dp))

        GoldButton(
            text = "Request Quote",
            onClick = {
                // Submission will be implemented later
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}
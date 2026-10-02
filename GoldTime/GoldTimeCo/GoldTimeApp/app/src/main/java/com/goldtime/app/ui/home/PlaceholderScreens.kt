package com.goldtime.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont

@Composable
private fun ComingSoonTab(icon: ImageVector, title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = GoldColors.Gold, modifier = Modifier.size(42.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
        Text(
            title,
            color = GoldColors.TextPrimary,
            fontFamily = HeadingFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
        Text(
            message,
            color = GoldColors.TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ShopTab() = ComingSoonTab(Icons.Outlined.ShoppingBag, "Shop", "Browse the full catalogue here soon.")

@Composable
fun SellGoldTab() = ComingSoonTab(Icons.Outlined.Sell, "Sell Gold", "Submit items for a quote here soon.")

@Composable
fun OrdersTab() = ComingSoonTab(Icons.Outlined.Receipt, "Orders", "Track your orders here soon.")
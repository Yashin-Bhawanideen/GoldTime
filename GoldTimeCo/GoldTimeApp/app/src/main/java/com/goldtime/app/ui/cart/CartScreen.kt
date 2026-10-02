package com.goldtime.app.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldtime.app.data.CartItem
import com.goldtime.app.data.toZarFormat
import com.goldtime.app.ui.components.GoldButton
import com.goldtime.app.ui.components.RemoteImage
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont
import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun CartScreen(
    onBack:() -> Unit,
    onCheckout: () -> Unit,
    vm: CartViewModel = viewModel ()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(GoldColors.Background)
            .systemBarsPadding()

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
                text= "GOLD TIME CO.",
                color= GoldColors.Gold,
                fontSize= 11.sp,
                letterSpacing= 2.sp,
                fontWeight= FontWeight.Bold
            )
        }

        Text(
            text = "Your Cart",
            color= GoldColors.TextPrimary,
            fontFamily = HeadingFont,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp,vertical = 8.dp)
        )

        Text("Sandbox only: displayed prices are samples. Confirm the final total at checkout.",
            color = GoldColors.Gold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your Cart Is Empty.",
                    color = GoldColors.TextMuted,
                    fontSize = 14.sp
                )

            }


        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items, key = {it.id}) {item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { vm.increment(item.id) },
                        onDecrement = { vm.decrement(item.id) },
                        onRemove = { vm.remove(item.id)}
                    )
                }
            }

            // if cart !empty then display the actual cart info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                HorizontalDivider(color = GoldColors.Divider, thickness = 1.dp)
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", color = GoldColors.TextMuted, fontSize = 14.sp)
                    Text(state.subtotalFormated, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Delivery", color = GoldColors.TextMuted, fontSize =14.sp)
                    Text(state.deliveryFeeFormatted, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(state.totalFormatted, color = GoldColors.Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))

                GoldButton(
                    text= "Proceed to Checkout",
                    onClick = onCheckout
                )

            }

        }

    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GoldColors.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        RemoteImage(
            url = item.imageUrl,
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = item.totalAmount.toZarFormat(),
                color = GoldColors.Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(GoldColors.FieldBorder)
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Remove, null, tint = Color.White, modifier = Modifier.size(14.dp ))
                }

                Text(
                    text = "${item.quantity}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color = GoldColors.FieldBorder)
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }

            }

        }


        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Removed item",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

    }


}

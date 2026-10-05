package com.goldtime.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldtime.app.data.CartItem
import com.goldtime.app.data.CartRepository
import com.goldtime.app.data.SavedOrder
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onShop: () -> Unit,
    onSellGold: () -> Unit,
    onOrders: () -> Unit,
    vm: ProfileViewModel = viewModel()
) {

    val state by vm.state.collectAsStateWithLifecycle()

    val cartItems by CartRepository.items.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                GoldColors.Background
            )
            .navigationBarsPadding()
    ) {

        TopNavigation(
            onHome = onHome,
            onShop = onShop,
            onSellGold = onSellGold,
            onOrders = onOrders
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 26.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(0.dp)
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(32.dp)
                )

                Text(
                    text = "GOLD TIME CO.",
                    color = GoldColors.Gold,
                    fontSize = 10.sp,
                    fontWeight =
                        FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text = "My Profile",
                    color =
                        GoldColors.TextPrimary,
                    fontFamily =
                        HeadingFont,
                    fontSize = 34.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )
            }


            state.profile?.let { profile ->

                item {

                    ProfileCard(
                        name =
                            "${profile.firstName} ${profile.surname}",
                        email =
                            profile.email
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }
            }


            item {

                AccountSettingsRow()

                Spacer(
                    modifier =
                        Modifier.height(39.dp)
                )
            }


            item {

                Text(
                    text = "ORDER HISTORY",
                    color =
                        GoldColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight =
                        FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(13.dp)
                )
            }


            if (state.loading) {

                item {

                    Text(
                        text =
                            "Loading your orders...",
                        color =
                            GoldColors.TextMuted,
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }
            }


            state.error?.let { error ->

                item {

                    Text(
                        text = error,
                        color =
                            GoldColors.Error,
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }
            }

            items(
                items = cartItems,
                key = {
                    "cart-${it.name}"
                }
            ) { cartItem ->

                CartPendingRow(
                    item = cartItem
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }


            /*
             * REAL ORDERS FROM THE SERVER
             */

            items(
                items = state.orders,
                key = {
                    "order-${it.id}"
                }
            ) { order ->

                OrderHistoryRow(
                    order = order
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }


            if (
                !state.loading &&
                cartItems.isEmpty() &&
                state.orders.isEmpty() &&
                state.error == null
            ) {

                item {

                    EmptyOrderHistory()

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(40.dp)
                )
            }
        }
    }
}


@Composable
private fun TopNavigation(
    onHome: () -> Unit,
    onShop: () -> Unit,
    onSellGold: () -> Unit,
    onOrders: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(53.dp)
            .background(
                GoldColors.Background
            )
            .border(
                width = 1.dp,
                color =
                    GoldColors.Divider
            )
            .padding(
                horizontal = 26.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "GOLD TIME CO.",
            color =
                GoldColors.Gold,
            fontSize = 10.sp,
            fontWeight =
                FontWeight.Bold,
            letterSpacing = 2.8.sp
        )

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        NavText(
            text = "Home",
            onClick = onHome
        )

        Spacer(
            modifier =
                Modifier.width(27.dp)
        )

        NavText(
            text = "Shop",
            onClick = onShop
        )

        Spacer(
            modifier =
                Modifier.width(27.dp)
        )

        NavText(
            text = "Sell Gold",
            onClick = onSellGold
        )

        Spacer(
            modifier =
                Modifier.width(27.dp)
        )

        NavText(
            text = "Orders",
            onClick = onOrders
        )
    }
}


@Composable
private fun NavText(
    text: String,
    onClick: () -> Unit
) {

    Text(
        text = text,
        color =
            GoldColors.TextPrimary,
        fontSize = 12.sp,
        fontWeight =
            FontWeight.SemiBold,
        maxLines = 1,
        modifier =
            Modifier.clickable(
                onClick = onClick
            )
    )
}


@Composable
private fun ProfileCard(
    name: String,
    email: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(15.dp)
            )
            .background(
                GoldColors.Surface
            )
            .border(
                width = 1.dp,
                color =
                    GoldColors.Gold.copy(
                        alpha = 0.20f
                    ),
                shape =
                    RoundedCornerShape(15.dp)
            )
            .padding(
                horizontal = 17.dp,
                vertical = 18.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
                .background(
                    GoldColors.Gold.copy(
                        alpha = 0.12f
                    )
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint =
                    GoldColors.Gold,
                modifier =
                    Modifier.size(31.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.width(14.dp)
        )

        Column {

            Text(
                text = name,
                color =
                    GoldColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text = email,
                color =
                    GoldColors.TextMuted,
                fontSize = 10.sp
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(50.dp)
                    )
                    .background(
                        GoldColors.Gold.copy(
                            alpha = 0.14f
                        )
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 4.dp
                    )
            ) {

                Text(
                    text = "Gold Member",
                    color =
                        GoldColors.Gold,
                    fontSize = 8.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun AccountSettingsRow() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(13.dp)
            )
            .background(
                GoldColors.Surface
            )
            .padding(
                horizontal = 18.dp,
                vertical = 18.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                Icons.Outlined.Settings,
            contentDescription =
                "Account Settings",
            tint =
                GoldColors.Gold,
            modifier =
                Modifier.size(21.dp)
        )

        Spacer(
            modifier =
                Modifier.width(13.dp)
        )

        Text(
            text = "Account Settings",
            color =
                GoldColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        Text(
            text = "›",
            color =
                GoldColors.Gold,
            fontSize = 25.sp
        )
    }
}


/*
 * CURRENT CART ITEM
 *
 * Displays the item from the cart as Pending.
 */

@Composable
private fun CartPendingRow(
    item: CartItem
) {

    val total =
        item.price * item.quantity

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(13.dp)
            )
            .background(
                GoldColors.Surface
            )
            .padding(
                horizontal = 16.dp,
                vertical = 15.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = item.name,
                color =
                    GoldColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "GTC-CART · Pending checkout",
                color =
                    GoldColors.TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Column(
            horizontalAlignment =
                Alignment.End
        ) {

            Text(
                text =
                    "R ${"%.2f".format(Locale.ENGLISH, total)}",
                color =
                    GoldColors.Gold,
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            StatusBadge(
                status = "pending"
            )
        }
    }
}


@Composable
private fun OrderHistoryRow(
    order: SavedOrder
) {

    val productName =
        order.order.items
            .firstOrNull()
            ?.name
            ?: "Order"

    val date =
        formatOrderDate(
            order.createdAtUtc
        )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(13.dp)
            )
            .background(
                GoldColors.Surface
            )
            .padding(
                horizontal = 16.dp,
                vertical = 15.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = productName,
                color =
                    GoldColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "${orderReference(order)} · $date",
                color =
                    GoldColors.TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Column(
            horizontalAlignment =
                Alignment.End
        ) {

            Text(
                text =
                    "R ${order.order.total}",
                color =
                    GoldColors.Gold,
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            StatusBadge(
                status = order.status
            )
        }
    }
}


@Composable
private fun StatusBadge(
    status: String
) {

    val displayStatus =
        status
            .replace(
                "_",
                " "
            )
            .replaceFirstChar {
                it.titlecase()
            }

    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(50.dp)
            )
            .background(
                GoldColors.Gold.copy(
                    alpha = 0.12f
                )
            )
            .padding(
                horizontal = 9.dp,
                vertical = 4.dp
            )
    ) {

        Text(
            text = displayStatus,
            color =
                GoldColors.Gold,
            fontSize = 8.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
private fun EmptyOrderHistory() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(13.dp)
            )
            .background(
                GoldColors.Surface
            )
            .padding(
                vertical = 22.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                "No orders yet.",
            color =
                GoldColors.TextMuted,
            fontSize = 12.sp
        )
    }
}


private fun orderReference(
    order: SavedOrder
): String {

    if (
        order.id.startsWith(
            "GTC-",
            ignoreCase = true
        )
    ) {
        return order.id
    }

    val year =
        runCatching {

            Instant.parse(
                order.createdAtUtc
            )
                .atZone(
                    ZoneId.systemDefault()
                )
                .year

        }.getOrDefault(
            2026
        )

    return "GTC-$year-${order.id.takeLast(4).uppercase()}"
}


private fun formatOrderDate(
    date: String
): String {

    if (date.isBlank()) {
        return ""
    }

    return runCatching {

        val formatter =
            DateTimeFormatter.ofPattern(
                "dd MMM yyyy",
                Locale.ENGLISH
            )

        Instant.parse(date)
            .atZone(
                ZoneId.systemDefault()
            )
            .format(formatter)

    }.getOrDefault("")
}
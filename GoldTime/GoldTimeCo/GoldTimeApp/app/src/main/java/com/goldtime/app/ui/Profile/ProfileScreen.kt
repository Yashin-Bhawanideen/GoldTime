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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

//the profile screen: the user's details, their pending cart items and their order history
//the navigation callbacks are passed in so this screen does not need to know about the NavController
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onShop: () -> Unit,
    onSellGold: () -> Unit,
    onOrders: () -> Unit,
    onSignOut: () -> Unit,
    vm: ProfileViewModel = viewModel()
) {
    //collectAsStateWithLifecycle turns the ViewModel's StateFlow into Compose state
    //and stops collecting while the screen is not visible, which saves battery
    val state by vm.state.collectAsStateWithLifecycle()
    //the cart is also observed so the pending items update automatically when the cart changes
    val cartItems by CartRepository.items.collectAsStateWithLifecycle()
    //controls whether the "Log out?" confirmation dialog is showing
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = GoldColors.Surface,
            title = {
                Text(
                    "Log out?",
                    color = GoldColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "You will need to sign in again to use the app.",
                    color = GoldColors.TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onSignOut()
                    }
                ) {
                    Text(
                        "Log out",
                        color = GoldColors.Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = GoldColors.TextMuted)
                }
            }
        )
    }

    Scaffold(
        containerColor = GoldColors.Background,
        bottomBar = {
            ProfileBottomBar(
                onHome = onHome,
                onShop = onShop,
                onSellGold = onSellGold,
                onOrders = onOrders
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 26.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            //header: title on the left, logout icon on the right
            item {
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GOLD TIME CO.",
                            color = GoldColors.Gold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "My Profile",
                            color = GoldColors.TextPrimary,
                            fontFamily = HeadingFont,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldColors.Surface)
                            .border(
                                1.dp,
                                GoldColors.Gold.copy(alpha = 0.25f),
                                CircleShape
                            )
                            .clickable { showLogoutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Log out",
                            tint = GoldColors.Gold,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
            }

            state.profile?.let { profile ->
                item {
                    ProfileCard(
                        name = "${profile.firstName} ${profile.surname}",
                        email = profile.email
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                AccountSettingsRow()

                Spacer(modifier = Modifier.height(39.dp))
            }

            item {
                Text(
                    text = "ORDER HISTORY",
                    color = GoldColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )

                Spacer(modifier = Modifier.height(13.dp))
            }

            //loading message while the orders are being fetched
            if (state.loading) {
                item {
                    Text(
                        text = "Loading your orders...",
                        color = GoldColors.TextMuted,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = GoldColors.Error,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            //items still in the cart are shown as pending
            items(
                items = cartItems,
                key = { "cart-${it.name}" }
            ) { cartItem ->
                CartPendingRow(item = cartItem)

                Spacer(modifier = Modifier.height(8.dp))
            }

            //real orders from the server
            items(
                items = state.orders,
                key = { "order-${it.id}" }
            ) { order ->
                OrderHistoryRow(order = order)

                Spacer(modifier = Modifier.height(8.dp))
            }

            //message shown when there is nothing to display
            if (
                !state.loading &&
                cartItems.isEmpty() &&
                state.orders.isEmpty() &&
                state.error == null
            ) {
                item {
                    EmptyOrderHistory()

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

//one item in the bottom navigation bar
private data class ProfileNavItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

//the bottom navigation bar, styled the same as the home screen's bar
//no item is highlighted because the profile is not one of the four tabs
@Composable
private fun ProfileBottomBar(
    onHome: () -> Unit,
    onShop: () -> Unit,
    onSellGold: () -> Unit,
    onOrders: () -> Unit
) {
    val items = listOf(
        ProfileNavItem("Home", Icons.Outlined.Home, onHome),
        ProfileNavItem("Shop", Icons.Outlined.ShoppingBag, onShop),
        ProfileNavItem("Sell Gold", Icons.Outlined.Sell, onSellGold),
        ProfileNavItem("Orders", Icons.Outlined.Receipt, onOrders)
    )

    NavigationBar(
        containerColor = Color(0xFF0B0B0B),
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = false,
                onClick = item.onClick,
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoldColors.Gold,
                    selectedTextColor = GoldColors.Gold,
                    indicatorColor = Color(0xFF1F1B10),
                    unselectedIconColor = GoldColors.TextMuted,
                    unselectedTextColor = GoldColors.TextMuted
                )
            )
        }
    }
}

//the card showing the user's avatar icon, name, email and membership badge
@Composable
private fun ProfileCard(
    name: String,
    email: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(GoldColors.Surface)
            .border(
                width = 1.dp,
                color = GoldColors.Gold.copy(alpha = 0.20f),
                shape = RoundedCornerShape(15.dp)
            )
            .padding(
                horizontal = 17.dp,
                vertical = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
                .background(GoldColors.Gold.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = null,
                tint = GoldColors.Gold,
                modifier = Modifier.size(31.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = name,
                color = GoldColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = email,
                color = GoldColors.TextMuted,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(GoldColors.Gold.copy(alpha = 0.14f))
                    .padding(
                        horizontal = 9.dp,
                        vertical = 4.dp
                    )
            ) {
                Text(
                    text = "Gold Member",
                    color = GoldColors.Gold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

//the Account Settings row below the profile card
@Composable
private fun AccountSettingsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(GoldColors.Surface)
            .padding(
                horizontal = 18.dp,
                vertical = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Account Settings",
            tint = GoldColors.Gold,
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.width(13.dp))

        Text(
            text = "Account Settings",
            color = GoldColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "›",
            color = GoldColors.Gold,
            fontSize = 25.sp
        )
    }
}

//displays an item that is currently in the cart as a pending order
@Composable
private fun CartPendingRow(
    item: CartItem
) {
    val total = item.price * item.quantity

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(GoldColors.Surface)
            .padding(
                horizontal = 16.dp,
                vertical = 15.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = GoldColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "GTC-CART · Pending checkout",
                color = GoldColors.TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "R ${"%.2f".format(Locale.ENGLISH, total)}",
                color = GoldColors.Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            StatusBadge(status = "pending")
        }
    }
}

//one saved order from the server: product name, reference, date, total and status
@Composable
private fun OrderHistoryRow(
    order: SavedOrder
) {
    val productName =
        order.order.items
            .firstOrNull()
            ?.name
            ?: "Order"

    val date = formatOrderDate(order.createdAtUtc)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(GoldColors.Surface)
            .padding(
                horizontal = 16.dp,
                vertical = 15.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = productName,
                color = GoldColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${orderReference(order)} · $date",
                color = GoldColors.TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "R ${order.order.total}",
                color = GoldColors.Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            StatusBadge(status = order.status)
        }
    }
}

//a small rounded badge that shows the order status, e.g. "pending_payment" becomes "Pending payment"
@Composable
private fun StatusBadge(
    status: String
) {
    val displayStatus =
        status
            .replace("_", " ")
            .replaceFirstChar { it.titlecase() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(GoldColors.Gold.copy(alpha = 0.12f))
            .padding(
                horizontal = 9.dp,
                vertical = 4.dp
            )
    ) {
        Text(
            text = displayStatus,
            color = GoldColors.Gold,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

//shown when the user has no cart items and no orders
@Composable
private fun EmptyOrderHistory() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(GoldColors.Surface)
            .padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No orders yet.",
            color = GoldColors.TextMuted,
            fontSize = 12.sp
        )
    }
}

//builds a readable order reference such as GTC-2026-AB12 from the order id
private fun orderReference(
    order: SavedOrder
): String {
    if (order.id.startsWith("GTC-", ignoreCase = true)) {
        return order.id
    }

    val year =
        runCatching {
            Instant.parse(order.createdAtUtc)
                .atZone(ZoneId.systemDefault())
                .year
        }.getOrDefault(2026)

    return "GTC-$year-${order.id.takeLast(4).uppercase()}"
}

//turns the ISO date from the API into a short date such as 28 Sep 2026
private fun formatOrderDate(
    date: String
): String {
    if (date.isBlank()) {
        return ""
    }

    return runCatching {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

        Instant.parse(date)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }.getOrDefault("")
}
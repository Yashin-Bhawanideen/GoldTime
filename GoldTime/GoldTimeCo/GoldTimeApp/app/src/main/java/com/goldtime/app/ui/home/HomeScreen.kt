package com.goldtime.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldtime.app.data.CartRepository
import com.goldtime.app.data.FeaturedAsset
import com.goldtime.app.data.HeroContent
import com.goldtime.app.ui.components.RemoteImage
import com.goldtime.app.ui.theme.GoldColors
import com.goldtime.app.ui.theme.HeadingFont

private enum class HomeTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    SHOP("Shop", Icons.Outlined.ShoppingBag),
    SELL_GOLD("Sell Gold", Icons.Outlined.Sell),
    ORDERS("Orders", Icons.Outlined.Receipt)
}

@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    onRequestQuote: () -> Unit,
    onBrowse: () -> Unit,
    onCart: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }

    Scaffold(
        containerColor = GoldColors.Background,
        bottomBar = {
            GoldBottomBar(
                selected = selectedTab,
                onSelect = { tab ->
                    when (tab) {
                        HomeTab.HOME -> selectedTab = HomeTab.HOME
                        HomeTab.SHOP -> onBrowse()
                        HomeTab.SELL_GOLD -> selectedTab = HomeTab.SELL_GOLD
                        HomeTab.ORDERS -> onCart()
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                HomeTab.HOME -> HomeTabContent(
                    vm = vm,
                    onSignOut = onSignOut,
                    onRequestQuote = onRequestQuote,
                    onBrowse = onBrowse,
                    onCart = onCart
                )
                HomeTab.SELL_GOLD -> SellGoldTab()
                // SHOP and ORDERS never render here — tapping them calls onBrowse()/onCart()
                // and navigates away via AppNavigation, so selectedTab never becomes SHOP or ORDERS.
                else -> Unit
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    vm: HomeViewModel,
    onSignOut: () -> Unit,
    onRequestQuote: () -> Unit,
    onBrowse: () -> Unit,
    onCart: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val comingSoon = {
        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        Spacer(Modifier.height(8.dp))

        HomeHeader(
            onBrowse = onBrowse,
            onCart = onCart,
            onSignOut = onSignOut
        )

        Spacer(Modifier.height(16.dp))

        HeroBanner(
            hero = state.data.hero,
            onExplore = comingSoon
        )

        state.error?.let {
            Spacer(Modifier.height(10.dp))

            Text(
                it,
                color = GoldColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.load() }
            )
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Featured Assets",
                color = GoldColors.TextPrimary,
                fontFamily = HeadingFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Text(
                "SEE ALL",
                color = GoldColors.Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable { comingSoon() }
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                state.data.featured,
                key = { it.id }
            ) { asset ->

                AssetCard(
                    asset = asset,
                    onQuote = onRequestQuote
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SellGoldCard(
            onClick = comingSoon
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeHeader(
    onBrowse: () -> Unit,
    onCart: () -> Unit,
    onSignOut: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {

        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .border(
                    1.dp,
                    GoldColors.Gold.copy(alpha = 0.3f),
                    CircleShape
                )
                .clickable(onClick = onBrowse)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                Icons.Outlined.GridView,
                null,
                tint = GoldColors.Gold,
                modifier = Modifier.size(13.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                "BROWSE",
                color = GoldColors.Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Text(
            "GOLD TIME CO",
            color = GoldColors.Gold,
            fontFamily = HeadingFont,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Box {

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        "Account",
                        tint = GoldColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = GoldColors.Surface
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Sign out",
                                color = GoldColors.TextPrimary
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onSignOut()
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .clickable(onClick = onCart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ShoppingCart,
                    "Cart",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    hero: HeroContent,
    onExplore: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(shape)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.15f),
                shape
            )
    ) {

        RemoteImage(
            hero.imageUrl,
            Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x22000000),
                            Color(0xCC000000),
                            Color(0xFF000000)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 18.dp,
                    bottom = 18.dp,
                    end = 110.dp
                )
        ) {

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        GoldColors.Gold.copy(alpha = 0.16f)
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    )
            ) {
                Text(
                    hero.badge,
                    color = GoldColors.Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                hero.title,
                color = Color.White,
                fontFamily = HeadingFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                hero.subtitle,
                color = Color(0xFFB0B0B0),
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = 16.dp
                )
                .clip(CircleShape)
                .background(GoldColors.Gold)
                .clickable(onClick = onExplore)
                .padding(
                    horizontal = 18.dp,
                    vertical = 9.dp
                )
        ) {
            Text(
                "EXPLORE",
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun AssetCard(
    asset: FeaturedAsset,
    onQuote: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF161616))
    ) {

        RemoteImage(
            asset.imageUrl,
            Modifier
                .fillMaxWidth()
                .height(125.dp)
        )

        Column(
            Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 14.dp
            )
        ) {

            Text(
                asset.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                asset.cta,
                color = GoldColors.Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(
                    onClick = onQuote
                )
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GoldColors.Gold)
                    .clickable {
                        CartRepository.addItem(asset)
                        Toast.makeText(context, "${asset.name} added to cart", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ADD TO CART",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun SellGoldCard(
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1C1808),
                        Color(0xFF0F0F0F)
                    )
                )
            )
            .border(
                1.dp,
                GoldColors.Gold.copy(alpha = 0.2f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            Modifier.weight(1f)
        ) {

            Text(
                "SELL YOUR GOLD",
                color = GoldColors.Gold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Get top value for your gold",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Gold jewellery, coins, Krugerrands & bars",
                color = GoldColors.TextMuted,
                fontSize = 12.sp
            )
        }

        Icon(
            Icons.Outlined.ChevronRight,
            null,
            tint = GoldColors.Gold
        )
    }
}

@Composable
private fun GoldBottomBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0B0B0B),
        tonalElevation = 0.dp
    ) {
        HomeTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        tab.label,
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
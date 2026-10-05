package com.goldtime.app.navigation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.goldtime.app.data.AuthRepository
import com.goldtime.app.data.CartRepository
import com.goldtime.app.ui.auth.AuthViewModel
import com.goldtime.app.ui.auth.LoginScreen
import com.goldtime.app.ui.auth.RegisterScreen
import com.goldtime.app.ui.browse.AssetDetailScreen
import com.goldtime.app.ui.browse.BrowseScreen
import com.goldtime.app.ui.cart.CartScreen
import com.goldtime.app.ui.checkout.CheckoutScreen
import com.goldtime.app.ui.checkout.OrderReview
import com.goldtime.app.ui.checkout.checkoutOrder
import com.goldtime.app.ui.checkout.checkoutSnapshot
import com.goldtime.app.ui.home.HomeScreen
import com.goldtime.app.ui.profile.ProfileScreen
import com.goldtime.app.ui.quote.RequestQuoteScreen
import com.goldtime.app.ui.theme.GoldColors
import java.math.BigDecimal
//the route names for every screen in one place, so a typo in a route string cannot happen
//private object: only this file can use these names
private object Routes {

    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val REQUEST_QUOTE = "request_quote"
    const val BROWSE = "browse"
    const val ASSET_DETAIL = "asset_detail"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val PROFILE = "profile"
}
//the root composable that decides which screen is shown and how the user moves between screens
@Composable
fun AppNavigation() {

    val nav =
        rememberNavController()

    val authVm: AuthViewModel =
        viewModel()

    val start =
        if (AuthRepository.isLoggedIn) {
            Routes.HOME
        } else {
            Routes.LOGIN
        }
//Surface gives every screen the app's background colour
    Surface(
        color = GoldColors.Background
    ) {

        NavHost(
            navController = nav,
            startDestination = start
        ) {

            composable(
                Routes.LOGIN
            ) {

                LoginScreen(
                    vm = authVm,

                    onLoggedIn = {

                        nav.navigate(
                            Routes.HOME
                        ) {

                            popUpTo(
                                Routes.LOGIN
                            ) {
                                inclusive = true
                            }
                        }
                    },

                    onCreateAccount = {

                        nav.navigate(
                            Routes.REGISTER
                        )
                    }
                )
            }

            composable(
                Routes.REGISTER
            ) {

                RegisterScreen(
                    vm = authVm,

                    onRegistered = {

                        nav.navigate(
                            Routes.HOME
                        ) {

                            popUpTo(
                                Routes.LOGIN
                            ) {
                                inclusive = true
                            }
                        }
                    },

                    onBack = {
                        nav.popBackStack()
                    }
                )
            }

            composable(
                Routes.HOME
            ) {

                HomeScreen(

                    onSignOut = {

                        AuthRepository.signOut()

                        CartRepository.clearCart()

                        nav.navigate(
                            Routes.LOGIN
                        ) {

                            popUpTo(
                                Routes.HOME
                            ) {
                                inclusive = true
                            }
                        }
                    },

                    onProfile = {

                        nav.navigate(
                            Routes.PROFILE
                        )
                    },

                    onRequestQuote = {

                        nav.navigate(
                            Routes.REQUEST_QUOTE
                        )
                    },

                    onBrowse = {

                        nav.navigate(
                            Routes.BROWSE
                        )
                    },

                    onCart = {

                        nav.navigate(
                            Routes.CART
                        )
                    }
                )
            }

            composable(Routes.PROFILE) {

                ProfileScreen(

                    onBack = {
                        nav.popBackStack()
                    },

                    onHome = {
                        nav.navigate(Routes.HOME)
                    },

                    onShop = {
                        nav.navigate(Routes.BROWSE)
                    },

                    onSellGold = {
                        nav.navigate(Routes.REQUEST_QUOTE)
                    },

                    onOrders = {
                        nav.navigate(Routes.CART)
                    }
                )
            }

            composable(
                Routes.CART
            ) {

                CartScreen(

                    onBack = {
                        nav.popBackStack()
                    },

                    onCheckout = {

                        val items =
                            CartRepository.items.value

                        if (items.isNotEmpty()) {

                            nav.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set(
                                    "checkoutItems",
                                    items.checkoutSnapshot(
                                        CartRepository.deliveryFee
                                    )
                                )

                            nav.navigate(
                                Routes.CHECKOUT
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(
                Routes.CHECKOUT
            ) { entry ->

                val order =
                    remember(entry) {

                        nav.previousBackStackEntry
                            ?.savedStateHandle
                            ?.get<ArrayList<String>>(
                                "checkoutItems"
                            )
                            ?.checkoutOrder()
                            ?: OrderReview(
                                emptyList(),
                                BigDecimal.ZERO
                            )
                    }

                CheckoutScreen(
                    order = order,

                    onBackToCart = {
                        nav.popBackStack()
                    }
                )
            }

            composable(
                Routes.REQUEST_QUOTE
            ) {

                RequestQuoteScreen(
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }

            composable(
                Routes.BROWSE
            ) {

                BrowseScreen(

                    onBack = {
                        nav.popBackStack()
                    },

                    onAssetClick = { assetId ->

                        nav.navigate(
                            "${Routes.ASSET_DETAIL}/$assetId"
                        )
                    }
                )
            }

            composable(
                route = "${Routes.ASSET_DETAIL}/{assetId}",

                arguments = listOf(
                    navArgument("assetId") {
                        type = NavType.StringType
                    }
                )
            ) { entry ->

                val assetId =
                    entry.arguments
                        ?.getString("assetId")
                        .orEmpty()

                AssetDetailScreen(

                    assetId = assetId,

                    onBack = {
                        nav.popBackStack()
                    }
                )
            }
        }
    }
}
// References
// developers, A., 2025. Create a navigation controller. [Online] 
// Available at: https://developer.android.com/guide/navigation/navcontroller
// Geek4geeks, 2025. Navigation Drawer in Android. [Online] 
// Available at: https://www.geeksforgeeks.org/android/navigation-drawer-in-android/
// studio, A., 2021. Navigation Editor. [Online] 
// Available at: https://developer.android.com/guide/navigation/design/editor
// studio, A., 2026. Understand and implement the basics. [Online] 
// Available at: https://developer.android.com/guide/navigation/navigation-3/basics



package com.goldtime.app.navigation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.goldtime.app.data.AuthRepository
import com.goldtime.app.ui.auth.AuthViewModel
import com.goldtime.app.ui.auth.LoginScreen
import com.goldtime.app.ui.auth.RegisterScreen
import com.goldtime.app.ui.browse.BrowseScreen
import com.goldtime.app.ui.home.HomeScreen
import com.goldtime.app.ui.quote.RequestQuoteScreen
import com.goldtime.app.ui.theme.GoldColors

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val REQUEST_QUOTE = "request_quote"
    const val BROWSE = "browse"
    const val CART = "cart"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val authVm: AuthViewModel = viewModel()

    val start =
        if (AuthRepository.isLoggedIn) {
            Routes.HOME
        } else {
            Routes.LOGIN
        }

    Surface(
        color = GoldColors.Background
    ) {
        NavHost(
            navController = nav,
            startDestination = start
        ) {

            composable(Routes.LOGIN) {
                LoginScreen(
                    vm = authVm,
                    onLoggedIn = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) {
                                inclusive = true
                            }
                        }
                    },
                    onCreateAccount = {
                        nav.navigate(Routes.REGISTER)
                    }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    vm = authVm,
                    onRegistered = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) {
                                inclusive = true
                            }
                        }
                    },
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onSignOut = {
                        AuthRepository.signOut()

                        nav.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) {
                                inclusive = true
                            }
                        }
                    },
                    onRequestQuote = {
                        nav.navigate(Routes.REQUEST_QUOTE)
                    },
                    onBrowse = {
                        nav.navigate(Routes.BROWSE)
                    },
                    onCart = {nav.navigate(Routes.CART)}
                )
            }

            composable(Routes.CART) {
                com.goldtime.app.ui.cart.CartScreen(
                    onBack = {nav.popBackStack() },
                    onCheckout = { /* do payment here */}
                )
            }

            composable(Routes.REQUEST_QUOTE) {
                RequestQuoteScreen(
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }

            composable(Routes.BROWSE) {
                BrowseScreen(
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }
        }
    }
}
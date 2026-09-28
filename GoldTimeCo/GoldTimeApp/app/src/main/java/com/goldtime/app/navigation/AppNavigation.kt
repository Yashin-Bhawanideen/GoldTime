package com.goldtime.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.goldtime.app.data.AuthRepository
import com.goldtime.app.ui.auth.AuthViewModel
import com.goldtime.app.ui.auth.LoginScreen
import com.goldtime.app.ui.auth.RegisterScreen
import com.goldtime.app.ui.home.HomeScreen
import com.goldtime.app.ui.theme.GoldColors

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val authVm: AuthViewModel = viewModel()

    // Already signed in from a previous session? Go straight to Home.
    val start = if (AuthRepository.isLoggedIn) Routes.HOME else Routes.LOGIN

    Surface(color = GoldColors.Background) {
        NavHost(navController = nav, startDestination = start) {

            composable(Routes.LOGIN) {
                LoginScreen(
                    vm = authVm,
                    onLoggedIn = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onCreateAccount = { nav.navigate(Routes.REGISTER) }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    vm = authVm,
                    onRegistered = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onSignOut = {
                        AuthRepository.signOut()
                        nav.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                    }
                )
            }
        }
    }
}

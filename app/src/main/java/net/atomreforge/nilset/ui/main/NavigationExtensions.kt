package net.atomreforge.nilset.ui.main

import androidx.navigation.NavController

fun NavController.popBackStackIfCurrent(expectedRoute: String) {
    if (currentBackStackEntry?.destination?.route == expectedRoute) {
        popBackStack()
    }
}

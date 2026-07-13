package com.quickcleanpro.phonecleaner.app.navigation

import androidx.navigation.NavHostController

/** The only API allowed to mutate the application navigation stack. */
interface AppNavigator {
    val currentRoute: String?

    fun open(
        destination: AppDestination,
        args: Map<String, String> = emptyMap(),
    )

    fun openRoute(route: String)

    fun replace(destination: AppDestination)

    fun resetTo(destination: AppDestination)

    fun home()

    fun back(): Boolean
}

class NavHostControllerAppNavigator(
    private val navController: NavHostController,
) : AppNavigator {
    override val currentRoute: String?
        get() = navController.currentDestination?.route

    override fun open(destination: AppDestination, args: Map<String, String>) {
        openRoute(destination.withArgs(args))
    }

    override fun openRoute(route: String) {
        runCatching {
            navController.navigate(route) { launchSingleTop = true }
        }
    }

    override fun replace(destination: AppDestination) {
        runCatching {
            val current = navController.currentDestination?.route
            navController.navigate(destination.route) {
                current?.let { popUpTo(it) { inclusive = true } }
                launchSingleTop = true
            }
        }
    }

    override fun resetTo(destination: AppDestination) {
        runCatching {
            navigateHomeClearingStack()
            if (destination.route !in AppDestination.homeRoutes) {
                navController.navigate(destination.route) { launchSingleTop = true }
            }
        }
    }

    override fun home() {
        runCatching { navigateHomeClearingStack() }
    }

    override fun back(): Boolean =
        runCatching {
            if (currentRoute in AppDestination.rootRoutes) return@runCatching false
            if (navController.popBackStack()) return@runCatching true
            home()
            true
        }.getOrDefault(false)

    private fun navigateHomeClearingStack() {
        if (currentRoute in AppDestination.homeRoutes) return
        val existingHomeRoute =
            AppDestination.homeRoutes.firstOrNull { route ->
                runCatching { navController.getBackStackEntry(route) }.isSuccess
            }
        if (existingHomeRoute != null && navController.popBackStack(existingHomeRoute, false)) return
        navController.navigate(AppDestination.Home.route) { launchSingleTop = true }
    }
}

fun NavHostController.appNavigator(): AppNavigator = NavHostControllerAppNavigator(this)

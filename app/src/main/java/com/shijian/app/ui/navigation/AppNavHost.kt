package com.shijian.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shijian.app.AppContainer
import com.shijian.app.ui.screens.expense.AddRecordScreen
import com.shijian.app.ui.screens.expense.CalendarDetailScreen
import com.shijian.app.ui.screens.expense.ExpenseScreen
import com.shijian.app.ui.screens.food.FoodScreen
import com.shijian.app.ui.screens.home.HomeScreen
import com.shijian.app.ui.screens.news.NewsScreen
import com.shijian.app.ui.screens.news.NewsSettingsScreen
import com.shijian.app.ui.screens.profile.AddressManageScreen
import com.shijian.app.ui.screens.profile.FoodListScreen
import com.shijian.app.ui.screens.profile.FoodSettingsScreen
import com.shijian.app.ui.screens.profile.PrivacyScreen
import com.shijian.app.ui.screens.profile.ProfileScreen
import com.shijian.app.ui.screens.profile.StatsScreen
import com.shijian.app.ui.screens.profile.UpdatesScreen

@Composable
fun AppNavHost(
    container: AppContainer,
    startTab: String = Routes.HOME
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val bottomVisible = currentDestination?.hierarchy?.any { dest ->
        dest.route?.substringBefore('?') in Routes.TABS
    } == true

    /** 统一导航：Tab 走栈切换，其余压栈 */
    val navigate: (String) -> Unit = { route ->
        val base = route.substringBefore('?')
        if (base in Routes.TABS) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(route)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (bottomVisible) {
                SjBottomBar(
                    currentRoute = currentDestination?.route,
                    onTabSelected = { tab ->
                        navController.navigate(tab) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startTab,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(container = container, nav = navigate)
            }
            composable(Routes.FOOD) {
                FoodScreen(container = container, nav = navigate)
            }
            // 开销 Tab 基础路由（不带参数），直接显示全部
            composable(Routes.EXPENSE) {
                ExpenseScreen(
                    container = container,
                    nav = navigate,
                    initialFilter = ""
                )
            }
            composable(
                route = Routes.EXPENSE_FILTER,
                arguments = listOf(
                    navArgument("filter") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                ExpenseScreen(
                    container = container,
                    nav = navigate,
                    initialFilter = entry.arguments?.getString("filter").orEmpty()
                )
            }
            composable(Routes.NEWS) {
                NewsScreen(container = container, nav = navigate)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(container = container, nav = navigate)
            }

            // ---- 子页面 ----
            composable(
                route = Routes.ADD_RECORD,
                arguments = listOf(
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("type") { type = NavType.StringType; defaultValue = "" },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                AddRecordScreen(
                    container = container,
                    editId = entry.arguments?.getLong("editId")?.takeIf { it > 0 },
                    initialType = entry.arguments?.getString("type").orEmpty(),
                    initialDate = entry.arguments?.getString("date").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CALENDAR_DETAIL) { entry ->
                CalendarDetailScreen(
                    container = container,
                    date = entry.arguments?.getString("date") ?: "",
                    onBack = { navController.popBackStack() },
                    nav = navigate
                )
            }
            composable(Routes.NEWS_SETTINGS) {
                NewsSettingsScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.ADDRESS_MANAGE) {
                AddressManageScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.FOOD_SETTINGS) {
                FoodSettingsScreen(container = container, onBack = { navController.popBackStack() }, nav = navigate)
            }
            composable(
                route = Routes.FOOD_LIST,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = "favorites" }
                )
            ) { entry ->
                FoodListScreen(
                    container = container,
                    type = entry.arguments?.getString("type") ?: "favorites",
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.UPDATES) {
                UpdatesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

package com.uthtodo.dpgh.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uthtodo.dpgh.data.local.SessionManager
import com.uthtodo.dpgh.data.model.OnboardingData
import com.uthtodo.dpgh.ui.screens.AddEditTaskScreen
import com.uthtodo.dpgh.ui.screens.HomeScreen
import com.uthtodo.dpgh.ui.screens.LoginScreen
import com.uthtodo.dpgh.ui.screens.NotificationScreen
import com.uthtodo.dpgh.ui.screens.OnboardingScreen
import com.uthtodo.dpgh.ui.screens.ProfileScreen
import com.uthtodo.dpgh.ui.screens.SettingsScreen
import com.uthtodo.dpgh.ui.screens.TaskDetailScreen
import com.uthtodo.dpgh.ui.screens.TaskListScreen
import com.uthtodo.dpgh.ui.screens.WelcomeScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            WelcomeScreen(
                onReadyClick = {
                    if (SessionManager.hasLoggedIn()) {
                        navController.navigate("home") {
                            popUpTo("start") { inclusive = true }
                        }
                    } else {
                        navController.navigate("onboarding/1") {
                            popUpTo("start") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("onboarding/1") {
            OnboardingScreen(
                step = OnboardingData.steps[0],
                onNextClick = {
                    navController.navigate("onboarding/2")
                },
                onSkipClick = {
                    navController.navigate("auth") {
                        popUpTo("start") { inclusive = true }
                    }
                },
                onDotClick = { step ->
                    if (step != 1) navController.navigate("onboarding/$step")
                }
            )
        }

        composable("onboarding/2") {
            OnboardingScreen(
                step = OnboardingData.steps[1],
                onNextClick = {
                    navController.navigate("onboarding/3")
                },
                onSkipClick = {
                    navController.navigate("auth") {
                        popUpTo("start") { inclusive = true }
                    }
                },
                onDotClick = { step ->
                    if (step != 2) navController.navigate("onboarding/$step")
                }
            )
        }

        composable("onboarding/3") {
            OnboardingScreen(
                step = OnboardingData.steps[2],
                onNextClick = {
                    navController.navigate("auth") {
                        popUpTo("start") { inclusive = true }
                    }
                },
                onSkipClick = {
                    navController.navigate("auth") {
                        popUpTo("start") { inclusive = true }
                    }
                },
                onDotClick = { step ->
                    if (step != 3) navController.navigate("onboarding/$step")
                }
            )
        }

        composable("auth") {
            LoginScreen(
                onGoogleClick = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onGuestClick = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            HomeScreen(
                onAvatarClick = { navController.navigate("profile") },
                onNotificationClick = {
                    navController.navigate("notifications") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                },
                onViewAllClick = {
                    navController.navigate("tasks") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                },
                onCategoryClick = { cat ->
                    navController.navigate("tasks?category=$cat")
                },
                onTaskClick = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
                onAddTaskClick = {
                    navController.navigate("task_form")
                },
                onNavClick = { route ->
                    if (route == "home") {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(route) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = "tasks?category={category}",
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")
            TaskListScreen(
                initialCategoryFilter = category,
                onTaskClick = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
                onAddTaskClick = {
                    navController.navigate("task_form")
                },
                onNavClick = { route ->
                    if (route == "home") {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(route) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = "task_detail/{taskId}",
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
            TaskDetailScreen(
                taskId = taskId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate("task_form?taskId=$id")
                }
            )
        }

        composable(
            route = "task_form?taskId={taskId}",
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val taskIdStr = backStackEntry.arguments?.getString("taskId")
            val taskId = taskIdStr?.toIntOrNull()
            AddEditTaskScreen(
                taskId = taskId,
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable("notifications") {
            NotificationScreen(
                onAddTaskClick = {
                    navController.navigate("task_form")
                },
                onNavClick = { route ->
                    if (route == "home") {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(route) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onAddTaskClick = {
                    navController.navigate("task_form")
                },
                onNavClick = { route ->
                    if (route == "home") {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(route) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLogoutClick = {
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onLinkGoogleClick = {
                    navController.navigate("auth")
                }
            )
        }
    }
}

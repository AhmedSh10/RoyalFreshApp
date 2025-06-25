package com.example.royalfreshapp.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.royalfreshapp.bluetooth.BluetoothViewModel
import com.example.royalfreshapp.bluetooth.BluetoothStatus
import com.example.royalfreshapp.ui.screens.*
import com.example.royalfreshapp.viewmodel.ScheduleViewModel

// Define navigation routes
object Routes {
    const val SPLASH = "splash"
    const val PASSWORD = "password"
    const val DEVICES = "devices"
    const val SCHEDULE_SCREEN = "schedule_screen"
    const val ADD_SCHEDULE_SCREEN = "add_schedule_screen"
    const val EDIT_SCHEDULE_SCREEN = "edit_schedule_screen/{scheduleId}"
}

@Composable
fun AppNavigation(bluetoothViewModel: BluetoothViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Initialize ScheduleViewModel
    val scheduleViewModel: ScheduleViewModel = viewModel()

    // Observe schedules from database
    val schedules by scheduleViewModel.allSchedules.observeAsState(emptyList())

    // Observe connection status to determine if we need to show devices screen
    val connectionStatus by bluetoothViewModel.connectionStatus.observeAsState(BluetoothStatus.IDLE)
    val connectedDeviceName by bluetoothViewModel.connectedDeviceName.observeAsState()

    // Determine the start destination based on SharedPreferences and connection status
    LaunchedEffect(key1 = Unit) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val passwordEntered = sharedPref.getBoolean(KEY_PASSWORD_ENTERED, false)

        // If password is entered, check if we have an active connection
        startDestination = if (passwordEntered) {
            // We'll always start with splash, it will navigate based on connection check
            Routes.SPLASH
        } else {
            Routes.SPLASH
        }
    }

    // Show loading indicator until start destination is determined
    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return // Exit composition until startDestination is set
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        composable(Routes.SPLASH) {
            // Splash screen now needs to know where to navigate *after* its delay
            SplashScreen { // Pass a lambda to execute after delay
                val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val passwordEntered = sharedPref.getBoolean(KEY_PASSWORD_ENTERED, false)

                // If password is entered, check connection status
                if (passwordEntered) {
                    // If we're already connected, go directly to schedule screen
                    // Otherwise, go to devices screen to establish connection
                    val destination = if (connectionStatus == BluetoothStatus.CONNECTED) {
                        Routes.SCHEDULE_SCREEN
                    } else {
                        Routes.DEVICES
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    // Password not entered yet, go to password screen
                    navController.navigate(Routes.PASSWORD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.PASSWORD) {
            PasswordScreen(navController = navController) {
                // This lambda is called when password is correct
                // Check if we already have a connection
                if (connectionStatus == BluetoothStatus.CONNECTED) {
                    navController.navigate(Routes.SCHEDULE_SCREEN) {
                        popUpTo(Routes.PASSWORD) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.DEVICES) {
                        popUpTo(Routes.PASSWORD) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.DEVICES) {
            DevicesScreen(
                navController = navController,
                viewModel = bluetoothViewModel
            )
        }

        composable(Routes.SCHEDULE_SCREEN) {
            ScheduleScreen(
                navController = navController,
                scheduleItems = schedules,
                onAddScheduleClick = {
                    navController.navigate(Routes.ADD_SCHEDULE_SCREEN)
                },
                onEditSchedule = { scheduleItem ->
                    // Navigate to edit screen with schedule ID
                    navController.navigate("edit_schedule_screen/${scheduleItem.id}")
                },
                onDeleteSchedule = { scheduleItem ->
                    // Delete the schedule
                    scheduleViewModel.delete(scheduleItem)
                },
                onToggleChange = { scheduleItem, isOn ->
                    // Update toggle state
                    scheduleViewModel.updateToggleState(scheduleItem, isOn)
                }
            )
        }

        composable(Routes.ADD_SCHEDULE_SCREEN) {
            AddScheduleScreen(
                navController = navController,
                onSaveSchedule = { newSchedule ->
                    // Add the new schedule to database
                    scheduleViewModel.insert(newSchedule)
                    // Navigate back to schedule screen
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_SCHEDULE_SCREEN,
            arguments = listOf(navArgument("scheduleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getLong("scheduleId") ?: 0L
            val scheduleToEdit = schedules.find { it.id == scheduleId }

            if (scheduleToEdit != null) {
                AddScheduleScreen(
                    navController = navController,
                    scheduleToEdit = scheduleToEdit,
                    onSaveSchedule = { updatedSchedule ->
                        // Update the schedule in database
                        scheduleViewModel.update(updatedSchedule)
                        // Navigate back to schedule screen
                        navController.popBackStack()
                    }
                )
            } else {
                // Handle case where schedule is not found
                Text("Schedule not found")
            }
        }
    }
}

package com.example.royalfreshapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import android.util.Log

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.material3.*

import androidx.lifecycle.ViewModelProvider
import com.example.royalfreshapp.bluetooth.BluetoothViewModel
import com.example.royalfreshapp.navigation.AppNavigation

import com.example.royalfreshapp.utils.TAG


// --- Theme Placeholder ---
@Composable
fun RoyalFreshTheme(content: @Composable () -> Unit) {
    // Replace with your actual theme if you have one
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}



class MainActivity : ComponentActivity() {

    private lateinit var bluetoothViewModel: BluetoothViewModel

    // Activity Result Launchers (must be registered in Activity/Fragment)
    private val requestBluetoothEnable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "MainActivity: requestBluetoothEnable result: ${result.resultCode}")
        // Ensure viewModel is initialized before accessing
        if (::bluetoothViewModel.isInitialized) {
            bluetoothViewModel.handleBluetoothEnableResult(result.resultCode)
        } else {
            Log.e(TAG, "MainActivity: bluetoothViewModel not initialized when handling enable result.")
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        Log.d(TAG, "MainActivity: requestMultiplePermissions result: $permissions")
        // Ensure viewModel is initialized before accessing
        if (::bluetoothViewModel.isInitialized) {
            bluetoothViewModel.handlePermissionResult(permissions)
        } else {
            Log.e(TAG, "MainActivity: bluetoothViewModel not initialized when handling permission result.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity: onCreate called.")
        enableEdgeToEdge()

        // Initialize ViewModel early to ensure it\'s available for launcher callbacks
        bluetoothViewModel = ViewModelProvider(this)[BluetoothViewModel::class.java]
        bluetoothViewModel.setActivityResultLaunchers(requestBluetoothEnable, requestMultiplePermissions)
        Log.d(TAG, "MainActivity: BluetoothViewModel initialized and launchers set.")

        setContent {

            RoyalFreshTheme {
                // Set up the navigation
                AppNavigation(bluetoothViewModel)
            }

        }

    }

    // Only disconnect when the app is actually being destroyed, not on configuration changes
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity: onDestroy called. isFinishing: $isFinishing")
        // Only disconnect if this is a real destroy, not a configuration change
        if (isFinishing) {
            // Ensure Bluetooth connection is closed when activity is destroyed
            if (::bluetoothViewModel.isInitialized) {
                bluetoothViewModel.disconnect()
                Log.d(TAG, "MainActivity: BluetoothViewModel disconnected on destroy.")
            }
        }
    }



}




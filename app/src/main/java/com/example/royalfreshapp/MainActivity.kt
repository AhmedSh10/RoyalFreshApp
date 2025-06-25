package com.example.royalfreshapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.royalfreshapp.ui.theme.RoyalFreshAppTheme

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.royalfreshapp.bluetooth.BluetoothViewModel
import com.example.royalfreshapp.navigation.AppNavigation

import kotlinx.coroutines.*
import java.io.IOException
import java.io.OutputStream
import java.util.*


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
        // Ensure viewModel is initialized before accessing
        if (::bluetoothViewModel.isInitialized) {
            bluetoothViewModel.handleBluetoothEnableResult(result.resultCode)
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        // Ensure viewModel is initialized before accessing
        if (::bluetoothViewModel.isInitialized) {
            bluetoothViewModel.handlePermissionResult(permissions)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewModel early to ensure it's available for launcher callbacks
        bluetoothViewModel = ViewModelProvider(this)[BluetoothViewModel::class.java]
        bluetoothViewModel.setActivityResultLaunchers(requestBluetoothEnable, requestMultiplePermissions)

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
        // Only disconnect if this is a real destroy, not a configuration change
        if (isFinishing) {
            // Ensure Bluetooth connection is closed when activity is destroyed
            if (::bluetoothViewModel.isInitialized) {
                bluetoothViewModel.disconnect()
            }
        }
    }



}


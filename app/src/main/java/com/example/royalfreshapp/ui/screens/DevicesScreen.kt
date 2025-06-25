package com.example.royalfreshapp.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.royalfreshapp.bluetooth.BluetoothStatus
import com.example.royalfreshapp.bluetooth.BluetoothViewModel
import com.example.royalfreshapp.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission") // Permissions checked in ViewModel
@Composable
fun DevicesScreen(
    navController: NavController,
    viewModel: BluetoothViewModel = viewModel()
) {
    val connectionStatus by viewModel.connectionStatus.observeAsState(BluetoothStatus.IDLE)
    val availableDevices by viewModel.availableDevices.observeAsState(emptyList())
    val errorMessage by viewModel.errorMessage.observeAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.observeAsState()

    // Effect to initialize Bluetooth when the screen is first composed
    LaunchedEffect(key1 = Unit) {
        viewModel.initializeBluetooth()
    }

    // Snackbar host state for showing errors and connection success
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error messages in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    // Show connection success message
    LaunchedEffect(connectionStatus) {
        if (connectionStatus == BluetoothStatus.CONNECTED) {
            snackbarHostState.showSnackbar(
                message = "Connected successfully to ${connectedDeviceName ?: "device"}",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Available Devices") },
                actions = {
                    IconButton(
                        onClick = { viewModel.scanForPairedDevices() },
                        enabled = connectionStatus != BluetoothStatus.SCANNING && 
                                connectionStatus != BluetoothStatus.CONNECTING
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Devices")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Connection Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                when (connectionStatus) {
                    BluetoothStatus.IDLE -> Icon(
                        Icons.Default.Settings, 
                        contentDescription = "Idle", 
                        tint = LocalContentColor.current.copy(alpha = 0.6f)
                    )
                    BluetoothStatus.SCANNING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), 
                            strokeWidth = 2.dp
                        )
                    }
                    BluetoothStatus.CONNECTING -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), 
                        strokeWidth = 2.dp
                    )
                    BluetoothStatus.CONNECTED -> Icon(
                        Icons.Default.Check, 
                        contentDescription = "Connected", 
                        tint = Color(0xFF00C853) // Green color for connected
                    )
                    BluetoothStatus.ERROR -> Icon(
                        Icons.Default.Warning, 
                        contentDescription = "Error", 
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (connectionStatus) {
                        BluetoothStatus.IDLE -> if (connectedDeviceName != null) 
                            "Connected to $connectedDeviceName" 
                        else 
                            "Select a device to connect"
                        BluetoothStatus.SCANNING -> "Scanning for paired devices..."
                        BluetoothStatus.CONNECTING -> "Connecting to ${connectedDeviceName ?: "device"}..."
                        BluetoothStatus.CONNECTED -> "Connected to ${connectedDeviceName ?: "device"}"
                        BluetoothStatus.ERROR -> "Error - ${errorMessage ?: "Check permissions or Bluetooth"}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Device List
            if (connectionStatus == BluetoothStatus.SCANNING && availableDevices.isEmpty()) {
                // Show progress indicator centered while scanning initially
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Scanning...", modifier = Modifier.padding(top = 60.dp))
                }
            } else if (availableDevices.isEmpty() && connectionStatus != BluetoothStatus.SCANNING) {
                // Show message if no devices found and not scanning
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No paired devices found.\nMake sure to pair your HC-05 device in Bluetooth settings first, then press refresh.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Show the list of devices
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(availableDevices) { device ->
                        DeviceItem(
                            device = device, 
                            viewModel = viewModel, 
                            onClick = {
                                if (connectionStatus != BluetoothStatus.CONNECTING && 
                                   connectionStatus != BluetoothStatus.CONNECTED) {
                                    viewModel.connectToDevice(device)
                                }
                            }
                        )
                    }
                }
            }

            // Next Button (only visible when connected)
            AnimatedVisibility(visible = connectionStatus == BluetoothStatus.CONNECTED) {
                Button(
                    onClick = { navController.navigate(Routes.SCHEDULE_SCREEN)

                              },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }
}

@SuppressLint("MissingPermission") // Permissions checked in ViewModel
@Composable
fun DeviceItem(
    device: BluetoothDevice, 
    viewModel: BluetoothViewModel, 
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info, 
                contentDescription = "Bluetooth Device", 
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = viewModel.getDeviceName(device),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

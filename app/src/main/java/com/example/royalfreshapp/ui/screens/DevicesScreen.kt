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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.royalfreshapp.R
import com.example.royalfreshapp.bluetooth.BluetoothStatus
import com.example.royalfreshapp.bluetooth.BluetoothViewModel
import com.example.royalfreshapp.navigation.Routes
import android.util.Log
import android.widget.Toast
import com.example.royalfreshapp.utils.TAG

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission") // Permissions checked in ViewModel
@Composable
fun DevicesScreen(
    navController: NavController,
    viewModel: BluetoothViewModel = viewModel()
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val context = LocalContext.current

    // Effect to initialize Bluetooth when the screen is first composed
    LaunchedEffect(key1 = Unit) {
        Log.d(TAG, "DevicesScreen: Initializing Bluetooth on screen composition.")
        viewModel.initializeBluetooth()
    }

    // Snackbar host state for showing errors and connection success
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Log.e(TAG, "DevicesScreen: Showing error snackbar: $it")
            Toast.makeText(navController.context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // String resources for Toast (need to be read in composable scope)
    val connectedSuccessfullyStr = stringResource(R.string.connected_successfully_to)

    // Show connection success message
    LaunchedEffect(connectionStatus) {
        if (connectionStatus == BluetoothStatus.CONNECTED) {
            val deviceName = connectedDeviceName ?: "device"
            val msg = String.format(connectedSuccessfullyStr, deviceName)
            Log.d(TAG, "DevicesScreen: Showing success snackbar: $msg")
            Toast.makeText(navController.context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.available_devices)) },
                actions = {
                    IconButton(
                        onClick = {
                            Log.d(TAG, "DevicesScreen: Refresh button clicked.")
                            viewModel.scanForPairedDevices()
                        },
                        enabled = connectionStatus != BluetoothStatus.SCANNING &&
                                connectionStatus != BluetoothStatus.CONNECTING
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_devices))
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
                        contentDescription = stringResource(R.string.idle),
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
                        contentDescription = stringResource(R.string.connected),
                        tint = Color(0xFF00C853)
                    )
                    BluetoothStatus.ERROR -> Icon(
                        Icons.Default.Warning,
                        contentDescription = stringResource(R.string.error),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (connectionStatus) {
                        BluetoothStatus.IDLE -> if (connectedDeviceName != null)
                            stringResource(R.string.connected_to, connectedDeviceName!!)
                        else
                            stringResource(R.string.select_device_to_connect)
                        BluetoothStatus.SCANNING -> stringResource(R.string.scanning_for_paired_devices)
                        BluetoothStatus.CONNECTING -> stringResource(R.string.connecting_to, connectedDeviceName ?: "device")
                        BluetoothStatus.CONNECTED -> stringResource(R.string.connected_to, connectedDeviceName ?: "device")
                        BluetoothStatus.ERROR -> stringResource(R.string.error_with_message, errorMessage ?: stringResource(R.string.error_check_permissions))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Device List
            if (connectionStatus == BluetoothStatus.SCANNING && availableDevices.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.scanning), modifier = Modifier.padding(top = 60.dp))
                }
                Log.d(TAG, "DevicesScreen: Showing scanning indicator.")
            } else if (availableDevices.isEmpty() && connectionStatus != BluetoothStatus.SCANNING) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_paired_devices),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Log.d(TAG, "DevicesScreen: No paired devices found.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(availableDevices) { device ->
                        DeviceItem(
                            device = device,
                            viewModel = viewModel,
                            onClick = {
                                Log.d(TAG, "DevicesScreen: Device item clicked: ${viewModel.getDeviceName(device)}")
                                if (connectionStatus != BluetoothStatus.CONNECTING &&
                                    connectionStatus != BluetoothStatus.CONNECTED) {
                                    viewModel.connectToDevice(device)
                                } else {
                                    Log.w(TAG, "DevicesScreen: Ignoring device click, already connecting or connected.")
                                }
                            }
                        )
                    }
                }
                Log.d(TAG, "DevicesScreen: Displaying available devices.")
            }

            // Next Button (only visible when connected)
            AnimatedVisibility(visible = connectionStatus == BluetoothStatus.CONNECTED) {
                Button(
                    onClick = {
                        Log.d(TAG, "DevicesScreen: Next button clicked, navigating to ScheduleScreen.")
                        navController.navigate(Routes.SCHEDULE_SCREEN)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(stringResource(R.string.next))
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
                contentDescription = stringResource(R.string.bluetooth_device),
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

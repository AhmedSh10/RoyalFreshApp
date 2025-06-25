package com.example.royalfreshapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.royalfreshapp.bluetooth.BluetoothStatus
import com.example.royalfreshapp.bluetooth.BluetoothViewModel
import androidx.compose.runtime.livedata.observeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    navController: NavController,
    viewModel: BluetoothViewModel = viewModel()
) {
    val connectionStatus by viewModel.connectionStatus.observeAsState(BluetoothStatus.IDLE)
    val connectedDeviceName by viewModel.connectedDeviceName.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()
    
    // Snackbar host state for showing errors
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Control Panel") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionStatus) {
                        BluetoothStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                        BluetoothStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (connectionStatus) {
                            BluetoothStatus.CONNECTED -> "Connected to ${connectedDeviceName ?: "device"}"
                            BluetoothStatus.ERROR -> "Connection Error"
                            else -> "Not Connected"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (connectionStatus != BluetoothStatus.CONNECTED) {
                        Text(
                            text = "Please go back and connect to a device",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Control buttons
            Text(
                text = "LED Control",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.sendCommand("A") },
                    enabled = connectionStatus == BluetoothStatus.CONNECTED,
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("ON", style = MaterialTheme.typography.titleLarge)
                }
                
                Button(
                    onClick = { viewModel.sendCommand("B") },
                    enabled = connectionStatus == BluetoothStatus.CONNECTED,
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("OFF", style = MaterialTheme.typography.titleLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status message
            Text(
                text = if (connectionStatus == BluetoothStatus.CONNECTED) 
                    "Ready to send commands" 
                else 
                    "Connect to a device to send commands",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

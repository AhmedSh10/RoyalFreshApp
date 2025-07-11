package com.example.royalfreshapp.bluetooth

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
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.example.royalfreshapp.utils.SPP_UUID
import com.example.royalfreshapp.utils.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.OutputStream

enum class BluetoothStatus { IDLE, SCANNING, CONNECTING, CONNECTED, ERROR }

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val _bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectJob: Job? = null

    // StateFlow for UI state
    private val _connectionStatus = MutableStateFlow(BluetoothStatus.IDLE)
    val connectionStatus: StateFlow<BluetoothStatus> = _connectionStatus.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availableDevices: StateFlow<List<BluetoothDevice>> = _availableDevices.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    // Activity Result Launchers (set by Activity)
    private var requestBluetoothEnable: ActivityResultLauncher<Intent>? = null
    private var requestMultiplePermissions: ActivityResultLauncher<Array<String>>? = null

    fun setActivityResultLaunchers(
        enableLauncher: ActivityResultLauncher<Intent>,
        permissionLauncher: ActivityResultLauncher<Array<String>>
    ) {
        requestBluetoothEnable = enableLauncher
        requestMultiplePermissions = permissionLauncher
    }

    // --- Permission and Bluetooth State Handling ---

    fun initializeBluetooth() {
        Log.d(TAG, "initializeBluetooth: Initializing Bluetooth...")
        _errorMessage.value = null // Clear previous errors
        if (_bluetoothAdapter == null) {
            _errorMessage.value = "Device does not support Bluetooth"
            _connectionStatus.value = BluetoothStatus.ERROR
            Log.e(TAG, "initializeBluetooth: Device does not support Bluetooth.")
            return
        }
        if (!hasRequiredPermissions()) {
            Log.d(TAG, "initializeBluetooth: Requesting permissions.")
            requestPermissions()
        } else if (!_bluetoothAdapter!!.isEnabled) {
            Log.d(TAG, "initializeBluetooth: Requesting Bluetooth enable.")
            requestEnableBluetooth()
        } else {
            Log.d(TAG, "initializeBluetooth: Permissions granted and Bluetooth enabled, scanning for paired devices.")
            scanForPairedDevices()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val context = getApplication<Application>().applicationContext
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION // Needed for discovery pre-S
            )
        }
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        Log.d(TAG, "hasRequiredPermissions: All required permissions granted: $allGranted")
        return allGranted
    }

    private fun requestPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) // Only location needs runtime request pre-S
        }
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(getApplication(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "requestPermissions: Requesting permissions: ${permissionsToRequest.joinToString()}")
            requestMultiplePermissions?.launch(permissionsToRequest)
        } else {
            Log.d(TAG, "requestPermissions: All necessary runtime permissions already granted.")
            // If permissions were already granted but BT wasn't enabled, trigger enable request
            if (_bluetoothAdapter?.isEnabled == false) {
                requestEnableBluetooth()
            } else {
                scanForPairedDevices() // Permissions OK, BT OK, start scan
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestEnableBluetooth() {
        Log.d(TAG, "requestEnableBluetooth: Requesting Bluetooth enable.")
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            _errorMessage.value = "BLUETOOTH_CONNECT permission required to enable Bluetooth."
            Log.e(TAG, "requestEnableBluetooth: BLUETOOTH_CONNECT permission missing.")
            requestPermissions() // Request missing connect permission
            return
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        try {
            requestBluetoothEnable?.launch(enableBtIntent)
        } catch (e: SecurityException) {
            Log.e(TAG, "requestEnableBluetooth: SecurityException requesting BT enable", e)
            _errorMessage.value = "Security error when requesting Bluetooth enable"
            _connectionStatus.value = BluetoothStatus.ERROR
        }
    }

    // --- Handler Callbacks from Activity ---

    fun handleBluetoothEnableResult(resultCode: Int) {
        if (resultCode == android.app.Activity.RESULT_OK) {
            Log.d(TAG, "handleBluetoothEnableResult: Bluetooth enabled by user.")
            scanForPairedDevices() // Bluetooth enabled, now scan
        } else {
            Log.d(TAG, "handleBluetoothEnableResult: User denied Bluetooth enable request.")
            _errorMessage.value = "Bluetooth must be enabled to use the app"
            _connectionStatus.value = BluetoothStatus.ERROR
        }
    }

    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        if (permissions.all { it.value }) {
            Log.d(TAG, "handlePermissionResult: All required Bluetooth permissions granted.")
            // Permissions granted, check if Bluetooth is enabled
            initializeBluetooth()
        } else {
            Log.e(TAG, "handlePermissionResult: One or more Bluetooth permissions were denied.")
            _errorMessage.value = "Bluetooth permissions are required for the app to function"
            _connectionStatus.value = BluetoothStatus.ERROR
        }
    }

    // --- Device Scanning ---

    @SuppressLint("MissingPermission")
    fun scanForPairedDevices() {
        Log.d(TAG, "scanForPairedDevices: Scanning for paired devices...")
        if (!checkAdapterAndPermissions()) {
            Log.w(TAG, "scanForPairedDevices: Adapter or permissions check failed.")
            return
        }

        _connectionStatus.value = BluetoothStatus.SCANNING
        _availableDevices.value = emptyList() // Clear previous list
        var foundDevices: List<BluetoothDevice> = emptyList()
        try {
            foundDevices = _bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            if (foundDevices.isEmpty()) {
                Log.d(TAG, "scanForPairedDevices: No paired devices found.")
                _errorMessage.value = "No paired devices found. Please pair an HC-05 device first in Bluetooth settings."
            } else {
                Log.d(TAG, "scanForPairedDevices: Found ${foundDevices.size} paired devices.")
                _errorMessage.value = null // Clear error if devices are found
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "scanForPairedDevices: SecurityException during scan", e)
            _errorMessage.value = "Permission error scanning for devices"
            requestPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "scanForPairedDevices: Unexpected error during scan", e)
            _errorMessage.value = "An unexpected error occurred during device scan."
        }
        _availableDevices.value = foundDevices
        _connectionStatus.value = BluetoothStatus.IDLE // Return to idle after scan
    }

    // --- Connection Logic ---

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice: Attempting to connect to ${getDeviceName(device)}")
        if (!checkAdapterAndPermissions()) {
            Log.w(TAG, "connectToDevice: Adapter or permissions check failed.")
            return
        }
        if (_connectionStatus.value == BluetoothStatus.CONNECTING || _connectionStatus.value == BluetoothStatus.CONNECTED) {
            Log.w(TAG, "connectToDevice: Already connecting or connected.")
            return
        }

        connectJob?.cancel() // Cancel any previous connection attempt
        disconnect() // Ensure previous connection is closed

        _connectionStatus.value = BluetoothStatus.CONNECTING
        _isConnected.value = false
        _errorMessage.value = null
        _connectedDeviceName.value = getDeviceName(device) // Show name during connection

        connectJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "connectToDevice: Creating RFCOMM socket for ${getDeviceName(device)}")
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                // Cancel discovery before connecting (requires permissions)
                if (hasPermission(Manifest.permission.BLUETOOTH_SCAN) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    _bluetoothAdapter?.cancelDiscovery()
                    Log.d(TAG, "connectToDevice: Bluetooth discovery cancelled.")
                } else {
                    Log.w(TAG, "connectToDevice: Missing BLUETOOTH_SCAN permission, cannot cancel discovery.")
                }

                Log.d(TAG, "connectToDevice: Connecting to socket...")
                bluetoothSocket?.connect() // Blocking call
                outputStream = bluetoothSocket?.outputStream

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "connectToDevice: Connection successful to ${getDeviceName(device)}")
                    _connectionStatus.value = BluetoothStatus.CONNECTED
                    _isConnected.value = true
                    _connectedDeviceName.value = getDeviceName(device)
                }

            } catch (e: IOException) {
                val errorMsg = when {
                    e.message?.contains("read failed, socket might closed") == true -> "Socket closed unexpectedly."
                    e.message?.contains("connection timed out") == true -> "Connection timed out."
                    e.message?.contains("Software caused connection abort") == true -> "Connection aborted by software."
                    else -> "Generic IOException: ${e.message}"
                }
                Log.e(TAG, "connectToDevice: IOException during connection attempt: $errorMsg", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Connection failed to ${getDeviceName(device)}. $errorMsg Make sure the device is powered on and in range."
                    disconnect() // Ensure cleanup on error
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "connectToDevice: SecurityException during connection attempt", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Bluetooth connection permission error"
                    disconnect()
                    requestPermissions()
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.i(TAG, "connectToDevice: Connection attempt cancelled.")
                    // Don't show error if cancelled intentionally
                    withContext(Dispatchers.Main) { disconnect() }
                } else {
                    Log.e(TAG, "connectToDevice: Unexpected error during connection attempt", e)
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "An unexpected error occurred during connection."
                        disconnect()
                    }
                }
            }
        }
    }

    // --- Disconnection Logic ---

    fun disconnect() {
        Log.d(TAG, "disconnect: Disconnecting Bluetooth...")
        connectJob?.cancel() // Cancel ongoing connection attempt if any
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d(TAG, "disconnect: Bluetooth socket closed.")
        } catch (e: IOException) {
            Log.e(TAG, "disconnect: Error closing Bluetooth socket", e)
        } finally {
            outputStream = null
            bluetoothSocket = null
            if (_connectionStatus.value != BluetoothStatus.ERROR) { // Don't overwrite error state
                _connectionStatus.value = BluetoothStatus.IDLE
            }
            _isConnected.value = false
            _connectedDeviceName.value = null
            Log.d(TAG, "disconnect: Bluetooth state reset.")
        }
    }

    // --- Send Command Logic ---
    fun write(command: String) {
        Log.d(TAG, "write: Attempting to send command: $command.")
        if (_connectionStatus.value != BluetoothStatus.CONNECTED || outputStream == null) {
            _errorMessage.value = "Not connected to a device. Please connect first."
            Log.w(TAG, "write: Command $command. called but not connected.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(command.toByteArray())
                outputStream?.flush()
                Log.d(TAG, "write: Successfully sent command: $command.")
            } catch (e: IOException) {
                Log.e(TAG, "write: Error sending command: $command., due to IOException", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error sending command. Try reconnecting."
                    disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "write: Unexpected error sending command: $command., due to ${e.javaClass.simpleName}", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "An unexpected error occurred while sending command."
                    disconnect()
                }
            }
        }
    }


    // --- Helper Functions ---
    @SuppressLint("MissingPermission")
    private fun checkAdapterAndPermissions(): Boolean {
        Log.d(TAG, "checkAdapterAndPermissions: Performing checks...")
        if (_bluetoothAdapter == null) {
            _errorMessage.value = "Device does not support Bluetooth"
            _connectionStatus.value = BluetoothStatus.ERROR
            Log.e(TAG, "checkAdapterAndPermissions: Bluetooth adapter is null.")
            return false
        }
        if (!hasRequiredPermissions()) {
            _errorMessage.value = "Bluetooth permissions required"
            requestPermissions()
            Log.w(TAG, "checkAdapterAndPermissions: Required permissions not granted.")
            return false
        }
        if (!_bluetoothAdapter!!.isEnabled) {
            _errorMessage.value = "Bluetooth must be enabled"
            requestEnableBluetooth()
            Log.w(TAG, "checkAdapterAndPermissions: Bluetooth is not enabled.")
            return false
        }
        _errorMessage.value = null // Clear error if checks pass
        Log.d(TAG, "checkAdapterAndPermissions: All checks passed.")
        return true
    }

    private fun hasPermission(permission: String): Boolean {
        val granted = ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "hasPermission: Permission $permission granted: $granted")
        return granted
    }

    @SuppressLint("MissingPermission")
    fun getDeviceName(device: BluetoothDevice?): String {
        if (device == null) {
            Log.w(TAG, "getDeviceName: Device is null, returning 'Unknown Device'.")
            return "Unknown Device"
        }
        return try {
            if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val name = device.name ?: device.address
                Log.d(TAG, "getDeviceName: Device name: $name")
                name
            } else {
                Log.w(TAG, "getDeviceName: Missing BLUETOOTH_CONNECT permission on S+, falling back to address.")
                device.address // Fallback to address if connect permission missing on S+
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "getDeviceName: SecurityException getting device name for ${device.address}", e)
            device.address // Fallback to address on security exception
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect() // Disconnect when ViewModel is cleared
        viewModelScope.cancel() // Cancel coroutines
        Log.d(TAG, "onCleared: BluetoothViewModel cleared.")
    }
}



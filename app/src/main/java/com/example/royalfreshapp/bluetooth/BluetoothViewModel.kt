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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.royalfreshapp.utils.SPP_UUID
import com.example.royalfreshapp.utils.TAG
import kotlinx.coroutines.*
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
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var connectJob: Job? = null

    // LiveData for UI state
    private val _connectionStatus = MutableLiveData(BluetoothStatus.IDLE)
    val connectionStatus: LiveData<BluetoothStatus> = _connectionStatus

    private val _availableDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val availableDevices: LiveData<List<BluetoothDevice>> = _availableDevices

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _connectedDeviceName = MutableLiveData<String?>(null)
    val connectedDeviceName: LiveData<String?> = _connectedDeviceName

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
        _errorMessage.value = null // Clear previous errors
        if (_bluetoothAdapter == null) {
            _errorMessage.value = "Device does not support Bluetooth"
            _connectionStatus.value = BluetoothStatus.ERROR
            return
        }
        if (!hasRequiredPermissions()) {
            requestPermissions()
        } else if (!_bluetoothAdapter!!.isEnabled) {
            requestEnableBluetooth()
        } else {
            // Permissions granted and Bluetooth enabled, start scanning for paired devices
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
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
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
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            requestMultiplePermissions?.launch(permissionsToRequest)
        } else {
             Log.d(TAG, "All necessary runtime permissions already granted.")
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
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            _errorMessage.value = "BLUETOOTH_CONNECT permission required to enable Bluetooth."
            requestPermissions() // Request missing connect permission
            return
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        try {
             requestBluetoothEnable?.launch(enableBtIntent)
        } catch (e: SecurityException) {
             Log.e(TAG, "SecurityException requesting BT enable", e)
             _errorMessage.value = "Security error when requesting Bluetooth enable"
             _connectionStatus.value = BluetoothStatus.ERROR
        }
    }

    // --- Handler Callbacks from Activity ---

    fun handleBluetoothEnableResult(resultCode: Int) {
        if (resultCode == android.app.Activity.RESULT_OK) {
            Log.d(TAG, "Bluetooth enabled by user.")
            scanForPairedDevices() // Bluetooth enabled, now scan
        } else {
            Log.d(TAG, "User denied Bluetooth enable request.")
            _errorMessage.value = "Bluetooth must be enabled to use the app"
            _connectionStatus.value = BluetoothStatus.ERROR
        }
    }

    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        if (permissions.all { it.value }) {
            Log.d(TAG, "All required Bluetooth permissions granted.")
            // Permissions granted, check if Bluetooth is enabled
            initializeBluetooth()
        } else {
            Log.e(TAG, "One or more Bluetooth permissions were denied.")
            _errorMessage.value = "Bluetooth permissions are required for the app to function"
            _connectionStatus.value = BluetoothStatus.ERROR
        }
    }

    // --- Device Scanning ---

    @SuppressLint("MissingPermission")
    fun scanForPairedDevices() {
        if (!checkAdapterAndPermissions()) return

        Log.d(TAG, "Scanning for paired devices...")
        _connectionStatus.value = BluetoothStatus.SCANNING
        _availableDevices.value = emptyList() // Clear previous list
        var foundDevices: List<BluetoothDevice> = emptyList()
        try {
            foundDevices = _bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            if (foundDevices.isEmpty()) {
                Log.d(TAG, "No paired devices found.")
                _errorMessage.value = "No paired devices found. Please pair an HC-05 device first in Bluetooth settings."
            } else {
                 Log.d(TAG, "Found ${foundDevices.size} paired devices.")
                 _errorMessage.value = null // Clear error if devices are found
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during scan", e)
            _errorMessage.value = "Permission error scanning for devices"
            requestPermissions()
        }
        _availableDevices.value = foundDevices
        _connectionStatus.value = BluetoothStatus.IDLE // Return to idle after scan
    }

    // --- Connection Logic ---

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (!checkAdapterAndPermissions()) return
        if (_connectionStatus.value == BluetoothStatus.CONNECTING || _connectionStatus.value == BluetoothStatus.CONNECTED) {
            Log.w(TAG, "Already connecting or connected.")
            return
        }

        connectJob?.cancel() // Cancel any previous connection attempt
        disconnect() // Ensure previous connection is closed

        _connectionStatus.value = BluetoothStatus.CONNECTING
        _errorMessage.value = null
        _connectedDeviceName.value = getDeviceName(device) // Show name during connection

        connectJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to connect to ${getDeviceName(device)}")
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                // Cancel discovery before connecting (requires permissions)
                if (hasPermission(Manifest.permission.BLUETOOTH_SCAN) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    _bluetoothAdapter?.cancelDiscovery()
                } else {
                     Log.w(TAG, "Missing BLUETOOTH_SCAN permission, cannot cancel discovery.")
                }

                bluetoothSocket?.connect() // Blocking call
                outputStream = bluetoothSocket?.outputStream

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Connection successful to ${getDeviceName(device)}")
                    _connectionStatus.value = BluetoothStatus.CONNECTED
                    _connectedDeviceName.value = getDeviceName(device)
                }

            } catch (e: IOException) {
                if (e.message?.contains("read failed, socket might closed") == true || e.message?.contains("connection timed out") == true || e.message?.contains("Software caused connection abort") == true) {
                     Log.w(TAG, "Connection failed (Socket closed, timed out or aborted): ${e.message}")
                } else {
                     Log.e(TAG, "IOException during connection attempt", e)
                }
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Connection failed to ${getDeviceName(device)}. Make sure the device is powered on and in range."
                    disconnect() // Ensure cleanup on error
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during connection attempt", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Bluetooth connection permission error"
                    disconnect()
                    requestPermissions()
                }
            } catch (e: Exception) {
                 if (e is CancellationException) {
                     Log.i(TAG, "Connection attempt cancelled.")
                     // Don't show error if cancelled intentionally
                     withContext(Dispatchers.Main) { disconnect() }
                 } else {
                     Log.e(TAG, "Unexpected error during connection attempt", e)
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
        connectJob?.cancel() // Cancel ongoing connection attempt if any
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d(TAG, "Bluetooth socket closed.")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing Bluetooth socket", e)
        } finally {
            outputStream = null
            bluetoothSocket = null
            if (_connectionStatus.value != BluetoothStatus.ERROR) { // Don't overwrite error state
                 _connectionStatus.postValue(BluetoothStatus.IDLE)
            }
            _connectedDeviceName.postValue(null)
        }
    }

    // --- Send Command Logic ---
    fun sendCommand(command: String) {
        if (_connectionStatus.value != BluetoothStatus.CONNECTED || outputStream == null) {
            _errorMessage.value = "Not connected to a device. Please connect first."
            Log.w(TAG, "SendCommand called but not connected.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(command.toByteArray())
                outputStream?.flush()
                Log.d(TAG, "Sent command: $command")
            } catch (e: IOException) {
                Log.e(TAG, "Error sending command: $command", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error sending command. Try reconnecting."
                    disconnect()
                }
            }
        }
    }

    // --- Helper Functions ---
    @SuppressLint("MissingPermission")
    private fun checkAdapterAndPermissions(): Boolean {
        if (_bluetoothAdapter == null) {
            _errorMessage.value = "Device does not support Bluetooth"
            _connectionStatus.value = BluetoothStatus.ERROR
            return false
        }
        if (!hasRequiredPermissions()) {
            _errorMessage.value = "Bluetooth permissions required"
            requestPermissions()
            return false
        }
        if (!_bluetoothAdapter!!.isEnabled) {
            _errorMessage.value = "Bluetooth must be enabled"
            requestEnableBluetooth()
            return false
        }
        _errorMessage.value = null // Clear error if checks pass
        return true
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getDeviceName(device: BluetoothDevice?): String {
        if (device == null) return "Unknown Device"
        return try {
             if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                 device.name ?: device.address
             } else {
                 device.address // Fallback to address if connect permission missing on S+
             }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting device name for ${device.address}", e)
            device.address // Fallback to address on security exception
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect() // Disconnect when ViewModel is cleared
        viewModelScope.cancel() // Cancel coroutines
        Log.d(TAG, "BluetoothViewModel cleared.")
    }
}

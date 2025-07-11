package com.example.royalfreshapp.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.royalfreshapp.RoyalFreshTheme
import com.example.royalfreshapp.bluetooth.BluetoothViewModel

import com.example.royalfreshapp.utils.TAG
import kotlin.math.abs
import kotlin.math.roundToInt

// Data class to represent a schedule item
data class ScheduleItem(
    val id: Long = 0,
    val timeRange: String,
    val frequency: String,
    val deviceId: String,
    val grade: String, // Added grade
    val isOn: Boolean = false // Default to OFF as requested
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    scheduleItems: List<ScheduleItem>,
    bluetoothViewModel: BluetoothViewModel,
    onAddScheduleClick: () -> Unit,
    onEditSchedule: (ScheduleItem) -> Unit,
    onDeleteSchedule: (ScheduleItem) -> Unit,
    onToggleChange: (ScheduleItem, Boolean) -> Unit
) {
    val isBluetoothConnected by bluetoothViewModel.isConnected.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Schedule",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Bluetooth connection indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isBluetoothConnected) Color.Green else Color.Red)
                            .border(1.dp, Color.White, CircleShape)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE91E63), // Pink color from the image
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddScheduleClick,
                containerColor = Color(0xFFE91E63), // Pink color from the image
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Schedule"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Device Timing",
                fontSize = 20.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (scheduleItems.isEmpty()) {
                // Show empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No schedules yet. Tap + to add one.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(scheduleItems) { item ->
                        CustomSwipeableCard(
                            scheduleItem = item,
                            onToggleChange = {
                                if (isBluetoothConnected) {
                                    if (item.isOn) {
                                        // If ON, send 'B' to turn off
                                        bluetoothViewModel.write("B")
                                        Toast.makeText(context, "Device turned OFF", Toast.LENGTH_SHORT).show()
                                        onToggleChange(item, false) // Update UI to OFF
                                        Log.d(TAG, "Command 'B' sent for schedule item ${item.id} (turned OFF)")
                                    } else {
                                        // If OFF, check if any other card is ON
                                        val isAnyOtherCardOn = scheduleItems.any { it.id != item.id && it.isOn }
                                        if (isAnyOtherCardOn) {
                                            Toast.makeText(context, "Another timer is already active. Please turn it off first.", Toast.LENGTH_LONG).show()
                                            Log.d(TAG, "Attempted to turn ON schedule item ${item.id}, but another card is already ON.")
                                        } else {
                                            // If OFF and no other card is ON, send full card data
                                            val dataToSend = "${item.timeRange}|${item.frequency}|${item.deviceId}|${item.grade}"
                                            bluetoothViewModel.write(dataToSend)
                                            // Assuming write is successful, update UI and show success message
                                            Toast.makeText(context, "Data sent successfully!", Toast.LENGTH_SHORT).show()
                                            onToggleChange(item, true) // Update UI to ON
                                            Log.d(TAG, "Full data '$dataToSend' sent for schedule item ${item.id} (turned ON)")
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Bluetooth not connected", Toast.LENGTH_SHORT).show()
                                    Log.w(TAG, "Attempted to send command but Bluetooth not connected.")
                                }
                            },
                            onEditSchedule = { onEditSchedule(item) }, // Re-enabled edit functionality
                            onDeleteSchedule = { onDeleteSchedule(item) }
                        )
                    }
                    // Add some space at the bottom
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSwipeableCard(
    scheduleItem: ScheduleItem,
    onToggleChange: (Boolean) -> Unit,
    onEditSchedule: () -> Unit, // Re-added edit functionality
    onDeleteSchedule: () -> Unit
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Thresholds for triggering actions
    val deleteThreshold = -150f // Negative for left swipe
    val editThreshold = 150f // Positive for right swipe

    // Animation for background alpha based on swipe distance
    val deleteBackgroundAlpha by animateFloatAsState(
        targetValue = (-offsetX / deleteThreshold).coerceIn(0f, 1f)
    )

    val editBackgroundAlpha by animateFloatAsState(
        targetValue = (offsetX / editThreshold).coerceIn(0f, 1f)
    )

    // Dialog for delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; offsetX = 0f },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this schedule?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteSchedule()
                    showDeleteDialog = false
                    Log.d(TAG, "Schedule item ${scheduleItem.id} confirmed for deletion.")
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false; offsetX = 0f }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        // Delete background (appears when swiping left)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(deleteBackgroundAlpha)
                .background(Color(0xFFF44336)) // Red for delete
                .padding(horizontal = 20.dp)
                .zIndex(1f), // Ensure background is behind the card but visible
            contentAlignment = Alignment.CenterEnd // Align to end for left swipe
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White
            )
        }

        // Edit background (appears when swiping right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(editBackgroundAlpha)
                .background(Color(0xFF2196F3)) // Blue for edit
                .padding(horizontal = 20.dp)
                .zIndex(1f), // Ensure background is behind the card but visible
            contentAlignment = Alignment.CenterStart // Align to start for right swipe
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = Color.White
            )
        }

        // Card content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // Allow both left and right swipe
                        offsetX += delta
                    },
                    onDragStopped = {
                        when {
                            // Swiped left beyond threshold - Show delete dialog
                            offsetX < deleteThreshold -> {
                                showDeleteDialog = true
                                Log.d(TAG, "Schedule item ${scheduleItem.id} swiped for deletion, showing dialog.")
                            }
                            // Swiped right beyond threshold - Trigger edit action
                            offsetX > editThreshold -> {
                                onEditSchedule()
                                offsetX = 0f // Reset position after action
                                Log.d(TAG, "Schedule item ${scheduleItem.id} swiped for editing.")
                            }
                            // Not beyond threshold - Reset position
                            else -> {
                                offsetX = 0f
                                Log.d(TAG, "Swipe on schedule item ${scheduleItem.id} reset.")
                            }
                        }
                    }
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = scheduleItem.timeRange,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = scheduleItem.frequency,
                            fontSize = 16.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                        Text(
                            text = scheduleItem.deviceId,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom toggle switch that looks like the one in the image
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (scheduleItem.isOn) Color.Green else Color.Red)
                                .border(
                                    width = 2.dp,
                                    color = if (scheduleItem.isOn) Color.Green else Color.Red,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    // Only allow toggle if Bluetooth is connected
                                    onToggleChange(scheduleItem.isOn) // Pass current state to trigger inverse
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (scheduleItem.isOn) "ON" else "OFF", // Use scheduleItem.isOn directly
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- Preview ---
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun ScheduleScreenPreview() {
    RoyalFreshTheme {
        ScheduleScreen(
            navController = rememberNavController(),
            scheduleItems = listOf(
                ScheduleItem(
                    id = 1,
                    timeRange = "08:00 - 10:00",
                    frequency = "Daily",
                    deviceId = "Device123",
                    grade = "A",
                    isOn = false
                )
            ),
            bluetoothViewModel = BluetoothViewModel(application = LocalContext.current.applicationContext as Application),
            onAddScheduleClick = {},
            onEditSchedule = {},
            onDeleteSchedule = {},
            onToggleChange = { _, _ -> }
        )
    }
}





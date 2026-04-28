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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.royalfreshapp.R
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
    val grade: String,
    val isOn: Boolean = false
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

    // Pre-read string resources for use in Toast (non-composable scope)
    val deviceTurnedOffStr = stringResource(R.string.device_turned_off)
    val anotherTimerActiveStr = stringResource(R.string.another_timer_active)
    val dataSentSuccessfullyStr = stringResource(R.string.data_sent_successfully)
    val bluetoothNotConnectedStr = stringResource(R.string.bluetooth_not_connected)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.schedule),
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
                            contentDescription = stringResource(R.string.back)
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
                    containerColor = Color(0xFFE91E63),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddScheduleClick,
                containerColor = Color(0xFFE91E63),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_schedule)
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
                text = stringResource(R.string.device_timing),
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
                        text = stringResource(R.string.no_schedules_yet),
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
                                        Toast.makeText(context, deviceTurnedOffStr, Toast.LENGTH_SHORT).show()
                                        onToggleChange(item, false)
                                        Log.d(TAG, "Command 'B' sent for schedule item ${item.id} (turned OFF)")
                                    } else {
                                        // If OFF, check if any other card is ON
                                        val isAnyOtherCardOn = scheduleItems.any { it.id != item.id && it.isOn }
                                        if (isAnyOtherCardOn) {
                                            Toast.makeText(context, anotherTimerActiveStr, Toast.LENGTH_LONG).show()
                                            Log.d(TAG, "Attempted to turn ON schedule item ${item.id}, but another card is already ON.")
                                        } else {
                                            // If OFF and no other card is ON, send full card data
                                            val dataToSend = "${item.timeRange}|${item.frequency}|${item.deviceId}|${item.grade}"
                                            bluetoothViewModel.write(dataToSend)
                                            Toast.makeText(context, dataSentSuccessfullyStr, Toast.LENGTH_SHORT).show()
                                            onToggleChange(item, true)
                                            Log.d(TAG, "Full data '$dataToSend' sent for schedule item ${item.id} (turned ON)")
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, bluetoothNotConnectedStr, Toast.LENGTH_SHORT).show()
                                    Log.w(TAG, "Attempted to send command but Bluetooth not connected.")
                                }
                            },
                            onEditSchedule = { onEditSchedule(item) },
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
    onEditSchedule: () -> Unit,
    onDeleteSchedule: () -> Unit
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Thresholds for triggering actions
    val deleteThreshold = -150f
    val editThreshold = 150f

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
            title = { Text(stringResource(R.string.confirm_deletion)) },
            text = { Text(stringResource(R.string.confirm_deletion_message)) },
            confirmButton = {
                Button(onClick = {
                    onDeleteSchedule()
                    showDeleteDialog = false
                    Log.d(TAG, "Schedule item ${scheduleItem.id} confirmed for deletion.")
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false; offsetX = 0f }) {
                    Text(stringResource(R.string.cancel))
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
                .background(Color.Red)
                .padding(horizontal = 20.dp)
                .zIndex(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete_icon),
                tint = Color.White
            )
        }

        // Edit background (appears when swiping right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(editBackgroundAlpha)
                .background(Color(0xFF2196F3))
                .padding(horizontal = 20.dp)
                .zIndex(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_icon),
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
                        offsetX += delta
                    },
                    onDragStopped = {
                        when {
                            offsetX < deleteThreshold -> {
                                showDeleteDialog = true
                                Log.d(TAG, "Schedule item ${scheduleItem.id} swiped for deletion, showing dialog.")
                            }
                            offsetX > editThreshold -> {
                                onEditSchedule()
                                offsetX = 0f
                                Log.d(TAG, "Schedule item ${scheduleItem.id} swiped for editing.")
                            }
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
                        // Custom toggle switch
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
                                    onToggleChange(scheduleItem.isOn)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (scheduleItem.isOn) stringResource(R.string.on_label) else stringResource(R.string.off_label),
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

package com.example.royalfreshapp.ui.screens

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.abs
import kotlin.math.roundToInt

// Data class to represent a schedule item
data class ScheduleItem(
    val id: Long = 0,
    val timeRange: String,
    val frequency: String,
    val deviceId: String,
    val isOn: Boolean = false // Default to OFF as requested
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    scheduleItems: List<ScheduleItem>,
    onAddScheduleClick: () -> Unit,
    onEditSchedule: (ScheduleItem) -> Unit,
    onDeleteSchedule: (ScheduleItem) -> Unit,
    onToggleChange: (ScheduleItem, Boolean) -> Unit
) {
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
                            onToggleChange = { isOn -> onToggleChange(item, isOn) },
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
    var isDismissed by remember { mutableStateOf(false) }

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

    // If card is dismissed, don't show it
    if (isDismissed) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        // Delete background (appears when swiping right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(deleteBackgroundAlpha)
                .background(Color(0xFFF44336)) // Red for delete
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White
            )
        }

        // Edit background (appears when swiping left)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(editBackgroundAlpha)
                .background(Color(0xFF2196F3)) // Blue for edit
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd
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
                        // Update offset with drag delta
                        offsetX += delta
                    },
                    onDragStopped = {
                        when {
                            // Swiped right beyond threshold - Delete
                            offsetX > editThreshold -> {
                                // Trigger delete action
                                onDeleteSchedule()
                                isDismissed = true
                            }
                            // Swiped left beyond threshold - Edit
                            offsetX < deleteThreshold -> {
                                // Trigger edit action
                                onEditSchedule()
                                // Reset position
                                offsetX = 0f
                            }
                            // Not beyond threshold - Reset position
                            else -> {
                                offsetX = 0f
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
                        val isChecked = remember { mutableStateOf(scheduleItem.isOn) }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFFE91E63),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    isChecked.value = !isChecked.value
                                    onToggleChange(isChecked.value)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isChecked.value) "ON" else "OFF",
                                color = Color(0xFFE91E63),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

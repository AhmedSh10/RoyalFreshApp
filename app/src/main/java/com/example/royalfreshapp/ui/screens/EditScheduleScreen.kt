package com.example.royalfreshapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.app.TimePickerDialog
import com.example.royalfreshapp.utils.DaySelectionButton
import com.example.royalfreshapp.utils.GradeSelectionItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    navController: NavController,
    onSaveSchedule: (ScheduleItem) -> Unit,
    scheduleToEdit: ScheduleItem
) {
    // State for start and end times
    var startTime by remember { mutableStateOf(scheduleToEdit.timeRange.split("-")[0].trim()) }
    var endTime by remember { mutableStateOf(scheduleToEdit.timeRange.split("-")[1].trim()) }

    // Context for TimePickerDialog
    val context = LocalContext.current

    // Calendar instances for time pickers
    val startCalendar = remember { Calendar.getInstance().apply { time = SimpleDateFormat("h:mm a", Locale.getDefault()).parse(startTime) ?: time } }
    val endCalendar = remember { Calendar.getInstance().apply { time = SimpleDateFormat("h:mm a", Locale.getDefault()).parse(endTime) ?: time } }

    // Time format
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Time picker for start time
    val startTimePicker = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            startCalendar.set(Calendar.MINUTE, minute)
            startTime = timeFormat.format(startCalendar.time)
        },
        startCalendar.get(Calendar.HOUR_OF_DAY),
        startCalendar.get(Calendar.MINUTE),
        false
    )

    // Time picker for end time
    val endTimePicker = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            endCalendar.set(Calendar.MINUTE, minute)
            endTime = timeFormat.format(endCalendar.time)
        },
        endCalendar.get(Calendar.HOUR_OF_DAY),
        endCalendar.get(Calendar.MINUTE),
        false
    )

    // State for selected days
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            val frequency = scheduleToEdit.frequency
            if (frequency == "Every day") {
                addAll(days)
            } else {
                addAll(frequency.split(", "))
            }
        }
    }

    // State for selected grade
    var selectedGrade by remember { mutableStateOf(scheduleToEdit.grade) }

    // Validation state
    val isStartTimeSelected = startTime.isNotEmpty()
    val isEndTimeSelected = endTime.isNotEmpty()
    val isDaySelected = selectedDays.isNotEmpty()
    val isGradeSelected = selectedGrade.isNotEmpty()

    // Combined validation state
    val isFormValid = isStartTimeSelected && isEndTimeSelected && isDaySelected && isGradeSelected

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Edit Schedule",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time selection section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start Time
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { startTimePicker.show() }
                ) {
                    Text(
                        text = "Start Time",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = startTime,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // End Time
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { endTimePicker.show() }
                ) {
                    Text(
                        text = "End Time",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = endTime,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            // Days selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    DaySelectionButton(
                        day = day,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedDays.remove(day)
                            } else {
                                selectedDays.add(day)
                            }
                        }
                    )
                }
            }

            // Placeholder for the clock (to be ignored as per instructions)
            Spacer(modifier = Modifier.height(200.dp))

            // Grade selection
            Text(
                text = "Grade",
                fontSize = 20.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 16.dp)
            )

            // Grade options
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items((1..10).map { "G$it" }) { grade ->
                    GradeSelectionItem(
                        grade = grade,
                        isSelected = selectedGrade == grade,
                        onClick = { selectedGrade = grade }
                    )
                }
            }

            // Save button
            Button(
                onClick = {
                    // Create an updated schedule item with the selected values
                    val timeRange = "$startTime-$endTime"
                    val frequency = if (selectedDays.size == 7) "Every day" else selectedDays.joinToString(", ")
                    val updatedSchedule = scheduleToEdit.copy(
                        timeRange = timeRange,
                        frequency = frequency,
                        deviceId = selectedGrade,
                        grade = selectedGrade,
                        isOn = if (scheduleToEdit.isOn) false else scheduleToEdit.isOn // Turn off if it was on
                    )
                    onSaveSchedule(updatedSchedule)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE91E63),
                    disabledContainerColor = Color.Gray
                ),
                enabled = isFormValid
            ) {
                Text(
                    text = "Save Changes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

//@Composable
//fun DaySelectionButton(
//    day: String,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .clip(RoundedCornerShape(50))
//            .background(
//                if (isSelected) Color(0xFFE91E63).copy(alpha = 0.1f)
//                else Color(0xFFF5F5F5)
//            )
//            .border(
//                width = if (isSelected) 1.dp else 0.dp,
//                color = if (isSelected) Color(0xFFE91E63) else Color.Transparent,
//                shape = RoundedCornerShape(50)
//            )
//            .clickable(onClick = onClick)
//            .padding(horizontal = 8.dp, vertical = 12.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = day,
//            color = if (isSelected) Color(0xFFE91E63) else Color.Black,
//            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
//        )
//    }
//}
//
//@Composable
//fun GradeSelectionItem(
//    grade: String,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick)
//            .padding(vertical = 12.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = grade,
//            fontSize = 24.sp,
//            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
//            color = if (isSelected) Color(0xFFE91E63) else Color.Gray
//        )
//    }
//}



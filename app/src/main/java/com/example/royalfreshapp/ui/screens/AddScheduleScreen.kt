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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.app.TimePickerDialog
import com.example.royalfreshapp.R
import com.example.royalfreshapp.utils.DaySelectionButton
import com.example.royalfreshapp.utils.GradeSelectionItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleScreen(
    navController: NavController,
    onSaveSchedule: (ScheduleItem) -> Unit,
    scheduleToEdit: ScheduleItem? = null
) {
    // Day keys (used for data storage - always English)
    val dayKeys = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Day display labels (localized)
    val dayLabels = listOf(
        stringResource(R.string.day_sun),
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat)
    )

    val everyDayStr = stringResource(R.string.every_day)

    // State for start and end times
    var startTime by remember { mutableStateOf(scheduleToEdit?.timeRange?.split("-")?.get(0)?.trim() ?: "5:45 PM") }
    var endTime by remember { mutableStateOf(scheduleToEdit?.timeRange?.split("-")?.get(1)?.trim() ?: "6:30 PM") }

    // Context for TimePickerDialog
    val context = LocalContext.current

    // Calendar instances for time pickers
    val startCalendar = remember { Calendar.getInstance() }
    val endCalendar = remember { Calendar.getInstance() }

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

    // State for selected days (store day keys for data)
    val selectedDayKeys = remember {
        mutableStateListOf<String>().apply {
            if (scheduleToEdit != null) {
                val frequency = scheduleToEdit.frequency
                if (frequency == "Every day") {
                    addAll(dayKeys)
                } else {
                    addAll(frequency.split(", "))
                }
            }
        }
    }

    // State for selected grade
    var selectedGrade by remember { mutableStateOf(scheduleToEdit?.grade ?: "") }

    // Validation state
    val isStartTimeSelected = startTime.isNotEmpty()
    val isEndTimeSelected = endTime.isNotEmpty()
    val isDaySelected = selectedDayKeys.isNotEmpty()
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
                            text = stringResource(R.string.schedule),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE91E63),
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
                        text = stringResource(R.string.start_time),
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
                        text = stringResource(R.string.end_time),
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
                dayKeys.forEachIndexed { index, dayKey ->
                    val isSelected = selectedDayKeys.contains(dayKey)
                    DaySelectionButton(
                        day = dayLabels[index],
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedDayKeys.remove(dayKey)
                            } else {
                                selectedDayKeys.add(dayKey)
                            }
                        }
                    )
                }
            }

            // Placeholder for the clock
            Spacer(modifier = Modifier.height(200.dp))

            // Grade selection
            Text(
                text = stringResource(R.string.grade),
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
                    val timeRange = "$startTime-$endTime"
                    val frequency = if (selectedDayKeys.size == 7) "Every day" else selectedDayKeys.joinToString(", ")
                    val newSchedule = ScheduleItem(
                        timeRange = timeRange,
                        frequency = frequency,
                        deviceId = selectedGrade,
                        grade = selectedGrade,
                        isOn = scheduleToEdit?.isOn ?: false
                    )
                    onSaveSchedule(newSchedule)
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
                    text = stringResource(R.string.save),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

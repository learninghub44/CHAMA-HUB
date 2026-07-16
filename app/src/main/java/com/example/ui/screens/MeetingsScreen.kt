package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Attendance
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.CrimsonRisk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    meetings: List<Meeting>,
    members: List<Member>,
    onScheduleMeeting: (title: String, date: String, time: String, agenda: String) -> Unit,
    onSaveAttendance: (meetingId: Int, attendances: List<Attendance>) -> Unit,
    onDeleteMeeting: ((Meeting) -> Unit)? = null,
    selectedMeeting: Meeting?,
    onSelectMeeting: (Meeting?) -> Unit,
    onBack: () -> Unit
) {
    var isScheduling by remember { mutableStateOf(false) }

    // Schedule form states
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var agenda by remember { mutableStateOf("") }

    // Attendance tracker states
    var isTrackingAttendance by remember { mutableStateOf(false) }
    val attendanceStates = remember { mutableStateMapOf<Int, String>() } // memberId to status

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("meetings_screen")
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Top Bar
            CenterAlignedTopAppBar(
                title = { Text("Chama Session Calendar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isScheduling = true }) {
                        Icon(imageVector = Icons.Default.AddBox, contentDescription = "Schedule Session", tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            // Calendar Meetings listing
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (meetings.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No sessions scheduled yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    items(meetings) { meeting ->
                        MeetingSessionCard(
                            meeting = meeting,
                            onAttendanceClick = {
                                onSelectMeeting(meeting)
                                // Initialize attendance states with "Present"
                                members.forEach { attendanceStates[it.id] = "Present" }
                                isTrackingAttendance = true
                            },
                            onDelete = if (onDeleteMeeting != null) { { onDeleteMeeting(meeting) } } else null
                        )
                    }
                }
            }
        }

        // 1. Schedule Session Dialog Form
        if (isScheduling) {
            AlertDialog(
                onDismissRequest = { isScheduling = false },
                title = { Text("Schedule Meeting Session") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Session Title / Topic") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("meeting_title_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = time,
                                onValueChange = { time = it },
                                label = { Text("Time (HH:MM)") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = agenda,
                            onValueChange = { agenda = it },
                            label = { Text("Meeting Agenda Objectives") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && date.isNotBlank()) {
                                onScheduleMeeting(title, date, time, agenda)
                                title = ""
                                date = ""
                                time = ""
                                agenda = ""
                                isScheduling = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Schedule")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isScheduling = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 2. Attendance Tracker Overlay Sheet
        if (isTrackingAttendance && selectedMeeting != null) {
            AlertDialog(
                onDismissRequest = { 
                    isTrackingAttendance = false
                    onSelectMeeting(null)
                },
                title = { Text("Register Attendance Matrix") },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(members) { m ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(m.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val currentStatus = attendanceStates[m.id] ?: "Present"
                                        
                                        // Present chip
                                        FilterChip(
                                            selected = currentStatus == "Present",
                                            onClick = { attendanceStates[m.id] = "Present" },
                                            label = { Text("P", fontSize = 11.sp) }
                                        )
                                        // Late chip
                                        FilterChip(
                                            selected = currentStatus == "Late",
                                            onClick = { attendanceStates[m.id] = "Late" },
                                            label = { Text("L", fontSize = 11.sp) }
                                        )
                                        // Absent chip
                                        FilterChip(
                                            selected = currentStatus == "Absent",
                                            onClick = { attendanceStates[m.id] = "Absent" },
                                            label = { Text("A", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val list = members.map {
                                Attendance(
                                    meetingId = selectedMeeting.id,
                                    memberId = it.id,
                                    status = attendanceStates[it.id] ?: "Present"
                                )
                            }
                            onSaveAttendance(selectedMeeting.id, list)
                            isTrackingAttendance = false
                            onSelectMeeting(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Save Matrix")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        isTrackingAttendance = false
                        onSelectMeeting(null)
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun MeetingSessionCard(meeting: Meeting, onAttendanceClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Event, contentDescription = null, tint = EmeraldGreen)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(meeting.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${meeting.date} at ${meeting.time}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAttendanceClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Attendance", fontSize = 11.sp)
                    }
                    
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Session",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (meeting.agenda.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Agenda objectives:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = meeting.agenda,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ItineraryActivity
import com.example.ui.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    viewModel: TripViewModel
) {
    val activeTrip by viewModel.selectedTrip.collectAsState()
    val allTrips by viewModel.trips.collectAsState()
    val itineraries by viewModel.itineraries.collectAsState()
    val activities by viewModel.activities.collectAsState()

    var selectedViewMode by remember { mutableStateOf("Daily") } // Daily, Weekly, Timeline
    var selectedDayNumber by remember { mutableIntStateOf(1) }
    var showAddActivityDialog by remember { mutableStateOf(false) }
    var showAddDayDialog by remember { mutableStateOf(false) }

    if (activeTrip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select or create a trip first to view its itinerary.", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddActivityDialog = true },
                modifier = Modifier.testTag("add_activity_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Trip Header Selector
            TripSelectorHeader(
                activeTrip = activeTrip!!,
                allTrips = allTrips,
                onTripSelect = { viewModel.selectTrip(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // View Mode Segmented Buttons (Daily, Weekly, Timeline)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf("Daily", "Weekly", "Timeline")
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selectedViewMode == mode,
                        onClick = { selectedViewMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                    ) {
                        Text(mode)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day Selector Row (in Daily / Weekly view)
            if (selectedViewMode == "Daily") {
                val daysCount = maxOf(1, itineraries.size)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items((1..daysCount).toList()) { day ->
                            FilterChip(
                                selected = selectedDayNumber == day,
                                onClick = { selectedDayNumber = day },
                                label = { Text("Day $day") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    IconButton(onClick = { showAddDayDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Day")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Filter activities based on view mode
            val displayedActivities = when (selectedViewMode) {
                "Daily" -> {
                    // Match activities for selected day
                    activities.filter {
                        val itItem = itineraries.find { it.dayNumber == selectedDayNumber }
                        itItem == null || it.itineraryId == itItem.itineraryId || it.itineraryId.contains("day_$selectedDayNumber")
                    }
                }
                "Weekly" -> activities
                else -> activities // Timeline view shows all
            }

            if (displayedActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No activities scheduled for this view.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showAddActivityDialog = true }) {
                            Text("Add First Activity")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(displayedActivities, key = { it.activityId }) { act ->
                        ActivityTimelineCard(
                            activity = act,
                            onToggleComplete = { viewModel.toggleActivityCompleted(act) },
                            onDelete = { viewModel.deleteActivity(act.activityId) }
                        )
                    }
                }
            }
        }
    }

    if (showAddActivityDialog) {
        AddActivityDialog(
            tripId = activeTrip!!.tripId,
            currentDay = selectedDayNumber,
            onDismiss = { showAddActivityDialog = false },
            onAdd = { title, desc, loc, addr, start, end, cost, cur, status, notes ->
                viewModel.addActivity(
                    tripId = activeTrip!!.tripId,
                    itineraryId = "day_$selectedDayNumber",
                    title = title,
                    description = desc,
                    location = loc,
                    address = addr,
                    startTime = start,
                    endTime = end,
                    estimatedCost = cost,
                    currency = cur,
                    status = status,
                    notes = notes
                )
                showAddActivityDialog = false
            }
        )
    }

    if (showAddDayDialog) {
        var dayTitle by remember { mutableStateOf("Explore New Spots") }
        var dayDate by remember { mutableStateOf("2026-11-21") }

        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = { Text("Add Itinerary Day") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayTitle,
                        onValueChange = { dayTitle = it },
                        label = { Text("Day Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dayDate,
                        onValueChange = { dayDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addItineraryDay(
                        activeTrip!!.tripId,
                        itineraries.size + 1,
                        dayDate,
                        dayTitle
                    )
                    selectedDayNumber = itineraries.size + 1
                    showAddDayDialog = false
                }) {
                    Text("Add Day")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TripSelectorHeader(
    activeTrip: com.example.data.model.Trip,
    allTrips: List<com.example.data.model.Trip>,
    onTripSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Selected Trip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = activeTrip.tripName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch Trip")

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allTrips.forEach { trip ->
                    DropdownMenuItem(
                        text = { Text(trip.tripName) },
                        onClick = {
                            onTripSelect(trip.tripId)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityTimelineCard(
    activity: ItineraryActivity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = activity.activityStatus == "Completed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Time / Dot marker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(54.dp)
            ) {
                Text(
                    text = activity.startTime.ifEmpty { "Any" },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { onToggleComplete() },
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (activity.location.isNotBlank()) {
                    Text(
                        text = "📍 ${activity.location}${if (activity.address.isNotBlank()) " (${activity.address})" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (activity.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (activity.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📝 ${activity.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activity.estimatedCost > 0) {
                        Text(
                            text = "${activity.currency} ${String.format(java.util.Locale.US, "%.0f", activity.estimatedCost)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Free / Included",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddActivityDialog(
    tripId: String,
    currentDay: Int,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        desc: String,
        loc: String,
        addr: String,
        start: String,
        end: String,
        cost: Double,
        cur: String,
        status: String,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("12:00") }
    var costInput by remember { mutableStateOf("0") }
    var currency by remember { mutableStateOf("USD") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Activity (Day $currentDay)") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Activity Title *") },
                        modifier = Modifier.fillMaxWidth().testTag("activity_title_input"),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Venue") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Navigation Info") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it },
                            label = { Text("Estimated Cost") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase() },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Confirmation / Booking Reference") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedCost = costInput.toDoubleOrNull() ?: 0.0
                        onAdd(title, description, location, address, startTime, endTime, parsedCost, currency, "Planned", notes)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_activity_button")
            ) {
                Text("Add Activity")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.TravelConstants
import com.example.data.model.Trip
import com.example.ui.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    viewModel: TripViewModel,
    onTripSelected: (String) -> Unit
) {
    val trips by viewModel.trips.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }

    val filteredTrips = trips.filter { trip ->
        val matchesSearch = trip.tripName.contains(searchQuery, ignoreCase = true) ||
                trip.destinations.any { it.contains(searchQuery, ignoreCase = true) }
        val matchesStatus = if (statusFilter == "All") true else trip.status.equals(statusFilter, ignoreCase = true)
        matchesSearch && matchesStatus
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Trip") },
                text = { Text("Plan Trip") },
                modifier = Modifier.testTag("plan_trip_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Search destination, trip name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trip_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            val filterOptions = listOf("All") + TravelConstants.TRIP_STATUSES
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterOptions) { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { viewModel.setStatusFilter(filter) },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (trips.isEmpty()) "No trips found. Plan your first adventure!" else "No matching trips found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Text("Create New Trip")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTrips, key = { it.tripId }) { trip ->
                        TripCardDetailed(
                            trip = trip,
                            onClick = {
                                viewModel.selectTrip(trip.tripId)
                                onTripSelected(trip.tripId)
                            },
                            onEdit = { tripToEdit = trip },
                            onDuplicate = { viewModel.duplicateTrip(trip) },
                            onDelete = { tripToDelete = trip }
                        )
                    }
                }
            }
        }
    }

    // Create / Edit Dialog
    if (showCreateDialog || tripToEdit != null) {
        TripFormDialog(
            initialTrip = tripToEdit,
            onDismiss = {
                showCreateDialog = false
                tripToEdit = null
            },
            onSave = { name, desc, destinations, start, end, type, status, currency, budget ->
                if (tripToEdit != null) {
                    viewModel.updateTrip(
                        tripToEdit!!.copy(
                            tripName = name,
                            description = desc,
                            destinations = destinations,
                            startDate = start,
                            endDate = end,
                            tripType = type,
                            status = status,
                            currency = currency,
                            estimatedBudget = budget
                        )
                    )
                    tripToEdit = null
                } else {
                    viewModel.createTrip(name, desc, destinations, start, end, type, status, currency, budget)
                    showCreateDialog = false
                }
            }
        )
    }

    // Delete Confirmation
    if (tripToDelete != null) {
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text("Delete Trip") },
            text = { Text("Are you sure you want to delete '${tripToDelete!!.tripName}'? All associated itineraries, expenses, and journals will also be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTrip(tripToDelete!!.tripId)
                        tripToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { tripToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TripCardDetailed(
    trip: Trip,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("trip_card_${trip.tripId}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(trip.status) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${trip.tripType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Trip Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Trip") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Trip") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Trip", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Text(
                text = trip.tripName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (trip.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trip.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📅 ${trip.startDate} - ${trip.endDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (trip.destinations.isNotEmpty()) {
                        Text(
                            text = "📍 ${trip.destinations.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (trip.estimatedBudget > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${trip.currency} ${String.format(java.util.Locale.US, "%.0f", trip.estimatedBudget)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripFormDialog(
    initialTrip: Trip?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        desc: String,
        destinations: List<String>,
        start: String,
        end: String,
        type: String,
        status: String,
        currency: String,
        budget: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialTrip?.tripName ?: "") }
    var description by remember { mutableStateOf(initialTrip?.description ?: "") }
    var destinationsInput by remember { mutableStateOf(initialTrip?.destinations?.joinToString(", ") ?: "") }
    var startDate by remember { mutableStateOf(initialTrip?.startDate ?: "2026-11-20") }
    var endDate by remember { mutableStateOf(initialTrip?.endDate ?: "2026-11-27") }
    var selectedType by remember { mutableStateOf(initialTrip?.tripType ?: "Solo") }
    var selectedStatus by remember { mutableStateOf(initialTrip?.status ?: "Planning") }
    var selectedCurrency by remember { mutableStateOf(initialTrip?.currency ?: "USD") }
    var budgetInput by remember { mutableStateOf(if ((initialTrip?.estimatedBudget ?: 0.0) > 0) initialTrip!!.estimatedBudget.toString() else "1500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTrip == null) "Plan New Trip" else "Edit Trip") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Trip Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("trip_name_input"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = destinationsInput,
                        onValueChange = { destinationsInput = it },
                        label = { Text("Destinations (comma-separated)") },
                        placeholder = { Text("e.g. Paris, Rome, Barcelona") },
                        modifier = Modifier.fillMaxWidth().testTag("destinations_input"),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Departure (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("Return (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { budgetInput = it },
                            label = { Text("Est. Budget") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = { selectedCurrency = it.uppercase() },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Text("Trip Type", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TravelConstants.TRIP_TYPES) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
                item {
                    Text("Trip Status", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TravelConstants.TRIP_STATUSES) { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(status) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val destList = destinationsInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        val parsedBudget = budgetInput.toDoubleOrNull() ?: 0.0
                        onSave(name, description, destList, startDate, endDate, selectedType, selectedStatus, selectedCurrency, parsedBudget)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_trip_button")
            ) {
                Text("Save Trip")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

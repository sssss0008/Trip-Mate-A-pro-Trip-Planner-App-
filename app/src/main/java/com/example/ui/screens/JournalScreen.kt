package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JournalEntry
import com.example.ui.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: TripViewModel
) {
    val activeTrip by viewModel.selectedTrip.collectAsState()
    val allTrips by viewModel.trips.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()

    var showAddEntryDialog by remember { mutableStateOf(false) }
    var selectedTagFilter by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }

    if (activeTrip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select or create a trip to record your travel memories.", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    val availableTags = listOf("All", "Adventure", "Food", "Nature", "Culture", "Family")
    val filteredEntries = journalEntries.filter { entry ->
        val matchesTag = if (selectedTagFilter == "All") true else entry.tags.contains(selectedTagFilter)
        val matchesFav = if (favoritesOnly) entry.isFavorite else true
        matchesTag && matchesFav
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddEntryDialog = true },
                icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                text = { Text("Write Memory") },
                modifier = Modifier.testTag("add_journal_entry_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            TripSelectorHeader(
                activeTrip = activeTrip!!,
                allTrips = allTrips,
                onTripSelect = { viewModel.selectTrip(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tag Filters & Favorites Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(availableTags) { tag ->
                        FilterChip(
                            selected = selectedTagFilter == tag,
                            onClick = { selectedTagFilter = tag },
                            label = { Text(tag) }
                        )
                    }
                }
                IconButton(onClick = { favoritesOnly = !favoritesOnly }) {
                    Icon(
                        imageVector = if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorites",
                        tint = if (favoritesOnly) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No journal memories yet for this filter.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showAddEntryDialog = true }) {
                            Text("Create Memory")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredEntries, key = { it.entryId }) { entry ->
                        JournalEntryCard(
                            entry = entry,
                            onToggleFavorite = { viewModel.toggleJournalFavorite(entry) },
                            onDelete = { viewModel.deleteJournalEntry(entry.entryId) }
                        )
                    }
                }
            }
        }
    }

    if (showAddEntryDialog) {
        AddJournalDialog(
            trip = activeTrip!!,
            onDismiss = { showAddEntryDialog = false },
            onAdd = { title, content, location, rating, tags, isPrivate, date ->
                viewModel.addJournalEntry(
                    tripId = activeTrip!!.tripId,
                    title = title,
                    content = content,
                    location = location,
                    rating = rating,
                    tags = tags,
                    isPrivate = isPrivate,
                    date = date
                )
                showAddEntryDialog = false
            }
        )
    }
}

@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
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
                        label = { Text(if (entry.visibility == "PRIVATE") "🔒 Private" else "🌐 Shared") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⭐ ${entry.rating.toInt()}/5",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFEAB308)
                    )
                }

                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (entry.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (entry.location.isNotBlank()) {
                Text(
                    text = "📍 ${entry.location} • 📅 ${entry.entryDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (entry.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddJournalDialog(
    trip: com.example.data.model.Trip,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        content: String,
        location: String,
        rating: Double,
        tags: List<String>,
        isPrivate: Boolean,
        date: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(trip.destinations.firstOrNull() ?: "") }
    var rating by remember { mutableDoubleStateOf(5.0) }
    var isPrivate by remember { mutableStateOf(false) }
    val allTags = listOf("Adventure", "Food", "Nature", "Culture", "Family")
    val selectedTags = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write Travel Memory") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title *") },
                        modifier = Modifier.fillMaxWidth().testTag("journal_title_input"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text("Rating: ${rating.toInt()} Stars", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = rating.toFloat(),
                        onValueChange = { rating = it.toDouble() },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
                item {
                    Text("Tags", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allTags) { tag ->
                            FilterChip(
                                selected = selectedTags.contains(tag),
                                onClick = {
                                    if (selectedTags.contains(tag)) selectedTags.remove(tag)
                                    else selectedTags.add(tag)
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                        Text("Keep Private (only you can see this entry)")
                    }
                }
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Experience & Thoughts *") },
                        modifier = Modifier.fillMaxWidth().testTag("journal_content_input"),
                        minLines = 4
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onAdd(title, content, location, rating, selectedTags.toList(), isPrivate, "")
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier.testTag("save_journal_button")
            ) {
                Text("Save Memory")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

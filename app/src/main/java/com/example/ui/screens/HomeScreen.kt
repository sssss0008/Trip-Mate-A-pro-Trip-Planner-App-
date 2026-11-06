package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TravelAllDetailsEntity
import com.example.data.model.*
import com.example.ui.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class InformationFilter(val label: String, val icon: ImageVector) {
    ALL("All Information", Icons.Default.AllInclusive),
    TRIPS("Trips & Stops", Icons.Default.Luggage),
    ACTIVITIES("Itinerary", Icons.Default.Schedule),
    EXPENSES("Budgets & Costs", Icons.Default.AccountBalanceWallet),
    PACKING("Packing Items", Icons.Default.Checklist),
    JOURNAL("Memories", Icons.Default.Book),
    ROOM_DB("Room DB Records", Icons.Default.Storage)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TripViewModel,
    onNavigateToTrips: () -> Unit,
    onNavigateToItinerary: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToPacking: () -> Unit,
    onCreateTripClick: () -> Unit
) {
    val trips by viewModel.trips.collectAsState()
    val activeTrip by viewModel.selectedTrip.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val packingItems by viewModel.packingItems.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val localRoomDetails by viewModel.localTravelDetails.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(InformationFilter.ALL) }
    var isRoomSyncing by remember { mutableStateOf(false) }

    // Aggregate statistics across loaded data
    val totalActualExpenses = expenses.sumOf { it.amount }
    val totalEstimatedBudget = trips.sumOf { it.estimatedBudget }
    val packedCount = packingItems.count { it.isPacked }
    val totalPackingCount = packingItems.size
    val packingPercent = if (totalPackingCount > 0) (packedCount.toFloat() / totalPackingCount * 100).toInt() else 0
    val completedActivitiesCount = activities.count { it.activityStatus == "Completed" }

    // Filter items based on searchQuery
    val query = searchQuery.trim().lowercase(Locale.ROOT)

    val filteredTrips = trips.filter { trip ->
        query.isEmpty() ||
                trip.tripName.lowercase(Locale.ROOT).contains(query) ||
                trip.description.lowercase(Locale.ROOT).contains(query) ||
                trip.destinations.any { it.lowercase(Locale.ROOT).contains(query) } ||
                trip.tripType.lowercase(Locale.ROOT).contains(query) ||
                trip.status.lowercase(Locale.ROOT).contains(query)
    }

    val filteredActivities = activities.filter { act ->
        query.isEmpty() ||
                act.title.lowercase(Locale.ROOT).contains(query) ||
                act.location.lowercase(Locale.ROOT).contains(query) ||
                act.description.lowercase(Locale.ROOT).contains(query) ||
                act.notes.lowercase(Locale.ROOT).contains(query)
    }

    val filteredExpenses = expenses.filter { exp ->
        query.isEmpty() ||
                exp.title.lowercase(Locale.ROOT).contains(query) ||
                exp.category.lowercase(Locale.ROOT).contains(query) ||
                exp.paidBy.lowercase(Locale.ROOT).contains(query) ||
                exp.description.lowercase(Locale.ROOT).contains(query)
    }

    val filteredPacking = packingItems.filter { item ->
        query.isEmpty() ||
                item.itemName.lowercase(Locale.ROOT).contains(query) ||
                item.category.lowercase(Locale.ROOT).contains(query) ||
                item.notes.lowercase(Locale.ROOT).contains(query)
    }

    val filteredJournals = journalEntries.filter { entry ->
        query.isEmpty() ||
                entry.title.lowercase(Locale.ROOT).contains(query) ||
                entry.content.lowercase(Locale.ROOT).contains(query) ||
                entry.location.lowercase(Locale.ROOT).contains(query) ||
                entry.tags.any { it.lowercase(Locale.ROOT).contains(query) }
    }

    val filteredRoomRecords = localRoomDetails.filter { record ->
        query.isEmpty() ||
                record.tripName.lowercase(Locale.ROOT).contains(query) ||
                record.destinations.lowercase(Locale.ROOT).contains(query) ||
                record.activitiesSummary.lowercase(Locale.ROOT).contains(query) ||
                record.expensesSummary.lowercase(Locale.ROOT).contains(query) ||
                record.packingSummary.lowercase(Locale.ROOT).contains(query) ||
                record.journalSummary.lowercase(Locale.ROOT).contains(query)
    }

    val totalInformationCount = filteredTrips.size +
            filteredActivities.size +
            filteredExpenses.size +
            filteredPacking.size +
            filteredJournals.size +
            filteredRoomRecords.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Master Header Banner ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TravelExplore,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TripMate",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                                Text(
                                    text = "Plan Together. Travel Better.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                )
                            }

                            // Quick Trips Overview Shortcut
                            IconButton(
                                onClick = onNavigateToTrips,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("explore_trips_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "View Trips",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Active Trip Spotlight Card
                        if (activeTrip != null) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToItinerary() }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Active Trip: ${activeTrip!!.tripName}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        AssistChip(
                                            onClick = { onNavigateToTrips() },
                                            label = { Text(activeTrip!!.status) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📅 ${activeTrip!!.startDate} - ${activeTrip!!.endDate} • ${activeTrip!!.tripType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (activeTrip!!.destinations.isNotEmpty()) {
                                        Text(
                                            text = "📍 ${activeTrip!!.destinations.joinToString(" → ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Room SQLite Storage Banner & Trigger ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Room Database Entity Storage",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Entity: TravelAllDetailsEntity (${localRoomDetails.size} record${if (localRoomDetails.size != 1) "s" else ""} in SQLite)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isRoomSyncing = true
                            viewModel.syncAllTravelDetailsToRoom {
                                isRoomSyncing = false
                            }
                        },
                        modifier = Modifier.testTag("sync_room_details_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRoomSyncing) "Saving..." else "Store All in Room",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // --- 3. Quick Action Shortcuts ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Quick Travel Shortcuts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = Icons.Default.AddLocationAlt,
                        label = "New Trip",
                        tag = "quick_new_trip",
                        onClick = onCreateTripClick
                    )
                    QuickActionButton(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        label = "Expense",
                        tag = "quick_expense",
                        onClick = onNavigateToExpenses
                    )
                    QuickActionButton(
                        icon = Icons.Default.Luggage,
                        label = "Packing",
                        tag = "quick_packing",
                        onClick = onNavigateToPacking
                    )
                    QuickActionButton(
                        icon = Icons.Default.Book,
                        label = "Journal",
                        tag = "quick_journal",
                        onClick = onNavigateToJournal
                    )
                }
            }
        }

        // --- 4. Executive KPI Overview Metrics ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Executive Travel Summary",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Trips",
                        value = "${trips.size}",
                        subtitle = "${trips.flatMap { it.destinations }.distinct().size} stops",
                        icon = Icons.Default.Luggage,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    StatCard(
                        modifier = Modifier.weight(1.3f),
                        title = "Spending",
                        value = "$${String.format(Locale.US, "%.0f", totalActualExpenses)}",
                        subtitle = "of $${String.format(Locale.US, "%.0f", totalEstimatedBudget)} est.",
                        icon = Icons.Default.AccountBalanceWallet,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Packed",
                        value = "$packingPercent%",
                        subtitle = "$packedCount/$totalPackingCount items",
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Activities",
                        value = "${activities.size}",
                        subtitle = "$completedActivitiesCount completed",
                        icon = Icons.Default.Schedule,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Memories",
                        value = "${journalEntries.size}",
                        subtitle = "${journalEntries.count { it.isFavorite }} favorited",
                        icon = Icons.Default.PhotoLibrary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Room SQLite",
                        value = "${localRoomDetails.size}",
                        subtitle = "Cached details",
                        icon = Icons.Default.Storage,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // --- 5. Real-Time Search Bar ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_search_input"),
                    placeholder = { Text("Search all details, destinations, activities, expenses...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
        }

        // --- 6. Information Category Filter Tabs ---
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("information_filter_row"),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(InformationFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        InformationFilter.ALL -> totalInformationCount
                        InformationFilter.TRIPS -> filteredTrips.size
                        InformationFilter.ACTIVITIES -> filteredActivities.size
                        InformationFilter.EXPENSES -> filteredExpenses.size
                        InformationFilter.PACKING -> filteredPacking.size
                        InformationFilter.JOURNAL -> filteredJournals.size
                        InformationFilter.ROOM_DB -> filteredRoomRecords.size
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.label} ($count)") },
                        leadingIcon = {
                            Icon(
                                imageVector = filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // --- 7. List of ALL THE THINGS OF THE INFORMATION ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Master Details Feed (${
                        when (selectedFilter) {
                            InformationFilter.ALL -> totalInformationCount
                            InformationFilter.TRIPS -> filteredTrips.size
                            InformationFilter.ACTIVITIES -> filteredActivities.size
                            InformationFilter.EXPENSES -> filteredExpenses.size
                            InformationFilter.PACKING -> filteredPacking.size
                            InformationFilter.JOURNAL -> filteredJournals.size
                            InformationFilter.ROOM_DB -> filteredRoomRecords.size
                        }
                    })",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (trips.isEmpty()) {
                    TextButton(onClick = { viewModel.seedSampleTripIfEmpty() }) {
                        Text("Seed Sample Data")
                    }
                }
            }
        }

        // Empty state
        if (totalInformationCount == 0 && trips.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Travel Information Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create your trip plan or load pre-built sample itinerary with full budgets, gear, and memories to store in Room DB.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onCreateTripClick,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create Trip")
                            }
                            OutlinedButton(
                                onClick = { viewModel.seedSampleTripIfEmpty() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Load Sample Itinerary")
                            }
                        }
                    }
                }
            }
        }

        // Section A: Trips & Destinations
        if (selectedFilter == InformationFilter.ALL || selectedFilter == InformationFilter.TRIPS) {
            if (filteredTrips.isNotEmpty()) {
                item {
                    Text(
                        text = "✈️ Trips & Destinations (${filteredTrips.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(filteredTrips) { trip ->
                    TripDetailInfoCard(
                        trip = trip,
                        isActive = trip.tripId == activeTrip?.tripId,
                        onSelect = {
                            viewModel.selectTrip(trip.tripId)
                            onNavigateToItinerary()
                        },
                        onNavigate = onNavigateToTrips
                    )
                }
            }
        }

        // Section B: Itinerary Activities
        if (selectedFilter == InformationFilter.ALL || selectedFilter == InformationFilter.ACTIVITIES) {
            if (filteredActivities.isNotEmpty()) {
                item {
                    Text(
                        text = "🗓️ Itinerary Activities (${filteredActivities.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(filteredActivities) { activity ->
                    ActivityDetailInfoCard(
                        activity = activity,
                        onToggle = { viewModel.toggleActivityCompleted(activity) }
                    )
                }
            }
        }

        // Section C: Budgets & Expenses
        if (selectedFilter == InformationFilter.ALL || selectedFilter == InformationFilter.EXPENSES) {
            if (filteredExpenses.isNotEmpty()) {
                item {
                    Text(
                        text = "💰 Expenses & Budget Log (${filteredExpenses.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(filteredExpenses) { expense ->
                    ExpenseDetailInfoCard(
                        expense = expense,
                        onClick = onNavigateToExpenses
                    )
                }
            }
        }

        // Section D: Packing Checklist Items
        if (selectedFilter == InformationFilter.ALL || selectedFilter == InformationFilter.PACKING) {
            if (filteredPacking.isNotEmpty()) {
                item {
                    Text(
                        text = "🎒 Packing Checklist Items (${filteredPacking.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(filteredPacking) { item ->
                    PackingDetailInfoCard(
                        item = item,
                        onToggle = { viewModel.togglePackingItem(item.itemId, item.isPacked) }
                    )
                }
            }
        }

        // Section E: Travel Journal Memories
        if (selectedFilter == InformationFilter.ALL || selectedFilter == InformationFilter.JOURNAL) {
            if (filteredJournals.isNotEmpty()) {
                item {
                    Text(
                        text = "📖 Travel Journal Memories (${filteredJournals.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(filteredJournals) { journal ->
                    JournalDetailInfoCard(
                        entry = journal,
                        onToggleFavorite = { viewModel.toggleJournalFavorite(journal) },
                        onClick = onNavigateToJournal
                    )
                }
            }
        }

        // Section F: Room SQLite Stored TravelAllDetailsEntity Records
        if (selectedFilter == InformationFilter.ALL || selectedFilter == InformationFilter.ROOM_DB) {
            if (filteredRoomRecords.isNotEmpty()) {
                item {
                    Text(
                        text = "💾 Room SQLite Consolidated Records (${filteredRoomRecords.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(filteredRoomRecords) { record ->
                    RoomAllDetailsRecordCard(record = record)
                }
            } else if (selectedFilter == InformationFilter.ROOM_DB) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Room Records Cached Yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Store All in Room' above to serialize all trips, expenses, itineraries, and packing lists into the Room SQLite database.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.syncAllTravelDetailsToRoom() }) {
                                Text("Store All in Room Database")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- DETAIL CARD COMPOSABLES -----------------

@Composable
fun TripDetailInfoCard(
    trip: Trip,
    isActive: Boolean,
    onSelect: () -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect)
            .testTag("trip_detail_card_${trip.tripId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trip.tripName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("Active", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
                AssistChip(
                    onClick = onNavigate,
                    label = { Text(trip.status) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📍 Destinations: ${if (trip.destinations.isNotEmpty()) trip.destinations.joinToString(" → ") else "To be planned"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "📅 ${trip.startDate} to ${trip.endDate} • ${trip.tripType} Travel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (trip.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trip.description,
                    style = MaterialTheme.typography.bodySmall,
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
                Text(
                    text = "Budget: $${String.format(Locale.US, "%.0f", trip.estimatedBudget)} ${trip.currency}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Tap to open itinerary →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ActivityDetailInfoCard(
    activity: ItineraryActivity,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("activity_detail_card_${activity.activityId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = activity.activityStatus == "Completed",
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("activity_check_${activity.activityId}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (activity.activityStatus == "Completed") TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (activity.activityStatus == "Completed") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "🕒 ${activity.startTime.ifEmpty { "Flexible" }} - ${activity.endTime.ifEmpty { "" }} • 📍 ${activity.location.ifEmpty { "General Location" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (activity.notes.isNotEmpty()) {
                    Text(
                        text = "📝 ${activity.notes}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (activity.estimatedCost > 0) {
                Text(
                    text = "$${String.format(Locale.US, "%.0f", activity.estimatedCost)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ExpenseDetailInfoCard(
    expense: Expense,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("expense_detail_card_${expense.expenseId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "🏷️ ${expense.category} • Paid by ${expense.paidBy} • 📅 ${expense.expenseDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "$${String.format(Locale.US, "%.2f", expense.amount)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PackingDetailInfoCard(
    item: PackingItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("packing_detail_card_${item.itemId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = item.isPacked,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("packing_check_${item.itemId}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (item.isPacked) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.isPacked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Category: ${item.category} • Qty: ${item.quantity}${if (item.notes.isNotEmpty()) " • ${item.notes}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AssistChip(
                onClick = onToggle,
                label = { Text(if (item.isPacked) "Packed" else "To Pack") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (item.isPacked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun JournalDetailInfoCard(
    entry: JournalEntry,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("journal_detail_card_${entry.entryId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "📍 ${entry.location} • 📅 ${entry.entryDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (entry.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (entry.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    repeat(entry.rating.toInt().coerceIn(1, 5)) {
                        Text("★", color = Color(0xFFFFB800), fontSize = 14.sp)
                    }
                }
                if (entry.tags.isNotEmpty()) {
                    Text(
                        text = entry.tags.joinToString(", ") { "#$it" },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Direct visualization of the consolidated record stored in Room SQLite database
 * via TravelAllDetailsEntity.
 */
@Composable
fun RoomAllDetailsRecordCard(record: TravelAllDetailsEntity) {
    var expanded by remember { mutableStateOf(false) }
    val formattedTime = remember(record.lastUpdatedEpoch) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(record.lastUpdatedEpoch))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { expanded = !expanded }
            .animateContentSize()
            .testTag("room_record_card_${record.tripId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.tripName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                AssistChip(
                    onClick = { expanded = !expanded },
                    label = { Text(if (expanded) "Collapse" else "Expand SQLite") }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📍 Destinations: ${record.destinations}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = "📅 ${record.startDate} - ${record.endDate} • Status: ${record.status} • Type: ${record.tripType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Budget: $${String.format(Locale.US, "%.0f", record.estimatedBudget)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Spent: $${String.format(Locale.US, "%.0f", record.totalActualSpent)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (record.budgetStatus == "OVER_BUDGET") Color.Red else MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Remaining: $${String.format(Locale.US, "%.0f", record.remainingBudget)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Consolidated Room Information Breakdown:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    DetailItemRow("Itinerary Days", record.itineraryDaysSummary)
                    DetailItemRow("Activities Log", record.activitiesSummary)
                    DetailItemRow("Expenses Summary", record.expensesSummary)
                    DetailItemRow("Packing Checklist", record.packingSummary)
                    DetailItemRow("Journal Memories", record.journalSummary)
                    DetailItemRow("Travel Companions", record.companions)
                    DetailItemRow("Accommodations", record.hotelReservationNotes)
                    DetailItemRow("Transportation", record.transportationNotes)
                    DetailItemRow("Database Table", "travel_all_details (SQLite)")
                    DetailItemRow("Cached Timestamp", formattedTime)
                }
            }
        }
    }
}

@Composable
fun DetailItemRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifEmpty { "N/A" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

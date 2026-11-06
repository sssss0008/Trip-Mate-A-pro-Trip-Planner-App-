package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TravelAllDetailsEntity
import com.example.data.model.*
import com.example.data.repository.TripRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TripViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TripRepository(application)
    val currentUserId: String? get() = Firebase.auth.currentUser?.uid

    // Navigation and Active Trip Selection
    private val _selectedTripId = MutableStateFlow<String?>(null)
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("All")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Real-time Trips flow
    val trips: StateFlow<List<Trip>> = repository.observeUserTrips()
        .catch { error ->
            _errorMessage.value = error.localizedMessage ?: "Failed to load trips"
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val selectedTrip: StateFlow<Trip?> = combine(trips, _selectedTripId) { tripList, activeId ->
        if (activeId != null) tripList.find { it.tripId == activeId }
        else tripList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    // Current Trip Itineraries
    val itineraries: StateFlow<List<Itinerary>> = _selectedTripId
        .filterNotNull()
        .flatMapLatest { tripId -> repository.observeTripItineraries(tripId) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Current Trip Activities
    val activities: StateFlow<List<ItineraryActivity>> = _selectedTripId
        .filterNotNull()
        .flatMapLatest { tripId -> repository.observeTripActivities(tripId) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Current Trip Budgets
    val budgets: StateFlow<List<Budget>> = _selectedTripId
        .filterNotNull()
        .flatMapLatest { tripId -> repository.observeTripBudgets(tripId) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Current Trip Expenses
    val expenses: StateFlow<List<Expense>> = _selectedTripId
        .filterNotNull()
        .flatMapLatest { tripId -> repository.observeTripExpenses(tripId) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Current Trip Settlements
    val settlements: StateFlow<List<Settlement>> = _selectedTripId
        .filterNotNull()
        .flatMapLatest { tripId -> repository.observeTripSettlements(tripId) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Current Trip Journal Entries
    val journalEntries: StateFlow<List<JournalEntry>> = _selectedTripId
        .filterNotNull()
        .flatMapLatest { tripId -> repository.observeTripJournal(tripId) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Current Trip Packing Items
    val packingItems: StateFlow<List<PackingItem>> = _selectedTripId
        .filterNotNull()
        .flatMapLatest { tripId -> repository.observeTripPackingItems(tripId) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Consolidated All Details stored in local Room SQLite database
    val localTravelDetails: StateFlow<List<TravelAllDetailsEntity>> = repository.observeLocalAllTravelDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun selectTrip(tripId: String) {
        _selectedTripId.value = tripId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // --- Trip Operations ---
    fun createTrip(
        name: String,
        description: String,
        destinations: List<String>,
        startDate: String,
        endDate: String,
        tripType: String,
        status: String,
        currency: String,
        budget: Double
    ) {
        viewModelScope.launch {
            val trip = Trip(
                tripName = name,
                description = description,
                destinations = destinations,
                startDate = startDate,
                endDate = endDate,
                tripType = tripType,
                status = status,
                currency = currency,
                estimatedBudget = budget
            )
            val result = repository.createTrip(trip)
            if (result.isSuccess) {
                _selectedTripId.value = result.getOrNull()
                _successMessage.value = "Trip created successfully!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to create trip"
            }
        }
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch {
            val result = repository.updateTrip(trip)
            if (result.isSuccess) {
                _successMessage.value = "Trip updated!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update trip"
            }
        }
    }

    fun duplicateTrip(original: Trip) {
        viewModelScope.launch {
            val duplicated = original.copy(
                tripId = "",
                tripName = "${original.tripName} (Copy)",
                status = "Planning"
            )
            val result = repository.createTrip(duplicated)
            if (result.isSuccess) {
                _successMessage.value = "Trip duplicated!"
            }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            val result = repository.deleteTrip(tripId)
            if (result.isSuccess) {
                if (_selectedTripId.value == tripId) {
                    _selectedTripId.value = null
                }
                _successMessage.value = "Trip deleted."
            }
        }
    }

    // --- Itinerary Operations ---
    fun addItineraryDay(tripId: String, dayNumber: Int, date: String, title: String) {
        viewModelScope.launch {
            val it = Itinerary(
                tripId = tripId,
                dayNumber = dayNumber,
                date = date,
                title = title
            )
            repository.createItinerary(it)
        }
    }

    fun addActivity(
        tripId: String,
        itineraryId: String,
        title: String,
        description: String,
        location: String,
        address: String,
        startTime: String,
        endTime: String,
        estimatedCost: Double,
        currency: String,
        status: String,
        notes: String
    ) {
        viewModelScope.launch {
            val act = ItineraryActivity(
                tripId = tripId,
                itineraryId = itineraryId,
                title = title,
                description = description,
                location = location,
                address = address,
                startTime = startTime,
                endTime = endTime,
                estimatedCost = estimatedCost,
                currency = currency,
                activityStatus = status,
                notes = notes,
                sortOrder = (activities.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            )
            repository.createActivity(act)
        }
    }

    fun toggleActivityCompleted(activity: ItineraryActivity) {
        viewModelScope.launch {
            val nextStatus = if (activity.activityStatus == "Completed") "Planned" else "Completed"
            repository.updateActivity(activity.copy(activityStatus = nextStatus))
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
        }
    }

    // --- Budget & Expense Operations ---
    fun saveBudgetCategory(tripId: String, category: String, estimated: Double, actual: Double, currency: String) {
        viewModelScope.launch {
            val existing = budgets.value.find { it.category == category }
            val budget = Budget(
                budgetId = existing?.budgetId ?: "",
                tripId = tripId,
                category = category,
                estimatedAmount = estimated,
                actualAmount = actual,
                currency = currency
            )
            repository.saveBudget(budget)
        }
    }

    fun addExpense(
        tripId: String,
        title: String,
        description: String,
        amount: Double,
        currency: String,
        category: String,
        paidBy: String,
        participants: List<String>,
        splitType: String,
        expenseDate: String
    ) {
        viewModelScope.launch {
            val exp = Expense(
                tripId = tripId,
                title = title,
                description = description,
                amount = amount,
                currency = currency,
                category = category,
                paidBy = paidBy,
                participantIds = participants,
                splitType = splitType,
                expenseDate = expenseDate.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
            )
            repository.createExpense(exp)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun recordSettlement(
        tripId: String,
        fromUser: String,
        toUser: String,
        amount: Double,
        currency: String,
        method: String,
        notes: String
    ) {
        viewModelScope.launch {
            val set = Settlement(
                tripId = tripId,
                fromUserId = fromUser,
                toUserId = toUser,
                amount = amount,
                currency = currency,
                settlementDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                paymentMethod = method,
                status = "COMPLETED",
                notes = notes
            )
            repository.recordSettlement(set)
            _successMessage.value = "Settlement confirmed & recorded!"
        }
    }

    // --- Journal Operations ---
    fun addJournalEntry(
        tripId: String,
        title: String,
        content: String,
        location: String,
        rating: Double,
        tags: List<String>,
        isPrivate: Boolean,
        date: String
    ) {
        viewModelScope.launch {
            val entry = JournalEntry(
                tripId = tripId,
                title = title,
                content = content,
                location = location,
                rating = rating,
                tags = tags,
                visibility = if (isPrivate) "PRIVATE" else "PUBLIC",
                entryDate = date.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
            )
            repository.createJournalEntry(entry)
        }
    }

    fun toggleJournalFavorite(entry: JournalEntry) {
        viewModelScope.launch {
            repository.updateJournalEntry(entry.copy(isFavorite = !entry.isFavorite))
        }
    }

    fun deleteJournalEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteJournalEntry(entryId)
        }
    }

    // --- Packing Checklist Operations ---
    fun addPackingItem(tripId: String, name: String, category: String, quantity: Int, notes: String) {
        viewModelScope.launch {
            val item = PackingItem(
                tripId = tripId,
                listId = "default_list",
                itemName = name,
                category = category,
                quantity = quantity,
                notes = notes,
                isPacked = false
            )
            repository.addPackingItem(item)
        }
    }

    fun togglePackingItem(itemId: String, currentPacked: Boolean) {
        viewModelScope.launch {
            repository.togglePackingItemPacked(itemId, !currentPacked)
        }
    }

    fun deletePackingItem(itemId: String) {
        viewModelScope.launch {
            repository.deletePackingItem(itemId)
        }
    }

    fun applyPackingTemplate(tripId: String, templateName: String) {
        viewModelScope.launch {
            val result = repository.loadPackingTemplate(tripId, templateName)
            if (result.isSuccess) {
                _successMessage.value = "Loaded ${result.getOrNull()} items from $templateName template!"
            }
        }
    }

    // Seed Initial Sample Trip if empty
    fun seedSampleTripIfEmpty() {
        if (trips.value.isEmpty()) {
            viewModelScope.launch {
                val sampleTrip = Trip(
                    tripName = "Explore Kyoto & Tokyo",
                    description = "A 7-day cultural immersion, ancient temples, Japanese cuisine, and modern sights with friends.",
                    destinations = listOf("Kyoto", "Tokyo", "Nara"),
                    startDate = "2026-11-15",
                    endDate = "2026-11-22",
                    tripType = "Friends",
                    status = "Upcoming",
                    currency = "USD",
                    estimatedBudget = 3200.0
                )
                val tripRes = repository.createTrip(sampleTrip)
                val tripId = tripRes.getOrNull()
                if (tripId != null) {
                    _selectedTripId.value = tripId

                    // Seed itinerary
                    repository.createItinerary(Itinerary(tripId = tripId, dayNumber = 1, date = "2026-11-15", title = "Arrival in Tokyo & Shinjuku Night Walk"))
                    repository.createItinerary(Itinerary(tripId = tripId, dayNumber = 2, date = "2026-11-16", title = "Meiji Shrine, Harajuku & Shibuya Crossing"))
                    repository.createItinerary(Itinerary(tripId = tripId, dayNumber = 3, date = "2026-11-17", title = "Shinkansen to Kyoto & Fushimi Inari Shrine"))

                    // Seed activities
                    repository.createActivity(ItineraryActivity(tripId = tripId, itineraryId = "day_1", title = "Check-in at Hotel Gracery", location = "Shinjuku, Tokyo", startTime = "15:00", endTime = "16:00", estimatedCost = 140.0, activityStatus = "Completed", notes = "Confirmation: #KG88294"))
                    repository.createActivity(ItineraryActivity(tripId = tripId, itineraryId = "day_1", title = "Ichiran Ramen Dinner", location = "Shinjuku East", startTime = "18:30", endTime = "20:00", estimatedCost = 35.0, activityStatus = "Completed"))
                    repository.createActivity(ItineraryActivity(tripId = tripId, itineraryId = "day_2", title = "Morning Walk at Meiji Jingu", location = "Shibuya", startTime = "09:00", endTime = "11:30", estimatedCost = 0.0, activityStatus = "Planned"))

                    // Seed budgets
                    repository.saveBudget(Budget(tripId = tripId, category = "Flights", estimatedAmount = 1100.0, actualAmount = 1050.0, currency = "USD"))
                    repository.saveBudget(Budget(tripId = tripId, category = "Hotels", estimatedAmount = 900.0, actualAmount = 880.0, currency = "USD"))
                    repository.saveBudget(Budget(tripId = tripId, category = "Food and Drinks", estimatedAmount = 600.0, actualAmount = 240.0, currency = "USD"))
                    repository.saveBudget(Budget(tripId = tripId, category = "Activities", estimatedAmount = 300.0, actualAmount = 120.0, currency = "USD"))

                    // Seed expenses
                    repository.createExpense(Expense(tripId = tripId, title = "Group Shinkansen Tickets", amount = 420.0, currency = "USD", category = "Transportation", paidBy = "Alex", participantIds = listOf("Alex", "Sam", "Jordan"), splitType = "EQUAL", expenseDate = "2026-11-15"))
                    repository.createExpense(Expense(tripId = tripId, title = "Traditional Kaiseki Dinner", amount = 210.0, currency = "USD", category = "Food and Drinks", paidBy = "Sam", participantIds = listOf("Alex", "Sam", "Jordan"), splitType = "EQUAL", expenseDate = "2026-11-16"))

                    // Seed packing items
                    repository.loadPackingTemplate(tripId, "International travel")

                    // Seed journal entry
                    repository.createJournalEntry(
                        JournalEntry(
                            tripId = tripId,
                            title = "Sunset at Shibuya Sky",
                            content = "Incredible 360-degree panoramic view of Tokyo as Mount Fuji was silhouetted against the pink twilight sky. Truly unforgettable moment!",
                            location = "Shibuya Sky Observation Deck",
                            entryDate = "2026-11-15",
                            rating = 5.0,
                            tags = listOf("Adventure", "Culture"),
                            isFavorite = true,
                            visibility = "PUBLIC"
                        )
                    )

                    // Automatically store all details in Room
                    syncAllTravelDetailsToRoom()
                }
            }
        }
    }

    /**
     * Consolidates ALL the details and information of trips and saves them
     * into the local Android Room database (TravelAllDetailsEntity).
     */
    fun syncAllTravelDetailsToRoom(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val currentTrips = trips.value
                val detailsToStore = mutableListOf<TravelAllDetailsEntity>()

                for (trip in currentTrips) {
                    val tripItineraries = if (trip.tripId == selectedTripId.value) itineraries.value else emptyList()
                    val tripActivities = if (trip.tripId == selectedTripId.value) activities.value else emptyList()
                    val tripExpenses = if (trip.tripId == selectedTripId.value) expenses.value else emptyList()
                    val tripPacking = if (trip.tripId == selectedTripId.value) packingItems.value else emptyList()
                    val tripJournal = if (trip.tripId == selectedTripId.value) journalEntries.value else emptyList()

                    val actualSpent = tripExpenses.sumOf { it.amount }
                    val remaining = trip.estimatedBudget - actualSpent
                    val budgetStatus = when {
                        actualSpent > trip.estimatedBudget -> "OVER_BUDGET"
                        actualSpent > trip.estimatedBudget * 0.85 -> "WARNING"
                        else -> "HEALTHY"
                    }

                    val itinerariesSummary = if (tripItineraries.isNotEmpty()) {
                        tripItineraries.joinToString("; ") { "Day ${it.dayNumber}: ${it.title}" }
                    } else {
                        "Scheduled Itinerary Days: ${tripItineraries.size}"
                    }

                    val activitiesSummary = if (tripActivities.isNotEmpty()) {
                        tripActivities.joinToString("; ") {
                            "${it.title} [${it.activityStatus}] at ${it.startTime.ifEmpty { "Flexible" }} (${it.location.ifEmpty { "General" }}) - $${String.format(Locale.US, "%.0f", it.estimatedCost)}"
                        }
                    } else {
                        "No activities recorded yet"
                    }

                    val expensesSummary = if (tripExpenses.isNotEmpty()) {
                        tripExpenses.joinToString("; ") {
                            "${it.title}: $${String.format(Locale.US, "%.0f", it.amount)} (${it.category}, paid by ${it.paidBy})"
                        }
                    } else {
                        "Total actual expenses: $${String.format(Locale.US, "%.0f", actualSpent)}"
                    }

                    val packedCount = tripPacking.count { it.isPacked }
                    val packingSummary = if (tripPacking.isNotEmpty()) {
                        "$packedCount/${tripPacking.size} items packed (${(packedCount * 100 / tripPacking.size)}%)"
                    } else {
                        "Packing checklist empty"
                    }

                    val journalSummary = if (tripJournal.isNotEmpty()) {
                        "${tripJournal.size} memories recorded: " + tripJournal.joinToString(", ") { "${it.title} (★${it.rating})" }
                    } else {
                        "No journal memories added yet"
                    }

                    val companions = if (trip.tripType.isNotEmpty() && trip.tripType != "Solo") "${trip.tripType} Group" else "Solo Travel"

                    val detailEntity = TravelAllDetailsEntity(
                        tripId = trip.tripId,
                        tripName = trip.tripName,
                        ownerId = trip.ownerId,
                        description = trip.description,
                        destinations = trip.destinations.joinToString(" → "),
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        tripType = trip.tripType,
                        status = trip.status,
                        currency = trip.currency,
                        estimatedBudget = trip.estimatedBudget,
                        totalActualSpent = actualSpent,
                        remainingBudget = remaining,
                        budgetStatus = budgetStatus,
                        itineraryDaysSummary = itinerariesSummary,
                        activitiesSummary = activitiesSummary,
                        expensesSummary = expensesSummary,
                        packingSummary = packingSummary,
                        journalSummary = journalSummary,
                        companions = companions,
                        hotelReservationNotes = "Accommodations & lodging details for ${trip.destinations.firstOrNull() ?: "trip"}",
                        transportationNotes = "Flights, trains, & transfers confirmed for ${trip.tripName}",
                        lastUpdatedEpoch = System.currentTimeMillis()
                    )
                    detailsToStore.add(detailEntity)
                }

                if (detailsToStore.isNotEmpty()) {
                    repository.storeAllTravelDetailsToRoom(detailsToStore)
                    _successMessage.value = "Stored ${detailsToStore.size} travel record(s) in Room SQLite DB"
                    onComplete?.invoke(detailsToStore.size)
                } else {
                    _successMessage.value = "No trips found to sync into Room DB"
                    onComplete?.invoke(0)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to store all details in Room: ${e.localizedMessage}"
            }
        }
    }
}

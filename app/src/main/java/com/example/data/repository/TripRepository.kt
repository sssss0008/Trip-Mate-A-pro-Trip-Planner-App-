package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.local.TripMateDatabase
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TripRepository(
    private val db: FirebaseFirestore,
    private val localDb: TripMateDatabase? = null
) {
    // Secondary constructor resolving named database ID from firebase_applet_config.xml
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        TripMateDatabase.getInstance(context)
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Trips ---
    fun observeUserTrips(): Flow<List<Trip>> = flow {
        val uid = requireUserId()
        val path = "trips"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerId", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Trip::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getTripById(tripId: String): Result<Trip?> {
        val uid = requireUserId()
        val docRef = db.collection("trips").document(tripId)
        return try {
            val snapshot = docRef.get().await()
            val trip = snapshot.toObject(Trip::class.java)
            if (trip != null && trip.ownerId == uid) {
                Result.success(trip)
            } else {
                Result.failure(SecurityException("Unauthorized trip access"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createTrip(trip: Trip): Result<String> {
        val uid = requireUserId()
        val tripId = if (trip.tripId.isNotBlank()) trip.tripId else UUID.randomUUID().toString()
        val docRef = db.collection("trips").document(tripId)

        val payload = mapOf(
            "tripId" to tripId,
            "ownerId" to uid,
            "tripName" to trip.tripName,
            "description" to trip.description,
            "coverImage" to trip.coverImage,
            "destinations" to trip.destinations,
            "startDate" to trip.startDate,
            "endDate" to trip.endDate,
            "tripType" to trip.tripType,
            "status" to trip.status,
            "currency" to trip.currency,
            "estimatedBudget" to trip.estimatedBudget,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(tripId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTrip(trip: Trip): Result<Unit> {
        val uid = requireUserId()
        require(trip.tripId.isNotBlank()) { "Trip ID must not be blank" }
        val docRef = db.collection("trips").document(trip.tripId)

        val updates = mapOf(
            "tripName" to trip.tripName,
            "description" to trip.description,
            "coverImage" to trip.coverImage,
            "destinations" to trip.destinations,
            "startDate" to trip.startDate,
            "endDate" to trip.endDate,
            "tripType" to trip.tripType,
            "status" to trip.status,
            "currency" to trip.currency,
            "estimatedBudget" to trip.estimatedBudget,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(tripId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("trips").document(tripId)
        return try {
            docRef.delete().await()
            localDb?.dao()?.deleteTrip(tripId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Itineraries ---
    fun observeTripItineraries(tripId: String): Flow<List<Itinerary>> = flow {
        val uid = requireUserId()
        val path = "itineraries"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Itinerary::class.java)
                        .filter { it.tripId == tripId }
                        .sortedBy { it.dayNumber }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createItinerary(itinerary: Itinerary): Result<String> {
        val uid = requireUserId()
        val itineraryId = if (itinerary.itineraryId.isNotBlank()) itinerary.itineraryId else UUID.randomUUID().toString()
        val docRef = db.collection("itineraries").document(itineraryId)

        val payload = mapOf(
            "itineraryId" to itineraryId,
            "tripId" to itinerary.tripId,
            "userId" to uid,
            "dayNumber" to itinerary.dayNumber,
            "date" to itinerary.date,
            "title" to itinerary.title,
            "description" to itinerary.description,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(itineraryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Activities ---
    fun observeTripActivities(tripId: String): Flow<List<ItineraryActivity>> = flow {
        val uid = requireUserId()
        val path = "itinerary_activities"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ItineraryActivity::class.java)
                        .filter { it.tripId == tripId }
                        .sortedBy { it.sortOrder }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createActivity(activity: ItineraryActivity): Result<String> {
        val uid = requireUserId()
        val actId = if (activity.activityId.isNotBlank()) activity.activityId else UUID.randomUUID().toString()
        val docRef = db.collection("itinerary_activities").document(actId)

        val payload = mapOf(
            "activityId" to actId,
            "tripId" to activity.tripId,
            "itineraryId" to activity.itineraryId,
            "userId" to uid,
            "title" to activity.title,
            "description" to activity.description,
            "location" to activity.location,
            "address" to activity.address,
            "startTime" to activity.startTime,
            "endTime" to activity.endTime,
            "estimatedCost" to activity.estimatedCost,
            "currency" to activity.currency,
            "activityStatus" to activity.activityStatus,
            "sortOrder" to activity.sortOrder,
            "notes" to activity.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(actId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateActivity(activity: ItineraryActivity): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("itinerary_activities").document(activity.activityId)
        val updates = mapOf(
            "title" to activity.title,
            "description" to activity.description,
            "location" to activity.location,
            "address" to activity.address,
            "startTime" to activity.startTime,
            "endTime" to activity.endTime,
            "estimatedCost" to activity.estimatedCost,
            "currency" to activity.currency,
            "activityStatus" to activity.activityStatus,
            "sortOrder" to activity.sortOrder,
            "notes" to activity.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteActivity(activityId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("itinerary_activities").document(activityId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Budgets ---
    fun observeTripBudgets(tripId: String): Flow<List<Budget>> = flow {
        val uid = requireUserId()
        val path = "budgets"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Budget::class.java).filter { it.tripId == tripId }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveBudget(budget: Budget): Result<String> {
        val uid = requireUserId()
        val bId = if (budget.budgetId.isNotBlank()) budget.budgetId else UUID.randomUUID().toString()
        val docRef = db.collection("budgets").document(bId)

        val payload = mapOf(
            "budgetId" to bId,
            "tripId" to budget.tripId,
            "userId" to uid,
            "category" to budget.category,
            "estimatedAmount" to budget.estimatedAmount,
            "actualAmount" to budget.actualAmount,
            "currency" to budget.currency,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(bId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Expenses ---
    fun observeTripExpenses(tripId: String): Flow<List<Expense>> = flow {
        val uid = requireUserId()
        val path = "expenses"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Expense::class.java)
                        .filter { it.tripId == tripId }
                        .sortedByDescending { it.expenseDate }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createExpense(expense: Expense): Result<String> {
        val uid = requireUserId()
        val expId = if (expense.expenseId.isNotBlank()) expense.expenseId else UUID.randomUUID().toString()
        val docRef = db.collection("expenses").document(expId)

        val payload = mapOf(
            "expenseId" to expId,
            "tripId" to expense.tripId,
            "userId" to uid,
            "title" to expense.title,
            "description" to expense.description,
            "amount" to expense.amount,
            "currency" to expense.currency,
            "category" to expense.category,
            "paidBy" to expense.paidBy,
            "participantIds" to expense.participantIds,
            "splitType" to expense.splitType,
            "expenseDate" to expense.expenseDate,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(expId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("expenses").document(expenseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Settlements ---
    fun observeTripSettlements(tripId: String): Flow<List<Settlement>> = flow {
        val uid = requireUserId()
        val path = "settlements"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Settlement::class.java).filter { it.tripId == tripId }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordSettlement(settlement: Settlement): Result<String> {
        val uid = requireUserId()
        val sId = if (settlement.settlementId.isNotBlank()) settlement.settlementId else UUID.randomUUID().toString()
        val docRef = db.collection("settlements").document(sId)

        val payload = mapOf(
            "settlementId" to sId,
            "tripId" to settlement.tripId,
            "userId" to uid,
            "fromUserId" to settlement.fromUserId,
            "toUserId" to settlement.toUserId,
            "amount" to settlement.amount,
            "currency" to settlement.currency,
            "settlementDate" to settlement.settlementDate,
            "paymentMethod" to settlement.paymentMethod,
            "status" to settlement.status,
            "notes" to settlement.notes,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(sId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Journal Entries ---
    fun observeTripJournal(tripId: String): Flow<List<JournalEntry>> = flow {
        val uid = requireUserId()
        val path = "journal_entries"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(JournalEntry::class.java)
                        .filter { it.tripId == tripId }
                        .sortedByDescending { it.entryDate }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createJournalEntry(entry: JournalEntry): Result<String> {
        val uid = requireUserId()
        val entryId = if (entry.entryId.isNotBlank()) entry.entryId else UUID.randomUUID().toString()
        val docRef = db.collection("journal_entries").document(entryId)

        val payload = mapOf(
            "entryId" to entryId,
            "tripId" to entry.tripId,
            "userId" to uid,
            "title" to entry.title,
            "content" to entry.content,
            "imageReferences" to entry.imageReferences,
            "location" to entry.location,
            "entryDate" to entry.entryDate,
            "rating" to entry.rating,
            "tags" to entry.tags,
            "isFavorite" to entry.isFavorite,
            "visibility" to entry.visibility,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(entryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateJournalEntry(entry: JournalEntry): Result<Unit> {
        requireUserId()
        val docRef = db.collection("journal_entries").document(entry.entryId)
        val updates = mapOf(
            "title" to entry.title,
            "content" to entry.content,
            "imageReferences" to entry.imageReferences,
            "location" to entry.location,
            "entryDate" to entry.entryDate,
            "rating" to entry.rating,
            "tags" to entry.tags,
            "isFavorite" to entry.isFavorite,
            "visibility" to entry.visibility,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("journal_entries").document(entryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Packing Checklist ---
    fun observeTripPackingItems(tripId: String): Flow<List<PackingItem>> = flow {
        val uid = requireUserId()
        val path = "packing_items"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PackingItem::class.java).filter { it.tripId == tripId }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addPackingItem(item: PackingItem): Result<String> {
        val uid = requireUserId()
        val itemId = if (item.itemId.isNotBlank()) item.itemId else UUID.randomUUID().toString()
        val docRef = db.collection("packing_items").document(itemId)

        val payload = mapOf(
            "itemId" to itemId,
            "tripId" to item.tripId,
            "listId" to item.listId,
            "userId" to uid,
            "itemName" to item.itemName,
            "quantity" to item.quantity,
            "isPacked" to item.isPacked,
            "notes" to item.notes,
            "category" to item.category,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePackingItemPacked(itemId: String, isPacked: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection("packing_items").document(itemId)
        return try {
            docRef.update(
                mapOf(
                    "isPacked" to isPacked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePackingItem(itemId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("packing_items").document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun loadPackingTemplate(tripId: String, templateName: String): Result<Int> {
        val items = TravelConstants.DEFAULT_PACKING_TEMPLATES[templateName] ?: emptyList()
        var addedCount = 0
        for ((name, category) in items) {
            val item = PackingItem(
                tripId = tripId,
                listId = "default_list",
                itemName = name,
                category = category,
                isPacked = false,
                quantity = 1
            )
            val res = addPackingItem(item)
            if (res.isSuccess) addedCount++
        }
        return Result.success(addedCount)
    }

    // --- Room Local Database: Consolidated All Details Entity Operations ---
    fun observeLocalAllTravelDetails(): Flow<List<com.example.data.local.TravelAllDetailsEntity>> {
        return localDb?.dao()?.getAllTravelDetails() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun storeAllTravelDetailsToRoom(details: List<com.example.data.local.TravelAllDetailsEntity>) {
        localDb?.dao()?.insertAllTravelDetails(details)
    }

    suspend fun storeTravelDetailToRoom(detail: com.example.data.local.TravelAllDetailsEntity) {
        localDb?.dao()?.insertTravelDetail(detail)
    }

    suspend fun deleteLocalTravelDetail(tripId: String) {
        localDb?.dao()?.deleteTravelDetail(tripId)
    }

    suspend fun clearLocalAllTravelDetails() {
        localDb?.dao()?.clearAllTravelDetails()
    }
}

private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(flow: Flow<T>) {
    flow.collect { value -> emit(value) }
}

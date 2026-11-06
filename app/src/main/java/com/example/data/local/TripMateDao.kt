package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripMateDao {
    // Consolidated Master Detail Entity - All Information
    @Query("SELECT * FROM travel_all_details ORDER BY startDate ASC")
    fun getAllTravelDetails(): Flow<List<TravelAllDetailsEntity>>

    @Query("SELECT * FROM travel_all_details WHERE tripId = :tripId LIMIT 1")
    fun getTravelDetailByTripId(tripId: String): Flow<TravelAllDetailsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravelDetail(detail: TravelAllDetailsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTravelDetails(details: List<TravelAllDetailsEntity>)

    @Query("DELETE FROM travel_all_details WHERE tripId = :tripId")
    suspend fun deleteTravelDetail(tripId: String)

    @Query("DELETE FROM travel_all_details")
    suspend fun clearAllTravelDetails()

    // Trips
    @Query("SELECT * FROM trips WHERE ownerId = :ownerId ORDER BY startDate ASC")
    fun getTrips(ownerId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE tripId = :tripId LIMIT 1")
    suspend fun getTripById(tripId: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE tripId = :tripId")
    suspend fun deleteTrip(tripId: String)

    // Itineraries
    @Query("SELECT * FROM itineraries WHERE tripId = :tripId ORDER BY dayNumber ASC")
    fun getItineraries(tripId: String): Flow<List<ItineraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItineraries(itineraries: List<ItineraryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItinerary(itinerary: ItineraryEntity)

    // Activities
    @Query("SELECT * FROM itinerary_activities WHERE tripId = :tripId ORDER BY sortOrder ASC")
    fun getActivities(tripId: String): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM itinerary_activities WHERE activityId = :activityId")
    suspend fun deleteActivity(activityId: String)

    // Budgets
    @Query("SELECT * FROM budgets WHERE tripId = :tripId")
    fun getBudgets(tripId: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    // Expenses
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY expenseDate DESC")
    fun getExpenses(tripId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE expenseId = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    // Journal
    @Query("SELECT * FROM journal_entries WHERE tripId = :tripId ORDER BY entryDate DESC")
    fun getJournalEntries(tripId: String): Flow<List<JournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntries(entries: List<JournalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntity)

    @Query("DELETE FROM journal_entries WHERE entryId = :entryId")
    suspend fun deleteJournalEntry(entryId: String)

    // Packing Items
    @Query("SELECT * FROM packing_items WHERE tripId = :tripId")
    fun getPackingItems(tripId: String): Flow<List<PackingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackingItems(items: List<PackingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackingItem(item: PackingItemEntity)

    @Update
    suspend fun updatePackingItem(item: PackingItemEntity)

    @Query("DELETE FROM packing_items WHERE itemId = :itemId")
    suspend fun deletePackingItem(itemId: String)

    // Settlements
    @Query("SELECT * FROM settlements WHERE tripId = :tripId ORDER BY settlementDate DESC")
    fun getSettlements(tripId: String): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Consolidated Room entity storing ALL the details and information of a trip plan.
 * Captures trips, destinations, dates, budgets, itineraries, activities, expenses,
 * packing items, journal entries, notes, and companions.
 */
@Entity(tableName = "travel_all_details")
data class TravelAllDetailsEntity(
    @PrimaryKey val tripId: String,
    val tripName: String,
    val ownerId: String,
    val description: String,
    val destinations: String,
    val startDate: String,
    val endDate: String,
    val tripType: String,
    val status: String,
    val currency: String,
    val estimatedBudget: Double,
    val totalActualSpent: Double,
    val remainingBudget: Double,
    val budgetStatus: String,
    val itineraryDaysSummary: String,
    val activitiesSummary: String,
    val expensesSummary: String,
    val packingSummary: String,
    val journalSummary: String,
    val companions: String,
    val hotelReservationNotes: String,
    val transportationNotes: String,
    val lastUpdatedEpoch: Long
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val tripId: String,
    val ownerId: String,
    val tripName: String,
    val description: String,
    val coverImage: String,
    val destinationsJson: String,
    val startDate: String,
    val endDate: String,
    val tripType: String,
    val status: String,
    val currency: String,
    val estimatedBudget: Double,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long
)

@Entity(tableName = "itineraries")
data class ItineraryEntity(
    @PrimaryKey val itineraryId: String,
    val tripId: String,
    val userId: String,
    val dayNumber: Int,
    val date: String,
    val title: String,
    val description: String,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long
)

@Entity(tableName = "itinerary_activities")
data class ActivityEntity(
    @PrimaryKey val activityId: String,
    val tripId: String,
    val itineraryId: String,
    val userId: String,
    val title: String,
    val description: String,
    val location: String,
    val address: String,
    val startTime: String,
    val endTime: String,
    val estimatedCost: Double,
    val currency: String,
    val activityStatus: String,
    val sortOrder: Int,
    val notes: String
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val budgetId: String,
    val tripId: String,
    val userId: String,
    val category: String,
    val estimatedAmount: Double,
    val actualAmount: Double,
    val currency: String
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val expenseId: String,
    val tripId: String,
    val userId: String,
    val title: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val paidBy: String,
    val participantsJson: String,
    val splitType: String,
    val expenseDate: String
)

@Entity(tableName = "journal_entries")
data class JournalEntity(
    @PrimaryKey val entryId: String,
    val tripId: String,
    val userId: String,
    val title: String,
    val content: String,
    val imagesJson: String,
    val location: String,
    val entryDate: String,
    val rating: Double,
    val tagsJson: String,
    val isFavorite: Boolean,
    val visibility: String
)

@Entity(tableName = "packing_items")
data class PackingItemEntity(
    @PrimaryKey val itemId: String,
    val tripId: String,
    val listId: String,
    val userId: String,
    val itemName: String,
    val quantity: Int,
    val isPacked: Boolean,
    val notes: String,
    val category: String
)

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val settlementId: String,
    val tripId: String,
    val userId: String,
    val fromUserId: String,
    val toUserId: String,
    val amount: Double,
    val currency: String,
    val settlementDate: String,
    val paymentMethod: String,
    val status: String,
    val notes: String
)

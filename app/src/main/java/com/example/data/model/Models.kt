package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Trip(
    val tripId: String = "",
    val ownerId: String = "",
    val tripName: String = "",
    val description: String = "",
    val coverImage: String = "",
    val destinations: List<String> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
    val tripType: String = "Solo", // Solo, Family, Friends, Business, Adventure, Honeymoon, Other
    val status: String = "Planning", // Planning, Upcoming, Ongoing, Completed, Cancelled
    val currency: String = "USD",
    val estimatedBudget: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TripMember(
    val memberId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "Viewer", // Owner, Editor, Viewer
    val invitationStatus: String = "Accepted", // Pending, Accepted, Declined
    @ServerTimestamp val joinedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Itinerary(
    val itineraryId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val dayNumber: Int = 1,
    val date: String = "",
    val title: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ItineraryActivity(
    val activityId: String = "",
    val tripId: String = "",
    val itineraryId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val address: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val estimatedCost: Double = 0.0,
    val currency: String = "USD",
    val activityStatus: String = "Planned", // Planned, Completed, Skipped
    val sortOrder: Int = 0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Budget(
    val budgetId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val category: String = "Other",
    val estimatedAmount: Double = 0.0,
    val actualAmount: Double = 0.0,
    val currency: String = "USD",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    val expenseId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val category: String = "Other",
    val paidBy: String = "",
    val participantIds: List<String> = emptyList(),
    val splitType: String = "EQUAL", // EQUAL, EXACT, PERCENTAGE
    val expenseDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpenseSplit(
    val splitId: String = "",
    val expenseId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val owedAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val settlementStatus: String = "UNSETTLED", // UNSETTLED, SETTLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Settlement(
    val settlementId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val settlementDate: String = "",
    val paymentMethod: String = "Cash",
    val status: String = "COMPLETED", // PENDING, COMPLETED
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JournalEntry(
    val entryId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val imageReferences: List<String> = emptyList(),
    val location: String = "",
    val entryDate: String = "",
    val rating: Double = 5.0,
    val tags: List<String> = emptyList(), // Adventure, Food, Nature, Culture, Family
    val isFavorite: Boolean = false,
    val visibility: String = "PUBLIC", // PUBLIC, PRIVATE, MEMBERS
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PackingList(
    val listId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val listName: String = "",
    val category: String = "Travel Essentials",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PackingItem(
    val itemId: String = "",
    val tripId: String = "",
    val listId: String = "",
    val userId: String = "",
    val itemName: String = "",
    val quantity: Int = 1,
    val isPacked: Boolean = false,
    val notes: String = "",
    val category: String = "Travel Essentials",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TripDestination(
    val destinationId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val destinationName: String = "",
    val country: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val arrivalDate: String = "",
    val departureDate: String = "",
    val notes: String = ""
)

@IgnoreExtraProperties
data class NotificationItem(
    val notificationId: String = "",
    val userId: String = "",
    val tripId: String = "",
    val title: String = "",
    val message: String = "",
    val notificationType: String = "TRIP", // TRIP, ITINERARY, HOTEL, FLIGHT, PACKING, BUDGET, SETTLEMENT
    val scheduledAt: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSettings(
    val settingsId: String = "",
    val userId: String = "",
    val defaultCurrency: String = "USD",
    val theme: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val notificationsEnabled: Boolean = true,
    val reminderPreferences: String = "All",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Simplified Debt Settlement Model
data class DebtRecord(
    val debtor: String,
    val creditor: String,
    val amount: Double
)

object TravelConstants {
    val EXPENSE_CATEGORIES = listOf(
        "Flights",
        "Transportation",
        "Hotels",
        "Food and Drinks",
        "Shopping",
        "Activities",
        "Entertainment",
        "Travel Insurance",
        "Visa and Documents",
        "Emergency",
        "Other"
    )

    val PACKING_CATEGORIES = listOf(
        "Clothing",
        "Electronics",
        "Documents",
        "Toiletries",
        "Medicines",
        "Accessories",
        "Travel Essentials",
        "Personal Items",
        "Other"
    )

    val TRIP_TYPES = listOf(
        "Solo",
        "Family",
        "Friends",
        "Business",
        "Adventure",
        "Honeymoon",
        "Other"
    )

    val TRIP_STATUSES = listOf(
        "Planning",
        "Upcoming",
        "Ongoing",
        "Completed",
        "Cancelled"
    )

    val CURRENCIES = listOf("USD", "EUR", "GBP", "JPY", "NPR", "INR", "CAD", "AUD", "SGD", "CHF")

    val DEFAULT_PACKING_TEMPLATES = mapOf(
        "Weekend trips" to listOf(
            "T-shirts (2)" to "Clothing",
            "Jeans/Pants" to "Clothing",
            "Underwear & socks" to "Clothing",
            "Toothbrush & paste" to "Toiletries",
            "Phone charger" to "Electronics",
            "Power bank" to "Electronics",
            "ID card / Driving license" to "Documents",
            "Water bottle" to "Travel Essentials"
        ),
        "International travel" to listOf(
            "Passport & Visa" to "Documents",
            "Flight tickets / Boarding passes" to "Documents",
            "Travel Insurance policy" to "Documents",
            "Universal power adapter" to "Electronics",
            "Noise-cancelling headphones" to "Electronics",
            "Prescription medicines" to "Medicines",
            "Travel pillow & eye mask" to "Travel Essentials",
            "Foreign currency / Forex card" to "Travel Essentials",
            "TSA-approved toiletries" to "Toiletries",
            "Light jacket" to "Clothing"
        ),
        "Business trips" to listOf(
            "Business suits/shirts" to "Clothing",
            "Formal shoes" to "Clothing",
            "Laptop & charger" to "Electronics",
            "Business cards" to "Documents",
            "Presentation pointer" to "Electronics",
            "Steamer / wrinkle release spray" to "Travel Essentials",
            "Notepad & pen" to "Documents"
        ),
        "Family vacations" to listOf(
            "First aid kit" to "Medicines",
            "Kids clothes & extra snacks" to "Personal Items",
            "Wet wipes & hand sanitizer" to "Toiletries",
            "Sunscreen (SPF 50+)" to "Toiletries",
            "Card games & entertainment" to "Travel Essentials",
            "Medical insurance cards" to "Documents",
            "Family passports" to "Documents"
        ),
        "Camping" to listOf(
            "Tent & sleeping bag" to "Travel Essentials",
            "Flashlight / Headlamp" to "Electronics",
            "Multi-tool / Swiss knife" to "Travel Essentials",
            "Mosquito repellent" to "Medicines",
            "Water purifier / tablets" to "Travel Essentials",
            "Thermal innerwear" to "Clothing",
            "Portable camping stove & fuel" to "Travel Essentials"
        ),
        "Hiking" to listOf(
            "Trekking boots" to "Clothing",
            "Hiking poles" to "Travel Essentials",
            "Hydration pack / bladder" to "Travel Essentials",
            "Electrolyte powder" to "Medicines",
            "Rain poncho" to "Clothing",
            "Trail snacks (energy bars)" to "Personal Items",
            "Navigation compass / GPS" to "Electronics"
        ),
        "Beach vacations" to listOf(
            "Swimwear & rash guard" to "Clothing",
            "Flip flops / sandals" to "Clothing",
            "Polarized sunglasses" to "Accessories",
            "Waterproof phone pouch" to "Electronics",
            "Beach towel" to "Travel Essentials",
            "Reef-safe sunscreen" to "Toiletries",
            "Sun hat" to "Accessories"
        ),
        "Winter travel" to listOf(
            "Heavy winter coat / Parka" to "Clothing",
            "Thermal fleece layers" to "Clothing",
            "Wool beanie & gloves" to "Clothing",
            "Snow boots" to "Clothing",
            "Moisturizing lip balm & lotion" to "Toiletries",
            "Hand warmers" to "Travel Essentials"
        )
    )
}

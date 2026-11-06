package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Trip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class TripRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTrip_validPayload_createsDocumentAndReturnsId(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TripRepository(firestore)

        val trip = Trip(
            tripId = UUID.randomUUID().toString(),
            tripName = "Tokyo Adventure",
            startDate = "2026-07-01",
            endDate = "2026-07-10",
            tripType = "Adventure",
            status = "Planning",
            currency = "USD",
            estimatedBudget = 2500.0
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTrip(trip) }
        assertTrue(result.isSuccess)
        val tripId = result.getOrThrow()
        assertTrue(tripId.isNotEmpty())
    }

    @Test
    fun observeUserTrips_authenticatedOwner_emitsCreatedTrip(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TripRepository(firestore)

        val uniqueTripName = "Trip-" + UUID.randomUUID().toString().take(6)
        val trip = Trip(
            tripId = UUID.randomUUID().toString(),
            tripName = uniqueTripName,
            startDate = "2026-08-01",
            endDate = "2026-08-08",
            tripType = "Solo",
            status = "Upcoming",
            currency = "EUR",
            estimatedBudget = 1800.0
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTrip(trip) }
        assertTrue(createResult.isSuccess)

        val trips = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserTrips().first { list -> list.any { it.tripName == uniqueTripName } }
        }
        assertTrue(trips.any { it.tripName == uniqueTripName })
    }

    @Test
    fun getTripById_crossUserAccess_failsWithPermissionDenied(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = TripRepository(firestore)
        val tripId = UUID.randomUUID().toString()

        val trip = Trip(
            tripId = tripId,
            tripName = "Alice Private Trip",
            startDate = "2026-09-01",
            endDate = "2026-09-05",
            tripType = "Solo",
            status = "Planning",
            currency = "USD"
        )
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.createTrip(trip) }
        assertTrue(createResult.isSuccess)

        // Switch to Bob
        signInTestUser(BOB_EMAIL)
        val bobRepo = TripRepository(firestore)

        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getTripById(tripId) }
        assertTrue(bobResult.isFailure)
    }

    @Test
    fun observeUserTrips_unauthenticatedUser_fails(): Unit = runBlocking {
        auth.signOut()
        val repository = TripRepository(firestore)

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                withTimeout(FLOW_TIMEOUT_MS) {
                    repository.observeUserTrips().first()
                }
            }
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}

package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.viewmodel.TripViewModel

enum class TripMateScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    MY_TRIPS("My Trips", Icons.Default.Luggage),
    ITINERARY("Itinerary", Icons.Default.CalendarMonth),
    EXPENSES("Expenses", Icons.Default.AccountBalanceWallet),
    JOURNAL("Journal", Icons.Default.Book),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripMateAppContent(
    onSignOut: () -> Unit,
    viewModel: TripViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: TripMateScreen.HOME.name

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showCreateTripDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            TripMateScreen.HOME.name -> "TripMate Dashboard"
                            TripMateScreen.MY_TRIPS.name -> "My Trips"
                            TripMateScreen.ITINERARY.name -> "Trip Itinerary"
                            TripMateScreen.EXPENSES.name -> "Budget & Expenses"
                            TripMateScreen.JOURNAL.name -> "Travel Journal"
                            TripMateScreen.SETTINGS.name -> "Preferences"
                            "PACKING" -> "Packing Checklist"
                            else -> "TripMate"
                        }
                    )
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("PACKING") },
                        modifier = Modifier.testTag("top_bar_packing_button")
                    ) {
                        Icon(Icons.Default.Checklist, contentDescription = "Packing Checklist")
                    }
                    IconButton(
                        onClick = { navController.navigate(TripMateScreen.SETTINGS.name) },
                        modifier = Modifier.testTag("top_bar_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val bottomScreens = listOf(
                    TripMateScreen.HOME,
                    TripMateScreen.MY_TRIPS,
                    TripMateScreen.ITINERARY,
                    TripMateScreen.EXPENSES,
                    TripMateScreen.JOURNAL
                )
                bottomScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.name
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.name) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = TripMateScreen.HOME.name,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(TripMateScreen.HOME.name) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTrips = { navController.navigate(TripMateScreen.MY_TRIPS.name) },
                    onNavigateToItinerary = { navController.navigate(TripMateScreen.ITINERARY.name) },
                    onNavigateToExpenses = { navController.navigate(TripMateScreen.EXPENSES.name) },
                    onNavigateToJournal = { navController.navigate(TripMateScreen.JOURNAL.name) },
                    onNavigateToPacking = { navController.navigate("PACKING") },
                    onCreateTripClick = { showCreateTripDialog = true }
                )
            }

            composable(TripMateScreen.MY_TRIPS.name) {
                MyTripsScreen(
                    viewModel = viewModel,
                    onTripSelected = {
                        navController.navigate(TripMateScreen.ITINERARY.name)
                    }
                )
            }

            composable(TripMateScreen.ITINERARY.name) {
                ItineraryScreen(viewModel = viewModel)
            }

            composable(TripMateScreen.EXPENSES.name) {
                ExpensesScreen(viewModel = viewModel)
            }

            composable(TripMateScreen.JOURNAL.name) {
                JournalScreen(viewModel = viewModel)
            }

            composable("PACKING") {
                PackingScreen(viewModel = viewModel)
            }

            composable(TripMateScreen.SETTINGS.name) {
                SettingsScreen(onSignOut = onSignOut)
            }
        }
    }

    if (showCreateTripDialog) {
        TripFormDialog(
            initialTrip = null,
            onDismiss = { showCreateTripDialog = false },
            onSave = { name, desc, destinations, start, end, type, status, currency, budget ->
                viewModel.createTrip(name, desc, destinations, start, end, type, status, currency, budget)
                showCreateTripDialog = false
            }
        )
    }
}

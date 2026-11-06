package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyManager
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.TravelConstants
import com.example.ui.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: TripViewModel
) {
    val activeTrip by viewModel.selectedTrip.collectAsState()
    val allTrips by viewModel.trips.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val settlements by viewModel.settlements.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Budget Calculator, 1: Group Expense Sharing, 2: Currency Converter
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf<DebtRecord?>(null) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    if (activeTrip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select or create a trip to manage budget & expenses.", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    val totalActual = expenses.sumOf { it.amount }
    val estimatedBudget = if (activeTrip!!.estimatedBudget > 0) activeTrip!!.estimatedBudget else budgets.sumOf { it.estimatedAmount }
    val remainingBudget = estimatedBudget - totalActual
    val spentPercentage = if (estimatedBudget > 0) (totalActual / estimatedBudget * 100).toInt() else 0
    val isOverBudget = remainingBudget < 0
    val isNearBudget = spentPercentage >= 80 && !isOverBudget

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) showAddBudgetDialog = true
                    else showAddExpenseDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (selectedTabIndex == 0) "Budget Item" else "Add Expense") },
                modifier = Modifier.testTag("add_expense_fab")
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

            // Navigation Tabs
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Budget") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Group Splits") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Currency") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTabIndex) {
                0 -> BudgetCalculatorTab(
                    trip = activeTrip!!,
                    totalActual = totalActual,
                    estimatedBudget = estimatedBudget,
                    remainingBudget = remainingBudget,
                    spentPercentage = spentPercentage,
                    isNearBudget = isNearBudget,
                    isOverBudget = isOverBudget,
                    budgets = budgets,
                    expenses = expenses
                )
                1 -> GroupExpenseSharingTab(
                    trip = activeTrip!!,
                    expenses = expenses,
                    settlements = settlements,
                    onSettle = { showSettleDialog = it },
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )
                2 -> CurrencyConverterTab(tripCurrency = activeTrip!!.currency)
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            trip = activeTrip!!,
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { title, desc, amount, cur, cat, paidBy, participants, splitType, date ->
                viewModel.addExpense(
                    tripId = activeTrip!!.tripId,
                    title = title,
                    description = desc,
                    amount = amount,
                    currency = cur,
                    category = cat,
                    paidBy = paidBy,
                    participants = participants,
                    splitType = splitType,
                    expenseDate = date
                )
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            trip = activeTrip!!,
            onDismiss = { showAddBudgetDialog = false },
            onSave = { cat, est, act, cur ->
                viewModel.saveBudgetCategory(activeTrip!!.tripId, cat, est, act, cur)
                showAddBudgetDialog = false
            }
        )
    }

    if (showSettleDialog != null) {
        val debt = showSettleDialog!!
        ConfirmSettlementDialog(
            debt = debt,
            currency = activeTrip!!.currency,
            onDismiss = { showSettleDialog = null },
            onConfirm = { method, notes ->
                viewModel.recordSettlement(
                    tripId = activeTrip!!.tripId,
                    fromUser = debt.debtor,
                    toUser = debt.creditor,
                    amount = debt.amount,
                    currency = activeTrip!!.currency,
                    method = method,
                    notes = notes
                )
                showSettleDialog = null
            }
        )
    }
}

@Composable
fun BudgetCalculatorTab(
    trip: com.example.data.model.Trip,
    totalActual: Double,
    estimatedBudget: Double,
    remainingBudget: Double,
    spentPercentage: Int,
    isNearBudget: Boolean,
    isOverBudget: Boolean,
    budgets: List<com.example.data.model.Budget>,
    expenses: List<Expense>
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Warning Banner
        if (isOverBudget) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Budget Alert: You have exceeded the trip budget by ${trip.currency} ${String.format(java.util.Locale.US, "%.0f", -remainingBudget)}!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        } else if (isNearBudget) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF856404))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Approaching Budget Limit: $spentPercentage% of your estimated budget has been spent.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF856404)
                        )
                    }
                }
            }
        }

        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Total Trip Budget", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${trip.currency} ${String.format(java.util.Locale.US, "%.2f", totalActual)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "of ${trip.currency} ${String.format(java.util.Locale.US, "%.0f", estimatedBudget)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { (spentPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remaining: ${trip.currency} ${String.format(java.util.Locale.US, "%.2f", remainingBudget)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "$spentPercentage% Used",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Category Breakdown
        item {
            Text("Cost Breakdown by Category", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        items(TravelConstants.EXPENSE_CATEGORIES) { category ->
            val catExpenses = expenses.filter { it.category == category }
            val catTotal = catExpenses.sumOf { it.amount }
            val catBudget = budgets.find { it.category == category }?.estimatedAmount ?: 0.0

            if (catTotal > 0 || catBudget > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                "${trip.currency} ${String.format(java.util.Locale.US, "%.2f", catTotal)}" +
                                        if (catBudget > 0) " / ${String.format(java.util.Locale.US, "%.0f", catBudget)}" else "",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (catBudget > 0) {
                            val catPct = (catTotal / catBudget).coerceIn(0.0, 1.0).toFloat()
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { catPct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupExpenseSharingTab(
    trip: com.example.data.model.Trip,
    expenses: List<Expense>,
    settlements: List<com.example.data.model.Settlement>,
    onSettle: (DebtRecord) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    // Gather all participants
    val allMembers = (expenses.flatMap { it.participantIds + listOf(it.paidBy) } + listOf("You")).distinct().filter { it.isNotBlank() }
    val simplifiedDebts = remember(expenses, allMembers) {
        CurrencyManager.calculateSimplifiedDebts(allMembers, expenses)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Debt Settlement Summary
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Simplified Group Settlements",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Minimizes the total transfers needed to settle all balances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (simplifiedDebts.isEmpty()) {
                        Text(
                            text = "🎉 All group expenses are fully settled!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        simplifiedDebts.forEach { debt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${debt.debtor} owes ${debt.creditor}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${trip.currency} ${String.format(java.util.Locale.US, "%.2f", debt.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onSettle(debt) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Settle", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Shared Expenses History
        item {
            Text("Shared Expense History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        if (expenses.isEmpty()) {
            item {
                Text("No shared expenses recorded yet. Tap '+ Add Expense' to record one.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(expenses) { exp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exp.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Paid by ${exp.paidBy} • Split among ${exp.participantIds.size.coerceAtLeast(1)} members (${exp.splitType})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("📅 ${exp.expenseDate} • ${exp.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${exp.currency} ${String.format(java.util.Locale.US, "%.2f", exp.amount)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { onDeleteExpense(exp.expenseId) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Past Settlements Record
        if (settlements.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Completed Settlements", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            items(settlements) { set ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("✓ ${set.fromUserId} paid ${set.toUserId}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Method: ${set.paymentMethod} • Date: ${set.settlementDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${set.currency} ${String.format(java.util.Locale.US, "%.2f", set.amount)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyConverterTab(tripCurrency: String) {
    var amountInput by remember { mutableStateOf("100") }
    var fromCurrency by remember { mutableStateOf(tripCurrency) }
    var toCurrency by remember { mutableStateOf("EUR") }
    var customRateInput by remember { mutableStateOf("") }
    var showCustomRateModal by remember { mutableStateOf(false) }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val converted = CurrencyManager.convert(amount, fromCurrency, toCurrency)
    val fromRate = CurrencyManager.rates[fromCurrency]
    val toRate = CurrencyManager.rates[toCurrency]

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Live Currency Converter", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fromCurrency,
                            onValueChange = { fromCurrency = it.uppercase() },
                            label = { Text("From") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = toCurrency,
                            onValueChange = { toCurrency = it.uppercase() },
                            label = { Text("To") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$amount $fromCurrency =",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", converted)} $toCurrency",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Clearly distinguish live vs manual rates
                            val isLive = (fromRate?.isLiveRate == true) && (toRate?.isLiveRate == true)
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(if (isLive) "⚡ Live Market Exchange Rate" else "✎ Manually Entered Exchange Rate")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isLive) Icons.Default.Bolt else Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Manual Exchange Rate Override", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Offline or got a custom bank rate? Enter it here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showCustomRateModal = true }) {
                        Text("Set Custom Exchange Rate")
                    }
                }
            }
        }
    }

    if (showCustomRateModal) {
        var overrideCode by remember { mutableStateOf("NPR") }
        var overrideRate by remember { mutableStateOf("135.0") }

        AlertDialog(
            onDismissRequest = { showCustomRateModal = false },
            title = { Text("Set Custom Currency Rate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = overrideCode,
                        onValueChange = { overrideCode = it.uppercase() },
                        label = { Text("Currency Code (e.g. NPR, JPY)") }
                    )
                    OutlinedTextField(
                        value = overrideRate,
                        onValueChange = { overrideRate = it },
                        label = { Text("Units per 1.00 USD") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val rate = overrideRate.toDoubleOrNull() ?: 1.0
                    CurrencyManager.setManualRate(overrideCode, rate)
                    showCustomRateModal = false
                }) {
                    Text("Save Custom Rate")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomRateModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddExpenseDialog(
    trip: com.example.data.model.Trip,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        desc: String,
        amount: Double,
        cur: String,
        cat: String,
        paidBy: String,
        participants: List<String>,
        splitType: String,
        date: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TravelConstants.EXPENSE_CATEGORIES[0]) }
    var paidBy by remember { mutableStateOf("You") }
    var companionsInput by remember { mutableStateOf("You, Alex, Sam") }
    var splitType by remember { mutableStateOf("EQUAL") } // EQUAL, EXACT, PERCENTAGE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Group Expense") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title *") },
                        modifier = Modifier.fillMaxWidth().testTag("expense_title_input"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (${trip.currency}) *") },
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = paidBy,
                        onValueChange = { paidBy = it },
                        label = { Text("Who Paid? *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = companionsInput,
                        onValueChange = { companionsInput = it },
                        label = { Text("Participants (comma-separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text("Category", style = MaterialTheme.typography.labelSmall)
                    LazyColumn(modifier = Modifier.height(100.dp)) {
                        items(TravelConstants.EXPENSE_CATEGORIES) { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat }
                                )
                                Text(cat, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        val parts = companionsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onAdd(title, description, amount, trip.currency, selectedCategory, paidBy, parts, splitType, "")
                    }
                },
                enabled = title.isNotBlank() && amountInput.toDoubleOrNull() != null,
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddBudgetDialog(
    trip: com.example.data.model.Trip,
    onDismiss: () -> Unit,
    onSave: (cat: String, est: Double, act: Double, cur: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TravelConstants.EXPENSE_CATEGORIES[0]) }
    var estimatedInput by remember { mutableStateOf("500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = estimatedInput,
                    onValueChange = { estimatedInput = it },
                    label = { Text("Estimated Amount (${trip.currency})") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Select Category", style = MaterialTheme.typography.labelSmall)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(TravelConstants.EXPENSE_CATEGORIES) { cat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedCategory == cat, onClick = { selectedCategory = cat })
                            Text(cat)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val est = estimatedInput.toDoubleOrNull() ?: 0.0
                onSave(selectedCategory, est, 0.0, trip.currency)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmSettlementDialog(
    debt: DebtRecord,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (method: String, notes: String) -> Unit
) {
    var method by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("Hand-to-hand repayment") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Debt Settlement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${debt.debtor} is paying $currency ${String.format(java.util.Locale.US, "%.2f", debt.amount)} to ${debt.creditor}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("Payment Method (Cash, Venmo, UPI, Bank)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Settlement Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(method, notes) },
                modifier = Modifier.testTag("confirm_settlement_button")
            ) {
                Text("Record Repayment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

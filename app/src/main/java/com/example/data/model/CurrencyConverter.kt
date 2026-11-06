package com.example.data.model

data class CurrencyRate(
    val code: String,
    val name: String,
    val rateAgainstUSD: Double,
    val isLiveRate: Boolean = false,
    val lastUpdated: String = "2026-11-03"
)

object CurrencyManager {
    // Standard baseline reference rates against 1 USD (with indicator if manual/user entered vs live)
    val rates = mutableMapOf(
        "USD" to CurrencyRate("USD", "US Dollar", 1.0, isLiveRate = true),
        "EUR" to CurrencyRate("EUR", "Euro", 0.92, isLiveRate = true),
        "GBP" to CurrencyRate("GBP", "British Pound", 0.79, isLiveRate = true),
        "JPY" to CurrencyRate("JPY", "Japanese Yen", 152.0, isLiveRate = true),
        "NPR" to CurrencyRate("NPR", "Nepalese Rupee", 134.5, isLiveRate = true),
        "INR" to CurrencyRate("INR", "Indian Rupee", 84.1, isLiveRate = true),
        "CAD" to CurrencyRate("CAD", "Canadian Dollar", 1.39, isLiveRate = true),
        "AUD" to CurrencyRate("AUD", "Australian Dollar", 1.52, isLiveRate = true),
        "SGD" to CurrencyRate("SGD", "Singapore Dollar", 1.32, isLiveRate = true),
        "CHF" to CurrencyRate("CHF", "Swiss Franc", 0.88, isLiveRate = true)
    )

    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount
        val fromRate = rates[fromCurrency]?.rateAgainstUSD ?: 1.0
        val toRate = rates[toCurrency]?.rateAgainstUSD ?: 1.0
        val amountInUSD = amount / fromRate
        return amountInUSD * toRate
    }

    fun setManualRate(code: String, rateAgainstUSD: Double) {
        rates[code] = CurrencyRate(code, code, rateAgainstUSD, isLiveRate = false, lastUpdated = "Manually entered")
    }

    // Debt Simplification Algorithm (Minimizes transactions needed to settle all debts)
    fun calculateSimplifiedDebts(
        members: List<String>,
        expenses: List<Expense>
    ): List<DebtRecord> {
        val netBalance = mutableMapOf<String, Double>()
        members.forEach { netBalance[it] = 0.0 }

        for (expense in expenses) {
            val payer = expense.paidBy
            val participants = if (expense.participantIds.isNotEmpty()) expense.participantIds else members
            if (participants.isEmpty()) continue

            val share = expense.amount / participants.size
            netBalance[payer] = (netBalance[payer] ?: 0.0) + expense.amount

            for (participant in participants) {
                netBalance[participant] = (netBalance[participant] ?: 0.0) - share
            }
        }

        // Separate debtors (negative net) and creditors (positive net)
        val debtors = mutableListOf<Pair<String, Double>>()
        val creditors = mutableListOf<Pair<String, Double>>()

        netBalance.forEach { (member, balance) ->
            if (balance < -0.01) {
                debtors.add(member to -balance)
            } else if (balance > 0.01) {
                creditors.add(member to balance)
            }
        }

        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val simplified = mutableListOf<DebtRecord>()
        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val (debtor, debt) = debtors[dIdx]
            val (creditor, credit) = creditors[cIdx]

            val settledAmount = minOf(debt, credit)
            if (settledAmount > 0.01) {
                simplified.add(DebtRecord(debtor, creditor, settledAmount))
            }

            val remainingDebt = debt - settledAmount
            val remainingCredit = credit - settledAmount

            if (remainingDebt <= 0.01) {
                dIdx++
            } else {
                debtors[dIdx] = debtor to remainingDebt
            }

            if (remainingCredit <= 0.01) {
                cIdx++
            } else {
                creditors[cIdx] = creditor to remainingCredit
            }
        }

        return simplified
    }
}

package com.runanywhere.startup_hackathon20

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.math.roundToInt

/**
 * Utility object for seeding test UPI transaction data.
 * FOR DEVELOPMENT AND TESTING ONLY - Remove in production.
 */
object TestDataSeeder {

    /**
     * Seeds test UPI transactions for a given UPI ID.
     * Creates transactions spanning 6 months with varying amounts.
     *
     * @param upiId The UPI ID to create transactions for
     * @param onComplete Callback when seeding is complete (success or failure)
     */
    fun seedTestTransactions(
        upiId: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        // Define monthly amounts for 6 months (realistic transaction patterns)
        val monthlyAmounts = listOf(
            8000.0,  // 6 months ago
            9500.0,  // 5 months ago
            8200.0,  // 4 months ago
            10000.0, // 3 months ago
            8800.0,  // 2 months ago
            9200.0   // 1 month ago
        )

        var totalTransactionsCreated = 0
        var transactionsToCreate = 0

        try {
            monthlyAmounts.forEachIndexed { monthIndex, monthlyAmount ->
                // Create 4-5 transactions per month
                val transactionsPerMonth = (4..5).random()
                transactionsToCreate += transactionsPerMonth

                repeat(transactionsPerMonth) { transactionIndex ->
                    // Calculate date for this transaction
                    // Distribute transactions across the month
                    val daysAgo = 180 - (monthIndex * 30) - (transactionIndex * 7)
                    calendar.timeInMillis = currentTime
                    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)

                    // Split monthly amount across transactions with some variation
                    val baseAmount = monthlyAmount / transactionsPerMonth
                    val variation = baseAmount * 0.15 * (Math.random() - 0.5) // ±7.5% variation
                    val transactionAmount = baseAmount + variation

                    val transaction = hashMapOf(
                        "borrower_upi" to upiId,
                        "amount" to transactionAmount,
                        "timestamp" to Timestamp(calendar.time),
                        "description" to "Test Transaction - Month ${6 - monthIndex}",
                        "type" to "credit" // Assuming all are incoming payments
                    )

                    firestore.collection("transactions")
                        .add(transaction)
                        .addOnSuccessListener {
                            totalTransactionsCreated++
                            if (totalTransactionsCreated == transactionsToCreate) {
                                onComplete(
                                    true,
                                    "Successfully created $totalTransactionsCreated test transactions"
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, "Failed to create transaction: ${e.message}")
                        }
                }
            }
        } catch (e: Exception) {
            onComplete(false, "Error seeding data: ${e.message}")
        }
    }

    /**
     * Seeds test transactions with custom monthly amounts.
     * Useful for testing different score scenarios.
     *
     * @param upiId The UPI ID to create transactions for
     * @param monthlyAmounts List of 6 monthly amounts (most recent last)
     * @param onComplete Callback when seeding is complete
     */
    fun seedCustomTransactions(
        upiId: String,
        monthlyAmounts: List<Double>,
        onComplete: (Boolean, String) -> Unit
    ) {
        require(monthlyAmounts.size == 6) { "Must provide exactly 6 monthly amounts" }

        val firestore = FirebaseFirestore.getInstance()
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        var totalTransactionsCreated = 0
        var transactionsToCreate = 0

        try {
            monthlyAmounts.forEachIndexed { monthIndex, monthlyAmount ->
                val transactionsPerMonth = 4
                transactionsToCreate += transactionsPerMonth

                repeat(transactionsPerMonth) { transactionIndex ->
                    val daysAgo = 180 - (monthIndex * 30) - (transactionIndex * 7)
                    calendar.timeInMillis = currentTime
                    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)

                    val transactionAmount = monthlyAmount / transactionsPerMonth

                    val transaction = hashMapOf(
                        "borrower_upi" to upiId,
                        "amount" to transactionAmount,
                        "timestamp" to Timestamp(calendar.time),
                        "description" to "Custom Test Transaction - Month ${6 - monthIndex}",
                        "type" to "credit"
                    )

                    firestore.collection("transactions")
                        .add(transaction)
                        .addOnSuccessListener {
                            totalTransactionsCreated++
                            if (totalTransactionsCreated == transactionsToCreate) {
                                onComplete(
                                    true,
                                    "Successfully created $totalTransactionsCreated custom transactions"
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, "Failed to create transaction: ${e.message}")
                        }
                }
            }
        } catch (e: Exception) {
            onComplete(false, "Error seeding data: ${e.message}")
        }
    }

    /**
     * Clears all test transactions for a given UPI ID.
     * Use with caution.
     *
     * @param upiId The UPI ID to clear transactions for
     * @param onComplete Callback when clearing is complete
     */
    fun clearTestTransactions(
        upiId: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("transactions")
            .whereEqualTo("borrower_upi", upiId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onComplete(true, "No transactions found to delete")
                    return@addOnSuccessListener
                }

                var deletedCount = 0
                val totalToDelete = documents.size()

                documents.documents.forEach { document ->
                    document.reference.delete()
                        .addOnSuccessListener {
                            deletedCount++
                            if (deletedCount == totalToDelete) {
                                onComplete(true, "Successfully deleted $deletedCount transactions")
                            }
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, "Failed to delete transaction: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Failed to fetch transactions: ${e.message}")
            }
    }

    /**
     * Test scenario generators for different SARRAL scores
     */
    object Scenarios {
        // High score: Consistent high income
        val HIGH_SCORE = listOf(15000.0, 15500.0, 15200.0, 15300.0, 15100.0, 15400.0)

        // Medium score: Moderate but consistent income
        val MEDIUM_SCORE = listOf(8000.0, 8500.0, 8200.0, 8300.0, 8100.0, 8400.0)

        // Low score: Inconsistent income
        val LOW_SCORE = listOf(5000.0, 12000.0, 3000.0, 15000.0, 4000.0, 10000.0)

        // Zero score: No transactions
        val ZERO_SCORE = listOf<Double>()

        // Growing income: Increasing over time
        val GROWING_INCOME = listOf(5000.0, 6000.0, 7500.0, 9000.0, 10500.0, 12000.0)

        // Declining income: Decreasing over time
        val DECLINING_INCOME = listOf(12000.0, 10500.0, 9000.0, 7500.0, 6000.0, 5000.0)
    }

    /**
     * Calculate expected SARRAL score for given monthly amounts (for testing).
     */
    fun calculateExpectedScore(monthlyAmounts: List<Double>): Pair<Int, Int> {
        if (monthlyAmounts.isEmpty()) return Pair(0, 0)

        // Calculate monthly inflow
        val monthlyInflow = monthlyAmounts.sum() / 6.0

        // Calculate income score (capped at 100)
        var incomeScore = (monthlyInflow / 60000.0) * 100.0
        if (incomeScore > 100.0) {
            incomeScore = 100.0
        }

        // Find max and min months
        val maxMonth = monthlyAmounts.maxOrNull() ?: 0.0
        val minMonth = monthlyAmounts.minOrNull() ?: 0.0

        // Calculate consistency score (bounded 0-100)
        var consistencyScore = if (maxMonth > 0) {
            100.0 - ((maxMonth - minMonth) / maxMonth * 100.0)
        } else {
            0.0
        }

        // Apply bounds
        if (consistencyScore < 0.0) {
            consistencyScore = 0.0
        }
        if (consistencyScore > 100.0) {
            consistencyScore = 100.0
        }

        // Calculate SARRAL score (30% income, 70% consistency)
        val sarralScore = (incomeScore * 0.3) + (consistencyScore * 0.7)

        // Calculate loan limit (30% of monthly inflow)
        val loanLimit = monthlyInflow * 0.30

        return Pair(sarralScore.roundToInt(), loanLimit.roundToInt())
    }

}

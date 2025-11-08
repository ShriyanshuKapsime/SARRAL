package com.runanywhere.startup_hackathon20

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UPIAnalyzerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    var upiId by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UPI Analyzer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isAnalyzing) {
                // Input Section
                Text(
                    text = "UPI Activity Analyzer",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Analyze your UPI transaction history to calculate your SARRAL Score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // UPI ID Input
                OutlinedTextField(
                    value = upiId,
                    onValueChange = {
                        upiId = it.lowercase()
                        errorMessage = null
                    },
                    label = { Text("Enter UPI ID") },
                    placeholder = { Text("shopkeeper@paytm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Analyze Button
                Button(
                    onClick = {
                        when {
                            upiId.isBlank() -> {
                                errorMessage = "Please enter a UPI ID"
                            }

                            !upiId.contains("@") -> {
                                errorMessage = "Invalid UPI ID format"
                            }

                            else -> {
                                isAnalyzing = true
                                errorMessage = null
                                // Start analysis
                                analyzeUPIActivity(
                                    upiId = upiId,
                                    firestore = firestore,
                                    context = context,
                                    onProgress = { progress ->
                                        progressText = progress
                                    },
                                    onSuccess = {
                                        onNavigateToDashboard()
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                        isAnalyzing = false
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Analyze UPI Activity",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "ℹ️ How it works",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We analyze your last 30 days of UPI transactions to calculate your SARRAL Score and determine your loan eligibility.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else {
                // Progress Section
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 6.dp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Analyzing UPI inflows...",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun analyzeUPIActivity(
    upiId: String,
    firestore: FirebaseFirestore,
    context: Context,
    onProgress: (String) -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            onProgress("Checking existing data...")

            // Step 1: Check if transactions already exist
            val existingTransactions = firestore.collection("transactions")
                .whereEqualTo("borrower_upi", upiId)
                .limit(1)
                .get()
                .await()

            if (!existingTransactions.isEmpty) {
                // Data exists, skip generation
                onProgress("Data found! Redirecting...")
                kotlinx.coroutines.delay(500)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
                return@launch
            }

            // Step 2: No data exists - Generate fake transactions
            onProgress("No transaction history found. Generating sample data...")

            generateFakeTransactions(
                upiId = upiId,
                firestore = firestore,
                context = context,
                onProgress = onProgress
            )

            onProgress("Analysis complete!")

            kotlinx.coroutines.delay(500)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                // Show success toast for generated data
                Toast.makeText(
                    context,
                    "✅ Generated sample UPI history successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess()
            }

        } catch (e: Exception) {
            android.util.Log.e("UPIAnalyzer", "Error analyzing UPI activity", e)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onError("Failed to analyze UPI activity: ${e.message}")
            }
        }
    }
}

private suspend fun generateFakeTransactions(
    upiId: String,
    firestore: FirebaseFirestore,
    context: Context,
    onProgress: (String) -> Unit
) {
    onProgress("Generating transaction history...")

    val calendar = Calendar.getInstance()
    val endDate = calendar.time
    calendar.add(Calendar.DAY_OF_YEAR, -30)
    val startDate = calendar.time

    var totalInflow = 0.0
    var totalTransactions = 0
    val batch = firestore.batch()
    var batchCount = 0

    // Generate transactions for last 30 days
    for (dayOffset in 0 until 30) {
        calendar.time = startDate
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
        val currentDate = calendar.time

        // Random transactions per day (5-12)
        val transactionsToday = Random.nextInt(5, 13)

        for (txIndex in 0 until transactionsToday) {
            // Random amount between ₹50-₹600
            val amount = Random.nextDouble(50.0, 601.0)
            totalInflow += amount
            totalTransactions++

            // Random time during the day
            val hourOfDay = Random.nextInt(8, 21) // 8 AM to 9 PM
            val minute = Random.nextInt(0, 60)
            calendar.time = currentDate
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, Random.nextInt(0, 60))

            val transactionData = hashMapOf(
                "borrower_upi" to upiId,
                "amount" to amount,
                "timestamp" to Timestamp(calendar.time),
                "source" to "Customer${Random.nextInt(1001, 9999)}",
                "type" to "credit"
            )

            val docRef = firestore.collection("transactions").document()
            batch.set(docRef, transactionData)
            batchCount++

            // Commit batch every 500 documents (Firestore limit)
            if (batchCount >= 500) {
                batch.commit().await()
                batchCount = 0
                onProgress("Generated ${totalTransactions} transactions...")
            }
        }
    }

    // Commit remaining transactions
    if (batchCount > 0) {
        batch.commit().await()
    }

    onProgress("Calculating SARRAL Score...")

    // Step 3: Calculate SARRAL Score
    val avgTx = totalInflow / totalTransactions
    val consistency = (totalTransactions.toDouble() / (30.0 * 12.0)) * 100.0
    val score = min(
        100.0,
        ((consistency * 0.5) + ((avgTx / 600.0) * 50.0)).roundToInt().toDouble()
    ).roundToInt()

    // Step 4: Calculate loan limit and goodwill score
    val loanLimit = (totalInflow * 0.3).roundToInt()
    val goodwillScore = Random.nextInt(40, 91)

    onProgress("Creating borrower profile...")

    // Step 5: Create borrower document
    val borrowerName = upiId.split("@")[0].replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

    val borrowerData = hashMapOf(
        "name" to borrowerName,
        "upi_id" to upiId,
        "sarral_score" to score,
        "loan_limit" to loanLimit,
        "goodwill_score" to goodwillScore,
        "active_loan" to false,
        "created_at" to Timestamp.now(),
        "total_inflow" to totalInflow.roundToInt(),
        "total_transactions" to totalTransactions,
        "avg_transaction" to avgTx.roundToInt()
    )

    firestore.collection("borrowers")
        .document(upiId)
        .set(borrowerData)
        .await()
}

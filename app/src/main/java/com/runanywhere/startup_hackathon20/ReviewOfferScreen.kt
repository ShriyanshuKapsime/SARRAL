package com.runanywhere.startup_hackathon20

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewOfferScreen(
    lenderUid: String,
    lenderName: String,
    amount: Int,
    interest: Double,
    tenureMonths: Int,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Validate parameters on screen load
    var hasValidationError by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        when {
            lenderUid.isBlank() -> {
                hasValidationError = true
                validationMessage = "Missing lender information"
            }

            lenderName.isBlank() -> {
                hasValidationError = true
                validationMessage = "Missing lender name"
            }

            amount <= 0 -> {
                hasValidationError = true
                validationMessage = "Invalid loan amount"
            }

            interest < 0 -> {
                hasValidationError = true
                validationMessage = "Invalid interest rate"
            }

            tenureMonths <= 0 -> {
                hasValidationError = true
                validationMessage = "Invalid tenure"
            }
        }
    }

    // Calculate loan details
    val totalInterest = (amount * (interest / 100)).roundToInt()
    val totalRepayable = amount + totalInterest
    val tenureDays = tenureMonths * 30  // Convert months to days (30 days per month)
    val dailyEmi = if (tenureDays > 0) {
        (totalRepayable.toDouble() / tenureDays).roundToInt()
    } else {
        0
    }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Loan Offer") },
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
        if (hasValidationError) {
            // Show validation error screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Invalid Parameters",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = validationMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Loan Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Loan Details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            thickness = 2.5.dp,
                            color = androidx.compose.ui.graphics.Color(0xCC33B6FF) // #33B6FF with 80% opacity
                        )

                        DetailRow(label = "Lender", value = lenderName)
                        DetailRow(label = "Loan Amount", value = "₹${String.format("%,d", amount)}")
                        DetailRow(label = "Interest Rate", value = "$interest%")
                        DetailRow(
                            label = "Tenure",
                            value = "$tenureMonths months ($tenureDays days)"
                        )
                    }
                }

                // Calculated Values Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Repayment Breakdown",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            thickness = 2.5.dp,
                            color = androidx.compose.ui.graphics.Color(0xCC33B6FF) // #33B6FF with 80% opacity
                        )

                        DetailRow(
                            label = "Total Interest",
                            value = "₹${String.format("%,d", totalInterest)}",
                            highlighted = true
                        )
                        DetailRow(
                            label = "Total Amount to Repay",
                            value = "₹${String.format("%,d", totalRepayable)}",
                            highlighted = true
                        )
                        DetailRow(
                            label = "Daily EMI via UPI AutoPay",
                            value = "₹${String.format("%,d", dailyEmi)}",
                            highlighted = true
                        )
                    }
                }

                // Footer
                Text(
                    text = "Powered by RunAnywhere Smart Financial Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Error Message
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confirm Button
                Button(
                    onClick = {
                        val currentUser = auth.currentUser
                        if (currentUser == null) {
                            errorMessage = "User not authenticated"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        // Create loan request document
                        val loanRequest = hashMapOf(
                            "borrower_uid" to currentUser.uid,
                            "lender_uid" to lenderUid,
                            "lender_name" to lenderName,
                            "amount" to amount,
                            "interest" to interest,
                            "tenure_months" to tenureMonths,
                            "tenure_days" to tenureDays,
                            "total_repayable" to totalRepayable,
                            "daily_emi" to dailyEmi,
                            "status" to "pending",
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        firestore.collection("loan_requests")
                            .add(loanRequest)
                            .addOnSuccessListener {
                                isLoading = false
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = "Failed to submit request: ${e.message}"
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Confirm Loan Request",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = if (highlighted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

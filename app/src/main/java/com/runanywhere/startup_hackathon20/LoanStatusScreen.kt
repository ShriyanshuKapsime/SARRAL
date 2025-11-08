package com.runanywhere.startup_hackathon20

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class LoanRequest(
    val id: String = "",
    val lenderName: String = "",
    val amount: Int = 0,
    val interest: Double = 0.0,
    val tenureMonths: Int = 0,
    val tenureDays: Int = 0,
    val status: String = "pending"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanStatusScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    var loanRequests by remember { mutableStateOf<List<LoanRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Fetch ALL loan requests
    LaunchedEffect(refreshTrigger) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        firestore.collection("loan_requests")
            .whereEqualTo("borrower_uid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                loanRequests = documents.mapNotNull { doc ->
                    try {
                        // Read tenure_days from document, or calculate from tenure_months
                        val tenureDaysFromDoc = doc.getLong("tenure_days")?.toInt() ?: 0
                        val tenureMonthsFromDoc = doc.getLong("tenure_months")?.toInt() ?: 0

                        // If tenure_days is 0 or missing, calculate from tenure_months
                        val finalTenureDays = if (tenureDaysFromDoc > 0) {
                            tenureDaysFromDoc
                        } else if (tenureMonthsFromDoc > 0) {
                            tenureMonthsFromDoc * 30  // Convert months to days
                        } else {
                            0
                        }

                        LoanRequest(
                            id = doc.id,
                            lenderName = doc.getString("lender_name") ?: "",
                            amount = doc.getLong("amount")?.toInt() ?: 0,
                            interest = doc.getDouble("interest") ?: 0.0,
                            tenureMonths = tenureMonthsFromDoc,
                            tenureDays = finalTenureDays,
                            status = doc.getString("status") ?: "pending"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load loan requests: ${e.message}"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Status") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                loanRequests.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No loan requests found",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        item {
                            Text(
                                text = "Your Loan Requests",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Loan Request Cards
                        items(loanRequests) { request ->
                            LoanRequestCard(
                                request = request,
                                onWithdraw = {
                                    // Delete the request
                                    firestore.collection("loan_requests")
                                        .document(request.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "Loan request withdrawn.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Refresh the list
                                            refreshTrigger++
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Failed to withdraw request: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoanRequestCard(
    request: LoanRequest,
    onWithdraw: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Lender Name and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.lenderName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (request.status) {
                        "pending" -> MaterialTheme.colorScheme.secondaryContainer
                        "approved" -> MaterialTheme.colorScheme.primaryContainer
                        "rejected" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = request.status.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = when (request.status) {
                            "pending" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "approved" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "rejected" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider()

            // Loan Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%,d", request.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Interest",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${request.interest}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Tenure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tenure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (request.tenureMonths > 0) {
                        "${request.tenureMonths} months (${request.tenureDays} days)"
                    } else {
                        "${request.tenureDays} days"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Withdraw Button (only for pending requests)
            if (request.status == "pending") {
                Button(
                    onClick = onWithdraw,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Withdraw Request")
                }
            }
        }
    }
}

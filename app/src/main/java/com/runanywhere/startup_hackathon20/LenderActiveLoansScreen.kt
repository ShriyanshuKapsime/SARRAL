package com.runanywhere.startup_hackathon20

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ActiveLoan(
    val id: String = "",
    val borrowerUid: String = "",
    val borrowerName: String = "",
    val amount: Int = 0,
    val interestRate: Double = 0.0,
    val tenureMonths: Int = 0,
    val status: String = "ongoing"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenderActiveLoansScreen(
    onNavigateBack: () -> Unit
) {
    var activeLoans by remember { mutableStateOf<List<ActiveLoan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        firestore.collection("active_loans")
            .whereEqualTo("lender_uid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val loansList = mutableListOf<ActiveLoan>()
                val borrowerUids = documents.mapNotNull { doc ->
                    doc.getString("borrower_uid")
                }.distinct()

                if (borrowerUids.isEmpty()) {
                    activeLoans = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }

                // Fetch borrower names
                val borrowerNames = mutableMapOf<String, String>()
                var fetchedCount = 0

                borrowerUids.forEach { borrowerUid ->
                    firestore.collection("user_profiles")
                        .document(borrowerUid)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            borrowerNames[borrowerUid] = userDoc.getString("name")
                                ?: userDoc.getString("email")?.substringBefore("@")
                                        ?: "Unknown User"

                            fetchedCount++
                            if (fetchedCount == borrowerUids.size) {
                                documents.forEach { doc ->
                                    try {
                                        val borrowerUid = doc.getString("borrower_uid") ?: ""
                                        loansList.add(
                                            ActiveLoan(
                                                id = doc.id,
                                                borrowerUid = borrowerUid,
                                                borrowerName = borrowerNames[borrowerUid]
                                                    ?: "Unknown User",
                                                amount = doc.getLong("amount")?.toInt() ?: 0,
                                                interestRate = doc.getDouble("interest_rate_total")
                                                    ?: doc.getDouble("interest") ?: 0.0,
                                                tenureMonths = doc.getLong("tenure_months")?.toInt()
                                                    ?: 0,
                                                status = doc.getString("status") ?: "ongoing"
                                            )
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "LenderActiveLoans",
                                            "Error parsing loan",
                                            e
                                        )
                                    }
                                }
                                activeLoans = loansList
                                isLoading = false
                            }
                        }
                        .addOnFailureListener {
                            borrowerNames[borrowerUid] = "Unknown User"
                            fetchedCount++
                            if (fetchedCount == borrowerUids.size) {
                                documents.forEach { doc ->
                                    try {
                                        val borrowerUid = doc.getString("borrower_uid") ?: ""
                                        loansList.add(
                                            ActiveLoan(
                                                id = doc.id,
                                                borrowerUid = borrowerUid,
                                                borrowerName = borrowerNames[borrowerUid]
                                                    ?: "Unknown User",
                                                amount = doc.getLong("amount")?.toInt() ?: 0,
                                                interestRate = doc.getDouble("interest_rate_total")
                                                    ?: doc.getDouble("interest") ?: 0.0,
                                                tenureMonths = doc.getLong("tenure_months")?.toInt()
                                                    ?: 0,
                                                status = doc.getString("status") ?: "ongoing"
                                            )
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "LenderActiveLoans",
                                            "Error parsing loan",
                                            e
                                        )
                                    }
                                }
                                activeLoans = loansList
                                isLoading = false
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load active loans: ${e.message}"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Loans") },
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .align(Alignment.Center),
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

                activeLoans.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No active loans found",
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
                        item {
                            Text(
                                text = "Your Active Loans Portfolio",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(activeLoans) { loan ->
                            ActiveLoanCard(loan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveLoanCard(loan: ActiveLoan) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = loan.borrowerName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = loan.status.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Loan Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%,d", loan.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Interest Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${loan.interestRate}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remaining Duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${loan.tenureMonths} months",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

package com.runanywhere.startup_hackathon20

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class LoanRequestData(
    val id: String = "",
    val lenderName: String = "",
    val amount: Int = 0,
    val interest: Double = 0.0,
    val tenureDays: Int = 0,
    val totalRepayable: Int = 0,
    val dailyEmi: Int = 0,
    val status: String = "pending"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowerProfileOverviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    var loanRequest by remember { mutableStateOf<LoanRequestData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Fetch loan requests
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        firestore.collection("loan_requests")
            .whereEqualTo("borrower_uid", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                isLoading = false
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    loanRequest = LoanRequestData(
                        id = doc.id,
                        lenderName = doc.getString("lender_name") ?: "",
                        amount = doc.getLong("amount")?.toInt() ?: 0,
                        interest = doc.getDouble("interest") ?: 0.0,
                        tenureDays = doc.getLong("tenure_days")?.toInt() ?: 0,
                        totalRepayable = doc.getLong("total_repayable")?.toInt() ?: 0,
                        dailyEmi = doc.getLong("daily_emi")?.toInt() ?: 0,
                        status = doc.getString("status") ?: "pending"
                    )
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Failed to load loan requests: ${e.message}"
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

                loanRequest == null -> {
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Text(
                            text = "Your Latest Loan Request",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Loan Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InfoRow("Lender Name", loanRequest!!.lenderName)
                                InfoRow("Amount", "₹${String.format("%,d", loanRequest!!.amount)}")
                                InfoRow("Tenure", "${loanRequest!!.tenureDays} days")
                                InfoRow("Interest", "${loanRequest!!.interest}%")
                                InfoRow(
                                    "Total Repayable",
                                    "₹${String.format("%,d", loanRequest!!.totalRepayable)}"
                                )
                                InfoRow(
                                    "Daily EMI",
                                    "₹${String.format("%,d", loanRequest!!.dailyEmi)}"
                                )
                            }
                        }

                        // Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when (loanRequest!!.status) {
                                    "pending" -> MaterialTheme.colorScheme.secondaryContainer
                                    "approved" -> MaterialTheme.colorScheme.primaryContainer
                                    "rejected" -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Status",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = loanRequest!!.status.uppercase(),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = when (loanRequest!!.status) {
                                        "pending" -> "Awaiting lender approval."
                                        "approved" -> "Loan Approved."
                                        "rejected" -> "Request Rejected."
                                        else -> "Unknown status"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
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
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

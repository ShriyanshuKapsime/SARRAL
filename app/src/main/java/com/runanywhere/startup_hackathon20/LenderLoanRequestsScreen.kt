package com.runanywhere.startup_hackathon20

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.google.firebase.Timestamp
import java.util.Date

data class LenderLoanRequest(
    val id: String = "",
    val borrowerUid: String = "",
    val borrowerName: String = "",
    val amount: Int = 0,
    val interestRateTotal: Double = 0.0,
    val tenureMonths: Int = 0,
    val tenureDays: Int = 0,
    val status: String = "pending",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenderLoanRequestsScreen(
    onNavigateBack: () -> Unit
) {
    var loanRequests by remember { mutableStateOf<List<LenderLoanRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var processingRequestId by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Fetch ALL loan requests addressed to this lender
    LaunchedEffect(refreshTrigger) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        firestore.collection("loan_requests")
            .whereEqualTo("lender_uid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                // Sort manually by timestamp in descending order after fetching
                val sortedDocuments = documents.documents.sortedByDescending { doc ->
                    doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                }

                val requestsList = mutableListOf<LenderLoanRequest>()
                val borrowerUids = sortedDocuments.mapNotNull { doc ->
                    doc.getString("borrower_uid")
                }.distinct()

                if (borrowerUids.isEmpty()) {
                    loanRequests = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }

                // Fetch borrower names from user_profiles
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
                                // All borrower names fetched, now create the loan requests list
                                requestsList.clear()
                                sortedDocuments.forEach { doc ->
                                    try {
                                        val borrowerUid = doc.getString("borrower_uid") ?: ""
                                        val tenureDaysFromDoc =
                                            doc.getLong("tenure_days")?.toInt() ?: 0
                                        val tenureMonthsFromDoc =
                                            doc.getLong("tenure_months")?.toInt() ?: 0

                                        // Calculate tenureDays if not present
                                        val finalTenureDays = if (tenureDaysFromDoc > 0) {
                                            tenureDaysFromDoc
                                        } else if (tenureMonthsFromDoc > 0) {
                                            tenureMonthsFromDoc * 30
                                        } else {
                                            0
                                        }

                                        requestsList.add(
                                            LenderLoanRequest(
                                                id = doc.id,
                                                borrowerUid = borrowerUid,
                                                borrowerName = borrowerNames[borrowerUid]
                                                    ?: "Unknown User",
                                                amount = doc.getLong("amount")?.toInt() ?: 0,
                                                interestRateTotal = doc.getDouble("interest_rate_total")
                                                    ?: doc.getDouble("interest") ?: 0.0,
                                                tenureMonths = tenureMonthsFromDoc,
                                                tenureDays = finalTenureDays,
                                                status = doc.getString("status") ?: "pending",
                                                timestamp = doc.getTimestamp("timestamp")
                                            )
                                        )
                                    } catch (e: Exception) {
                                        // Skip malformed documents
                                    }
                                }
                                loanRequests = requestsList
                                isLoading = false
                            }
                        }
                        .addOnFailureListener {
                            borrowerNames[borrowerUid] = "Unknown User"
                            fetchedCount++
                            if (fetchedCount == borrowerUids.size) {
                                requestsList.clear()
                                sortedDocuments.forEach { doc ->
                                    try {
                                        val borrowerUid = doc.getString("borrower_uid") ?: ""
                                        val tenureDaysFromDoc =
                                            doc.getLong("tenure_days")?.toInt() ?: 0
                                        val tenureMonthsFromDoc =
                                            doc.getLong("tenure_months")?.toInt() ?: 0

                                        val finalTenureDays = if (tenureDaysFromDoc > 0) {
                                            tenureDaysFromDoc
                                        } else if (tenureMonthsFromDoc > 0) {
                                            tenureMonthsFromDoc * 30
                                        } else {
                                            0
                                        }

                                        requestsList.add(
                                            LenderLoanRequest(
                                                id = doc.id,
                                                borrowerUid = borrowerUid,
                                                borrowerName = borrowerNames[borrowerUid]
                                                    ?: "Unknown User",
                                                amount = doc.getLong("amount")?.toInt() ?: 0,
                                                interestRateTotal = doc.getDouble("interest_rate_total")
                                                    ?: doc.getDouble("interest") ?: 0.0,
                                                tenureMonths = tenureMonthsFromDoc,
                                                tenureDays = finalTenureDays,
                                                status = doc.getString("status") ?: "pending",
                                                timestamp = doc.getTimestamp("timestamp")
                                            )
                                        )
                                    } catch (e: Exception) {
                                        // Skip malformed documents
                                    }
                                }
                                loanRequests = requestsList
                                isLoading = false
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load loan requests: ${e.message}"
                android.util.Log.e(
                    "LenderRequests",
                    "Query failed. You may need to create a composite index in Firestore for lender_uid and timestamp.",
                    e
                )
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Requests") },
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
                                text = "Loan Requests for You",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Loan Request Cards
                        items(loanRequests) { request ->
                            LenderLoanRequestCard(
                                request = request,
                                isProcessing = processingRequestId == request.id,
                                onApprove = {
                                    processingRequestId = request.id

                                    // Update status to approved
                                    firestore.collection("loan_requests")
                                        .document(request.id)
                                        .update("status", "approved")
                                        .addOnSuccessListener {
                                            // Create entry in active_loans
                                            val currentUser = auth.currentUser
                                            if (currentUser != null) {
                                                val activeLoan = hashMapOf(
                                                    "borrower_uid" to request.borrowerUid,
                                                    "lender_uid" to currentUser.uid,
                                                    "amount" to request.amount,
                                                    "interest_rate_total" to request.interestRateTotal,
                                                    "tenure_months" to request.tenureMonths,
                                                    "start_date" to Timestamp.now(),
                                                    "status" to "ongoing"
                                                )

                                                firestore.collection("active_loans")
                                                    .add(activeLoan)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(
                                                            context,
                                                            "Loan approved.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        processingRequestId = null
                                                        refreshTrigger++
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to create active loan: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        processingRequestId = null
                                                    }
                                            } else {
                                                processingRequestId = null
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Failed to approve request: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            processingRequestId = null
                                        }
                                },
                                onReject = {
                                    processingRequestId = request.id

                                    // Update status to rejected
                                    firestore.collection("loan_requests")
                                        .document(request.id)
                                        .update("status", "rejected")
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "Loan rejected.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            processingRequestId = null
                                            refreshTrigger++
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Failed to reject request: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            processingRequestId = null
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
fun LenderLoanRequestCard(
    request: LenderLoanRequest,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
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
            // Borrower Name and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.borrowerName,
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
                        text = "${request.interestRateTotal}%",
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

            // Approve and Reject Buttons (only for pending requests)
            if (request.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Reject")
                        }
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Approve")
                        }
                    }
                }
            }
        }
    }
}

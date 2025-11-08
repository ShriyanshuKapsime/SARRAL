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

data class ListedOffer(
    val id: String = "",
    val amount: Int = 0,
    val interestRateTotal: Double = 0.0,
    val tenureMonths: Int = 0,
    val status: String = "available"
)

data class ActiveLoanItem(
    val id: String = "",
    val borrowerUid: String = "",
    val borrowerName: String = "",
    val amount: Int = 0,
    val interestRateTotal: Double = 0.0,
    val tenureMonths: Int = 0,
    val status: String = "ongoing"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LenderListedLoansScreen(
    onNavigateBack: () -> Unit
) {
    var listedOffers by remember { mutableStateOf<List<ListedOffer>>(emptyList()) }
    var activeLoans by remember { mutableStateOf<List<ActiveLoanItem>>(emptyList()) }
    var isLoadingOffers by remember { mutableStateOf(true) }
    var isLoadingActiveLoans by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var processingOfferId by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Fetch Listed Offers (only available status)
    LaunchedEffect(refreshTrigger) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage = "User not authenticated"
            isLoadingOffers = false
            return@LaunchedEffect
        }

        isLoadingOffers = true
        firestore.collection("offers")
            .whereEqualTo("lender_uid", currentUser.uid)
            .whereEqualTo("status", "available")
            .get()
            .addOnSuccessListener { documents ->
                val offersList = documents.mapNotNull { doc ->
                    try {
                        ListedOffer(
                            id = doc.id,
                            amount = doc.getLong("amount")?.toInt() ?: 0,
                            interestRateTotal = doc.getDouble("interest_rate_total") ?: 0.0,
                            tenureMonths = doc.getLong("tenure_months")?.toInt() ?: 0,
                            status = doc.getString("status") ?: "available"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                listedOffers = offersList
                isLoadingOffers = false
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load listed offers: ${e.message}"
                isLoadingOffers = false
            }
    }

    // Fetch Active Loans
    LaunchedEffect(refreshTrigger) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            isLoadingActiveLoans = false
            return@LaunchedEffect
        }

        isLoadingActiveLoans = true
        firestore.collection("active_loans")
            .whereEqualTo("lender_uid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val borrowerUids = documents.mapNotNull { doc ->
                    doc.getString("borrower_uid")
                }.distinct()

                if (borrowerUids.isEmpty()) {
                    activeLoans = emptyList()
                    isLoadingActiveLoans = false
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
                                val loansList = documents.mapNotNull { doc ->
                                    try {
                                        val borrowerUid = doc.getString("borrower_uid") ?: ""
                                        ActiveLoanItem(
                                            id = doc.id,
                                            borrowerUid = borrowerUid,
                                            borrowerName = borrowerNames[borrowerUid]
                                                ?: "Unknown User",
                                            amount = doc.getLong("amount")?.toInt() ?: 0,
                                            interestRateTotal = doc.getDouble("interest_rate_total")
                                                ?: doc.getDouble("interest") ?: 0.0,
                                            tenureMonths = doc.getLong("tenure_months")?.toInt()
                                                ?: 0,
                                            status = doc.getString("status") ?: "ongoing"
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                activeLoans = loansList
                                isLoadingActiveLoans = false
                            }
                        }
                        .addOnFailureListener {
                            borrowerNames[borrowerUid] = "Unknown User"
                            fetchedCount++
                            if (fetchedCount == borrowerUids.size) {
                                val loansList = documents.mapNotNull { doc ->
                                    try {
                                        val borrowerUid = doc.getString("borrower_uid") ?: ""
                                        ActiveLoanItem(
                                            id = doc.id,
                                            borrowerUid = borrowerUid,
                                            borrowerName = borrowerNames[borrowerUid]
                                                ?: "Unknown User",
                                            amount = doc.getLong("amount")?.toInt() ?: 0,
                                            interestRateTotal = doc.getDouble("interest_rate_total")
                                                ?: doc.getDouble("interest") ?: 0.0,
                                            tenureMonths = doc.getLong("tenure_months")?.toInt()
                                                ?: 0,
                                            status = doc.getString("status") ?: "ongoing"
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                activeLoans = loansList
                                isLoadingActiveLoans = false
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load active loans: ${e.message}"
                isLoadingActiveLoans = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listed Loans") },
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
                isLoadingOffers || isLoadingActiveLoans -> {
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

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Section A: Live Offers
                        item {
                            Text(
                                text = "Live Offers",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (listedOffers.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "No live offers found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(listedOffers) { offer ->
                                LiveOfferCard(
                                    offer = offer,
                                    isProcessing = processingOfferId == offer.id,
                                    onRemove = {
                                        processingOfferId = offer.id
                                        firestore.collection("offers")
                                            .document(offer.id)
                                            .update("status", "inactive")
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    context,
                                                    "Offer removed successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                processingOfferId = null
                                                refreshTrigger++
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(
                                                    context,
                                                    "Failed to remove offer: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                processingOfferId = null
                                            }
                                    }
                                )
                            }
                        }

                        // Spacer between sections
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Section B: Active Loans
                        item {
                            Text(
                                text = "Active Loans",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (activeLoans.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "No active loans found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(activeLoans) { loan ->
                                ActiveLoanCard(loan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveOfferCard(
    offer: ListedOffer,
    isProcessing: Boolean,
    onRemove: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Offer #${offer.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "LIVE",
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
                        text = "Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%,d", offer.amount)}",
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
                        text = "${offer.interestRateTotal}%",
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
                    text = "Tenure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${offer.tenureMonths} months",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Remove Offer Button
            Button(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
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
                    Text("Remove Offer")
                }
            }
        }
    }
}

@Composable
fun ActiveLoanCard(loan: ActiveLoanItem) {
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
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = loan.status.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                        text = "${loan.interestRateTotal}%",
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
                    text = "Tenure",
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

package com.runanywhere.startup_hackathon20

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import java.util.*
import kotlin.math.roundToInt

// Data class for loan offer
data class LoanOffer(
    val id: String = "",
    val lenderUid: String = "",
    val lenderName: String = "",
    val loanAmount: Int = 0,
    val interestRate: Double = 0.0,
    val tenureMonths: Int = 0
)

// Data class for UPI transaction
data class UPITransaction(
    val amount: Double,
    val timestamp: Date
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowerLoanDashboardScreen(
    onNavigateBack: () -> Unit,
    onRequestLoan: (LoanOffer) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var sarralScore by remember { mutableStateOf(0) }
    var loanLimit by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loanOffers by remember { mutableStateOf<List<LoanOffer>>(emptyList()) }
    var isLoadingOffers by remember { mutableStateOf(true) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Function to process offers without UIDs
    fun processOffersWithoutUid(
        offers: List<Pair<String, com.google.firebase.firestore.DocumentSnapshot>>,
        uidMap: Map<String, String>,
        resultList: MutableList<LoanOffer>
    ) {
        offers.forEach { (lenderName, doc) ->
            try {
                resultList.add(
                    LoanOffer(
                        id = doc.id,
                        lenderUid = uidMap[lenderName] ?: "unknown_lender",
                        lenderName = lenderName,
                        loanAmount = doc.getLong("amount")?.toInt() ?: 0,
                        interestRate = doc.getDouble("interest_rate_total") ?: 0.0,
                        tenureMonths = doc.getLong("tenure_months")?.toInt() ?: 0
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("OffersFetch", "Error parsing offer ${doc.id}", e)
            }
        }
        loanOffers = resultList
        isLoadingOffers = false
        android.util.Log.d("OffersFetch", "Final offers loaded: ${resultList.size}")
    }

    // Fetch loan offers from Firestore
    LaunchedEffect(Unit) {
        isLoadingOffers = true

        firestore.collection("offers")
            .get()
            .addOnSuccessListener { offersSnapshot ->
                android.util.Log.d("OffersFetch", "Found ${offersSnapshot.documents.size} offers")
                
                if (offersSnapshot.documents.isEmpty()) {
                    loanOffers = emptyList()
                    isLoadingOffers = false
                    return@addOnSuccessListener
                }
                
                val offersList = mutableListOf<LoanOffer>()
                
                // First, check if offers have lender_uid directly in them
                val offersWithoutUid = mutableListOf<Pair<String, com.google.firebase.firestore.DocumentSnapshot>>()
                
                offersSnapshot.documents.forEach { doc ->
                    val lenderUid = doc.getString("lender_uid")
                    val lenderName = doc.getString("lender_name") ?: ""
                    
                    if (!lenderUid.isNullOrBlank()) {
                        // Offer has lender_uid, use it directly
                        try {
                            offersList.add(
                                LoanOffer(
                                    id = doc.id,
                                    lenderUid = lenderUid,
                                    lenderName = lenderName,
                                    loanAmount = doc.getLong("amount")?.toInt() ?: 0,
                                    interestRate = doc.getDouble("interest_rate_total") ?: 0.0,
                                    tenureMonths = doc.getLong("tenure_months")?.toInt() ?: 0
                                )
                            )
                            android.util.Log.d("OffersFetch", "Offer ${doc.id} has lender_uid: $lenderUid")
                        } catch (e: Exception) {
                            android.util.Log.e("OffersFetch", "Error parsing offer ${doc.id}", e)
                        }
                    } else {
                        // No lender_uid in offer, need to look up
                        offersWithoutUid.add(Pair(lenderName, doc))
                        android.util.Log.d("OffersFetch", "Offer ${doc.id} missing lender_uid, will lookup: $lenderName")
                    }
                }
                
                // If all offers have UIDs, we're done
                if (offersWithoutUid.isEmpty()) {
                    loanOffers = offersList
                    isLoadingOffers = false
                    android.util.Log.d("OffersFetch", "All offers loaded successfully: ${offersList.size}")
                    return@addOnSuccessListener
                }
                
                // For offers without UIDs, try to look up from user_profiles
                val lenderNames = offersWithoutUid.map { it.first }.distinct()
                val lenderUidMap = mutableMapOf<String, String>()
                var fetchedCount = 0

                lenderNames.forEach { lenderName ->
                    if (lenderName.isBlank()) {
                        lenderUidMap[lenderName] = "unknown_lender"
                        fetchedCount++
                        if (fetchedCount == lenderNames.size) {
                            processOffersWithoutUid(offersWithoutUid, lenderUidMap, offersList)
                        }
                        return@forEach
                    }
                    
                    firestore.collection("user_profiles")
                        .whereEqualTo("name", lenderName)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { profileDocs ->
                            if (!profileDocs.isEmpty) {
                                val uid = profileDocs.documents[0].id
                                lenderUidMap[lenderName] = uid
                                android.util.Log.d("OffersFetch", "Found UID for $lenderName: $uid")
                            } else {
                                // Generate a placeholder UID based on lender name
                                val placeholderUid = "lender_${lenderName.replace(" ", "_").lowercase()}"
                                lenderUidMap[lenderName] = placeholderUid
                                android.util.Log.w("OffersFetch", "No profile found for $lenderName, using placeholder: $placeholderUid")
                            }

                            fetchedCount++
                            if (fetchedCount == lenderNames.size) {
                                processOffersWithoutUid(offersWithoutUid, lenderUidMap, offersList)
                            }
                        }
                        .addOnFailureListener { e ->
                            // Use placeholder uid if lookup fails
                            val placeholderUid = "lender_${lenderName.replace(" ", "_").lowercase()}"
                            lenderUidMap[lenderName] = placeholderUid
                            android.util.Log.e("OffersFetch", "Failed to lookup $lenderName, using placeholder: $placeholderUid", e)
                            
                            fetchedCount++
                            if (fetchedCount == lenderNames.size) {
                                processOffersWithoutUid(offersWithoutUid, lenderUidMap, offersList)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("OffersFetch", "Failed to fetch offers", e)
                isLoadingOffers = false
            }
    }

    // Fetch UPI ID and calculate SARRAL score
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        // Step 1: Fetch user's UPI ID from user_profiles
        firestore.collection("user_profiles")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { profileDoc ->
                val upiId = profileDoc.getString("upi_id")

                if (upiId == null) {
                    errorMessage = "UPI ID not found. Please enter your UPI details first."
                    isLoading = false
                    return@addOnSuccessListener
                }

                // Step 2: Query transactions for the last 180 days
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -180)
                val startDate = calendar.time

                firestore.collection("transactions")
                    .whereEqualTo("borrower_upi", upiId)
                    .whereGreaterThanOrEqualTo(
                        "timestamp",
                        com.google.firebase.Timestamp(startDate)
                    )
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { transactionDocs ->
                        // Step 3: Parse transactions
                        val transactions = transactionDocs.documents.mapNotNull { doc ->
                            val amount = doc.getDouble("amount")
                            val timestamp = doc.getTimestamp("timestamp")?.toDate()
                            if (amount != null && timestamp != null) {
                                UPITransaction(amount, timestamp)
                            } else null
                        }

                        if (transactions.isEmpty()) {
                            // No transactions found - use default values
                            sarralScore = 0
                            loanLimit = 0
                            errorMessage =
                                "No transaction history found. Unable to calculate SARRAL score."
                            isLoading = false
                            return@addOnSuccessListener
                        }

                        // Step 4: Group transactions by calendar month
                        val monthlyTotals = mutableMapOf<String, Double>()
                        val monthFormat = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault())

                        transactions.forEach { transaction ->
                            val monthKey = monthFormat.format(transaction.timestamp)
                            monthlyTotals[monthKey] =
                                (monthlyTotals[monthKey] ?: 0.0) + transaction.amount
                        }

                        // Step 5: Calculate monthly_inflow (average of all monthly totals)
                        val monthlyInflow = if (monthlyTotals.isNotEmpty()) {
                            monthlyTotals.values.sum() / 6.0 // Divide by 6 months (as per requirement)
                        } else {
                            0.0
                        }

                        // Step 6: Calculate income_score
                        var incomeScore = (monthlyInflow / 60000.0) * 100.0
                        if (incomeScore > 100.0) {
                            incomeScore = 100.0
                        }

                        // Step 7: Find max and min months
                        val maxMonth = monthlyTotals.values.maxOrNull() ?: 0.0
                        val minMonth = monthlyTotals.values.minOrNull() ?: 0.0

                        // Step 8: Calculate consistency score
                        var consistencyScore = if (maxMonth > 0) {
                            100.0 - ((maxMonth - minMonth) / maxMonth * 100.0)
                        } else {
                            0.0
                        }

                        // Apply bounds to consistency_score
                        if (consistencyScore < 0.0) {
                            consistencyScore = 0.0
                        }
                        if (consistencyScore > 100.0) {
                            consistencyScore = 100.0
                        }

                        // Step 9: Calculate SARRAL score (income 30%, consistency 70%)
                        val calculatedSarralScore = (incomeScore * 0.3) + (consistencyScore * 0.7)

                        // Step 10: Calculate loan limit (30% of monthly inflow)
                        val calculatedLoanLimit = monthlyInflow * 0.30

                        // Round the values
                        val roundedSarralScore = calculatedSarralScore.roundToInt()
                        val roundedLoanLimit = calculatedLoanLimit.roundToInt()

                        // Update user profile in Firestore with calculated scores
                        val profileUpdates = hashMapOf<String, Any>(
                            "sarral_score" to roundedSarralScore,
                            "loan_limit" to roundedLoanLimit,
                            "last_score_update" to com.google.firebase.Timestamp.now()
                        )

                        firestore.collection("user_profiles")
                            .document(currentUser.uid)
                            .update(profileUpdates)
                            .addOnSuccessListener {
                                // Update state after successful Firestore update
                                sarralScore = roundedSarralScore
                                loanLimit = roundedLoanLimit
                                isLoading = false
                            }
                            .addOnFailureListener { e ->
                                // Even if Firestore update fails, show the calculated values
                                sarralScore = roundedSarralScore
                                loanLimit = roundedLoanLimit
                                isLoading = false
                                // Log the error but don't show to user since calculation succeeded
                            }
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Failed to fetch transactions: ${e.message}"
                        isLoading = false
                    }
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to fetch user profile: ${e.message}"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Dashboard") },
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
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
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyLarge,
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SARRAL Score Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title
                            Text(
                                text = "Your SARRAL Score",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Large Score
                            Text(
                                text = "$sarralScore/100",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 64.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Loan Limit
                            Text(
                                text = "Loan Limit Available: ₹${String.format("%,d", loanLimit)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Loan Marketplace Title
                item {
                    Text(
                        text = "Loan Marketplace",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Loading offers indicator
                if (isLoadingOffers) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (loanOffers.isEmpty()) {
                    // Empty state
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF151718)
                                    .copy(alpha = 0.95f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No offers available at the moment. Please check back later.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = androidx.compose.ui.graphics.Color(0xFFB0B0B0),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Loan Offers List
                    items(loanOffers) { offer ->
                        LoanOfferCard(
                            offer = offer,
                            onRequestLoan = { onRequestLoan(offer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoanOfferCard(
    offer: LoanOffer,
    onRequestLoan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp,
            focusedElevation = 4.dp
        ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFF151718).copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Lender Name
            Text(
                text = offer.lenderName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = androidx.compose.ui.graphics.Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loan Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Loan Amount
                Column {
                    Text(
                        text = "Loan Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFFB0B0B0)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%,d", offer.loanAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }

                // Interest Rate
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Interest Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFFB0B0B0)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${offer.interestRate}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tenure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tenure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFFB0B0B0)
                )
                Text(
                    text = "${offer.tenureMonths} months",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = androidx.compose.ui.graphics.Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Request Loan Button with shadow effect
            Button(
                onClick = onRequestLoan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                    hoveredElevation = 6.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Request Loan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

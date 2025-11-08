package com.runanywhere.startup_hackathon20

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val upiId: String = "",
    val role: String = "borrower",
    val sarralScore: Int = 0,
    val goodwillScore: Int = 0,
    val loanLimit: Int = 0
)

data class ActiveLoanDetail(
    val id: String = "",
    val borrowerName: String = "",
    val amount: Int = 0,
    val interestRateTotal: Double = 0.0,
    val tenureMonths: Int = 0,
    val startDate: Timestamp? = null,
    val status: String = "ongoing"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLoanStatus: () -> Unit = {},
    onNavigateToLenderRequests: () -> Unit = {}
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loanCount by remember { mutableStateOf(0) }
    var lendedLoanCount by remember { mutableStateOf(0) }
    var isSwitchingRole by remember { mutableStateOf(false) }
    var activeLoansDetails by remember { mutableStateOf<List<ActiveLoanDetail>>(emptyList()) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Fetch user profile and loan count
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        // Fetch user profile
        firestore.collection("user_profiles")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userProfile = UserProfile(
                        name = currentUser.displayName ?: "User",
                        email = currentUser.email ?: "",
                        upiId = doc.getString("upi_id") ?: "",
                        role = doc.getString("role") ?: "borrower",
                        sarralScore = doc.getLong("sarral_score")?.toInt() ?: 0,
                        goodwillScore = doc.getLong("goodwill_score")?.toInt() ?: 0,
                        loanLimit = doc.getLong("loan_limit")?.toInt() ?: 0
                    )

                    val currentRole = doc.getString("role") ?: "borrower"

                    // Fetch loan counts based on role
                    if (currentRole == "borrower") {
                        // Fetch loan request count for borrower
                        firestore.collection("loan_requests")
                            .whereEqualTo("borrower_uid", currentUser.uid)
                            .get()
                            .addOnSuccessListener { loans ->
                                loanCount = loans.size()
                                isLoading = false
                            }
                            .addOnFailureListener {
                                isLoading = false
                            }
                    } else {
                        // Fetch active loans with details for lender
                        firestore.collection("active_loans")
                            .whereEqualTo("lender_uid", currentUser.uid)
                            .whereEqualTo("status", "ongoing")
                            .get()
                            .addOnSuccessListener { loans ->
                                lendedLoanCount = loans.size()

                                // Fetch borrower names for each loan
                                val borrowerUids = loans.documents.mapNotNull {
                                    it.getString("borrower_uid")
                                }.distinct()

                                if (borrowerUids.isNotEmpty()) {
                                    val borrowerNames = mutableMapOf<String, String>()
                                    var fetchedCount = 0

                                    borrowerUids.forEach { borrowerUid ->
                                        firestore.collection("user_profiles")
                                            .document(borrowerUid)
                                            .get()
                                            .addOnSuccessListener { userDoc ->
                                                borrowerNames[borrowerUid] =
                                                    userDoc.getString("name")
                                                        ?: userDoc.getString("email")
                                                            ?.substringBefore("@")
                                                                ?: "Unknown User"

                                                fetchedCount++
                                                if (fetchedCount == borrowerUids.size) {
                                                    // All names fetched, create loan details list
                                                    val loansList =
                                                        loans.documents.mapNotNull { doc ->
                                                            try {
                                                                val borrowerUid =
                                                                    doc.getString("borrower_uid")
                                                                        ?: ""
                                                                ActiveLoanDetail(
                                                                    id = doc.id,
                                                                    borrowerName = borrowerNames[borrowerUid]
                                                                        ?: "Unknown User",
                                                                    amount = doc.getLong("amount")
                                                                        ?.toInt() ?: 0,
                                                                    interestRateTotal = doc.getDouble(
                                                                        "interest_rate_total"
                                                                    )
                                                                        ?: doc.getDouble("interest")
                                                                        ?: 0.0,
                                                                    tenureMonths = doc.getLong("tenure_months")
                                                                        ?.toInt() ?: 0,
                                                                    startDate = doc.getTimestamp("start_date"),
                                                                    status = doc.getString("status")
                                                                        ?: "ongoing"
                                                                )
                                                            } catch (e: Exception) {
                                                                null
                                                            }
                                                        }
                                                    activeLoansDetails = loansList
                                                    isLoading = false
                                                }
                                            }
                                            .addOnFailureListener {
                                                borrowerNames[borrowerUid] = "Unknown User"
                                                fetchedCount++
                                                if (fetchedCount == borrowerUids.size) {
                                                    val loansList =
                                                        loans.documents.mapNotNull { doc ->
                                                            try {
                                                                val borrowerUid =
                                                                    doc.getString("borrower_uid")
                                                                        ?: ""
                                                                ActiveLoanDetail(
                                                                    id = doc.id,
                                                                    borrowerName = borrowerNames[borrowerUid]
                                                                        ?: "Unknown User",
                                                                    amount = doc.getLong("amount")
                                                                        ?.toInt() ?: 0,
                                                                    interestRateTotal = doc.getDouble(
                                                                        "interest_rate_total"
                                                                    )
                                                                        ?: doc.getDouble("interest")
                                                                        ?: 0.0,
                                                                    tenureMonths = doc.getLong("tenure_months")
                                                                        ?.toInt() ?: 0,
                                                                    startDate = doc.getTimestamp("start_date"),
                                                                    status = doc.getString("status")
                                                                        ?: "ongoing"
                                                                )
                                                            } catch (e: Exception) {
                                                                null
                                                            }
                                                        }
                                                    activeLoansDetails = loansList
                                                    isLoading = false
                                                }
                                            }
                                    }
                                } else {
                                    isLoading = false
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                            }
                    }
                } else {
                    // Create default profile if doesn't exist
                    userProfile = UserProfile(
                        name = currentUser.displayName ?: "User",
                        email = currentUser.email ?: "",
                        upiId = "",
                        role = "borrower"
                    )
                    isLoading = false
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load profile: ${e.message}"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Profile")
                    }
                },
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
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar and Basic Info Card
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Circular Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile?.name?.firstOrNull()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                ),
                                color = Color.White
                            )
                        }

                        // Name
                        Text(
                            text = userProfile?.name ?: "User",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Email
                        Text(
                            text = userProfile?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // UPI ID
                        if (!userProfile?.upiId.isNullOrEmpty()) {
                            Text(
                                text = "UPI: ${userProfile?.upiId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Role Badge
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = userProfile?.role?.uppercase() ?: "BORROWER",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Role-specific Information
                if (userProfile?.role == "borrower") {
                    // Borrower-specific cards
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Credit Information",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "SARRAL Score",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${userProfile?.sarralScore ?: 0}/100",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Goodwill Score",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${userProfile?.goodwillScore ?: 0}/100",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Loan Limit",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "₹${String.format("%,d", userProfile?.loanLimit ?: 0)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Loans Requested Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Loans Requested",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = loanCount.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = onNavigateToLoanStatus,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View Loan Status")
                            }
                        }
                    }
                } else {
                    // Lender-specific cards
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Loans Lended",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = lendedLoanCount.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = onNavigateToLenderRequests,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View Requests to Approve")
                            }
                        }
                    }

                    // Loans Currently Lended Section
                    if (activeLoansDetails.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Loans Currently Lended",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )

                                activeLoansDetails.forEach { loan ->
                                    ActiveLoanDetailCard(loan = loan)
                                }
                            }
                        }
                    }
                }

                // Switch Role Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Switch Role",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = if (userProfile?.role == "borrower") {
                                "Switch to Lender to offer loans to others"
                            } else {
                                "Switch to Borrower to request loans"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Button(
                            onClick = {
                                val currentUser = auth.currentUser ?: return@Button
                                isSwitchingRole = true

                                // Check for active loans where user is borrower or lender
                                firestore.collection("active_loans")
                                    .whereEqualTo("borrower_uid", currentUser.uid)
                                    .whereEqualTo("status", "ongoing")
                                    .get()
                                    .addOnSuccessListener { borrowerLoans ->
                                        if (borrowerLoans.isEmpty) {
                                            firestore.collection("active_loans")
                                                .whereEqualTo("lender_uid", currentUser.uid)
                                                .whereEqualTo("status", "ongoing")
                                                .get()
                                                .addOnSuccessListener { lenderLoans ->
                                                    if (lenderLoans.isEmpty) {
                                                        // No active loans, proceed with role switch
                                                        val newRole =
                                                            if (userProfile?.role == "borrower") "lender" else "borrower"

                                                        firestore.collection("user_profiles")
                                                            .document(currentUser.uid)
                                                            .update("role", newRole)
                                                            .addOnSuccessListener {
                                                                userProfile =
                                                                    userProfile?.copy(role = newRole)
                                                                isSwitchingRole = false
                                                                Toast.makeText(
                                                                    context,
                                                                    "Role switched to $newRole",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                            .addOnFailureListener {
                                                                isSwitchingRole = false
                                                                Toast.makeText(
                                                                    context,
                                                                    "Failed to switch role",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                    } else {
                                                        isSwitchingRole = false
                                                        Toast.makeText(
                                                            context,
                                                            "Cannot switch roles while a loan is active",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    isSwitchingRole = false
                                                    Toast.makeText(
                                                        context,
                                                        "Error checking lender loans",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        } else {
                                            isSwitchingRole = false
                                            Toast.makeText(
                                                context,
                                                "Cannot switch roles while a loan is active",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        isSwitchingRole = false
                                        Toast.makeText(
                                            context,
                                            "Error checking borrower loans",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSwitchingRole
                        ) {
                            if (isSwitchingRole) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    if (userProfile?.role == "borrower") {
                                        "Switch to Lender"
                                    } else {
                                        "Switch to Borrower"
                                    }
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
fun ActiveLoanDetailCard(loan: ActiveLoanDetail) {
    // Calculate loan details
    val totalInterest = (loan.amount * (loan.interestRateTotal / 100)).roundToInt()
    val totalRepayable = loan.amount + totalInterest
    val dailyEmi = if (loan.tenureMonths > 0) {
        (totalRepayable.toDouble() / (loan.tenureMonths * 30)).roundToInt()
    } else {
        0
    }

    // Calculate days passed since start date
    val daysPassed = if (loan.startDate != null) {
        val startDateMillis = loan.startDate.toDate().time
        val currentDateMillis = Date().time
        val diffInMillis = currentDateMillis - startDateMillis
        TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
    } else {
        0
    }

    // Calculate remaining amount
    val amountPaid = (dailyEmi * daysPassed)
    val remainingAmount = (totalRepayable - amountPaid).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
            // Borrower Name Header
            Text(
                text = loan.borrowerName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider()

            // Loan Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Loan Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${String.format("%,d", loan.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Interest Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Interest Rate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%.2f", loan.interestRateTotal)}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Tenure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tenure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${loan.tenureMonths} months",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Days Passed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Days Passed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$daysPassed days",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            HorizontalDivider()

            // Key Financial Metrics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Remaining EMI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remaining EMI:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "₹${String.format("%,d", remainingAmount)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Daily EMI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily EMI:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "₹${String.format("%,d", dailyEmi)}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Amount To Receive Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Amount To Receive Total:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "₹${String.format("%,d", totalRepayable)}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

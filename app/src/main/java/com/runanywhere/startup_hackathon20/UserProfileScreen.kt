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

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val upiId: String = "",
    val role: String = "borrower",
    val sarralScore: Int = 0,
    val goodwillScore: Int = 0,
    val loanLimit: Int = 0
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
                        // Fetch lended loan count for lender (from active_loans)
                        firestore.collection("active_loans")
                            .whereEqualTo("lender_uid", currentUser.uid)
                            .get()
                            .addOnSuccessListener { loans ->
                                lendedLoanCount = loans.size()
                                isLoading = false
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

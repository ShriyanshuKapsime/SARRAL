package com.runanywhere.startup_hackathon20

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLoanOfferScreen(
    onNavigateBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var tenureMonths by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Loan Offer") },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Create a New Loan Offer",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Set your lending terms and make them available to borrowers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Loan Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    errorMessage = null
                },
                label = { Text("Loan Amount (₹)") },
                placeholder = { Text("e.g., 10000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            // Interest Rate Input
            OutlinedTextField(
                value = interestRate,
                onValueChange = {
                    interestRate = it
                    errorMessage = null
                },
                label = { Text("Interest Rate Total (%)") },
                placeholder = { Text("e.g., 12.5") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            // Tenure Months Input
            OutlinedTextField(
                value = tenureMonths,
                onValueChange = {
                    tenureMonths = it
                    errorMessage = null
                },
                label = { Text("Tenure (Months)") },
                placeholder = { Text("e.g., 6") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Note",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your offer will be visible to all borrowers in the marketplace. They can request loans based on your terms.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Create Offer Button
            Button(
                onClick = {
                    // Validate inputs
                    val amountValue = amount.toIntOrNull()
                    val interestValue = interestRate.toDoubleOrNull()
                    val tenureValue = tenureMonths.toIntOrNull()

                    when {
                        amountValue == null || amountValue <= 0 -> {
                            errorMessage = "Please enter a valid loan amount"
                        }

                        amountValue > 50000 -> {
                            errorMessage = "Maximum loan amount allowed is ₹50,000."
                            Toast.makeText(
                                context,
                                "Maximum loan amount allowed is ₹50,000.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        interestValue == null || interestValue < 0 -> {
                            errorMessage = "Please enter a valid interest rate."
                        }

                        tenureValue == null || tenureValue <= 0 -> {
                            errorMessage = "Please enter a valid tenure in months"
                        }

                        else -> {
                            val currentUser = auth.currentUser
                            if (currentUser == null) {
                                errorMessage = "User not authenticated"
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null

                            // First, get lender name from user_profiles
                            firestore.collection("user_profiles")
                                .document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { profileDoc ->
                                    val lenderName = profileDoc.getString("name")
                                        ?: currentUser.displayName
                                        ?: currentUser.email?.substringBefore("@")
                                        ?: "Lender"

                                    // Create offer document
                                    val offer = hashMapOf(
                                        "lender_uid" to currentUser.uid,
                                        "lender_name" to lenderName,
                                        "amount" to amountValue,
                                        "interest_rate_total" to interestValue,
                                        "tenure_months" to tenureValue,
                                        "status" to "available",
                                        "created_at" to com.google.firebase.Timestamp.now()
                                    )

                                    firestore.collection("offers")
                                        .add(offer)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(
                                                context,
                                                "Offer Created Successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onNavigateBack()
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            errorMessage = "Failed to create offer: ${e.message}"
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = "Failed to fetch user profile: ${e.message}"
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Create Offer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }
    }
}

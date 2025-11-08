package com.runanywhere.startup_hackathon20

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UPIInputScreen(
    onNavigateBack: () -> Unit,
    onSubmitUPI: (String) -> Unit
) {
    var upiId by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter UPI Details") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Enter Your UPI ID",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "We'll verify your UPI payment history to calculate your SARRAL Score",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // UPI ID Input Field
            OutlinedTextField(
                value = upiId,
                onValueChange = {
                    upiId = it.lowercase()
                    isError = false
                    errorMessage = ""
                },
                label = { Text("UPI ID") },
                placeholder = { Text("example@paytm") },
                singleLine = true,
                isError = isError,
                enabled = !isLoading,
                supportingText = {
                    if (isError) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Format: yourname@bankname")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Tip",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your UPI ID is usually in the format: yourname@bankname (e.g., john@paytm, user@oksbi)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Submit Button
            Button(
                onClick = {
                    when {
                        upiId.isBlank() -> {
                            isError = true
                            errorMessage = "Please enter your UPI ID"
                        }

                        !upiId.contains("@") -> {
                            isError = true
                            errorMessage = "Invalid UPI ID format. Must contain @"
                        }

                        upiId.count { it == '@' } > 1 -> {
                            isError = true
                            errorMessage = "Invalid UPI ID format"
                        }

                        else -> {
                            // Valid UPI ID - Save to Firestore
                            val currentUser = auth.currentUser
                            if (currentUser == null) {
                                isError = true
                                errorMessage = "User not authenticated"
                                return@Button
                            }

                            isLoading = true

                            // Save UPI ID to user profile
                            val userProfile = hashMapOf(
                                "upi_id" to upiId,
                                "user_uid" to currentUser.uid,
                                "updated_at" to com.google.firebase.Timestamp.now()
                            )

                            firestore.collection("user_profiles")
                                .document(currentUser.uid)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    isLoading = false
                                    onSubmitUPI(upiId)
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    isError = true
                                    errorMessage = "Failed to save UPI ID: ${e.message}"
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Verify UPI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Note
            Text(
                text = "🔒 Your data is secure and encrypted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Developer test data seeder (for testing only)
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = {
                    if (upiId.isBlank() || !upiId.contains("@")) {
                        isError = true
                        errorMessage = "Enter a valid UPI ID first"
                        return@TextButton
                    }

                    isLoading = true
                    TestDataSeeder.seedTestTransactions(upiId) { success, message ->
                        isLoading = false
                        if (success) {
                            errorMessage = "✅ Test data seeded successfully! You can now verify."
                        } else {
                            isError = true
                            errorMessage = message
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text(
                    text = "🧪 Seed Test Data (Dev Only)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

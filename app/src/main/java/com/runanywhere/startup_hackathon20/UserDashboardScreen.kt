package com.runanywhere.startup_hackathon20

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    onNavigateToBorrow: () -> Unit,
    onNavigateToLend: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var isUpdatingRole by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile"
                        )
                    }
                    IconButton(onClick = {
                        auth.signOut()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
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
            // Heading
            Text(
                text = "Welcome to SARRAL",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = "Smart Automated Reliable,\nRepayment and Lending",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Borrow Money Button
            Button(
                onClick = {
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isUpdatingRole = true
                    // Update role to borrower
                    firestore.collection("user_profiles")
                        .document(currentUser.uid)
                        .update("role", "borrower")
                        .addOnSuccessListener {
                            isUpdatingRole = false
                            onNavigateToBorrow()
                        }
                        .addOnFailureListener { e ->
                            isUpdatingRole = false
                            Toast.makeText(
                                context,
                                "Failed to update role: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isUpdatingRole
            ) {
                if (isUpdatingRole) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Borrow Money",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Lend Money Button
            Button(
                onClick = {
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isUpdatingRole = true
                    // Update role to lender
                    firestore.collection("user_profiles")
                        .document(currentUser.uid)
                        .update("role", "lender")
                        .addOnSuccessListener {
                            isUpdatingRole = false
                            onNavigateToLend()
                        }
                        .addOnFailureListener { e ->
                            isUpdatingRole = false
                            Toast.makeText(
                                context,
                                "Failed to update role: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                enabled = !isUpdatingRole
            ) {
                if (isUpdatingRole) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Text(
                        text = "Lend Money",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        )
                    )
                }
            }
        }
    }
}

package com.runanywhere.startup_hackathon20

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class for loan offer
data class LoanOffer(
    val id: String,
    val lenderName: String,
    val loanAmount: Int,
    val interestRate: Double,
    val tenureMonths: Int
)

// Placeholder dummy data
private val dummyLoanOffers = listOf(
    LoanOffer(
        id = "1",
        lenderName = "Rajesh Kumar",
        loanAmount = 5000,
        interestRate = 12.5,
        tenureMonths = 6
    ),
    LoanOffer(
        id = "2",
        lenderName = "Priya Sharma",
        loanAmount = 10000,
        interestRate = 11.0,
        tenureMonths = 12
    ),
    LoanOffer(
        id = "3",
        lenderName = "Amit Patel",
        loanAmount = 7500,
        interestRate = 13.0,
        tenureMonths = 9
    ),
    LoanOffer(
        id = "4",
        lenderName = "Sneha Reddy",
        loanAmount = 12000,
        interestRate = 10.5,
        tenureMonths = 12
    ),
    LoanOffer(
        id = "5",
        lenderName = "Vikram Singh",
        loanAmount = 8000,
        interestRate = 12.0,
        tenureMonths = 8
    ),
    LoanOffer(
        id = "6",
        lenderName = "Ananya Desai",
        loanAmount = 6000,
        interestRate = 11.5,
        tenureMonths = 6
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowerLoanDashboardScreen(
    onNavigateBack: () -> Unit,
    onRequestLoan: (LoanOffer) -> Unit = {}
) {
    // Placeholder values
    val sarralScore = 750
    val loanLimit = 12000

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
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            text = sarralScore.toString(),
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

            // Loan Offers List
            items(dummyLoanOffers) { offer ->
                LoanOfferCard(
                    offer = offer,
                    onRequestLoan = { onRequestLoan(offer) }
                )
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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Lender Name
            Text(
                text = offer.lenderName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%,d", offer.loanAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Interest Rate
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Interest Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${offer.interestRate}% p.a.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tenure
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tenure: ",
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

            Spacer(modifier = Modifier.height(16.dp))

            // Request Loan Button
            Button(
                onClick = onRequestLoan,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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

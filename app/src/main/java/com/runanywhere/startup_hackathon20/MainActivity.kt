package com.runanywhere.startup_hackathon20

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Startup_hackathon20Theme {
                AppNavigation()
            }
        }
    }
}

// Helper functions for safe URL encoding/decoding
fun String.encodeUrl(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}

fun String.decodeUrl(): String {
    return URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    // Determine start destination based on authentication state
    val startDestination = if (currentUser != null) "userDashboard" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToSignup = {
                    navController.navigate("signup")
                },
                onLoginSuccess = {
                    navController.navigate("userDashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("signup") {
            SignupScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onSignupSuccess = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        composable("userDashboard") {
            UserDashboardScreen(
                onNavigateToBorrow = {
                    navController.navigate("borrowFlow")
                },
                onNavigateToLend = {
                    navController.navigate("lenderDashboard")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("userProfile")
                }
            )
        }

        composable("borrowFlow") {
            BorrowFlowStartScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUPIInput = {
                    navController.navigate("upiInput")
                }
            )
        }

        composable("lendFlow") {
            LendFlowStartScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("upiInput") {
            UPIInputScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubmitUPI = { upiId ->
                    // Navigate to Borrower Loan Dashboard after UPI verification
                    navController.navigate("borrowerDashboard")
                }
            )
        }

        composable("borrowerDashboard") {
            BorrowerLoanDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRequestLoan = { offer ->
                    try {
                        // Navigate to review offer screen with loan details
                        // Encode lenderName to handle special characters
                        val encodedLenderName = offer.lenderName.encodeUrl()

                        // Validate offer data before navigation
                        // Note: lenderUid can be a placeholder (lender_xxx or unknown_lender)
                        android.util.Log.d(
                            "NavigationDebug",
                            "Navigating with offer - lenderUid: ${offer.lenderUid}, lenderName: ${offer.lenderName}, amount: ${offer.loanAmount}, interest: ${offer.interestRate}, tenure: ${offer.tenureMonths}"
                        )

                        if (offer.lenderName.isBlank()) {
                            Toast.makeText(
                                context,
                                "Error: Missing lender name",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@BorrowerLoanDashboardScreen
                        }

                        if (offer.loanAmount <= 0) {
                            Toast.makeText(
                                context,
                                "Error: Invalid loan amount",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@BorrowerLoanDashboardScreen
                        }

                        if (offer.tenureMonths <= 0) {
                            Toast.makeText(
                                context,
                                "Error: Invalid tenure",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@BorrowerLoanDashboardScreen
                        }

                        // Convert Double to Float for navigation
                        val interestFloat = offer.interestRate.toFloat()

                        navController.navigate(
                            "reviewOffer/${offer.lenderUid}/$encodedLenderName/${offer.loanAmount}/$interestFloat/${offer.tenureMonths}"
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Navigation error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        android.util.Log.e(
                            "NavigationError",
                            "Failed to navigate to reviewOffer",
                            e
                        )
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("userProfile")
                }
            )
        }

        composable(
            "reviewOffer/{lender_uid}/{lender_name}/{amount}/{interest}/{tenure_months}",
            arguments = listOf(
                navArgument("lender_uid") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("lender_name") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("amount") {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument("interest") {
                    type = NavType.FloatType
                    defaultValue = 0.0f
                },
                navArgument("tenure_months") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            var errorState by remember { mutableStateOf<String?>(null) }

            // Safe argument retrieval with validation
            val lenderUid =
                backStackEntry.arguments?.getString("lender_uid")?.takeIf { it.isNotBlank() } ?: ""
            val lenderName = backStackEntry.arguments?.getString("lender_name")?.decodeUrl()
                ?.takeIf { it.isNotBlank() } ?: ""
            val amount = backStackEntry.arguments?.getInt("amount") ?: 0
            val interest = backStackEntry.arguments?.getFloat("interest")?.toDouble() ?: 0.0
            val tenureMonths = backStackEntry.arguments?.getInt("tenure_months") ?: 0

            android.util.Log.d(
                "ReviewOfferNav",
                "Received params - lenderUid: $lenderUid, lenderName: $lenderName, amount: $amount, interest: $interest, tenureMonths: $tenureMonths"
            )

            // Validate required parameters
            if (lenderName.isBlank()) {
                errorState = "Missing lender name"
            } else if (amount <= 0) {
                errorState = "Invalid loan amount"
            } else if (interest < 0) {
                errorState = "Invalid interest rate"
            } else if (tenureMonths <= 0) {
                errorState = "Invalid tenure"
            }

            if (errorState != null) {
                android.util.Log.e("ReviewOfferNav", "Validation error: $errorState")
                LaunchedEffect(Unit) {
                    Toast.makeText(
                        context,
                        "Error: $errorState. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                }
                // Show error screen briefly before navigating back
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "Invalid parameters: $errorState",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                ReviewOfferScreen(
                    lenderUid = lenderUid,
                    lenderName = lenderName,
                    amount = amount,
                    interest = interest,
                    tenureMonths = tenureMonths,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSuccess = {
                        navController.navigate("successRequest") {
                            popUpTo("borrowerDashboard") { inclusive = false }
                        }
                    }
                )
            }
        }

        composable("successRequest") {
            SuccessRequestScreen(
                onViewLoanStatus = {
                    navController.navigate("borrowerProfile")
                }
            )
        }

        composable("borrowerProfile") {
            LoanStatusScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = {
                    navController.navigate("userProfile")
                }
            )
        }

        composable("userProfile") {
            UserProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLoanStatus = {
                    navController.navigate("borrowerProfile")
                },
                onNavigateToLenderRequests = {
                    navController.navigate("lenderRequests")
                }
            )
        }

        composable("lenderRequests") {
            LenderLoanRequestsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("lenderDashboard") {
            LenderDashboardScreen(
                onNavigateToCreateOffer = {
                    navController.navigate("createLoanOffer")
                },
                onNavigateToLoanRequests = {
                    navController.navigate("lenderRequests")
                },
                onNavigateToActiveLoans = {
                    navController.navigate("lenderActiveLoans")
                },
                onNavigateToProfile = {
                    navController.navigate("userProfile")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("createLoanOffer") {
            CreateLoanOfferScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("lenderActiveLoans") {
            LenderListedLoansScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val currentModelId by viewModel.currentModelId.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Chat") },
                actions = {
                    TextButton(onClick = { showModelSelector = !showModelSelector }) {
                        Text("Models")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    downloadProgress?.let { progress ->
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }

            // Model selector (collapsible)
            if (showModelSelector) {
                ModelSelector(
                    models = availableModels,
                    currentModelId = currentModelId,
                    onDownload = { modelId -> viewModel.downloadModel(modelId) },
                    onLoad = { modelId -> viewModel.loadModel(modelId) },
                    onRefresh = { viewModel.refreshModels() }
                )
            }

            // Messages List
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }

            // Auto-scroll to bottom when new messages arrive
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            // Input Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    enabled = !isLoading && currentModelId != null
                )

                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = !isLoading && inputText.isNotBlank() && currentModelId != null
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (message.isUser) "You" else "AI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ModelSelector(
    models: List<com.runanywhere.sdk.models.ModelInfo>,
    currentModelId: String?,
    onDownload: (String) -> Unit,
    onLoad: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Available Models",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (models.isEmpty()) {
                Text(
                    text = "No models available. Initializing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(models) { model ->
                        ModelItem(
                            model = model,
                            isLoaded = model.id == currentModelId,
                            onDownload = { onDownload(model.id) },
                            onLoad = { onLoad(model.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelItem(
    model: com.runanywhere.sdk.models.ModelInfo,
    isLoaded: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoaded)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleSmall
            )

            if (isLoaded) {
                Text(
                    text = "✓ Currently Loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        enabled = !model.isDownloaded
                    ) {
                        Text(if (model.isDownloaded) "Downloaded" else "Download")
                    }

                    Button(
                        onClick = onLoad,
                        modifier = Modifier.weight(1f),
                        enabled = model.isDownloaded
                    ) {
                        Text("Load")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Startup_hackathon20Theme {
        ChatScreen()
    }
}
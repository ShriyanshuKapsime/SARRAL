package com.runanywhere.startup_hackathon20

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessRequestScreen(
    onViewLoanStatus: () -> Unit
) {
    // Animation state
    var animationCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Pre-delay to let screen render fully
        kotlinx.coroutines.delay(100)
        // Mark animation as completed after optimized duration
        kotlinx.coroutines.delay(900) // 300ms circle + 400ms check + 200ms bounce
        animationCompleted = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Sent Successfully") }
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
            // Animated Success Icon with hardware acceleration
            AnimatedSuccessCheckmark(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        // Enable hardware acceleration for smooth rendering
                        // Hardware layer automatically enabled by graphicsLayer
                    },
                color = Color(0xFF5CB8FF) // Primary blue color
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title with fade-in animation (delayed 150ms after animation completes)
            androidx.compose.animation.AnimatedVisibility(
                visible = animationCompleted,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = 150, // Sequential reveal delay
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Request Sent Successfully",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message
                    Text(
                        text = "Your loan request has been submitted to the lender.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // View Loan Status Button
                    Button(
                        onClick = onViewLoanStatus,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "View Loan Status",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSuccessCheckmark(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF5CB8FF)
) {
    // Optimized animation states with smooth easing
    val fadeInAnim = remember { Animatable(0f) }
    val circleAnim = remember { Animatable(0f) }
    val checkAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) } // Start from 0.85x for smoother entry

    // Custom easing function for smooth acceleration/deceleration
    val easeOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

    LaunchedEffect(Unit) {
        // Step 1: Fade-in (200ms) + Circle draws (100ms overlap for fluid transition)
        launch {
            fadeInAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
        }

        // Small delay for visual separation
        kotlinx.coroutines.delay(50)

        // Circle animation with smooth easing
        circleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 300,
                easing = easeOutCubic
            )
        )

        // Step 2: Checkmark draws with spring animation for natural motion
        checkAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 200f
            )
        )

        // Step 3: Scale animation with overshoot bounce (0.85x → 1.05x → 1.0x)
        scaleAnim.animateTo(
            targetValue = 1.05f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 300f
            )
        )

        // Settle back to 1.0x with gentle bounce
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 250f
            )
        )
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            // Apply alpha for fade-in
            alpha = fadeInAnim.value
            // Enable hardware acceleration
        }
    ) {
        val canvasSize = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = canvasSize / 2f - 8.dp.toPx()
        val strokeWidth = 8.dp.toPx()

        // Apply scale transformation for bounce effect
        scale(scale = scaleAnim.value, pivot = center) {
            // Draw glow effect during scale animation
            if (scaleAnim.value > 1f) {
                drawCircle(
                    color = color.copy(alpha = 0.2f * fadeInAnim.value),
                    radius = radius + 12.dp.toPx(),
                    center = center
                )
            }

            // Draw circle outline with smooth arc
            val sweepAngle = 360f * circleAnim.value
            drawArc(
                color = color.copy(alpha = fadeInAnim.value),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Draw checkmark (only after circle is complete)
            if (circleAnim.value >= 1f) {
                val checkProgress = checkAnim.value

                // Checkmark coordinates
                val checkStartX = center.x - radius * 0.4f
                val checkStartY = center.y
                val checkMidX = center.x - radius * 0.1f
                val checkMidY = center.y + radius * 0.3f
                val checkEndX = center.x + radius * 0.5f
                val checkEndY = center.y - radius * 0.4f

                // Draw first segment (left part of checkmark)
                if (checkProgress > 0f) {
                    val segment1Progress = (checkProgress * 2f).coerceAtMost(1f)
                    val segment1EndX = checkStartX + (checkMidX - checkStartX) * segment1Progress
                    val segment1EndY = checkStartY + (checkMidY - checkStartY) * segment1Progress

                    drawLine(
                        color = color.copy(alpha = fadeInAnim.value),
                        start = Offset(checkStartX, checkStartY),
                        end = Offset(segment1EndX, segment1EndY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Draw second segment (right part of checkmark)
                if (checkProgress > 0.5f) {
                    val segment2Progress = ((checkProgress - 0.5f) * 2f).coerceAtMost(1f)
                    val segment2EndX = checkMidX + (checkEndX - checkMidX) * segment2Progress
                    val segment2EndY = checkMidY + (checkEndY - checkMidY) * segment2Progress

                    drawLine(
                        color = color.copy(alpha = fadeInAnim.value),
                        start = Offset(checkMidX, checkMidY),
                        end = Offset(segment2EndX, segment2EndY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

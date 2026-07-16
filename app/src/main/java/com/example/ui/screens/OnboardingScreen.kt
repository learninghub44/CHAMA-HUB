package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    val onboardingPages = listOf(
        OnboardingPageData(
            title = "Welcome to ChamaHub",
            description = "Manage your savings group digitally with transparency, trust and ease.",
            icon = Icons.Default.Groups,
            accentColor = EmeraldGreen,
            illustrationType = "people"
        ),
        OnboardingPageData(
            title = "Track Contributions",
            description = "Never miss a contribution again. Complete transparency on savings totals.",
            icon = Icons.Default.AccountBalanceWallet,
            accentColor = GoldAccent,
            illustrationType = "wallet"
        ),
        OnboardingPageData(
            title = "Loan Management",
            description = "Issue loans, set customized interest rates, and monitor structured repayments.",
            icon = Icons.Default.CurrencyExchange,
            accentColor = Color(0xFF3B82F6),
            illustrationType = "loan"
        ),
        OnboardingPageData(
            title = "Reports & Analytics",
            description = "Know exactly how your savings group is performing with real-time financial health summaries.",
            icon = Icons.Default.Insights,
            accentColor = Color(0xFFA855F7),
            illustrationType = "charts"
        ),
        OnboardingPageData(
            title = "Ironclad Security",
            description = "Your financial records stay secure with secure PIN and biometric authorization.",
            icon = Icons.Default.Security,
            accentColor = EmeraldGreen,
            illustrationType = "security"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen")
            .background(Color(0xFF0F172A)) // Dark slate atmosphere
    ) {
        // Content horizontal scroll pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = onboardingPages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Vector Canvas Illustration
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    RenderIllustration(type = page.illustrationType, accentColor = page.accentColor)
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(page.accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = page.accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = page.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = page.description,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }

        // Top skip button
        if (pagerState.currentPage < 4) {
            TextButton(
                onClick = { onFinished() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .testTag("skip_button")
            ) {
                Text("Skip", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
            }
        }

        // Bottom Actions & Page Indicators
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicator dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(width = if (isSelected) 18.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) onboardingPages[pagerState.currentPage].accentColor
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Action Button
            AnimatedVisibility(
                visible = pagerState.currentPage == 4,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                Button(
                    onClick = { onFinished() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_start_button")
                ) {
                    Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            AnimatedVisibility(
                visible = pagerState.currentPage < 4,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilledIconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = onboardingPages[pagerState.currentPage].accentColor
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RenderIllustration(type: String, accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        when (type) {
            "people" -> {
                // Draw decorative ambient circles
                drawCircle(color = accentColor.copy(alpha = 0.15f), radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = accentColor.copy(alpha = 0.08f), radius = w * 0.5f, center = Offset(w * 0.5f, h * 0.5f))
                
                // Represent family/members dots and linking line paths
                drawLine(color = accentColor.copy(alpha = 0.4f), start = Offset(w * 0.25f, h * 0.6f), end = Offset(w * 0.5f, h * 0.4f), strokeWidth = 3f)
                drawLine(color = accentColor.copy(alpha = 0.4f), start = Offset(w * 0.75f, h * 0.6f), end = Offset(w * 0.5f, h * 0.4f), strokeWidth = 3f)
                drawLine(color = accentColor.copy(alpha = 0.4f), start = Offset(w * 0.25f, h * 0.6f), end = Offset(w * 0.75f, h * 0.6f), strokeWidth = 3f)

                drawCircle(color = accentColor, radius = 24f, center = Offset(w * 0.5f, h * 0.4f)) // Admin node
                drawCircle(color = Color(0xFF3B82F6), radius = 18f, center = Offset(w * 0.25f, h * 0.6f))
                drawCircle(color = Color(0xFFF59E0B), radius = 18f, center = Offset(w * 0.75f, h * 0.6f))
            }
            "wallet" -> {
                drawCircle(color = accentColor.copy(alpha = 0.15f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f))
                // Draw money coins entering a wallet
                drawCircle(color = GoldAccent, radius = 22f, center = Offset(w * 0.5f, h * 0.3f))
                drawCircle(color = GoldAccent.copy(alpha = 0.6f), radius = 18f, center = Offset(w * 0.42f, h * 0.4f))
                
                // Draw card rectangle representative
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.25f, h * 0.45f),
                    size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.3f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = 6f)
                )
            }
            "loan" -> {
                drawCircle(color = accentColor.copy(alpha = 0.15f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f))
                // Arrow flowing curves representing loan/repayments
                drawArc(
                    color = accentColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.25f, h * 0.35f),
                    size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.3f),
                    style = Stroke(width = 5f)
                )
                
                drawCircle(color = accentColor, radius = 12f, center = Offset(w * 0.25f, h * 0.5f))
                drawCircle(color = Color(0xFF10B981), radius = 12f, center = Offset(w * 0.75f, h * 0.5f))
            }
            "charts" -> {
                drawCircle(color = accentColor.copy(alpha = 0.15f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f))
                // Bar graphs representation
                drawRect(color = accentColor.copy(alpha = 0.4f), topLeft = Offset(w * 0.3f, h * 0.55f), size = androidx.compose.ui.geometry.Size(25f, h * 0.2f))
                drawRect(color = accentColor.copy(alpha = 0.7f), topLeft = Offset(w * 0.45f, h * 0.45f), size = androidx.compose.ui.geometry.Size(25f, h * 0.3f))
                drawRect(color = accentColor, topLeft = Offset(w * 0.6f, h * 0.35f), size = androidx.compose.ui.geometry.Size(25f, h * 0.4f))
            }
            "security" -> {
                drawCircle(color = accentColor.copy(alpha = 0.15f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f))
                // Draw lock shield
                drawCircle(
                    color = accentColor,
                    radius = 48f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 6f)
                )
                // Draw inner keyhole representation
                drawCircle(color = accentColor, radius = 14f, center = Offset(w * 0.5f, h * 0.46f))
                drawRect(color = accentColor, topLeft = Offset(w * 0.47f, h * 0.48f), size = androidx.compose.ui.geometry.Size(15f, 30f))
            }
        }
    }
}

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val illustrationType: String
)

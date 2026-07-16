package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChamaGroup
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.delay

@Composable
fun JoinGroupScreen(
    availableGroups: List<ChamaGroup>,
    onJoinSubmitted: (String, (String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isScanningMock by remember { mutableStateOf(false) }
    var scannerProgress by remember { mutableStateOf(0f) }

    // Dynamic QR scanning animation with real database code fallback
    LaunchedEffect(isScanningMock) {
        if (isScanningMock) {
            scannerProgress = 0f
            while (scannerProgress < 1f) {
                delay(30)
                scannerProgress += 0.05f
            }
            isScanningMock = false
            
            // Extract a real valid invite code from existing groups so scanning actually works!
            val targetCode = availableGroups.firstOrNull()?.inviteCode ?: "CHAMA-WELCOME"
            inviteCode = targetCode
            successMessage = "QR Code Invitation scanned successfully! Group Code: $targetCode"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("join_group_screen")
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NavyPrimary, Color(0xFF0F172A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Rudi", tint = Color.White)
                }
                Text(
                    text = "Jiunge na Kikundi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = "Join Existing Savings Circle",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Enter an invite code or scan the QR code shared by your Treasurer or Administrator.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Notifications/Error cards
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                successMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // QR Code Mock Scanner Container
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanningMock) {
                        // Animated Scanning Laser Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(4.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (200 * scannerProgress).dp)
                                .background(EmeraldGreen)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isScanningMock) Icons.Default.QrCodeScanner else Icons.Default.QrCode,
                            contentDescription = null,
                            tint = if (isScanningMock) EmeraldGreen else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        
                        Text(
                            text = if (isScanningMock) "Scanning camera..." else "Camera Ready",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )

                        Button(
                            onClick = { isScanningMock = true; errorMessage = null; successMessage = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate QR Code Scan", color = Color.White)
                        }
                    }
                }
            }

            // Invite Code Manual Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = { Text("Group Invite Code (e.g., CHAMA-12345)", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("CHAMA-XXXXX", color = Color.White.copy(alpha = 0.3f)) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("join_invite_input")
                    )

                    Button(
                        onClick = {
                            errorMessage = null
                            successMessage = null
                            if (inviteCode.isBlank()) {
                                errorMessage = "Tafadhali weka msimbo wa mwaliko kwanza."
                            } else {
                                onJoinSubmitted(inviteCode) { error ->
                                    errorMessage = error
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("join_submit_button")
                    ) {
                        Text("Jiunge Sasa (Join Circle)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

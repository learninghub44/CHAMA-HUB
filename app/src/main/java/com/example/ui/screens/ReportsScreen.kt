package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChamaGroup
import com.example.data.repository.ReceiptScanResult
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.CrimsonRisk
import com.example.ui.theme.NavyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    group: ChamaGroup,
    totalSavings: Double,
    totalLoans: Double,
    availableCash: Double,
    currency: String,
    onScanReceipt: (String) -> Unit,
    scannedReceipt: ReceiptScanResult?,
    onClearReceipt: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isGeneratingSummary by remember { mutableStateOf(false) }
    var executiveSummaryText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen")
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Top Bar
            CenterAlignedTopAppBar(
                title = { Text("Audit Reports Panel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            // Content scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Export Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { /* Export PDF Mock */ },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF")
                    }

                    OutlinedButton(
                        onClick = { /* Export CSV Mock */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.GridOn, contentDescription = null, tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export CSV")
                    }
                }

                // AI Executive Summary Panel
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AI Executive Board Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (executiveSummaryText.isBlank()) {
                            Button(
                                onClick = {
                                    isGeneratingSummary = true
                                    executiveSummaryText = """
                                        **ChamaHub Financial Audit Report Summary:**
                                        
                                        Your Savings Circle *${group.name}* holds a total savings balance of **$currency ${String.format("%,.2f", totalSavings)}**. Credit exposure is KES ${String.format("%,.2f", totalLoans)} with liquid cash reserves at **$currency ${String.format("%,.2f", availableCash)}**.
                                        
                                        *   **Financial health score:** 89/100 (Extremely Healthy)
                                        *   **Member participation rate:** 94% compliance
                                        *   **Identified default risks:** David Ochieng (Behind), Grace Mwangi (Overdue).
                                        
                                        *Advice:* Focus on recovering Grace Mwangi's outstanding loan of ${currency} 22,000 to increase available liquid cash before disbursing new lines of credit.
                                    """.trimIndent()
                                    isGeneratingSummary = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Board Summary", color = Color.White)
                            }
                        } else {
                            Text(
                                text = executiveSummaryText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // AI RECEIPT SCANNER OCR MODULE
                Text("AI Expense Receipt Scanner", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (scannedReceipt == null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { onScanReceipt("Apex Stationers Office Supplies Receipt") },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capture Receipt")
                                }

                                Button(
                                    onClick = { onScanReceipt("Hilltop Community Hall Room Rental Invoice") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Bill")
                                }
                            }
                        } else {
                            // Render Scanned Results Card
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Extracted OCR Bill Facts", fontWeight = FontWeight.Bold, color = EmeraldGreen, fontSize = 14.sp)
                                    IconButton(onClick = onClearReceipt) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Business Vendor", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(scannedReceipt.businessName, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Purchase Date", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(scannedReceipt.date, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tax Surcharge", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text("$currency ${String.format("%,.2f", scannedReceipt.tax)}", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Extracted Cost", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text("$currency ${String.format("%,.2f", scannedReceipt.amount)}", fontWeight = FontWeight.Bold, color = CrimsonRisk)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldAccent.copy(alpha = 0.15f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Categorized Auto-Supplies: ${scannedReceipt.category}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.repository.LoanRiskAssessment
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.CrimsonRisk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    loans: List<Loan>,
    members: List<Member>,
    onIssueLoan: (memberId: Int, memberName: String, amount: Double, rate: Double, duration: Int, risk: LoanRiskAssessment?) -> Unit,
    onEvaluateLoanRisk: (memberName: String, requestAmount: Double, memberId: Int) -> Unit,
    loanRiskAssessment: LoanRiskAssessment?,
    onClearLoanRisk: () -> Unit,
    onDeleteLoan: ((Loan) -> Unit)? = null,
    currency: String,
    onBack: () -> Unit
) {
    var isIssuingLoan by remember { mutableStateOf(false) }

    // Form states
    var selectedMemberIndex by remember { mutableStateOf(0) }
    var amountText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("3") }
    var interestRateText by remember { mutableStateOf("10") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loans_screen")
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Top Bar
            CenterAlignedTopAppBar(
                title = { Text("Chama Credit Ledger", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isIssuingLoan = true }) {
                        Icon(imageVector = Icons.Default.Handshake, contentDescription = "Issue Loan", tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            // Portfolio Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Outstanding Credits", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currency ${String.format("%,.0f", loans.filter { it.status == "Active" || it.status == "Overdue" }.sumOf { it.remainingBalance })}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CrimsonRisk.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Handshake, contentDescription = null, tint = CrimsonRisk)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Loans list
            Text(
                "Active Credit Lines",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (loans.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No credit lines issued yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    items(loans) { loan ->
                        ActiveLoanCard(
                            loan = loan,
                            currency = currency,
                            onDelete = if (onDeleteLoan != null) { { onDeleteLoan(loan) } } else null
                        )
                    }
                }
            }
        }

        // Add Loan Dialog popup
        if (isIssuingLoan && members.isNotEmpty()) {
            var dropdownExpanded by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { 
                    isIssuingLoan = false
                    onClearLoanRisk()
                },
                title = { Text("Approve Credit Disbursal") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dropdown Member picker
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = members[selectedMemberIndex].name,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                members.forEachIndexed { index, member ->
                                    DropdownMenuItem(
                                        text = { Text(member.name) },
                                        onClick = {
                                            selectedMemberIndex = index
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Amount text field
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Lending Capital ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_amount_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            // Duration Months
                            OutlinedTextField(
                                value = durationText,
                                onValueChange = { durationText = it },
                                label = { Text("Duration (Months)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            // Interest rate
                            OutlinedTextField(
                                value = interestRateText,
                                onValueChange = { interestRateText = it },
                                label = { Text("Interest Rate (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // AI Loan risk checker trigger
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                onEvaluateLoanRisk(members[selectedMemberIndex].name, amount, members[selectedMemberIndex].id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_risk_check_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Query AI Risk Advisor", color = Color.White)
                        }

                        // AI Risk recommendation display area
                        AnimatedVisibility(visible = loanRiskAssessment != null) {
                            if (loanRiskAssessment != null) {
                                val color = when (loanRiskAssessment.riskLevel) {
                                    "Low Risk" -> EmeraldGreen
                                    "Medium Risk" -> GoldAccent
                                    else -> CrimsonRisk
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Risk Level: ${loanRiskAssessment.riskLevel}",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = color,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${loanRiskAssessment.score}/100 Score",
                                                fontWeight = FontWeight.Bold,
                                                color = color,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = loanRiskAssessment.reasoning,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Suggested rate: ${loanRiskAssessment.suggestedInterestRate}% | Confidence: ${loanRiskAssessment.confidence}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            val rate = interestRateText.toDoubleOrNull() ?: 10.0
                            val duration = durationText.toIntOrNull() ?: 3
                            if (amount > 0) {
                                onIssueLoan(
                                    members[selectedMemberIndex].id,
                                    members[selectedMemberIndex].name,
                                    amount,
                                    rate,
                                    duration,
                                    loanRiskAssessment
                                )
                                amountText = ""
                                isIssuingLoan = false
                                onClearLoanRisk()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Disburse")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        isIssuingLoan = false
                        onClearLoanRisk()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ActiveLoanCard(loan: Loan, currency: String, onDelete: (() -> Unit)? = null) {
    val progress = ((loan.amount - loan.remainingBalance) / loan.amount).toFloat().coerceIn(0f, 1f)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (loan.status == "Overdue") CrimsonRisk.copy(alpha = 0.12f)
                                else EmeraldGreen.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = if (loan.status == "Overdue") CrimsonRisk else EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(loan.memberName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Due: ${loan.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Risk status label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (loan.status == "Overdue") CrimsonRisk.copy(alpha = 0.12f)
                                else EmeraldGreen.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = loan.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (loan.status == "Overdue") CrimsonRisk else EmeraldGreen
                        )
                    }
                    
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Credit Line",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Balance: $currency ${String.format("%,.0f", loan.remainingBalance)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total: $currency ${String.format("%,.0f", loan.amount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = if (loan.status == "Overdue") CrimsonRisk else EmeraldGreen,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
    }
}

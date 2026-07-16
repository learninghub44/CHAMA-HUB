package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.SyncStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    group: ChamaGroup,
    allGroups: List<ChamaGroup> = emptyList(),
    onSwitchGroup: (Int) -> Unit = {},
    members: List<Member>,
    contributions: List<Contribution>,
    loans: List<Loan>,
    meetings: List<Meeting>,
    logs: List<ActivityLog>,
    currency: String,
    onNavigate: (String) -> Unit,
    onQuickAction: (String) -> Unit,
    onToggleTheme: () -> Unit,
    currentUser: User? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    userMembership: Membership? = null
) {
    val scrollState = rememberScrollState()

    // Calculated fields
    val totalSavings = contributions.sumOf { it.amount }
    val totalOutstandingLoans = loans.filter { it.status == "Active" || it.status == "Overdue" }.sumOf { it.remainingBalance }
    val availableCash = totalSavings - loans.filter { it.status == "Active" || it.status == "Approved" }.sumOf { it.amount } + loans.sumOf { it.amount - it.remainingBalance }
    val availableCashSafe = availableCash.coerceAtLeast(0.0)
    
    val monthlyTarget = contributions.take(5).sumOf { it.amount } // estimate
    val formattedDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

    // Role-based state permissions dialog
    var permissionDeniedMsg by remember { mutableStateOf<String?>(null) }

    val userRole = userMembership?.role ?: "Member"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Premium Header with dynamic greeting, switcher and quick action buttons
            HeaderSection(
                group = group,
                allGroups = allGroups,
                onSwitchGroup = onSwitchGroup,
                formattedDate = formattedDate,
                onNavigate = onNavigate,
                onToggleTheme = onToggleTheme,
                currentUser = currentUser,
                syncStatus = syncStatus
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // AI INSIGHTS CARD SNEAKPEEK
                AIInsightsBanner(onNavigate = onNavigate)

                Spacer(modifier = Modifier.height(16.dp))

                // Vault Financial Statistics Cards Grid
                FinancialCardsGrid(
                    totalSavings = totalSavings,
                    availableCash = availableCashSafe,
                    outstandingLoans = totalOutstandingLoans,
                    monthlyContributions = monthlyTarget,
                    currency = currency
                )

                Spacer(modifier = Modifier.height(24.dp))

                // QUICK ACTION CARDS
                Text("Quick Administrative Actions", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                QuickActionsRow(
                    onQuickAction = { action ->
                        // Enforce multi-user Role-Based Permission Control boundaries
                        val allowed = when (action) {
                            "member" -> userRole == "Owner" || userRole == "Admin"
                            "contribution" -> userRole == "Owner" || userRole == "Admin" || userRole == "Treasurer"
                            "loan" -> userRole == "Owner" || userRole == "Admin" || userRole == "Treasurer"
                            "meeting" -> userRole == "Owner" || userRole == "Admin" || userRole == "Secretary"
                            else -> true // Reports can be viewed by anyone
                        }

                        if (allowed) {
                            onQuickAction(action)
                        } else {
                            permissionDeniedMsg = when (action) {
                                "member" -> "Idhini Inahitajika! Akaunti yako imesajiliwa kama '$userRole'. Kusimamia wanachama kunahitaji jukumu la Msimamizi (Owner au Admin).\n\nAction restricted! Member management requires Owner or Admin role."
                                "contribution" -> "Mruhusi wa Mweka Hazina Unahitajika! Kurekodi michango kunahitaji jukumu la Treasurer, Admin, au Owner.\n\nRecording contributions requires Treasurer, Admin, or Owner role."
                                "loan" -> "Idhini ya mkopo imezuiliwa! Kutoa mikopo kunahitaji jukumu la Treasurer, Admin, au Owner.\n\nDisbursing loans requires Treasurer, Admin, or Owner role."
                                "meeting" -> "Kupanga vikao kunahitaji jukumu la Secretary, Admin, au Owner.\n\nScheduling sessions requires Secretary, Admin, or Owner role."
                                else -> "Unauthorized action"
                            }
                        }
                    },
                    userRole = userRole
                )

                Spacer(modifier = Modifier.height(24.dp))

                // HIGH FIDELITY CHARTS
                Text("Financial Analytics Trends", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                ChartsSection(totalSavings = totalSavings, outstandingLoans = totalOutstandingLoans, availableCash = availableCashSafe)

                Spacer(modifier = Modifier.height(24.dp))

                // RECENT ACTIVITIES TIMELINE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Audit Trail Activities", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "View All",
                        color = EmeraldGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigate("activity_logs") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                RecentActivitiesTimeline(logs = logs)
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Permission denied warning alert dialog
        permissionDeniedMsg?.let { msg ->
            AlertDialog(
                onDismissRequest = { permissionDeniedMsg = null },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(40.dp)) },
                title = { Text("Idhini Imezuiwa (Access Restricted)", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = { Text(msg, fontSize = 14.sp, textAlign = TextAlign.Center) },
                confirmButton = {
                    Button(
                        onClick = { permissionDeniedMsg = null },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Sawa (I Understand)", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun HeaderSection(
    group: ChamaGroup,
    allGroups: List<ChamaGroup>,
    onSwitchGroup: (Int) -> Unit,
    formattedDate: String,
    onNavigate: (String) -> Unit,
    onToggleTheme: () -> Unit,
    currentUser: User? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left profile & Group Details with interactive Switcher drop-down
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.name.take(1).uppercase(),
                            color = EmeraldGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier
                            .clickable { dropdownExpanded = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Habari, ${currentUser?.name ?: "Mwanachama"}! 👋",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Group",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .background(NavyPrimary)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("SELECT ACTIVE GROUP", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) },
                                onClick = {},
                                enabled = false
                            )
                            allGroups.forEach { grp ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (grp.id == group.id) {
                                                Icon(Icons.Default.Check, contentDescription = "Active", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                            } else {
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }
                                            Text(
                                                text = grp.name,
                                                color = if (grp.id == group.id) EmeraldGreen else Color.White,
                                                fontWeight = if (grp.id == group.id) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        dropdownExpanded = false
                                        onSwitchGroup(grp.id)
                                    }
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Create Group", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                        Text("Create New Group", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    onNavigate("create_group")
                                }
                            )
                        }
                    }
                }

                // Header Action triggers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(imageVector = Icons.Default.LightMode, contentDescription = "Theme", tint = GoldAccent)
                    }
                    IconButton(
                        onClick = { onNavigate("notifications") },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                    IconButton(
                        onClick = { onNavigate("settings") },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Date & Cloud Sync status & Role badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Role badge
                    val roleLabel = currentUser?.role ?: "Owner"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldGreen.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }

                    // 2. Cloud Sync status indicator badge
                    val (syncText, syncColor, syncIcon) = when (syncStatus) {
                        SyncStatus.SYNCED -> Triple("Synced", EmeraldGreen, Icons.Default.CloudDone)
                        SyncStatus.PENDING -> Triple("Syncing...", GoldAccent, Icons.Default.Sync)
                        SyncStatus.OFFLINE -> Triple("Offline", Color.Gray, Icons.Default.CloudOff)
                        SyncStatus.ERROR -> Triple("Sync Error", CrimsonRisk, Icons.Default.CloudQueue)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(syncColor.copy(alpha = 0.15f))
                            .border(1.dp, syncColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(syncIcon, contentDescription = null, tint = syncColor, modifier = Modifier.size(10.dp))
                            Text(
                                text = syncText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = syncColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AIInsightsBanner(onNavigate: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Rich deep indigo
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("ai_assistant") }
            .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF312E81)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = GoldAccent, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("AI Financial Assistant Online", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Ask: 'Who hasn't contributed this month?'", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Ask", tint = Color.White)
        }
    }
}

@Composable
fun FinancialCardsGrid(
    totalSavings: Double,
    availableCash: Double,
    outstandingLoans: Double,
    monthlyContributions: Double,
    currency: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Savings Hero
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Total Cumulative Savings", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$currency ${String.format("%,.2f", totalSavings)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen)
                    }
                }
            }
        }

        // Secondary metrics grid
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            // Cash box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Available Liquid Cash", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$currency ${String.format("%,.0f", availableCash)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Loans box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Outstanding Credits", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$currency ${String.format("%,.0f", outstandingLoans)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CrimsonRisk)
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onQuickAction: (String) -> Unit,
    userRole: String
) {
    val actions = listOf(
        QuickActionItem("Add Member", Icons.Default.PersonAdd, "member", EmeraldGreen, requiresAdmin = true),
        QuickActionItem("Contribution", Icons.Default.Paid, "contribution", GoldAccent, requiresTreasurer = true),
        QuickActionItem("Disburse Loan", Icons.Default.Handshake, "loan", NavyLight, requiresTreasurer = true),
        QuickActionItem("Meet Session", Icons.Default.Groups, "meeting", Color(0xFFA855F7), requiresSecretary = true),
        QuickActionItem("Doc Report", Icons.Default.Summarize, "report", Color(0xFFF43F5E), requiresAdmin = false)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { act ->
            val isRestricted = when {
                act.requiresAdmin && !(userRole == "Owner" || userRole == "Admin") -> true
                act.requiresTreasurer && !(userRole == "Owner" || userRole == "Admin" || userRole == "Treasurer") -> true
                act.requiresSecretary && !(userRole == "Owner" || userRole == "Admin" || userRole == "Secretary") -> true
                else -> false
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(110.dp)
                    .clickable { onQuickAction(act.actionKey) }
                    .border(
                        width = 1.dp,
                        color = if (isRestricted) Color.Gray.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isRestricted) Color.Gray.copy(alpha = 0.1f) else act.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = act.icon,
                                contentDescription = null,
                                tint = if (isRestricted) Color.Gray else act.color
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = act.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRestricted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    // Padlock indicator icon overlay for restricted buttons
                    if (isRestricted) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = CrimsonRisk,
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartsSection(totalSavings: Double, outstandingLoans: Double, availableCash: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Asset Allocation Vault (Composition)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Canvas drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Draw continuous curve line for 'Savings Growth' trend simulation
                    val path = Path().apply {
                        moveTo(0f, h * 0.8f)
                        cubicTo(w * 0.25f, h * 0.7f, w * 0.5f, h * 0.4f, w * 0.75f, h * 0.5f)
                        lineTo(w, h * 0.2f)
                    }
                    
                    drawPath(
                        path = path,
                        color = EmeraldGreen,
                        style = Stroke(width = 8f)
                    )

                    // Draw supporting grids lines
                    drawLine(color = Color.LightGray.copy(alpha = 0.3f), start = Offset(0f, h * 0.2f), end = Offset(w, h * 0.2f))
                    drawLine(color = Color.LightGray.copy(alpha = 0.3f), start = Offset(0f, h * 0.5f), end = Offset(w, h * 0.5f))
                    drawLine(color = Color.LightGray.copy(alpha = 0.3f), start = Offset(0f, h * 0.8f), end = Offset(w, h * 0.8f))
                }

                // Micro legend overlays
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("May Session", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                    Text("June Session", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                    Text("July Session", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color-coded Chart legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = EmeraldGreen, label = "Savings growth")
                ChartLegendItem(color = NavyLight, label = "Cash liquidity")
                ChartLegendItem(color = CrimsonRisk, label = "Issued loans")
            }
        }
    }
}

@Composable
fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
fun RecentActivitiesTimeline(logs: List<ActivityLog>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val displayLogs = logs.take(3)
            if (displayLogs.isEmpty()) {
                Text(
                    "No audit logs compiled yet.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            } else {
                displayLogs.forEachIndexed { idx, log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Log state icons
                        val icon = when {
                            log.action.contains("Group") -> Icons.Default.AccountBalance
                            log.action.contains("Member") -> Icons.Default.Person
                            log.action.contains("Loan") -> Icons.Default.Handshake
                            else -> Icons.Default.Paid
                        }
                        val tint = when {
                            log.action.contains("Delete") -> CrimsonRisk
                            log.action.contains("Add") -> EmeraldGreen
                            else -> GoldAccent
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.details, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(log.action, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }

                        // Time stamp formatting
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    if (idx < displayLogs.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val actionKey: String,
    val color: Color,
    val requiresAdmin: Boolean = false,
    val requiresTreasurer: Boolean = false,
    val requiresSecretary: Boolean = false
)

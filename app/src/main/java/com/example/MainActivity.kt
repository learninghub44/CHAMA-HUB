package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChamaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.repository.FirebaseSyncManager.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChamaViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ChamaAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ChamaAppContent(viewModel: ChamaViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    
    // Core Entity state observations
    val group by viewModel.group.collectAsState()
    val members by viewModel.members.collectAsState()
    val contributions by viewModel.contributions.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val meetings by viewModel.meetings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    // Sub-screen parameters
    val selectedMember by viewModel.selectedMember.collectAsState()
    val selectedMeeting by viewModel.selectedMeeting.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAILoading by viewModel.isAILoading.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()
    val loanRiskAssessment by viewModel.loanRiskAssessment.collectAsState()
    val scannedReceiptResult by viewModel.scannedReceiptResult.collectAsState()

    when (currentScreen) {
        "splash" -> {
            SplashScreen(
                onAnimationFinished = {
                    // Route to onboarding or login depending on database state
                    if (group != null) {
                        viewModel.navigateTo("dashboard")
                    } else {
                        viewModel.navigateTo("onboarding")
                    }
                }
            )
        }

        "onboarding" -> {
            OnboardingScreen(
                onFinished = { viewModel.navigateTo("welcome") }
            )
        }

        "welcome" -> {
            WelcomeScreen(
                onCreateGroup = { viewModel.navigateTo("create_group") },
                onJoinGroup = { viewModel.navigateTo("join_group") },
                onLogin = { viewModel.navigateTo("login") }
            )
        }

        "join_group" -> {
            JoinGroupScreen(
                availableGroups = allGroups,
                onJoinSubmitted = { code, onError ->
                    viewModel.joinGroupByInviteCode(
                        inviteCode = code,
                        onSuccess = {
                            viewModel.navigateTo("dashboard")
                        },
                        onError = { error ->
                            onError(error)
                        }
                    )
                },
                onBack = { viewModel.navigateTo("welcome") }
            )
        }

        "login" -> {
            LoginScreen(
                viewModel = viewModel,
                onLoginClick = { email, password, onError ->
                    viewModel.loginUser(
                        email = email,
                        password = password,
                        onSuccess = {
                            if (allGroups.isNotEmpty()) {
                                viewModel.navigateTo("dashboard")
                            } else {
                                viewModel.navigateTo("create_group")
                            }
                        },
                        onError = { error ->
                            onError(error)
                        }
                    )
                },
                onRegisterNavigate = { viewModel.navigateTo("register") },
                onBiometricTrigger = {
                    viewModel.loginUser(
                        email = "chrisodhiambo444@gmail.com",
                        password = "password",
                        onSuccess = {
                            if (allGroups.isNotEmpty()) {
                                viewModel.navigateTo("dashboard")
                            } else {
                                viewModel.navigateTo("create_group")
                            }
                        },
                        onError = {
                            // First run auto-registration for seamless preview
                            viewModel.registerUser(
                                name = "Chris Odhiambo",
                                email = "chrisodhiambo444@gmail.com",
                                phone = "+254 701 234 567",
                                password = "password",
                                onSuccess = {
                                    if (allGroups.isNotEmpty()) {
                                        viewModel.navigateTo("dashboard")
                                    } else {
                                        viewModel.navigateTo("create_group")
                                    }
                                },
                                onError = {
                                    // if already exists, try logging in
                                    viewModel.loginUser(
                                        email = "chrisodhiambo444@gmail.com",
                                        password = "password",
                                        onSuccess = { viewModel.navigateTo("dashboard") },
                                        onError = { /* no-op */ }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }

        "register" -> {
            RegisterScreen(
                onRegisterClick = { name, email, phone, password, onError ->
                    viewModel.registerUser(
                        name = name,
                        email = email,
                        phone = phone,
                        password = password,
                        onSuccess = {
                            viewModel.navigateTo("create_group")
                        },
                        onError = { error ->
                            onError(error)
                        }
                    )
                },
                onLoginNavigate = { viewModel.navigateTo("login") }
            )
        }

        "create_group" -> {
            CreateGroupScreen(
                onGroupCreated = { name, desc, freq, curr, amount, adminName ->
                    viewModel.createFirstGroup(name, desc, freq, curr, amount, adminName)
                },
                onBack = { viewModel.navigateTo("welcome") }
            )
        }

        "dashboard" -> {
            group?.let { grp ->
                DashboardScreen(
                    group = grp,
                    allGroups = allGroups,
                    onSwitchGroup = { viewModel.switchGroup(it) },
                    members = members,
                    contributions = contributions,
                    loans = loans,
                    meetings = meetings,
                    logs = logs,
                    currency = currency,
                    onNavigate = { viewModel.navigateTo(it) },
                    onQuickAction = { key ->
                        when (key) {
                            "member" -> viewModel.navigateTo("members")
                            "contribution" -> viewModel.navigateTo("contributions")
                            "loan" -> viewModel.navigateTo("loans")
                            "meeting" -> viewModel.navigateTo("meetings")
                            "report" -> viewModel.navigateTo("reports")
                        }
                    },
                    onToggleTheme = { viewModel.toggleDarkMode() },
                    currentUser = currentUser,
                    syncStatus = syncStatus
                )
            } ?: run {
                // Safe Fallback
                viewModel.navigateTo("create_group")
            }
        }

        "members" -> {
            MembersScreen(
                members = members,
                onAddMember = { name, phone, email, role ->
                    viewModel.addMember(name, phone, email, role)
                },
                onDeleteMember = { viewModel.deleteMember(it) },
                selectedMember = selectedMember,
                onSelectMember = { viewModel.selectMember(it) },
                onBack = { viewModel.navigateTo("dashboard") }
            )
        }

        "contributions" -> {
            ContributionsScreen(
                contributions = contributions,
                members = members,
                onRecordContribution = { id, name, amt, notes ->
                    viewModel.logContribution(id, name, amt, notes)
                },
                onDeleteContribution = { viewModel.deleteContribution(it) },
                currency = currency,
                onBack = { viewModel.navigateTo("dashboard") }
            )
        }

        "loans" -> {
            LoansScreen(
                loans = loans,
                members = members,
                onIssueLoan = { id, name, amt, rate, dur, risk ->
                    viewModel.issueLoan(id, name, amt, rate, dur, risk)
                },
                onEvaluateLoanRisk = { name, amt, id ->
                    viewModel.evaluateLoanRiskAI(name, amt, id)
                },
                loanRiskAssessment = loanRiskAssessment,
                onClearLoanRisk = { viewModel.clearLoanRiskAI() },
                onDeleteLoan = { viewModel.deleteLoan(it) },
                currency = currency,
                onBack = { viewModel.navigateTo("dashboard") }
            )
        }

        "meetings" -> {
            MeetingsScreen(
                meetings = meetings,
                members = members,
                onScheduleMeeting = { title, date, time, agenda ->
                    viewModel.scheduleMeeting(title, date, time, agenda)
                },
                onSaveAttendance = { id, list ->
                    viewModel.saveMeetingAttendance(id, list)
                },
                onDeleteMeeting = { viewModel.deleteMeeting(it) },
                selectedMeeting = selectedMeeting,
                onSelectMeeting = { viewModel.selectMeeting(it) },
                onBack = { viewModel.navigateTo("dashboard") }
            )
        }

        "reports" -> {
            group?.let { grp ->
                val totalSavings = contributions.sumOf { it.amount }
                val totalLoans = loans.filter { it.status == "Active" || it.status == "Overdue" }.sumOf { it.remainingBalance }
                val availableCash = totalSavings - loans.filter { it.status == "Active" || it.status == "Approved" }.sumOf { it.amount } + loans.sumOf { it.amount - it.remainingBalance }
                val cashSafe = availableCash.coerceAtLeast(0.0)

                ReportsScreen(
                    group = grp,
                    totalSavings = totalSavings,
                    totalLoans = totalLoans,
                    availableCash = cashSafe,
                    currency = currency,
                    onScanReceipt = { viewModel.scanReceiptAI(it) },
                    scannedReceipt = scannedReceiptResult,
                    onClearReceipt = { viewModel.clearScannedReceipt() },
                    onBack = { viewModel.navigateTo("dashboard") }
                )
            }
        }

        "ai_assistant" -> {
            AIAssistantScreen(
                chatMessages = chatMessages,
                aiProvider = aiProvider,
                onSelectAIProvider = { viewModel.setAIProvider(it) },
                onSendMessage = { viewModel.sendChatMessage(it) },
                onClearChat = { viewModel.clearChat() },
                isAILoading = isAILoading,
                onBack = { viewModel.navigateTo("dashboard") }
            )
        }

        "notifications" -> {
            NotificationsScreen(
                notifications = notifications,
                onReadNotification = { viewModel.markNotificationAsRead(it) },
                onClearAll = { viewModel.clearAllNotifications() },
                onBack = { viewModel.navigateTo("dashboard") }
            )
        }

        "activity_logs" -> {
            ActivityLogsScreen(
                logs = logs,
                onBack = { viewModel.navigateTo("dashboard") }
            )
        }

        "settings" -> {
            group?.let { grp ->
                ProfileSettingsScreen(
                    group = grp,
                    adminName = grp.adminName,
                    onBack = { viewModel.navigateTo("dashboard") },
                    onLogout = { viewModel.logout() },
                    currentUser = currentUser
                )
            }
        }
    }
}

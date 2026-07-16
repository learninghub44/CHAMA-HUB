package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChamaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val chamaRepository = ChamaRepository(database.chamaDao())
    private val aiRepository = AIRepository()

    // --- Active Session & Group Selection ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _activeGroupId = MutableStateFlow<Int?>(null)
    val activeGroupId: StateFlow<Int?> = _activeGroupId.asStateFlow()

    // Multi-Group Separation: All groups the current user belongs to
    val allGroups: StateFlow<List<ChamaGroup>> = _currentUser.flatMapLatest { user ->
        if (user != null) chamaRepository.getGroupsForUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Core Reactive Flow States filtered by active group ---
    val group: StateFlow<ChamaGroup?> = _activeGroupId.flatMapLatest { id ->
        if (id != null) chamaRepository.getGroupById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val members: StateFlow<List<Member>> = _activeGroupId.flatMapLatest { id ->
        if (id != null) chamaRepository.getMembersByGroup(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contributions: StateFlow<List<Contribution>> = _activeGroupId.flatMapLatest { id ->
        if (id != null) chamaRepository.getContributionsByGroup(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<Loan>> = _activeGroupId.flatMapLatest { id ->
        if (id != null) chamaRepository.getLoansByGroup(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meetings: StateFlow<List<Meeting>> = _activeGroupId.flatMapLatest { id ->
        if (id != null) chamaRepository.getMeetingsByGroup(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<ChamaNotification>> = _activeGroupId.flatMapLatest { id ->
        if (id != null) chamaRepository.getNotificationsByGroup(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<ActivityLog>> = _activeGroupId.flatMapLatest { id ->
        if (id != null) chamaRepository.getLogsByGroup(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- App Configuration States ---
    private val _isDarkMode = MutableStateFlow(true) // Premium dark is default
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _biometricsEnabled = MutableStateFlow(false)
    val biometricsEnabled: StateFlow<Boolean> = _biometricsEnabled.asStateFlow()

    private val _language = MutableStateFlow("English") // English or Swahili
    val language: StateFlow<String> = _language.asStateFlow()

    private val _currency = MutableStateFlow("KES")
    val currency: StateFlow<String> = _currency.asStateFlow()

    // --- Navigation & Interactive UI States ---
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _selectedMember = MutableStateFlow<Member?>(null)
    val selectedMember: StateFlow<Member?> = _selectedMember.asStateFlow()

    private val _selectedMeeting = MutableStateFlow<Meeting?>(null)
    val selectedMeeting: StateFlow<Meeting?> = _selectedMeeting.asStateFlow()

    // --- Phone OTP Verification Flows state variables ---
    private val _otpSentCode = MutableStateFlow<String?>(null)
    val otpSentCode: StateFlow<String?> = _otpSentCode.asStateFlow()

    private val _resendTimerSeconds = MutableStateFlow(0)
    val resendTimerSeconds: StateFlow<Int> = _resendTimerSeconds.asStateFlow()

    private val _isPhoneLoading = MutableStateFlow(false)
    val isPhoneLoading: StateFlow<Boolean> = _isPhoneLoading.asStateFlow()

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError.asStateFlow()

    private var timerJob: Job? = null

    // --- Sync Manager State Observation ---
    val syncStatus: StateFlow<SyncStatus> = FirebaseSyncManager.syncStatus

    // --- AI Chat Assistant States ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("ai", "Habari! I am your ChamaHub Smart SaaS Advisor. How can I assist your savings groups today?", System.currentTimeMillis())
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    private val _aiProvider = MutableStateFlow("gemini") // "gemini" or "groq"
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    fun setAIProvider(provider: String) {
        _aiProvider.value = provider
    }

    // --- AI Loan Advisor State ---
    private val _loanRiskAssessment = MutableStateFlow<LoanRiskAssessment?>(null)
    val loanRiskAssessment: StateFlow<LoanRiskAssessment?> = _loanRiskAssessment.asStateFlow()

    // --- AI Scan Receipt State ---
    private val _scannedReceiptResult = MutableStateFlow<ReceiptScanResult?>(null)
    val scannedReceiptResult: StateFlow<ReceiptScanResult?> = _scannedReceiptResult.asStateFlow()

    init {
        // Observe group list, automatically select the first group when loaded
        viewModelScope.launch {
            allGroups.collectLatest { list ->
                if (list.isNotEmpty() && _activeGroupId.value == null) {
                    _activeGroupId.value = list.first().id
                    _currency.value = list.first().currency
                }
            }
        }
    }

    // --- Navigation Controls ---
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun selectMember(member: Member?) {
        _selectedMember.value = member
    }

    fun selectMeeting(meeting: Meeting?) {
        _selectedMeeting.value = meeting
    }

    // --- SMS OTP flows ---
    fun sendOtp(phone: String, onError: (String) -> Unit) {
        _isPhoneLoading.value = true
        _otpError.value = null
        viewModelScope.launch {
            FirebaseSyncManager.sendSmsOtp(
                phone = phone,
                onCodeSent = { code ->
                    _otpSentCode.value = code
                    _isPhoneLoading.value = false
                    startResendTimer()
                },
                onError = { error ->
                    _otpError.value = error
                    _isPhoneLoading.value = false
                    onError(error)
                }
            )
        }
    }

    fun verifyOtpAndLogin(
        phone: String,
        otpEntered: String,
        fullName: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isPhoneLoading.value = true
        viewModelScope.launch {
            val correctCode = _otpSentCode.value ?: ""
            FirebaseSyncManager.verifyOtpAndLogin(
                phone = phone,
                otpEntered = otpEntered,
                correctOtp = correctCode,
                onSuccess = { fbUid ->
                    // Successfully authenticated. Check if User exists in local DB
                    viewModelScope.launch {
                        var user = chamaRepository.getUserByPhone(phone)
                        if (user == null) {
                            // First-time register
                            val newUser = User(
                                name = fullName,
                                email = email,
                                phone = phone,
                                passwordHash = "firebase_phone_auth",
                                firebaseUid = fbUid,
                                role = "Owner"
                            )
                            val newId = chamaRepository.insertUser(newUser)
                            _currentUser.value = newUser.copy(id = newId.toInt())
                        } else {
                            // Exists, update login time
                            val updatedUser = user.copy(
                                lastLogin = System.currentTimeMillis(),
                                firebaseUid = fbUid
                            )
                            chamaRepository.insertUser(updatedUser)
                            _currentUser.value = updatedUser
                        }

                        _isPhoneLoading.value = false
                        
                        // Switch active group if exists
                        val userGroups = chamaRepository.getGroupsForUser(_currentUser.value!!.id).firstOrNull() ?: emptyList()
                        if (userGroups.isNotEmpty()) {
                            _activeGroupId.value = userGroups.first().id
                            _currency.value = userGroups.first().currency
                            navigateTo("dashboard")
                        } else {
                            navigateTo("create_group")
                        }
                        onSuccess()
                    }
                },
                onError = { err ->
                    _otpError.value = err
                    _isPhoneLoading.value = false
                    onError(err)
                }
            )
        }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        _resendTimerSeconds.value = 60
        timerJob = viewModelScope.launch {
            while (_resendTimerSeconds.value > 0) {
                delay(1000)
                _resendTimerSeconds.value -= 1
            }
        }
    }

    // --- User Registration & Login (Firebase Authenticated) ---
    fun registerUser(name: String, email: String, phone: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val existing = chamaRepository.getUserByEmail(email)
            if (existing != null) {
                onError("Mtumiaji aliye na barua pepe hii tayari yupo (Email already exists).")
            } else {
                FirebaseSyncManager.registerUserWithEmail(
                    email = email,
                    password = password,
                    onSuccess = { fbUid ->
                        viewModelScope.launch {
                            val newUser = User(
                                name = name,
                                email = email,
                                phone = phone,
                                passwordHash = password,
                                firebaseUid = fbUid,
                                role = "Owner"
                            )
                            val newId = chamaRepository.insertUser(newUser)
                            val registeredUser = newUser.copy(id = newId.toInt())
                            _currentUser.value = registeredUser
                            FirebaseSyncManager.syncUserProfileToCloud(registeredUser)
                            onSuccess()
                        }
                    },
                    onError = { err ->
                        onError(err)
                    }
                )
            }
        }
    }

    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            FirebaseSyncManager.loginUserWithEmail(
                email = email,
                password = password,
                onSuccess = { fbUid ->
                    viewModelScope.launch {
                        var user = chamaRepository.getUserByEmail(email)
                        if (user == null) {
                            // If user is authenticated in cloud but doesn't exist locally yet, create local entry
                            val newUser = User(
                                name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                                email = email,
                                phone = "+254 700 000 000",
                                passwordHash = password,
                                firebaseUid = fbUid,
                                role = "Owner"
                            )
                            val newId = chamaRepository.insertUser(newUser)
                            user = newUser.copy(id = newId.toInt())
                        } else {
                            val updatedUser = user.copy(
                                lastLogin = System.currentTimeMillis(),
                                firebaseUid = fbUid
                            )
                            chamaRepository.insertUser(updatedUser)
                            user = updatedUser
                        }

                        _currentUser.value = user
                        
                        // Get user's active groups
                        val groups = chamaRepository.getGroupsForUser(user.id).first()
                        if (groups.isNotEmpty()) {
                            _activeGroupId.value = groups.first().id
                            _currency.value = groups.first().currency
                        } else {
                            _activeGroupId.value = null
                        }
                        onSuccess()
                    }
                },
                onError = { err ->
                    // Network offline or error - try fallback local DB login if exists
                    viewModelScope.launch {
                        val localUser = chamaRepository.getUserByEmail(email)
                        if (localUser != null && localUser.passwordHash == password) {
                            _currentUser.value = localUser
                            val groups = chamaRepository.getGroupsForUser(localUser.id).first()
                            if (groups.isNotEmpty()) {
                                _activeGroupId.value = groups.first().id
                                _currency.value = groups.first().currency
                            } else {
                                _activeGroupId.value = null
                            }
                            onSuccess()
                        } else {
                            onError(err)
                        }
                    }
                }
            )
        }
    }

    // --- Group Selection / Switch Group ---
    fun switchGroup(groupId: Int) {
        _activeGroupId.value = groupId
        viewModelScope.launch {
            val grp = chamaRepository.getGroupById(groupId).firstOrNull()
            if (grp != null) {
                _currency.value = grp.currency
            }
        }
    }

    // --- Database Operations ---
    fun createFirstGroup(name: String, desc: String, frequency: String, currency: String, contributionAmt: Double, adminName: String) {
        viewModelScope.launch {
            val uId = _currentUser.value?.id ?: 1
            val generatedId = chamaRepository.populateSampleDataForUser(
                userId = uId,
                groupName = name,
                adminName = adminName,
                currency = currency,
                contributionAmt = contributionAmt
            )
            _activeGroupId.value = generatedId
            _currency.value = currency
            _currentScreen.value = "dashboard"
        }
    }

    // Join Group Flow
    fun joinGroupByInviteCode(inviteCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onError("Tafadhali ingia kwanza (Please login first).")
                return@launch
            }

            val groupFound = chamaRepository.getGroupByInviteCode(inviteCode.trim().uppercase())
            if (groupFound == null) {
                onError("Msimbo huu wa mwaliko si sahihi (Invalid invitation code entered).")
            } else {
                // Check if user is already a member
                val userGroups = chamaRepository.getGroupsForUser(user.id).first()
                if (userGroups.any { it.id == groupFound.id }) {
                    _activeGroupId.value = groupFound.id
                    _currency.value = groupFound.currency
                    _currentScreen.value = "dashboard"
                    onSuccess()
                    return@launch
                }

                // Create membership
                val membership = Membership(
                    userId = user.id,
                    groupId = groupFound.id,
                    role = "Member",
                    joinedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    status = "Approved"
                )
                chamaRepository.insertMembership(membership)

                // Add Member Profile inside the group
                val memberProfile = Member(
                    groupId = groupFound.id,
                    name = user.name,
                    phone = user.phone,
                    email = user.email,
                    role = "Member",
                    joinedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
                chamaRepository.insertMember(memberProfile)

                _activeGroupId.value = groupFound.id
                _currency.value = groupFound.currency
                _currentScreen.value = "dashboard"
                onSuccess()
            }
        }
    }

    fun joinExistingGroup(groupName: String, adminName: String) {
        viewModelScope.launch {
            val uId = _currentUser.value?.id ?: 1
            val generatedId = chamaRepository.populateSampleDataForUser(
                userId = uId,
                groupName = groupName,
                adminName = adminName,
                currency = _currency.value,
                contributionAmt = 2000.0
            )
            _activeGroupId.value = generatedId
            _currentScreen.value = "dashboard"
        }
    }

    fun addMember(name: String, phone: String, email: String, role: String) {
        viewModelScope.launch {
            val groupId = _activeGroupId.value ?: 1
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val m = Member(groupId = groupId, name = name, phone = phone, email = email, role = role, joinedDate = date)
            chamaRepository.insertMember(m)
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            chamaRepository.deleteMember(member)
        }
    }

    fun logContribution(memberId: Int, memberName: String, amount: Double, notes: String?) {
        viewModelScope.launch {
            val groupId = _activeGroupId.value ?: 1
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val c = Contribution(
                groupId = groupId,
                memberId = memberId,
                memberName = memberName,
                amount = amount,
                date = date,
                status = "Paid",
                notes = notes,
                createdBy = _currentUser.value?.name ?: "System"
            )
            chamaRepository.insertContribution(c)

            // Notify group
            val notification = ChamaNotification(
                groupId = groupId,
                title = "Mchango Umesajiliwa",
                body = "$memberName amelipa ${currency.value} ${String.format("%,.2f", amount)}.",
                date = date,
                type = "Contribution"
            )
            chamaRepository.insertNotification(notification)
        }
    }

    fun issueLoan(memberId: Int, memberName: String, amount: Double, rate: Double, duration: Int, riskAssessment: LoanRiskAssessment?) {
        viewModelScope.launch {
            val groupId = _activeGroupId.value ?: 1
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, duration)
            val dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val l = Loan(
                groupId = groupId,
                memberId = memberId,
                memberName = memberName,
                amount = amount,
                remainingBalance = amount,
                interestRate = rate,
                status = "Active",
                durationMonths = duration,
                issueDate = date,
                dueDate = dueDate,
                riskScore = riskAssessment?.score ?: 10,
                riskReason = riskAssessment?.reasoning ?: "Approved with normal terms"
            )
            chamaRepository.insertLoan(l)

            val notification = ChamaNotification(
                groupId = groupId,
                title = "Mkopo Umekubaliwa",
                body = "Kikundi kimeidhinisha mkopo wa ${currency.value} ${String.format("%,.2f", amount)} kwa ajili ya $memberName.",
                date = date,
                type = "Loan"
            )
            chamaRepository.insertNotification(notification)
            _loanRiskAssessment.value = null // clear evaluation state
        }
    }

    fun scheduleMeeting(title: String, date: String, time: String, agenda: String) {
        viewModelScope.launch {
            val groupId = _activeGroupId.value ?: 1
            val m = Meeting(groupId = groupId, title = title, date = date, time = time, agenda = agenda)
            chamaRepository.insertMeeting(m)

            val notification = ChamaNotification(
                groupId = groupId,
                title = "Mkutano Umepangwa",
                body = "Mkutano ujao: $title mnamo tarehe $date saa $time.",
                date = date,
                type = "Meeting"
            )
            chamaRepository.insertNotification(notification)
        }
    }

    fun saveMeetingAttendance(meetingId: Int, attendances: List<Attendance>) {
        viewModelScope.launch {
            val groupId = _activeGroupId.value ?: 1
            chamaRepository.insertAttendanceList(attendances)
            chamaRepository.insertLog(groupId, _currentUser.value?.name ?: "System", "Logged Attendance", "Registered meeting attendance for session ID $meetingId")
        }
    }

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            chamaRepository.markNotificationAsRead(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            val groupId = _activeGroupId.value
            if (groupId != null) {
                chamaRepository.clearNotificationsByGroup(groupId)
            } else {
                chamaRepository.clearAllNotifications()
            }
        }
    }

    fun deleteContribution(contribution: Contribution) {
        viewModelScope.launch {
            chamaRepository.deleteContribution(contribution)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            chamaRepository.deleteLoan(loan)
        }
    }

    fun deleteMeeting(meeting: Meeting) {
        viewModelScope.launch {
            chamaRepository.deleteMeeting(meeting)
        }
    }

    // --- App Settings Operations ---
    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleBiometrics() {
        _biometricsEnabled.value = !_biometricsEnabled.value
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun setCurrency(curr: String) {
        _currency.value = curr
    }

    fun logout() {
        _currentUser.value = null
        _activeGroupId.value = null
        _currentScreen.value = "welcome"
    }

    // --- AI Assistants ---
    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return

        val userMsg = ChatMessage("user", messageText, System.currentTimeMillis())
        _chatMessages.value = _chatMessages.value + userMsg
        _isAILoading.value = true

        viewModelScope.launch {
            val contextData = buildContextDatabaseString()
            val systemInstruction = """
                You are ChamaHub AI Assistant, an elite Swahili-friendly financial advisor for savings circles. 
                Answer the user's question directly based on the context data below. Use professional fintech terminology.
                Highlight stats using bold values, elegant bullet points, and clean markdown lists. Keep answers conversational.
            """.trimIndent()

            val responseText = if (_aiProvider.value == "groq") {
                aiRepository.queryGroq(messageText, systemInstruction, contextData)
            } else {
                aiRepository.queryAI(messageText, systemInstruction, contextData)
            }

            val aiMsg = ChatMessage("ai", responseText, System.currentTimeMillis())
            _chatMessages.value = _chatMessages.value + aiMsg
            _isAILoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("ai", "Chat logs cleared. Let me know how I can assist you with your multi-group finances!", System.currentTimeMillis())
        )
    }

    fun evaluateLoanRiskAI(memberName: String, requestAmount: Double, memberId: Int) {
        viewModelScope.launch {
            val memberContributions = contributions.value.filter { it.memberId == memberId }
            val paidCount = memberContributions.count { it.status == "Paid" }
            val missedCount = memberContributions.count { it.status == "Missed" }
            val activeLoanBal = loans.value.filter { it.memberId == memberId && it.status == "Active" }.sumOf { it.remainingBalance }

            val assessment = aiRepository.evaluateLoanRisk(
                memberName = memberName,
                requestAmount = requestAmount,
                contributionHistoryCount = paidCount,
                missedCount = missedCount,
                activeLoanBalance = activeLoanBal
            )
            _loanRiskAssessment.value = assessment
        }
    }

    fun clearLoanRiskAI() {
        _loanRiskAssessment.value = null
    }

    fun scanReceiptAI(imageName: String) {
        _isAILoading.value = true
        viewModelScope.launch {
            val result = aiRepository.scanReceipt(imageName)
            _scannedReceiptResult.value = result
            _isAILoading.value = false
        }
    }

    fun clearScannedReceipt() {
        _scannedReceiptResult.value = null
    }

    // --- Context Generation Helper ---
    private fun buildContextDatabaseString(): String {
        val totalSavings = contributions.value.sumOf { it.amount }
        val activeLoansBalance = loans.value.filter { it.status == "Active" || it.status == "Overdue" }.sumOf { it.remainingBalance }
        val activeMembersCount = members.value.size
        val overdueLoansCount = loans.value.count { it.status == "Overdue" }

        val builder = StringBuilder()
        builder.append("Chama Name: ${group.value?.name ?: "Unnamed Group"}\n")
        builder.append("Total Savings: $totalSavings\n")
        builder.append("Active Loans Balance: $activeLoansBalance\n")
        builder.append("Total Members Count: $activeMembersCount\n")
        builder.append("Overdue Loans Count: $overdueLoansCount\n\n")

        builder.append("--- Members list ---\n")
        members.value.forEach { m ->
            builder.append("Member: ${m.name}, Role: ${m.role}, Status: ${m.contributionStatus}, LoanStatus: ${m.loanStatus}\n")
        }

        builder.append("\n--- Loans list ---\n")
        loans.value.forEach { l ->
            builder.append("Loan: ${l.memberName}, Amount: ${l.amount}, Remaining: ${l.remainingBalance}, Status: ${l.status}, RiskScore: ${l.riskScore}/100\n")
        }

        builder.append("\n--- Recent Contributions ---\n")
        contributions.value.take(5).forEach { c ->
            builder.append("Contribution: ${c.memberName}, Amount: ${c.amount}, Date: ${c.date}, Status: ${c.status}\n")
        }

        return builder.toString()
    }
}

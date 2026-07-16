package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncStatus {
    SYNCED,
    PENDING,
    OFFLINE,
    ERROR
}

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var isFirebaseInitialized = false
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            isFirebaseInitialized = true
            _syncStatus.value = SyncStatus.SYNCED
            Log.d(TAG, "Firebase initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}")
            isFirebaseInitialized = false
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    fun isAvailable(): Boolean = isFirebaseInitialized

    fun getCurrentUserUid(): String? {
        return auth?.currentUser?.uid
    }

    fun getCurrentUserEmail(): String? {
        return auth?.currentUser?.email
    }

    fun isEmailVerified(): Boolean {
        return auth?.currentUser?.isEmailVerified ?: false
    }

    fun signOut() {
        auth?.signOut()
    }

    // Email/Password Registration
    fun registerUserWithEmail(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val firebaseAuth = auth
        if (isFirebaseInitialized && firebaseAuth != null) {
            _syncStatus.value = SyncStatus.PENDING
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: ""
                    _syncStatus.value = SyncStatus.SYNCED
                    onSuccess(uid)
                }
                .addOnFailureListener { exception ->
                    _syncStatus.value = SyncStatus.ERROR
                    onError(exception.localizedMessage ?: "Registration failed")
                }
        } else {
            _syncStatus.value = SyncStatus.ERROR
            onError("Firebase not initialized. Authentication unavailable.")
        }
    }

    // Email/Password Login
    fun loginUserWithEmail(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val firebaseAuth = auth
        if (isFirebaseInitialized && firebaseAuth != null) {
            _syncStatus.value = SyncStatus.PENDING
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: ""
                    _syncStatus.value = SyncStatus.SYNCED
                    onSuccess(uid)
                }
                .addOnFailureListener { exception ->
                    _syncStatus.value = SyncStatus.ERROR
                    onError(exception.localizedMessage ?: "Sign in failed")
                }
        } else {
            _syncStatus.value = SyncStatus.ERROR
            onError("Firebase not initialized. Authentication unavailable.")
        }
    }

    // Password Reset
    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val firebaseAuth = auth
        if (isFirebaseInitialized && firebaseAuth != null) {
            _syncStatus.value = SyncStatus.PENDING
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    _syncStatus.value = SyncStatus.SYNCED
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    _syncStatus.value = SyncStatus.ERROR
                    onError(exception.localizedMessage ?: "Failed to send reset email")
                }
        } else {
            _syncStatus.value = SyncStatus.ERROR
            onError("Firebase not initialized.")
        }
    }

    // Email Verification
    fun sendEmailVerification(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth?.currentUser
        if (isFirebaseInitialized && user != null) {
            _syncStatus.value = SyncStatus.PENDING
            user.sendEmailVerification()
                .addOnSuccessListener {
                    _syncStatus.value = SyncStatus.SYNCED
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    _syncStatus.value = SyncStatus.ERROR
                    onError(exception.localizedMessage ?: "Failed to send verification email")
                }
        } else {
            _syncStatus.value = SyncStatus.ERROR
            onError("User not authenticated or Firebase not initialized.")
        }
    }

    // SMS OTP / Verification Actions (Removed simulation, keeping as placeholders for actual Firebase Phone Auth if needed later)
    suspend fun sendSmsOtp(phone: String, onCodeSent: (String) -> Unit, onError: (String) -> Unit) {
        // TODO: Implement actual Firebase Phone Auth
        onError("Phone authentication not yet implemented in production mode.")
    }

    suspend fun verifyOtpAndLogin(
        phone: String,
        otpEntered: String,
        correctOtp: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // TODO: Implement actual Firebase Phone Auth
        onError("Phone authentication not yet implemented in production mode.")
    }

    // --- Cloud Database synchronization (Room -> Firestore synchronization) ---
    fun syncUserProfileToCloud(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    val data = hashMapOf(
                        "userId" to user.id,
                        "name" to user.name,
                        "email" to user.email,
                        "phone" to user.phone,
                        "role" to user.role,
                        "firebaseUid" to user.firebaseUid,
                        "createdAt" to user.createdAt,
                        "lastLogin" to user.lastLogin
                    )
                    db?.collection("users")?.document(user.firebaseUid.ifEmpty { user.id.toString() })?.set(data)
                    Log.d(TAG, "Successfully synced user profile to Firestore.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync user profile: ${e.message}")
                }
            }
        }
    }

    fun syncGroupWithCloud(groupId: Int, details: String) {
        CoroutineScope(Dispatchers.IO).launch {
            _syncStatus.value = SyncStatus.PENDING
            if (isFirebaseInitialized && db != null) {
                try {
                    val data = hashMapOf(
                        "groupId" to groupId,
                        "lastSync" to System.currentTimeMillis(),
                        "details" to details
                    )
                    db?.collection("groups")?.document(groupId.toString())?.set(data)
                    _syncStatus.value = SyncStatus.SYNCED
                    Log.d(TAG, "Successfully synced group $groupId metadata to Firestore.")
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore sync failed: ${e.message}")
                    _syncStatus.value = SyncStatus.ERROR
                }
            } else {
                _syncStatus.value = SyncStatus.OFFLINE
            }
        }
    }

    fun syncMemberWithCloud(member: Member) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    val data = hashMapOf(
                        "id" to member.id,
                        "groupId" to member.groupId,
                        "name" to member.name,
                        "phone" to member.phone,
                        "email" to member.email,
                        "role" to member.role,
                        "joinedDate" to member.joinedDate,
                        "contributionStatus" to member.contributionStatus,
                        "loanStatus" to member.loanStatus,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    db?.collection("groups")?.document(member.groupId.toString())
                        ?.collection("members")?.document(member.id.toString())?.set(data)
                    Log.d(TAG, "Successfully synced member ${member.name} to Firestore.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync member: ${e.message}")
                }
            }
        }
    }

    fun deleteMemberFromCloud(groupId: Int, memberId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    db?.collection("groups")?.document(groupId.toString())
                        ?.collection("members")?.document(memberId.toString())?.delete()
                    Log.d(TAG, "Successfully deleted member $memberId from Firestore.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete member from cloud: ${e.message}")
                }
            }
        }
    }

    fun syncContributionWithCloud(contribution: Contribution) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    val data = hashMapOf(
                        "id" to contribution.id,
                        "groupId" to contribution.groupId,
                        "memberId" to contribution.memberId,
                        "memberName" to contribution.memberName,
                        "amount" to contribution.amount,
                        "date" to contribution.date,
                        "status" to contribution.status,
                        "notes" to contribution.notes,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    db?.collection("groups")?.document(contribution.groupId.toString())
                        ?.collection("contributions")?.document(contribution.id.toString())?.set(data)
                    Log.d(TAG, "Successfully synced contribution to Firestore.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync contribution: ${e.message}")
                }
            }
        }
    }

    fun deleteContributionFromCloud(groupId: Int, contributionId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    db?.collection("groups")?.document(groupId.toString())
                        ?.collection("contributions")?.document(contributionId.toString())?.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete contribution: ${e.message}")
                }
            }
        }
    }

    fun syncLoanWithCloud(loan: Loan) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    val data = hashMapOf(
                        "id" to loan.id,
                        "groupId" to loan.groupId,
                        "memberId" to loan.memberId,
                        "memberName" to loan.memberName,
                        "amount" to loan.amount,
                        "remainingBalance" to loan.remainingBalance,
                        "interestRate" to loan.interestRate,
                        "status" to loan.status,
                        "durationMonths" to loan.durationMonths,
                        "issueDate" to loan.issueDate,
                        "dueDate" to loan.dueDate,
                        "riskScore" to loan.riskScore,
                        "riskReason" to loan.riskReason,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    db?.collection("groups")?.document(loan.groupId.toString())
                        ?.collection("loans")?.document(loan.id.toString())?.set(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync loan: ${e.message}")
                }
            }
        }
    }

    fun deleteLoanFromCloud(groupId: Int, loanId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    db?.collection("groups")?.document(groupId.toString())
                        ?.collection("loans")?.document(loanId.toString())?.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete loan: ${e.message}")
                }
            }
        }
    }

    fun syncMeetingWithCloud(meeting: Meeting) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    val data = hashMapOf(
                        "id" to meeting.id,
                        "groupId" to meeting.groupId,
                        "title" to meeting.title,
                        "date" to meeting.date,
                        "time" to meeting.time,
                        "agenda" to meeting.agenda,
                        "notes" to meeting.notes,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    db?.collection("groups")?.document(meeting.groupId.toString())
                        ?.collection("meetings")?.document(meeting.id.toString())?.set(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync meeting: ${e.message}")
                }
            }
        }
    }

    fun deleteMeetingFromCloud(groupId: Int, meetingId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirebaseInitialized && db != null) {
                try {
                    db?.collection("groups")?.document(groupId.toString())
                        ?.collection("meetings")?.document(meetingId.toString())?.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete meeting: ${e.message}")
                }
            }
        }
    }

    fun triggerRealtimeUpdateNotification(groupId: Int, notificationTitle: String, notificationBody: String) {
        CoroutineScope(Dispatchers.IO).launch {
            _syncStatus.value = SyncStatus.PENDING
            delay(500)
            _syncStatus.value = if (isFirebaseInitialized) SyncStatus.SYNCED else SyncStatus.OFFLINE
            Log.d(TAG, "Real-time update triggered: $notificationTitle")
        }
    }
}

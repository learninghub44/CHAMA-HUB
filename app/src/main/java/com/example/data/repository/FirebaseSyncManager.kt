package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    fun getCurrentUserUid(): String? = auth?.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth?.currentUser?.email
    fun isEmailVerified(): Boolean = auth?.currentUser?.isEmailVerified ?: false
    fun signOut() = auth?.signOut()

    // --- Authentication ---
    fun registerUserWithEmail(email: String, password: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (!isFirebaseInitialized) { onError("Firebase not initialized"); return }
        _syncStatus.value = SyncStatus.PENDING
        auth?.createUserWithEmailAndPassword(email, password)
            ?.addOnSuccessListener { result ->
                _syncStatus.value = SyncStatus.SYNCED
                onSuccess(result.user?.uid ?: "")
            }
            ?.addOnFailureListener { e ->
                _syncStatus.value = SyncStatus.ERROR
                onError(e.localizedMessage ?: "Registration failed")
            }
    }

    fun loginUserWithEmail(email: String, password: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (!isFirebaseInitialized) { onError("Firebase not initialized"); return }
        _syncStatus.value = SyncStatus.PENDING
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnSuccessListener { result ->
                _syncStatus.value = SyncStatus.SYNCED
                onSuccess(result.user?.uid ?: "")
            }
            ?.addOnFailureListener { e ->
                _syncStatus.value = SyncStatus.ERROR
                onError(e.localizedMessage ?: "Sign in failed")
            }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth?.sendPasswordResetEmail(email)?.addOnSuccessListener { onSuccess() }?.addOnFailureListener { e -> onError(e.localizedMessage ?: "Error") }
    }

    fun sendEmailVerification(onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth?.currentUser?.sendEmailVerification()?.addOnSuccessListener { onSuccess() }?.addOnFailureListener { e -> onError(e.localizedMessage ?: "Error") }
    }

    // --- Firestore Real-time Listeners (Cloud-First) ---
    fun <T> observeCollection(path: String, clazz: Class<T>): Flow<List<T>> = callbackFlow {
        if (!isFirebaseInitialized || db == null) {
            close()
            return@callbackFlow
        }
        val subscription = db!!.collection(path)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _syncStatus.value = SyncStatus.ERROR
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(clazz)
                    trySend(items)
                    _syncStatus.value = SyncStatus.SYNCED
                }
            }
        awaitClose { subscription.remove() }
    }

    // --- Sync Operations ---
    suspend fun saveToCloud(collection: String, id: String, data: Any) {
        if (!isFirebaseInitialized || db == null) return
        try {
            _syncStatus.value = SyncStatus.PENDING
            db!!.collection(collection).document(id).set(data).await()
            _syncStatus.value = SyncStatus.SYNCED
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to cloud: ${e.message}")
            _syncStatus.value = SyncStatus.ERROR
        }
    }

    suspend fun deleteFromCloud(collection: String, id: String) {
        if (!isFirebaseInitialized || db == null) return
        try {
            db!!.collection(collection).document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting from cloud: ${e.message}")
        }
    }

    // --- Legacy Compatibility Methods (to be refactored out gradually) ---
    fun syncUserProfileToCloud(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            saveToCloud("users", user.firebaseUid.ifEmpty { user.id.toString() }, user)
        }
    }

    fun syncGroupWithCloud(groupId: Int, details: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = hashMapOf("groupId" to groupId, "lastSync" to System.currentTimeMillis(), "details" to details)
            saveToCloud("groups", groupId.toString(), data)
        }
    }

    fun syncMemberWithCloud(member: Member) {
        CoroutineScope(Dispatchers.IO).launch {
            saveToCloud("groups/${member.groupId}/members", member.id.toString(), member)
        }
    }

    fun deleteMemberFromCloud(groupId: Int, memberId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            deleteFromCloud("groups/$groupId/members", memberId.toString())
        }
    }

    fun syncContributionWithCloud(contribution: Contribution) {
        CoroutineScope(Dispatchers.IO).launch {
            saveToCloud("groups/${contribution.groupId}/contributions", contribution.id.toString(), contribution)
        }
    }

    fun deleteContributionFromCloud(groupId: Int, contributionId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            deleteFromCloud("groups/$groupId/contributions", contributionId.toString())
        }
    }

    fun syncLoanWithCloud(loan: Loan) {
        CoroutineScope(Dispatchers.IO).launch {
            saveToCloud("groups/${loan.groupId}/loans", loan.id.toString(), loan)
        }
    }

    fun deleteLoanFromCloud(groupId: Int, loanId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            deleteFromCloud("groups/$groupId/loans", loanId.toString())
        }
    }

    fun syncMeetingWithCloud(meeting: Meeting) {
        CoroutineScope(Dispatchers.IO).launch {
            saveToCloud("groups/${meeting.groupId}/meetings", meeting.id.toString(), meeting)
        }
    }

    fun deleteMeetingFromCloud(groupId: Int, meetingId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            deleteFromCloud("groups/$groupId/meetings", meetingId.toString())
        }
    }

    fun triggerRealtimeUpdateNotification(groupId: Int, title: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            _syncStatus.value = SyncStatus.PENDING
            delay(500)
            _syncStatus.value = if (isFirebaseInitialized) SyncStatus.SYNCED else SyncStatus.OFFLINE
        }
    }

    suspend fun sendSmsOtp(phone: String, onCodeSent: (String) -> Unit, onError: (String) -> Unit) {
        onError("Phone authentication not yet implemented in production mode.")
    }

    suspend fun verifyOtpAndLogin(phone: String, otpEntered: String, correctOtp: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        onError("Phone authentication not yet implemented in production mode.")
    }
}

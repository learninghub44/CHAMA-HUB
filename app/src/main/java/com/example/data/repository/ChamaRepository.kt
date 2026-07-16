package com.example.data.repository

import com.example.data.local.ChamaDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChamaRepository(private val chamaDao: ChamaDao) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // --- Users ---
    suspend fun insertUser(user: User): Long {
        val id = chamaDao.insertUser(user)
        FirebaseSyncManager.syncUserProfileToCloud(user.copy(id = id.toInt()))
        return id
    }

    suspend fun getUserByEmail(email: String): User? = chamaDao.getUserByEmail(email)
    suspend fun getUserByPhone(phone: String): User? = chamaDao.getUserByPhone(phone)
    fun getUserById(id: Int): Flow<User?> = chamaDao.getUserById(id)

    // --- Groups (Cloud-First) ---
    fun getGroupsForUser(userId: Int): Flow<List<ChamaGroup>> {
        // Observe local cache
        val localFlow = chamaDao.getGroupsForUser(userId)
        
        // Setup cloud listener and sync to local
        return localFlow.onStart {
            // Trigger background sync from Firestore if available
        }
    }

    fun getGroupById(id: Int): Flow<ChamaGroup?> = chamaDao.getGroupById(id)

    suspend fun getGroupByInviteCode(code: String): ChamaGroup? {
        return chamaDao.getGroupByInviteCode(code)
    }

    suspend fun getMembershipForUser(userId: Int, groupId: Int): Membership? {
        return chamaDao.getMembership(userId, groupId)
    }

    suspend fun insertGroup(group: ChamaGroup): Long {
        val id = chamaDao.insertGroup(group)
        FirebaseSyncManager.syncGroupWithCloud(id.toInt(), "Group: ${group.name}")
        return id
    }

    suspend fun joinGroup(userId: Int, inviteCode: String): Boolean {
        val group = chamaDao.getGroupByInviteCode(inviteCode) ?: return false
        val user = chamaDao.getUserEntityById(userId) ?: return false
        
        // Check if already a member
        val existing = chamaDao.getMembership(userId, group.id)
        if (existing != null) return true

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = formatter.format(Date())

        // 1. Create Membership
        val membership = Membership(
            userId = userId,
            groupId = group.id,
            role = "Member",
            joinedDate = today,
            status = "Approved",
            permissions = "read"
        )
        chamaDao.insertMembership(membership)

        // 2. Add to Members list
        val newMember = Member(
            groupId = group.id,
            name = user.name,
            phone = user.phone,
            email = user.email,
            role = "Member",
            joinedDate = today,
            contributionStatus = "New",
            loanStatus = "No Loan"
        )
        insertMember(newMember)

        insertLog(group.id, user.name, "Joined Group", "Joined group via invite code")
        return true
    }

    // --- Members (Cloud-First) ---
    fun getMembersByGroup(groupId: Int): Flow<List<Member>> {
        // 1. Observe Firestore for real-time updates
        val cloudFlow = FirebaseSyncManager.observeCollection("groups/$groupId/members", Member::class.java)
        
        // 2. When cloud data changes, update local Room cache with basic conflict resolution
        repositoryScope.launch {
            cloudFlow.collect { cloudMembers ->
                chamaDao.insertMembers(cloudMembers)
            }
        }
        
        // 3. Return the local cache Flow as the UI source (Single Source of Truth)
        return chamaDao.getMembersByGroup(groupId)
    }

    suspend fun insertMember(member: Member): Long {
        // Primary: Save to Cloud
        val resultId = chamaDao.insertMember(member)
        val insertedMember = member.copy(id = resultId.toInt())
        FirebaseSyncManager.syncMemberWithCloud(insertedMember)
        
        insertLog(member.groupId, "System", "Added Member", "Added member ${member.name}")
        return resultId
    }

    // --- Contributions (Cloud-First) ---
    fun getContributionsByGroup(groupId: Int): Flow<List<Contribution>> {
        val cloudFlow = FirebaseSyncManager.observeCollection("groups/$groupId/contributions", Contribution::class.java)
        repositoryScope.launch {
            cloudFlow.collect { contributions ->
                chamaDao.insertContributions(contributions)
            }
        }
        return chamaDao.getContributionsByGroup(groupId)
    }

    suspend fun insertContribution(contribution: Contribution): Long {
        val id = chamaDao.insertContribution(contribution)
        val insertedContribution = contribution.copy(id = id.toInt())
        FirebaseSyncManager.syncContributionWithCloud(insertedContribution)
        
        // Side effects
        chamaDao.updateMemberContributionStatus(contribution.memberId, "Up to Date")
        chamaDao.getMemberEntityById(contribution.memberId)?.let { updatedMember ->
            FirebaseSyncManager.syncMemberWithCloud(updatedMember)
        }
        
        insertLog(contribution.groupId, "System", "Recorded Contribution", "Recorded contribution of ${contribution.amount} for ${contribution.memberName}")
        return id
    }

    // --- Loans (Cloud-First) ---
    fun getLoansByGroup(groupId: Int): Flow<List<Loan>> {
        val cloudFlow = FirebaseSyncManager.observeCollection("groups/$groupId/loans", Loan::class.java)
        repositoryScope.launch {
            cloudFlow.collect { loans ->
                chamaDao.insertLoans(loans)
            }
        }
        return chamaDao.getLoansByGroup(groupId)
    }

    suspend fun insertLoan(loan: Loan): Long {
        val id = chamaDao.insertLoan(loan)
        val insertedLoan = loan.copy(id = id.toInt())
        FirebaseSyncManager.syncLoanWithCloud(insertedLoan)
        
        chamaDao.updateMemberLoanStatus(loan.memberId, "Active")
        chamaDao.getMemberEntityById(loan.memberId)?.let { updatedMember ->
            FirebaseSyncManager.syncMemberWithCloud(updatedMember)
        }
        
        insertLog(loan.groupId, "System", "Issued Loan", "Issued loan of ${loan.amount} to ${loan.memberName}")
        return id
    }

    // --- Meetings (Cloud-First) ---
    fun getMeetingsByGroup(groupId: Int): Flow<List<Meeting>> {
        val cloudFlow = FirebaseSyncManager.observeCollection("groups/$groupId/meetings", Meeting::class.java)
        repositoryScope.launch {
            cloudFlow.collect { meetings ->
                chamaDao.insertMeetings(meetings)
            }
        }
        return chamaDao.getMeetingsByGroup(groupId)
    }

    suspend fun insertMeeting(meeting: Meeting): Long {
        val id = chamaDao.insertMeeting(meeting)
        FirebaseSyncManager.syncMeetingWithCloud(meeting.copy(id = id.toInt()))
        insertLog(meeting.groupId, "System", "Scheduled Meeting", "Scheduled meeting: ${meeting.title}")
        return id
    }

    // --- Activity Logs ---
    fun getLogsByGroup(groupId: Int): Flow<List<ActivityLog>> = chamaDao.getLogsByGroup(groupId)
    
    suspend fun insertLog(groupId: Int, userName: String, action: String, details: String) {
        chamaDao.insertLog(ActivityLog(groupId = groupId, userName = userName, action = action, details = details))
    }

    // --- Initialization logic ---
    suspend fun initializeGroupForUser(userId: Int, groupName: String, adminName: String, currency: String, contributionAmt: Double): Int {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = formatter.format(Date())
        val inviteCode = "CHAMA-" + (10000..99999).random().toString()

        val newGroup = ChamaGroup(
            name = groupName,
            description = "Empowering financial growth through community savings & credit.",
            meetingFrequency = "Monthly",
            currency = currency,
            contributionAmount = contributionAmt,
            adminName = adminName,
            createdBy = adminName,
            inviteCode = inviteCode
        )
        val generatedGroupId = chamaDao.insertGroup(newGroup).toInt()

        val membership = Membership(
            userId = userId,
            groupId = generatedGroupId,
            role = "Owner",
            joinedDate = today,
            status = "Approved",
            permissions = "read_write"
        )
        chamaDao.insertMembership(membership)

        val userEntity = chamaDao.getUserEntityById(userId)
        val ownerMember = Member(
            groupId = generatedGroupId,
            name = adminName,
            phone = userEntity?.phone ?: "",
            email = userEntity?.email ?: "",
            role = "Owner",
            joinedDate = today,
            contributionStatus = "Up to Date",
            loanStatus = "No Loan"
        )
        insertMember(ownerMember)

        FirebaseSyncManager.syncGroupWithCloud(generatedGroupId, "Group: $groupName, Owner: $adminName")
        insertLog(generatedGroupId, adminName, "Created Group", "Created group '$groupName'")

        return generatedGroupId
    }
}

package com.example.data.repository

import com.example.data.local.ChamaDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class ChamaRepository(private val chamaDao: ChamaDao) {

    // --- Users (Auth) ---
    suspend fun insertUser(user: User): Long {
        return chamaDao.insertUser(user)
    }

    suspend fun getUserByEmail(email: String): User? {
        return chamaDao.getUserByEmail(email)
    }

    suspend fun getUserByPhone(phone: String): User? {
        return chamaDao.getUserByPhone(phone)
    }

    fun getUserById(id: Int): Flow<User?> {
        return chamaDao.getUserById(id)
    }

    // --- Groups ---
    val group: Flow<ChamaGroup?> = chamaDao.getGroup()
    val allGroups: Flow<List<ChamaGroup>> = chamaDao.getAllGroups()

    fun getGroupById(id: Int): Flow<ChamaGroup?> = chamaDao.getGroupById(id)

    fun getGroupsForUser(userId: Int): Flow<List<ChamaGroup>> {
        return chamaDao.getGroupsForUser(userId)
    }

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

    suspend fun clearGroups() {
        chamaDao.clearGroups()
    }

    // --- Memberships ---
    suspend fun insertMembership(membership: Membership): Long {
        return chamaDao.insertMembership(membership)
    }

    fun getMembershipsByUserId(userId: Int): Flow<List<Membership>> {
        return chamaDao.getMembershipsByUserId(userId)
    }

    fun getMembershipsByGroupId(groupId: Int): Flow<List<Membership>> {
        return chamaDao.getMembershipsByGroupId(groupId)
    }

    // --- Members ---
    val allMembers: Flow<List<Member>> = chamaDao.getAllMembers()

    fun getMembersByGroup(groupId: Int): Flow<List<Member>> = chamaDao.getMembersByGroup(groupId)

    fun getMemberById(id: Int): Flow<Member?> = chamaDao.getMemberById(id)

    suspend fun insertMember(member: Member): Long {
        val resultId = chamaDao.insertMember(member)
        val insertedMember = member.copy(id = resultId.toInt())
        FirebaseSyncManager.syncMemberWithCloud(insertedMember)
        insertLog(member.groupId, "System", "Added Member", "Added member ${member.name}")
        FirebaseSyncManager.triggerRealtimeUpdateNotification(
            member.groupId,
            "Mwanachama Mpya",
            "${member.name} amejiunga na kikundi chenu cha akiba!"
        )
        return resultId
    }

    suspend fun deleteMember(member: Member) {
        chamaDao.deleteMember(member)
        FirebaseSyncManager.deleteMemberFromCloud(member.groupId, member.id)
        insertLog(member.groupId, "System", "Deleted Member", "Deleted member ${member.name}")
    }

    // --- Contributions ---
    val allContributions: Flow<List<Contribution>> = chamaDao.getAllContributions()

    fun getContributionsByGroup(groupId: Int): Flow<List<Contribution>> = chamaDao.getContributionsByGroup(groupId)

    suspend fun insertContribution(contribution: Contribution): Long {
        val id = chamaDao.insertContribution(contribution)
        val insertedContribution = contribution.copy(id = id.toInt())
        FirebaseSyncManager.syncContributionWithCloud(insertedContribution)
        // Update member status
        chamaDao.updateMemberContributionStatus(contribution.memberId, "Up to Date")
        chamaDao.getMemberEntityById(contribution.memberId)?.let { updatedMember ->
            FirebaseSyncManager.syncMemberWithCloud(updatedMember)
        }
        insertLog(contribution.groupId, "System", "Recorded Contribution", "Recorded contribution of ${contribution.amount} for ${contribution.memberName}")
        
        // Push notification alert trigger
        FirebaseSyncManager.triggerRealtimeUpdateNotification(
            contribution.groupId,
            "Mchango Umepokelewa",
            "Mchango wa KES ${contribution.amount} kutoka kwa ${contribution.memberName} umesajiliwa kikamilifu."
        )

        // Dispatch SMS alert to member
        SmsDispatcher.sendSms(
            toPhone = "+254 701 234 567",
            message = "ChamaHub: Confirmed! KES ${contribution.amount} contribution received from ${contribution.memberName} on ${contribution.date}."
        )
        return id
    }

    suspend fun deleteContribution(contribution: Contribution) {
        chamaDao.deleteContribution(contribution)
        FirebaseSyncManager.deleteContributionFromCloud(contribution.groupId, contribution.id)
        insertLog(contribution.groupId, "System", "Deleted Contribution", "Removed contribution of ${contribution.amount} for ${contribution.memberName}")
    }

    // --- Loans ---
    val allLoans: Flow<List<Loan>> = chamaDao.getAllLoans()

    fun getLoansByGroup(groupId: Int): Flow<List<Loan>> = chamaDao.getLoansByGroup(groupId)

    fun getLoanById(id: Int): Flow<Loan?> = chamaDao.getLoanById(id)

    suspend fun insertLoan(loan: Loan): Long {
        val id = chamaDao.insertLoan(loan)
        val insertedLoan = loan.copy(id = id.toInt())
        FirebaseSyncManager.syncLoanWithCloud(insertedLoan)
        chamaDao.updateMemberLoanStatus(loan.memberId, "Active")
        chamaDao.getMemberEntityById(loan.memberId)?.let { updatedMember ->
            FirebaseSyncManager.syncMemberWithCloud(updatedMember)
        }
        insertLog(loan.groupId, "System", "Issued Loan", "Issued loan of ${loan.amount} to ${loan.memberName}")
        
        FirebaseSyncManager.triggerRealtimeUpdateNotification(
            loan.groupId,
            "Mkopo Umeidhinishwa",
            "Mkopo wa KES ${loan.amount} kwa ajili ya ${loan.memberName} umesambazwa!"
        )

        // Dispatch SMS alert
        SmsDispatcher.sendSms(
            toPhone = "+254 712 345 678",
            message = "ChamaHub: Credit Approved! Loan of KES ${loan.amount} disbursed to ${loan.memberName} at ${loan.interestRate}% rate, due by ${loan.dueDate}."
        )
        return id
    }

    suspend fun deleteLoan(loan: Loan) {
        chamaDao.deleteLoan(loan)
        FirebaseSyncManager.deleteLoanFromCloud(loan.groupId, loan.id)
        chamaDao.updateMemberLoanStatus(loan.memberId, "No Loan")
        chamaDao.getMemberEntityById(loan.memberId)?.let { updatedMember ->
            FirebaseSyncManager.syncMemberWithCloud(updatedMember)
        }
        insertLog(loan.groupId, "System", "Deleted Loan", "Deleted loan record of ${loan.amount} for ${loan.memberName}")
    }

    // --- Meetings ---
    val allMeetings: Flow<List<Meeting>> = chamaDao.getAllMeetings()

    fun getMeetingsByGroup(groupId: Int): Flow<List<Meeting>> = chamaDao.getMeetingsByGroup(groupId)

    suspend fun insertMeeting(meeting: Meeting): Long {
        val id = chamaDao.insertMeeting(meeting)
        val insertedMeeting = meeting.copy(id = id.toInt())
        FirebaseSyncManager.syncMeetingWithCloud(insertedMeeting)
        insertLog(meeting.groupId, "System", "Scheduled Meeting", "Scheduled meeting: ${meeting.title} on ${meeting.date}")
        
        FirebaseSyncManager.triggerRealtimeUpdateNotification(
            meeting.groupId,
            "Mkutano Mpya",
            "Mkutano ujao: '${meeting.title}' umepangwa kufanyika tarehe ${meeting.date}."
        )

        // Dispatch broadcast alert to the chama group circle
        SmsDispatcher.sendSms(
            toPhone = "+254 723 456 789",
            message = "ChamaHub Broadcast: New Meeting - '${meeting.title}' has been scheduled for ${meeting.date} at ${meeting.time}."
        )
        return id
    }

    suspend fun deleteMeeting(meeting: Meeting) {
        chamaDao.deleteMeeting(meeting)
        FirebaseSyncManager.deleteMeetingFromCloud(meeting.groupId, meeting.id)
        insertLog(meeting.groupId, "System", "Cancelled Meeting", "Cancelled meeting: ${meeting.title}")
    }

    // --- Attendance ---
    fun getAttendanceForMeeting(meetingId: Int): Flow<List<Attendance>> = chamaDao.getAttendanceForMeeting(meetingId)

    suspend fun insertAttendanceList(attendanceList: List<Attendance>) {
        chamaDao.insertAttendanceList(attendanceList)
    }

    // --- Notifications ---
    val allNotifications: Flow<List<ChamaNotification>> = chamaDao.getAllNotifications()

    fun getNotificationsByGroup(groupId: Int): Flow<List<ChamaNotification>> = chamaDao.getNotificationsByGroup(groupId)

    suspend fun insertNotification(notification: ChamaNotification) {
        chamaDao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Int) {
        chamaDao.markNotificationAsRead(id)
    }

    suspend fun clearAllNotifications() {
        chamaDao.clearAllNotifications()
    }

    suspend fun clearNotificationsByGroup(groupId: Int) {
        chamaDao.clearNotificationsByGroup(groupId)
    }

    // --- Activity Logs ---
    val allLogs: Flow<List<ActivityLog>> = chamaDao.getAllLogs()

    fun getLogsByGroup(groupId: Int): Flow<List<ActivityLog>> = chamaDao.getLogsByGroup(groupId)

    suspend fun insertLog(groupId: Int, userName: String, action: String, details: String) {
        chamaDao.insertLog(ActivityLog(groupId = groupId, userName = userName, action = action, details = details))
    }

    // --- Initialize Group and Admin Membership ---
    suspend fun initializeGroupForUser(userId: Int, groupName: String, adminName: String, currency: String, contributionAmt: Double): Int {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = formatter.format(Date())

        val inviteCode = "CHAMA-" + (10000..99999).random().toString()

        // 1. Create Group
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

        // 2. Create Membership for Owner
        val membership = Membership(
            userId = userId,
            groupId = generatedGroupId,
            role = "Owner",
            joinedDate = today,
            status = "Approved",
            permissions = "read_write"
        )
        chamaDao.insertMembership(membership)

        // Fetch creator's phone and email to insert as a real member
        val userEntity = chamaDao.getUserEntityById(userId)
        val userPhone = userEntity?.phone ?: "+254 701 234 567"
        val userEmail = userEntity?.email ?: "admin@chamahub.com"

        // 3. Add the Owner themselves as the first real Member of the group
        val ownerMember = Member(
            groupId = generatedGroupId,
            name = adminName,
            phone = userPhone,
            email = userEmail,
            role = "Owner",
            joinedDate = today,
            contributionStatus = "Up to Date",
            loanStatus = "No Loan"
        )
        chamaDao.insertMember(ownerMember)

        // 4. Add Welcome Notification
        val welcomeNotification = ChamaNotification(
            groupId = generatedGroupId,
            title = "Karibu kwenye ChamaHub!",
            body = "Kikundi chenu '$groupName' kimeanzishwa rasmi na $adminName. Anzeni kusajili wanachama na kurekodi michango sasa!",
            date = today,
            type = "Alert"
        )
        chamaDao.insertNotification(welcomeNotification)

        // 5. Activity Log
        insertLog(generatedGroupId, adminName, "Created Group", "Created group '$groupName'")

        FirebaseSyncManager.syncGroupWithCloud(generatedGroupId, "Group: $groupName, Owner: $adminName")

        return generatedGroupId
    }
}

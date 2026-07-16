package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChamaDao {

    // --- Users (Auth) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserEntityById(id: Int): User?

    // --- Groups ---
    @Query("SELECT * FROM groups LIMIT 1")
    fun getGroup(): Flow<ChamaGroup?>

    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<ChamaGroup>>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    fun getGroupById(id: Int): Flow<ChamaGroup?>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getGroupEntityById(id: Int): ChamaGroup?

    @Query("SELECT * FROM groups WHERE inviteCode = :code LIMIT 1")
    suspend fun getGroupByInviteCode(code: String): ChamaGroup?

    @Query("SELECT * FROM groups WHERE id IN (SELECT groupId FROM memberships WHERE userId = :userId AND status = 'Approved')")
    fun getGroupsForUser(userId: Int): Flow<List<ChamaGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ChamaGroup): Long

    @Query("DELETE FROM groups")
    suspend fun clearGroups()

    // --- Memberships ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: Membership): Long

    @Query("SELECT * FROM memberships WHERE userId = :userId")
    fun getMembershipsByUserId(userId: Int): Flow<List<Membership>>

    @Query("SELECT * FROM memberships WHERE groupId = :groupId")
    fun getMembershipsByGroupId(groupId: Int): Flow<List<Membership>>

    @Query("SELECT * FROM memberships WHERE userId = :userId AND groupId = :groupId LIMIT 1")
    suspend fun getMembership(userId: Int, groupId: Int): Membership?

    // --- Members ---
    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY name ASC")
    fun getMembersByGroup(groupId: Int): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    fun getMemberById(id: Int): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberEntityById(id: Int): Member?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("UPDATE members SET contributionStatus = :status WHERE id = :memberId")
    suspend fun updateMemberContributionStatus(memberId: Int, status: String)

    @Query("UPDATE members SET loanStatus = :status WHERE id = :memberId")
    suspend fun updateMemberLoanStatus(memberId: Int, status: String)

    // --- Contributions ---
    @Query("SELECT * FROM contributions ORDER BY date DESC")
    fun getAllContributions(): Flow<List<Contribution>>

    @Query("SELECT * FROM contributions WHERE groupId = :groupId ORDER BY date DESC")
    fun getContributionsByGroup(groupId: Int): Flow<List<Contribution>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: Contribution): Long

    @Delete
    suspend fun deleteContribution(contribution: Contribution)

    // --- Loans ---
    @Query("SELECT * FROM loans ORDER BY issueDate DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE groupId = :groupId ORDER BY issueDate DESC")
    fun getLoansByGroup(groupId: Int): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    fun getLoanById(id: Int): Flow<Loan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Delete
    suspend fun deleteLoan(loan: Loan)

    // --- Meetings ---
    @Query("SELECT * FROM meetings ORDER BY date DESC")
    fun getAllMeetings(): Flow<List<Meeting>>

    @Query("SELECT * FROM meetings WHERE groupId = :groupId ORDER BY date DESC")
    fun getMeetingsByGroup(groupId: Int): Flow<List<Meeting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: Meeting): Long

    @Delete
    suspend fun deleteMeeting(meeting: Meeting)

    // --- Attendance ---
    @Query("SELECT * FROM attendance WHERE meetingId = :meetingId")
    fun getAttendanceForMeeting(meetingId: Int): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(attendanceList: List<Attendance>)

    // --- Notifications ---
    @Query("SELECT * FROM notifications ORDER BY date DESC")
    fun getAllNotifications(): Flow<List<ChamaNotification>>

    @Query("SELECT * FROM notifications WHERE groupId = :groupId ORDER BY date DESC")
    fun getNotificationsByGroup(groupId: Int): Flow<List<ChamaNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: ChamaNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    @Query("DELETE FROM notifications WHERE groupId = :groupId")
    suspend fun clearNotificationsByGroup(groupId: Int)

    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getLogsByGroup(groupId: Int): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)
}

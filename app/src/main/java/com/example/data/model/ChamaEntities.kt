package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val profilePhoto: String? = null,
    val role: String = "Member", // Owner, Admin, Treasurer, Secretary, Member
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val status: String = "Active",
    val firebaseUid: String = ""
)

@Entity(tableName = "groups")
data class ChamaGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val meetingFrequency: String = "Monthly",
    val currency: String = "KES",
    val contributionAmount: Double = 2000.0,
    val adminName: String,
    val groupLogo: String? = null,
    val createdBy: String = "Admin",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "Active",
    val inviteCode: String = ""
)

@Entity(tableName = "memberships")
data class Membership(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val groupId: Int,
    val role: String = "Member", // "Owner", "Admin", "Treasurer", "Secretary", "Member"
    val joinedDate: String,
    val status: String = "Approved", // "Pending", "Approved", "Declined"
    val permissions: String = "read_write"
)

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 1,
    val name: String,
    val phone: String,
    val email: String,
    val profilePhoto: String? = null,
    val role: String = "Member", // "Owner", "Admin", "Treasurer", "Secretary", "Member"
    val joinedDate: String,
    val contributionStatus: String = "Up to Date", // "Up to Date", "Behind", "Late"
    val loanStatus: String = "No Loan", // "No Loan", "Active", "Overdue", "Paid"
    val isSynced: Boolean = true
)

@Entity(tableName = "contributions")
data class Contribution(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 1,
    val memberId: Int,
    val memberName: String,
    val amount: Double,
    val date: String,
    val status: String, // "Paid", "Late", "Missed"
    val notes: String? = null,
    val createdBy: String = "System",
    val isSynced: Boolean = true
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 1,
    val memberId: Int,
    val memberName: String,
    val amount: Double,
    val remainingBalance: Double,
    val interestRate: Double,
    val status: String, // "Pending", "Approved", "Active", "Overdue", "Paid"
    val durationMonths: Int,
    val issueDate: String,
    val dueDate: String,
    val riskScore: Int = 10, // 0 to 100
    val riskReason: String = "Safe",
    val isSynced: Boolean = true
)

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 1,
    val title: String,
    val date: String,
    val time: String,
    val agenda: String,
    val notes: String? = null,
    val attendancePhoto: String? = null,
    val isSynced: Boolean = true
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val meetingId: Int,
    val memberId: Int,
    val status: String, // "Present", "Absent", "Late"
    val notes: String? = null
)

@Entity(tableName = "notifications")
data class ChamaNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 1,
    val title: String,
    val body: String,
    val date: String,
    val type: String, // "Contribution", "Loan", "Meeting", "Alert"
    val isRead: Boolean = false
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 1,
    val userName: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

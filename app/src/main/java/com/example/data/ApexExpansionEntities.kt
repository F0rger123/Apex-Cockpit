package com.example.data

import androidx.room.*

@Entity(tableName = "saved_posts")
data class SavedPost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val saverUsername: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "follow_relationships")
data class FollowRelationship(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val followerUsername: String,
    val followedUsername: String,
    val isMutual: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users")
data class BlockedUser(
    @PrimaryKey val usernameToBlock: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "dm_messages")
data class DmMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String, // format "user1_user2" sorted alphabetically
    val sender: String,
    val recipient: String,
    val content: String,
    val imageUrl: String? = null,
    val reactions: String = "", // comma separated "heart,thumbs_up"
    val deliveryState: String = "SENT", // SENT, DELIVERED, READ
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_chats")
data class GroupChat(
    @PrimaryKey val id: String,
    val name: String,
    val creator: String,
    val roleMap: String = "", // format "user1:owner,user2:admin"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_messages")
data class GroupMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: String,
    val sender: String,
    val content: String,
    val imageUrl: String? = null,
    val reactions: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "car_clubs")
data class CarClub(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val creator: String,
    val logoUrl: String? = null,
    val announcements: String = "", // pins or announcements format
    val memberCount: Int = 1
)

@Entity(tableName = "club_members")
data class ClubMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clubId: String,
    val username: String,
    val role: String = "MEMBER", // OWNER, ADMIN, OFFICER, MEMBER
    val isApproved: Boolean = true
)

@Entity(tableName = "maintenance_logs")
data class MaintenanceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val serviceType: String, // Oil Change, Tires, Brakes, Services
    val mileage: Int,
    val cost: Double,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "moderation_reports")
data class ReportItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportedType: String, // "post" or "user"
    val reportedId: String,
    val reportedBy: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "speed_zones")
data class SpeedZone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val limitMph: Int,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)

@Entity(tableName = "notification_items")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipient: String,
    val title: String,
    val message: String,
    val type: String, // DM, CHANCE, VISIT, SYSTEM
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "credit_wallets")
data class CreditWallet(
    @PrimaryKey val username: String,
    val balance: Double = 1000.0,
    val lifetimeEarned: Double = 1000.0,
    val lifetimeSpent: Double = 0.0,
    val lifetimeWon: Double = 0.0,
    val lifetimeLost: Double = 0.0
)

@Entity(tableName = "credit_transactions")
data class CreditTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val amount: Double,
    val type: String, // "Race win", "Race loss", "Challenge reward", "Club reward", "Admin reward", "Purchase", "Transfer"
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "race_wagers")
data class RaceWager(
    @PrimaryKey val wagerId: String,
    val challengerId: String,
    val opponentId: String,
    val creditAmount: Double,
    val status: String, // "Pending", "Accepted", "LockedInEscrow", "Completed", "Cancelled"
    val winnerId: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vehicle_modifications")
data class VehicleModification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val category: String, // Engine, Turbo, Supercharger, Fuel, Exhaust, Suspension, Brakes, Transmission, Tires, Weight Reduction, Aerodynamics
    val partName: String,
    val brand: String,
    val cost: Double,
    val estimatedHpGain: Int,
    val estimatedTorqueGain: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "race_proofs")
data class RaceProof(
    @PrimaryKey val raceId: String,
    val videoUrl: String?,
    val imageUrl: String?,
    val telemetrySnapshot: String,
    val auditTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "driver_presence")
data class DriverPresence(
    @PrimaryKey val username: String,
    val status: String, // "Online", "Offline"
    val latitude: Double,
    val longitude: Double,
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "driver_ratings")
data class DriverRating(
    @PrimaryKey val username: String,
    val eloRating: Int = 1200,
    val wins: Int = 0,
    val losses: Int = 0,
    val winStreak: Int = 0,
    val rankTier: String = "Bronze" // "Bronze", "Silver", "Gold", "Platinum", "Diamond", "Legend"
)

@Entity(tableName = "credit_escrows")
data class CreditEscrow(
    @PrimaryKey val escrowId: String,
    val userId: String,
    val amount: Double,
    val wagerId: String,
    val status: String, // "LOCKED", "RELEASED", "REFUNDED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "race_runs")
data class RaceRun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val distanceMiles: Double,
    val durationSeconds: Double,
    val avgSpeedMph: Double,
    val maxSpeedMph: Double,
    val telemetryTrace: String, // Comma-separated speed/G-force readings
    val validationStatus: String, // "VALID", "INVALID_GPS_DRIFT", "FAILED_SPEED_THRESHOLD", "ANTI_CHEAT_TRIGGERED"
    val timestamp: Long = System.currentTimeMillis()
)


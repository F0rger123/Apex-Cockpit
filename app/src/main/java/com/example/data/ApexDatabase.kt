package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. DATABASE ENTITIES
// ==========================================

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "me",
    val username: String,
    val bio: String,
    val location: String,
    val followers: Int,
    val following: Int,
    val wins: Int,
    val losses: Int,
    val badges: String = "First Race,Local Legend,Top Builder"
)

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val year: Int,
    val make: String,
    val model: String,
    val trim: String,
    val engine: String,
    val horsepower: Int,
    val torque: Int,
    val weight: Int,
    val transmission: String,
    val drivetrain: String,
    val modifications: String = "", // Comma-separated list
    val estimatedValue: Double = 0.0
)

@Entity(tableName = "performance_runs")
data class PerformanceRun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val zeroToSixty: Double, // Seconds
    val zeroToOneHundred: Double, // Seconds
    val sixtyToOneThirty: Double, // Seconds
    val quarterMile: Double, // Seconds
    val topSpeed: Double, // MPH
    val maxGForce: Double,
    val distanceTraveled: Double // Miles
)

@Entity(tableName = "car_meets")
data class CarMeet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val locationName: String,
    val rules: String,
    val capacity: Int,
    val isPublic: Boolean = true,
    val rsvpCount: Int = 1,
    val hasRsvped: Boolean = false,
    val hostName: String,
    val startTime: String,
    val endTime: String,
    val latitude: Double = 34.0522,
    val longitude: Double = -118.2437
)

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val challengerName: String,
    val opponentName: String,
    val opponentVehicle: String,
    val raceType: String, // Roll race, Dig race, Quarter mile, etc.
    val scheduledTime: String,
    val status: String = "Pending" // Pending, Accepted, Declined, Completed
)

@Entity(tableName = "social_posts")
data class SocialPost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val authorTag: String,
    val textContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val hasVideoProof: Boolean = false,
    val videoProofStats: String = "" // e.g. "0-60: 3.42s"
)

// ==========================================
// 2. DATA ACCESS OBJECT (DAO)
// ==========================================

@Dao
interface ApexDao {
    @Query("SELECT * FROM user_profiles WHERE id = 'me' LIMIT 1")
    fun getMyProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Query("SELECT * FROM vehicles ORDER BY id DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM performance_runs ORDER BY timestamp DESC")
    fun getAllPerformanceRuns(): Flow<List<PerformanceRun>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformanceRun(run: PerformanceRun)

    @Query("SELECT * FROM car_meets ORDER BY id DESC")
    fun getAllMeets(): Flow<List<CarMeet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeet(meet: CarMeet)

    @Update
    suspend fun updateMeet(meet: CarMeet)

    @Query("SELECT * FROM challenges ORDER BY id DESC")
    fun getAllChallenges(): Flow<List<Challenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: Challenge)

    @Update
    suspend fun updateChallenge(challenge: Challenge)

    @Query("SELECT * FROM social_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<SocialPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: SocialPost)

    @Update
    suspend fun updatePost(post: SocialPost)

    // EXPANSION QUERIES
    @Query("SELECT * FROM saved_posts WHERE saverUsername = :username ORDER BY timestamp DESC")
    fun getSavedPosts(username: String): Flow<List<SavedPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPost(post: SavedPost)

    @Query("DELETE FROM saved_posts WHERE postId = :postId AND saverUsername = :username")
    suspend fun deleteSavedPost(postId: Long, username: String)

    @Query("SELECT * FROM follow_relationships WHERE followerUsername = :username")
    fun getFollowing(username: String): Flow<List<FollowRelationship>>

    @Query("SELECT * FROM follow_relationships WHERE followedUsername = :username")
    fun getFollowers(username: String): Flow<List<FollowRelationship>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(relationship: FollowRelationship)

    @Query("DELETE FROM follow_relationships WHERE followerUsername = :follower AND followedUsername = :followed")
    suspend fun deleteFollow(follower: String, followed: String)

    @Query("SELECT * FROM blocked_users")
    fun getBlockedUsers(): Flow<List<BlockedUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(user: BlockedUser)

    @Delete
    suspend fun deleteBlockedUser(user: BlockedUser)

    @Query("SELECT * FROM dm_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getDmMessages(convId: String): Flow<List<DmMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDmMessage(msg: DmMessage)

    @Update
    suspend fun updateDmMessage(msg: DmMessage)

    @Query("SELECT * FROM group_chats ORDER BY timestamp DESC")
    fun getAllGroupChats(): Flow<List<GroupChat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupChat(chat: GroupChat)

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getGroupMessages(groupId: String): Flow<List<GroupMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMessage(msg: GroupMessage)

    @Query("SELECT * FROM car_clubs ORDER BY id DESC")
    fun getAllClubs(): Flow<List<CarClub>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClub(club: CarClub)

    @Update
    suspend fun updateClub(club: CarClub)

    @Query("SELECT * FROM club_members WHERE clubId = :clubId")
    fun getClubMembers(clubId: String): Flow<List<ClubMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubMember(member: ClubMember)

    @Update
    suspend fun updateClubMember(member: ClubMember)

    @Query("SELECT * FROM maintenance_logs WHERE vehicleId = :vId ORDER BY timestamp DESC")
    fun getMaintenanceLogs(vId: Long): Flow<List<MaintenanceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceLog(log: MaintenanceLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportItem)

    @Query("SELECT * FROM speed_zones")
    fun getAllSpeedZones(): Flow<List<SpeedZone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedZone(zone: SpeedZone)

    @Query("SELECT * FROM notification_items ORDER BY timestamp DESC")
    fun getNotifications(): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notif: NotificationItem)

    @Query("UPDATE notification_items SET isRead = 1 WHERE isRead = 0")
    suspend fun markNotificationsRead()

    // --- CREDITS ECONOMY DAOs ---
    @Query("SELECT * FROM credit_wallets WHERE username = :username LIMIT 1")
    fun getCreditWallet(username: String): Flow<CreditWallet?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: CreditWallet)

    @Query("SELECT * FROM credit_transactions WHERE username = :username ORDER BY timestamp DESC")
    fun getCreditTransactions(username: String): Flow<List<CreditTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditTransaction(tx: CreditTransaction)

    // --- RACE BETTING / WAGERS DAOs ---
    @Query("SELECT * FROM race_wagers WHERE wagerId = :wagerId LIMIT 1")
    fun getRaceWager(wagerId: String): Flow<RaceWager?>

    @Query("SELECT * FROM race_wagers ORDER BY createdAt DESC")
    fun getAllWagers(): Flow<List<RaceWager>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceWager(wager: RaceWager)

    @Update
    suspend fun updateRaceWager(wager: RaceWager)

    // --- VEHICLE BUILD SYSTEM / MODIFICATIONS DAOs ---
    @Query("SELECT * FROM vehicle_modifications WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getVehicleModifications(vehicleId: Long): Flow<List<VehicleModification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleModification(mod: VehicleModification)

    @Delete
    suspend fun deleteVehicleModification(mod: VehicleModification)

    // --- RACE PROOF SYSTEM DAOs ---
    @Query("SELECT * FROM race_proofs WHERE raceId = :raceId LIMIT 1")
    fun getRaceProof(raceId: String): Flow<RaceProof?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceProof(proof: RaceProof)

    // --- LIVE DRIVER NETWORK PRESENCE DAOs ---
    @Query("SELECT * FROM driver_presence WHERE status = 'Online'")
    fun getOnlineDrivers(): Flow<List<DriverPresence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriverPresence(presence: DriverPresence)

    @Query("SELECT * FROM driver_presence")
    fun getAllDriverPresences(): Flow<List<DriverPresence>>

    // --- RANKED RACING RATING (ELO) DAOs ---
    @Query("SELECT * FROM driver_ratings WHERE username = :username LIMIT 1")
    fun getDriverRating(username: String): Flow<DriverRating?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDriverRating(rating: DriverRating)

    @Query("SELECT * FROM driver_ratings ORDER BY eloRating DESC")
    fun getLeaderboardRatings(): Flow<List<DriverRating>>

    // --- ESCROW DAOs ---
    @Query("SELECT * FROM credit_escrows WHERE userId = :userId")
    fun getCreditEscrows(userId: String): Flow<List<CreditEscrow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditEscrow(escrow: CreditEscrow)

    @Update
    suspend fun updateCreditEscrow(escrow: CreditEscrow)

    // --- RACE RUN DAOs ---
    @Query("SELECT * FROM race_runs ORDER BY timestamp DESC")
    fun getAllRaceRuns(): Flow<List<RaceRun>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceRun(run: RaceRun)
}

// ==========================================
// 3. ROOM DATABASE CLASS
// ==========================================

@Database(
    entities = [
        UserProfile::class,
        Vehicle::class,
        PerformanceRun::class,
        CarMeet::class,
        Challenge::class,
        SocialPost::class,
        SavedPost::class,
        FollowRelationship::class,
        BlockedUser::class,
        DmMessage::class,
        GroupChat::class,
        GroupMessage::class,
        CarClub::class,
        ClubMember::class,
        MaintenanceLog::class,
        ReportItem::class,
        SpeedZone::class,
        NotificationItem::class,
        CreditWallet::class,
        CreditTransaction::class,
        RaceWager::class,
        VehicleModification::class,
        RaceProof::class,
        DriverPresence::class,
        DriverRating::class,
        CreditEscrow::class,
        RaceRun::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ApexDatabase : RoomDatabase() {
    abstract fun apexDao(): ApexDao

    companion object {
        @Volatile
        private var INSTANCE: ApexDatabase? = null

        fun getDatabase(context: Context): ApexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApexDatabase::class.java,
                    "apex_automotive_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. DATA REPOSITORY
// ==========================================

class ApexRepository(private val dao: ApexDao) {
    val myProfile: Flow<UserProfile?> = dao.getMyProfile()
    val allVehicles: Flow<List<Vehicle>> = dao.getAllVehicles()
    val allPerformanceRuns: Flow<List<PerformanceRun>> = dao.getAllPerformanceRuns()
    val allMeets: Flow<List<CarMeet>> = dao.getAllMeets()
    val allChallenges: Flow<List<Challenge>> = dao.getAllChallenges()
    val allPosts: Flow<List<SocialPost>> = dao.getAllPosts()
    
    // Expanded flows
    val allBlockedUsers: Flow<List<BlockedUser>> = dao.getBlockedUsers()
    val allGroupChats: Flow<List<GroupChat>> = dao.getAllGroupChats()
    val allClubs: Flow<List<CarClub>> = dao.getAllClubs()
    val allSpeedZones: Flow<List<SpeedZone>> = dao.getAllSpeedZones()
    val allNotifications: Flow<List<NotificationItem>> = dao.getNotifications()

    suspend fun saveProfile(profile: UserProfile) {
        dao.insertOrUpdateProfile(profile)
        try {
            com.example.service.ApexFirebaseService.syncUserProfileToCloud(profile)
        } catch (e: Exception) {
            // Ignore offline fallback exception
        }
    }

    suspend fun addVehicle(vehicle: Vehicle) {
        dao.insertVehicle(vehicle)
    }

    suspend fun removeVehicle(vehicle: Vehicle) {
        dao.deleteVehicle(vehicle)
    }

    suspend fun addPerformanceRun(run: PerformanceRun) {
        dao.insertPerformanceRun(run)
        try {
            com.example.service.ApexFirebaseService.syncTelemetryRunToCloud(run)
        } catch (e: Exception) {
            // Ignore offline fallback exception
        }
    }

    suspend fun addMeet(meet: CarMeet) {
        dao.insertMeet(meet)
    }

    suspend fun updateMeet(meet: CarMeet) {
        dao.updateMeet(meet)
    }

    suspend fun addChallenge(challenge: Challenge) {
        dao.insertChallenge(challenge)
    }

    suspend fun updateChallenge(challenge: Challenge) {
        dao.updateChallenge(challenge)
    }

    suspend fun addPost(post: SocialPost) {
        dao.insertPost(post)
    }

    suspend fun updatePost(post: SocialPost) {
        dao.updatePost(post)
    }

    // Expanded actions
    fun getSavedPosts(username: String): Flow<List<SavedPost>> = dao.getSavedPosts(username)
    suspend fun insertSavedPost(post: SavedPost) = dao.insertSavedPost(post)
    suspend fun deleteSavedPost(postId: Long, username: String) = dao.deleteSavedPost(postId, username)

    fun getFollowing(username: String): Flow<List<FollowRelationship>> = dao.getFollowing(username)
    fun getFollowers(username: String): Flow<List<FollowRelationship>> = dao.getFollowers(username)
    suspend fun insertFollow(relationship: FollowRelationship) = dao.insertFollow(relationship)
    suspend fun deleteFollow(follower: String, followed: String) = dao.deleteFollow(follower, followed)

    suspend fun insertBlockedUser(user: BlockedUser) = dao.insertBlockedUser(user)
    suspend fun deleteBlockedUser(user: BlockedUser) = dao.deleteBlockedUser(user)

    fun getDmMessages(convId: String): Flow<List<DmMessage>> = dao.getDmMessages(convId)
    suspend fun insertDmMessage(msg: DmMessage) = dao.insertDmMessage(msg)
    suspend fun updateDmMessage(msg: DmMessage) = dao.updateDmMessage(msg)

    suspend fun insertGroupChat(chat: GroupChat) = dao.insertGroupChat(chat)
    fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> = dao.getGroupMessages(groupId)
    suspend fun insertGroupMessage(msg: GroupMessage) = dao.insertGroupMessage(msg)

    suspend fun insertClub(club: CarClub) = dao.insertClub(club)
    suspend fun updateClub(club: CarClub) = dao.updateClub(club)
    fun getClubMembers(clubId: String): Flow<List<ClubMember>> = dao.getClubMembers(clubId)
    suspend fun insertClubMember(member: ClubMember) = dao.insertClubMember(member)
    suspend fun updateClubMember(member: ClubMember) = dao.updateClubMember(member)

    fun getMaintenanceLogs(vehicleId: Long): Flow<List<MaintenanceLog>> = dao.getMaintenanceLogs(vehicleId)
    suspend fun insertMaintenanceLog(log: MaintenanceLog) = dao.insertMaintenanceLog(log)

    suspend fun insertReport(report: ReportItem) = dao.insertReport(report)

    suspend fun insertSpeedZone(zone: SpeedZone) = dao.insertSpeedZone(zone)
    suspend fun insertNotification(notif: NotificationItem) = dao.insertNotification(notif)
    suspend fun markNotificationsRead() = dao.markNotificationsRead()

    // --- NEW SYSTEM REPOSITORY HANDLERS ---
    fun getCreditWallet(username: String): Flow<CreditWallet?> = dao.getCreditWallet(username)
    suspend fun saveCreditWallet(wallet: CreditWallet) = dao.insertOrUpdateWallet(wallet)
    fun getCreditTransactions(username: String): Flow<List<CreditTransaction>> = dao.getCreditTransactions(username)
    suspend fun addCreditTransaction(tx: CreditTransaction) = dao.insertCreditTransaction(tx)

    fun getRaceWager(wagerId: String): Flow<RaceWager?> = dao.getRaceWager(wagerId)
    val allWagers: Flow<List<RaceWager>> = dao.getAllWagers()
    suspend fun addRaceWager(wager: RaceWager) = dao.insertRaceWager(wager)
    suspend fun updateRaceWager(wager: RaceWager) = dao.updateRaceWager(wager)

    fun getVehicleModifications(vehicleId: Long): Flow<List<VehicleModification>> = dao.getVehicleModifications(vehicleId)
    suspend fun insertVehicleModification(mod: VehicleModification) = dao.insertVehicleModification(mod)
    suspend fun deleteVehicleModification(mod: VehicleModification) = dao.deleteVehicleModification(mod)

    fun getRaceProof(raceId: String): Flow<RaceProof?> = dao.getRaceProof(raceId)
    suspend fun insertRaceProof(proof: RaceProof) = dao.insertRaceProof(proof)

    val onlineDrivers: Flow<List<DriverPresence>> = dao.getOnlineDrivers()
    val allDriverPresences: Flow<List<DriverPresence>> = dao.getAllDriverPresences()
    suspend fun saveDriverPresence(presence: DriverPresence) = dao.insertDriverPresence(presence)

    fun getDriverRating(username: String): Flow<DriverRating?> = dao.getDriverRating(username)
    suspend fun saveDriverRating(rating: DriverRating) = dao.insertOrUpdateDriverRating(rating)
    val leaderboardRatings: Flow<List<DriverRating>> = dao.getLeaderboardRatings()

    // --- ESCROW SYSTEM REPOSITORY HANDLERS ---
    fun getCreditEscrows(userId: String): Flow<List<CreditEscrow>> = dao.getCreditEscrows(userId)
    suspend fun insertCreditEscrow(escrow: CreditEscrow) = dao.insertCreditEscrow(escrow)
    suspend fun updateCreditEscrow(escrow: CreditEscrow) = dao.updateCreditEscrow(escrow)

    // --- RACE RUN SYSTEM REPOSITORY HANDLERS ---
    val allRaceRuns: Flow<List<RaceRun>> = dao.getAllRaceRuns()
    suspend fun insertRaceRun(run: RaceRun) {
        dao.insertRaceRun(run)
        try {
            com.example.service.ApexFirebaseService.syncRaceRunToCloud(run)
        } catch (e: Exception) {
            // Ignore offline fallback exception
        }
    }
}

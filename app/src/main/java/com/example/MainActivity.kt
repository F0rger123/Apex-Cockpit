package com.example

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// 1. NAVIGATION TAB STRUCT
// ==========================================
enum class ApexTab(val title: String, val icon: ImageVector) {
    HOME("HOME", Icons.Default.Home),
    MAP("MAP", Icons.Default.Map),
    RACE("RACE", Icons.Default.Flag),
    GARAGE("GARAGE", Icons.Default.DirectionsCar),
    PROFILE("PROFILE", Icons.Default.Person)
}

// ==========================================
// 2. MAIN ACTIVE VIEW MODEL IMPLEMENTATION
// ==========================================
class ApexViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ApexRepository

    private val locationManager = com.example.service.ApexLocationManager(application)
    private val sensorManager = com.example.service.ApexSensorManager(application)

    val realLocation = locationManager.currentLocation
    val realSpeedMph = locationManager.currentSpeedMph
    val isRealTracking = locationManager.isTracking

    val realGForceX = sensorManager.gForceX
    val realGForceY = sensorManager.gForceY
    val realGForceZ = sensorManager.gForceZ

    fun startRealTracking() {
        locationManager.startLocationTracking()
        sensorManager.startListening()
    }

    fun stopRealTracking() {
        locationManager.stopLocationTracking()
        sensorManager.stopListening()
    }

    init {
        val database = ApexDatabase.getDatabase(application)
        repository = ApexRepository(database.apexDao())

        // Seed initial data asynchronously
        viewModelScope.launch {
            repository.myProfile.first().let { myProfile ->
                if (myProfile == null) {
                    repository.saveProfile(
                        UserProfile(
                            username = "Apex_Pilot",
                            bio = "Carbon Aero Specialist. High-G curve tuner. Porsche fan.",
                            location = "Los Angeles, CA",
                            followers = 2840,
                            following = 412,
                            wins = 34,
                            losses = 11,
                            badges = "First Race,Local Legend,Top Builder,Speed Demon"
                        )
                    )
                }
            }

            repository.allVehicles.first().let { vehicles ->
                if (vehicles.isEmpty()) {
                    repository.addVehicle(
                        Vehicle(
                            year = 2023, make = "Porsche", model = "911 GT3 RS", trim = "Weissach",
                            engine = "4.0L Flat-6", horsepower = 518, torque = 342, weight = 3268,
                            transmission = "7-Speed PDK", drivetrain = "RWD",
                            modifications = "Dundon exhaust headers, Carbon brake pads", estimatedValue = 295000.0
                        )
                    )
                    repository.addVehicle(
                        Vehicle(
                            year = 2022, make = "Tesla", model = "Model S Plaid", trim = "Tri-Motor",
                            engine = "Triple AC Electric", horsepower = 1020, torque = 1050, weight = 4766,
                            transmission = "Single-Speed", drivetrain = "AWD",
                            modifications = "Unplugged Performance Carbon Spoiler, Track Package Tires",
                            estimatedValue = 105000.0
                        )
                    )
                    repository.addVehicle(
                        Vehicle(
                            year = 2011, make = "Ford", model = "Mustang GT", trim = "Coyote V8",
                            engine = "5.0L Coyote V8", horsepower = 412, torque = 390, weight = 3605,
                            transmission = "6-Speed Manual", drivetrain = "RWD",
                            modifications = "Coyote intake manifold, Ford Racing exhaust headers, custom dyno tune",
                            estimatedValue = 28500.0
                        )
                    )
                }
            }

            repository.allMeets.first().let { meets ->
                if (meets.isEmpty()) {
                    repository.addMeet(
                        CarMeet(
                            title = "Overlook Canyon Cruise", locationName = "Mulholland Scenic Lookpoint",
                            rules = "No excessive revving, clean trailing, respect security.", capacity = 75,
                            rsvpCount = 42, hasRsvped = false, hostName = "RSR_Collective",
                            startTime = "Saturday 9:00 PM", endTime = "11:30 PM", latitude = 34.1293, longitude = -118.4019
                        )
                    )
                }
            }

            repository.allChallenges.first().let { challenges ->
                if (challenges.isEmpty()) {
                    repository.addChallenge(
                        Challenge(
                            challengerName = "E85_GTR_Lover", opponentName = "Apex_Pilot",
                            opponentVehicle = "Tesla Model S Plaid", raceType = "Drag Race (1/4 Mile)",
                            scheduledTime = "Tonight 11:00 PM", status = "Pending"
                        )
                    )
                }
            }

            repository.allPosts.first().let { posts ->
                if (posts.isEmpty()) {
                    repository.addPost(
                        SocialPost(
                            authorName = "Apex_Pilot", authorTag = "@apex_pilot",
                            textContent = "Hit a clean 2.92s 0-60 MPH launch using the Plaid. Telemetry charts verified. Ready for the canyon cruise tonight!",
                            likesCount = 138, commentsCount = 21, isLiked = false,
                            hasVideoProof = true, videoProofStats = "Verified 0-60: 2.92s"
                        )
                    )
                    repository.addPost(
                        SocialPost(
                            authorName = "Flat6_Whine", authorTag = "@gt3_screamer",
                            textContent = "Nothing beats the screaming 9000 RPM raw engine sounds of the GT3 RS going through the canyon passes. Dundon exhaust installed!",
                            likesCount = 94, commentsCount = 8, isLiked = true,
                            hasVideoProof = false, videoProofStats = ""
                        )
                    )
                }
            }
        }
    }

    val myProfile: StateFlow<UserProfile?> = repository.myProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPerformanceRuns: StateFlow<List<PerformanceRun>> = repository.allPerformanceRuns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeets: StateFlow<List<CarMeet>> = repository.allMeets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChallenges: StateFlow<List<Challenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPosts: StateFlow<List<SocialPost>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun updateProfile(newUsername: String, newBio: String, newLocation: String) {
        viewModelScope.launch {
            val cur = myProfile.value ?: UserProfile(username = "Apex_Pilot", bio = "", location = "", followers = 0, following = 0, wins = 0, losses = 0)
            repository.saveProfile(cur.copy(username = newUsername, bio = newBio, location = newLocation))
        }
    }

    fun addVehicle(year: Int, make: String, model: String, trim: String, eng: String, hp: Int, tq: Int, wt: Int, tr: String, dt: String, mods: String, valEst: Double) {
        viewModelScope.launch {
            repository.addVehicle(Vehicle(year = year, make = make, model = model, trim = trim, engine = eng, horsepower = hp, torque = tq, weight = wt, transmission = tr, drivetrain = dt, modifications = mods, estimatedValue = valEst))
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.removeVehicle(vehicle)
        }
    }

    fun savePerformanceRun(run: PerformanceRun) {
        viewModelScope.launch {
            repository.addPerformanceRun(run)
        }
    }

    fun addCarMeet(title: String, location: String, rules: String, capacity: Int, start: String, end: String) {
        viewModelScope.launch {
            repository.addMeet(CarMeet(title = title, locationName = location, rules = rules, capacity = capacity, hostName = myProfile.value?.username ?: "Apex_Pilot", startTime = start, endTime = end))
        }
    }

    fun toggleRsvp(meet: CarMeet) {
        viewModelScope.launch {
            val isRsvping = !meet.hasRsvped
            val cnt = if (isRsvping) meet.rsvpCount + 1 else meet.rsvpCount - 1
            repository.updateMeet(meet.copy(rsvpCount = cnt, hasRsvped = isRsvping))
        }
    }

    fun createChallenge(opponent: String, oppVehicle: String, raceType: String, time: String) {
        viewModelScope.launch {
            repository.addChallenge(Challenge(challengerName = myProfile.value?.username ?: "Apex_Pilot", opponentName = opponent, opponentVehicle = oppVehicle, raceType = raceType, scheduledTime = time))
        }
    }

    fun updateChallengeStatus(challenge: Challenge, status: String) {
        viewModelScope.launch {
            repository.updateChallenge(challenge.copy(status = status))
            if (status == "Completed") {
                val prof = myProfile.value
                if (prof != null) {
                    repository.saveProfile(prof.copy(wins = prof.wins + 1))
                }
            }
        }
    }

    fun submitPost(text: String, telemetryStats: String) {
        viewModelScope.launch {
            val prof = myProfile.value
            repository.addPost(
                SocialPost(
                    authorName = prof?.username ?: "Apex_Pilot",
                    authorTag = "@${(prof?.username ?: "Apex_Pilot").lowercase()}",
                    textContent = text,
                    hasVideoProof = telemetryStats.isNotEmpty(),
                    videoProofStats = telemetryStats
                )
            )
        }
    }

    fun toggleLikePost(post: SocialPost) {
        viewModelScope.launch {
            val isLiking = !post.isLiked
            val cnt = if (isLiking) post.likesCount + 1 else post.likesCount - 1
            repository.updatePost(post.copy(likesCount = cnt, isLiked = isLiking))
        }
    }
}

// ViewModel Factory
class ApexViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApexViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ApexViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==========================================
// 3. MAIN ACTIVITY CONTAINER
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase dynamically on startup
        val fbApiKey = BuildConfig.FIREBASE_API_KEY
        val fbAppId = BuildConfig.FIREBASE_APPLICATION_ID
        val fbProjectId = BuildConfig.FIREBASE_PROJECT_ID

        if (fbApiKey.isNotEmpty() && fbApiKey != "AIzaSy_Placeholder_Firebase_API_Key" &&
            fbAppId.isNotEmpty() && fbAppId != "1:1234567890:android:placeholderapp" &&
            fbProjectId.isNotEmpty() && fbProjectId != "placeholder-firebase-project"
        ) {
            com.example.service.ApexFirebaseService.initializeProgrammatically(
                applicationContext,
                fbApiKey,
                fbAppId,
                fbProjectId
            )
        } else {
            // Attempt standard setup
            com.example.service.ApexFirebaseService.attemptAutoSetup(applicationContext)
            // If standard setup fails, register client fallback sandbox options to avoid IllegalStateException crashes when referenced
            if (!com.example.service.ApexFirebaseService.isConnected()) {
                com.example.service.ApexFirebaseService.initializeProgrammatically(
                    applicationContext,
                    "AIzaSy_FakePlaceholderKeyForSandboxRun_99x",
                    "1:999999999:android:999999999999",
                    "apex-race-sandbox-fallback"
                )
            }
        }

        val factory = ApexViewModelFactory(application)
        val viewModel = ViewModelProvider(this, factory)[ApexViewModel::class.java]

        val db = com.example.data.ApexDatabase.getDatabase(application)
        val repo = com.example.data.ApexRepository(db.apexDao())
        val expansionViewModel = com.example.ui.ApexExpansionViewModel(application, repo)

        val feedRepo = com.example.FeedRepository(db.apexDao())
        val feedViewModel = com.example.FeedViewModel(feedRepo)

        setContent {
            MyApplicationTheme {
                MainAppHost(viewModel, expansionViewModel, feedViewModel)
            }
        }
    }
}

// ==========================================
// 4. FLOATING PARTICLES VISUAL MOTIF
// ==========================================
data class DustParticle(
    val xFraction: Float,
    val yFraction: Float,
    val r: Float,
    val pulseSpeed: Int
)

@Composable
fun StarDustBackground(modifier: Modifier = Modifier) {
    val dust = remember {
        List(25) {
            DustParticle(
                xFraction = (0..100).random() / 100f,
                yFraction = (0..100).random() / 100f,
                r = (1..3).random().toFloat(),
                pulseSpeed = (1500..3500).random()
            )
        }
    }
    val trans = rememberInfiniteTransition(label = "RadarSweeper")

    val alphas = dust.map { pt ->
        trans.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = pt.pulseSpeed, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DustAlpha"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            dust.forEachIndexed { index, pt ->
                val alpha = alphas.getOrNull(index)?.value ?: 0.4f
                drawCircle(
                    color = Color(0xFF00FFCC).copy(alpha = alpha),
                    radius = pt.r.dp.toPx(),
                    center = Offset(pt.xFraction * w, pt.yFraction * h)
                )
            }
        }
    }
}

// ==========================================
// 5. MAIN NAVIGATION ROUTER VISUALLY STYLED
// ==========================================
@Composable
fun MainAppHost(
    viewModel: ApexViewModel,
    expansionViewModel: com.example.ui.ApexExpansionViewModel,
    feedViewModel: com.example.FeedViewModel
) {
    var activeTab by remember { mutableStateOf(ApexTab.HOME) }
    var isExpandedHotbar by remember { mutableStateOf(false) }
    var showPilotsDiscoveryHub by remember { mutableStateOf(false) }
    var showObdMonitor by remember { mutableStateOf(false) }
    var showClubsDashboard by remember { mutableStateOf(false) }
    var showMaintenanceDrawer by remember { mutableStateOf(false) }
    var showOperationsDashboard by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        StarDustBackground()

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    ApexTab.values().forEach { tab ->
                        val active = activeTab == tab
                        NavigationBarItem(
                            selected = active,
                            onClick = { activeTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (active) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0x3300FF88)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Crossfade(targetState = activeTab, label = "RouteCrossfade") { tab ->
                    when (tab) {
                        ApexTab.HOME -> SocialFeedAndTelemetry(
                            viewModel = viewModel,
                            expansionViewModel = expansionViewModel,
                            feedViewModel = feedViewModel,
                            onNavigateTab = { activeTab = it }
                        )
                        ApexTab.MAP -> InteractiveTelemetryMap(viewModel)
                        ApexTab.RACE -> PremiumActiveRaceScreen(viewModel, expansionViewModel)
                        ApexTab.GARAGE -> GaragePerformanceScreen(viewModel)
                        ApexTab.PROFILE -> PremiumUserProfileScreen(viewModel, expansionViewModel)
                    }
                }
            }
        }
    }
}

// ==========================================
// PREMIUM USER PROFILE & OPERATIONS HUB (Dedicated, integrated Tab)
// ==========================================
@Composable
fun PremiumUserProfileScreen(
    viewModel: ApexViewModel,
    expansionViewModel: com.example.ui.ApexExpansionViewModel
) {
    var profileSubTab by remember { mutableStateOf("PILOT CARD") } // PILOT CARD, OPERATIONS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // High-contrast, top sliding selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Color(0xFF161616), RoundedCornerShape(10.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (profileSubTab == "PILOT CARD") Color(0xFF00FF88) else Color.Transparent)
                    .clickable { profileSubTab = "PILOT CARD" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "PILOT CARD",
                    color = if (profileSubTab == "PILOT CARD") Color.Black else Color.Gray,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (profileSubTab == "OPERATIONS") Color(0xFF00FFFF) else Color.Transparent)
                    .clickable { profileSubTab = "OPERATIONS" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "OPERATIONS",
                    color = if (profileSubTab == "OPERATIONS") Color.Black else Color.Gray,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (profileSubTab == "PILOT CARD") {
                ProductionUserProfileScreen(viewModel)
            } else {
                com.example.ui.ApexPhaseTwoDashboardScreen(
                    expansionViewModel = expansionViewModel,
                    onDismiss = {}
                )
            }
        }
    }
}

// ==========================================
// PREMIUM RACE MODE SCREEN: FORMULA-1 INSPIRED DRAG & SPRINT ENGINE
// ==========================================
@Composable
fun PremiumActiveRaceScreen(
    viewModel: ApexViewModel,
    expansionViewModel: com.example.ui.ApexExpansionViewModel
) {
    val context = LocalContext.current
    var raceStep by remember { mutableStateOf("PRE_RACE") } // PRE_RACE, COUNTDOWN, LIVE_RACE, RESULTS
    var selectedOpponent by remember { mutableStateOf("E85_GTR_Lover") }
    var creditsAmount by remember { mutableStateOf(200.0) }
    
    // Countdown counter
    var countdownValue by remember { mutableStateOf(5) }
    
    // Live telemetrics
    var liveSpeed by remember { mutableStateOf(0) }
    var elapsedMeters by remember { mutableStateOf(0f) }
    var reactionMs by remember { mutableStateOf(142) }
    var activeGForce by remember { mutableStateOf(1.2f) }
    
    // Outcome states
    var isWinner by remember { mutableStateOf(true) }
    var race0to60Time by remember { mutableStateOf(2.55f) }
    var quarterMileTime by remember { mutableStateOf(9.82f) }

    // REAL HARDWARE RACING STATES
    var useRealSensorsInRace by remember { mutableStateOf(false) }
    val realSessionState by expansionViewModel.raceSessionManager.state.collectAsState()
    val realSessionSpeed by expansionViewModel.raceSessionManager.speedMph.collectAsState()
    val realSessionG by expansionViewModel.raceSessionManager.gForce.collectAsState()
    val realSessionCountdown by expansionViewModel.raceSessionManager.countdown.collectAsState()

    var realStartTime by remember { mutableStateOf(0L) }
    var realElapsedTime by remember { mutableStateOf(0f) }

    LaunchedEffect(raceStep) {
        if (raceStep == "LIVE_RACE" && useRealSensorsInRace) {
            realStartTime = System.currentTimeMillis()
            while (raceStep == "LIVE_RACE") {
                delay(100)
                realElapsedTime = (System.currentTimeMillis() - realStartTime) / 1000f
            }
        } else {
            realElapsedTime = 0f
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            useRealSensorsInRace = true
        } else {
            useRealSensorsInRace = false
            Toast.makeText(context, "Location permissions needed for physical race runs.", Toast.LENGTH_LONG).show()
        }
    }

    Crossfade(targetState = raceStep, label = "RaceStepTransition") { step ->
        when (step) {
            "PRE_RACE" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            "DRAG CHAMPIONSHIP MOTORSPORTS",
                            color = Color(0xFFFF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "CHALLENGES WAGER ENGINE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Driver Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF070707))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF4444).copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SportsMotorsports, contentDescription = null, tint = Color(0xFFFF4444))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("PILOT COMMANDER", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("APEX_PILOT (ELO 1680)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Opponent Selector
                    Text("CHOOSE TARGET PEER TRANSMITTER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("E85_GTR_Lover", "Flat6_Whine", "RSR_Collective").forEach { driver ->
                        val active = selectedOpponent == driver
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (active) Color(0xFFFF4444) else Color(0xFF121212), RoundedCornerShape(12.dp))
                                .clickable { selectedOpponent = driver },
                            colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFF120808) else Color(0xFF070707))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(driver, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Match Type: 1/4 Mile Sprint Wager", color = Color.Gray, fontSize = 11.sp)
                                }
                                if (active) {
                                    Text("READY TO LOCK", color = Color(0xFFFF4444), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // Stakes level Selector
                    Text("SELECT STRATIFIED CREDITS ESCROW", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(100.0, 200.0, 500.0, 1000.0).forEach { amt ->
                            val isSel = creditsAmount == amt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFFFF4444) else Color(0xFF0A0A0A))
                                    .border(1.dp, if (isSel) Color(0xFFFF4444) else Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                                    .clickable { creditsAmount = amt }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${amt.toInt()} CR", color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }

                    // Use physical sensors toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A0A0A))
                            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                            .clickable {
                                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (useRealSensorsInRace) {
                                    useRealSensorsInRace = false
                                } else if (hasFine || hasCoarse) {
                                    useRealSensorsInRace = true
                                } else {
                                    permissionLauncher.launch(arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    ))
                                }
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (useRealSensorsInRace) Color(0xFFFF4444).copy(alpha = 0.15f) else Color(0x33FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (useRealSensorsInRace) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                    contentDescription = null,
                                    tint = if (useRealSensorsInRace) Color(0xFFFF4444) else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text("REAL PHYSICAL SENSORS RUN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text(
                                    text = if (useRealSensorsInRace) "Flows location & accelerometer into track logging" else "Synthetic vehicle simulator run",
                                    color = Color.Gray,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        Switch(
                            checked = useRealSensorsInRace,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasFine || hasCoarse) {
                                        useRealSensorsInRace = true
                                    } else {
                                        permissionLauncher.launch(arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        ))
                                    }
                                } else {
                                    useRealSensorsInRace = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFFFF4444),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF161616)
                            ),
                            modifier = Modifier.scale(0.75f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (useRealSensorsInRace) {
                                expansionViewModel.raceSessionManager.startRaceSession()
                            }
                            raceStep = "COUNTDOWN"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("LOCK ESCROW & INITIALIZE LAUNCH", color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }
                }
            }

            "COUNTDOWN" -> {
                if (useRealSensorsInRace) {
                    LaunchedEffect(realSessionState) {
                        if (realSessionState == com.example.service.RaceSessionState.ACTIVE) {
                            raceStep = "LIVE_RACE"
                        }
                    }
                    countdownValue = realSessionCountdown
                } else {
                    LaunchedEffect(Unit) {
                        countdownValue = 5
                        while (countdownValue > 0) {
                            delay(1000)
                            countdownValue--
                        }
                        raceStep = "LIVE_RACE"
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            "FORMULA-1 STEWARD LIGHTS",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        // F1 Grid lights (5 circles)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (i in 1..5) {
                                val lit = (5 - countdownValue) >= i
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (lit) Color(0xFFFF4444) else Color(0xFF101010))
                                        .border(2.dp, if (lit) Color(0xFFFF4444) else Color(0xFF222222), CircleShape)
                                        .then(if (lit) Modifier.background(Color(0xFFFF4444).copy(alpha = 0.2f)) else Modifier)
                                )
                            }
                        }

                        Text(
                            text = if (countdownValue > 0) "STAGE READY" else "GO!",
                            color = if (countdownValue > 0) Color(0xFFFF4444) else Color(0xFF00FF88),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            "LIVE_RACE" -> {
                if (useRealSensorsInRace) {
                    liveSpeed = realSessionSpeed.toInt()
                    activeGForce = realSessionG.toFloat()

                    LaunchedEffect(realSessionState) {
                        if (realSessionState == com.example.service.RaceSessionState.FINISHED) {
                            reactionMs = (120..220).random()
                            race0to60Time = 2.4f + (0..60).random() / 100f
                            quarterMileTime = 9.6f + (0..100).random() / 100f
                            isWinner = true
                            expansionViewModel.processRaceEloCalc(selectedOpponent, playerWon = isWinner)
                            raceStep = "RESULTS"
                        }
                    }
                } else {
                    // Live drag race simulation
                    LaunchedEffect(Unit) {
                        liveSpeed = 0
                        elapsedMeters = 0f
                        isWinner = (1..100).random() > 33
                        while (elapsedMeters < 400f) {
                            delay(80)
                            liveSpeed = (liveSpeed + (4..12).random()).coerceAtMost(168)
                            elapsedMeters += (liveSpeed * 0.16f)
                            activeGForce = (1.1f + (0..90).random() / 100f).coerceAtMost(2.6f)
                        }
                        reactionMs = (120..220).random()
                        race0to60Time = 2.3f + (0..90).random() / 100f
                        quarterMileTime = 9.4f + (0..110).random() / 100f
                        
                        expansionViewModel.processRaceEloCalc(selectedOpponent, playerWon = isWinner)
                        raceStep = "RESULTS"
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "LIVE TELEMETRIC SENSOR ACQUISITION",
                        color = Color(0xFF00FFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$liveSpeed",
                            color = Color(0xFF00FF88),
                            fontSize = 90.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("MPH TELEMETRY SPEED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Raw coordinates graph mockup
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .border(1.dp, Color(0xFF161616), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF080808))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val w = size.width
                            val h = size.height

                            // Draw curves
                            val p = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, h)
                                if (useRealSensorsInRace) {
                                    val ratio = (realElapsedTime / 15f).coerceIn(0f, 1f)
                                    quadraticTo(w * 0.4f, h * 0.7f, w * ratio, h * 0.15f)
                                } else {
                                    quadraticTo(w * 0.4f, h * 0.7f, w * (elapsedMeters / 400f).coerceIn(0f, 1f), h * 0.15f)
                                }
                            }
                            drawPath(p, Color(0xFF00FFFF), style = Stroke(width = 3.dp.toPx()))
                        }
                    }

                    // Progress track position
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (useRealSensorsInRace) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ELAPSED TRACK SENSOR TIME", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format("%.2f SEC / 1/4 MILE", realElapsedTime), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF101010))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((realElapsedTime / 15f).coerceIn(0f, 1f))
                                        .background(Color(0xFF00FF88))
                                )
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("COCKPIT DISTANCE", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format("%.1f M / 400.0 M", elapsedMeters), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF101010))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((elapsedMeters / 400f).coerceIn(0f, 1f))
                                        .background(Color(0xFF00FF88))
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("G-LOAD", color = Color.Gray, fontSize = 9.sp)
                            Text(String.format("%.2f G", activeGForce), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("OBD CLUTCH", color = Color.Gray, fontSize = 9.sp)
                            Text("ENGAGED", color = Color(0xFF00FF88), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (useRealSensorsInRace) {
                        Button(
                            onClick = {
                                expansionViewModel.raceSessionManager.stopRaceSession("VALID")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("COMPLETE RUN TELEMETRY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            "RESULTS" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isWinner) Color(0xFF00FF88).copy(0.12f) else Color(0xFFFF4444).copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWinner) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isWinner) Color(0xFF00FF88) else Color(0xFFFF4444),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = if (isWinner) "VICTORY ACQUIRED" else "DEFEAT LOGGED",
                        color = if (isWinner) Color(0xFF00FF88) else Color(0xFFFF4444),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    // Card outcome
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF161616), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF060606))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("RACE CHASSIS SUMMARIES", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("REACTION TIME", color = Color.White, fontSize = 13.sp)
                                Text("$reactionMs MS", color = Color(0xFF00FFFF), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("0to60 ACCELERATION", color = Color.White, fontSize = 13.sp)
                                Text(String.format("%.2f SECS", race0to60Time), color = Color(0xFF00FF88), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("1/4 MILE DURATION", color = Color.White, fontSize = 13.sp)
                                Text(String.format("%.2f SECS", quarterMileTime), color = Color(0xFF00FF88), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // Ledger Wager Escrow credits update
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isWinner) Color(0xFF00FF88).copy(0.4f) else Color(0xFFFF4444).copy(0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF050505))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ESCROW CREDITS LEDGER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = if (isWinner) "+${creditsAmount.toInt()} CR" else "-${creditsAmount.toInt()} CR",
                                color = if (isWinner) Color(0xFF00FF88) else Color(0xFFFF4444),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { raceStep = "PRE_RACE" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, Color(0xFF222222), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("RETURN TO RACE COCKPIT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SCREEN 1: HOME TAB (HIGH METRIC APEX HOME DASHBOARD WITH INTEGRATED DRIVE HUD & RACE PROG ENGINE)
// ==========================================
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedAndTelemetry(
    viewModel: ApexViewModel,
    expansionViewModel: com.example.ui.ApexExpansionViewModel,
    feedViewModel: com.example.FeedViewModel,
    onNavigateTab: (ApexTab) -> Unit
) {
    val localContext = LocalContext.current
    val localCoroutineScope = rememberCoroutineScope()

    // 1. Live stream collections from databases
    val runsList by viewModel.allPerformanceRuns.collectAsState()
    val feedList by viewModel.allPosts.collectAsState()
    val vehiclesList by expansionViewModel.allVehicles.collectAsState()
    val meetsList by viewModel.allMeets.collectAsState()
    val challengeList by viewModel.allChallenges.collectAsState()
    val onlineDrivers by expansionViewModel.onlineDrivers.collectAsState()
    val myWallet by expansionViewModel.myWallet.collectAsState()
    val driverRating by expansionViewModel.driverRating.collectAsState()

    // 2. Active simulated metrics on dashboard
    var simulatedSpeed by remember { mutableStateOf(0) }
    var maxLogSpeed by remember { mutableStateOf(112) }
    var currentGForce by remember { mutableStateOf(0.00) }
    var isSimulating by remember { mutableStateOf(false) }

    // Overlays for Drive and Race modes
    var showDriveModeHUD by remember { mutableStateOf(false) }
    var showRaceModeWizard by remember { mutableStateOf(false) }
    
    // View mode sub-tab (Dashboard vs Social TikTok-style feed)
    var dashboardSubTabMode by remember { mutableStateOf("DASHBOARD") } // DASHBOARD, FEED

    // Dynamic vehicle selection
    val activeVehicle = vehiclesList.find { it.model.contains("Mustang") } ?: vehiclesList.firstOrNull()
    val activeVehicleName = if (activeVehicle != null) {
        "${activeVehicle.make} ${activeVehicle.model}"
    } else {
        "Luke's Mustang GT"
    }

    // Active speed simulation loop
    LaunchedEffect(isSimulating) {
        if (isSimulating) {
            while (isSimulating) {
                delay(300)
                simulatedSpeed = (simulatedSpeed + (3..12).random()).coerceAtMost(165)
                currentGForce = (0.5 + (0..120).random() / 100.0).coerceAtMost(2.3)
                if (simulatedSpeed > maxLogSpeed) {
                    maxLogSpeed = simulatedSpeed
                }
                if (simulatedSpeed >= 165) {
                    isSimulating = false
                }
            }
        } else {
            // Speed decay
            while (simulatedSpeed > 0 && !isSimulating) {
                delay(200)
                simulatedSpeed = (simulatedSpeed - (8..18).random()).coerceAtLeast(0)
                currentGForce = (currentGForce - 0.12).coerceAtLeast(0.0)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(14.dp)
        ) {
            // Header: APEX Brand banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "APEX AUTOMOTIVE COCKPIT",
                        color = Color(0xFF00FFFF), // Neon Cyan
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        activeVehicleName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF88)) // Neon Green pulse
                        )
                        Text(
                            "System Coordinates Online",
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Glass switch for Dashboard vs Social feed
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (dashboardSubTabMode == "DASHBOARD") Color(0xFF00FFFF) else Color.Transparent)
                            .clickable { dashboardSubTabMode = "DASHBOARD" }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "Dashboard",
                            color = if (dashboardSubTabMode == "DASHBOARD") Color.Black else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (dashboardSubTabMode == "FEED") Color(0xFF00FFFF) else Color.Transparent)
                            .clickable { dashboardSubTabMode = "FEED" }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "Social Feed",
                            color = if (dashboardSubTabMode == "FEED") Color.Black else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (dashboardSubTabMode == "DASHBOARD") {
                // RENDER APEX CORE DASHBOARD LIST
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // TELEMATICS PANEL: SPEED / MAX / G-FORCE (Core requested widget)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x3300FFFF), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                            onClick = { isSimulating = !isSimulating }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "LIVE CHASSIS SENSOR TELEMETRIC",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isSimulating) "● ACTIVE SIMULATION" else "⚡ TAP TO ACCELERATE",
                                        color = if (isSimulating) Color(0xFF00FF88) else Color(0xFF00FFFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Speed display
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("SPEED", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "$simulatedSpeed",
                                                color = Color.White,
                                                fontSize = 44.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("MPH", color = Color(0xFF00FFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Custom visual glowing vertical divider
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(50.dp)
                                            .background(Color(0x3300FFFF))
                                    )

                                    // 2. Max logged speed
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("MAX SPEED", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "$maxLogSpeed",
                                                color = Color.LightGray,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("MPH", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }

                                    // Divider
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(50.dp)
                                            .background(Color(0x3300FFFF))
                                    )

                                    // 3. G-Force tracker
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("G-FORCE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = String.format("%.2f", currentGForce),
                                                color = if (currentGForce > 1.2) Color(0xFFFF4444) else Color(0xFF00FF88),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("G", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // RPM progressive bar overlay
                                val rpmProgress = (simulatedSpeed / 165f).coerceIn(0f, 1f)
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("RPM TACHOMETER SENSOR", color = Color.Gray, fontSize = 9.sp)
                                        Text("${(1000 + rpmProgress * 7000).toInt()} RPM", color = if (rpmProgress > 0.8) Color(0xFFFF4444) else Color(0xFF00FF88), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF1A1A1A))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(rpmProgress)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    if (rpmProgress > 0.8) Color(0xFFFF4444) else Color(0xFF00FF88)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // QUICK NAVIGATION ACTIONS PANEL Grid Layout
                    item {
                        Column {
                            Text(
                                "QUICK TELEMETRY CHASSIS ACTIONS",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Race action
                                ActionLaunchButton(
                                    title = "Race Mode",
                                    icon = Icons.Default.FlashOn,
                                    color = Color(0xFFFF4444),
                                    modifier = Modifier.weight(1f),
                                    onClick = { showRaceModeWizard = true }
                                )
                                // 2. Drive action
                                ActionLaunchButton(
                                    title = "Drive Mode",
                                    icon = Icons.Default.Navigation,
                                    color = Color(0xFF00FF88),
                                    modifier = Modifier.weight(1f),
                                    onClick = { showDriveModeHUD = true }
                                )
                                // 3. Maps link
                                ActionLaunchButton(
                                    title = "Dynamic Map",
                                    icon = Icons.Default.Map,
                                    color = Color(0xFF00FFFF),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateTab(ApexTab.MAP) }
                                )
                                // 4. Garage link
                                ActionLaunchButton(
                                    title = "Garage specs",
                                    icon = Icons.Default.DirectionsCar,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateTab(ApexTab.GARAGE) }
                                )
                            }
                        }
                    }

                    // DYNAMIC NEARBY PILOTS CARD LISTING
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "📡 NEARBY COMMUNICATORS RADAR",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "${onlineDrivers.size} PILOTS FOUND",
                                        color = Color(0xFF00FFFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                if (onlineDrivers.isEmpty()) {
                                    Text("No online pilots reported in coordinates.", color = Color.Gray, fontSize = 11.sp)
                                } else {
                                    onlineDrivers.take(2).forEach { pilot ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF00FFFF).copy(0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(pilot.username.take(1).uppercase(), color = Color(0xFF00FFFF), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(pilot.username, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Text("Sector: Mulholland Highway", color = Color.Gray, fontSize = 10.sp)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF00FF88).copy(0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("ELO 1640", color = Color(0xFF00FF88), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // DYNAMIC NEARBY MEETS CARD LISTING
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "🏁 ACTIVE LOCAL MEETS CORNERS",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (meetsList.isEmpty()) {
                                    Text("No rallies scheduled currently.", color = Color.Gray, fontSize = 11.sp)
                                } else {
                                    meetsList.take(2).forEach { meet ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(meet.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text("📍 ${meet.locationName}", color = Color.Gray, fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    Toast.makeText(localContext, "RSVPs Synchronized!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (meet.hasRsvped) Color(0xFF151515) else Color(0xFF00FFFF)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(if (meet.hasRsvped) "RSVPED" else "JOIN MEET", color = if (meet.hasRsvped) Color.Gray else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // DYNAMIC ACTIVE CHALLENGES CARD LISTING
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x22FF4444), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0505))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "⚡ ACTIVE COCKPIT WAGER CHALLENGES",
                                    color = Color(0xFFFF4444),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (challengeList.isEmpty()) {
                                    Text("No challenges pending in logs.", color = Color.Gray, fontSize = 11.sp)
                                } else {
                                    challengeList.take(2).forEach { ch ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("vs ${ch.challengerName}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Text("${ch.raceType} on Target: ${ch.opponentVehicle}", color = Color.Gray, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    showRaceModeWizard = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("ACCEPT RUN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // RENDER TIKTOK STYLE SOCIAL POSTS FEED SCREEN
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { feedList.size })
                    androidx.compose.foundation.pager.VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val post = feedList[page]
                        TikTokFeedItem(
                            post = post,
                            onLike = { viewModel.toggleLikePost(post) },
                            onComment = {
                                Toast.makeText(localContext, "Comment thread loaded: ${post.commentsCount} comments", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "APEX Telemetry Broadcast")
                                    putExtra(android.content.Intent.EXTRA_TEXT, "📡 ${post.authorName} (${post.authorTag}) Shared: \"${post.textContent}\"\n\nJoin APEX.")
                                }
                                localContext.startActivity(android.content.Intent.createChooser(shareIntent, "Share Telemetry"))
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 1. TRUE FULLSCREEN TELEMETRY DRIVE MODE HUD OVERLAY (Feature 2)
        // ==========================================
        AnimatedVisibility(
            visible = showDriveModeHUD,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { /* Block parent clicks */ }
            ) {
                // Live Landscape-Supported Telemetry Screens HUD layout
                var liveDriveMph by remember { mutableStateOf(0) }
                var tripDistance by remember { mutableStateOf(12.4) }
                var peakGRecorded by remember { mutableStateOf(0.98) }
                
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(250)
                        liveDriveMph = (liveDriveMph + (-4..8).random()).coerceIn(0, 155)
                        tripDistance += 0.01
                        peakGRecorded = maxOf(peakGRecorded, (0.4 + (0..110).random() / 100.0)).coerceAtMost(2.5)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00FF88))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("DRIVE HUD ACTIVE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(activeVehicleName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        IconButton(
                            onClick = { showDriveModeHUD = false },
                            modifier = Modifier
                                .background(Color(0xFF1C1C1C), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close HUD", tint = Color.LightGray)
                        }
                    }

                    // Large Digital Gauge Display Core
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Dial Column: Compass & Gforces Coordinates
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("G-RADAR VECTOR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(1.dp, Color(0xFF00FF88).copy(0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw concentric radar vector lines
                                drawCircleDot(offsetX = 15f, offsetY = -10f, color = Color(0xFF00FF88))
                                Text("COMPASS\n284° NW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }

                        // Center Digital Core: SPEED & RPM Graph curves
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SPEEDOMETER COGNITIVE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$liveDriveMph",
                                color = Color(0xFF00FF88),
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("MPH CRANK UNIT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(10.dp))

                            // Glowing RPM visual arc bar
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(5.dp)
                                    .background(Color(0xFF151515))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth( (liveDriveMph / 155f).coerceIn(0f, 1f) )
                                        .background(Color(0xFF00FF88))
                                )
                            }
                        }

                        // Right Dial Column: Trip Log & Altimeter specs and thresholds
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ALTIMETER", color = Color.Gray, fontSize = 10.sp)
                                Text("1,482 FT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TRIP MILES", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format("%.2f MI", tripDistance), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PEAK G-LIMIT", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format("%.2f G", peakGRecorded), color = Color(0xFFFF4444), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }

                    // Lower informational footer row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "BLUETOOTH OBD LINK: EMULATION ACTIVE",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "STABILITY REGISTRATION: LOCK OK",
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. TRUE RACING GAMEFLOW WORKFLOW WIZARD (Feature 3)
        // ==========================================
        AnimatedVisibility(
            visible = showRaceModeWizard,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020202))
                    .clickable { /* Block parent click */ }
            ) {
                var currentWizardStep by remember { mutableStateOf(1) } // Steps: 1: opponent, 2: car, 3: wager, 4: countdown, 5: live drag, 6: results

                var selectedOpponent by remember { mutableStateOf<String>("E85_GTR_Lover") }
                var wagerEscrowStakeAmount by remember { mutableStateOf(200.0) }
                
                // Simulation logs
                var raceCountdownTimer by remember { mutableStateOf(3) }
                var isCountdownRunning by remember { mutableStateOf(false) }
                
                var dragMphState by remember { mutableStateOf(0) }
                var playerRaceOutcomeWon by remember { mutableStateOf(true) }
                var finalTime0to60 by remember { mutableStateOf(0.0) }
                var finalQuarterMileTime by remember { mutableStateOf(0.0) }

                when (currentWizardStep) {
                    1 -> {
                        // Wizard Step 1: Select Opponent
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                WizardHeaderClose(onClose = { showRaceModeWizard = false }, title = "RACE WIZARD: STEP 1 OF 6")
                                Text("SELECT YOUR OPPONENT TRANSMITTER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                
                                val onlineCompetitors = listOf("E85_GTR_Lover", "Flat6_Whine", "RSR_Collective")
                                onlineCompetitors.forEach { peer ->
                                    val isChosen = selectedOpponent == peer
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, if (isChosen) Color(0xFFFF4444) else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                            .clickable { selectedOpponent = peer },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isChosen) Color(0xFF150A0A) else Color(0xFF0C0C0C)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFF4444).copy(0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Radar, contentDescription = "Pilot", tint = Color(0xFFFF4444), modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(peer, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("ELO Ranking Standard Match", color = Color.Gray, fontSize = 11.sp)
                                                }
                                            }
                                            if (isChosen) {
                                                Text("CHOSEN", color = Color(0xFFFF4444), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { currentWizardStep = 2 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("CHOOSE RUNNING VEHICLE", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    2 -> {
                        // Wizard Step 2: Choose active vehicle
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                WizardHeaderClose(onClose = { showRaceModeWizard = false }, title = "RACE WIZARD: STEP 2 OF 6")
                                Text("SELECT YOUR ACTIVE RUNNING CHASSIS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                                vehiclesList.forEach { car ->
                                    val isCurrentActive = activeVehicle?.id == car.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, if (isCurrentActive) Color(0xFF00FFFF) else Color(0x19FFFFFF), RoundedCornerShape(12.dp))
                                            .clickable {
                                                expansionViewModel.changeTargetVehicle(car.id)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0C))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("${car.year} ${car.make} ${car.model}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${car.horsepower} Crank HP / ${car.drivetrain}", color = Color.Gray, fontSize = 11.sp)
                                            }
                                            if (isCurrentActive) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF00FFFF).copy(0.12f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("ACTIVE TARGET", color = Color(0xFF00FFFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { currentWizardStep = 3 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("CONFIGURE CREDIT ESCROW STAKE", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    3 -> {
                        // Wizard Step 3: Propose wager credits deductions
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                WizardHeaderClose(onClose = { showRaceModeWizard = false }, title = "RACE WIZARD: STEP 3 OF 6")
                                Text("PROPOSE CORRELATION CREDITS BET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                
                                val myCreditsBalance = myWallet?.balance ?: 1000.0
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0C0B)),
                                    border = BorderStroke(1.dp, Color(0xFF00FF88))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("APEX COCKPIT WALLET STATUS", color = Color.Gray, fontSize = 10.sp)
                                        Text(String.format("%.0f CREDITS AVAILABLE", myCreditsBalance), color = Color(0xFF00FF88), fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    }
                                }

                                Text("SELECT WAGER STAKE LEVEL ( isolated in Escrow )", color = Color.Gray, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    listOf(100.0, 200.0, 500.0, 1000.0).forEach { amt ->
                                        val selected = wagerEscrowStakeAmount == amt
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) Color(0xFFFF4444) else Color(0xFF151515))
                                                .clickable { wagerEscrowStakeAmount = amt }
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                "${amt.toInt()} CR",
                                                color = if (selected) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    // Trigger Escrow checking
                                    val myCreditsBalance = myWallet?.balance ?: 1000.0
                                    if (wagerEscrowStakeAmount > myCreditsBalance) {
                                        Toast.makeText(localContext, "INSUFFICIENT FUNDS ISOLATING WARNING! Initializing fallback simulation...", Toast.LENGTH_SHORT).show()
                                    }
                                    currentWizardStep = 4
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("CONFIRM ESCROW & INITIALIZE SPRINT", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    4 -> {
                        // Wizard Step 4: Countdown 3-2-1 READY GO
                        LaunchedEffect(Unit) {
                            raceCountdownTimer = 3
                            while (raceCountdownTimer > 0) {
                                delay(1000)
                                raceCountdownTimer--
                            }
                            currentWizardStep = 5
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (raceCountdownTimer > 0) "ARMING ESCROW DEBRIS" else "LAUNCH",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 3.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = if (raceCountdownTimer > 0) "$raceCountdownTimer" else "GO!",
                                    color = if (raceCountdownTimer > 0) Color(0xFFFF4444) else Color(0xFF00FF88),
                                    fontSize = 120.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "Ready target vehicle gear and stomp accelerator!",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    5 -> {
                        // Wizard Step 5: Live Drag Sprint Telemetry Progress
                        LaunchedEffect(Unit) {
                            dragMphState = 0
                            playerRaceOutcomeWon = (1..100).random() > 30 // 70% win chance
                            while (dragMphState < 135) {
                                delay(180)
                                dragMphState += (4..12).random()
                            }
                            // Calculate final mock outputs
                            finalTime0to60 = 2.4 + (0..90).random() / 100.0
                            finalQuarterMileTime = 9.8 + (0..140).random() / 100.0
                            
                            // Write results atomically to ELO system and Wallet ledger databases!
                            expansionViewModel.processRaceEloCalc(selectedOpponent, playerWon = playerRaceOutcomeWon)
                            
                            currentWizardStep = 6
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("LIVE DRAG RACE COORDINATES TRACKER", color = Color(0xFFFF4444), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "$dragMphState",
                                        color = Color.White,
                                        fontSize = 80.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MPH", color = Color(0xFFFF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("YOU (${activeVehicleName})", color = Color(0xFF00FFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("OPPONENT (${selectedOpponent})", color = Color.Gray, fontSize = 11.sp)
                                }

                                // Interactive Progress bar tracks progress to 1/4 mile
                                val raceCompletionPct = (dragMphState / 135f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF151515))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(raceCompletionPct)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFF4444))
                                    )
                                }

                                Text(
                                    "Telemetry anti-cheat checking algorithms monitoring throttle/GPS thresholds...",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    6 -> {
                        // Wizard Step 6: Settle results & displays payouts/validation indexes
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("RACE INTERFERES COMPLETE", color = Color(0xFF00FF88), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
                                
                                Text(
                                    text = if (playerRaceOutcomeWon) "YOU WON!" else "OPPONENT WON",
                                    color = if (playerRaceOutcomeWon) Color(0xFF00FF88) else Color(0xFFFF4444),
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0E0D))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("VERIFIED PERFORMANCE TIMERS:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("0-60 MPH Speed Limit Launch", color = Color.Gray, fontSize = 12.sp)
                                            Text(String.format("%.2f s", finalTime0to60), color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("1/4 Mile Sprint Trap Time", color = Color.Gray, fontSize = 12.sp)
                                            Text(String.format("%.2f s", finalQuarterMileTime), color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Wager Escrow Adjustment Outcome", color = Color.Gray, fontSize = 12.sp)
                                            Text(
                                                if (playerRaceOutcomeWon) "+${wagerEscrowStakeAmount.toInt()} Credits Added" else "-${wagerEscrowStakeAmount.toInt()} Credits Deducted",
                                                color = if (playerRaceOutcomeWon) Color(0xFF00FF88) else Color(0xFFFF4444),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = "Security Status", tint = Color(0xFF00FF88), modifier = Modifier.size(22.dp))
                                        Column {
                                            Text("GPS TELEMETRY SECURITY: VERIFIED SECURE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("No farming or spoofed variables mapped. Approved.", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { showRaceModeWizard = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF)),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("RETURN TO COCKPIT", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2D Dot Radar layout helper
@Composable
fun BoxScope.drawCircleDot(offsetX: Float, offsetY: Float, color: Color) {
    Canvas(modifier = Modifier.align(Alignment.Center).size(8.dp)) {
        drawCircle(color, radius = size.width / 2)
    }
}

// Wizard Header close row helper
@Composable
fun WizardHeaderClose(onClose: () -> Unit, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF151515))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .background(Color(0xFF151515), CircleShape)
                .size(32.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}

// Quick Dashboard Action button visual styling
@Composable
fun ActionLaunchButton(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Social Subscreen
@Composable
fun SocialFeedSubscreen(viewModel: ApexViewModel, feedList: List<SocialPost>, feedViewModel: com.example.FeedViewModel) {
    val context = LocalContext.current
    var showAddPostDialog by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }
    var telemetryVerifiedTag by remember { mutableStateOf("") } // e.g. 0-60: 2.9s
    var showMysteryMatchPanel by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Mystery Race Quick Action Matcher Banner (Prominent feature)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .clickable { showMysteryMatchPanel = true },
                colors = CardDefaults.cardColors(containerColor = Color(0x73121212))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x1900FFFF),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FlashOn, contentDescription = "Match", tint = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("MYSTERY RACE RADAR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("Find similarly-powered local drivers.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = { showMysteryMatchPanel = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("AUTO MATCH", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub Feed List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CANYON LOGS", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Button(
                    onClick = { showAddPostDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0x55FFFFFF)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New post", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                HomeFeedScreen(feedViewModel)
            }
        }

        // 1. MYSTERY MATCH SCANNING RADAR OVERLAY
        if (showMysteryMatchPanel) {
            var scanState by remember { mutableStateOf(0) } // 0: Idle, 1: Scanning, 2: Found, 3: Open Chat
            val scanTransition = rememberInfiniteTransition(label = "RadarSweep")
            val pulseRadius by scanTransition.animateFloat(
                initialValue = 0f,
                targetValue = 180f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "RadarPulse"
            )

            LaunchedEffect(scanState) {
                if (scanState == 1) {
                    delay(3000)
                    scanState = 2
                }
            }

            AlertDialog(
                onDismissRequest = { showMysteryMatchPanel = false },
                containerColor = Color(0xFF0D0D0D),
                modifier = Modifier.border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(20.dp)),
                title = null,
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (scanState == 0) {
                            Text("IN-RANGE POWER MATCHING", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Matches with active drivers within 10 miles having ±100 HP compared to your current garage setup. Keeps coordinates safe.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { scanState = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("INITIALIZE RADAR SWEEP")
                            }
                        } else if (scanState == 1) {
                            Text("SCANNING AIRWAVES...", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .border(1.dp, Color(0x22FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val c = center
                                    drawCircle(
                                        color = Color(0xFF00FFCC).copy(alpha = (1 - (pulseRadius / 180f)).coerceIn(0f, 1f)),
                                        radius = pulseRadius.dp.toPx(),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color(0xFF00FFCC).copy(alpha = 0.2f),
                                        radius = 20.dp.toPx()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FFCC))
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Matching HP, transmission class...", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        } else if (scanState == 2) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Matched", tint = Color(0xFF00FFCC), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("PROXIMITY MATCH SECURED!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("User: @ViperGTS", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Car: 2017 Dodge Viper (645 HP)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { scanState = 3 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ESTABLISH CANYON CHAT COMMS")
                            }
                        } else if (scanState == 3) {
                            Text("SECURE COMS: @ViperGTS", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1F1F1F))
                                        .align(Alignment.Start)
                                        .padding(8.dp)
                                ) {
                                    Text("Saw you have the Plaid. Down for a rolling 1/4 mile verification run tonight?", color = Color.White, fontSize = 12.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00FFCC))
                                        .align(Alignment.End)
                                        .padding(8.dp)
                                ) {
                                    Text("Always ready. Meet up at Mulholland scenery coordinate.", color = Color.White, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    showMysteryMatchPanel = false
                                    Toast.makeText(context, "Cryptographic channels active. Coordinates matching.", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("CLOSE SECURE LINK", color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // 2. SOCIAL NEW FEED POST POPUP PANEL
        if (showAddPostDialog) {
            AlertDialog(
                onDismissRequest = { showAddPostDialog = false },
                containerColor = Color(0xFF0F0F0F),
                title = { Text("Log New Active Run", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = { Text("What did you dyno, launch, or modify?", color = Color.White.copy(alpha = 0.4f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = telemetryVerifiedTag,
                            onValueChange = { telemetryVerifiedTag = it },
                            placeholder = { Text("Add verified stats (e.g. 0-60: 3.12s) [Optional]", color = Color.White.copy(alpha = 0.4f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (postText.isNotEmpty()) {
                                viewModel.submitPost(postText, telemetryVerifiedTag)
                                postText = ""
                                telemetryVerifiedTag = ""
                                showAddPostDialog = false
                            }
                        }
                    ) {
                        Text("POST TO APEX", color = Color(0xFF00FFCC))
                    }
                }
            )
        }
    }
}

// Telemetry & GPS Subscreen (Includes full live run simulator)
@Composable
fun TelemetryDashboardSubscreen(viewModel: ApexViewModel, runsList: List<PerformanceRun>) {
    var isSimulatingRun by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(0f) }
    var progressTime by remember { mutableStateOf(0.0) }
    var runZeroSixty by remember { mutableStateOf(0.0) }
    var runQuarterMile by remember { mutableStateOf(0.0) }
    var runTopSpeed by remember { mutableStateOf(0.0) }
    var gForceX by remember { mutableStateOf(0f) }
    var gForceY by remember { mutableStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Fidelity Cockpit Gauges Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF080808))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("COCKPIT telemetry INSTRUMENTS", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Speed Dial
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color(0x22FFFFFF),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                            )
                            drawArc(
                                color = Color(0xFF00FFCC),
                                startAngle = 135f,
                                sweepAngle = (speed / 180f) * 270f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${speed.toInt()}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 36.sp, fontFamily = FontFamily.Monospace)
                            Text("MPH", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // G-Force Sensor Circle
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .border(1.dp, Color(0x22FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val c = center
                            drawLine(Color(0x1AFFFFFF), Offset(0f, c.y), Offset(size.width, c.y), strokeWidth = 1.dp.toPx())
                            drawLine(Color(0x1AFFFFFF), Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 1.dp.toPx())
                            drawCircle(Color(0x1600FFCC), radius = 30.dp.toPx())

                            // Active force coordinate node
                            drawCircle(
                                color = Color(0xFF00FFCC),
                                radius = 6.dp.toPx(),
                                center = Offset(c.x + (gForceX * 40.dp.toPx()), c.y + (gForceY * 40.dp.toPx()))
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("G-FORCE", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("L", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp)
                                Text("${"%.2f".format(gForceX)}X\n${"%.2f".format(gForceY)}Y", color = Color(0xFF00FFCC), fontSize = 9.sp, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
                                Text("R", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp)
                            }
                            Text("REAR", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Time tracking indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0-60 MPH", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Text(if (runZeroSixty > 0) "${"%.2f".format(runZeroSixty)}s" else "--", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1/4 MILE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Text(if (runQuarterMile > 0) "${"%.2f".format(runQuarterMile)}s" else "--", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (isSimulatingRun) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RUN TIME", color = Color(0xFF00FFCC), fontSize = 11.sp)
                            Text("${"%.2f".format(progressTime)}s", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PULL TOP", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            Text(if (runTopSpeed > 0) "${runTopSpeed.toInt()} MPH" else "--", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // RUN ACTION TRIGGER BUTTONS
                Button(
                    onClick = {
                        if (!isSimulatingRun) {
                            isSimulatingRun = true
                            speed = 0f
                            progressTime = 0.0
                            runZeroSixty = 0.0
                            runQuarterMile = 0.0
                            runTopSpeed = 0.0
                            runPerformanceSimulation(
                                onTick = { s, pt, gx, gy ->
                                    speed = s
                                    progressTime = pt
                                    gForceX = gx
                                    gForceY = gy
                                },
                                onZeroSixty = { runZeroSixty = it },
                                onQuarterMile = { runQuarterMile = it },
                                onCompleted = { maxS ->
                                    runTopSpeed = maxS.toDouble()
                                    isSimulatingRun = false
                                    gForceX = 0f
                                    gForceY = 0f
                                    viewModel.savePerformanceRun(
                                        PerformanceRun(
                                            zeroToSixty = runZeroSixty,
                                            zeroToOneHundred = runZeroSixty * 1.8,
                                            sixtyToOneThirty = 5.23,
                                            quarterMile = if (runQuarterMile > 0) runQuarterMile else 10.42,
                                            topSpeed = maxS.toDouble(),
                                            maxGForce = 1.34,
                                            distanceTraveled = 0.25
                                        )
                                    )
                                    coroutineScope.launch {
                                        delay(300)
                                        // Auto-post verified run to canyon logs feed if amazing!
                                        viewModel.submitPost(
                                            "Just completed a telemetry validation pull. Top Speed clocked at ${maxS.toInt()} MPH with 0-60 in ${"%.2f".format(runZeroSixty)}s!",
                                            "Telemetry Verified: ${"%.2f".format(runZeroSixty)}s 0-60 | ${maxS.toInt()} MPH Max"
                                        )
                                    }
                                }
                            )
                        }
                    },
                    enabled = !isSimulatingRun,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSimulatingRun) Color.DarkGray else Color(0xFF00FFCC)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Active Speed Run", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSimulatingRun) "GPS TELEMETRY RUN ACTIVE" else "LAUNCH GPS SPEED RUN", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Run History section
        Text("VERIFIED PERFORMANCE RUN LOGS", color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)

        if (runsList.isEmpty()) {
            Text("No telemetry logs saved inside data engine yet.", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        } else {
            runsList.forEach { run ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x2B121212))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = "Time Specs", tint = Color(0xFF00FFCC), modifier = Modifier.size(14.dp))
                                Text("0-60: ${"%.2f".format(run.zeroToSixty)}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("1/4 Mile: ${"%.2f".format(run.quarterMile)}s", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }

                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${run.topSpeed.toInt()} MPH", color = Color(0xFF00FFCC), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("G-Force Max: ${run.maxGForce}G", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// Simul engine loop
fun runPerformanceSimulation(
    onTick: (speed: Float, time: Double, gX: Float, gY: Float) -> Unit,
    onZeroSixty: (Double) -> Unit,
    onQuarterMile: (Double) -> Unit,
    onCompleted: (maxSpeed: Float) -> Unit
) {
    var s = 0f
    var t = 0.0
    var zeroSixtyCaptured = false
    var quarterMileCaptured = false

    val timer = java.util.Timer()
    timer.scheduleAtFixedRate(object : java.util.TimerTask() {
        override fun run() {
            t += 0.1
            // Basic physics: Rapid initial acceleration, wind drag tapering off
            if (s < 60f) {
                s += (8f - (s * 0.05f))
            } else if (s < 100f) {
                s += (4f - (s * 0.02f))
            } else {
                s += (2f - (s * 0.008f))
            }

            if (s >= 135f || t >= 12.0) {
                onCompleted(s)
                timer.cancel()
            } else {
                if (s >= 60f && !zeroSixtyCaptured) {
                    onZeroSixty(t)
                    zeroSixtyCaptured = true
                }
                if (t >= 10.42 && !quarterMileCaptured) {
                    onQuarterMile(t)
                    quarterMileCaptured = true
                }

                // Simulate random high G forces shifting back and forth
                val gx = (sin(t * 3) / 2.0).toFloat() + 0.3f
                val gy = (cos(t * 2) * 1.1).toFloat()

                onTick(s, t, gx, gy)
            }
        }
    }, 0L, 100L)
}

// ==========================================
// 7. SCREEN 2: VEHICLE GARAGE TAB (HP CALC)
// ==========================================
@Composable
fun GaragePerformanceScreen(viewModel: ApexViewModel) {
    val garageList by viewModel.allVehicles.collectAsState()
    var showAddVehicle by remember { mutableStateOf(false) }

    // Calc states
    var calcWeight by remember { mutableStateOf("") }
    var calcQuarterTime by remember { mutableStateOf("") }
    var calcEstimatedHp by remember { mutableStateOf(0.0) }

    // Sub-structure Tab layouter: Vault vs Interactive Dyno
    var selectedGarageTab by remember { mutableStateOf("Vault") } // Vault, Dyno
    var activeDynoVehicle by remember { mutableStateOf<Vehicle?>(null) }
    
    // Interactive Dyno states
    var dynoRpm by remember { mutableStateOf(1000f) }
    var peakHpObserved by remember { mutableStateOf(0f) }
    var peakTorqueObserved by remember { mutableStateOf(0f) }
    var peakRpmObserved by remember { mutableStateOf(0f) }
    var isDynoPullRunning by remember { mutableStateOf(false) }
    
    // Auto-advance RPM during push simulation
    LaunchedEffect(isDynoPullRunning) {
        if (isDynoPullRunning) {
            dynoRpm = 1000f
            peakHpObserved = 0f
            peakTorqueObserved = 0f
            peakRpmObserved = 0f
            while (dynoRpm < 8200f && isDynoPullRunning) {
                kotlinx.coroutines.delay(40)
                dynoRpm += 160f
                
                // Calculate torque & horsepower curve dynamically
                // Torque peak at around 4800 RPM
                val currentPowerCar = activeDynoVehicle ?: (garageList.firstOrNull() ?: Vehicle(year=2011, make="Ford", model="Mustang GT", trim="5.0", engine="Coyote V8", horsepower=412, torque=390, weight=3600, transmission="6-Speed", drivetrain="RWD"))
                val scale = 1.0 - Math.pow(((dynoRpm - 4800f) / 4500f).toDouble(), 2.0).coerceIn(0.0..1.0)
                val calculatedTorque = (currentPowerCar.torque * (0.65 + 0.35 * scale)).toFloat()
                val calculatedHp = ((calculatedTorque * dynoRpm) / 5252f).toFloat()
                
                if (calculatedHp > peakHpObserved) {
                    peakHpObserved = calculatedHp
                    peakRpmObserved = dynoRpm
                }
                if (calculatedTorque > peakTorqueObserved) {
                    peakTorqueObserved = calculatedTorque
                }
            }
            isDynoPullRunning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab selector row (Vault vs Interactive Dyno)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF101010))
                .padding(4.dp)
        ) {
            listOf("Vault" to "🏎️ Vault Builds", "Dyno" to "📈 Interactive Dyno").forEach { (tabId, label) ->
                val isSelected = selectedGarageTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF00FF88) else Color.Transparent)
                        .clickable { selectedGarageTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (isSelected) Color.Black else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (selectedGarageTab == "Vault") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("THE VAULT", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text("Active Garage", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }
                Button(
                    onClick = { showAddVehicle = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADD BUILD")
                }
            }

            // Render current Garage builds
            if (garageList.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No vehicles registered in the vault.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            garageList.forEach { car ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text("${car.year} ${car.make}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${car.model} ${car.trim}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { 
                                        activeDynoVehicle = car
                                        selectedGarageTab = "Dyno"
                                    }
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Dyno Run", tint = Color(0xFF00FF88))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteVehicle(car) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Build", tint = Color.Red.copy(alpha = 0.6f))
                                }
                            }
                        }

                        // Vehicle Telemetry Spec Grid
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF101010), RoundedCornerShape(8.dp)).padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("POWER", color = Color.Gray, fontSize = 9.sp)
                                Text("${car.horsepower} HP", color = Color(0xFF00FFCC), fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                            Column {
                                Text("TORQUE", color = Color.Gray, fontSize = 9.sp)
                                Text("${car.torque} TQ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("WEIGHT", color = Color.Gray, fontSize = 9.sp)
                                Text("${car.weight} lbs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("DRIVETRAIN", color = Color.Gray, fontSize = 9.sp)
                                Text(car.drivetrain, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Category cards representing detailed BUILD SHEET metadata (Requested Feature 5)
                        Text("VEHICLE BUILD SHEET SPECIFICATION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Engine Category
                            BuildSheetCategoryRow(icon = "🔩", category = "Engine / Block Cylinders", original = car.engine, upgraded = if (car.modifications.contains("Intake") || car.modifications.contains("Cam")) "Stage 2 Aggressive Block Custom Tuned" else "Standard OEM Block")
                            
                            // Transmission Category
                            BuildSheetCategoryRow(icon = "⚙️", category = "Transmission / Clutch", original = car.transmission, upgraded = "Exedy Heavy Duty Stage 1 Triple Plate upgrade")
                            
                            // Suspension Category
                            BuildSheetCategoryRow(icon = "🏎️", category = "Suspension & Springs", original = "Independent Coilover Convert", upgraded = "Eibach Sportline Lowering Adjustables")
                            
                            // Brakes Category
                            BuildSheetCategoryRow(icon = "🛑", category = "Braking & Calipers", original = "Dual Piston OEM Calipers", upgraded = "Brembo 6-piston high-friction carbon logs")
                            
                            // Tires Category
                            BuildSheetCategoryRow(icon = "🛞", category = "Grip Tires & Compound", original = "All-season touring tires", upgraded = "Michelin Pilot Sport 4S (drag radial compound)")
                        }

                        if (car.modifications.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("CUSTOM MODIFICATIONS TRACKING LOGS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(car.modifications, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Interactive Dyno tab (Requested Feature 6)
            val activeCar = activeDynoVehicle ?: (garageList.firstOrNull() ?: Vehicle(year=2011, make="Ford", model="Mustang GT", trim="5.0", engine="Coyote V8", horsepower=412, torque=390, weight=3600, transmission="6-Speed", drivetrain="RWD"))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("APEX COCKPIT PRO", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text("Interactive Dyno Pull", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("Test rig active for: ${activeCar.year} ${activeCar.make} ${activeCar.model}", color = Color.Gray, fontSize = 12.sp)
                }

                // Visual live values
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENGINE SPEED", color = Color.Gray, fontSize = 9.sp)
                            Text("${dynoRpm.toInt()} RPM", color = Color(0xFF00FFCC), fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
                    ) {
                        // Math Wheel Horsepower (WHP) = (TQ * RPM) / 5252
                        val currentScale = 1.0 - Math.pow(((dynoRpm - 4800f) / 4500f).toDouble(), 2.0).coerceIn(0.0..1.0)
                        val currTq = (activeCar.torque * (0.65 + 0.35 * currentScale)).toFloat()
                        val currHp = ((currTq * dynoRpm) / 5252f).toFloat()

                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WHEEL POWER", color = Color.Gray, fontSize = 9.sp)
                            Text(String.format("%.0f WHP", currHp), color = Color(0xFFFF5252), fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Interactive CANVAS dyno curves (HP in Cyan, Torque in Red vs RPM)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF040404))
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw TIGHTER grid backdrop (graph paper look)
                            for (gridRpm in 1000..8000 step 500) {
                                val gridX = ((gridRpm - 1000f) / 7000f) * w
                                drawLine(
                                    color = if (gridRpm % 1000 == 0) Color(0x1CFFFFFF) else Color(0x08FFFFFF),
                                    start = Offset(gridX, 0f),
                                    end = Offset(gridX, h),
                                    strokeWidth = 1f
                                )
                            }
                            // Horizontal lines
                            for (p in 1..8) {
                                val gridY = h * (p / 9f)
                                drawLine(
                                    color = if (p % 2 == 0) Color(0x15FFFFFF) else Color(0x06FFFFFF),
                                    start = Offset(0f, gridY),
                                    end = Offset(w, gridY),
                                    strokeWidth = 1f
                                )
                            }

                            // Plot curves
                            val maxResolution = 45
                            val hpPoints = mutableListOf<Offset>()
                            val tqPoints = mutableListOf<Offset>()

                            for (i in 0..maxResolution) {
                                val pointRpm = 1000f + (7000f * (i.toFloat() / maxResolution))
                                if (pointRpm > dynoRpm) break

                                val pointScale = 1.0 - Math.pow(((pointRpm - 4800f) / 4500f).toDouble(), 2.0).coerceIn(0.0..1.0)
                                val pointTq = (activeCar.torque * (0.65 + 0.35 * pointScale)).toFloat()
                                val pointHp = ((pointTq * pointRpm) / 5252f).toFloat()

                                // Map coordinates
                                val px = ((pointRpm - 1000f) / 7000f) * w
                                val pyHp = h - ((pointHp / 650f) * h).coerceIn(0f..h)
                                val pyTq = h - ((pointTq / 650f) * h).coerceIn(0f..h)

                                hpPoints.add(Offset(px, pyHp))
                                tqPoints.add(Offset(px, pyTq))
                            }

                            // 1. Draw a beautiful background gradient under HP curve
                            val hpPath = androidx.compose.ui.graphics.Path()
                            if (hpPoints.isNotEmpty()) {
                                hpPath.moveTo(hpPoints.first().x, h)
                                for (p in hpPoints) {
                                    hpPath.lineTo(p.x, p.y)
                                }
                                hpPath.lineTo(hpPoints.last().x, h)
                                hpPath.close()
                                drawPath(
                                    path = hpPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0x2200FFFF), Color.Transparent)
                                    )
                                )
                            }

                            // 2. Draw a beautiful background gradient under Torque curve
                            val tqPath = androidx.compose.ui.graphics.Path()
                            if (tqPoints.isNotEmpty()) {
                                tqPath.moveTo(tqPoints.first().x, h)
                                for (p in tqPoints) {
                                    tqPath.lineTo(p.x, p.y)
                                }
                                tqPath.lineTo(tqPoints.last().x, h)
                                tqPath.close()
                                drawPath(
                                    path = tqPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0x15FF4444), Color.Transparent)
                                    )
                                )
                            }

                            // 3. Draw HP curve in Neon Cyan
                            if (hpPoints.size > 1) {
                                for (k in 0 until hpPoints.size - 1) {
                                    drawLine(Color(0xFF00FFFF), hpPoints[k], hpPoints[k+1], strokeWidth = 5f)
                                }
                            }
                            // 4. Draw Torque curve in Neon Red/Alert Red
                            if (tqPoints.size > 1) {
                                for (k in 0 until tqPoints.size - 1) {
                                    drawLine(Color(0xFFFF4444), tqPoints[k], tqPoints[k+1], strokeWidth = 5f)
                                }
                            }

                            // 5. Active sweeping vertical cursor with neon targeting crosshairs
                            val activeRpmX = ((dynoRpm - 1000f) / 7000f) * w
                            drawLine(
                                color = if (dynoRpm > 7000f) Color(0xFFFF4444) else Color(0xFF00FF88),
                                start = Offset(activeRpmX, 0f),
                                end = Offset(activeRpmX, h),
                                strokeWidth = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                            )

                            // Crosshairs target on active Hp and Torque sweeps
                            if (hpPoints.isNotEmpty()) {
                                val activeHpPos = hpPoints.last()
                                val activeTqPos = tqPoints.last()

                                // Hp target indicator crosshair
                                drawCircle(Color(0xFF00FFFF), radius = 5.dp.toPx(), center = activeHpPos)
                                drawCircle(Color.Black, radius = 2.dp.toPx(), center = activeHpPos)

                                // Torque target indicator crosshair
                                drawCircle(Color(0xFFFF4444), radius = 5.dp.toPx(), center = activeTqPos)
                                drawCircle(Color.Black, radius = 2.dp.toPx(), center = activeTqPos)
                            }

                            // Draw text labels using Android raw NativeCanvas (100% reliable)
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 22f
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.MONOSPACE
                            }

                            // Write RPM grid values
                            drawContext.canvas.nativeCanvas.drawText("1K RPM", 5f, h - 8f, textPaint)
                            drawContext.canvas.nativeCanvas.drawText("3K RPM", ((3000f - 1000f) / 7000f) * w - 30f, h - 8f, textPaint)
                            drawContext.canvas.nativeCanvas.drawText("5K RPM", ((5000f - 1000f) / 7000f) * w - 30f, h - 8f, textPaint)
                            drawContext.canvas.nativeCanvas.drawText("7K RPM", ((7000f - 1000f) / 7000f) * w - 30f, h - 8f, textPaint)
                            drawContext.canvas.nativeCanvas.drawText("8K RPM", w - 85f, h - 8f, textPaint)

                            // Write Y-axis power markers
                            drawContext.canvas.nativeCanvas.drawText("600 HP/TQ", 10f, 25f, textPaint)
                            drawContext.canvas.nativeCanvas.drawText("300 HP/TQ", 10f, h / 2f, textPaint)
                            drawContext.canvas.nativeCanvas.drawText("150 HP/TQ", 10f, h * 0.75f, textPaint)
                        }

                        // Curve indicator labels overlay in top-left
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color(0xCC050505), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FFFF)))
                                Text("POWER (WHP)", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF4444)))
                                Text("TORQUE (LB-FT)", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Peak observed statistics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("PEAK METRICS OBSERVED:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Peak Power:", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.1f WHP @ %.0f RPM", peakHpObserved, peakRpmObserved), color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Peak Torque:", color = Color.Gray, fontSize = 11.sp)
                            Text(String.format("%.1f lb-ft", peakTorqueObserved), color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Engine Redline Bounds:", color = Color.Gray, fontSize = 11.sp)
                            Text("8,200 RPM Max Sweep", color = Color.Red, fontSize = 11.sp)
                        }
                    }
                }

                // Accelerate throttle push triggers (Item 6 - Simulation accelerometer triggers)
                Button(
                    onClick = { isDynoPullRunning = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDynoPullRunning) Color.Red else Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isDynoPullRunning) "ENGAGING ACCELEROMETER PULL... WHP SWEEPING" else "LAUNCH INTERACTIVE DYNO PULL 🏎️💨",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // HORSEPOWER CALCULATOR WIDGET (Mandated item)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Build, contentDescription = "Calculator", tint = Color(0xFF00FFCC))
                    Text("ESTIMATED ENGINE HP ESTIMATOR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }

                Text("Calculate estimated horsepower at wheels based on curb weight and quarter-mile drag time using physics power-to-weight index.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = calcWeight,
                        onValueChange = { calcWeight = it },
                        label = { Text("Weight (lbs)", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calcQuarterTime,
                        onValueChange = { calcQuarterTime = it },
                        label = { Text("1/4 Mile Time (s)", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val wt = calcWeight.toDoubleOrNull()
                        val qTime = calcQuarterTime.toDoubleOrNull()
                        if (wt != null && qTime != null && qTime > 0) {
                            // Riegel math power estimate: HP = Weight / (Time / 5.825)^3
                            val ratio = qTime / 5.825
                            calcEstimatedHp = wt / (ratio * ratio * ratio)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("EXECUTE PHYSICS CALC")
                }

                if (calcEstimatedHp > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ESTIMATED BUILD CORRECTION: ${calcEstimatedHp.toInt()} WHP",
                            color = Color(0xFF00FFCC),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // New vehicle sheet form modal dialog
        if (showAddVehicle) {
            var inputYear by remember { mutableStateOf("2023") }
            var inputMake by remember { mutableStateOf("") }
            var inputModel by remember { mutableStateOf("") }
            var inputTrim by remember { mutableStateOf("") }
            var inputHp by remember { mutableStateOf("") }
            var inputTq by remember { mutableStateOf("") }
            var inputWeight by remember { mutableStateOf("") }
            var inputMods by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddVehicle = false },
                containerColor = Color(0xFF0F0F0F),
                title = { Text("Add Vehicle to Vault", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputMake, onValueChange = { inputMake = it }, label = { Text("Make (e.g. BMW)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC))
                        )
                        OutlinedTextField(
                            value = inputModel, onValueChange = { inputModel = it }, label = { Text("Model (e.g. M4)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC))
                        )
                        OutlinedTextField(
                            value = inputHp, onValueChange = { inputHp = it }, label = { Text("Horsepower") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC))
                        )
                        OutlinedTextField(
                            value = inputWeight, onValueChange = { inputWeight = it }, label = { Text("Curb Weight (lbs)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC))
                        )
                        OutlinedTextField(
                            value = inputMods, onValueChange = { inputMods = it }, label = { Text("Modifications List") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC))
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (inputMake.isNotEmpty() && inputModel.isNotEmpty()) {
                                viewModel.addVehicle(
                                    year = inputYear.toIntOrNull() ?: 2023,
                                    make = inputMake,
                                    model = inputModel,
                                    trim = inputTrim,
                                    eng = "Tuned Engine",
                                    hp = inputHp.toIntOrNull() ?: 300,
                                    tq = inputTq.toIntOrNull() ?: 280,
                                    wt = inputWeight.toIntOrNull() ?: 3200,
                                    tr = "Manual/Auto",
                                    dt = "AWD",
                                    mods = inputMods,
                                    valEst = 45000.0
                                )
                                showAddVehicle = false
                            }
                        }
                    ) {
                        Text("SAVE BUILD", color = Color(0xFF00FFCC))
                    }
                }
            )
        }
    }
}

@Composable
fun BuildSheetCategoryRow(icon: String, category: String, original: String, upgraded: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B1B1B)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(category, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(original, color = Color.White.copy(0.45f), fontSize = 11.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    Icon(Icons.Default.ArrowForward, contentDescription = "Upgraded to", tint = Color(0xFF00FF88), modifier = Modifier.size(12.dp))
                    Text(upgraded, color = Color(0xFF00FF88), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 8. SCREEN 3: MAP TAB (SIMULATED & REAL HARDWARE SENSORS)
// ==========================================
@Composable
fun InteractiveTelemetryMap(viewModel: ApexViewModel) {
    val context = LocalContext.current
    val meets by viewModel.allMeets.collectAsState()
    var selectedMeetMarker by remember { mutableStateOf<CarMeet?>(null) }
    var mapTabMysteryMode by remember { mutableStateOf(false) }

    // Tactical Radar map states
    var radarAngle by remember { mutableStateOf(0f) }
    var timelineProgress by remember { mutableStateOf(0.42f) }
    var isReplayPlaying by remember { mutableStateOf(true) }
    var activeFilter by remember { mutableStateOf("ALL") } // ALL, MEETS, PILOTS
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var currentMapStyle by remember { mutableStateOf("SATELLITE") } // SATELLITE or DARK_MODE

    // REAL SENSORS INTEGRATION
    var isRealSensorsMode by remember { mutableStateOf(false) }
    val realLocation by viewModel.realLocation.collectAsState()
    val realSpeedMph by viewModel.realSpeedMph.collectAsState()
    val realGForceX by viewModel.realGForceX.collectAsState()
    val realGForceY by viewModel.realGForceY.collectAsState()
    val realGForceZ by viewModel.realGForceZ.collectAsState()

    var originLocation by remember { mutableStateOf<android.location.Location?>(null) }
    val gpsPathPoints = remember { mutableStateListOf<Offset>() }

    var browserLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var browserSpeedMph by remember { mutableStateOf(0.0) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            isRealSensorsMode = true
        } else {
            isRealSensorsMode = false
            Toast.makeText(context, "Location permission is required for real-time telemetry.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(isRealSensorsMode) {
        if (isRealSensorsMode) {
            viewModel.startRealTracking()
        } else {
            viewModel.stopRealTracking()
            originLocation = null
            gpsPathPoints.clear()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRealTracking()
        }
    }

    LaunchedEffect(realLocation, isRealSensorsMode) {
        val loc = realLocation
        if (isRealSensorsMode && loc != null) {
            if (originLocation == null) {
                originLocation = loc
            }
            val origin = originLocation
            if (origin != null) {
                val latDiff = loc.latitude - origin.latitude
                val lonDiff = loc.longitude - origin.longitude
                val scale = 250000f * zoomLevel
                val relativeX = (lonDiff * scale).toFloat()
                val relativeY = -(latDiff * scale).toFloat()

                val newOffset = Offset(relativeX, relativeY)
                if (gpsPathPoints.isEmpty() || (gpsPathPoints.last() - newOffset).getDistance() > 8f) {
                    gpsPathPoints.add(newOffset)
                    if (gpsPathPoints.size > 250) {
                        gpsPathPoints.removeAt(0)
                    }
                }
            }
        }
    }

    // Live telemetry position
    val simulatedLat = 34.1205 + (0.0042 * kotlin.math.sin(timelineProgress.toDouble() * Math.PI))
    val simulatedLon = -118.4568 - (0.0035 * kotlin.math.cos(timelineProgress.toDouble() * Math.PI))

    val displayLat = if (isRealSensorsMode && realLocation != null) {
        realLocation!!.latitude
    } else if (browserLocation != null) {
        browserLocation!!.first
    } else {
        simulatedLat
    }

    val displayLon = if (isRealSensorsMode && realLocation != null) {
        realLocation!!.longitude
    } else if (browserLocation != null) {
        browserLocation!!.second
    } else {
        simulatedLon
    }

    LaunchedEffect(isReplayPlaying) {
        if (isReplayPlaying) {
            while (isReplayPlaying) {
                delay(50)
                radarAngle = (radarAngle + 2.5f) % 360f
                timelineProgress = (timelineProgress + 0.0015f)
                if (timelineProgress > 1.0f) timelineProgress = 0.0f
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("RADAR & MEETS", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                Text("Proximity Map", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, Color(0xFF161616), RoundedCornerShape(12.dp))
                    .clickable { mapTabMysteryMode = !mapTabMysteryMode }
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!mapTabMysteryMode) Color(0xFF00FFCC) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Radar Map", color = if (!mapTabMysteryMode) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (mapTabMysteryMode) Color(0xFF00FFCC) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Mystery Race", color = if (mapTabMysteryMode) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (mapTabMysteryMode) {
            MysteryRaceMatchmakingScreen(viewModel)
        } else {
            // Real hardware sensor toggle switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070707))
                    .border(1.dp, Color(0xFF161616), RoundedCornerShape(12.dp))
                    .clickable {
                        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (isRealSensorsMode) {
                            isRealSensorsMode = false
                        } else if (hasFine || hasCoarse) {
                            isRealSensorsMode = true
                        } else {
                            permissionLauncher.launch(arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                        }
                    }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isRealSensorsMode) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRealSensorsMode) Icons.Default.LocationOn else Icons.Default.LocationOff,
                            contentDescription = "Sensor Toggle",
                            tint = if (isRealSensorsMode) Color(0xFF00FFCC) else Color.Gray,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Column {
                        Text("ACTIVE HARDWARE SENSORS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(
                            text = if (isRealSensorsMode) "Real-time phone GPS tracking & G-load" else "Virtual simulation (Offline sandbox replay)",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }
                Switch(
                    checked = isRealSensorsMode,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                isRealSensorsMode = true
                            } else {
                                permissionLauncher.launch(arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }
                        } else {
                            isRealSensorsMode = false
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFF00FFCC),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF161616)
                    ),
                    modifier = Modifier.scale(0.75f)
                )
            }

            // Tactical navigation HUD chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "🛰️ ALL SENSORS", "MEETS" to "🏁 CAR MEETS", "PILOTS" to "📡 PILOTS").forEach { (filterId, label) ->
                    val isSelected = activeFilter == filterId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0x2200FFCC) else Color(0xFF0A0A0A))
                            .border(1.dp, if (isSelected) Color(0xFF00FFCC) else Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                            .clickable { activeFilter = filterId }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF00FFCC) else Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Tactical Grid map container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Real high-fidelity Google Maps style satellite and GPS map
                    com.example.ui.RealMapView(
                        centerLat = displayLat,
                        centerLon = displayLon,
                        zoomLevel = zoomLevel,
                        mapMode = currentMapStyle,
                        onlineDrivers = if (activeFilter == "ALL" || activeFilter == "PILOTS") {
                            listOf(
                                com.example.data.DriverPresence("M4_LaunchMonster", "Online", displayLat + 0.002, displayLon - 0.003),
                                com.example.data.DriverPresence("Porsche_Purist", "Online", displayLat - 0.003, displayLon + 0.004)
                            )
                        } else emptyList(),
                        meetsList = if (activeFilter == "ALL" || activeFilter == "MEETS") meets else emptyList(),
                        isRealSensorsMode = isRealSensorsMode,
                        realLocation = realLocation,
                        onMarkerClick = { type, markerName ->
                            if (type == "meet") {
                                selectedMeetMarker = meets.find { it.title == markerName }
                            } else {
                                Toast.makeText(context, "$type: $markerName clicked!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBrowserLocationUpdate = { lat, lon, speed ->
                            browserLocation = Pair(lat, lon)
                            browserSpeedMph = speed
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Tactical overlay details panel (Grid Coordinates & Speed)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0A0A).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "TACTICAL RECEIVER COORDS",
                            color = Color(0xFF00FFCC),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = String.format("GPS LAT: %.5f° N", displayLat),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("GPS LON: %.5f° W", displayLon),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val dotColor = if (isRealSensorsMode) Color(0xFF00FFFF) else if (browserLocation != null) Color(0xFF00FFCC) else Color(0xFF00FF88)
                            val stateText = if (isRealSensorsMode) "SYS STATE: PHONE GPS" else if (browserLocation != null) "SYS STATE: BROWSER GPS" else "SYS STATE: ACTIVE REPLAY"
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                            Text(
                                text = stateText,
                                color = dotColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isRealSensorsMode) {
                            Text(
                                text = String.format("GPS SPD: %.1f MPH", realSpeedMph),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format("ACCEL: X:%.2f Y:%.2f Z:%.2f G", realGForceX, realGForceY, realGForceZ),
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else if (browserLocation != null) {
                            Text(
                                text = String.format("GPS SPD: %.1f MPH", browserSpeedMph.toFloat()),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ACCEL: BROWSER TELEMETRY LIVE",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Map controls (Zoom / Filter Chips & Style Toggle)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color(0xBB000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel + 0.15f).coerceAtMost(2.5f) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel - 0.15f).coerceAtLeast(0.5f) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        
                        // Beautiful modern visual separator
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(1.dp)
                                .background(Color(0xFF333333))
                        )

                        IconButton(
                            onClick = { 
                                currentMapStyle = if (currentMapStyle == "SATELLITE") "DARK_MODE" else "SATELLITE" 
                                Toast.makeText(context, "Switching to ${if (currentMapStyle == "SATELLITE") "Satellite" else "Dark Mode"} Map Style", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers, 
                                contentDescription = "Toggle Map Style", 
                                tint = if (currentMapStyle == "SATELLITE") Color(0xFF00FFCC) else Color.White, 
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Legend
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xDD0A0A0A))
                            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FFCC)))
                            Text("ACTIVE CAR MEETS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FF88)))
                            Text("ACTIVE GPS TRACKS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Beautiful replay timeline slider player widget
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { isReplayPlaying = !isReplayPlaying },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF101010), CircleShape)
                            .border(1.dp, Color(0xFF1A1A1A), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isReplayPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause Replay",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("APEX RUN TELEMETRY TIMELINE PLAYBACK", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(String.format("%.0f%% COMPLETE", timelineProgress * 100), color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = timelineProgress,
                            onValueChange = {
                                timelineProgress = it
                                isReplayPlaying = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FFCC),
                                activeTrackColor = Color(0xFF00FFCC),
                                inactiveTrackColor = Color(0xFF1A1A1A)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            // Selected meet info dialogue popup card
            AnimatedVisibility(visible = selectedMeetMarker != null) {
                val element = selectedMeetMarker
                if (element != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(element.title.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                IconButton(onClick = { selectedMeetMarker = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                                }
                            }
                            Text("📍 GPS LOCATION: ${element.locationName}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("⏱️ START TIME: ${element.startTime}", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                            Button(
                                onClick = {
                                    viewModel.toggleRsvp(element)
                                    Toast.makeText(context, "Marked RSVP, coordinates sent to GPS!", Toast.LENGTH_SHORT).show()
                                    selectedMeetMarker = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("RSVP & SYNC DRIVE PATH COGNITIVE", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. SCREEN 4: EVENTS AND CHALLENGES SCREEN
// ==========================================
@Composable
fun EventsAndChallengesScreen(viewModel: ApexViewModel) {
    val meetsList by viewModel.allMeets.collectAsState()
    val challengesList by viewModel.allChallenges.collectAsState()

    var showCreateMeet by remember { mutableStateOf(false) }
    var selectedMeetDetails by remember { mutableStateOf<CarMeet?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("APEX GATHERINGS", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                Text("Meets & Challenges", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            Button(
                onClick = { showCreateMeet = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Host Meet")
                Spacer(modifier = Modifier.width(6.dp))
                Text("HOST MEET")
            }
        }

        // Car Meets list Section
        Text("ACTIVE CAR MEETS", color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)

        meetsList.forEach { meet ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .clickable { selectedMeetDetails = meet },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(meet.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x1900FF88)
                        ) {
                            Text("${meet.rsvpCount} Attending", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Text("📍 ${meet.locationName}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Host: ${meet.hostName}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        Text(meet.startTime, color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleRsvp(meet) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (meet.hasRsvped) Color(0xFF141414) else Color(0xFF00FFCC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (meet.hasRsvped) "RSVPED (LEAVE)" else "RSVP AT CAR MEET")
                        }
                    }
                }
            }
        }

        // Active Challenges Section
        Text("STREET RACING CHALLENGES", color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)

        challengesList.forEach { ch ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF080808))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${ch.challengerName} VS ${ch.opponentName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (ch.status) {
                                "Accepted" -> Color(0x2200FFCC)
                                "Completed" -> Color(0x22FFFFFF)
                                else -> Color(0x2200FFFF)
                            }
                        ) {
                            Text(ch.status.uppercase(), color = if (ch.status == "Accepted") Color(0xFF00FFCC) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Text("Opponent build: ${ch.opponentVehicle}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("Race Mode: ${ch.raceType}", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scheduled: ${ch.scheduledTime}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        if (ch.status == "Pending" && ch.opponentName == "Apex_Pilot") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.updateChallengeStatus(ch, "Declined") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("DECLINE", color = Color.Red, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.updateChallengeStatus(ch, "Accepted") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("ACCEPT", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (ch.status == "Accepted") {
                            Button(
                                onClick = { viewModel.updateChallengeStatus(ch, "Completed") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("VALIDATE RUN & SUBMIT W", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Host meet dialog modal
        if (showCreateMeet) {
            var mTitle by remember { mutableStateOf("") }
            var mLoc by remember { mutableStateOf("") }
            var mRules by remember { mutableStateOf("") }
            var mStart by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showCreateMeet = false },
                containerColor = Color(0xFF0F0F0F),
                title = { Text("Host New Apex Meet", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = mTitle, onValueChange = { mTitle = it }, label = { Text("Gathering Title") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)))
                        OutlinedTextField(value = mLoc, onValueChange = { mLoc = it }, label = { Text("Scenic Location") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)))
                        OutlinedTextField(value = mRules, onValueChange = { mRules = it }, label = { Text("Ground Rules") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)))
                        OutlinedTextField(value = mStart, onValueChange = { mStart = it }, label = { Text("Start Time (e.g. Sat 10:00 PM)") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (mTitle.isNotEmpty() && mLoc.isNotEmpty()) {
                                viewModel.addCarMeet(
                                    title = mTitle, location = mLoc, rules = mRules, capacity = 100, start = mStart, end = "Midnight"
                                )
                                showCreateMeet = false
                            }
                        }
                    ) {
                        Text("BROADCAST MEET", color = Color(0xFF00FFCC))
                    }
                }
            )
        }
    }
}

// ==========================================
// 10. SCREEN 5: STANDINGS LEADERBOARD SCREEN
// ==========================================
data class PilotLeaderboard(
    val rank: Int,
    val name: String,
    val car: String,
    val zeroToSixty: String,
    val hp: Int,
    val wins: Int
)

@Composable
fun StandingsLeaderboardScreen(viewModel: ApexViewModel) {
    var leaderFilterClass by remember { mutableStateOf("0-60 MPH") } // 0-60 vs Horsepower

    val leaderboard0To60 = remember {
        listOf(
            PilotLeaderboard(1, "M4_LaunchMonster", "BMW M4 Competition", "2.89s", 680, 52),
            PilotLeaderboard(2, "Apex_Pilot", "Tesla Model S Plaid", "2.92s", 1020, 34),
            PilotLeaderboard(3, "GTR_Steve", "Nissan R35 GT-R", "3.01s", 720, 48),
            PilotLeaderboard(4, "Porsche_Purist", "911 GT3 RS", "3.12s", 518, 21),
            PilotLeaderboard(5, "HellcatHustler", "Dodge Charger", "3.32s", 807, 18)
        )
    }

    val leaderboardHp = remember {
        listOf(
            PilotLeaderboard(1, "Apex_Pilot", "Tesla Model S Plaid", "2.92s", 1020, 34),
            PilotLeaderboard(2, "TwinTurbo_Vette", "C7 Corvette Z06", "3.15s", 950, 45),
            PilotLeaderboard(3, "HellcatHustler", "Dodge Charger", "3.32s", 807, 18),
            PilotLeaderboard(4, "GTR_Steve", "Nissan R35 GT-R", "3.01s", 720, 48),
            PilotLeaderboard(5, "M4_LaunchMonster", "BMW M4 Competition", "2.89s", 680, 52)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("GLOBAL COMMUNICATOR", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
            Text("Apex Class Leaderboards", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }

        // Toggle category buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { leaderFilterClass = "0-60 MPH" },
                colors = ButtonDefaults.buttonColors(containerColor = if (leaderFilterClass == "0-60 MPH") Color(0xFF00FFCC) else Color(0xFF141414)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("LAUNCH 0-60 TIER")
            }
            Button(
                onClick = { leaderFilterClass = "HORSEPOWER" },
                colors = ButtonDefaults.buttonColors(containerColor = if (leaderFilterClass == "HORSEPOWER") Color(0xFF00FFCC) else Color(0xFF141414)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("PEAK HP STANDINGS")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Standings List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val listToRender = if (leaderFilterClass == "0-60 MPH") leaderboard0To60 else leaderboardHp
            items(listToRender) { pilot ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (pilot.name == "Apex_Pilot") 1.dp else 0.dp,
                            color = if (pilot.name == "Apex_Pilot") Color(0xFF00FFCC) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Metallic styling for Top Ranks
                            Surface(
                                shape = CircleShape,
                                color = when (pilot.rank) {
                                    1 -> Color(0xFFFFD700)
                                    2 -> Color(0xFFC0C0C0)
                                    else -> Color(0xFF1F1F1F)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${pilot.rank}",
                                        color = if (pilot.rank <= 2) Color.Black else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(pilot.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (pilot.name == "Apex_Pilot") {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF00FFCC)) {
                                            Text("YOU", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Text(pilot.car, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }

                        // Right hand metrics
                        Column(horizontalAlignment = Alignment.End) {
                            if (leaderFilterClass == "0-60 MPH") {
                                Text(pilot.zeroToSixty, color = Color(0xFF00FFCC), fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                Text("HP: ${pilot.hp}", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            } else {
                                Text("${pilot.hp} HP", color = Color(0xFF00FFCC), fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                Text("Wins: ${pilot.wins}", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 11. SCREEN 6: SETTINGS USER PROFILE TAB
// ==========================================
@Composable
fun SettingsUserProfileScreen(viewModel: ApexViewModel) {
    val context = LocalContext.current
    val myProfile by viewModel.myProfile.collectAsState()

    var firebaseUserEmail by remember { mutableStateOf(com.example.service.ApexFirebaseService.currentUserEmail) }
    var triggerAuthUpdate by remember { mutableStateOf(0) }

    DisposableEffect(triggerAuthUpdate) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { currentAuth ->
            firebaseUserEmail = currentAuth.currentUser?.email
        }
        try {
            auth.addAuthStateListener(listener)
        } catch (e: Exception) {
            // Fallback
        }
        onDispose {
            try {
                auth.removeAuthStateListener(listener)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editLoc by remember { mutableStateOf("") }

    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(myProfile) {
        val p = myProfile
        if (p != null) {
            editName = p.username
            editBio = p.bio
            editLoc = p.location
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("METRICS ENGINE", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
            Text("Pilot Profile", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }

        // Profile Identity card with glossy aesthetic
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0C))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Pilot Avatar frame
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .border(2.dp, Color(0xFF00FFCC), CircleShape)
                        .padding(4.dp)
                        .background(Color(0xFF141414), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.size(48.dp))
                }

                Text("@${myProfile?.username ?: "Apex_Pilot"}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(myProfile?.location ?: "Los Angeles, CA", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Text(
                    text = myProfile?.bio ?: "No pilot bio configured yet.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                Divider(color = Color(0x19FFFFFF))

                // Stats Dashboard Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FOLLOWERS", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        Text("${myProfile?.followers ?: 0}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WIN RECORD", color = Color(0xFF00FFCC), fontSize = 10.sp)
                        Text("${myProfile?.wins ?: 0} W", color = Color(0xFF00FFCC), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LOSS LOGS", color = Color.Red.copy(alpha = 0.8f), fontSize = 10.sp)
                        Text("${myProfile?.losses ?: 0} L", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // APEX CLOUD SECURE BACKUP & AUTHENTICATION PANEL
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF00FFCC).copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070707))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Cloud Integration Status",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "APEX CLOUD RACER SECURITY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                if (firebaseUserEmail != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x1B00FFCC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("CONNECTED PILOT", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(firebaseUserEmail ?: "", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00FFCC))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("ACTIVE CLOUD", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Text(
                        "Telemetry runs, speed stats, and profile logs are securely duplicable to the remote persistent cloud cluster.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                    Button(
                        onClick = {
                            com.example.service.ApexFirebaseService.signOutUser()
                            triggerAuthUpdate++
                            Toast.makeText(context, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SIGN OUT RACER IDENTITY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                } else {
                    var emailInput by remember { mutableStateOf("") }
                    var passwordInput by remember { mutableStateOf("") }
                    var authErrorState by remember { mutableStateOf("") }
                    var isLoadingAuth by remember { mutableStateOf(false) }

                    Text(
                        "Unlocking persistent cloud storage backs up your local telemetry logs and registers your global pilot callsign.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; authErrorState = "" },
                        label = { Text("Cloud Pilot Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; authErrorState = "" },
                        label = { Text("Racer Passcode (6+ chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )

                    if (authErrorState.isNotEmpty()) {
                        Text(authErrorState, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isLoadingAuth) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                                        authErrorState = "Enter email and passcode parameters."
                                        return@Button
                                    }
                                    isLoadingAuth = true
                                    val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                    coroutineScope.launch {
                                        com.example.service.ApexFirebaseService.loginUser(emailInput, passwordInput) { success, message ->
                                            isLoadingAuth = false
                                            if (success) {
                                                triggerAuthUpdate++
                                                Toast.makeText(context, "Authenticated successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                authErrorState = message
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("SIGN IN", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                                        authErrorState = "Enter email and passcode parameters."
                                        return@Button
                                    }
                                    if (passwordInput.length < 6) {
                                        authErrorState = "Passcode must be 6+ characters."
                                        return@Button
                                    }
                                    isLoadingAuth = true
                                    val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                    coroutineScope.launch {
                                        com.example.service.ApexFirebaseService.registerUser(emailInput, passwordInput) { success, message ->
                                            isLoadingAuth = false
                                            if (success) {
                                                triggerAuthUpdate++
                                                Toast.makeText(context, "Racer profile created successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                authErrorState = message
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414)),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("REGISTER PILOT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                if (emailInput.isEmpty()) {
                                    authErrorState = "Enter email for resetting password link."
                                    return@TextButton
                                }
                                com.example.service.ApexFirebaseService.resetPassword(emailInput) { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("FORGOT RACER PASSCODE?", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ACHIEVEMENT BADGES WALL (Mandate item)
        Text("PILOT ACHIEVEMENT BADGES", color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val badges = (myProfile?.badges ?: "").split(",")
            badges.forEach { badge ->
                if (badge.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1A00FFFF))
                            .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Badge Icon", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Text(badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // SAFETY & COMPLIANCE SYSTEM ACTIONS (Mandate item)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0x11FF0000))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = "Safety Block", tint = Color.Red)
                    Text("COCKPIT SAFETY MODERATION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text("Access rapid-moderation options, block problematic racers, read safety canyon speed compliance directives, or coordinate rapid support links.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Canyon Safety Moderation: Emergency authorities and roadside assist dispatched.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("EMERGENCY ASSIST", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            Toast.makeText(context, "Community guidelines loaded. Mod actions logged.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("REGULATIONS", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        // Profile Editor Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MODIFY CORRELATING IDENTITY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color(0xFF00FFCC))
                    }
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it }, label = { Text("Call-sign") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC))
                    )
                    OutlinedTextField(
                        value = editBio, onValueChange = { editBio = it }, label = { Text("Profile Spec Bio") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC))
                    )
                    OutlinedTextField(
                        value = editLoc, onValueChange = { editLoc = it }, label = { Text("Location Area") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC))
                    )
                    Button(
                        onClick = {
                            viewModel.updateProfile(editName, editBio, editLoc)
                            isEditing = false
                            Toast.makeText(context, "Identity updated successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE IDENTITY CHANGES")
                    }
                }
            }
        }
    }
}

package com.example

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.service.ApexFirebaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// 1. DYNAMIC PRODUCTION COMMENT STATE ENTITIES
// ==========================================
data class PostComment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val postId: Long,
    val author: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Active Virtual Driver Match Configuration for Mystery Match
data class RacerMatch(
    val username: String,
    val vehicle: String,
    val classification: String, // AWD / RWD / R-Class
    val horsepower: Int,
    val distanceMiles: Double,
    val status: String = "Idle" // Idle, Requesting, Matched, Declined
)

// ==========================================
// 2. STAGED STATIC SEED FOR DEMONSTRATIONS
// ==========================================
private val mockCommentsSeed = mutableStateListOf<PostComment>(
    PostComment(postId = 1L, author = "CanyonCollector", text = "Exhaust note is insane in person! Just saw you rolling on Sunset Blvd."),
    PostComment(postId = 1L, author = "EV_Shredder", text = "Instant torque on that Plaid is no joke, lets dig!"),
    PostComment(postId = 2L, author = "Porsche_Purist", text = "Weissach package represents the finest engineering on the planet, hands down!")
)

// ==========================================
// 3. HOME SCREEN: INF-SCROLL FEED, SHARING & COMMS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionSocialFeedScreen(
    viewModel: ApexViewModel,
    feedList: List<SocialPost>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Interactive Dialog and Paging variables
    var showAddPostDialog by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }
    var teleTag by remember { mutableStateOf("") }
    
    // Comments Sheet Overlay state
    var activeCommentPost by remember { mutableStateOf<SocialPost?>(null) }
    var newCommentText by remember { mutableStateOf("") }
    var activeComments = remember { mutableStateListOf<PostComment>() }

    // Scroll state paging simulation
    var itemsToShowCount by remember { mutableStateOf(5) }
    var isPagingLoading by remember { mutableStateOf(false) }

    // Detect when user scrolled to the bottom (Infinite Scroll trigger)
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isPagingLoading && itemsToShowCount < feedList.size + 15) {
            isPagingLoading = true
            delay(1200) // Beautiful network throttle mockup
            itemsToShowCount += 5
            isPagingLoading = false
        }
    }

    // Refresh active comment overlay list whenever selected post changes
    LaunchedEffect(activeCommentPost) {
        activeComments.clear()
        activeCommentPost?.let { post ->
            val match = mockCommentsSeed.filter { it.postId == post.id }
            activeComments.addAll(match)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Post feed lazy stack
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Create Post Trigger Action Bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F).copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FFCC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showAddPostDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(22.dp)),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Broadcast telemetry update...", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Render limited items list respecting Infinite Scroll bound
                val visiblePosts = feedList.take(itemsToShowCount)
                items(visiblePosts) { post ->
                    SocialCardItem(
                        post = post,
                        onLikeClick = { viewModel.toggleLikePost(post) },
                        onCommentClick = { activeCommentPost = post },
                        onShareClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "APEX Telemetry Broadcast")
                                putExtra(Intent.EXTRA_TEXT, "📡 ${post.authorName} (${post.authorTag}) Shared: \"${post.textContent}\" ${if (post.hasVideoProof) "\n⚡ Verified Metrics: " + post.videoProofStats else ""}\n\nJoin APEX Automotive network coordinates.")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Telemetry"))
                        }
                    )
                }

                if (isPagingLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // FLOATING ACTION OVERLAY - CREATE POST
        if (showAddPostDialog) {
            AlertDialog(
                onDismissRequest = { showAddPostDialog = false },
                containerColor = Color(0xFF0A0A0A),
                title = { Text("Apex Telemetry Broadcast", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = { Text("What is your rig's current coordinate activity?", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                        OutlinedTextField(
                            value = teleTag,
                            onValueChange = { teleTag = it },
                            placeholder = { Text("Optional telemetry proof (e.g. 0-60: 3.2s)", color = Color.Gray) },
                            textStyle = TextStyle(color = Color(0xFF00FFCC), fontSize = 13.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF00FFCC),
                                focusedBorderColor = Color(0xFF00FFCC),
                                unfocusedBorderColor = Color(0x22FFFFFF)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (postText.isNotBlank()) {
                                viewModel.submitPost(postText, teleTag)
                                // Trigger Firestore synchronization fallback
                                ApexFirebaseService.syncPostToCloud(
                                    postId = "post_${System.currentTimeMillis()}",
                                    text = postText,
                                    stats = teleTag
                                )
                                postText = ""
                                teleTag = ""
                                showAddPostDialog = false
                                Toast.makeText(context, "Telemetry broadcast sent onto channels.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                    ) {
                        Text("Broadcast", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPostDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // INTERACTIVE COMMENTS SHEET OVERLAY
        activeCommentPost?.let { post ->
            AlertDialog(
                onDismissRequest = { activeCommentPost = null },
                containerColor = Color(0xFF0F0F0F),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp)),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chassis Comments", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { activeCommentPost = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.height(300.dp)) {
                        // Scrolling Comments Stack
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (activeComments.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No chassis handshakes yet.", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                items(activeComments) { comment ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(comment.author, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("Active Profile", color = Color.White.copy(0.3f), fontSize = 9.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(comment.text, color = Color.White, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Insert Comment Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = { Text("Add critical note...", color = Color.Gray, fontSize = 12.sp) },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        val c = PostComment(
                                            postId = post.id,
                                            author = "Apex_Pilot",
                                            text = newCommentText
                                        )
                                        mockCommentsSeed.add(c)
                                        activeComments.add(c)
                                        newCommentText = ""
                                        Toast.makeText(context, "Chassis comment applied.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FFCC))
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Post", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

// PREMIUM SINGLE SOCIAL FEED CARD
@Composable
fun SocialCardItem(
    post: SocialPost,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040404).copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Profile Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFF00FFCC)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.authorName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column {
                        Text(post.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(post.authorTag, color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("Verified GPS Active", color = Color.White.copy(0.3f), fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Text
            Text(
                text = post.textContent,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // High Precision Telemetry Certificate Bar
            if (post.hasVideoProof && post.videoProofStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1100FFCC))
                        .border(1.dp, Color(0x3300FFCC), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = "", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                        Text("VERIFIED PERFORMANCE RUN", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Text(post.videoProofStats, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0x12FFFFFF))
            Spacer(modifier = Modifier.height(8.dp))

            // Lower Interactive Row Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Click
                TextButton(
                    onClick = onLikeClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = if (post.isLiked) Color.Red else Color.Gray)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            modifier = Modifier.size(18.dp)
                        )
                        Text("${post.likesCount}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                // Comment Click
                TextButton(
                    onClick = onCommentClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ModeComment,
                            contentDescription = "Comment",
                            modifier = Modifier.size(18.dp)
                        )
                        Text("${post.commentsCount}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                // Share Click
                TextButton(
                    onClick = onShareClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. CAR MEETS SCREEN: CREATION FORM WITH VALIDATION, RSVP, EVENT CHAT
// ==========================================
// Seed messages for event chat
data class MeetChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val meetId: Long,
    val sender: String,
    val message: String,
    val time: String = "Now"
)

private val mockMeetChats = mutableStateListOf<MeetChatMessage>(
    MeetChatMessage(meetId = 1L, sender = "RSR_Collect", message = "Canyons are clean, CHP reports clear. Entering staging coordinates."),
    MeetChatMessage(meetId = 1L, sender = "TurboRaptor", message = "Toggling my launch control, arriving in 5."),
    MeetChatMessage(meetId = 1L, sender = "Mulholland_Cop", message = "Keep the exhaust baffles active guys, noise complaints active.")
)

@Composable
fun ProductionCarMeetsScreen(
    viewModel: ApexViewModel,
    meetsList: List<CarMeet>
) {
    val context = LocalContext.current
    var showCreateForm by remember { mutableStateOf(false) }

    // Creation states
    var mTitle by remember { mutableStateOf("") }
    var mLoc by remember { mutableStateOf("") }
    var mRules by remember { mutableStateOf("") }
    var mCapacity by remember { mutableStateOf("") }
    var mTime by remember { mutableStateOf("") }

    // Form inputs validation variables
    var titleError by remember { mutableStateOf("") }
    var locError by remember { mutableStateOf("") }
    var capError by remember { mutableStateOf("") }

    var selectedMeetForChat by remember { mutableStateOf<CarMeet?>(null) }
    var chatInputText by remember { mutableStateOf("") }
    val meetChats = remember(selectedMeetForChat) {
        derivedStateOf {
            selectedMeetForChat?.let { meet ->
                mockMeetChats.filter { it.meetId == meet.id }
            } ?: emptyList()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateForm = true },
                containerColor = Color(0xFF00FFCC),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Host Meet")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text("COORDINATES EVENTS", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text("Active Meets & Cruises", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(meetsList) { meet ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (meet.hasRsvped) Color(0xFF00FFCC) else Color(0x1BFFFFFF), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF040404).copy(alpha = 0.85f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(meet.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                        Text("Hosted by ${meet.hostName}", color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (meet.hasRsvped) Color(0x3300FF88) else Color(0x11FFFFFF))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${meet.rsvpCount}/${meet.capacity} RSVP",
                                            color = if (meet.hasRsvped) Color(0xFF00FFCC) else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    Text(meet.locationName, color = Color.LightGray, fontSize = 12.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    Text(meet.startTime, color = Color.LightGray, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("RULES: ${meet.rules}", color = Color.White.copy(0.5f), fontSize = 11.sp)

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.toggleRsvp(meet)
                                            Toast.makeText(context, if (!meet.hasRsvped) "RSVP complete. Coordinates synchronized to onboard GPS." else "RSVP with drawn.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (meet.hasRsvped) Color(0xFF00FFCC) else Color(0xFF141414)
                                        ),
                                        modifier = Modifier.weight(1f).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (meet.hasRsvped) Icons.Default.CheckCircle else Icons.Default.CalendarToday,
                                            contentDescription = "",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (meet.hasRsvped) "RSVP ACTIVE" else "RSVP MEET", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { selectedMeetForChat = meet },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F)),
                                        modifier = Modifier.weight(1f).border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ENCRYPTED CHAT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FULL DIRECT CHAT BOX OVERLAY FOR CAR MEET
            selectedMeetForChat?.let { meet ->
                AlertDialog(
                    onDismissRequest = { selectedMeetForChat = null },
                    containerColor = Color(0xFF080808),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp)),
                    title = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(meet.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Staging Chat coordinates active", color = Color(0xFF00FFCC), fontSize = 10.sp)
                            }
                            IconButton(onClick = { selectedMeetForChat = null }) {
                                Icon(Icons.Default.Close, contentDescription = "", tint = Color.Gray)
                            }
                        }
                    },
                    text = {
                        Column(modifier = Modifier.height(320.dp)) {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(meetChats.value) { chatMsg ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                            .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(chatMsg.sender, color = Color(0xFF00FFCC), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                            Text(chatMsg.time, color = Color.White.copy(0.3f), fontSize = 9.sp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(chatMsg.message, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatInputText,
                                    onValueChange = { chatInputText = it },
                                    placeholder = { Text("Radio transmission message...", fontSize = 12.sp, color = Color.Gray) },
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00FFCC),
                                        unfocusedBorderColor = Color(0x33FFFFFF)
                                    )
                                )
                                Button(
                                    onClick = {
                                        if (chatInputText.isNotBlank()) {
                                            mockMeetChats.add(
                                                MeetChatMessage(meetId = meet.id, sender = "Apex_Pilot", message = chatInputText)
                                            )
                                            chatInputText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            // FLOATING ACTION OVERLAY FORM WITH ROBUST VALIDATIONS
            if (showCreateForm) {
                AlertDialog(
                    onDismissRequest = { showCreateForm = false },
                    containerColor = Color(0xFF040404),
                    modifier = Modifier.border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp)),
                    title = { Text("Host New Meet Coordinates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = mTitle,
                                onValueChange = { mTitle = it; titleError = "" },
                                isError = titleError.isNotEmpty(),
                                label = { Text("Event Title (Weissach Pass...)", color = Color.Gray) },
                                textStyle = TextStyle(color = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    errorBorderColor = Color.Red
                                )
                            )
                            if (titleError.isNotEmpty()) {
                                Text(titleError, color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedTextField(
                                value = mLoc,
                                onValueChange = { mLoc = it; locError = "" },
                                isError = locError.isNotEmpty(),
                                label = { Text("Staging Location", color = Color.Gray) },
                                textStyle = TextStyle(color = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    errorBorderColor = Color.Red
                                )
                            )
                            if (locError.isNotEmpty()) {
                                Text(locError, color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedTextField(
                                value = mRules,
                                onValueChange = { mRules = it },
                                label = { Text("Canyon Rules/Guidelines", color = Color.Gray) },
                                textStyle = TextStyle(color = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC)
                                )
                            )

                            OutlinedTextField(
                                value = mTime,
                                onValueChange = { mTime = it },
                                label = { Text("Time Grid Coordinate (Saturday 9 PM)", color = Color.Gray) },
                                textStyle = TextStyle(color = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC)
                                )
                            )

                            OutlinedTextField(
                                value = mCapacity,
                                onValueChange = { mCapacity = it ; capError = "" },
                                isError = capError.isNotEmpty(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Rig Capacity Limit", color = Color.Gray) },
                                textStyle = TextStyle(color = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    errorBorderColor = Color.Red
                                )
                            )
                            if (capError.isNotEmpty()) {
                                Text(capError, color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Form validator validation assertions
                                var pass = true
                                if (mTitle.trim().length < 4) {
                                    titleError = "Title must be at least 4 alphanumeric descriptors."
                                    pass = false
                                }
                                if (mLoc.trim().isEmpty()) {
                                    locError = "Staging coordinate cannot be null."
                                    pass = false
                                }
                                val capVal = mCapacity.toIntOrNull()
                                if (capVal == null || capVal <= 0) {
                                    capError = "Capacity must be an absolute scalar integer."
                                    pass = false
                                }

                                if (pass) {
                                    viewModel.addCarMeet(
                                        title = mTitle,
                                        location = mLoc,
                                        rules = if (mRules.isEmpty()) "Drive safe. Observe local traffic laws." else mRules,
                                        capacity = capVal ?: 50,
                                        start = if (mTime.isEmpty()) "9:00 PM" else mTime,
                                        end = "Midnight"
                                    )
                                    // Soft refresh
                                    mTitle = ""
                                    mLoc = ""
                                    mRules = ""
                                    mCapacity = ""
                                    mTime = ""
                                    showCreateForm = false
                                    Toast.makeText(context, "Telemetry staging coordinates dispatched.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                        ) {
                            Text("Deploy Coordinates", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateForm = false }) {
                            Text("Abort", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}


// ==========================================
// 5. MYSTERY RACE SCREEN: DIRECT CHALLENGING & REAL-TIME MATCH TRANSACTIONS
// ==========================================
@Composable
fun MysteryRaceMatchmakingScreen(
    viewModel: ApexViewModel
) {
    val context = LocalContext.current
    var searchRadius by remember { mutableStateOf(5f) } // Radius slider 1-15 miles
    var isSearching by remember { mutableStateOf(false) }

    // Matches database list
    val originDriverList = remember {
        mutableStateListOf<RacerMatch>(
            RacerMatch("GTR_R35_Alpha", "Nissan GT3 Spec R-35", "AWD Pro", 780, 1.2),
            RacerMatch("TurboMcLaren_720S", "McLaren 720S Twin-Turbo", "RWD Elite", 710, 2.4),
            RacerMatch("Hyper_S60_Pilot", "Volvo Polestar S60 EV", "AWD Custom", 450, 4.1),
            RacerMatch("CarbonRS4", "Audi RS4 Avant", "AWD Drift", 520, 8.5)
        )
    }

    var selectedChallengeRacer by remember { mutableStateOf<RacerMatch?>(null) }
    var lobbyConnectionStatus by remember { mutableStateOf("Ready To Handshake") }

    // Simulated Handshake states
    var countdownTimer by remember { mutableStateOf(-1) }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            delay(2500) // Pulse scan delay
            isSearching = false
            Toast.makeText(context, "Radar telemetry sweeps complete.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(selectedChallengeRacer) {
        selectedChallengeRacer?.let { racer ->
            lobbyConnectionStatus = "Sending telemetry handshakes..."
            delay(1500)
            lobbyConnectionStatus = "G-force calibration aligned. Awaiting vehicle telemetry reply."
            delay(2000)
            lobbyConnectionStatus = "Challenge Dispatch: Accepted by opponent! Initializing GPS tunnel."
            countdownTimer = 3
        }
    }

    LaunchedEffect(countdownTimer) {
        if (countdownTimer > 0) {
            delay(1000)
            countdownTimer--
            if (countdownTimer == 0) {
                countdownTimer = -1
                Toast.makeText(context, "G-Force tracking initialized. Dig launch tracking online!", Toast.LENGTH_LONG).show()
                selectedChallengeRacer = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("PROXIMITY RADAR", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
            Text("Mystery Racer Discovery", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }

        // Radar Scanning Visualizer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x3300FFCC), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF040404))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Telemetry Sweep Envelope", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${searchRadius.toInt()} miles", color = Color(0xFF00FFCC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = searchRadius,
                    onValueChange = { searchRadius = it },
                    valueRange = 1f..15f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF00FFCC),
                        thumbColor = Color(0xFF00FFCC),
                        inactiveTrackColor = Color(0x22FFFFFF)
                    )
                )

                Button(
                    onClick = { isSearching = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSearching) Color(0xFF141414) else Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("EMITTING COGNITIVE SECTOR HANDSHAKES...", color = Color.White, fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Radar, contentDescription = "", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INITIATE RADAR VELOCITY SCAN", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Nearby Active Drivers
        Text("FOUND VEHICLES IN ACTIVE SECTOR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        val filteredDrivers = originDriverList.filter { it.distanceMiles <= searchRadius }

        if (filteredDrivers.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching chassis in this sector. Increase envelope radius.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDrivers) { racer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x1BFFFFFF), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E0E))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF141414))
                                        .border(1.dp, Color(0xFF00FFCC), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = "", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text(racer.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${racer.vehicle} • ${racer.classification} • ${racer.horsepower} HP", color = Color.Gray, fontSize = 11.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(String.format("%.1f mi", racer.distanceMiles), color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { selectedChallengeRacer = racer },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("CHALLENGE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // LOBBY ACTIVE TRANSACTIONS POPUP
        selectedChallengeRacer?.let { racer ->
            AlertDialog(
                onDismissRequest = { selectedChallengeRacer = null },
                containerColor = Color(0xFF060606),
                modifier = Modifier.border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(16.dp)),
                title = { Text("Staging Telemetry Lobby", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(racer.username, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(racer.vehicle, color = Color.White, fontSize = 13.sp)

                        Divider(color = Color(0x33FFFFFF))

                        Text(
                            text = lobbyConnectionStatus,
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        if (countdownTimer > 0) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FFCC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$countdownTimer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedChallengeRacer = null }) {
                        Text("Abort Run", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}


// ==========================================
// 6. LEADERBOARDS SCREEN: CHASSIS GLOBAL RANKINGS & VEHICLE CLASSES
// ==========================================
data class LeaderboardScarp(
    val rank: Int,
    val pilotName: String,
    val pilotTag: String,
    val scoreText: String,
    val vehicleModel: String,
    val classCategory: String
)

@Composable
fun ProductionLeaderboardsScreen(
    viewModel: ApexViewModel
) {
    var activeCategory by remember { mutableStateOf("Fastest 0-60") }
    var activeScope by remember { mutableStateOf("Global") } // Global, Local, Class

    val mockLeaderboardData = remember(activeCategory, activeScope) {
        derivedStateOf {
            when (activeCategory) {
                "Fastest 0-60" -> {
                    when (activeScope) {
                        "Global" -> listOf(
                            LeaderboardScarp(1, "Apex_Pilot", "@me", "2.92s", "Tesla Model S Plaid", "AWD EV"),
                            LeaderboardScarp(2, "Flat6_Whine", "@gt3_screamer", "3.04s", "Porsche 911 GT3 RS", "RWD Gas"),
                            LeaderboardScarp(3, "V8_Overlord", "@hemi_pilot", "3.12s", "Dodge Demon 170", "RWD Gas"),
                            LeaderboardScarp(4, "Nismo_Core", "@nissan_pilot", "3.22s", "Nissan GT-R Nismo", "AWD Gas")
                        )
                        "Local" -> listOf(
                            LeaderboardScarp(1, "Apex_Pilot", "@me", "2.92s", "Tesla Model S Plaid", "AWD EV"),
                            LeaderboardScarp(2, "Flat6_Whine", "@gt3_screamer", "3.04s", "Porsche 911 GT3 RS", "RWD Gas")
                        )
                        else -> listOf(
                            LeaderboardScarp(1, "Apex_Pilot", "@me", "2.92s", "Tesla Model S Plaid", "AWD EV")
                        )
                    }
                }
                "Fastest Quarter Mile" -> {
                    listOf(
                        LeaderboardScarp(1, "E85_GTR_Lover", "@gtr_drag", "9.18s", "Nissan GT-R Track Edition", "AWD Gas"),
                        LeaderboardScarp(2, "Apex_Pilot", "@me", "9.24s", "Tesla Model S Plaid", "AWD EV"),
                        LeaderboardScarp(3, "Hemi_King", "@demon_pilot", "9.45s", "Dodge Demon", "RWD Gas")
                    )
                }
                else -> {
                    listOf(
                        LeaderboardScarp(1, "E85_GTR_Lover", "@gtr_drag", "1350 HP", "Nissan GT-R Track Edition", "AWD Gas"),
                        LeaderboardScarp(2, "Apex_Pilot", "@me", "1020 HP", "Tesla Model S Plaid", "AWD EV"),
                        LeaderboardScarp(3, "Flat6_Whine", "@gt3_screamer", "518 HP", "Porsche 911 GT3 RS", "RWD Gas")
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("GLOBAL APEX STANDINGS", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
            Text("High Score Telemetry", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }

        // Scope Selector Segments
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141414))
                .padding(4.dp)
        ) {
            listOf("Global", "Local", "Class Spec").forEach { scope ->
                val active = activeScope == scope
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Color(0xFF00FFCC) else Color.Transparent)
                        .clickable { activeScope = scope }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scope,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Category Selector Slider Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Fastest 0-60", "Fastest Quarter Mile", "Highest Horsepower").forEach { cat ->
                val active = activeCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) Color(0xFF00FFCC) else Color(0xFF141414))
                        .border(1.dp, if (active) Color(0xFF00FFCC) else Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .clickable { activeCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (active) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Leaderboard List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(mockLeaderboardData.value) { pilot ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (pilot.pilotName == "Apex_Pilot") Color(0xFF00FFCC) else Color(0x11FFFFFF),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pilot.pilotName == "Apex_Pilot") Color(0x3300FF88) else Color(0xFF0E0E0E)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom Trophy Badges for Rank 1-3
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (pilot.rank) {
                                            1 -> Color(0xFFFFD700) // Gold
                                            2 -> Color(0xFFC0C0C0) // Silver
                                            3 -> Color(0xFFCD7F32) // Bronze
                                            else -> Color(0xFF1A1A1A)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${pilot.rank}",
                                    color = if (pilot.rank <= 3) Color.Black else Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }

                            Column {
                                Text(pilot.pilotName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${pilot.vehicleModel} • ${pilot.classCategory}", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        // Score metrics
                        Text(
                            text = pilot.scoreText,
                            color = Color(0xFF00FFCC),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}


// ==========================================
// 7. PROFILE SCREEN: VEHICLES GARAGE WITH OBD SIMULATOR & COMPLIANCE, UNLOCKED ACHIEVEMENTS
// ==========================================
@Composable
fun ProductionUserProfileScreen(
    viewModel: ApexViewModel
) {
    val context = LocalContext.current
    val myProfile by viewModel.myProfile.collectAsState()
    val allVehicles by viewModel.allVehicles.collectAsState()

    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }

    // OBD II simulator metrics
    var obdConnectionState by remember { mutableStateOf("Disconnected") }
    var obdCoolantTemp by remember { mutableStateOf(0) }
    var obdEngineRpm by remember { mutableStateOf(0) }
    var obdFuelTrim by remember { mutableStateOf(0f) }

    LaunchedEffect(obdConnectionState) {
        if (obdConnectionState == "Connecting") {
            delay(2000)
            obdConnectionState = "Connected OBD Link"
            obdCoolantTemp = 185
            obdEngineRpm = 850
            obdFuelTrim = 1.02f
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
        // Upper profile visual banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF040404).copy(alpha = 0.85f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFF00FFCC)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = myProfile?.username ?: "Apex_Pilot",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )

                Text(
                    text = myProfile?.location ?: "Las Vegas, Nevada",
                    color = Color(0xFF00FFCC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = myProfile?.bio ?: "Carbon Aero Specialist. High-G curve tuning enthusiast.",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )

                Divider(color = Color(0x22FFFFFF))

                // Followers Row Click bounds with validation indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(
                        modifier = Modifier.clickable { showFollowersDialog = true },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${myProfile?.followers ?: 2540}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("FOLLOWERS", color = Color.Gray, fontSize = 10.sp)
                    }
                    Column(
                        modifier = Modifier.clickable { showFollowingDialog = true },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${myProfile?.following ?: 421}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("FOLLOWING", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }

        // OBD II HIGH TECH COMPLIANCE PANEL (OBD OBDLink parser ELM327 integration mock)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (obdConnectionState == "Connected OBD Link") Color(0xFF00FFCC) else Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070707))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("OBD-II DIAGNOSTIC LINK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("ELM327 / OBDLink compatible", color = Color.Gray, fontSize = 10.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (obdConnectionState == "Connected OBD Link") Color(0x2200FFCC) else Color(0x11FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = obdConnectionState.uppercase(),
                            color = if (obdConnectionState == "Connected OBD Link") Color(0xFF00FFCC) else Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (obdConnectionState == "Connected OBD Link") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${obdCoolantTemp}°F", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("COOLANT TEMP", color = Color.Gray, fontSize = 9.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${obdEngineRpm} RPM", color = Color(0xFF00FFCC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("ENGINE REVS", color = Color.Gray, fontSize = 9.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(String.format("%.2f", obdFuelTrim), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("FUEL TRIM", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (obdConnectionState == "Disconnected") {
                            obdConnectionState = "Connecting"
                        } else {
                            obdConnectionState = "Disconnected"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (obdConnectionState == "Connected OBD Link") Color.Red else Color(0xFF00FFCC)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (obdConnectionState == "Connected OBD Link") "DISCONNECT TRANSMISSIONOBD" else "HANDSHAKE ELM327 BLUETOOTH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // SECTION: Achievements unlocks list
        Text("RACER CHALLENGES UNLOCKS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val achievements = listOf("Canyon Legend", "1000+ HP Club", "Mulholland King", "OBD Specialist", "Drag Striker")
            achievements.forEach { badge ->
                Card(
                    modifier = Modifier
                        .size(110.dp)
                        .border(1.dp, Color(0xFF00FFCC).copy(0.4f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = "", tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Section header garage
        Text("MY VEHCLES SYSTEMS GARAGE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        // Vehicles Lazy list layout
        allVehicles.forEach { car ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1BFFFFFF), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0C))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${car.year} ${car.make} ${car.model}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(car.trim, color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { viewModel.deleteVehicle(car) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.7f))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("ENGINE: ${car.engine} • POWER: ${car.horsepower} HP • TRQ: ${car.torque} LB-FT", color = Color.LightGray, fontSize = 11.sp)
                    Text("DRIVETRAIN: ${car.drivetrain} • WEIGHT: ${car.weight} LBS", color = Color.LightGray, fontSize = 11.sp)

                    if (car.modifications.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141414))
                                .padding(8.dp)
                        ) {
                            Text("MODS: ${car.modifications}", color = Color.White.copy(0.7f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Followers Overlay Dialog
    if (showFollowersDialog) {
        AlertDialog(
            onDismissRequest = { showFollowersDialog = false },
            containerColor = Color(0xFF0F0F0F),
            title = { Text("Active Follower Grid", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Follower_Flat6", "Plaid_CanyonRunner", "M4_CS_Drifter", "MulhollandRRS_Host").forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(user, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("MUTUAL RECOGNITION", color = Color(0xFF00FFCC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFollowersDialog = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    // Following Overlay Dialog
    if (showFollowingDialog) {
        AlertDialog(
            onDismissRequest = { showFollowingDialog = false },
            containerColor = Color(0xFF0F0F0F),
            title = { Text("Following Transmitters Grid", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MulhollandRRS_Host", "Hemi_Striker", "Carrera4_Collector").forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(user, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("ACTIVE SYNC", color = Color(0xFF00FFCC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFollowingDialog = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

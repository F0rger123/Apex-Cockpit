package com.example

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SocialPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Popup states
    var activeCommentingPost by remember { mutableStateOf<SocialPost?>(null) }
    var activeCommentText by remember { mutableStateOf("") }
    
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var postInputText by remember { mutableStateOf("") }
    var telemetryInputValue by remember { mutableStateOf("") }

    // Connect and stream comments context when commenting target changes
    LaunchedEffect(activeCommentingPost) {
        activeCommentingPost?.let { post ->
            viewModel.fetchCommentsTrail(post.id)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF010101)) // Pure midnight black
    ) {
        // Decorative background radar sweep grids
        Canvas(modifier = Modifier.fillMaxSize()) {
            val columns = 8
            val rows = 12
            val colWidth = size.width / columns
            val rowHeight = size.height / rows
            for (i in 0..columns) {
                drawLine(
                    color = Color(0x0500FFCC),
                    start = androidx.compose.ui.geometry.Offset(i * colWidth, 0f),
                    end = androidx.compose.ui.geometry.Offset(i * colWidth, size.height),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
            for (i in 0..rows) {
                drawLine(
                    color = Color(0x0500FFFF),
                    start = androidx.compose.ui.geometry.Offset(0f, i * rowHeight),
                    end = androidx.compose.ui.geometry.Offset(size.width, i * rowHeight),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
        }

        // Body rendering based on states
        Crossfade(
            targetState = uiState,
            animationSpec = spring(),
            label = "FeedStateTransition"
        ) { state ->
            when (state) {
                is FeedUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00FFCC),
                                modifier = Modifier.size(42.dp).testTag("feed_progress")
                            )
                            Text(
                                text = "CALIBRATING TELEMETRY CORRECTION...",
                                color = Color(0xFF00FFCC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }
                is FeedUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error Logo",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.errorMessage,
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.loadInitialPage() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("feed_retry_button")
                        ) {
                            Text("Retry Connection", color = Color.White)
                        }
                    }
                }
                is FeedUiState.Success -> {
                    if (state.posts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Empty Logo",
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No live broadcasts registered yet.",
                                color = Color.Gray,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Be the first to share GPS speed measurements or track slips!",
                                color = Color.DarkGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showCreatePostDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Broadcast Telemetry", color = Color.White)
                            }
                        }
                    } else {
                        // Vertical TikTok Swipe style Layout
                        val pagerState = rememberPagerState(
                            initialPage = 0,
                            initialPageOffsetFraction = 0f,
                            pageCount = { state.posts.size }
                        )

                        // Detect infinite scroll threshold (when user is close to the end of pages list)
                        val shouldLoadMore = remember {
                            derivedStateOf {
                                val totalItems = state.posts.size
                                val currentPage = pagerState.currentPage
                                currentPage >= totalItems - 2 && totalItems > 0
                            }
                        }

                        LaunchedEffect(shouldLoadMore.value) {
                            if (shouldLoadMore.value) {
                                viewModel.loadNextPage()
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            VerticalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("feed_lazy_column") // Enforce test tag compatibility for linter verification
                            ) { page ->
                                val post = state.posts[page]
                                TikTokFeedItem(
                                    post = post,
                                    onLike = { viewModel.likeSocialPost(post) },
                                    onComment = { activeCommentingPost = post },
                                    onShare = {
                                        val textToShare = "🏁 Telemetry check by ${post.authorName} (${post.authorTag}):\n\"${post.textContent}\"\n" +
                                                if (post.hasVideoProof) "📈 verified telemetry metric: ${post.videoProofStats}" else ""
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "APEX Telemetry")
                                            putExtra(Intent.EXTRA_TEXT, textToShare)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Track Run"))
                                    }
                                )
                            }

                            // Transparent overlay header at top of view
                            TransparentHeaderOverlay(
                                onAddClick = { showCreatePostDialog = true }
                            )

                            // Immersive loading pagination bar at bottom
                            if (state.isPaginating) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 24.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xE60D0D0D))
                                        .border(0.5.dp, Color(0xFF00FFCC).copy(0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF00FFCC),
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                        Text(
                                            text = "TUNING FEED SYNCING...",
                                            color = Color(0xFF00FFCC),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FLOATING ACTION SHEET - CREATE DISPATCH POST
        if (showCreatePostDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePostDialog = false },
                containerColor = Color(0xFF0C0C0C),
                modifier = Modifier
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .testTag("create_post_dialog"),
                title = {
                    Text(
                        text = "Broadcast Coordinate Activity",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = postInputText,
                            onValueChange = { postInputText = it },
                            placeholder = { Text("What street coordinates or tuning notes are you tracking?", color = Color.Gray, fontSize = 13.sp) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("post_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC),
                                unfocusedBorderColor = Color(0x22FFFFFF)
                            )
                        )
                        OutlinedTextField(
                            value = telemetryInputValue,
                            onValueChange = { telemetryInputValue = it },
                            placeholder = { Text("Performance validation stats e.g. 100-200: 4.8s", color = Color.Gray, fontSize = 13.sp) },
                            textStyle = TextStyle(color = Color(0xFF00FFCC), fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("telemetry_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF00FFCC),
                                focusedBorderColor = Color(0xFF00FFCC),
                                unfocusedBorderColor = Color(0x18FFFFFF)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (postInputText.isNotBlank()) {
                                viewModel.submitTelemetryPost(postInputText, telemetryInputValue)
                                postInputText = ""
                                telemetryInputValue = ""
                                showCreatePostDialog = false
                                Toast.makeText(context, "Telemetry coordinates sent successfully", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier.testTag("post_submit_button")
                    ) {
                        Text("Broadcast", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreatePostDialog = false },
                        modifier = Modifier.testTag("post_cancel_button")
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // FULL INTERACTIVE COMMENTS HANDSHAKE WINDOW OVERLAY
        activeCommentingPost?.let { post ->
            val commentsList by viewModel.postComments.collectAsState()
            val isCommentsLoading by viewModel.isCommentsLoading.collectAsState()

            AlertDialog(
                onDismissRequest = { activeCommentingPost = null },
                containerColor = Color(0xFF080808),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
                    .testTag("comments_dialog"),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Chassis Handshakes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Secure peer comments active", color = Color(0xFF00FFCC), fontSize = 10.sp)
                        }
                        IconButton(onClick = { activeCommentingPost = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.height(340.dp)) {
                        Divider(color = Color(0x11FFFFFF))
                        Spacer(modifier = Modifier.height(10.dp))

                        if (isCommentsLoading) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(28.dp))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("comments_lazy_column"),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (commentsList.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No track comments yet. Send a calibration note!",
                                                color = Color.DarkGray,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    items(commentsList) { item ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = item.author,
                                                        color = Color(0xFF00FFCC),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = "Active Pilot",
                                                        color = Color.White.copy(0.2f),
                                                        fontSize = 9.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.text,
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color(0x11FFFFFF))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Insert comment input text bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = activeCommentText,
                                onValueChange = { activeCommentText = it },
                                placeholder = { Text("Radio comment payload...", color = Color.Gray, fontSize = 12.sp) },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_comment_input"),
                                maxLines = 1,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color(0x22FFFFFF)
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (activeCommentText.isNotBlank()) {
                                        viewModel.postChassisComment(post.id, activeCommentText, post)
                                        activeCommentText = ""
                                        Toast.makeText(context, "Note added to chassis", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FFCC))
                                    .testTag("comment_submit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Post Comment",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun TikTokFeedItem(
    post: SocialPost,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    var isSaved by remember { mutableStateOf(false) }
    val details = remember(post.authorName) { getPostVehicleDetails(post.authorName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("post_card_${post.id}")
            .background(Color.Black)
    ) {
        // Full screen custom media visual representation (gorgeous canvas track or vector blueprint)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw premium digital radar sweeps / futuristic track curves
                drawCircle(
                    color = Color(0x1100FFFF),
                    radius = w * 0.42f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = Color(0x0600FF88),
                    radius = w * 0.22f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // High-performance vehicle telemetry coordinates lines (Formula 1 styling)
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.45f)
                    cubicTo(w * 0.25f, h * 0.35f, w * 0.5f, h * 0.65f, w * 0.75f, h * 0.4f)
                    lineTo(w, h * 0.55f)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF00FFFF).copy(alpha = 0.22f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Speed lines
                for (i in 0..15) {
                    val x = w * 0.08f * i
                    drawLine(
                        color = Color(0x1F00FF88),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x + 30f, h),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }

            // Central Telemetry Vector Badge
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .border(1.dp, Color(0x1100FF88), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                DynamicBackgroundTelemetryGauge(tickerText = post.authorName)
            }
        }

        // Ambient Edge Glow Panels (Top, bottom and side vignettes)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xBB000000),
                            Color.Transparent,
                            Color(0xAA000000),
                            Color(0xFA000000)
                        ),
                        startY = 0f
                    )
                )
        )

        // Dynamic edge glow highlight color strips (Left indicator)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF00FF88), Color(0xFF00FFFF))
                    )
                )
        )

        // Details container overlay (bottom left aligned) - Styled precisely like Porsche cockpit metrics
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Profile Info card (glassmorphism look)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0A0A).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                // Avatar Frame with Neon Glow
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF00FF88), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.authorName.take(1).uppercase(),
                        color = Color(0xFF00FF88),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        // Verification Badges (Requested)
                        if (details.isVerified) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00FF88).copy(0.12f))
                                    .border(0.5.dp, Color(0xFF00FF88), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "VERIFIED PILOT",
                                    color = Color(0xFF00FF88),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    Text(
                        text = post.authorTag,
                        color = Color(0xFF00FFFF).copy(0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Text commentary content with modern display look
            Text(
                text = post.textContent,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Vehicle Specs Glass Card overlay (Requested Specs: Vehicle, HP, Quarter mile)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A).copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE CHASSIS",
                            color = Color(0xFFA0A0A0),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00FFFF).copy(0.12f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CALIBRATION OK",
                                color = Color(0xFF00FFFF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Vehicle Identity Label
                    Text(
                        text = details.model.uppercase(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    // Specs readouts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("POWER", color = Color(0xFFA0A0A0), fontSize = 8.sp)
                            Text("${details.hp} HP", color = Color(0xFF00FF88), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text("QUARTER MILE", color = Color(0xFFA0A0A0), fontSize = 8.sp)
                            Text(details.quarterMile, color = Color(0xFF00FFFF), fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Floating TikTok control buttons (right overlay side panel)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp)
                .width(58.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. LIKE BUTTON Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, if (post.isLiked) Color(0xFFFF4444) else Color(0xFF1A1A1A), CircleShape)
                        .clickable(onClick = onLike)
                        .testTag("like_btn_${post.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Icon Button",
                        tint = if (post.isLiked) Color(0xFFFF4444) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${post.likesCount}",
                    color = if (post.isLiked) Color(0xFFFF4444) else Color(0xFFA0A0A0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 2. COMMENT BUTTON Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, Color(0xFF1A1A1A), CircleShape)
                        .clickable(onClick = onComment)
                        .testTag("comment_btn_${post.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ModeComment,
                        contentDescription = "Comment Icon Button",
                        tint = Color(0xFF00FFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${post.commentsCount}",
                    color = Color(0xFFA0A0A0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 3. SAVE BUTTON Action (Requested saves functionality)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, if (isSaved) Color(0xFF00FF88) else Color(0xFF1A1A1A), CircleShape)
                        .clickable { isSaved = !isSaved },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Icon Button",
                        tint = if (isSaved) Color(0xFF00FF88) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSaved) "SAVED" else "SAVE",
                    color = if (isSaved) Color(0xFF00FF88) else Color(0xFFA0A0A0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
            }

            // 4. SHARE BUTTON Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, Color(0xFF1A1A1A), CircleShape)
                        .clickable(onClick = onShare)
                        .testTag("share_btn_${post.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Icon Button",
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SHARE",
                    color = Color(0xFFA0A0A0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

data class PostVehicleDetails(
    val model: String,
    val hp: Int,
    val quarterMile: String,
    val isVerified: Boolean
)

fun getPostVehicleDetails(authorName: String): PostVehicleDetails {
    return when (authorName) {
        "Apex_Pilot" -> PostVehicleDetails("Tesla Model S Plaid", 1020, "9.21s @ 151 MPH", true)
        "Flat6_Whine" -> PostVehicleDetails("Porsche 911 GT3 RS", 518, "10.42s @ 132 MPH", true)
        "E85_GTR_Lover" -> PostVehicleDetails("Nissan GT-R Nismo", 800, "9.62s @ 142 MPH", true)
        "RSR_Collective" -> PostVehicleDetails("Porsche 911 Carrera RSR", 450, "10.95s @ 126 MPH", true)
        else -> PostVehicleDetails("Mustang GT V8", 412, "12.21s @ 115 MPH", false)
    }
}

@Composable
fun DynamicBackgroundTelemetryGauge(tickerText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "GaugeTransitions")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GaugeRotationAngle"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GaugePulseFactor"
    )

    Box(
        modifier = Modifier
            .size(260.dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = center
            val radius = size.width / 2.3f

            // Outer gauge track ring
            drawCircle(
                color = Color(0x0600FFCC),
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
            )

            // Concentric grids
            drawCircle(
                color = Color(0x0300FFFF),
                radius = radius * 0.72f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0x0200FFCC),
                radius = radius * 0.44f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Sweep radar line
            val angleRad = Math.toRadians(sweepAngle.toDouble())
            val lineEnd = androidx.compose.ui.geometry.Offset(
                x = (centerOffset.x + radius * Math.cos(angleRad)).toFloat(),
                y = (centerOffset.y + radius * Math.sin(angleRad)).toFloat()
            )
            drawLine(
                color = Color(0x1A00FFCC),
                start = centerOffset,
                end = lineEnd,
                strokeWidth = 1.2.dp.toPx()
            )

            // Holographic perimeter arc highlights
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x0500FF88), Color(0x0C00FFCC))
                ),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            )

            // Ticking calibration ticks
            drawArc(
                color = Color(0x1F00FFCC),
                startAngle = 220f,
                sweepAngle = 60f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Inner digital readout values core
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GPS LOCK 10HZ",
                color = Color(0xFF00FFCC).copy(0.45f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "TELEMETRY RUN",
                color = Color.White.copy(0.12f),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${tickerText.take(6).uppercase()}_RX",
                color = Color(0xFF00FFCC).copy(0.4f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransparentHeaderOverlay(
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FFCC))
                )
                Text(
                    text = "APEX RADAR LIVE",
                    color = Color(0xFF00FFCC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }
            Text(
                text = "Telemetry Dispatch",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF00FFCC))
                .testTag("header_publish_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Dispatch Broadcast",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

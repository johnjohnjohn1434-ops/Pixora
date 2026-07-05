package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.ui.theme.PixoraDarkBg
import com.example.ui.theme.PixoraPrimary
import com.example.ui.theme.PixoraTertiary
import com.example.ui.theme.PixoraSecondary
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.text.TextStyle
import com.example.ui.viewmodel.AiTaskState
import com.example.ui.viewmodel.PixoraViewModel
import kotlinx.coroutines.delay

@Composable
fun MainAppContainer(viewModel: PixoraViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    MyApplicationTheme(darkTheme = isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                bottomBar = {
                    if (currentScreen != "auth" && currentScreen != "register") {
                        PixoraBottomNavigation(
                            currentScreen = currentScreen,
                            onNavigate = { screen ->
                                if (screen == "profile") {
                                    viewModel.navigateTo("profile", userParam = currentUser?.username)
                                } else {
                                    viewModel.navigateTo(screen)
                                }
                            }
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "ScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            "auth" -> AuthScreen(viewModel)
                            "register" -> RegisterScreen(viewModel)
                            "feed" -> MainFeedScreen(viewModel)
                            "reels" -> ReelsScreen(viewModel)
                            "search" -> SearchScreen(viewModel)
                            "create_post" -> CreatePostScreen(viewModel)
                            "chat_list" -> ChatListScreen(viewModel)
                            "chat_active" -> ChatActiveScreen(viewModel)
                            "profile" -> UserProfileScreen(viewModel)
                            "post_detail" -> PostDetailScreen(viewModel)
                            "admin_panel" -> AdminDashboardScreen(viewModel)
                            "settings" -> SettingsScreen(viewModel)
                            else -> MainFeedScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// --- SHARED UI COMPONENTS ---

@Composable
fun PixoraBottomNavigation(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .testTag("bottom_nav_bar"),
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("feed", Icons.Filled.Home, Icons.Outlined.Home),
            Triple("search", Icons.Filled.Search, Icons.Outlined.Search),
            Triple("create_post", Icons.Filled.AddBox, Icons.Outlined.AddBox),
            Triple("reels", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
            Triple("chat_list", Icons.Filled.Mail, Icons.Outlined.Mail),
            Triple("profile", Icons.Filled.Person, Icons.Outlined.Person)
        )

        items.forEach { (screen, filledIcon, outlinedIcon) ->
            val isActive = currentScreen == screen || (screen == "profile" && currentScreen == "profile")
            NavigationBarItem(
                selected = isActive,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = if (isActive) filledIcon else outlinedIcon,
                        contentDescription = screen,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = screen.replace("_", " ").replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
fun PixoraHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions?.invoke(this)
            }
        }
    }
}

// --- AUTHENTICATION SCREEN ---

@Composable
fun AuthScreen(viewModel: PixoraViewModel) {
    var usernameInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetLogins = listOf("alice", "sarah_travels", "chef_marcus", "neon_dreams", "admin")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        // Pixora Logo with radiant circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PixoraPrimary.copy(alpha = 0.4f), Color.Transparent),
                            center = center,
                            radius = size.width / 1.5f
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Pixora Logo",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pixora",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Share Moments. Build Community.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = usernameInput,
            onValueChange = {
                usernameInput = it.lowercase().trim().replace(" ", "")
                errorMessage = null
            },
            label = { Text("Enter Username") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input"),
            shape = RoundedCornerShape(12.dp)
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (usernameInput.isBlank()) {
                    errorMessage = "Please enter a username to proceed"
                } else {
                    viewModel.loginUser(usernameInput) {
                        viewModel.navigateTo("feed")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("login_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { viewModel.navigateTo("register") }) {
            Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Quick Demo Accounts:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            presetLogins.forEach { preset ->
                AssistChip(
                    onClick = {
                        viewModel.loginUser(preset) {
                            viewModel.navigateTo("feed")
                        }
                    },
                    label = { Text(preset) },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = { content() }
    )
}

// --- SIGN UP REGISTER SCREEN ---

@Composable
fun RegisterScreen(viewModel: PixoraViewModel) {
    var usernameInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it.lowercase().trim().replace(" ", "") },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = displayNameInput,
            onValueChange = { displayNameInput = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bioInput,
            onValueChange = { bioInput = it },
            label = { Text("Bio (Favorite hobby, camera model, etc.)") },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (usernameInput.isBlank() || displayNameInput.isBlank()) {
                    errorMessage = "Username and Full Name cannot be empty"
                } else {
                    viewModel.registerUser(
                        username = usernameInput,
                        name = displayNameInput,
                        bio = bioInput,
                        onSuccess = { viewModel.navigateTo("feed") },
                        onError = { error -> errorMessage = error }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Up & Explore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { viewModel.navigateTo("auth") }) {
            Text("Already have an account? Sign In", color = MaterialTheme.colorScheme.primary)
        }
    }
}

// --- MAIN FEED SCREEN (INSTAGRAM STYLE) ---

@Composable
fun MainFeedScreen(viewModel: PixoraViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val sortedPosts by viewModel.sortedPosts.collectAsStateWithLifecycle()
    val feedTab by viewModel.feedTab.collectAsStateWithLifecycle()

    var activeStoryView by remember { mutableStateOf<StoryEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar
        PixoraHeader(
            title = "Pixora",
            actions = {
                // Settings Icon
                IconButton(onClick = { viewModel.navigateTo("settings") }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Admin Panel (if admin or superadmin)
                if (currentUser?.role == "admin" || currentUser?.role == "superadmin") {
                    IconButton(onClick = { viewModel.navigateTo("admin_panel") }) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Admin Dashboard",
                            tint = PixoraTertiary
                        )
                    }
                }
            }
        )

        // Stories Row & Filters
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                // Stories horizontal row
                StoriesRow(stories = stories, onStoryClick = { activeStoryView = it })
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                // Sorting Tabs: RECOMMENDED, LATEST, TRENDING
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val tabs = listOf("RECOMMENDED", "LATEST", "TRENDING")
                    tabs.forEach { tab ->
                        val isSelected = feedTab == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.setFeedTab(tab) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(2.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }

            if (sortedPosts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No posts available yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Be the first to share a moment!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(sortedPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLike = { viewModel.likePost(post.id) },
                        onSave = { viewModel.savePost(post.id) },
                        onCommentClick = {
                            viewModel.navigateTo("post_detail", postParam = post.id)
                        },
                        onUserClick = {
                            viewModel.navigateTo("profile", userParam = post.authorId)
                        }
                    )
                }
            }
        }
    }

    // Story View Overlay
    if (activeStoryView != null) {
        StoryViewOverlay(
            story = activeStoryView!!,
            onClose = { activeStoryView = null }
        )
    }
}

@Composable
fun StoriesRow(
    stories: List<StoryEntity>,
    onStoryClick: (StoryEntity) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(stories) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(story) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(PixoraPrimary, PixoraTertiary, PixoraPrimary)
                            ),
                            shape = CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = story.authorAvatar,
                        contentDescription = story.authorName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.authorId,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(64.dp),
                    textAlign = Alignment.CenterHorizontally.let { TextAlign.Center }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: PostEntity,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onCommentClick: () -> Unit,
    onUserClick: () -> Unit
) {
    var showHeartBurst by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Post Header (Author info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onUserClick() }
                ) {
                    AsyncImage(
                        model = post.authorAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Verification check
                            if (post.authorId == "sarah_travels" || post.authorId == "chef_marcus" || post.authorId == "neon_dreams") {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified",
                                    tint = PixoraPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        if (post.location != null) {
                            Text(
                                text = post.location,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                }
            }

            // Post Media Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .combinedClickable(
                        onDoubleClick = {
                            if (!post.isLiked) {
                                onLike()
                            }
                            showHeartBurst = true
                        },
                        onClick = onCommentClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = "Post Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Double tap pink heart splash
                if (showHeartBurst) {
                    LaunchedEffect(Unit) {
                        delay(600)
                        showHeartBurst = false
                    }
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = PixoraTertiary,
                        modifier = Modifier
                            .size(100.dp)
                            .animateContentSize()
                    )
                }

                // Sensitive Content warning overlay
                if (post.isSensitive) {
                    var revealContent by remember { mutableStateOf(false) }
                    if (!revealContent) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.9f))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = PixoraTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Sensitive Content Warning",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "This content has been flagged by Pixora safety systems.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { revealContent = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PixoraPrimary)
                                ) {
                                    Text("Reveal Image")
                                }
                            }
                        }
                    }
                }
            }

            // Actions (Like, Comment, Save)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(onClick = onLike, modifier = Modifier.testTag("like_button")) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) PixoraTertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onCommentClick) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment"
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share"
                        )
                    }
                }
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (post.isSaved) PixoraPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Likes and Caption details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${post.likesCount} likes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${post.authorId} ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = post.caption,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (post.hashtags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = post.hashtags,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (post.commentsCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "View all ${post.commentsCount} comments",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onCommentClick() }
                            .padding(vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// --- STORY VIEWER OVERLAY ---

@Composable
fun StoryViewOverlay(
    story: StoryEntity,
    onClose: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(key1 = story) {
        progress = 0f
        val duration = 5000 // 5 seconds
        val interval = 50
        val steps = duration / interval
        for (i in 1..steps) {
            delay(interval.toLong())
            progress = i.toFloat() / steps
        }
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = story.mediaUrl,
            contentDescription = "Story",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Story Top Overlay Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = PixoraPrimary,
                trackColor = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = story.authorAvatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = story.authorName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// --- REELS (VERTICAL SHORT VIDEO SWIPE) SCREEN ---

@Composable
fun ReelsScreen(viewModel: PixoraViewModel) {
    val reels by viewModel.reels.collectAsStateWithLifecycle()
    var activeIndex by remember { mutableStateOf(0) }

    if (reels.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reels available yet!")
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixoraDarkBg)
    ) {
        val currentReel = reels[activeIndex]

        // Custom Simulated Full Screen Video (displays high-res cover + video progress line)
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = currentReel.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Linear Progress simulation (moving video bar)
            var videoProgress by remember(activeIndex) { mutableStateOf(0f) }
            LaunchedEffect(activeIndex) {
                while (true) {
                    delay(100)
                    videoProgress += 0.01f
                    if (videoProgress >= 1f) videoProgress = 0f
                }
            }

            // Dark bottom vignette
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Dynamic Progress Indicator
            LinearProgressIndicator(
                progress = videoProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter),
                color = PixoraPrimary,
                trackColor = Color.Transparent
            )

            // Right side overlay tools (Like, Comment, Share)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Like Button Widget
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { viewModel.likeReel(currentReel.id) },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (currentReel.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like Reel",
                            tint = if (currentReel.isLiked) PixoraTertiary else Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentReel.likesCount}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comment Button Widget
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment Reel",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentReel.commentsCount}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Share Button Widget
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Reel",
                        tint = Color.White
                    )
                }
            }

            // Bottom text / Creator metadata
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 40.dp, start = 16.dp, end = 80.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.navigateTo("profile", userParam = currentReel.authorId)
                    }
                ) {
                    AsyncImage(
                        model = currentReel.authorAvatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = currentReel.authorId,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable { viewModel.followUser(currentReel.authorId) }
                    ) {
                        Text(
                            text = "Follow",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentReel.caption,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Next / Prev Reel navigation trigger (Swipe mock buttons for quick desktop/emulator test)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (activeIndex > 0) activeIndex-- },
                    enabled = activeIndex > 0,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev", tint = Color.White)
                }
                IconButton(
                    onClick = { if (activeIndex < reels.size - 1) activeIndex++ },
                    enabled = activeIndex < reels.size - 1,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next", tint = Color.White)
                }
            }
        }
    }
}

// --- UNIFIED SEARCH SCREEN ---

@Composable
fun SearchScreen(viewModel: PixoraViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()

    val filteredUsers = remember(searchQuery, allUsers) {
        if (searchQuery.isBlank()) emptyList()
        else allUsers.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                    it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users, hashtags, keywords...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        )

        if (searchQuery.isNotBlank()) {
            Text(
                text = "Search Results",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredUsers) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo("profile", userParam = user.username) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = user.username, fontWeight = FontWeight.Bold)
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = PixoraPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(text = user.displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Divider()
                }
            }
        } else {
            // Discovery Grid (Insta search grid style)
            Text(
                text = "Explore Moments",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(posts) { post ->
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                viewModel.navigateTo("post_detail", postParam = post.id)
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// --- CREATE POST / DISCOVER AI CAPTION TOOLS SCREEN ---

@Composable
fun CreatePostScreen(viewModel: PixoraViewModel) {
    var caption by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var isSensitive by remember { mutableStateOf(false) }

    var aiPromptInput by remember { mutableStateOf("") }
    val aiCaptionState by viewModel.aiCaptionState.collectAsStateWithLifecycle()
    val aiHashtagsState by viewModel.aiHashtagsState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Preset gorgeous backdrop suggestions to pick
    val presetImages = listOf(
        "https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?w=800" to "Beach Sunrise",
        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800" to "Yosemite Valley",
        "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800" to "Magic Woods",
        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800" to "Mountain Mist"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Create New Post", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        // Image URL input
        OutlinedTextField(
            value = mediaUrl,
            onValueChange = { mediaUrl = it },
            label = { Text("Image URL") },
            placeholder = { Text("Paste custom image URL or select preset below") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Preset row
        Text("Or pick a premium preset landscape:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presetImages) { (url, label) ->
                AssistChip(
                    onClick = { mediaUrl = url },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text Caption
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Caption") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic AI Generation Suite (Gemini integration!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PixoraPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pixora AI Writer (Gemini Flash)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = aiPromptInput,
                    onValueChange = { aiPromptInput = it },
                    placeholder = { Text("Describe image (e.g. cute puppy in park, starry sky Seattle)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.generateAiCaption(aiPromptInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = PixoraPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("AI Caption", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.suggestAiHashtags(aiPromptInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = PixoraSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("AI Hashtags", fontSize = 12.sp)
                    }
                }

                // AI Response Displays
                when (val result = aiCaptionState) {
                    is AiTaskState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    is AiTaskState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = result.result,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            TextButton(onClick = { caption = result.result }) {
                                Text("Use Caption")
                            }
                            TextButton(onClick = { viewModel.clearAiCaption() }) {
                                Text("Clear")
                            }
                        }
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(result.error, color = Color.Red, fontSize = 11.sp)
                    }
                    else -> {}
                }

                when (val hashResult = aiHashtagsState) {
                    is AiTaskState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    is AiTaskState.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = hashResult.result,
                            fontSize = 13.sp,
                            color = PixoraPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            TextButton(onClick = { hashtags = hashResult.result }) {
                                Text("Use Hashtags")
                            }
                            TextButton(onClick = { viewModel.clearAiHashtags() }) {
                                Text("Clear")
                            }
                        }
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(hashResult.error, color = Color.Red, fontSize = 11.sp)
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location & Custom hashtags
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = hashtags,
            onValueChange = { hashtags = it },
            label = { Text("Hashtags (Optional)") },
            placeholder = { Text("#sunset #landscape") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isSensitive, onCheckedChange = { isSensitive = it })
            Text("Flag as sensitive content (will display warning overlay)", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (mediaUrl.isBlank()) {
                    mediaUrl = "https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?w=800"
                }
                viewModel.submitPost(
                    caption = caption,
                    mediaUrl = mediaUrl,
                    location = location.ifBlank { null },
                    hashtags = hashtags,
                    isSensitive = isSensitive
                ) {
                    viewModel.navigateTo("feed")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_post_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Publish Post", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- DIRECT CHAT LIST SCREEN ---

@Composable
fun ChatListScreen(viewModel: PixoraViewModel) {
    val chatPartners by viewModel.chatPartners.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        PixoraHeader(title = "Direct Messages")

        if (chatPartners.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(48.dp))
                    Text("No active conversations. Start one from a profile!")
                }
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chatPartners) { partnerId ->
                val partnerUser = allUsers.find { it.username == partnerId } ?: UserEntity(partnerId, partnerId, "", "", "")
                val latestMessage by viewModel.getLatestMessageForPartnerFlow(partnerId).collectAsStateWithLifecycle(initialValue = null)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setActiveChatPartner(partnerId)
                            viewModel.navigateTo("chat_active")
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = partnerUser.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        // Online simulated dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color.Green, CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = partnerUser.displayName, fontWeight = FontWeight.Bold)
                        Text(
                            text = latestMessage?.text ?: "No messages",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (latestMessage?.isSeen == false && latestMessage?.senderId == partnerId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (latestMessage?.isSeen == false && latestMessage?.senderId == partnerId) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                Divider()
            }
        }
    }
}

// --- ACTIVE CHAT DIALOGUE BUBBLES SCREEN ---

@Composable
fun ChatActiveScreen(viewModel: PixoraViewModel) {
    val partnerId by viewModel.activeChatPartner.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val partnerUser = allUsers.find { it.username == partnerId } ?: UserEntity(partnerId ?: "", partnerId ?: "", "", "", "")

    Column(modifier = Modifier.fillMaxSize()) {
        PixoraHeader(
            title = partnerUser.displayName,
            onBack = {
                viewModel.setActiveChatPartner(null)
                viewModel.navigateBack()
            }
        )

        // Chat bubble list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            reverseLayout = false
        ) {
            items(activeMessages) { msg ->
                val isMe = msg.senderId == currentUser?.username
                val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(bubbleColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .widthIn(max = 260.dp)
                    ) {
                        Text(text = msg.text, color = textColor, fontSize = 14.sp)
                    }
                }
            }
        }

        // Typing Status simulation
        var isTypingSim by remember { mutableStateOf(false) }
        LaunchedEffect(textInput) {
            if (textInput.isNotEmpty()) {
                isTypingSim = true
                delay(1200)
                isTypingSim = false
            }
        }

        if (isTypingSim) {
            Text(
                text = "${partnerUser.displayName} is typing...",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // Bottom text field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Send a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendChatMessage(partnerId ?: "", textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

// --- CREATOR USER PROFILE SCREEN ---

@Composable
fun UserProfileScreen(viewModel: PixoraViewModel) {
    val usernameParam by viewModel.navigationUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val user = allUsers.find { it.username == usernameParam } ?: currentUser

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading profile...")
        }
        return
    }

    val userPosts = posts.filter { it.authorId == user.username && !it.isArchived }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Posts, 1 = Tagged

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PixoraHeader(
            title = user.username,
            onBack = { viewModel.navigateBack() }
        )

        // Banner and Avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PixoraPrimary, PixoraTertiary)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = (-30).dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${user.postsCount}", fontWeight = FontWeight.Bold)
                    Text(text = "Posts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${user.followersCount}", fontWeight = FontWeight.Bold)
                    Text(text = "Followers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${user.followingCount}", fontWeight = FontWeight.Bold)
                    Text(text = "Following", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Bio section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-16).dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified Creator",
                        tint = PixoraPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = user.bio, fontSize = 13.sp)
            if (user.website.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.website,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action button (Follow, Direct Message, Edit, Block)
            Row(modifier = Modifier.fillMaxWidth()) {
                if (user.username == currentUser?.username) {
                    Button(
                        onClick = { viewModel.navigateTo("settings") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val isFollow = user.isFollowing
                    Button(
                        onClick = { viewModel.followUser(user.username) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollow) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isFollow) "Following" else "Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.setActiveChatPartner(user.username)
                            viewModel.navigateTo("chat_active")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PixoraSecondary)
                    ) {
                        Text("Message", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Block button
                    IconButton(
                        onClick = { viewModel.blockUser(user.username) },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (user.isBlocked) Icons.Default.Block else Icons.Outlined.Block,
                            contentDescription = "Block User",
                            tint = if (user.isBlocked) PixoraTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Story highlights placeholders (Zen aesthetics)
        Text(
            text = "Story Highlights",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val highlights = listOf("Kyoto ⛩️", "Crust 🥖", "Zens 🌿")
            highlights.forEach { h ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = h, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Tabs (Posts, Tagged)
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.GridView, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Posts")
                }
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tagged")
                }
            }
        }

        // Grid contents
        if (selectedTab == 0) {
            if (userPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No posts shared yet.")
                }
            } else {
                val chunkedPosts = userPosts.chunked(3)
                Column(modifier = Modifier.padding(4.dp)) {
                    chunkedPosts.forEach { rowPosts ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowPosts.forEach { post ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                ) {
                                    AsyncImage(
                                        model = post.mediaUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                viewModel.navigateTo("post_detail", postParam = post.id)
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            // Fill blank weights
                            val blanks = 3 - rowPosts.size
                            if (blanks > 0) {
                                repeat(blanks) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No tagged posts found.")
            }
        }
    }
}

// --- POST DETAIL SCREEN (COMMENTS SYSTEM) ---

@Composable
fun PostDetailScreen(viewModel: PixoraViewModel) {
    val postIdParam by viewModel.navigationPostId.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val comments by viewModel.getCommentsForPostFlow(postIdParam ?: -1).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val post = posts.find { it.id == postIdParam }
    var commentText by remember { mutableStateOf("") }

    if (post == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading post details...")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PixoraHeader(title = "Post Details", onBack = { viewModel.navigateBack() })

        LazyColumn(modifier = Modifier.weight(1f)) {
            // Post card
            item {
                PostCard(
                    post = post,
                    onLike = { viewModel.likePost(post.id) },
                    onSave = { viewModel.savePost(post.id) },
                    onCommentClick = {},
                    onUserClick = { viewModel.navigateTo("profile", userParam = post.authorId) }
                )
                Divider()
                Text(
                    text = "Comments (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No comments yet. Start the conversation!", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                items(comments) { comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = comment.authorAvatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = comment.authorId, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = comment.text, fontSize = 13.sp)
                            }
                        }

                        // Moderation comment deletion
                        if (currentUser?.username == comment.authorId || currentUser?.role == "admin" || currentUser?.role == "superadmin") {
                            IconButton(onClick = { viewModel.deleteComment(comment) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Comment input box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Add a comment...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (commentText.isNotBlank()) {
                        viewModel.submitComment(post.id, commentText)
                        commentText = ""
                    }
                },
                shape = CircleShape
            ) {
                Text("Post")
            }
        }
    }
}

// --- ADMIN CONTROL DASHBOARD SCREEN ---

@Composable
fun AdminDashboardScreen(viewModel: PixoraViewModel) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Reports, 1 = Users, 2 = Posts

    Column(modifier = Modifier.fillMaxSize()) {
        PixoraHeader(title = "Moderation Panel", onBack = { viewModel.navigateBack() })

        // Statistics Metric Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Users", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${allUsers.size}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Reports Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${reports.filter { it.status == "PENDING" }.size}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Active Posts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${posts.size}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) { Text("Reports", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) { Text("Users", modifier = Modifier.padding(12.dp)) }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) { Text("Posts", modifier = Modifier.padding(12.dp)) }
        }

        when (activeTab) {
            0 -> {
                // Reports Tab
                if (reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No safety reports submitted yet.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(reports) { r ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Type: ${r.reportedType}", fontWeight = FontWeight.Bold, color = PixoraPrimary)
                                        Text("Status: ${r.status}", fontWeight = FontWeight.Bold, color = PixoraTertiary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Reason: ${r.reason}", fontSize = 13.sp)
                                    Text("Details: ${r.description}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Reporter: ${r.reporterId}", fontSize = 11.sp, fontStyle = FontStyle.Italic)

                                    if (r.evidenceUrl != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AsyncImage(
                                            model = r.evidenceUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { viewModel.updateReportStatus(r.id, "RESOLVED") },
                                            colors = ButtonDefaults.buttonColors(containerColor = PixoraPrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Resolve", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.updateReportStatus(r.id, "REJECTED") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Reject", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Users Tab
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(allUsers) { u ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = u.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = u.username, fontWeight = FontWeight.Bold)
                                    Text(text = "Role: ${u.role}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Row {
                                // Verified Badge toggle
                                IconButton(onClick = { viewModel.verifyUserStatus(u.username, !u.isVerified) }) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = if (u.isVerified) PixoraPrimary else Color.LightGray
                                    )
                                }
                                // Ban toggle
                                IconButton(onClick = { viewModel.terminateUserAccount(u.username) }) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Ban Account",
                                        tint = if (u.role == "banned") PixoraTertiary else Color.Gray
                                    )
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
            2 -> {
                // Post Moderation Tab
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(posts) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                AsyncImage(
                                    model = p.mediaUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = p.caption, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                    Text(text = "Author: ${p.authorId}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Button(
                                onClick = { viewModel.deletePost(p) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Delete", fontSize = 11.sp)
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

// --- SETTINGS (THEME, PRIVACY, KEYWORD FILTERS) SCREEN ---

@Composable
fun SettingsScreen(viewModel: PixoraViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isPrivate by viewModel.isPrivateAccount.collectAsStateWithLifecycle()
    val keywordFilter by viewModel.keywordFilter.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var displayNameInput by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var bioInput by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "") }
    var websiteInput by remember(currentUser) { mutableStateOf(currentUser?.website ?: "") }
    var photoUrlInput by remember(currentUser) { mutableStateOf(currentUser?.photoUrl ?: "") }

    var localKeyword by remember(keywordFilter) { mutableStateOf(keywordFilter) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        PixoraHeader(title = "Settings", onBack = { viewModel.navigateBack() })

        Spacer(modifier = Modifier.height(16.dp))

        // Profile details edits
        Text("Edit Personal Profile Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = photoUrlInput,
            onValueChange = { photoUrlInput = it },
            label = { Text("Profile Photo URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = displayNameInput,
            onValueChange = { displayNameInput = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = bioInput,
            onValueChange = { bioInput = it },
            label = { Text("Profile Bio") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = websiteInput,
            onValueChange = { websiteInput = it },
            label = { Text("Website Portfolio") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                viewModel.updateProfile(displayNameInput, bioInput, websiteInput, photoUrlInput)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Profile Edits", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Styling / Theme Switch
        Text("Appearance & Environment", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dark Mode Theme", fontWeight = FontWeight.Medium)
            Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode() })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Switches
        Text("Account Privacy Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Private Account (approves followers)", fontWeight = FontWeight.Medium)
            Switch(checked = isPrivate, onCheckedChange = { viewModel.setPrivateAccount(it) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Filters / Comment Hide
        Text("Safety & Content Moderation Filters", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Auto-censor comments containing any of these keywords:", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = localKeyword,
            onValueChange = {
                localKeyword = it
                viewModel.setKeywordFilter(it)
            },
            placeholder = { Text("spam, offer, promo, tokens...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Future coming soon modules
        Text("Upcoming features (Coming Soon)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val upcoming = listOf("Live Streaming", "Voice Rooms", "Events", "Marketplace")
            upcoming.forEach { label ->
                AssistChip(onClick = {}, label = { Text("$label 🚀") })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.logoutUser()
                viewModel.navigateTo("auth")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Log Out Session", fontWeight = FontWeight.Bold)
        }
    }
}

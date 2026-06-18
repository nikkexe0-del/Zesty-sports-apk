package com.zestyysports.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val fontName = GoogleFont("Inter")

val InterFontFamily = FontFamily(
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Black)
)


data class M3UItem(
    val id: String,
    val name: String,
    val logo: String,
    val group: String,
    val url: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("zesty_prefs", Context.MODE_PRIVATE) }
            val isDarkTheme = remember { mutableStateOf(sharedPrefs.getBoolean("is_dark_theme", true)) }

            val colorScheme = if (isDarkTheme.value) {
                darkColorScheme(
                    background = Color(0xFF0A0A0A), // Tailwind bg-neutral-950
                    surface = Color(0xFF171717), // Tailwind dark:bg-neutral-900 / 0xFF171717
                    onBackground = Color(0xFFFFFFFF), // primary white text
                    onSurface = Color(0xFFE5E5E5) // secondary warm gray text
                )
            } else {
                lightColorScheme(
                    background = Color(0xFFF5F5F5), // Tailwind bg-neutral-100
                    surface = Color.White, // Tailwind bg-white
                    onBackground = Color(0xFF171717), // primary dark neutral-900 text
                    onSurface = Color(0xFF525252) // secondary dark neutral-600 text
                )
            }

            val sfLetterSpacing = (-0.5).sp
            val typography = Typography(
                displayLarge = androidx.compose.material3.Typography().displayLarge.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                displayMedium = androidx.compose.material3.Typography().displayMedium.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                displaySmall = androidx.compose.material3.Typography().displaySmall.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                headlineLarge = androidx.compose.material3.Typography().headlineLarge.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                headlineMedium = androidx.compose.material3.Typography().headlineMedium.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                titleLarge = androidx.compose.material3.Typography().titleLarge.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                titleMedium = androidx.compose.material3.Typography().titleMedium.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                titleSmall = androidx.compose.material3.Typography().titleSmall.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                bodySmall = androidx.compose.material3.Typography().bodySmall.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                labelLarge = androidx.compose.material3.Typography().labelLarge.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                labelMedium = androidx.compose.material3.Typography().labelMedium.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing),
                labelSmall = androidx.compose.material3.Typography().labelSmall.copy(fontFamily = InterFontFamily, letterSpacing = sfLetterSpacing)
            )

            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalTextStyle provides androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ZestyyApp(
                            isDarkTheme = isDarkTheme.value,
                            onThemeToggle = { 
                                val newValue = !isDarkTheme.value
                                isDarkTheme.value = newValue 
                                sharedPrefs.edit().putBoolean("is_dark_theme", newValue).apply()
                            },
                            onOrientationChange = { landscape ->
                                requestedOrientation = if (landscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZestyyApp(isDarkTheme: Boolean, onThemeToggle: () -> Unit, onOrientationChange: (Boolean) -> Unit) {
    var selectedChannel by remember { mutableStateOf<M3UItem?>(null) }
    var channels by remember { mutableStateOf<List<M3UItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/nikkexe0-del/alexplaylist/refs/heads/main/premium.m3u")
                .build()
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                val parsed = parseM3U(body)
                withContext(Dispatchers.Main) {
                    channels = parsed
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    if (selectedChannel != null) {
        VideoPlayerScreen(
            channel = selectedChannel!!,
            allChannels = channels,
            onOrientationChange = onOrientationChange,
            onBack = { selectedChannel = null },
            onChannelSelect = { selectedChannel = it }
        )
    } else {
        onOrientationChange(false) // Make sure portrait
        MainScreen(
            channels = channels,
            isLoading = isLoading,
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            onPlay = { selectedChannel = it }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    channels: List<M3UItem>,
    isLoading: Boolean,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onPlay: (M3UItem) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("zesty_prefs", Context.MODE_PRIVATE) }
    
    var currentTab by remember { mutableStateOf(sharedPrefs.getString("active_tab", "home") ?: "home") }
    var searchQuery by remember { mutableStateOf("") }
    var activeGroup by remember { mutableStateOf("All") }
    var favorites by remember { 
        mutableStateOf(sharedPrefs.getStringSet("favorites", emptySet()) ?: emptySet()) 
    }
    
    var displayedItemCount by remember { mutableStateOf(20) }
    var showPopup by remember { mutableStateOf(true) }

    // Save favorites and tab state whenever they change
    LaunchedEffect(favorites) {
        sharedPrefs.edit().putStringSet("favorites", favorites).apply()
    }
    LaunchedEffect(currentTab) {
        sharedPrefs.edit().putString("active_tab", currentTab).apply()
    }

    val groups = remember(channels) {
        listOf("All") + channels.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredChannels = remember(channels, currentTab, searchQuery, activeGroup, favorites) {
        displayedItemCount = 20
        when (currentTab) {
            "favorites" -> channels.filter { favorites.contains(it.id) }
            "search" -> if (searchQuery.isNotBlank()) channels.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            } else emptyList()
            else -> if (activeGroup == "All") channels else channels.filter { it.group == activeGroup }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Red,
                        selectedTextColor = Color.Red,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "favorites",
                    onClick = { currentTab = "favorites" },
                    icon = { Icon(if (currentTab == "favorites") Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "Fav") },
                    label = { Text("Favorites", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Red,
                        selectedTextColor = Color.Red,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "search",
                    onClick = { currentTab = "search" },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Red,
                        selectedTextColor = Color.Red,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Hero Section
                    HeroSection()
                }

                if (currentTab == "search") {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Search channels...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDarkTheme) Color.White else Color(0xFF171717),
                                unfocusedTextColor = if (isDarkTheme) Color.White else Color(0xFF171717),
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                focusedBorderColor = Color(0xFFDC2626), // focus:border-red-500
                                unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E5E5),
                                focusedContainerColor = if (isDarkTheme) Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (isDarkTheme) Color(0xFF121212) else Color.White,
                            )
                        )
                    }
                }

                if (currentTab == "home") {
                    item {
                        val groupListState = rememberLazyListState()
                        LazyRow(
                            state = groupListState,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            flingBehavior = rememberSnapFlingBehavior(groupListState)
                        ) {
                            items(groups) { group ->
                                val isSelected = activeGroup == group
                                val borderStroke = if (isSelected) {
                                    null
                                } else {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E5E5)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .bounceClick { activeGroup = group }
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isSelected) Color(0xFFDC2626) else (if (isDarkTheme) Color(0xFF171717) else Color.White)
                                        )
                                        .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(50)) else Modifier)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = group.uppercase(),
                                        color = if (isSelected) Color.White else (if (isDarkTheme) Color(0xFFA3A3A3) else Color(0xFF525252)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Red)
                    }
                }
            } else {
                if (currentTab == "search" && searchQuery.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SEARCH RESULTS", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Red.copy(alpha=0.1f)))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("${filteredChannels.size} FOUND", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (filteredChannels.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                                .border(2.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("NO MATCHING CHANNELS.", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        }
                    }
                } else {
                    val columns = 2
                    val chunkedList = filteredChannels.take(displayedItemCount).chunked(columns)
                    items(chunkedList, key = { rowItems -> rowItems.joinToString { it.id } }) { rowItems ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                            rowItems.forEach { channel ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ChannelMiniCard(
                                        channel = channel,
                                        isFavorite = favorites.contains(channel.id),
                                        onToggleFavorite = { id ->
                                            favorites = if (favorites.contains(id)) favorites - id else favorites + id
                                        },
                                        onPlay = onPlay
                                    )
                                }
                            }
                            if (rowItems.size < columns) {
                                Box(modifier = Modifier.weight(1f)) // spacer
                            }
                        }
                    }
                    
                    if (filteredChannels.size > displayedItemCount) {
                        item {
                            Column(Modifier.padding(horizontal = 12.dp)) {
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { displayedItemCount += 20 },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("SHOW MORE", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Footer()
            }
        }

        // Floating Info warning popup absolute at top center
        AnimatedVisibility(
            visible = showPopup,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF171717) else Color.White
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.widthIn(max = 450.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E5E5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Icon
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF59E0B).copy(alpha = 0.12f), CircleShape)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    
                    // Middle Text with styled link
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "If streams fail, visit ",
                            fontSize = 10.sp,
                            color = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF525252),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "nikkitv.vercel.app",
                            fontSize = 10.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://nikkitv.vercel.app")))
                            }
                        )
                    }
                    
                    // Close button
                    IconButton(
                        onClick = { showPopup = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = if (isDarkTheme) Color(0xFF737373) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Floating global theme switcher pill at top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFF171717).copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .bounceClick { onThemeToggle() }
                    .border(
                        1.dp,
                        if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color(0xFF171717).copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isDarkTheme) "LIGHT MODE" else "DARK MODE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun HeroSection() {
    val context = LocalContext.current
    val animSetting = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulseHero")
    val pulseAlpha by animSetting.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF171717), Color.Black)))
    ) {
        // Overlay gradient matching the web UI
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=0.9f), Color.Black.copy(alpha=0.4f), Color.Transparent))))
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha=0.8f), Color.Transparent, Color.Transparent))))
        
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(Color.Transparent).border(1.dp, Color(0xFFFF3B30).copy(alpha=0.5f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF3B30).copy(alpha = pulseAlpha), CircleShape))
                        Text("LIVE NOW", color = Color(0xFFFF3B30), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(modifier = Modifier.background(Color.White.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Text("ULTRA HD", color = Color.White.copy(alpha=0.8f), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Box(modifier = Modifier.background(Color(0xFF4ADE80).copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Text("₹0 COST", color = Color(0xFF4ADE80), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "zestyysports",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Worldwide channels in HD, Ad-free, 4K — for free.\nAccess premium channels instantly.", 
                color = Color.LightGray, 
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Telegram Gradient Button
                Box(
                    modifier = Modifier
                        .bounceClick { 
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9"))
                            context.startActivity(i)
                        }
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp), spotColor = Color(0xFF3B82F6), ambientColor = Color(0xFF3B82F6))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF38BDF8))))
                        .padding(horizontal=24.dp, vertical=12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription=null, modifier=Modifier.size(16.dp), tint=Color(0xFF0A0A0A))
                        Spacer(Modifier.width(8.dp))
                        Text("TELEGRAM", fontSize=11.sp, fontWeight=FontWeight.Black, color=Color(0xFF0A0A0A), letterSpacing = 2.sp)
                    }
                }
                
                // Instagram Gradient Button
                Box(
                    modifier = Modifier
                        .bounceClick { 
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/nikkk.exe"))
                            context.startActivity(i)
                        }
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp), spotColor = Color(0xFF34D399), ambientColor = Color(0xFF34D399))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF34D399), Color(0xFF06B6D4))))
                        .padding(horizontal=24.dp, vertical=12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription=null, modifier=Modifier.size(16.dp), tint=Color(0xFF0A0A0A))
                        Spacer(Modifier.width(8.dp))
                        Text("@NIKKK.EXE", fontSize=11.sp, fontWeight=FontWeight.Black, color=Color(0xFF0A0A0A), letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Footer() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.zestyy_logo),
            contentDescription = "Zestyysports Logo",
            modifier = Modifier.height(32.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "WORLDWIDE CHANNELS IN HD, AD-FREE, 4K — FOR FREE.", 
            color = Color.LightGray, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            // Telegram Gradient Button
            Box(
                modifier = Modifier
                    .bounceClick { 
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9")))
                    }
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp), spotColor = Color(0xFF3B82F6), ambientColor = Color(0xFF3B82F6))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF38BDF8))))
                    .padding(horizontal=24.dp, vertical=12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription=null, modifier=Modifier.size(16.dp), tint=Color(0xFF0A0A0A))
                    Spacer(Modifier.width(8.dp))
                    Text("TELEGRAM", fontSize=11.sp, fontWeight=FontWeight.Black, color=Color(0xFF0A0A0A), letterSpacing = 2.sp)
                }
            }
            
            // Instagram Gradient Button
            Box(
                modifier = Modifier
                    .bounceClick { 
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/nikkk.exe")))
                    }
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp), spotColor = Color(0xFF34D399), ambientColor = Color(0xFF34D399))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF34D399), Color(0xFF06B6D4))))
                    .padding(horizontal=24.dp, vertical=12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription=null, modifier=Modifier.size(16.dp), tint=Color(0xFF0A0A0A))
                    Spacer(Modifier.width(8.dp))
                    Text("@NIKKK.EXE", fontSize=11.sp, fontWeight=FontWeight.Black, color=Color(0xFF0A0A0A), letterSpacing = 2.sp)
                }
            }
            
            // ZestyyFlix Button
            Box(
                modifier = Modifier
                    .bounceClick { 
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zestyyflix.vercel.app")))
                    }
                    .border(1.dp, Color.White.copy(alpha=0.1f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF171717))
                    .padding(horizontal=24.dp, vertical=12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription=null, modifier=Modifier.size(16.dp), tint=Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("MORE FROM ZESTYY", fontSize=11.sp, fontWeight=FontWeight.Black, color=Color.White, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
fun ChannelMiniCard(channel: M3UItem, isFavorite: Boolean, onToggleFavorite: (String) -> Unit, onPlay: (M3UItem) -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0A0A0A)
    
    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .bounceClick { onPlay(channel) },
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF121212) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFE5E5E5)
        )
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF0A0A0A) else Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(channel.logo)
                            .crossfade(300)
                            .build(),
                        contentDescription = channel.name,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    )
                } else {
                    Text(
                        text = channel.name,
                        color = if (isDark) Color(0xFF737373) else Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite Icon
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onToggleFavorite(channel.id) }
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = if(isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Fav",
                        tint = if(isFavorite) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }

                // Live Badge
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulseCard")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(750, easing = androidx.compose.animation.core.LinearEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "pulseAlphaCard"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xFFDC2626).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFFEF4444).copy(alpha = pulseAlpha), CircleShape))
                        Text(text = "LIVE", color = Color(0xFFEF4444), fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name, 
                    color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF171717), 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0A84FF).copy(alpha=0.12f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF0A84FF).copy(alpha=0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("AD-FREE", color = Color(0xFF0A84FF), fontSize = 6.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
            
            if (channel.group.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = channel.group.uppercase(), 
                    color = if (isDark) Color(0xFF737373) else Color(0xFF9E9E9E), 
                    fontSize = 8.sp, 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VideoPlayerScreen(
    channel: M3UItem, 
    allChannels: List<M3UItem>,
    onOrientationChange: (Boolean) -> Unit, 
    onBack: () -> Unit,
    onChannelSelect: (M3UItem) -> Unit
) {
    val context = LocalContext.current
    var showStatsForNerds by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var showJoinPopup by remember { mutableStateOf(true) }

    val bandwidthMeter = remember { androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.Builder(context).build() }
    val exoPlayer = remember {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000, // minBufferMs
                60000, // maxBufferMs
                2500,  // bufferForPlaybackMs
                5000   // bufferForPlaybackAfterRebufferMs 
            )
            .setBackBuffer(0, false)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(channel) {
        showStatsForNerds = false
        showJoinPopup = true
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
        val mediaItem = MediaItem.fromUri(channel.url)
            
        val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(mediaItem)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            onOrientationChange(false)
        }
    }

    BackHandler {
        onBack()
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var showControls by remember { mutableStateOf(false) }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (isLandscape) Color.Black else MaterialTheme.colorScheme.background)
            .then(if (isLandscape) Modifier else Modifier.statusBarsPadding())
    ) {
        val playerModifier = if (isLandscape) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
        }
        Box(
            modifier = playerModifier
        ) {
            AndroidView(
                factory = {
                    val viewStyle = android.view.LayoutInflater.from(context).inflate(R.layout.exo_texture_view, null, false) as androidx.media3.ui.PlayerView
                    viewStyle.apply {
                        player = exoPlayer
                        useController = false
                        isClickable = false
                        isFocusable = false
                        setOnTouchListener { _, _ -> true } // Consume touch so ExoPlayer won't pause on tap
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Transparent overlay to catch ALL clicks and prevent ExoPlayer native clicks
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { 
                        showControls = !showControls 
                    }
            )

            if (isBuffering) {
                var pulse by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    while(true) {
                        pulse = !pulse
                        delay(500)
                    }
                }
                val pulseAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (pulse) 1f else 0.4f,
                    animationSpec = androidx.compose.animation.core.tween(500),
                    label = "pulseAlpha"
                )
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha=0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.zestyy_logo),
                        contentDescription = "Buffering",
                        modifier = Modifier.height(48.dp),
                        alpha = pulseAlpha
                    )
                }
            } else {
                // Watermark
                Box(modifier = Modifier.matchParentSize().padding(16.dp).then(if (isLandscape) Modifier.statusBarsPadding() else Modifier), contentAlignment = Alignment.TopEnd) {
                     Image(
                         painter = painterResource(id = R.drawable.zestyy_logo),
                         contentDescription = "Watermark",
                         modifier = Modifier.height(32.dp),
                         alpha = 0.6f
                     )
                }
                
                if (showStatsForNerds) {
                    var currentPosition by remember { mutableStateOf(0L) }
                    var currentBuffer by remember { mutableStateOf(0L) }
                    var networkBwKbps by remember { mutableStateOf(0L) }
                    LaunchedEffect(Unit) {
                        while(true) {
                            currentPosition = exoPlayer.currentPosition
                            currentBuffer = exoPlayer.bufferedPosition
                            networkBwKbps = bandwidthMeter.bitrateEstimate / 8192L // estimate as KB/s
                            delay(1000)
                        }
                    }
                    val videoFormat = exoPlayer.videoFormat
                    val width = videoFormat?.width ?: 1920
                    val height = videoFormat?.height ?: 1080
                    val resolution = if (width > 0) "${width}x${height}" else "1920x1080"
                    val isHD = if (width >= 1280) " (HD)" else ""
                    val knownBitrate = videoFormat?.bitrate?.takeIf { it > 0 }?.let { it / 1000 } ?: 0
                    val displayBitrateKbps = if (knownBitrate > 0) knownBitrate.toLong() else networkBwKbps
                    val dataConsumedMB = (displayBitrateKbps * (currentPosition / 1000f)) / 8192f
                    
                    Box(modifier = Modifier.padding(16.dp).statusBarsPadding().align(Alignment.TopStart).background(Color.Black.copy(alpha=0.8f)).border(1.dp, Color.White.copy(alpha=0.2f)).padding(16.dp).widthIn(min = 320.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Stats for nerds", color=Color.White, fontSize=12.sp, fontWeight=FontWeight.Bold)
                                IconButton(onClick = { showStatsForNerds = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Close Stats", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Video ID / sCPN", color=Color.White.copy(alpha=0.7f), fontSize=10.sp)
                                Text("hx1-2k0 / zesty", color=Color.White, fontSize=10.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Resolution", color=Color.White.copy(alpha=0.7f), fontSize=10.sp)
                                Text("$resolution$isHD", color=Color.White, fontSize=10.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Data Consumed", color=Color.White.copy(alpha=0.7f), fontSize=10.sp)
                                Text("${String.format("%.1f", dataConsumedMB)} MB", color=Color.White, fontSize=10.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Network Activity", color=Color.White.copy(alpha=0.7f), fontSize=10.sp)
                                Text("$networkBwKbps KB/s", color=Color.White, fontSize=10.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Buffer Health", color=Color.White.copy(alpha=0.7f), fontSize=10.sp)
                                val bufferSecs = currentBuffer / 1000
                                Text("${bufferSecs} s", color=Color.White, fontSize=10.sp)
                            }
                        }
                    }
                }
            }

            // Custom Controller Overlay
            if (showControls) {
                var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
                val activity = context as? android.app.Activity
                var isMuted by remember { mutableStateOf(exoPlayer.volume == 0f) }
                var streamDuration by remember { mutableStateOf(0L) }
                
                LaunchedEffect(Unit) {
                    while(true) {
                         streamDuration = exoPlayer.currentPosition
                         delay(1000)
                    }
                }
                
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha=0.4f))
                ) {
                    // Top Left: Back & Channel Name
                    Row(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).then(if (isLandscape) Modifier.statusBarsPadding() else Modifier), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = channel.name, 
                            color = Color.White, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Bottom Controls
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha=0.8f))))) {
                        Column {
                            // Red line progress bar logic
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha=0.2f))) {
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFFF0000))) // Live stream implies full buffer
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).then(if (isLandscape) Modifier.navigationBarsPadding() else Modifier), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Play/Pause
                                    IconButton(
                                        onClick = { 
                                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                            isPlaying = !isPlaying
                                        }, 
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    // Volume Mute
                                    IconButton(
                                        onClick = { 
                                            isMuted = !isMuted
                                            exoPlayer.volume = if (isMuted) 0f else 1f
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = "Mute Toggle", tint = Color.White)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    // Duration / Live in red
                                    val seconds = (streamDuration / 1000) % 60
                                    val minutes = (streamDuration / (1000 * 60)) % 60
                                    val hours = (streamDuration / (1000 * 60 * 60))
                                    val timeString = if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds) else String.format("%02d:%02d", minutes, seconds)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF3B30), CircleShape))
                                        Spacer(Modifier.width(6.dp))
                                        Text(timeString, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.width(8.dp))
                                        Text("LIVE", color = Color.White.copy(alpha=0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showStatsForNerds = !showStatsForNerds }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Info, contentDescription = "Stats", tint = Color.White)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = { /* Settings Placeholder */ }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = { onOrientationChange(!isLandscape) }, modifier = Modifier.size(36.dp)) {
                                        Icon(if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (!isLandscape) {
            // Channel Info Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var isFavorite by remember { mutableStateOf(context.getSharedPreferences("zesty_prefs", Context.MODE_PRIVATE).getStringSet("zesty_favs", setOf())?.contains(channel.id) == true) }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channel.name.uppercase(), 
                                color = MaterialTheme.colorScheme.onBackground, 
                                fontSize = 20.sp, 
                                fontWeight = FontWeight.Black, 
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.background(Color(0xFF0A84FF).copy(alpha=0.15f), RoundedCornerShape(50)).padding(horizontal=8.dp, vertical=4.dp)) {
                                    Text("AD-FREE", color = Color(0xFF0A84FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.background(Color(0xFFFF3B30).copy(alpha=0.15f), RoundedCornerShape(50)).padding(horizontal=8.dp, vertical=4.dp)) {
                                    Text("LIVE", color = Color(0xFFFF3B30), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { 
                                    val prefs = context.getSharedPreferences("zesty_prefs", Context.MODE_PRIVATE)
                                    val favs = prefs.getStringSet("zesty_favs", setOf())?.toMutableSet() ?: mutableSetOf()
                                    if (favs.contains(channel.id)) {
                                        favs.remove(channel.id)
                                        isFavorite = false
                                    } else {
                                        favs.add(channel.id)
                                        isFavorite = true
                                    }
                                    prefs.edit().putStringSet("zesty_favs", favs).apply()
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Color(0xFFFF3B30) else Color.Gray)
                                }
                            }
                        }

                    Spacer(Modifier.height(24.dp))
                    Text("MORE CHANNELS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    
                    // Horizontal list
                    val lazyListState = rememberLazyListState()
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        flingBehavior = rememberSnapFlingBehavior(lazyListState)
                    ) {
                        items(allChannels.take(20), key = { it.id }) { ch ->
                            val isPlaying = ch.id == channel.id
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(if (isPlaying) 2.dp else 1.dp, if (isPlaying) Color(0xFFFF3B30) else Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .bounceClick { onChannelSelect(ch) }
                            ) {
                                Column {
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                                        if (ch.logo.isNotEmpty()) {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                    .data(ch.logo)
                                                    .crossfade(300)
                                                    .build(),
                                                contentDescription = ch.name,
                                                contentScale = ContentScale.Inside,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        } else {
                                            Text(ch.name.take(3).uppercase(), color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(6.dp), contentAlignment = Alignment.Center) {
                                        Text(ch.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    // Player Footer
                    Footer()
                    } // End of inner Column
                } // End of item
            } // End of outer LazyColumn
        } // End of if
    } // End of outer Column
    if (showJoinPopup && !isLandscape) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showJoinPopup = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha=0.1f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(id = R.drawable.zestyy_logo), contentDescription = null, modifier = Modifier.height(32.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("JOIN TELEGRAM", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Join our official Telegram group for updates, support, and to request channels. Also, visit ZestyyFlix for movies!", color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    
                    // Bento style
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color(0xFF0A84FF).copy(alpha=0.15f)).bounceClick{ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9"))) }.padding(16.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Send, contentDescription=null, tint=Color(0xFF0A84FF), modifier=Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("OPEN TELEGRAM", fontSize=12.sp, fontWeight=FontWeight.Bold, color=Color(0xFF0A84FF))
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color(0xFF0A84FF).copy(alpha=0.15f)).bounceClick{ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zestyyflix.vercel.app/"))) }.padding(16.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Movie, contentDescription=null, tint=Color(0xFF0A84FF), modifier=Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("VISIT ZESTYYFLIX", fontSize=12.sp, fontWeight=FontWeight.Bold, color=Color(0xFF0A84FF))
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showJoinPopup = false }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(50)) {
                        Text("CLOSE & WATCH STREAM", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
} // End of VideoPlayerScreen

fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounceClickScale"
    )
    val currentOnClick by androidx.compose.runtime.rememberUpdatedState(onClick)

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    currentOnClick()
                }
            )
        }
}

fun parseM3U(content: String): List<M3UItem> {
    val items = mutableListOf<M3UItem>()
    val lines = content.split("\n")
    var currentName = ""
    var currentLogo = ""
    var currentGroup = ""

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("#EXTINF:")) {
            val groupMatch = Regex("group-title=\"([^\"]+)\"").find(trimmed)
            if (groupMatch != null) currentGroup = groupMatch.groupValues[1]

            val logoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(trimmed)
            if (logoMatch != null) currentLogo = logoMatch.groupValues[1]

            val parts = trimmed.split(",")
            if (parts.size > 1) {
                currentName = parts[1].trim()
            }
        } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
            items.add(M3UItem(id = java.util.UUID.randomUUID().toString(), name = currentName, logo = currentLogo, group = currentGroup, url = trimmed))
            currentName = ""
            currentLogo = ""
            currentGroup = ""
        }
    }
    return items
}

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
                    background = Color(0xFF0A0A0A),
                    surface = Color(0xFF141414),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    background = Color(0xFFF5F5F5),
                    surface = Color.White,
                    onBackground = Color(0xFF171717),
                    onSurface = Color(0xFF171717)
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
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
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        }
                    )
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Theme Toggle positioned top right
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = onThemeToggle,
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (isDarkTheme) "LIGHT MODE" else "DARK MODE",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Hero Section
            HeroSection()

            if (currentTab == "search") {
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
                        focusedBorderColor = Color.Red,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            }

            if (currentTab == "home") {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups) { group ->
                        val isSelected = activeGroup == group
                        Button(
                            onClick = { activeGroup = group },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color.Red else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(group.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Red)
                }
            } else {
                if (currentTab == "search" && searchQuery.isNotBlank()) {
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

                if (filteredChannels.isEmpty()) {
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
                } else {
                    // Non-scrollable Grid with pagination
                    Column(Modifier.padding(horizontal = 12.dp)) {
                        val columns = 2
                        val chunkedList = filteredChannels.take(displayedItemCount).chunked(columns)
                        chunkedList.forEach { rowItems ->
                            Row(Modifier.fillMaxWidth()) {
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

            Spacer(modifier = Modifier.height(24.dp))
            Footer()
        }
    }
}

@Composable
fun HeroSection() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF111111), Color.Black)))
    ) {
        // Overlay gradient
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha=0.8f)))))
        
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(Color.Red.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Text("LIVE NOW", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.background(Color.White.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Text("ULTRA HD", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.background(Color.Green.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Text("₹0 COST", color = Color.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("zestyysports", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Worldwide channels in HD, Ad-free, 4K — for free. Access premium channels instantly.", color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { 
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9"))
                        context.startActivity(i)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal=16.dp, vertical=8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription=null, modifier=Modifier.size(14.dp), tint=Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("TELEGRAM", fontSize=10.sp, fontWeight=FontWeight.Black, color=Color.White)
                }
                Button(
                    onClick = { 
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/nikkk.exe"))
                        context.startActivity(i)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal=16.dp, vertical=8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription=null, modifier=Modifier.size(14.dp), tint=Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("@NIKKK.EXE", fontSize=10.sp, fontWeight=FontWeight.Black, color=Color.White)
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surface))
        Spacer(Modifier.height(24.dp))
        Text("ZESTY", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(
            "Worldwide channels in HD, Ad-free, 4K — for free.", 
            color = Color.Gray, 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Bold, 
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9"))) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal=12.dp, vertical=6.dp)
            ) {
                Text("TELEGRAM", fontSize=9.sp, fontWeight=FontWeight.Black, color=Color.White)
            }
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zestyyflix.vercel.app"))) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal=12.dp, vertical=6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha=0.2f))
            ) {
                Text("MORE FROM ZESTYY", fontSize=9.sp, fontWeight=FontWeight.Black)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ChannelMiniCard(channel: M3UItem, isFavorite: Boolean, onToggleFavorite: (String) -> Unit, onPlay: (M3UItem) -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { onPlay(channel) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } else {
                    Text(
                        text = channel.name.take(3).uppercase(),
                        color = Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }

                // Favorite Icon
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable { onToggleFavorite(channel.id) }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if(isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Fav",
                        tint = if(isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Live Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Red, RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = "LIVE", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.background(Color.Blue.copy(alpha=0.1f), RoundedCornerShape(2.dp)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                    Text("Ad-Free", color = Color.Blue, fontSize = 6.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            
            if (channel.group.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.group.uppercase(), 
                    color = Color.Gray, 
                    fontSize = 8.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(
    channel: M3UItem, 
    allChannels: List<M3UItem>,
    onOrientationChange: (Boolean) -> Unit, 
    onBack: () -> Unit,
    onChannelSelect: (M3UItem) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("zesty_prefs", Context.MODE_PRIVATE) }
    
    var bufferCountdown by remember { mutableStateOf(15) }
    var isUnlocked by remember { mutableStateOf(sharedPrefs.getBoolean("zesty_unlocked", false)) }
    var previewSeconds by remember { mutableStateOf(120) }
    var unlockCode by remember { mutableStateOf("") }
    var showStatsForNerds by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(channel) {
        bufferCountdown = 15
        previewSeconds = 120
        showStatsForNerds = false
        val mediaItem = MediaItem.fromUri(channel.url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(bufferCountdown) {
        if (bufferCountdown > 0) {
            delay(1000)
            bufferCountdown--
        }
    }

    LaunchedEffect(isUnlocked, previewSeconds) {
        if (!isUnlocked && previewSeconds > 0) {
            delay(1000)
            previewSeconds--
        } else if (!isUnlocked && previewSeconds == 0) {
            exoPlayer.pause()
        }
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

    val handleUnlock = { 
        if (unlockCode.trim().lowercase() == "nikkiboss") {
            isUnlocked = true
            sharedPrefs.edit().putBoolean("zesty_unlocked", true).apply()
            exoPlayer.play()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            // 15 seconds pseudo-buffer 
            if (bufferCountdown > 0) {
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha=0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.Red, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Your stream is right here", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        Spacer(Modifier.height(8.dp))
                        Text(text = "${bufferCountdown}s", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else if (!isUnlocked && previewSeconds == 0) {
                // 2 Minute Lock Screen
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha=0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp).background(Color(0xFF141414), RoundedCornerShape(12.dp)).padding(16.dp)
                    ) {
                        Text("PREVIEW ENDED", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("Access premium channels by unlocking with the code from @SPEEDNIKK.", color = Color.LightGray, fontSize = 10.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = unlockCode,
                            onValueChange = { unlockCode = it },
                            placeholder = { Text("Code...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Red,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.height(50.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = handleUnlock,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("PROCEED", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            } else {
                // Watermark & Countdown
                Box(modifier = Modifier.matchParentSize().padding(16.dp).statusBarsPadding(), contentAlignment = Alignment.TopEnd) {
                    Column(horizontalAlignment = Alignment.End) {
                         Text("zestyysports", color = Color.White.copy(alpha=0.4f), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                         if (!isUnlocked) {
                             Text(text = "${previewSeconds / 60}:${String.format("%02d", previewSeconds % 60)}", color=Color.White.copy(alpha=0.4f), fontSize=12.sp, fontWeight=FontWeight.Bold)
                         }
                    }
                }
                
                if (showStatsForNerds) {
                    // Stats for Nerds overlay
                    Box(modifier = Modifier.padding(16.dp).statusBarsPadding().align(Alignment.CenterStart).background(Color.Black.copy(alpha=0.7f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription="Stats", tint = Color.Red, modifier=Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("STATS FOR NERDS", color=Color.White, fontSize=10.sp, fontWeight=FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Resolution: 1920x1080 (HD)", color=Color.Green, fontSize=9.sp)
                            Text("Buffer Length: 2.4s", color=Color(0xFFFFC107), fontSize=9.sp)
                            Text("Network: ${exoPlayer.videoFormat?.bitrate?.let { it / 1000 } ?: "3200"} kbps", color=Color(0xFF3B82F6), fontSize=9.sp)
                            Text("Codec: ${exoPlayer.videoFormat?.codecs ?: "avc1, mp4a"}", color=Color.White, fontSize=9.sp)
                        }
                    }
                }
            }

            // Top bar overlay for back/fullscreen
            if (bufferCountdown == 0 && previewSeconds > 0 || isUnlocked) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .statusBarsPadding(), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = channel.name, 
                        color = Color.White, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end=8.dp)
                    )
                    IconButton(
                        onClick = { onOrientationChange(true) },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Fullscreen", tint = Color.White)
                    }
                }
            }
        }
        
        // Channel Info Area
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(Color.Red.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Text("LIVE", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text("${channel.group} · ULTRA HD".uppercase(), color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { showStatsForNerds = !showStatsForNerds },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("STATS", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(channel.name.uppercase(), color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.background(Color.Blue.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Ad-Free", color = Color.Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("MORE CHANNELS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            
            // Horizontal list
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allChannels.take(20)) { ch ->
                    val isPlaying = ch.id == channel.id
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(if (isPlaying) 2.dp else 1.dp, if (isPlaying) Color.Red else Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable { onChannelSelect(ch) }
                    ) {
                        Column {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                                if (ch.logo.isNotEmpty()) {
                                    AsyncImage(model = ch.logo, contentDescription = ch.name, contentScale = ContentScale.Inside, modifier = Modifier.padding(12.dp))
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("TELEGRAM", color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9"))) })
                        Text("@NIKKK.EXE", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/nikkk.exe"))) })
                    }
                    Text("adfree by Nikshep", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
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
            items.add(M3UItem(id = trimmed, name = currentName, logo = currentLogo, group = currentGroup, url = trimmed))
            currentName = ""
            currentLogo = ""
            currentGroup = ""
        }
    }
    return items
}

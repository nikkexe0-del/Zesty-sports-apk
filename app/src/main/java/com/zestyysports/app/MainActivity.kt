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
                    background = Color(0xFF0F1014),
                    surface = Color(0xFF16181F),
                    onBackground = Color(0xFFFFFFFF),
                    onSurface = Color(0xFFE1E6F0)
                )
            } else {
                lightColorScheme(
                    background = Color(0xFFF5F5F7),
                    surface = Color.White,
                    onBackground = Color(0xFF1D1D1F),
                    onSurface = Color(0xFF1D1D1F)
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
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
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
    }
}

@Composable
fun HeroSection() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(180.dp)
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
            Image(
                painter = painterResource(id = R.drawable.zestyy_logo),
                contentDescription = "Zesty Logo",
                modifier = Modifier.height(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Worldwide channels in HD, Ad-free, 4K — for free. Access premium channels instantly.", color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Telegram Gradient Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF))))
                        .bounceClick { 
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9"))
                            context.startActivity(i)
                        }
                        .padding(horizontal=16.dp, vertical=10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription=null, modifier=Modifier.size(14.dp), tint=Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("TELEGRAM", fontSize=10.sp, fontWeight=FontWeight.Black, color=Color.White)
                    }
                }
                
                // Instagram Gradient Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))))
                        .bounceClick { 
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/nikkk.exe"))
                            context.startActivity(i)
                        }
                        .padding(horizontal=16.dp, vertical=10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription=null, modifier=Modifier.size(14.dp), tint=Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("@NIKKK.EXE", fontSize=10.sp, fontWeight=FontWeight.Black, color=Color.White)
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
        Spacer(Modifier.height(12.dp))
        Text(
            "Worldwide channels in HD, Ad-free, 4K — for free.", 
            color = Color.Gray, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Medium, 
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )
        Spacer(Modifier.height(32.dp))
        
        // Instagram
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFf09433), Color(0xFFe6683c), Color(0xFFdc2743), Color(0xFFcc2366), Color(0xFFbc1888))))
                .bounceClick { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/nikkk.exe"))) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription=null, tint=Color.White, modifier=Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connect on Instagram", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Telegram
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF))))
                .bounceClick { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9"))) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Send, contentDescription=null, tint=Color.White, modifier=Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Join on Telegram", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ZestyyFlix
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFE50914), Color(0xFFB81D24))))
                .bounceClick { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zestyyflix.vercel.app"))) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Movie, contentDescription=null, tint=Color.White, modifier=Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("For premium movie experience", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
            }
        }
    }
}

@Composable
fun ChannelMiniCard(channel: M3UItem, isFavorite: Boolean, onToggleFavorite: (String) -> Unit, onPlay: (M3UItem) -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .bounceClick { onPlay(channel) },
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
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(channel.logo)
                            .crossfade(300)
                            .build(),
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
                        .bounceClick { onToggleFavorite(channel.id) }
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

    val exoPlayer = remember {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                32000, 
                65536, 
                2500,  
                5000   
            )
            .setBackBuffer(0, false)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
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
        val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(channel.url))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = false
    }

    LaunchedEffect(showJoinPopup) {
        if (!showJoinPopup) {
            exoPlayer.play()
        } else {
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
            modifier = playerModifier.clickable { showControls = !showControls }
        ) {
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
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
                         modifier = Modifier.height(48.dp),
                         alpha = 0.6f
                     )
                }
                
                if (showStatsForNerds) {
                    var currentPosition by remember { mutableStateOf(0L) }
                    LaunchedEffect(Unit) {
                        while(true) {
                            currentPosition = exoPlayer.currentPosition
                            delay(1000)
                        }
                    }
                    val videoFormat = exoPlayer.videoFormat
                    val width = videoFormat?.width ?: 1920
                    val height = videoFormat?.height ?: 1080
                    val resolution = if (width > 0) "${width}x${height}" else "1920x1080"
                    val isHD = if (width >= 1280) " (HD)" else ""
                    val bitrateKbps = videoFormat?.bitrate?.takeIf { it > 0 }?.let { it / 1000 } ?: 3200
                    val dataConsumedMB = (bitrateKbps * (currentPosition / 1000f)) / 8192f
                    
                    Box(modifier = Modifier.padding(16.dp).statusBarsPadding().align(Alignment.CenterStart).background(Color.Black.copy(alpha=0.8f), RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha=0.1f), RoundedCornerShape(12.dp)).padding(16.dp).widthIn(min = 280.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("STATS FOR NERDS", color=Color.White, fontSize=10.sp, fontWeight=FontWeight.Bold, letterSpacing = 2.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Resolution: $resolution$isHD", color=Color.White, fontSize=11.sp)
                            Text("Data Consumed: ${String.format("%.1f", dataConsumedMB)} MB", color=Color.White, fontSize=11.sp)
                            Text("Network: $bitrateKbps kbps", color=Color.White, fontSize=11.sp)
                            Text("Quality: Auto Mode", color=Color.White, fontSize=11.sp)
                        }
                    }
                }
            }

            // Custom Controller Overlay
            if (showControls) {
                var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
                val activity = context as? android.app.Activity
                var isMuted by remember { mutableStateOf(exoPlayer.volume == 0f) }
                
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

                    // Play/Pause Center
                    IconButton(
                        onClick = { 
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            isPlaying = !isPlaying
                        }, 
                        modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.5f), CircleShape).size(64.dp)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    
                    // Bottom Right: Fullscreen, Stats, Mute
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        }) {
                            Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = "Mute Toggle", tint = Color.White)
                        }
                        IconButton(onClick = { showStatsForNerds = !showStatsForNerds }) {
                            Icon(Icons.Default.Info, contentDescription = "Stats", tint = Color.White)
                        }
                        IconButton(onClick = { /* Settings Placeholder */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                        IconButton(onClick = { onOrientationChange(!isLandscape) }) {
                            Icon(if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
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
                                Box(modifier = Modifier.background(Color.Blue.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=4.dp)) {
                                    Text("AD-FREE", color = Color.Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.background(Color.Red.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=4.dp)) {
                                    Text("LIVE", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Color.Red else Color.Gray)
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(if (isPlaying) 2.dp else 1.dp, if (isPlaying) Color.Red else Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))).bounceClick{ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+0sACDI0bSDI2Njg9"))) }.padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Send, contentDescription=null, tint=Color.White, modifier=Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("OPEN TELEGRAM", fontSize=12.sp, fontWeight=FontWeight.Black, color=Color.White, textAlign = TextAlign.Center)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFE50914), Color(0xFFB81D24)))).bounceClick{ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zestyyflix.vercel.app/"))) }.padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Movie, contentDescription=null, tint=Color.White, modifier=Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("VISIT ZESTYYFLIX", fontSize=12.sp, fontWeight=FontWeight.Black, color=Color.White, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showJoinPopup = false }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                        Text("CLOSE & WATCH STREAM", color = Color.White, fontWeight = FontWeight.Bold)
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

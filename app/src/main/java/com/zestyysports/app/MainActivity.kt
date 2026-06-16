package com.zestyysports.app

import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class M3UItem(val id: String, val name: String, val logo: String, val group: String, val url: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF050505),
                surface = Color(0xFF111111)
            )) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ZestyyApp(onOrientationChange = { landscape ->
                        requestedOrientation = if (landscape) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun ZestyyApp(onOrientationChange: (Boolean) -> Unit) {
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
            onOrientationChange = onOrientationChange,
            onBack = { selectedChannel = null }
        )
    } else {
        onOrientationChange(false) // Make sure portrait
        ChannelListScreen(channels, isLoading) {
            selectedChannel = it
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(channels: List<M3UItem>, isLoading: Boolean, onPlay: (M3UItem) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("zestyysports adfree", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505), 
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF050505)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                items(channels) { channel ->
                    ChannelMiniCard(channel, onPlay)
                }
            }
        }
    }
}

@Composable
fun ChannelMiniCard(channel: M3UItem, onPlay: (M3UItem) -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onPlay(channel) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                } else {
                    Text(
                        text = channel.name.take(3).uppercase(),
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
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
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = channel.name, 
                color = Color.White, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (channel.group.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.group.uppercase(), 
                    color = Color.Gray, 
                    fontSize = 9.sp, 
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
fun VideoPlayerScreen(channel: M3UItem, onOrientationChange: (Boolean) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(channel.url)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(Unit) {
        onOrientationChange(true) // Switch to Landscape
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            onOrientationChange(false) // Switch back when leaving
        }
    }

    BackHandler {
        onBack()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Custom Back Button Overlay
        Row(Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = channel.name, 
                color = Color.White, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
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

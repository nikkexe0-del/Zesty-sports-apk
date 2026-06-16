package com.zestyysports.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

data class M3UItem(val id: String, val name: String, val logo: String, val group: String, val url: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ZestyyApp()
            }
        }
    }
}

@Composable
fun ZestyyApp() {
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
        VideoPlayerScreen(channel = selectedChannel!!) {
            selectedChannel = null
        }
    } else {
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
                title = { Text("ZestyySports Premium") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(channels) { channel ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onPlay(channel) },
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(channel.name, color = Color.White, fontSize = 18.sp)
                            if (channel.group.isNotEmpty()) {
                                Text(channel.group, color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(channel: M3UItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(channel.url)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(channel.name, color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(start = 16.dp))
        }
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f)
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
            items.add(M3UItem(id = trimmed, name = currentName, logo = currentLogo, group = currentGroup, url = trimmed))
            currentName = ""
            currentLogo = ""
            currentGroup = ""
        }
    }
    return items
}

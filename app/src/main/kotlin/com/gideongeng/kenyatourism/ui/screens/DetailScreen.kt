package com.gideongeng.kenyatourism.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gideongeng.kenyatourism.R
import com.gideongeng.kenyatourism.data.Comment
import com.gideongeng.kenyatourism.data.Destination
import com.gideongeng.kenyatourism.data.DestinationsRepository
import com.gideongeng.kenyatourism.data.FavoritesManager
import com.gideongeng.kenyatourism.data.optimizeUnsplashUrl
import com.gideongeng.kenyatourism.ui.components.Chip
import com.gideongeng.kenyatourism.ui.components.WeatherWidget
import com.gideongeng.kenyatourism.ui.theme.MaasaiRed
import com.gideongeng.kenyatourism.ui.theme.SafariGreen
import com.gideongeng.kenyatourism.ui.theme.SavannahGold
import kotlinx.coroutines.flow.collect

import com.gideongeng.kenyatourism.data.VisitedManager
import com.gideongeng.kenyatourism.data.DestinationMedia
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DestinationDetailScreen(
    destination: Destination,
    favoritesManager: FavoritesManager,
    visitedManager: VisitedManager,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val isFavorite by favoritesManager.favorites.collectAsState()
    val isVisited by visitedManager.visitedDestinations.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val comments by DestinationsRepository.getComments(destination.id).collectAsState(initial = emptyList())
    val communityMedia by DestinationsRepository.getMedia(destination.id).collectAsState(initial = emptyList())
    var newCommentText by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    // Prepare all media items (official images + community media)
    val allMedia = remember(destination, communityMedia) {
        val list = mutableListOf<DestinationMedia>()
        
        // Add official main image from local resources
        val mainDrawableId = DestinationsRepository.getDestinationDrawable(context, destination.name)
        if (mainDrawableId != 0) {
            list.add(DestinationMedia("android.resource://${context.packageName}/$mainDrawableId", "image", "Kenya Tourism", 0))
            
            // Discover gallery images (_2, _3, _4, etc.)
            val resName = context.resources.getResourceEntryName(mainDrawableId)
            for (i in 2..10) {
                val galleryId = context.resources.getIdentifier("${resName}_$i", "drawable", context.packageName)
                if (galleryId != 0) {
                    list.add(DestinationMedia("android.resource://${context.packageName}/$galleryId", "image", "Kenya Tourism", 0))
                }
            }
        }
        
        // Add video if any
        destination.videoUrl?.let {
            if (it.isNotBlank() && it.startsWith("http")) {
                list.add(DestinationMedia(it, "video", "Kenya Tourism", 0))
            }
        }

        // Add community media
        list.addAll(communityMedia)
        list
    }

    val pagerState = rememberPagerState(pageCount = { allMedia.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    // Smart Auto-Loop
    LaunchedEffect(allMedia.size, isDragged) {
        if (!isDragged && allMedia.size > 1) {
            while (true) {
                delay(4000) // 4 seconds interval
                if (!isDragged) {
                    val nextPage = (pagerState.currentPage + 1) % allMedia.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val mimeType = context.contentResolver.getType(it)
                val type = if (mimeType?.contains("video") == true) "video" else "image"
                
                if (type == "video") {
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, it)
                        val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = time?.toLong() ?: 0
                        if (durationMs > 60000) {
                            android.widget.Toast.makeText(context, "Videos must be less than 1 minute", android.widget.Toast.LENGTH_SHORT).show()
                            return@let
                        }
                    } catch (e: Exception) {
                        // Fallback or ignore
                    } finally {
                        retriever.release()
                    }
                }

                isUploading = true
                DestinationsRepository.uploadMedia(context, destination.id, it, type) { success ->
                    isUploading = false
                }
            }
        }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Media Gallery Header
                Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        MediaSliderItem(media = allMedia[page])
                    }
                    
                    // Indicators
                    if (allMedia.size > 1) {
                        Row(
                            Modifier
                                .height(50.dp)
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(allMedia.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val shareText = "Check out ${destination.name} on Kenya Tourism! 🇰🇪\n\n${destination.description.take(300)}...\n\nExplore more here: ${destination.imageUrl}"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, destination.name)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            if (isUploading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                IconButton(
                                    onClick = {
                                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                    },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Upload", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // Content Body
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .widthIn(max = 800.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    var isExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = destination.name,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { visitedManager.toggleVisited(destination.id) }) {
                                Icon(
                                    imageVector = if (isVisited.contains(destination.id)) Icons.Default.Place else Icons.Default.PushPin,
                                    contentDescription = "Visited",
                                    tint = if (isVisited.contains(destination.id)) SavannahGold else Color.Gray
                                )
                            }
                            IconButton(onClick = { favoritesManager.toggleFavorite(destination.id) }) {
                                Icon(
                                    imageVector = if (isFavorite.contains(destination.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite.contains(destination.id)) MaasaiRed else Color.Gray
                                )
                            }
                        }
                    }

                    WeatherWidget(destination.latitude ?: -1.2921, destination.longitude ?: 36.8219)

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Read More",
                            color = SafariGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val lat = destination.latitude ?: -1.2921 
                                val lon = destination.longitude ?: 36.8219
                                val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${destination.name})")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SafariGreen)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.view_map))
                        }

                        destination.videoUrl?.let { url ->
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaasaiRed)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.watch_video))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Activities Section
                    if (destination.activities.isNotEmpty()) {
                        Text(
                            text = "Top Activities",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            destination.activities.forEach { activity ->
                                Chip(text = activity)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // User Views/Comments Section
                    Text(
                        text = "User Views",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (comments.isEmpty()) {
                        Text("No experiences shared yet. Be the first!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        comments.forEach { comment ->
                            CommentItem(comment)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("Share your view...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                DestinationsRepository.addComment(destination.id, "Tourist", newCommentText)
                                newCommentText = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafariGreen)
                    ) {
                        Text("Post View")
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
fun MediaSliderItem(media: DestinationMedia) {
    if (media.type == "video") {
        VideoPlayer(url = media.url)
    } else {
        AsyncImage(
            model = media.url.optimizeUnsplashUrl(width = 800),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false // Don't auto-play to save data
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = comment.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

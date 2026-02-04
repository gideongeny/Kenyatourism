package com.gideongeng.kenyatourism.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gideongeng.kenyatourism.R
import com.gideongeng.kenyatourism.data.Comment
import com.gideongeng.kenyatourism.data.Destination
import com.gideongeng.kenyatourism.data.DestinationsRepository
import com.gideongeng.kenyatourism.data.FavoritesManager
import com.gideongeng.kenyatourism.ui.components.Chip
import com.gideongeng.kenyatourism.ui.components.WeatherWidget
import com.gideongeng.kenyatourism.ui.theme.MaasaiRed
import com.gideongeng.kenyatourism.ui.theme.SafariGreen
import com.gideongeng.kenyatourism.ui.theme.SavannahGold
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DestinationDetailScreen(
    destination: Destination,
    favoritesManager: FavoritesManager,
    onDismiss: () -> Unit
) {
    val isFavorite by favoritesManager.favorites.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val comments by DestinationsRepository.getComments(destination.id).collectAsState(initial = emptyList())
    var newCommentText by remember { mutableStateOf("") }

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
                // Image Header
                Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    val drawableId = remember(destination.name) {
                        DestinationsRepository.getDestinationDrawable(context, destination.name)
                    }

                    AsyncImage(
                        model = if (drawableId != 0) drawableId else destination.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
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
                    }
                }

                // Content Body
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .widthIn(max = 800.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
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
                        IconButton(onClick = { favoritesManager.toggleFavorite(destination.id) }) {
                            Icon(
                                imageVector = if (isFavorite.contains(destination.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite.contains(destination.id)) MaasaiRed else Color.Gray
                            )
                        }
                    }

                    WeatherWidget(destination.latitude ?: -1.2921, destination.longitude ?: 36.8219)

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = Color.DarkGray
                    )

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
                    
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = comment.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

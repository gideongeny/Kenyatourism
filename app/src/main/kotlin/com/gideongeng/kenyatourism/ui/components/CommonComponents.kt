package com.gideongeng.kenyatourism.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .blur(20.dp) // Optional blur if supported (needs S+)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        content()
    }
}
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.gideongeng.kenyatourism.R
import com.gideongeng.kenyatourism.data.Destination
import com.gideongeng.kenyatourism.data.DestinationsRepository
import com.gideongeng.kenyatourism.data.FavoritesManager
import com.gideongeng.kenyatourism.ui.theme.MaasaiRed
import com.gideongeng.kenyatourism.ui.theme.SavannahGold

@Composable
fun BuyMeCoffeeButton(onClick: () -> Unit) {
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
            },
        containerColor = Color(0xFFFFDD00),
        contentColor = Color.Black
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("☕", fontSize = 20.sp)
            Text(stringResource(R.string.support_us), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.header_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun DestinationCard(destination: Destination, favoritesManager: FavoritesManager, onClick: () -> Unit) {
    var isHovered by remember { mutableStateOf(false) }
    val isFavorite by favoritesManager.favorites.collectAsState()
    val rotationX by animateFloatAsState(targetValue = if (isHovered) 5f else 0f)
    val rotationY by animateFloatAsState(targetValue = if (isHovered) -5f else 0f)
    val scale by animateFloatAsState(targetValue = if (isHovered) 1.02f else 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .graphicsLayer {
                this.rotationX = rotationX
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
                this.cameraDistance = 12f * density
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(if (isHovered) 16.dp else 8.dp)
    ) {
        Box {
            val context = LocalContext.current
            val drawableId = remember(destination.name) {
                DestinationsRepository.getDestinationDrawable(context, destination.name)
            }
            
            AsyncImage(
                model = if (drawableId != 0) drawableId else destination.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            
            GlassBox(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = destination.name,
                        style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
                    )
                    Text(
                        text = "${destination.category} • ${destination.region}",
                        style = MaterialTheme.typography.bodyLarge.copy(color = SavannahGold)
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaasaiRed.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "★ ${destination.rating}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(
                    onClick = { favoritesManager.toggleFavorite(destination.id) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite.contains(destination.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite.contains(destination.id)) Color.Red else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun AdBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-1281448884303417/5175601050"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun Chip(text: String) {
    Surface(
        color = SavannahGold.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

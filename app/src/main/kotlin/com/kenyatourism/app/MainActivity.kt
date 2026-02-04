package com.kenyatourism.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.kenyatourism.app.ads.AdsManager
import com.kenyatourism.app.data.Destination
import com.kenyatourism.app.data.DestinationsRepository
import com.kenyatourism.app.data.FavoritesManager
import com.kenyatourism.app.ui.screens.FavoritesScreen
import com.kenyatourism.app.ui.theme.*

class MainActivity : ComponentActivity() {
    private lateinit var favoritesManager: FavoritesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdsManager.initialize(this)
        AdsManager.loadInterstitial(this)
        favoritesManager = FavoritesManager(this)
        
        setContent {
            KenyaTourismTheme {
                MainApp(favoritesManager)
            }
        }
    }
}

@Composable
fun MainApp(favoritesManager: FavoritesManager) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            Column {
                AdBanner()
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text("Wishlist") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }
            }
        },
        floatingActionButton = {
            BuyMeCoffeeButton {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/Gideongeny"))
                context.startActivity(intent)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> MainDashboard(favoritesManager) { destination ->
                    selectedDestination = destination
                }
                1 -> FavoritesScreen(favoritesManager)
            }
            
            selectedDestination?.let { destination ->
                DestinationDetailScreen(
                    destination = destination,
                    favoritesManager = favoritesManager,
                    onDismiss = { selectedDestination = null }
                )
            }
        }
    }
}

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
            Text("Support", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MainDashboard(favoritesManager: FavoritesManager, onDestinationClick: (Destination) -> Unit) {
    val destinations = DestinationsRepository.allDestinations
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DashboardHeader()
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Explore ${destinations.size} Amazing Destinations",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            items(destinations) { destination ->
                DestinationCard(destination, favoritesManager, onClick = {
                    onDestinationClick(destination)
                    AdsManager.showInterstitial(context as ComponentActivity)
                })
            }
        }
    }
}

@Composable
fun DashboardHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    listOf(MaasaiRed, SavannahGold)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TEMBEA KENYA",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = "Discover Kenya's Hidden Treasures",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f)
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
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
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
fun DestinationDetailScreen(
    destination: Destination,
    favoritesManager: FavoritesManager,
    onDismiss: () -> Unit
) {
    val isFavorite by favoritesManager.favorites.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
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
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                
                IconButton(
                    onClick = { favoritesManager.toggleFavorite(destination.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite.contains(destination.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite.contains(destination.id)) Color.Red else Color.Gray
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = destination.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(text = destination.category)
                        Chip(text = "★ ${destination.rating}")
                        Chip(text = destination.region)
                    }
                }
                
                item {
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                destination.bestTimeToVisit?.let { time ->
                    item {
                        InfoSection("Best Time to Visit", time)
                    }
                }
                
                if (destination.activities.isNotEmpty()) {
                    item {
                        Text("Activities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        destination.activities.forEach { activity ->
                            Text("• $activity", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                destination.latitude?.let { lat ->
                    destination.longitude?.let { lon ->
                        item {
                            InfoSection("Location", "$lat, $lon")
                        }
                    }
                }
            }
        }
    }
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

@Composable
fun InfoSection(title: String, content: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(content, style = MaterialTheme.typography.bodyMedium)
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

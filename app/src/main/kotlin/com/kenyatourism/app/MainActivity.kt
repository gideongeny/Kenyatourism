package com.kenyatourism.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.kenyatourism.app.ui.theme.MaasaiRed
import com.kenyatourism.app.ui.theme.SafariGreen
import com.kenyatourism.app.ui.theme.SavannahGold
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(favoritesManager: FavoritesManager, onDestinationClick: (Destination) -> Unit) {
    val context = LocalContext.current
    val allDestinations = DestinationsRepository.allDestinations
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    val categories = listOf("All", "Wildlife Safari", "Beach", "Hiking", "Culture", "City")

    val filteredDestinations = remember(searchQuery, selectedCategory) {
        allDestinations.filter { 
            (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategory == "All" || it.category.contains(selectedCategory, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            DashboardHeader()
        }
        
        // Search & Categories Section
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Find Diani, Mara...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaasaiRed) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaasaiRed,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                // Animated Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaasaiRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Featured Destinations",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(filteredDestinations) { destination ->
            DestinationCard(destination, favoritesManager, onClick = { 
                onDestinationClick(destination)
                AdsManager.showInterstitial(context as ComponentActivity)
            })
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
        }
    }
}

@Composable
fun DashboardHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp), // Increased height for better visual impact
        contentAlignment = Alignment.Center
    ) {
        // Background Image
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.header_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Overlay for Text Readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

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
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                    
                    // Top Bar with Back and Share
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
                        
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Check out ${destination.name}!")
                                    putExtra(Intent.EXTRA_TEXT, "I'm planning to visit ${destination.name} in Kenya! 🇰🇪 It looks amazing: ${destination.description}. Download the Tembea Kenya app to see more!")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Destination"))
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                }

                // Content Body
                Column(modifier = Modifier.padding(20.dp)) {
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

                    // Weather Widget (Simulated)
                    WeatherWidget()

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Location Button
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
                            Text("View Map")
                        }

                        // Video Button (if url exists)
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
                                Text("Watch Video")
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
fun WeatherWidget() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "☀️", 
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Current Weather",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Text(
                    text = "28°C • Sunny",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1565C0)
                )
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

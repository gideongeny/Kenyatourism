package com.kenyatourism.app

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.kenyatourism.app.ui.theme.KenyaTourismTheme
import com.kenyatourism.app.ui.theme.SavannahGold
import com.kenyatourism.app.ui.theme.MaasaiRed

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdsManager.initialize(this)
        AdsManager.loadInterstitial(this)
        
        setContent {
            KenyaTourismTheme {
                MainDashboard()
            }
        }
    }
}

@Composable
fun MainDashboard() {
    val destinations = DestinationsRepository.allDestinations
    val context = LocalContext.current

    Scaffold(
        bottomBar = { AdBanner() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        text = "Explore 100+ Magical Destinations",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                items(destinations) { destination ->
                    DestinationCard(destination) {
                        AdsManager.showInterstitial(context as ComponentActivity)
                    }
                }
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
                text = "MAGICAL KENYA",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = "YOUR 2026 WORLD CLASS GATEWAY",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}

@Composable
fun DestinationCard(destination: Destination, onClick: () -> Unit) {
    var isHovered by remember { mutableStateOf(false) }
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
            AsyncImage(
                model = destination.imageUrl,
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
            
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
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

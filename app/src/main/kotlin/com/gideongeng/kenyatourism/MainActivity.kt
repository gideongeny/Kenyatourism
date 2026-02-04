package com.gideongeng.kenyatourism

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.gideongeng.kenyatourism.ads.AdsManager
import com.gideongeng.kenyatourism.data.FavoritesManager
import com.gideongeng.kenyatourism.ui.components.AdBanner
import com.gideongeng.kenyatourism.ui.components.BuyMeCoffeeButton
import com.gideongeng.kenyatourism.ui.screens.DashboardScreen
import com.gideongeng.kenyatourism.ui.screens.DestinationDetailScreen
import com.gideongeng.kenyatourism.ui.screens.FavoritesScreen
import com.gideongeng.kenyatourism.ui.screens.MapScreen
import com.gideongeng.kenyatourism.ui.screens.ChatScreen
import com.gideongeng.kenyatourism.ui.theme.KenyaTourismTheme
import com.gideongeng.kenyatourism.ui.viewmodels.DestinationViewModel
import androidx.compose.ui.res.stringResource
import com.gideongeng.kenyatourism.data.DestinationsRepository
import com.gideongeng.kenyatourism.R

class MainActivity : ComponentActivity() {
    private lateinit var favoritesManager: FavoritesManager
    private val viewModel: DestinationViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DestinationsRepository.initialize(this)
        com.gideongeng.kenyatourism.ai.AiManager.initialize()
        AdsManager.initialize(this)
        AdsManager.loadInterstitial(this)
        favoritesManager = FavoritesManager(this)
        
        setContent {
            KenyaTourismTheme {
                MainApp(viewModel, favoritesManager)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: DestinationViewModel, favoritesManager: FavoritesManager) {
    var selectedTab by remember { mutableStateOf(0) }
    val selectedDestination by viewModel.selectedDestination.collectAsState()
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
                        icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
                        label = { Text(stringResource(R.string.explore_map)) },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Face, contentDescription = "AI") },
                        label = { Text("AI Guide") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text(stringResource(R.string.wishlist_title)) },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab != 2) {
                BuyMeCoffeeButton {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/Gideongeny"))
                    context.startActivity(intent)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> {
                    val searchQuery by viewModel.searchQuery.collectAsState()
                    val selectedCategory by viewModel.selectedCategory.collectAsState()
                    val filteredDestinations by viewModel.filteredDestinations.collectAsState(initial = emptyList())
                    
                    DashboardScreen(
                        destinations = filteredDestinations,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { viewModel.updateCategory(it) },
                        favoritesManager = favoritesManager,
                        onDestinationClick = { destination ->
                            viewModel.selectDestination(destination)
                            AdsManager.showInterstitial(context as ComponentActivity)
                        }
                    )
                }
                1 -> MapScreen(viewModel)
                2 -> ChatScreen()
                3 -> FavoritesScreen(favoritesManager)
            }
            
            selectedDestination?.let { destination ->
                DestinationDetailScreen(
                    destination = destination,
                    favoritesManager = favoritesManager,
                    onDismiss = { viewModel.selectDestination(null) }
                )
            }
        }
    }
}

package com.gideongeng.kenyatourism

import android.content.Context
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
import com.gideongeng.kenyatourism.data.AuthManager
import com.gideongeng.kenyatourism.data.FavoritesManager
import com.gideongeng.kenyatourism.data.LanguageManager
import com.gideongeng.kenyatourism.ui.components.AdBanner
import com.gideongeng.kenyatourism.ui.screens.ContactUsScreen
import com.gideongeng.kenyatourism.ui.screens.DashboardScreen
import com.gideongeng.kenyatourism.ui.screens.DestinationDetailScreen
import com.gideongeng.kenyatourism.ui.screens.FavoritesScreen
import com.gideongeng.kenyatourism.ui.screens.LoginScreen
import com.gideongeng.kenyatourism.ui.screens.MapScreen
import com.gideongeng.kenyatourism.ui.screens.ChatScreen
import com.gideongeng.kenyatourism.ui.screens.PrivacyPolicyScreen
import com.gideongeng.kenyatourism.ui.screens.SettingsScreen
import com.gideongeng.kenyatourism.ui.screens.WelcomeScreen
import com.gideongeng.kenyatourism.ui.theme.KenyaTourismTheme
import com.gideongeng.kenyatourism.ui.viewmodels.DestinationViewModel
import androidx.compose.ui.res.stringResource
import com.gideongeng.kenyatourism.data.DestinationsRepository
import com.gideongeng.kenyatourism.R

import com.gideongeng.kenyatourism.data.VisitedManager
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var favoritesManager: FavoritesManager
    private lateinit var visitedManager: VisitedManager
    private val viewModel: DestinationViewModel by viewModels()
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DestinationsRepository.initialize(this)
        com.gideongeng.kenyatourism.ai.AiManager.initialize()
        AdsManager.initialize(this)
        AdsManager.loadInterstitial(this)
        AdsManager.showAppOpenAd(this)
        favoritesManager = FavoritesManager(this)
        visitedManager = VisitedManager(this)
        
        val isFirstLaunch = LanguageManager.isFirstLaunch(this)
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        setContent {
            KenyaTourismTheme {
                AppNavigation(
                    viewModel = viewModel,
                    favoritesManager = favoritesManager,
                    visitedManager = visitedManager,
                    isFirstLaunch = isFirstLaunch
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: DestinationViewModel,
    favoritesManager: FavoritesManager,
    visitedManager: VisitedManager,
    isFirstLaunch: Boolean
) {
    var currentScreen by remember { mutableStateOf(if (isFirstLaunch) "welcome" else "main") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    when (currentScreen) {
        "welcome" -> {
            WelcomeScreen(
                onSignIn = { currentScreen = "login" },
                onSkip = {
                    LanguageManager.setFirstLaunchComplete(context)
                    scope.launch {
                        AuthManager.signInAnonymously()
                    }
                    currentScreen = "main"
                },
                onLanguageSelected = { langCode ->
                    LanguageManager.setSelectedLanguage(context, langCode)
                }
            )
        }
        "login" -> {
            LoginScreen(
                onLoginSuccess = {
                    LanguageManager.setFirstLaunchComplete(context)
                    // Navigate immediately - don't block on cloud sync
                    currentScreen = "main"
                    // Sync favorites from cloud in background
                    scope.launch {
                        try {
                            val cloudFavorites = AuthManager.loadFavoritesFromCloud()
                            cloudFavorites?.forEach { id ->
                                try {
                                    if (!favoritesManager.isFavorite(id)) {
                                        favoritesManager.toggleFavorite(id)
                                    }
                                } catch (_: Exception) { }
                            }
                        } catch (_: Exception) { }
                    }
                },
                onBack = { currentScreen = "welcome" },
                onSkip = {
                    LanguageManager.setFirstLaunchComplete(context)
                    scope.launch {
                        AuthManager.signInAnonymously()
                    }
                    currentScreen = "main"
                }
            )
        }
        "settings" -> {
            SettingsScreen(
                onBack = { currentScreen = "main" },
                onSignOut = {
                    currentScreen = "welcome"
                },
                onLanguageChanged = { langCode ->
                    LanguageManager.setSelectedLanguage(context, langCode)
                    // Restart activity to apply language change reliably
                    var currentContext = context
                    while (currentContext is android.content.ContextWrapper) {
                        if (currentContext is android.app.Activity) {
                            val intent = android.content.Intent(currentContext, MainActivity::class.java)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            currentContext.startActivity(intent)
                            currentContext.finish()
                            break
                        }
                        currentContext = currentContext.baseContext
                    }
                },
                onContactUsClick = {
                    currentScreen = "contact_us"
                },
                onPrivacyClick = {
                    currentScreen = "privacy"
                }
            )
        }
        "contact_us" -> {
            ContactUsScreen(onBack = { currentScreen = "settings" })
        }
        "privacy" -> {
            PrivacyPolicyScreen(onBack = { currentScreen = "settings" })
        }
        "main" -> {
            MainApp(
                viewModel = viewModel,
                favoritesManager = favoritesManager,
                visitedManager = visitedManager,
                onOpenSettings = { currentScreen = "settings" }
            )
        }
    }
}

@Composable
fun MainApp(
    viewModel: DestinationViewModel,
    favoritesManager: FavoritesManager,
    visitedManager: VisitedManager,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val context = LocalContext.current

    if (selectedTab != 0 && selectedDestination == null) {
        BackHandler {
            selectedTab = 0
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                AdBanner()
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text(stringResource(R.string.nav_home)) },
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectDestination(null); selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
                        label = { Text(stringResource(R.string.nav_map)) },
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectDestination(null); selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Face, contentDescription = "AI") },
                        label = { Text(stringResource(R.string.nav_ai)) },
                        selected = selectedTab == 2,
                        onClick = { viewModel.selectDestination(null); selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text(stringResource(R.string.nav_wishlist)) },
                        selected = selectedTab == 3,
                        onClick = { viewModel.selectDestination(null); selectedTab = 3 }
                    )
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
                        },
                        onOpenSettings = onOpenSettings
                    )
                }
                1 -> MapScreen(viewModel, visitedManager)
                2 -> ChatScreen()
                3 -> FavoritesScreen(favoritesManager)
            }
            
            selectedDestination?.let { destination ->
                DestinationDetailScreen(
                    destination = destination,
                    favoritesManager = favoritesManager,
                    visitedManager = visitedManager,
                    onDismiss = { viewModel.selectDestination(null) }
                )
            }
        }
    }
}

package com.gideongeng.kenyatourism.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gideongeng.kenyatourism.R
import com.gideongeng.kenyatourism.data.AuthManager
import com.gideongeng.kenyatourism.data.LanguageManager
import com.gideongeng.kenyatourism.ui.theme.*

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onContactUsClick: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by AuthManager.currentUser.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(LanguageManager.getSelectedLanguage(context)) }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onBack()
    }

    // Language dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LanguageManager.supportedLanguages.forEach { lang ->
                        val isSelected = selectedLanguage == lang.code
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedLanguage = lang.code
                                    LanguageManager.setSelectedLanguage(context, lang.code)
                                    onLanguageChanged(lang.code)
                                    showLanguageDialog = false
                                },
                            color = if (isSelected) SavannahGold.copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lang.nativeName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = SavannahGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        TopAppBar(
            title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Profile Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile picture
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = SafariGreen.copy(alpha = 0.3f),
                border = BorderStroke(3.dp, SavannahGold)
            ) {
                if (currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = currentUser?.photoUrl,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = SavannahGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = currentUser?.displayName ?: stringResource(R.string.guest_user),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            currentUser?.email?.let { email ->
                if (email.isNotEmpty()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        // Preferences Section
        SettingsSection(title = "Preferences") {
            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.language),
                subtitle = LanguageManager.supportedLanguages.find { it.code == selectedLanguage }?.nativeName ?: "English",
                onClick = { showLanguageDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notifications),
                subtitle = "Manage notification preferences",
                onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // About Section
        SettingsSection(title = stringResource(R.string.about)) {
            SettingsExpandableItem(
                icon = Icons.Default.Visibility,
                title = stringResource(R.string.about_vision),
                content = stringResource(R.string.about_vision_text),
                isExpanded = expandedSection == "vision",
                onToggle = { expandedSection = if (expandedSection == "vision") null else "vision" }
            )
            SettingsExpandableItem(
                icon = Icons.Default.Flag,
                title = stringResource(R.string.about_goal),
                content = stringResource(R.string.about_goal_text),
                isExpanded = expandedSection == "goal",
                onToggle = { expandedSection = if (expandedSection == "goal") null else "goal" }
            )
            SettingsExpandableItem(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.privacy_policy),
                content = stringResource(R.string.privacy_text),
                isExpanded = expandedSection == "privacy",
                onToggle = { expandedSection = if (expandedSection == "privacy") null else "privacy" }
            )
            SettingsExpandableItem(
                icon = Icons.Default.Gavel,
                title = stringResource(R.string.guidelines),
                content = stringResource(R.string.guidelines_text),
                isExpanded = expandedSection == "guidelines",
                onToggle = { expandedSection = if (expandedSection == "guidelines") null else "guidelines" }
            )
            SettingsItem(
                icon = Icons.Default.Email,
                title = stringResource(R.string.contact_us),
                subtitle = "Get support or send feedback",
                onClick = onContactUsClick
            )
        }

        // App Section
        SettingsSection(title = "App") {
            SettingsItem(
                icon = Icons.Default.Share,
                title = stringResource(R.string.share_app),
                subtitle = "Share Tembea Kenya with friends",
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Discover Kenya's hidden gems with Tembea Kenya! Download now.")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                }
            )
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.app_version),
                subtitle = "2.0.0",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Out Button
        if (currentUser != null) {
            Button(
                onClick = {
                    AuthManager.signOut(context)
                    onSignOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaasaiRed.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, null, tint = MaasaiRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sign_out), color = MaasaiRed, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = SavannahGold,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SettingsExpandableItem(
    icon: ImageVector,
    title: String,
    content: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 64.dp, end = 24.dp, bottom = 12.dp),
                lineHeight = 22.sp
            )
        }
    }
}

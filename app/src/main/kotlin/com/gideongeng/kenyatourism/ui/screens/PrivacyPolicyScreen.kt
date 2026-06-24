package com.gideongeng.kenyatourism.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gideongeng.kenyatourism.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D0D1A), Color(0xFF1a1a3e))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = {
                    Text(
                        "Privacy & Data",
                        fontWeight = FontWeight.Bold,
                        color = SavannahGold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF14142B),
                contentColor = SavannahGold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "🔒 Privacy Policy",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "🗑️ Delete My Data",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    // Privacy Policy content
                    Text(
                        "Last updated: June 2026",
                        color = SavannahGold.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(SavannahGold.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )

                    PrivacyCard(
                        emoji = "📋", title = "Overview",
                        items = listOf(
                            "Tembea Kenya is a Kenya tourism guide developed by Gideon Geng.",
                            "We are committed to protecting your privacy and being transparent about what data we collect.",
                            "This policy applies to all users of the Tembea Kenya mobile app."
                        )
                    )

                    PrivacyCard(
                        emoji = "📊", title = "Data We Collect",
                        items = listOf(
                            "Email address (when signing in with Google or Email/Password)",
                            "Display name and profile photo (from your Google account)",
                            "Saved favourite destinations (synced to your account)",
                            "Anonymous usage analytics via Firebase Analytics",
                            "Device info (OS version, model) for crash reporting only"
                        )
                    )

                    PrivacyCard(
                        emoji = "🎯", title = "Why We Collect It",
                        items = listOf(
                            "To save and sync your favourite destinations across devices",
                            "To personalise your experience in the app",
                            "To improve app performance and fix bugs",
                            "We do NOT sell your data to third parties",
                            "We do NOT use your data for advertising profiling"
                        )
                    )

                    PrivacyCard(
                        emoji = "🔐", title = "How We Protect Your Data",
                        items = listOf(
                            "All data transmitted over HTTPS (TLS encryption)",
                            "Data stored securely in Google Firebase Firestore",
                            "Firebase enforces authentication before any data access",
                            "We follow Google Play's data safety guidelines"
                        )
                    )

                    PrivacyCard(
                        emoji = "🤝", title = "Third-Party Services",
                        items = listOf(
                            "Google Firebase — Authentication, Database, Analytics",
                            "Google AdMob — In-app advertisements",
                            "OpenStreetMap — Map tiles (no personal data shared)"
                        )
                    )

                    PrivacyCard(
                        emoji = "👤", title = "Your Rights",
                        items = listOf(
                            "Request a copy of all data we hold about you",
                            "Request full deletion of your account and all data",
                            "Request deletion of specific data without deleting your account",
                            "Use the app as a guest (anonymous) without an account"
                        )
                    )

                    // Contact card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14142B)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("✉️", fontSize = 22.sp)
                                Text("Contact Us", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "For any privacy questions contact us at:",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:gideongeng@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Privacy Question — Tembea Kenya")
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SavannahGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("gideongeng@gmail.com", color = Color(0xFF0D0D1A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Delete Data content
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14142B)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🗑️", fontSize = 22.sp)
                                Text("Request Data Deletion", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }

                            Text(
                                "Tap a button below to send us a deletion request. We will process it within 30 days.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )

                            // Full account deletion
                            DeletionOptionButton(
                                emoji = "💀",
                                title = "Delete Full Account & All Data",
                                subtitle = "Permanently removes your account, favourites, and all personal data",
                                color = MaasaiRed,
                                onClick = {
                                    val subject = Uri.encode("Account Deletion Request — Tembea Kenya")
                                    val body = Uri.encode("Hello,\n\nI would like to request deletion of my full Tembea Kenya account and all associated data.\n\nPlease confirm once complete.\n\nThank you.")
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:gideongeng@gmail.com?subject=$subject&body=$body")
                                    }
                                    context.startActivity(intent)
                                }
                            )

                            // Favourites only
                            DeletionOptionButton(
                                emoji = "❤️",
                                title = "Delete My Saved Favourites Only",
                                subtitle = "Removes your saved destinations but keeps your account",
                                color = SavannahGold,
                                onClick = {
                                    val subject = Uri.encode("Favourites Deletion Request — Tembea Kenya")
                                    val body = Uri.encode("Hello,\n\nI would like to request deletion of my saved favourites only. Please keep my account active.\n\nThank you.")
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:gideongeng@gmail.com?subject=$subject&body=$body")
                                    }
                                    context.startActivity(intent)
                                }
                            )

                            // Analytics data
                            DeletionOptionButton(
                                emoji = "📊",
                                title = "Delete Analytics Data Only",
                                subtitle = "Removes usage analytics while keeping your account and favourites",
                                color = Color(0xFF4285F4),
                                onClick = {
                                    val subject = Uri.encode("Analytics Data Deletion — Tembea Kenya")
                                    val body = Uri.encode("Hello,\n\nI would like to request deletion of my analytics data only.\n\nThank you.")
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:gideongeng@gmail.com?subject=$subject&body=$body")
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    // What happens notice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SavannahGold.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ℹ️ What happens after you submit", fontWeight = FontWeight.Bold, color = SavannahGold, fontSize = 13.sp)
                            listOf(
                                "You will receive a confirmation email within 24 hours",
                                "Your request will be processed within 30 days",
                                "Full account deletion removes all Firestore data and authentication records",
                                "Anonymous analytics may be retained up to 90 days per Google policy"
                            ).forEach {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("▸", color = SavannahGold, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                                    Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PrivacyCard(emoji: String, title: String, items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14142B)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(emoji, fontSize = 22.sp)
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            }
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("▸", color = SavannahGold, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    Text(item, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun DeletionOptionButton(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Icon(Icons.Default.Delete, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

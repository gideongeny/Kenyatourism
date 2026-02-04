package com.gideongeng.kenyatourism.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gideongeng.kenyatourism.R
import com.gideongeng.kenyatourism.ui.theme.SafariGreen
import com.gideongeng.kenyatourism.ui.theme.SavannahGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Message(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf(Message("Jambo! I am your Safari AI Guide. How can I help you explore Kenya today?", false))) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SafariGreen,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.ai_guide),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by Gemini AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        // Input Area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = SafariGreen
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val userMsg = inputText
                            messages = messages + Message(userMsg, true)
                            inputText = ""
                            
                            scope.launch {
                                delay(500)
                                listState.animateScrollToItem(messages.size - 1)
                                
                                // Real Gemini AI Call
                                val aiResponse = com.gideongeng.kenyatourism.ai.GeminiManager.getResponse(
                                    com.gideongeng.kenyatourism.ai.GeminiManager.getSafariPrompt(userMsg)
                                )
                                
                                messages = messages + Message(aiResponse, false)
                                delay(100)
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    containerColor = SafariGreen,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (message.isUser) SavannahGold else Color(0xFFE8F5E9),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            ),
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser) Color.Black else Color.DarkGray
            )
        }
    }
}

private fun getAiResponse(query: String): String {
    val q = query.lowercase()
    return when {
        q.contains("mara") || q.contains("wildlife") -> "Maasai Mara is world-famous for the Great Migration. You can see the Big Five there! Would you like to check the weather there?"
        q.contains("beach") || q.contains("diani") -> "Diani Beach has amazing white sands. It's perfect for skydiving and snorkeling!"
        q.contains("food") || q.contains("eat") -> "You must try Nyama Choma and Ugali! Also, Swahili dishes like Pilau in Mombasa are delicious."
        q.contains("mount kenya") -> "Mount Kenya is the second highest peak in Africa. It's a challenging but rewarding trek for hikers."
        else -> "Kenya has so much to offer! From safaris to beaches and vibrant culture. Have you visited the Nairobi National Park yet?"
    }
}

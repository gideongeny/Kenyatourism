package com.gideongeng.kenyatourism.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gideongeng.kenyatourism.data.Destination
import com.gideongeng.kenyatourism.data.FavoritesManager
import com.gideongeng.kenyatourism.ui.components.DashboardHeader
import com.gideongeng.kenyatourism.ui.components.DestinationCard
import com.gideongeng.kenyatourism.ui.theme.MaasaiRed
import com.gideongeng.kenyatourism.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    destinations: List<Destination>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    favoritesManager: FavoritesManager,
    onDestinationClick: (Destination) -> Unit
) {
    val categories = listOf("All", "Wildlife Safari", "Beach", "Hiking", "Culture", "City")

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 350.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp, top = 0.dp, start = 16.dp, end = 16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            DashboardHeader()
        }
        
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_placeholder), color = Color.Gray) },
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

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryChange(category) },
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

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.featured_destinations),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        items(destinations) { destination ->
            DestinationCard(destination, favoritesManager, onClick = { 
                onDestinationClick(destination)
            })
        }
    }
}

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gideongeng.kenyatourism.data.Destination
import com.gideongeng.kenyatourism.data.FavoritesManager
import com.gideongeng.kenyatourism.ui.components.DashboardHeader
import com.gideongeng.kenyatourism.ui.components.DestinationCard
import com.gideongeng.kenyatourism.ui.theme.MaasaiRed
import com.gideongeng.kenyatourism.ui.theme.SavannahGold
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
    onDestinationClick: (Destination) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val categories = listOf("All", "Wildlife Safari", "Beach", "Hiking", "Culture", "City", "Mountain", "Nature", "Urban", "Historical", "Marine")
    
    // Filter & sort state
    var selectedRegion by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("default") }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    val regions = listOf("All", "Nairobi", "Coast", "Rift Valley", "Western", "Central", "Eastern", "Nyanza", "North Eastern")
    
    // Apply additional filters
    val displayedDestinations = remember(destinations, selectedRegion, sortBy) {
        var result = destinations
        if (selectedRegion != "All") {
            result = result.filter { it.region.contains(selectedRegion, ignoreCase = true) }
        }
        when (sortBy) {
            "name" -> result.sortedBy { it.name }
            "rating" -> result.sortedByDescending { it.rating }
            else -> result
        }
    }
    
    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.filter_region),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(regions) { region ->
                        FilterChip(
                            selected = selectedRegion == region,
                            onClick = { selectedRegion = region },
                            label = { Text(region) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SavannahGold,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.sort_by),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sortBy == "default",
                        onClick = { sortBy = "default" },
                        label = { Text(stringResource(R.string.filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SavannahGold,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = sortBy == "name",
                        onClick = { sortBy = "name" },
                        label = { Text(stringResource(R.string.sort_name)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SavannahGold,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = sortBy == "rating",
                        onClick = { sortBy = "rating" },
                        label = { Text(stringResource(R.string.sort_rating)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SavannahGold,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedRegion != "All" || sortBy != "default") {
                    TextButton(onClick = { selectedRegion = "All"; sortBy = "default" }) {
                        Text(stringResource(R.string.clear_filters), color = MaasaiRed)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

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
            Box {
                DashboardHeader()
                // Settings icon at top right
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = Color.White
                    )
                }
            }
        }
        
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(stringResource(R.string.search_placeholder), color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaasaiRed) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaasaiRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedRegion != "All" || sortBy != "default") SavannahGold else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.featured_destinations),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Text(
                    text = "${displayedDestinations.size} found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        items(displayedDestinations) { destination ->
            DestinationCard(destination, favoritesManager, onClick = { 
                onDestinationClick(destination)
            })
        }
    }
}

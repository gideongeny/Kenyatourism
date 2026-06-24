package com.gideongeng.kenyatourism.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.gideongeng.kenyatourism.data.Destination
import com.gideongeng.kenyatourism.data.DestinationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class DestinationViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedDestination = MutableStateFlow<Destination?>(null)
    val selectedDestination: StateFlow<Destination?> = _selectedDestination.asStateFlow()

    val filteredDestinations = combine(
        _searchQuery,
        _selectedCategory,
        DestinationsRepository.allDestinations
    ) { query, category, allDestinations ->
        allDestinations.filter {
            (query.isEmpty() || it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)) &&
            (category == "All" || it.category.contains(category, ignoreCase = true))
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectDestination(destination: Destination?) {
        _selectedDestination.value = destination
    }
}

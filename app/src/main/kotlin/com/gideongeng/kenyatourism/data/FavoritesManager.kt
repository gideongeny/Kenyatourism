package com.gideongeng.kenyatourism.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow<Set<Int>>(loadFavorites())
    val favorites: StateFlow<Set<Int>> = _favorites.asStateFlow()

    private fun loadFavorites(): Set<Int> {
        val favoritesString = prefs.getString("favorites", "") ?: ""
        return if (favoritesString.isEmpty()) {
            emptySet()
        } else {
            favoritesString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    private fun saveFavorites(favorites: Set<Int>) {
        prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
    }

    fun toggleFavorite(destinationId: Int) {
        val currentFavorites = _favorites.value.toMutableSet()
        if (currentFavorites.contains(destinationId)) {
            currentFavorites.remove(destinationId)
        } else {
            currentFavorites.add(destinationId)
        }
        _favorites.value = currentFavorites
        saveFavorites(currentFavorites)
    }

    fun isFavorite(destinationId: Int): Boolean {
        return _favorites.value.contains(destinationId)
    }

    fun getFavoriteDestinations(): List<Destination> {
        val favoriteIds = _favorites.value
        return DestinationsRepository.allDestinations.filter { it.id in favoriteIds }
    }
}

package com.gideongeng.kenyatourism.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VisitedManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("visited_prefs", Context.MODE_PRIVATE)
    private val _visitedDestinations = MutableStateFlow<Set<Int>>(loadVisited())
    val visitedDestinations: StateFlow<Set<Int>> = _visitedDestinations.asStateFlow()

    private fun loadVisited(): Set<Int> {
        val visitedString = prefs.getString("visited", "") ?: ""
        return if (visitedString.isEmpty()) {
            emptySet()
        } else {
            visitedString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    private fun saveVisited(visited: Set<Int>) {
        prefs.edit().putString("visited", visited.joinToString(",")).apply()
    }

    fun toggleVisited(destinationId: Int) {
        val currentVisited = _visitedDestinations.value.toMutableSet()
        if (currentVisited.contains(destinationId)) {
            currentVisited.remove(destinationId)
        } else {
            currentVisited.add(destinationId)
        }
        _visitedDestinations.value = currentVisited
        saveVisited(currentVisited)
    }

    fun isVisited(destinationId: Int): Boolean {
        return _visitedDestinations.value.contains(destinationId)
    }
}

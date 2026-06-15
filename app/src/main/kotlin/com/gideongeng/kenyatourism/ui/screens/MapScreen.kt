package com.gideongeng.kenyatourism.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gideongeng.kenyatourism.data.Destination
import com.gideongeng.kenyatourism.data.VisitedManager
import com.gideongeng.kenyatourism.ui.viewmodels.DestinationViewModel
import androidx.core.content.ContextCompat
import com.gideongeng.kenyatourism.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(viewModel: DestinationViewModel, visitedManager: VisitedManager) {
    val destinations by viewModel.filteredDestinations.collectAsState(initial = emptyList())
    val visitedIds by visitedManager.visitedDestinations.collectAsState()
    val context = LocalContext.current
    
    // Initialize OSM Configuration
    Configuration.getInstance().userAgentValue = context.packageName

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(7.0)
                    controller.setCenter(GeoPoint(-1.2921, 36.8219)) // Nairobi
                    setMultiTouchControls(true)
                }
            },
            update = { view ->
                view.overlays.clear()
                destinations.forEach { destination ->
                    val lat = destination.latitude
                    val lon = destination.longitude
                    if (lat != null && lon != null) {
                        val marker = Marker(view)
                        marker.position = GeoPoint(lat, lon)
                        marker.title = destination.name
                        marker.snippet = destination.category
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        
                        // Change marker color if visited
                        if (visitedIds.contains(destination.id)) {
                            marker.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
                            marker.icon?.setTint(android.graphics.Color.parseColor("#FFD700")) // Savannah Gold
                        }

                        marker.setOnMarkerClickListener { m, _ ->
                            viewModel.selectDestination(destination)
                            m.showInfoWindow()
                            true
                        }
                        view.overlays.add(marker)
                    }
                }
                view.invalidate()
            }
        )
    }
}

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
import com.gideongeng.kenyatourism.ui.viewmodels.DestinationViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(viewModel: DestinationViewModel) {
    val destinations by viewModel.filteredDestinations.collectAsState(initial = emptyList())
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
                    
                    destinations.forEach { destination ->
                        val lat = destination.latitude
                        val lon = destination.longitude
                        if (lat != null && lon != null) {
                            val marker = Marker(this)
                            marker.position = GeoPoint(lat, lon)
                            marker.title = destination.name
                            marker.snippet = destination.category
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.setOnMarkerClickListener { m, _ ->
                                viewModel.selectDestination(destination)
                                m.showInfoWindow()
                                true
                            }
                            overlays.add(marker)
                        }
                    }
                }
            },
            update = { view ->
                // Update logic if needed
            }
        )
    }
}

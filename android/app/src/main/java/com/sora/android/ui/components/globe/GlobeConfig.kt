package com.sora.android.ui.components.globe

import androidx.compose.ui.graphics.Color
import com.mapbox.geojson.Point

sealed class GlobeType {
    data object MainFeed : GlobeType()
    data object Explore : GlobeType()
    data object Profile : GlobeType()
}

data class GlobeConfig(
    val type: GlobeType = GlobeType.Profile,
    val initialZoom: Double = 1.5,
    val initialCenter: Point = Point.fromLngLat(0.0, 20.0),
    val enableCountryHighlight: Boolean = true,
    val countryHighlightColor: String = "#FFD700",
    val countryHighlightOpacity: Double = 0.35,
    val enableMarkers: Boolean = true,
    val enableClustering: Boolean = true,
    val clusterRadius: Int = 50,
    val clusterMaxZoom: Int = 14,
    val markerColor: String = "#FF6B35",
    val markerRadius: Double = 8.0,
    val clusterColor: String = "#FF6B35",
    val clusterDisplayRadius: Double = 20.0,
    val enableUserLocationCenter: Boolean = false,
    val userLocationZoom: Double = 3.0
) {
    companion object {
        fun mainFeed(userLocation: Point? = null) = GlobeConfig(
            type = GlobeType.MainFeed,
            initialZoom = 1.5,
            initialCenter = userLocation ?: Point.fromLngLat(0.0, 20.0),
            enableCountryHighlight = false,
            enableMarkers = true,
            enableClustering = true,
            markerColor = "#0066CC",
            enableUserLocationCenter = true,
            userLocationZoom = 3.0
        )

        fun explore(userLocation: Point? = null) = GlobeConfig(
            type = GlobeType.Explore,
            initialZoom = if (userLocation != null) 2.5 else 1.5,
            initialCenter = userLocation ?: Point.fromLngLat(0.0, 20.0),
            enableCountryHighlight = true,
            countryHighlightColor = "#FF4444",
            countryHighlightOpacity = 0.5,
            enableMarkers = true,
            enableClustering = true,
            markerColor = "#FF4444",
            enableUserLocationCenter = false
        )

        fun profile(userLocation: Point? = null) = GlobeConfig(
            type = GlobeType.Profile,
            initialZoom = 1.5,
            initialCenter = userLocation ?: Point.fromLngLat(0.0, 20.0),
            enableCountryHighlight = true,
            countryHighlightColor = "#FFD700",
            countryHighlightOpacity = 0.35,
            enableMarkers = true,
            enableClustering = true,
            markerColor = "#FF6B35",
            enableUserLocationCenter = true,
            userLocationZoom = 3.0
        )
    }
}

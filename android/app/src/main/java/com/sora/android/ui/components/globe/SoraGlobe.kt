package com.sora.android.ui.components.globe

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.has
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(MapboxExperimental::class, ExperimentalComposeUiApi::class)
@Composable
fun SoraGlobe(
    data: GlobeDisplayData?,
    config: GlobeConfig = GlobeConfig.profile(),
    isLoading: Boolean = false,
    onMarkerClick: (GlobeMarker) -> Unit = {},
    onCountryClick: (String) -> Unit = {},
    isInteracting: MutableState<Boolean> = remember { mutableStateOf(false) },
    emptyMessage: String = "No data to display"
) {
    val highlightedCountries = remember(data) {
        data?.countries?.filter { it.shouldHighlight } ?: emptyList()
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(config.initialZoom)
            center(config.initialCenter)
            pitch(0.0)
            bearing(0.0)
        }
    }

    LaunchedEffect(data) {
        Log.d("SORA_GLOBE", "Globe data changed: ${data?.markers?.size ?: 0} markers, ${data?.countries?.size ?: 0} countries")
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (data != null && (data.markers.isNotEmpty() || data.countries.isNotEmpty())) {
        val coroutineScope = rememberCoroutineScope()
        var resetJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                Log.d("SORA_GLOBE", "Globe disposed - resetting interaction")
                resetJob?.cancel()
                isInteracting.value = false
            }
        }

        MapboxMap(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            resetJob?.cancel()
                            isInteracting.value = true

                            resetJob = coroutineScope.launch {
                                delay(150)
                                isInteracting.value = false
                                Log.d("SORA_GLOBE", "Auto-reset interaction state")
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            resetJob?.cancel()
                            isInteracting.value = false
                            Log.d("SORA_GLOBE", "Touch ended - interaction = false")
                        }
                    }
                    false
                },
            mapViewportState = mapViewportState,
            style = { Style.STANDARD }
        ) {
            MapEffect(highlightedCountries, data.markers) { mapView ->
                val userLocale = Locale.getDefault()
                val language = when (userLocale.language) {
                    "pt" -> "pt"
                    else -> userLocale.language
                }

                mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->
                    style.setStyleImportConfigProperty("basemap", "language", com.mapbox.bindgen.Value.valueOf(language))

                    try {
                        mapView.mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .zoom(config.initialZoom)
                                .center(config.initialCenter)
                                .build()
                        )

                        if (config.enableCountryHighlight && highlightedCountries.isNotEmpty()) {
                            setupCountryHighlighting(style, highlightedCountries, config)
                        }

                        if (config.enableMarkers && data.markers.isNotEmpty()) {
                            setupMarkers(style, data.markers, config, onMarkerClick, onCountryClick, mapView)
                        }
                    } catch (e: Exception) {
                        Log.e("SORA_GLOBE", "Failed to setup map", e)
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun setupCountryHighlighting(
    style: Style,
    countries: List<GlobeCountry>,
    config: GlobeConfig
) {
    val countryCodes = countries.map { it.code }
    val hasIntensity = countries.any { it.intensity != null }

    if (!style.styleSourceExists("country-boundaries")) {
        style.addSource(
            vectorSource("country-boundaries") {
                url("mapbox://mapbox.country-boundaries-v1")
            }
        )
    }

    if (style.styleLayerExists("visited-countries-fill")) {
        style.removeStyleLayer("visited-countries-fill")
    }

    if (hasIntensity) {
        val intensityMap = countries.associate { it.code to (it.intensity ?: 0.0) }

        style.addLayer(
            fillLayer("visited-countries-fill", "country-boundaries") {
                sourceLayer("country_boundaries")
                fillColor(
                    match {
                        get { literal("iso_3166_1") }
                        countries.map { country ->
                            literal(country.code) to literal(getHeatMapColor(country.intensity ?: 0.0))
                        }.toTypedArray()
                        literal(config.countryHighlightColor)
                    }
                )
                fillOpacity(config.countryHighlightOpacity)
                filter(
                    match {
                        get { literal("iso_3166_1") }
                        literal(countryCodes)
                        literal(true)
                        literal(false)
                    }
                )
            }
        )
        Log.d("SORA_GLOBE", "Heat map highlighting added for ${countryCodes.size} countries")
    } else {
        style.addLayer(
            fillLayer("visited-countries-fill", "country-boundaries") {
                sourceLayer("country_boundaries")
                fillColor(config.countryHighlightColor)
                fillOpacity(config.countryHighlightOpacity)
                filter(
                    match {
                        get { literal("iso_3166_1") }
                        literal(countryCodes)
                        literal(true)
                        literal(false)
                    }
                )
            }
        )
        Log.d("SORA_GLOBE", "Country highlighting added for: $countryCodes")
    }
}

private fun getHeatMapColor(intensity: Double): String {
    return when {
        intensity >= 0.7 -> "#FF0000"
        intensity >= 0.4 -> "#FF8800"
        intensity >= 0.2 -> "#FFDD00"
        else -> "#00FF00"
    }
}

private fun setupMarkers(
    style: Style,
    markers: List<GlobeMarker>,
    config: GlobeConfig,
    onMarkerClick: (GlobeMarker) -> Unit,
    onCountryClick: (String) -> Unit,
    mapView: MapView
) {
    val features = markers.map { marker ->
        Feature.fromGeometry(
            Point.fromLngLat(marker.longitude, marker.latitude)
        ).apply {
            addStringProperty("markerId", marker.id)
            addStringProperty("cityName", marker.cityName ?: "")
            addStringProperty("countryCode", marker.countryCode)
        }
    }

    if (style.styleSourceExists("globe-markers")) {
        style.removeStyleSource("globe-markers")
    }
    listOf("clusters", "cluster-count", "unclustered-point").forEach { layerId ->
        if (style.styleLayerExists(layerId)) {
            style.removeStyleLayer(layerId)
        }
    }

    style.addSource(
        geoJsonSource("globe-markers") {
            featureCollection(FeatureCollection.fromFeatures(features))
            if (config.enableClustering) {
                cluster(true)
                clusterRadius(config.clusterRadius.toLong())
                clusterMaxZoom(config.clusterMaxZoom.toLong())
            }
        }
    )

    if (config.enableClustering) {
        style.addLayer(
            circleLayer("clusters", "globe-markers") {
                circleRadius(config.clusterDisplayRadius)
                circleColor(config.clusterColor)
                filter(has { literal("point_count") })
            }
        )

        style.addLayer(
            symbolLayer("cluster-count", "globe-markers") {
                textField(get { literal("point_count") })
                textSize(12.0)
                textColor("#FFFFFF")
                filter(has { literal("point_count") })
            }
        )
    }

    style.addLayer(
        circleLayer("unclustered-point", "globe-markers") {
            circleRadius(config.markerRadius)
            circleColor(config.markerColor)
            circleStrokeWidth(2.0)
            circleStrokeColor("#FFFFFF")
            if (config.enableClustering) {
                filter(
                    com.mapbox.maps.extension.style.expressions.dsl.generated.not {
                        has { literal("point_count") }
                    }
                )
            }
        }
    )

    mapView.gestures.addOnMapClickListener { point ->
        val screenCoordinate = mapView.mapboxMap.pixelForCoordinate(point)
        val queryGeometry = RenderedQueryGeometry(screenCoordinate)
        val options = RenderedQueryOptions(listOf("clusters", "unclustered-point"), null)

        mapView.mapboxMap.queryRenderedFeatures(queryGeometry, options) { result ->
            result.value?.let { features ->
                if (features.isNotEmpty()) {
                    val feature = features.first()
                    val geojsonFeature = feature.queriedFeature.feature
                    val pointCount = geojsonFeature.getNumberProperty("point_count")

                    if (pointCount != null && config.enableClustering) {
                        val clusterGeometry = geojsonFeature.geometry()
                        if (clusterGeometry is Point) {
                            val currentZoom = mapView.mapboxMap.cameraState.zoom
                            mapView.mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(clusterGeometry)
                                    .zoom(currentZoom + 2.0)
                                    .build()
                            )
                        }
                    } else {
                        val markerId = geojsonFeature.getStringProperty("markerId")
                        val countryCode = geojsonFeature.getStringProperty("countryCode")

                        val marker = markers.find { it.id == markerId }
                        if (marker != null) {
                            onMarkerClick(marker)
                        } else if (countryCode != null) {
                            onCountryClick(countryCode)
                        }
                    }
                }
            }
        }
        true
    }

    Log.d("SORA_GLOBE", "Added ${features.size} markers with clustering=${config.enableClustering}")
}

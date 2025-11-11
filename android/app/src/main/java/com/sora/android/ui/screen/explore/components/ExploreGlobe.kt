package com.sora.android.ui.screen.explore.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.mapbox.geojson.Point
import com.sora.android.R
import com.sora.android.domain.model.GlobeDataModel
import com.sora.android.ui.components.globe.GlobeConfig
import com.sora.android.ui.components.globe.SoraGlobe
import com.sora.android.ui.components.globe.toGlobeDisplayData

@Composable
fun ExploreGlobe(
    globeData: GlobeDataModel?,
    userLocation: Point?,
    isLoading: Boolean,
    onCountryClick: (String) -> Unit,
    isGlobeInteracting: MutableState<Boolean>
) {
    val config = remember(userLocation) {
        GlobeConfig.explore(userLocation)
    }

    val displayData = remember(globeData) {
        globeData?.toGlobeDisplayData()
    }

    SoraGlobe(
        data = displayData,
        config = config,
        isLoading = isLoading,
        onMarkerClick = { marker ->
            onCountryClick(marker.countryCode)
        },
        onCountryClick = { countryCode ->
            onCountryClick(countryCode)
        },
        isInteracting = isGlobeInteracting,
        emptyMessage = stringResource(R.string.explore_globe_empty)
    )
}

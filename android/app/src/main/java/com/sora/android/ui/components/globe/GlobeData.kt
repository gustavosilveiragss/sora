package com.sora.android.ui.components.globe

data class GlobeMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String,
    val cityName: String? = null,
    val intensity: Double? = null,
    val metadata: Map<String, Any> = emptyMap()
)

data class GlobeCountry(
    val code: String,
    val shouldHighlight: Boolean = true,
    val intensity: Double? = null
)

data class GlobeDisplayData(
    val markers: List<GlobeMarker> = emptyList(),
    val countries: List<GlobeCountry> = emptyList()
)

package com.sora.android.ui.components.globe

import com.sora.android.domain.model.CountryMarkerModel
import com.sora.android.domain.model.GlobeDataModel

fun GlobeDataModel.toGlobeDisplayData(): GlobeDisplayData {
    val markers = mutableListOf<GlobeMarker>()
    val countries = mutableListOf<GlobeCountry>()

    countryMarkers.forEach { countryMarker ->
        countries.add(
            GlobeCountry(
                code = countryMarker.countryCode,
                shouldHighlight = true
            )
        )

        countryMarker.recentPosts.forEach { post ->
            val lat = post.cityLatitude ?: countryMarker.latitude
            val lon = post.cityLongitude ?: countryMarker.longitude

            if (lat != null && lon != null) {
                markers.add(
                    GlobeMarker(
                        id = "post-${post.id}",
                        latitude = lat,
                        longitude = lon,
                        countryCode = countryMarker.countryCode,
                        cityName = post.cityName,
                        metadata = mapOf(
                            "postId" to post.id
                        )
                    )
                )
            }
        }
    }

    return GlobeDisplayData(
        markers = markers,
        countries = countries
    )
}

fun CountryMarkerModel.toGlobeMarker(postIndex: Int = 0): GlobeMarker? {
    val post = recentPosts.getOrNull(postIndex) ?: return null
    val lat = post.cityLatitude ?: latitude ?: return null
    val lon = post.cityLongitude ?: longitude ?: return null

    return GlobeMarker(
        id = "post-${post.id}",
        latitude = lat,
        longitude = lon,
        countryCode = countryCode,
        cityName = post.cityName,
        metadata = mapOf(
            "postId" to post.id
        )
    )
}

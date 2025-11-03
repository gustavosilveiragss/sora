package com.sora.android.domain.model

import android.net.Uri

enum class CreatePostStep {
    MEDIA_SOURCE,
    COUNTRY_SELECTION,
    CITY_SEARCH,
    CATEGORY_SELECTION,
    CAPTION_INPUT,
    REVIEW,
    UPLOADING
}

enum class MediaSource {
    CAMERA,
    GALLERY,
    COLLABORATIVE
}

data class CreatePostState(
    val currentStep: CreatePostStep = CreatePostStep.MEDIA_SOURCE,
    val selectedMediaSource: MediaSource? = null,
    val imageUri: Uri? = null,
    val imageFile: java.io.File? = null,
    val selectedCountry: CountryModel? = null,
    val selectedCity: CitySearchResult? = null,
    val selectedCategory: String = "GENERAL",
    val caption: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val uploadProgress: Float = 0f,
    val isUploading: Boolean = false,
    val detectedLocation: DetectedLocation? = null
)

data class DetectedLocation(
    val latitude: Double,
    val longitude: Double,
    val countryCode: String?,
    val cityName: String?
)

data class CitySearchResult(
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String
)

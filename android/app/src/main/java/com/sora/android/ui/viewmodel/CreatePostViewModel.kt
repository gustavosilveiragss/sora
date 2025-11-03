package com.sora.android.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.R
import com.sora.android.core.util.PermissionManager
import com.sora.android.data.repository.LocationRepositoryImpl
import com.sora.android.domain.model.*
import com.sora.android.domain.repository.CountryRepository
import com.sora.android.domain.repository.LocationRepository
import com.sora.android.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val countryRepository: CountryRepository,
    private val locationRepository: LocationRepository,
    private val locationRepositoryImpl: LocationRepositoryImpl,
    private val permissionManager: PermissionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CreatePostState())
    val state: StateFlow<CreatePostState> = _state.asStateFlow()

    private val _countries = MutableStateFlow<List<CountryModel>>(emptyList())
    val countries: StateFlow<List<CountryModel>> = _countries.asStateFlow()

    private val _citySearchResults = MutableStateFlow<List<SearchLocationModel>>(emptyList())
    val citySearchResults: StateFlow<List<SearchLocationModel>> = _citySearchResults.asStateFlow()

    private val _citySearchQuery = MutableStateFlow("")
    val citySearchQuery: StateFlow<String> = _citySearchQuery.asStateFlow()

    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    fun selectMediaSource(source: MediaSource) {
        Log.d("SORA_CREATE_POST", "Media source selecionada: $source")
        _state.update { it.copy(selectedMediaSource = source) }
    }

    fun setImage(uri: Uri?, file: java.io.File?) {
        Log.d("SORA_CREATE_POST", "Imagem definida: uri=$uri, file=${file?.absolutePath}")
        _state.update {
            it.copy(
                imageUri = uri,
                imageFile = file
            )
        }
    }

    fun nextStep() {
        val currentState = _state.value
        val currentStep = currentState.currentStep

        Log.d("SORA_CREATE_POST", "Avancando do passo: $currentStep")

        val nextStep = when (currentStep) {
            CreatePostStep.MEDIA_SOURCE -> {
                if (currentState.imageUri != null) {
                    CreatePostStep.COUNTRY_SELECTION
                } else {
                    Log.w("SORA_CREATE_POST", "Nao pode avancar sem imagem selecionada")
                    return
                }
            }
            CreatePostStep.COUNTRY_SELECTION -> {
                if (currentState.selectedCountry != null) {
                    CreatePostStep.CITY_SEARCH
                } else {
                    Log.w("SORA_CREATE_POST", "Nao pode avancar sem pais selecionado")
                    return
                }
            }
            CreatePostStep.CITY_SEARCH -> {
                if (currentState.selectedCity != null) {
                    CreatePostStep.CATEGORY_SELECTION
                } else {
                    Log.w("SORA_CREATE_POST", "Nao pode avancar sem cidade selecionada")
                    return
                }
            }
            CreatePostStep.CATEGORY_SELECTION -> CreatePostStep.CAPTION_INPUT
            CreatePostStep.CAPTION_INPUT -> CreatePostStep.REVIEW
            CreatePostStep.REVIEW -> {
                Log.d("SORA_CREATE_POST", "Iniciando upload do post")
                createPost()
                return
            }
            CreatePostStep.UPLOADING -> return
        }

        _state.update { it.copy(currentStep = nextStep) }
        Log.d("SORA_CREATE_POST", "Passo atualizado para: $nextStep")

        if (nextStep == CreatePostStep.COUNTRY_SELECTION && _countries.value.isEmpty()) {
            loadCountries()
        }

        if (nextStep == CreatePostStep.CITY_SEARCH) {
            autoSelectCityIfDetected()
        }
    }

    private fun autoSelectCityIfDetected() {
        val detectedLoc = _state.value.detectedLocation
        if (detectedLoc != null && detectedLoc.cityName != null) {
            Log.d("SORA_CREATE_POST", "Auto-selecionando cidade detectada: ${detectedLoc.cityName}")
            val cityName = extractCityName(detectedLoc.cityName)
            val cityResult = CitySearchResult(
                name = cityName,
                displayName = detectedLoc.cityName,
                latitude = detectedLoc.latitude,
                longitude = detectedLoc.longitude,
                countryCode = detectedLoc.countryCode ?: _state.value.selectedCountry?.code ?: ""
            )
            _state.update { it.copy(selectedCity = cityResult) }
            _citySearchQuery.value = ""
            _citySearchResults.value = emptyList()
            Log.d("SORA_CREATE_POST", "Cidade auto-selecionada com sucesso: $cityName")
        }
    }

    fun previousStep() {
        val currentState = _state.value
        val currentStep = currentState.currentStep

        Log.d("SORA_CREATE_POST", "Voltando do passo: $currentStep")

        if (currentStep == CreatePostStep.MEDIA_SOURCE) {
            Log.d("SORA_CREATE_POST", "Ja no primeiro passo, cancelando")
            cancelPostCreation()
            return
        }

        val previousStep = when (currentStep) {
            CreatePostStep.COUNTRY_SELECTION -> CreatePostStep.MEDIA_SOURCE
            CreatePostStep.CITY_SEARCH -> CreatePostStep.COUNTRY_SELECTION
            CreatePostStep.CATEGORY_SELECTION -> CreatePostStep.CITY_SEARCH
            CreatePostStep.CAPTION_INPUT -> CreatePostStep.CATEGORY_SELECTION
            CreatePostStep.REVIEW -> CreatePostStep.CAPTION_INPUT
            CreatePostStep.UPLOADING -> return
            CreatePostStep.MEDIA_SOURCE -> CreatePostStep.MEDIA_SOURCE
        }

        _state.update { it.copy(currentStep = previousStep) }
        Log.d("SORA_CREATE_POST", "Passo atualizado para: $previousStep")
    }

    fun goToStep(step: CreatePostStep) {
        Log.d("SORA_CREATE_POST", "Indo diretamente para o passo: $step")
        _state.update { it.copy(currentStep = step) }
    }

    fun loadCountries() {
        viewModelScope.launch {
            try {
                Log.d("SORA_CREATE_POST", "Carregando paises")
                _state.update { it.copy(isLoading = true, error = null) }

                countryRepository.getAllCountries()
                    .catch { e ->
                        Log.e("SORA_CREATE_POST", "Erro ao carregar paises: ${e.message}", e)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = context.getString(R.string.error_loading_countries)
                            )
                        }
                    }
                    .collect { countriesList ->
                        Log.d("SORA_CREATE_POST", "Paises carregados: ${countriesList.size}")
                        _countries.value = countriesList
                        _state.update { it.copy(isLoading = false) }
                    }
            } catch (e: Exception) {
                Log.e("SORA_CREATE_POST", "Excecao ao carregar paises: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.error_loading_countries)
                    )
                }
            }
        }
    }

    fun selectCountry(country: CountryModel) {
        Log.d("SORA_CREATE_POST", "Pais selecionado: ${country.code}")
        _state.update {
            it.copy(
                selectedCountry = country,
                selectedCity = null
            )
        }
        _citySearchResults.value = emptyList()
        _citySearchQuery.value = ""
    }

    fun searchCities(query: String) {
        _citySearchQuery.value = query

        searchJob?.cancel()

        if (query.length < 2) {
            _citySearchResults.value = emptyList()
            return
        }

        val countryCode = _state.value.selectedCountry?.code
        if (countryCode == null) {
            Log.w("SORA_CREATE_POST", "Nenhum pais selecionado para buscar cidades")
            return
        }

        searchJob = viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(500)

                Log.d("SORA_CREATE_POST", "Buscando cidades: query=$query, country=$countryCode")
                _state.update { it.copy(isLoading = true) }

                locationRepository.searchCitiesInCountry(countryCode, query, 20)
                    .catch { e ->
                        Log.e("SORA_CREATE_POST", "Erro ao buscar cidades: ${e.message}", e)
                        _citySearchResults.value = emptyList()
                        _state.update { it.copy(isLoading = false) }
                    }
                    .collect { cities ->
                        Log.d("SORA_CREATE_POST", "Cidades encontradas: ${cities.size}")
                        _citySearchResults.value = cities
                        _state.update { it.copy(isLoading = false) }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d("SORA_CREATE_POST", "Busca de cidades cancelada")
            } catch (e: Exception) {
                Log.e("SORA_CREATE_POST", "Excecao ao buscar cidades: ${e.message}", e)
                _citySearchResults.value = emptyList()
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectCity(city: SearchLocationModel) {
        Log.d("SORA_CREATE_POST", "Cidade selecionada: ${city.displayName}")
        val cityName = extractCityName(city.displayName)
        val cityResult = CitySearchResult(
            name = cityName,
            displayName = city.displayName,
            latitude = city.latitude,
            longitude = city.longitude,
            countryCode = city.countryCode ?: _state.value.selectedCountry?.code ?: ""
        )
        _state.update { it.copy(selectedCity = cityResult) }
        Log.d("SORA_CREATE_POST", "Nome da cidade extraido: $cityName")
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            try {
                Log.d("SORA_CREATE_POST", "Obtendo localizacao atual para cidade")
                _isLoadingLocation.value = true
                _state.update { it.copy(isLoading = true, error = null) }

                val currentState = _state.value
                val detectedLoc = currentState.detectedLocation

                if (detectedLoc != null && detectedLoc.cityName != null) {
                    Log.d("SORA_CREATE_POST", "Reutilizando cidade detectada anteriormente: ${detectedLoc.cityName}")

                    val cityName = extractCityName(detectedLoc.cityName)
                    val cityResult = CitySearchResult(
                        name = cityName,
                        displayName = detectedLoc.cityName,
                        latitude = detectedLoc.latitude,
                        longitude = detectedLoc.longitude,
                        countryCode = detectedLoc.countryCode ?: currentState.selectedCountry?.code ?: ""
                    )

                    _state.update {
                        it.copy(
                            selectedCity = cityResult,
                            isLoading = false
                        )
                    }
                    _isLoadingLocation.value = false
                    Log.d("SORA_CREATE_POST", "Cidade reutilizada com sucesso, pulando GPS e reverse geocode")
                    return@launch
                }

                val location = if (detectedLoc != null) {
                    Log.d("SORA_CREATE_POST", "Reutilizando coordenadas detectadas anteriormente (sem cityName)")
                    val mockLocation = android.location.Location("").apply {
                        latitude = detectedLoc.latitude
                        longitude = detectedLoc.longitude
                    }
                    mockLocation
                } else {
                    Log.d("SORA_CREATE_POST", "Obtendo nova localizacao GPS")
                    locationRepositoryImpl.getCurrentLocation()
                }

                if (location == null) {
                    Log.w("SORA_CREATE_POST", "Nao foi possivel obter localizacao")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = context.getString(R.string.error_location_unavailable)
                        )
                    }
                    _isLoadingLocation.value = false
                    return@launch
                }

                Log.d("SORA_CREATE_POST", "Fazendo reverse geocode: ${location.latitude}, ${location.longitude}")

                locationRepository.reverseGeocode(location.latitude, location.longitude)
                    .catch { e ->
                        Log.e("SORA_CREATE_POST", "Erro no reverse geocode: ${e.message}", e)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = context.getString(R.string.error_reverse_geocode)
                            )
                        }
                        _isLoadingLocation.value = false
                    }
                    .collect { result ->
                        Log.d("SORA_CREATE_POST", "Reverse geocode: ${result.displayName}")

                        val cityName = result.cityName?.takeIf { it.isNotBlank() }
                            ?: extractCityName(result.displayName)

                        val cityResult = CitySearchResult(
                            name = cityName,
                            displayName = result.displayName,
                            latitude = result.latitude,
                            longitude = result.longitude,
                            countryCode = result.countryCode ?: ""
                        )

                        _state.update {
                            it.copy(
                                selectedCity = cityResult,
                                isLoading = false
                            )
                        }
                        _isLoadingLocation.value = false

                        Log.d("SORA_CREATE_POST", "Cidade da localizacao: ${cityResult.name}")
                    }
            } catch (e: Exception) {
                Log.e("SORA_CREATE_POST", "Excecao ao usar localizacao atual: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.error_location_unavailable)
                    )
                }
                _isLoadingLocation.value = false
            }
        }
    }

    fun useCurrentLocationForCountry() {
        viewModelScope.launch {
            try {
                Log.d("SORA_CREATE_POST", "Obtendo localizacao atual para detectar pais")
                _isLoadingLocation.value = true
                _state.update { it.copy(isLoading = true, error = null) }

                val location = locationRepositoryImpl.getCurrentLocation()

                if (location == null) {
                    Log.w("SORA_CREATE_POST", "Nao foi possivel obter localizacao")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = context.getString(R.string.error_location_unavailable)
                        )
                    }
                    _isLoadingLocation.value = false
                    return@launch
                }

                Log.d("SORA_CREATE_POST", "Localizacao GPS obtida: ${location.latitude}, ${location.longitude}")

                locationRepository.reverseGeocode(location.latitude, location.longitude)
                    .catch { e ->
                        Log.e("SORA_CREATE_POST", "Erro no reverse geocode: ${e.message}", e)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = context.getString(R.string.error_reverse_geocode)
                            )
                        }
                        _isLoadingLocation.value = false
                    }
                    .collect { result ->
                        Log.d("SORA_CREATE_POST", "Reverse geocode: ${result.displayName}")

                        val countryCode = result.countryCode
                        if (countryCode != null) {
                            val matchingCountry = _countries.value.find {
                                it.code.equals(countryCode, ignoreCase = true)
                            }

                            if (matchingCountry != null) {
                                Log.d("SORA_CREATE_POST", "Pais detectado: ${matchingCountry.code}")
                                val detectedLocation = DetectedLocation(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    countryCode = countryCode,
                                    cityName = result.cityName
                                )
                                _state.update {
                                    it.copy(
                                        selectedCountry = matchingCountry,
                                        isLoading = false,
                                        detectedLocation = detectedLocation
                                    )
                                }
                                _isLoadingLocation.value = false
                                Log.d("SORA_CREATE_POST", "Localizacao armazenada para reutilizacao")
                                nextStep()
                            } else {
                                Log.w("SORA_CREATE_POST", "Pais $countryCode nao encontrado na lista")
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        error = context.getString(R.string.error_country_not_found)
                                    )
                                }
                                _isLoadingLocation.value = false
                            }
                        } else {
                            Log.w("SORA_CREATE_POST", "Codigo do pais nao encontrado no resultado")
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = context.getString(R.string.error_country_not_detected)
                                )
                            }
                            _isLoadingLocation.value = false
                        }
                    }
            } catch (e: Exception) {
                Log.e("SORA_CREATE_POST", "Excecao ao detectar pais: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.error_location_unavailable)
                    )
                }
                _isLoadingLocation.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        Log.d("SORA_CREATE_POST", "Categoria selecionada: $category")
        _state.update { it.copy(selectedCategory = category) }
    }

    fun updateCaption(caption: String) {
        _state.update { it.copy(caption = caption) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun createPost() {
        viewModelScope.launch {
            try {
                val currentState = _state.value

                if (currentState.imageUri == null) {
                    Log.e("SORA_CREATE_POST", "Tentativa de criar post sem imagem")
                    _state.update {
                        it.copy(error = context.getString(R.string.error_no_image_selected))
                    }
                    return@launch
                }

                if (currentState.selectedCountry == null || currentState.selectedCity == null) {
                    Log.e("SORA_CREATE_POST", "Tentativa de criar post sem pais ou cidade")
                    _state.update {
                        it.copy(error = context.getString(R.string.error_missing_location))
                    }
                    return@launch
                }

                Log.d("SORA_CREATE_POST", "Iniciando criacao do post")
                _state.update {
                    it.copy(
                        currentStep = CreatePostStep.UPLOADING,
                        isUploading = true,
                        uploadProgress = 0f,
                        error = null
                    )
                }

                val request = PostCreateRequest(
                    countryCode = currentState.selectedCountry.code,
                    collectionCode = currentState.selectedCategory,
                    cityName = currentState.selectedCity.name,
                    cityLatitude = currentState.selectedCity.latitude,
                    cityLongitude = currentState.selectedCity.longitude,
                    caption = currentState.caption.ifBlank { null },
                    sharingOption = SharingOption.PERSONAL_ONLY,
                    profileOwnerId = null,
                    visibilityOption = null
                )

                Log.d("SORA_CREATE_POST", "Criando post: $request")
                _state.update { it.copy(uploadProgress = 0.3f) }

                val postResult = postRepository.createPost(request)

                postResult.fold(
                    onSuccess = { posts ->
                        if (posts.isEmpty()) {
                            Log.e("SORA_CREATE_POST", "Nenhum post retornado da API")
                            _state.update {
                                it.copy(
                                    isUploading = false,
                                    error = context.getString(R.string.error_post_creation_failed)
                                )
                            }
                            return@launch
                        }

                        val post = posts.first()
                        Log.d("SORA_CREATE_POST", "Post criado com ID: ${post.id}")
                        _state.update { it.copy(uploadProgress = 0.5f) }

                        Log.d("SORA_CREATE_POST", "Fazendo upload da imagem")
                        val uploadResult = postRepository.uploadPostMedia(
                            post.id,
                            listOf(currentState.imageUri.toString())
                        )

                        uploadResult.fold(
                            onSuccess = { uploadResponse ->
                                Log.d("SORA_CREATE_POST", "Upload concluido: ${uploadResponse.media.size} medias")
                                _state.update {
                                    it.copy(
                                        uploadProgress = 1f,
                                        isUploading = false
                                    )
                                }

                                Log.d("SORA_CREATE_POST", "Post criado com sucesso!")
                            },
                            onFailure = { error ->
                                Log.e("SORA_CREATE_POST", "Erro no upload: ${error.message}", error)
                                _state.update {
                                    it.copy(
                                        isUploading = false,
                                        uploadProgress = 0f,
                                        error = error.message ?: context.getString(R.string.error_upload_failed)
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e("SORA_CREATE_POST", "Erro na criacao do post: ${error.message}", error)
                        _state.update {
                            it.copy(
                                isUploading = false,
                                uploadProgress = 0f,
                                error = error.message ?: context.getString(R.string.error_post_creation_failed)
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("SORA_CREATE_POST", "Excecao ao criar post: ${e.message}", e)
                _state.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = 0f,
                        error = e.message ?: context.getString(R.string.error_unknown)
                    )
                }
            }
        }
    }

    fun cancelPostCreation() {
        Log.d("SORA_CREATE_POST", "Cancelando criacao de post")
        resetState()
    }

    private fun resetState() {
        _state.value = CreatePostState()
        _countries.value = emptyList()
        _citySearchResults.value = emptyList()
        _citySearchQuery.value = ""
        _isLoadingLocation.value = false
    }

    private fun extractCityName(displayName: String): String {
        val parts = displayName.split(",").map { it.trim() }
        return when {
            parts.isEmpty() -> displayName.take(100)
            parts.size == 1 -> parts[0].take(100)
            else -> parts[0].take(100)
        }
    }
}

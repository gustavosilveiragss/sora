package com.sora.android.ui.screens.createpost

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sora.android.R
import com.sora.android.core.translation.TranslationManager
import com.sora.android.core.util.PermissionManager
import com.sora.android.domain.model.CountryModel
import com.sora.android.ui.viewmodel.CreatePostViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun CountrySelectionStep(
    viewModel: CreatePostViewModel,
    permissionManager: PermissionManager,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    val countries by viewModel.countries.collectAsState()
    val state by viewModel.state.collectAsState()
    val isLoadingLocation by viewModel.isLoadingLocation.collectAsState()
    val context = LocalContext.current

    val translationManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TranslationManagerEntryPoint::class.java
        ).translationManager()
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(countries, searchQuery) {
        if (searchQuery.isBlank()) {
            countries
        } else {
            countries.filter { country ->
                val countryName = translationManager.translateAny(country.nameKey, country.code)
                countryName.contains(searchQuery, ignoreCase = true) ||
                        country.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (countries.isEmpty()) {
            viewModel.loadCountries()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            placeholder = { Text(stringResource(R.string.create_post_search_country)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        TextButton(
            onClick = {
                if (permissionManager.hasLocationPermission(context)) {
                    viewModel.useCurrentLocationForCountry()
                } else {
                    locationPermissionLauncher.launch(PermissionManager.LOCATION_PERMISSIONS)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = !isLoadingLocation
        ) {
            if (isLoadingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = stringResource(R.string.create_post_use_current_location),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        when {
            state.isLoading && countries.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            filteredCountries.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(filteredCountries) { country ->
                        CountryItem(
                            country = country,
                            isSelected = state.selectedCountry?.code == country.code,
                            translationManager = translationManager,
                            onClick = {
                                viewModel.selectCountry(country)
                                viewModel.nextStep()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: CountryModel,
    isSelected: Boolean,
    translationManager: TranslationManager,
    onClick: () -> Unit
) {
    val countryName = remember(country) {
        translationManager.translateAny(country.nameKey, country.code)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = countryName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TranslationManagerEntryPoint {
    fun translationManager(): TranslationManager
}

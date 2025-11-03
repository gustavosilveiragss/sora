package com.sora.android.ui.screens.createpost

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.sora.android.core.util.PermissionManager
import com.sora.android.domain.model.SearchLocationModel
import com.sora.android.ui.viewmodel.CreatePostViewModel

@Composable
fun CitySearchStep(
    viewModel: CreatePostViewModel,
    permissionManager: PermissionManager,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    val citySearchResults by viewModel.citySearchResults.collectAsState()
    val citySearchQuery by viewModel.citySearchQuery.collectAsState()
    val isLoadingLocation by viewModel.isLoadingLocation.collectAsState()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = citySearchQuery,
            onValueChange = { viewModel.searchCities(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            placeholder = { Text(stringResource(R.string.create_post_search_city)) },
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
                    viewModel.useCurrentLocation()
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
            state.selectedCity != null && citySearchQuery.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.selectedCity!!.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = { viewModel.nextStep() }) {
                            Text(
                                text = stringResource(R.string.next),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            citySearchQuery.length < 2 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.min_search_length),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            citySearchResults.isEmpty() -> {
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
                    items(citySearchResults) { city ->
                        CityItem(
                            city = city,
                            onClick = {
                                viewModel.selectCity(city)
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
private fun CityItem(
    city: SearchLocationModel,
    onClick: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (city.countryName != null) {
                    Text(
                        text = city.countryName!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

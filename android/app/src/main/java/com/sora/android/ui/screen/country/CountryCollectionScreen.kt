package com.sora.android.ui.screen.country

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.sora.android.R
import com.sora.android.core.translation.TranslationManager
import com.sora.android.ui.components.FilterablePostList
import com.sora.android.ui.components.SoraScaffold
import com.sora.android.ui.theme.SoraIcons
import com.sora.android.ui.theme.SoraTextPrimary
import com.sora.android.ui.theme.SoraTextSecondary
import com.sora.android.ui.viewmodel.CountryCollectionViewModel
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCollectionScreen(
    userId: Long,
    countryCode: String,
    viewModel: CountryCollectionViewModel = hiltViewModel(),
    onPostClick: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    val context = LocalContext.current

    val translationManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TranslationManagerEntryPoint::class.java
        ).translationManager()
    }

    SoraScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.country?.let {
                            translationManager.translateAny(it.nameKey, it.code)
                        } ?: stringResource(R.string.loading),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = SoraIcons.ArrowLeft,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = SoraIcons.MoreVertical,
                            contentDescription = stringResource(R.string.more_options)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.country == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null && uiState.country == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.error_loading_stats),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = { viewModel.retry() }) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }
            } else {
                uiState.country?.let { country ->
                    FilterablePostList(
                        posts = posts,
                        filters = filters,
                        currentUserId = uiState.currentUserId,
                        postsLoaded = uiState.postsLoaded,
                        onFilterChange = viewModel::updateFilters,
                        onLikeClick = { postId, isLiked, likesCount ->
                            viewModel.toggleLike(postId, isLiked, likesCount)
                        },
                        onCommentClick = { },
                        onProfileClick = onProfileClick,
                        showFilters = true,
                        modifier = Modifier.weight(1f),
                        header = {
                            CountryHeader(
                                countryName = translationManager.translateAny(country.nameKey, country.code),
                                username = uiState.user?.username ?: "",
                                visitInfo = uiState.visitInfo
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryHeader(
    countryName: String,
    username: String,
    visitInfo: com.sora.android.domain.model.CountryVisitInfoModel?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = countryName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = SoraTextPrimary
        )

        visitInfo?.let { info ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                if (info.totalPostsCount > 0) {
                    StatColumn(
                        value = info.totalPostsCount.toString(),
                        label = stringResource(R.string.posts)
                    )
                }

                val citiesCount = info.citiesVisited?.size ?: 1
                StatColumn(
                    value = citiesCount.toString(),
                    label = stringResource(R.string.cities)
                )

                info.firstVisitDate?.let { firstVisit ->
                    StatColumn(
                        value = formatDate(firstVisit),
                        label = stringResource(R.string.country_collection_first_visit).split(":")[0].trim()
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = SoraTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SoraTextSecondary
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface TranslationManagerEntryPoint {
    fun translationManager(): TranslationManager
}

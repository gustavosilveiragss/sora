package com.sora.android.ui.screens.createpost

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sora.android.R
import com.sora.android.core.util.CameraManager
import com.sora.android.core.util.PermissionManager
import com.sora.android.domain.model.CreatePostStep
import com.sora.android.domain.model.MediaSource
import com.sora.android.ui.viewmodel.CreatePostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostBottomSheet(
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel(),
    cameraManager: CameraManager,
    permissionManager: PermissionManager
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val sheetHeight = screenHeight * 0.7f

    var currentImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageFile by remember { mutableStateOf<java.io.File?>(null) }

    val locationPermissionLauncherForCity = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            viewModel.useCurrentLocation()
        }
    }

    val locationPermissionLauncherForCountry = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            viewModel.useCurrentLocationForCountry()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setImage(it, null)
            viewModel.nextStep()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentImageUri != null) {
            viewModel.setImage(currentImageUri, currentImageFile)
            viewModel.nextStep()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            val (file, uri) = cameraManager.createImageFile(context)
            currentImageFile = file
            currentImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            galleryLauncher.launch("image/*")
        }
    }

    LaunchedEffect(state.uploadProgress) {
        if (state.uploadProgress >= 1f && !state.isUploading && state.error == null) {
            onPostCreated()
            viewModel.cancelPostCreation()
            onDismiss()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelPostCreation()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.cancelPostCreation()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .padding(horizontal = 12.dp)
        ) {
            InstagramStyleTopBar(
                currentStep = state.currentStep,
                onClose = {
                    if (!state.isUploading) {
                        viewModel.cancelPostCreation()
                        onDismiss()
                    }
                },
                onNext = {
                    if (state.isUploading) return@InstagramStyleTopBar
                    when (state.currentStep) {
                        CreatePostStep.CATEGORY_SELECTION -> {
                            if (state.selectedCategory.isNotEmpty()) viewModel.nextStep()
                        }
                        CreatePostStep.CAPTION_INPUT -> viewModel.nextStep()
                        else -> {}
                    }
                },
                canProceed = when (state.currentStep) {
                    CreatePostStep.CATEGORY_SELECTION -> state.selectedCategory.isNotEmpty()
                    CreatePostStep.CAPTION_INPUT -> true
                    else -> false
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state.error != null) {
                ErrorBanner(
                    message = state.error!!,
                    onDismiss = { viewModel.clearError() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (state.currentStep) {
                CreatePostStep.MEDIA_SOURCE -> {
                    MediaSourceSelectionStep(
                        selectedSource = state.selectedMediaSource,
                        onSourceSelected = { source ->
                            viewModel.selectMediaSource(source)
                            when (source) {
                                MediaSource.CAMERA -> {
                                    if (permissionManager.hasCameraPermission(context)) {
                                        val (file, uri) = cameraManager.createImageFile(context)
                                        currentImageFile = file
                                        currentImageUri = uri
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(PermissionManager.CAMERA_PERMISSIONS)
                                    }
                                }
                                MediaSource.GALLERY -> {
                                    if (permissionManager.hasGalleryPermission(context)) {
                                        galleryLauncher.launch("image/*")
                                    } else {
                                        galleryPermissionLauncher.launch(PermissionManager.GALLERY_PERMISSIONS)
                                    }
                                }
                                MediaSource.COLLABORATIVE -> {
                                }
                            }
                        }
                    )
                }

                CreatePostStep.COUNTRY_SELECTION -> {
                    CountrySelectionStep(
                        viewModel = viewModel,
                        permissionManager = permissionManager,
                        locationPermissionLauncher = locationPermissionLauncherForCountry
                    )
                }

                CreatePostStep.CITY_SEARCH -> {
                    CitySearchStep(
                        viewModel = viewModel,
                        permissionManager = permissionManager,
                        locationPermissionLauncher = locationPermissionLauncherForCity
                    )
                }

                CreatePostStep.CATEGORY_SELECTION -> {
                    CategorySelectionStep(
                        selectedCategory = state.selectedCategory,
                        onCategorySelected = viewModel::selectCategory
                    )
                }

                CreatePostStep.CAPTION_INPUT -> {
                    CaptionInputStep(
                        caption = state.caption,
                        onCaptionChanged = viewModel::updateCaption
                    )
                }

                CreatePostStep.REVIEW -> {
                    ReviewStep(
                        state = state,
                        onPublish = { viewModel.nextStep() }
                    )
                }

                CreatePostStep.UPLOADING -> {
                    UploadingStep(
                        progress = state.uploadProgress
                    )
                }
            }
        }
    }
}

@Composable
private fun InstagramStyleTopBar(
    currentStep: CreatePostStep,
    onClose: () -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onClose) {
            Text(
                text = stringResource(R.string.cancel),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Text(
            text = when (currentStep) {
                CreatePostStep.MEDIA_SOURCE -> stringResource(R.string.create_post_select_media)
                CreatePostStep.COUNTRY_SELECTION -> stringResource(R.string.create_post_select_country)
                CreatePostStep.CITY_SEARCH -> stringResource(R.string.create_post_select_city)
                CreatePostStep.CATEGORY_SELECTION -> stringResource(R.string.create_post_select_category)
                CreatePostStep.CAPTION_INPUT -> stringResource(R.string.create_post_add_caption)
                CreatePostStep.REVIEW -> stringResource(R.string.create_post_review)
                CreatePostStep.UPLOADING -> stringResource(R.string.create_post_uploading)
            },
            style = MaterialTheme.typography.titleMedium
        )

        if (canProceed) {
            TextButton(onClick = onNext) {
                Text(
                    text = stringResource(R.string.next),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}


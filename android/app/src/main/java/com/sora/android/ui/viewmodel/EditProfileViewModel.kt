package com.sora.android.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.R
import com.sora.android.data.remote.dto.UpdateProfileRequest
import com.sora.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val bio: String = "",
    val profilePicture: String? = null,
    val localImageUri: String? = null,
    val isLoading: Boolean = false,
    val isPictureUploading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val errors: Map<String, String> = emptyMap(),
    val isValid: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            userRepository.getCurrentUserProfile().fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        firstName = user.firstName,
                        lastName = user.lastName,
                        username = user.username,
                        bio = user.bio ?: "",
                        profilePicture = user.profilePicture,
                        isLoading = false
                    )
                    validateForm()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName)
        validateForm()
    }

    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(lastName = lastName)
        validateForm()
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
        validateForm()
    }

    fun updateBio(bio: String) {
        _uiState.value = _uiState.value.copy(bio = bio)
        validateForm()
    }

    fun updateProfilePicture(imageUri: String) {
        _uiState.value = _uiState.value.copy(
            localImageUri = imageUri,
            isPictureUploading = true,
            error = null
        )

        viewModelScope.launch {
            userRepository.uploadProfilePicture(imageUri).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        profilePicture = response.profilePictureUrl,
                        localImageUri = null,
                        isPictureUploading = false,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: context.getString(R.string.error_upload_image),
                        isPictureUploading = false
                    )
                }
            )
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = UpdateProfileRequest(
                firstName = _uiState.value.firstName,
                lastName = _uiState.value.lastName,
                username = _uiState.value.username,
                bio = _uiState.value.bio
            )

            userRepository.updateUserProfile(request).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun validateForm() {
        val errors = mutableMapOf<String, String>()

        if (_uiState.value.firstName.isBlank()) {
            errors["firstName"] = context.getString(R.string.validation_first_name_required)
        } else if (_uiState.value.firstName.length < 2) {
            errors["firstName"] = context.getString(R.string.validation_first_name_min_length)
        }

        if (_uiState.value.lastName.isBlank()) {
            errors["lastName"] = context.getString(R.string.validation_last_name_required)
        } else if (_uiState.value.lastName.length < 2) {
            errors["lastName"] = context.getString(R.string.validation_last_name_min_length)
        }

        if (_uiState.value.username.isBlank()) {
            errors["username"] = context.getString(R.string.validation_username_required_edit)
        } else if (_uiState.value.username.length < 3) {
            errors["username"] = context.getString(R.string.validation_username_min_length_edit)
        } else if (!_uiState.value.username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            errors["username"] = context.getString(R.string.validation_username_invalid_chars_edit)
        }

        if (_uiState.value.bio.length > 500) {
            errors["bio"] = context.getString(R.string.validation_bio_max_length)
        }

        _uiState.value = _uiState.value.copy(
            errors = errors,
            isValid = errors.isEmpty()
        )
    }
}
package com.sora.android.ui.screens.createpost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sora.android.R
import com.sora.android.domain.model.CreatePostState

@Composable
fun ReviewStep(
    state: CreatePostState,
    onPublish: () -> Unit
) {
    val context = LocalContext.current

    val countryName = remember(state.selectedCountry) {
        state.selectedCountry?.let { country ->
            val resId = context.resources.getIdentifier(
                country.nameKey,
                "string",
                context.packageName
            )
            if (resId != 0) {
                context.getString(resId)
            } else {
                country.code
            }
        } ?: ""
    }

    val categoryName = remember(state.selectedCategory) {
        when (state.selectedCategory) {
            "GENERAL" -> context.getString(R.string.create_post_category_general)
            "CULINARY" -> context.getString(R.string.create_post_category_culinary)
            "EVENTS" -> context.getString(R.string.create_post_category_events)
            "OTHERS" -> context.getString(R.string.create_post_category_others)
            else -> state.selectedCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.imageUri != null) {
            AsyncImage(
                model = state.imageUri,
                contentDescription = stringResource(R.string.post_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReviewItem(
                label = stringResource(R.string.create_post_country_label),
                value = countryName
            )

            ReviewItem(
                label = stringResource(R.string.create_post_city_label),
                value = state.selectedCity?.name ?: ""
            )

            ReviewItem(
                label = stringResource(R.string.create_post_category_label),
                value = categoryName
            )

            if (state.caption.isNotBlank()) {
                ReviewItem(
                    label = stringResource(R.string.create_post_caption_label),
                    value = state.caption
                )
            }
        }

        Button(
            onClick = onPublish,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            enabled = !state.isUploading,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(R.string.create_post_publish),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ReviewItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

package com.sora.android.ui.screens.createpost

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sora.android.R

@Composable
fun CategorySelectionStep(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)
    ) {
        CategoryCard(
            icon = Icons.Default.Public,
            title = stringResource(R.string.create_post_category_general),
            categoryCode = "GENERAL",
            isSelected = selectedCategory == "GENERAL",
            onClick = { onCategorySelected("GENERAL") }
        )

        CategoryCard(
            icon = Icons.Default.Restaurant,
            title = stringResource(R.string.create_post_category_culinary),
            categoryCode = "CULINARY",
            isSelected = selectedCategory == "CULINARY",
            onClick = { onCategorySelected("CULINARY") }
        )

        CategoryCard(
            icon = Icons.Default.Event,
            title = stringResource(R.string.create_post_category_events),
            categoryCode = "EVENTS",
            isSelected = selectedCategory == "EVENTS",
            onClick = { onCategorySelected("EVENTS") }
        )

        CategoryCard(
            icon = Icons.Default.MoreHoriz,
            title = stringResource(R.string.create_post_category_others),
            categoryCode = "OTHERS",
            isSelected = selectedCategory == "OTHERS",
            onClick = { onCategorySelected("OTHERS") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    categoryCode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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

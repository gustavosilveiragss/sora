package com.sora.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sora.android.R
import com.sora.android.domain.model.CollectionCode
import com.sora.android.ui.theme.CollectionIcons
import com.sora.android.ui.theme.SoraBackground
import com.sora.android.ui.theme.SoraPrimary
import com.sora.android.ui.theme.SoraTextPrimary
import com.sora.android.ui.theme.SoraTextSecondary

data class CollectionTab(
    val code: CollectionCode?,
    val labelResId: Int
)

@Composable
fun CollectionFilterTabRow(
    selectedCollection: CollectionCode?,
    onCollectionSelected: (CollectionCode?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            CollectionTab(null, R.string.collection_filter_all),
            CollectionTab(CollectionCode.GENERAL, R.string.collection_filter_general),
            CollectionTab(CollectionCode.CULINARY, R.string.collection_filter_culinary),
            CollectionTab(CollectionCode.EVENTS, R.string.collection_filter_events),
            CollectionTab(CollectionCode.OTHERS, R.string.collection_filter_others)
        )
    }

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.code == selectedCollection },
        modifier = modifier.fillMaxWidth(),
        containerColor = SoraBackground,
        contentColor = SoraPrimary,
        edgePadding = 16.dp,
        indicator = {},
        divider = {}
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.code == selectedCollection

            Tab(
                selected = isSelected,
                onClick = { onCollectionSelected(tab.code) },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .height(36.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) SoraPrimary else SoraBackground,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CollectionIcons.getIcon(tab.code),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else SoraTextSecondary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = stringResource(tab.labelResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else SoraTextPrimary
                        )
                    }
                }
            }
        }
    }
}

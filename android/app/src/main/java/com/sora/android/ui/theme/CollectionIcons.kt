package com.sora.android.ui.theme

import androidx.compose.ui.graphics.vector.ImageVector
import com.sora.android.domain.model.CollectionCode

object CollectionIcons {
    fun getIcon(code: CollectionCode?): ImageVector {
        return when (code) {
            null -> SoraIcons.Grid
            CollectionCode.GENERAL -> SoraIcons.Photo
            CollectionCode.CULINARY -> SoraIcons.Restaurant
            CollectionCode.EVENTS -> SoraIcons.Event
            CollectionCode.OTHERS -> SoraIcons.MoreDots
        }
    }
}

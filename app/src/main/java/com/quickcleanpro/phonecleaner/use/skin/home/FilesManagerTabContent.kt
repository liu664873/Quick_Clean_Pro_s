package com.quickcleanpro.phonecleaner.use.skin.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.feature.iconRes
import com.quickcleanpro.phonecleaner.use.core.feature.destinationOrNull
import com.quickcleanpro.phonecleaner.use.core.feature.titleRes
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXIconTile
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXGridSpacing

private val FileManagerFeatures =
    listOf(
        FeatureKey.PHOTOS,
        FeatureKey.SIMILAR_PHOTOS,
        FeatureKey.PHOTO_PRIVACY,
        FeatureKey.SCREENSHOTS,
        FeatureKey.VIDEOS,
        FeatureKey.AUDIOS,
        FeatureKey.LARGE_FILES,
        FeatureKey.DUPLICATE_FILES,
        FeatureKey.DOCUMENTS,
    )

@Composable
fun FilesManagerTabContent(onFeatureClick: (FeatureKey) -> Unit = {}) {
    val categories = remember { FileManagerFeatures.filter { feature -> feature.destinationOrNull() != null } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(CleanXGridSpacing),
        verticalArrangement = Arrangement.spacedBy(CleanXGridSpacing),
    ) {
        items(categories, key = { feature -> feature.name }) { feature ->
            val title = stringResource(feature.titleRes())
            CleanXIconTile(
                title = title,
                icon = painterResource(id = feature.iconRes()),
                onClick = {
                    onFeatureClick(feature)
                },
            )
        }
    }
}

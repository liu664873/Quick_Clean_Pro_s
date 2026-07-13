package com.quickcleanpro.phonecleaner.use.skin.files.common.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.CleanXScanProgress

@Composable
internal fun FileManagerScanningView(text: String = "Scanning...") {
    CleanXScanProgress(
        captionText = text,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_file),
            contentDescription = null,
            modifier = Modifier.size(74.dp),
        )
    }
}

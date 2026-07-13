package com.quickcleanpro.phonecleaner.use.skin.applock.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.feature.applock.presentation.AppLockUiState

@Composable
internal fun AppLockSelectAppsView(
    uiState: AppLockUiState,
    onTogglePackage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        when {
            uiState.isLoading && uiState.apps.isEmpty() -> item { LoadingCard() }
            uiState.apps.isEmpty() -> item { EmptyCard(text = stringResource(R.string.app_lock_no_apps)) }
            else -> items(uiState.apps, key = { app -> app.packageName }) { app ->
                AppLockRow(
                    app = app,
                    onClick = { onTogglePackage(app.packageName) },
                )
            }
        }
    }
}

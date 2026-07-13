package com.quickcleanpro.phonecleaner.feature.applock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.applock.AppLockApp
import com.quickcleanpro.phonecleaner.feature.applock.AppLockUiState

@Composable
internal fun AppLockManageView(
    uiState: AppLockUiState,
    onOpenSearch: () -> Unit,
    onTogglePackage: (String) -> Unit,
    onToggleAll: () -> Unit,
    onAutoLockChange: (Boolean) -> Unit
) {
    val noAppsText = stringResource(R.string.app_lock_no_apps)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 50.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item { SearchEntryCard(onClick = onOpenSearch) }
        item {
            AutoLockCard(
                checked = uiState.autoLockEnabled,
                onClick = { onAutoLockChange(!uiState.autoLockEnabled) }
            )
        }
        item {
            CheckActionCard(
                title = stringResource(R.string.lock_all_apps),
                checked = uiState.allAppsLocked,
                onClick = onToggleAll
            )
        }
        if (uiState.isLoading && uiState.apps.isEmpty()) {
            item { LoadingCard() }
        } else {
            appSections(
                apps = uiState.apps,
                emptyText = noAppsText,
                onTogglePackage = onTogglePackage
            )
        }
    }
}

internal fun LazyListScope.appSections(
    apps: List<AppLockApp>,
    emptyText: String,
    onTogglePackage: (String) -> Unit
) {
    if (apps.isEmpty()) {
        item { EmptyCard(text = emptyText) }
        return
    }
    val lockedApps = apps.filter { it.isLocked }
    val unlockedApps = apps.filterNot { it.isLocked }
    if (lockedApps.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.locked_apps),
                color = AppLockNavy,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        lockedApps.forEachIndexed { index, app ->
            item(key = app.packageName) {
                AppLockRowCard(app = app, onToggle = { onTogglePackage(app.packageName) })
            }
        }
    }
    if (unlockedApps.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.my_apps),
                color = AppLockNavy,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        unlockedApps.forEachIndexed { index, app ->
            item(key = app.packageName) {
                AppLockRowCard(app = app, onToggle = { onTogglePackage(app.packageName) })
            }
        }
    }
}

@Composable
internal fun SearchEntryCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() },
        color = AppLockCardColor,
        shape = RoundedCornerShape(AppLockCardRadius)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_ser),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.search_app),
                color = AppLockPlaceholderText,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
internal fun AutoLockCard(
    checked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        color = AppLockCardColor,
        shape = RoundedCornerShape(AppLockCardRadius)
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.turn_on_auto_lock),
                    color = AppLockNavy,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                CheckImage(checked = checked)
            }
            AppLockDivider()
            Text(
                text = stringResource(R.string.turn_on_auto_lock_hint),
                color = AppLockSecondaryText,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(69.dp)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
internal fun CheckActionCard(
    title: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable { onClick() },
        color = AppLockCardColor,
        shape = RoundedCornerShape(AppLockCardRadius)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = AppLockNavy,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            CheckImage(checked = checked)
        }
    }
}

@Composable
internal fun CheckImage(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(if (checked) R.mipmap.ic_check else R.mipmap.ic_check_nor),
        contentDescription = null,
        modifier = modifier.size(width = 50.dp, height = 30.dp)
    )
}

@Composable
private fun AppLockRowCard(
    app: AppLockApp,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        PackageAppIcon(
            packageName = app.packageName,
            fallbackText = app.appName.take(1).ifBlank { "A" }
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = app.appName,
            color = AppLockNavy,
            fontSize = 16.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        CheckImage(checked = app.isLocked)
    }
}

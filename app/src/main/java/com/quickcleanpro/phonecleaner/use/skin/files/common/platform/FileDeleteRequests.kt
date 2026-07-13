package com.quickcleanpro.phonecleaner.use.skin.files.common.platform

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.FileUri

fun requestMediaStoreDeleteOrDeleteDirectly(
    context: Context,
    uris: List<FileUri>,
    launchRequest: (IntentSenderRequest) -> Unit,
    deleteDirectly: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        deleteDirectly()
        return
    }

    val mediaStoreUris = uris.map { Uri.parse(it.value) }.filter { it.canUseMediaStoreDeleteRequest() }
    requestMediaStoreDeleteUrisOrDeleteDirectly(
        mediaStoreUris = mediaStoreUris,
        launchDeleteRequest = { requestUris ->
            val request = MediaStore.createDeleteRequest(context.contentResolver, requestUris)
            launchRequest(IntentSenderRequest.Builder(request.intentSender).build())
        },
        deleteDirectly = deleteDirectly,
    )
}

fun requestMediaStoreDeleteUrisOrDeleteDirectly(
    mediaStoreUris: List<Uri>,
    launchDeleteRequest: (List<Uri>) -> Unit,
    deleteDirectly: () -> Unit,
) {
    if (mediaStoreUris.isEmpty()) {
        deleteDirectly()
        return
    }
    runCatching {
        launchDeleteRequest(mediaStoreUris)
    }.onFailure {
        deleteDirectly()
    }
}

private fun Uri.canUseMediaStoreDeleteRequest(): Boolean =
    scheme == ContentResolver.SCHEME_CONTENT &&
        authority == MediaStore.AUTHORITY &&
        runCatching {
            ContentUris.parseId(this)
            true
        }.getOrDefault(false)

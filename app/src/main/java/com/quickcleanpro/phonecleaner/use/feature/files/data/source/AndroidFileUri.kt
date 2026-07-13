package com.quickcleanpro.phonecleaner.use.feature.files.data.source

import android.net.Uri
import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.FileUri

internal fun Uri.toFileUri(): FileUri = FileUri(toString())

internal fun FileUri.toAndroidUri(): Uri = Uri.parse(value)

package com.quickcleanpro.phonecleaner.feature.files.shared.data.source

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import android.net.Uri
import com.quickcleanpro.phonecleaner.feature.files.shared.FileUri

internal fun Uri.toFileUri(): FileUri = FileUri(toString())

internal fun FileUri.toAndroidUri(): Uri = Uri.parse(value)

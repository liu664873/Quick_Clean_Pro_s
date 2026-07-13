package com.quickcleanpro.phonecleaner.feature.files.shared.data.source

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import android.content.Context
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.CoroutineContext

internal object FileContentHasher {
    fun hash(context: Context, item: ManagedFileItem, coroutineContext: CoroutineContext): String? =
        runCatching {
            coroutineContext.ensureActive()
            val stream =
                if (!item.path.isNullOrBlank() && File(item.path).isFile) {
                    FileInputStream(item.path)
                } else {
                    context.contentResolver.openInputStream(item.uri.toAndroidUri())
                }
            stream?.use { sha256(it) { coroutineContext.ensureActive() } }
        }.getOrElse {
            coroutineContext.ensureActive()
            null
        }

    fun sha256(input: InputStream, checkCancellation: () -> Unit = {}): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(HASH_BUFFER_SIZE)
        while (true) {
            checkCancellation()
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private const val HASH_BUFFER_SIZE = 64 * 1024
}

internal data class DuplicateCandidate<T>(
    val value: T,
    val identity: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedSeconds: Long,
)

internal object DuplicateFileGrouping {
    fun <T> group(
        candidates: List<DuplicateCandidate<T>>,
        contentHash: (T) -> String?,
    ): List<List<T>> {
        val uniqueFiles = candidates.filter { it.sizeBytes > 0L }.distinctBy { it.identity }
        val hashGroups = mutableListOf<List<DuplicateCandidate<T>>>()
        val fallbackCandidates = mutableListOf<DuplicateCandidate<T>>()

        uniqueFiles
            .groupBy { it.sizeBytes }
            .values
            .filter { it.size > 1 }
            .forEach { sameSizeFiles ->
                val hashed = sameSizeFiles.map { file -> file to contentHash(file.value) }
                if (hashed.any { it.second == null }) fallbackCandidates += sameSizeFiles
                hashed
                    .filter { it.second != null }
                    .groupBy { it.second.orEmpty() }
                    .values
                    .map { group -> group.map { it.first } }
                    .filter { it.size > 1 }
                    .forEach { group -> hashGroups += group.sortedByDescending { it.modifiedSeconds } }
            }

        val fallbackGroups =
            fallbackCandidates
                .groupBy { "${it.name.lowercase(Locale.US)}#${it.sizeBytes}" }
                .values
                .filter { it.size > 1 }
                .map { group -> group.sortedByDescending { it.modifiedSeconds } }

        return (hashGroups + fallbackGroups)
            .distinctBy { group -> group.joinToString("|") { it.identity } }
            .sortedByDescending { group -> group.sortedByDescending { it.modifiedSeconds }.drop(1).sumOf { it.sizeBytes } }
            .map { group -> group.map { it.value } }
    }
}

package com.quickcleanpro.phonecleaner.feature.junkclean

import android.content.Context
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.AdJunkScanner
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.ApkScanner
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.CacheScanner
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.DuplicateFileScanner
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.JunkScanner
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.ResidualScanner
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.ScanDirectoryHelper
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.TempFileScanner
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.MemoryCleaner
import com.quickcleanpro.phonecleaner.feature.junkclean.model.CleanItem
import com.quickcleanpro.phonecleaner.feature.junkclean.model.CleanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkCategory
import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkFile
import com.quickcleanpro.phonecleaner.feature.junkclean.model.MemoryCleanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.model.ScanProgress
import com.quickcleanpro.phonecleaner.feature.junkclean.model.ScanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanRepository
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanSessionStore
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkAuthorizedDeleteResult
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkDeleteOutcome
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.JunkFileDeleteHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class CleanRepositoryImpl(
    private val context: Context,
    private val sharedState: CleanSessionStore,
) : CleanRepository {
    private data class DedupedScanSnapshot(
        val junkFiles: List<JunkFile>,
        val totalSize: Long,
        val totalCount: Int,
        val categoryBreakdown: Map<JunkCategory, List<JunkFile>>,
    )

    private val cacheScanner = CacheScanner(context)
    private val tempFileScanner = TempFileScanner()
    private val apkScanner = ApkScanner()
    private val residualScanner = ResidualScanner(context)
    private val adJunkScanner = AdJunkScanner(context)
    private val duplicateFileScanner = DuplicateFileScanner()
    private val memoryCleaner = MemoryCleaner

    private val scanners =
        listOf(
            cacheScanner,
            tempFileScanner,
            residualScanner,
            apkScanner,
            adJunkScanner,
            duplicateFileScanner,
        )

    override val scanProgress: Flow<ScanProgress> get() = sharedState.scanProgress

    override suspend fun performFullScan(): ScanResult =
        withContext(Dispatchers.IO) {
            ScanDirectoryHelper.clearCache()
            val allJunkFiles = mutableListOf<JunkFile>()
            sharedState.setScanProgress(buildDedupedScanSnapshot(allJunkFiles).toScanProgress(percent = 0f))

            val weightPerScanner = 100f / scanners.size

            for ((index, scanner) in scanners.withIndex()) {
                val beforeScanSnapshot = buildDedupedScanSnapshot(allJunkFiles)
                sharedState.setScanProgress(
                    beforeScanSnapshot.toScanProgress(
                        percent = index * weightPerScanner,
                        currentCategory = scanner.category,
                    ),
                )
                delay(150)

                val files = scanSafely(scanner)
                allJunkFiles.addAll(files)

                val afterScanSnapshot = buildDedupedScanSnapshot(allJunkFiles)
                sharedState.setScanProgress(
                    afterScanSnapshot.toScanProgress(
                        percent = (index + 1) * weightPerScanner,
                        currentCategory = scanner.category,
                    ),
                )
            }

            val finalSnapshot = buildDedupedScanSnapshot(allJunkFiles)
            sharedState.setScanProgress(finalSnapshot.toScanProgress(percent = 100f))

            val result =
                ScanResult(
                    junkFiles = finalSnapshot.junkFiles,
                    totalSize = finalSnapshot.totalSize,
                    totalCount = finalSnapshot.totalCount,
                    categoryBreakdown = finalSnapshot.categoryBreakdown,
                )
            sharedState.setScanResult(result)
            result
        }

    override suspend fun cleanFiles(selectedItems: List<CleanItem>): CleanResult =
        withContext(Dispatchers.IO) {
            val cleanedFiles = mutableListOf<JunkFile>()
            val failedFiles = mutableListOf<JunkFile>()
            var freedSpace = 0L

            for (item in selectedItems) {
                val outcome = JunkFileDeleteHelper.delete(context, item.junkFile)
                if (outcome.deleted) {
                    cleanedFiles.add(item.junkFile)
                    freedSpace += outcome.freedBytes
                } else {
                    failedFiles.add(item.junkFile)
                }
            }

            val result =
                CleanResult(
                    cleanedFiles = cleanedFiles,
                    freedSpace = freedSpace,
                    failedFiles = failedFiles,
                )
            sharedState.removeCleanedFiles(cleanedFiles)
            sharedState.setCleanResult(result)
            result
        }

    override suspend fun cleanMemory(): MemoryCleanResult =
        withContext(Dispatchers.IO) {
            val result = memoryCleaner.clean(context.applicationContext)
            sharedState.setMemoryResult(result)
            result
        }

    override suspend fun deleteFileItems(selectedItems: List<CleanItem>): List<JunkDeleteOutcome> =
        withContext(Dispatchers.IO) {
            selectedItems.map { JunkFileDeleteHelper.delete(context, it.junkFile) }
        }

    override suspend fun finalizeAuthorizedDeletes(pendingOutcomes: List<JunkDeleteOutcome>): JunkAuthorizedDeleteResult =
        withContext(Dispatchers.IO) {
            JunkFileDeleteHelper.finalizeAuthorizedDeletes(pendingOutcomes)
        }

    private fun buildDedupedScanSnapshot(files: List<JunkFile>): DedupedScanSnapshot {
        val uniqueJunkFiles = dedupeJunkFiles(files)
        return DedupedScanSnapshot(
            junkFiles = uniqueJunkFiles,
            totalSize = uniqueJunkFiles.sumOf { it.fileSize },
            totalCount = uniqueJunkFiles.size,
            categoryBreakdown = uniqueJunkFiles.groupBy { it.category },
        )
    }

    private fun DedupedScanSnapshot.toScanProgress(
        percent: Float,
        currentCategory: JunkCategory? = null,
    ): ScanProgress =
        ScanProgress(
            percent = percent,
            currentCategory = currentCategory,
            foundCount = totalCount,
            foundSize = totalSize,
        )

    private fun dedupeJunkFiles(files: List<JunkFile>): List<JunkFile> {
        val sorted =
            files.sortedWith(
                compareBy<JunkFile> { normalizedPath(it.filePath).length }
                    .thenBy { it.category.ordinal },
            )
        val kept = mutableListOf<JunkFile>()
        val keptPaths = mutableSetOf<String>()

        for (file in sorted) {
            val path = normalizedPath(file.filePath)
            if (path in keptPaths) continue
            if (keptPaths.any { parent -> path != parent && path.startsWith("$parent/") }) continue

            kept += file
            keptPaths += path
        }

        return kept
    }

    private fun normalizedPath(path: String): String =
        runCatching { File(path).canonicalPath }
            .getOrElse { path }
            .replace('\\', '/')
            .trimEnd('/')

    private suspend fun scanSafely(scanner: JunkScanner): List<JunkFile> =
        try {
            scanner.scan()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyList()
        }
}

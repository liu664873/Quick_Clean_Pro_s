package com.quickcleanpro.phonecleaner.architecture

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotherTemplateBoundaryTest {
    private val sourceRoot = locate("src/main/java")
    private val manifest = locate("src/main/AndroidManifest.xml")

    @Test
    fun `manifest component classes exist in source`() {
        val sourceClasses = sourceRoot.walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap(::qualifiedClassNames)
            .toSet()
        val declaredClasses = Regex("android:name=\"(com\\.quickcleanpro\\.phonecleaner[^\"]+)\"")
            .findAll(manifest.readText())
            .map { it.groupValues[1] }
            .toList()

        assertTrue(declaredClasses.isNotEmpty())
        assertEquals(emptyList<String>(), declaredClasses.filterNot(sourceClasses::contains))
    }

    @Test
    fun `logic never imports feature ui`() {
        val violations = kotlinFiles().filter { file ->
            "${File.separator}logic${File.separator}" in file.path &&
                Regex("import com\\.quickcleanpro\\.phonecleaner\\.feature\\..*\\.ui").containsMatchIn(file.readText())
        }
        assertEquals(emptyList<String>(), violations.map { it.relativeTo(sourceRoot).path }.toList())
    }

    @Test
    fun `ui contains no view model implementations`() {
        val violations = kotlinFiles().filter { file ->
            "${File.separator}ui${File.separator}" in file.path &&
                file.name.endsWith("ViewModel.kt")
        }
        assertEquals(emptyList<String>(), violations.map { it.relativeTo(sourceRoot).path }.toList())
    }

    @Test
    fun `logic never imports compose`() {
        val violations = kotlinFiles().filter { file ->
            "${File.separator}logic${File.separator}" in file.path &&
                Regex("import androidx\\.compose").containsMatchIn(file.readText())
        }
        assertEquals(emptyList<String>(), violations.map { it.relativeTo(sourceRoot).path }.toList())
    }

    @Test
    fun `common ui never imports features`() {
        val commonUi = File(sourceRoot, "com/quickcleanpro/phonecleaner/common/ui")
        val violations = commonUi.walkTopDown().filter { it.extension == "kt" }.filter {
            "import com.quickcleanpro.phonecleaner.feature." in it.readText()
        }.toList()
        assertEquals(emptyList<String>(), violations.map { it.relativeTo(sourceRoot).path }.toList())
    }

    @Test
    fun `screens and routes never import business data access`() {
        val violations = kotlinFiles()
            .filter { it.name.endsWith("Screen.kt") || it.name.endsWith("Route.kt") }
            .filter {
            Regex("import .*\\.(?:Repository|DataSource|Scanner)(?:Impl)?$")
                .containsMatchIn(it.readText())
        }
        assertEquals(emptyList<String>(), violations.map { it.relativeTo(sourceRoot).path }.toList())
    }

    private fun kotlinFiles(): Sequence<File> =
        sourceRoot.walkTopDown().filter { it.extension == "kt" }

    private fun qualifiedClassNames(file: File): Sequence<String> {
        val text = file.readText()
        val packageName = Regex("^package\\s+([A-Za-z0-9_.]+)", RegexOption.MULTILINE)
            .find(text)?.groupValues?.get(1) ?: return emptySequence()
        return Regex("^(?:public\\s+|internal\\s+)?(?:class|object)\\s+([A-Za-z0-9_]+)", RegexOption.MULTILINE)
            .findAll(text)
            .map { "$packageName.${it.groupValues[1]}" }
    }

    private fun locate(path: String): File {
        val direct = File(path)
        if (direct.exists()) return direct
        return File("app", path).also { require(it.exists()) { "Missing project path: $path" } }
    }
}

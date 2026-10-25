package com.elkrrai.feather.ui.brand

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The old blue bird exists in several files (the Compose drawing, vector drawables, SVGs). BirdShapes
 * is the source; these tests fail if a copy was edited and the others were not. The owl replaced it
 * on screens and as the launcher icon; these are the places still waiting for owl art (see
 * `art/README.md`), and the test goes when the last of them does.
 */
class BirdArtworkTest {

    /** Unit tests run from the app module, but find the file from the repository root too. */
    private fun read(path: String): String {
        val candidates = listOf(File(path), File("app/$path"), File("../$path"))
        val file = candidates.firstOrNull { it.exists() } ?: error("Cannot find $path")
        return file.readText()
    }

    private val all = mapOf(
        "tail" to BirdShapes.TAIL,
        "body" to BirdShapes.BODY,
        "head" to BirdShapes.HEAD,
        "belly" to BirdShapes.BELLY,
        "wing" to BirdShapes.WING,
        "beak" to BirdShapes.BEAK,
        "crest" to BirdShapes.CREST,
    )

    private val silhouette = all.filterKeys { it in setOf("tail", "body", "head", "beak", "crest") }

    private fun assertContainsPaths(file: String, shapes: Map<String, String>) {
        val text = read(file)
        for ((name, data) in shapes) {
            assertTrue("$file is missing the $name path", text.contains(data))
        }
    }

    @Test
    fun theFullColorDrawablesHaveEveryShape() {
        assertContainsPaths("src/main/res/drawable/ic_bird_mark.xml", all)
        assertContainsPaths("src/main/res/drawable/ic_splash_bird.xml", all)
    }

    @Test
    fun theSingleColorDrawablesHaveTheSilhouette() {
        assertContainsPaths("src/main/res/drawable/ic_stat_feather.xml", silhouette)
    }

    @Test
    fun theStoreGraphicsHaveEveryShape() {
        assertContainsPaths("../store/graphics/icon.svg", all)
        assertContainsPaths("../store/graphics/feature-graphic.svg", all)
    }

    @Test
    fun theBrandColorsAreTheOnesInTheDrawables() {
        val mark = read("src/main/res/drawable/ic_bird_mark.xml").uppercase()
        val colors = mapOf(
            "plumage" to "#2A7F9E",
            "deep" to "#17485E",
            "wing" to "#78C0D8",
            "cream" to "#FFF3DC",
            "amber" to "#F2A93B",
            "coral" to "#E8735F",
            "ink" to "#16262E",
        )
        for ((name, hex) in colors) {
            assertTrue("ic_bird_mark is missing the $name color", mark.contains(hex))
        }
    }
}

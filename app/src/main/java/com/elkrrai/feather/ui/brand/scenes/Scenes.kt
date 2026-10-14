package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.brand.BirdMood

// Where the bird stands in each scene: its 96 by 96 box is moved and scaled by one transform. The
// numbers put its feet on the ground, the branch or the rim of the nest or bin.
internal val EmptyListBird = BirdPlacement(x = 18f, y = 90f, scale = 0.95f)
internal val AllDoneBird = BirdPlacement(x = 52f, y = 66f, scale = 0.95f)
internal val NothingDueBird = BirdPlacement(x = 52f, y = 36f, scale = 1f)
internal val NoResultsBird = BirdPlacement(x = 14f, y = 94f, scale = 0.9f, tilt = -10f)
internal val RemindersBlockedBird = BirdPlacement(x = 116f, y = 118f, scale = 0.6f)
internal val FirstLaunchBird = BirdPlacement(x = 50f, y = 78f, scale = 0.8f)
internal val TrashEmptyBird = BirdPlacement(x = 80f, y = 43f, scale = 0.8f)

/**
 * One of the eight scenes of the illustration kit, drawn in code on a 200 by 200 board and shown
 * at [size]. The scene says nothing to screen readers; put the sentence under it.
 */
@Composable
fun Illustration(kind: SceneKind, modifier: Modifier = Modifier, size: Dp = 190.dp) {
    when (kind) {
        SceneKind.EmptyList -> EmptyListScene(size, modifier)
        SceneKind.AllDone -> AllDoneScene(size, modifier)
        SceneKind.NothingDue -> NothingDueScene(size, modifier)
        SceneKind.NoResults -> NoResultsScene(size, modifier)
        else -> Unit
    }
}

/** The bird beside an empty nest, with a leaf that sways. */
@Composable
private fun EmptyListScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    Layer {
        drawGround(p)
        drawNestBack(p, 146f, 150f, 38f)
    }
    SceneBird(BirdMood.IDLE, EmptyListBird)
    Layer {
        drawNestFront(p, 146f, 150f, 38f)
        val stemBase = Offset(176f, 138f)
        rotate(sway(), pivot = stemBase) {
            drawLine(p.bowl, stemBase, Offset(176f, 114f), 3f, StrokeCap.Round)
            drawLeaf(Offset(176f, 118f), 26f, -20f, p.moss)
        }
    }
}

/** A happy bird on a nest with three eggs, under a sun, with two sparkles. */
@Composable
private fun AllDoneScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    Layer {
        drawSun(p, Offset(162f, 40f), 14f)
        drawSparkle(Offset(44f, 66f), 7f, p.amber)
        drawSparkle(Offset(172f, 92f), 5f, p.cream)
        drawGround(p, y = 176f, halfWidth = 66f)
        drawNestBack(p, 100f, 152f, 54f)
    }
    SceneBird(BirdMood.HAPPY, AllDoneBird)
    Layer {
        // The eggs sit in the bowl: their lower halves are hidden by its front.
        drawEgg(p, Offset(66f, 150f))
        drawEgg(p, Offset(100f, 156f))
        drawEgg(p, Offset(134f, 150f))
        drawNestFront(p, 100f, 152f, 54f)
    }
}

/** A sleeping bird on a branch, with a cloud and a small sun. */
@Composable
private fun NothingDueScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    Layer {
        drawCloud(p, Offset(160f, 54f))
        drawSun(p, Offset(40f, 42f), 9f)
        drawBranch(p, Offset(30f, 126f), Offset(170f, 122f))
        drawLeaf(Offset(150f, 123f), 20f, -30f, p.moss)
    }
    SceneBird(BirdMood.SLEEPING, NothingDueBird)
}

/** A bird tilting its head at a loupe held over bare twigs. */
@Composable
private fun NoResultsScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    Layer {
        drawGround(p)
        drawTwig(p, Offset(132f, 172f), Offset(128f, 116f))
        drawTwig(p, Offset(152f, 172f), Offset(160f, 104f))
        drawTwig(p, Offset(172f, 172f), Offset(170f, 128f))
    }
    SceneBird(BirdMood.IDLE, NoResultsBird)
    Layer { drawLoupe(p, Offset(150f, 112f), 24f) }
}

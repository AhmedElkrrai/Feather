package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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
        SceneKind.BackupSaved -> BackupSavedScene(size, modifier)
        SceneKind.RemindersBlocked -> RemindersBlockedScene(size, modifier)
        SceneKind.FirstLaunch -> FirstLaunchScene(size, modifier)
        SceneKind.TrashEmpty -> TrashEmptyScene(size, modifier)
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

/** A woven basket with its lid ajar, a moss check badge, and a sky-blue feather drifting by. */
@Composable
private fun BackupSavedScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    Layer {
        drawGround(p)
        drawBasket(p, 100f, 108f, 164f, 48f, 40f)
        // The lid is lifted on its right side; a dark wedge shows between it and the rim.
        val gap = Path().apply {
            moveTo(52f, 104f)
            lineTo(148f, 104f)
            lineTo(150f, 82f)
            close()
        }
        drawPath(gap, p.weave)
        rotate(-14f, pivot = Offset(52f, 104f)) {
            drawRoundRect(p.bowl, topLeft = Offset(50f, 92f), size = Size(100f, 12f), cornerRadius = CornerRadius(6f, 6f))
        }
        drawCheckBadge(p, Offset(146f, 134f), 15f)
        // The feather: a leaf shape with a quill, swaying a little.
        rotate(28f + sway() * 2f, pivot = Offset(46f, 62f)) {
            drawLeaf(Offset(46f, 76f), 40f, 0f, p.sky)
            drawLine(p.cream.copy(alpha = 0.7f), Offset(46f, 76f), Offset(77f, 45f), 1.6f, StrokeCap.Round)
        }
    }
}

/** An amber bell crossed by a twig (not a red slash), with a small bird peeking from behind it. */
@Composable
private fun RemindersBlockedScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    Layer { drawGround(p, y = 176f) }
    SceneBird(BirdMood.IDLE, RemindersBlockedBird)
    Layer {
        drawBell(p, 100f, 30f)
        drawLine(p.bowl, Offset(56f, 160f), Offset(150f, 70f), 7f, StrokeCap.Round)
        drawLeaf(Offset(146f, 74f), 24f, -10f, p.moss)
    }
}

/** A cracked egg with the bird's head peeking out, and the sun climbing over a dawn horizon. */
@Composable
private fun FirstLaunchScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier, risingSun = true) {
    val p = palette
    Layer {
        // The sun starts low behind the horizon and climbs 24 units.
        drawCircle(p.amber, radius = 30f, center = Offset(100f, 168f - sunRise()))
        drawOval(p.dawnGround, topLeft = Offset(6f, 146f), size = Size(188f, 52f))
    }
    SceneBird(BirdMood.IDLE, FirstLaunchBird)
    Layer {
        // The lower half of the shell, with a zigzag crack along its top.
        val shell = Path().apply {
            moveTo(60f, 124f)
            lineTo(70f, 114f)
            lineTo(80f, 124f)
            lineTo(90f, 112f)
            lineTo(100f, 124f)
            lineTo(110f, 112f)
            lineTo(120f, 124f)
            lineTo(130f, 114f)
            lineTo(140f, 124f)
            cubicTo(144f, 150f, 126f, 172f, 100f, 172f)
            cubicTo(74f, 172f, 56f, 150f, 60f, 124f)
            close()
        }
        drawPath(shell, p.cream)
        drawCircle(p.amber.copy(alpha = 0.5f), radius = 2f, center = Offset(80f, 146f))
        drawCircle(p.coral.copy(alpha = 0.45f), radius = 1.8f, center = Offset(118f, 140f))
        drawCircle(p.amber.copy(alpha = 0.5f), radius = 1.6f, center = Offset(104f, 160f))
    }
}

/** A woven bin with its lid leaned against it, a happy bird on the rim, and a leaf. */
@Composable
private fun TrashEmptyScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    Layer {
        drawGround(p)
        drawBasket(p, 118f, 112f, 168f, 36f, 30f)
        // The lid, standing on its edge against the bin.
        rotate(-72f, pivot = Offset(60f, 168f)) {
            drawRoundRect(p.bowl, topLeft = Offset(60f, 162f), size = Size(60f, 12f), cornerRadius = CornerRadius(6f, 6f))
        }
        drawLine(p.bowl, Offset(172f, 168f), Offset(172f, 148f), 3f, StrokeCap.Round)
        drawLeaf(Offset(172f, 152f), 26f, -10f, p.moss)
    }
    SceneBird(BirdMood.HAPPY, TrashEmptyBird)
}

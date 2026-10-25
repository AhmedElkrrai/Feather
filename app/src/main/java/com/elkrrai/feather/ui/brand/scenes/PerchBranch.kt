package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.elkrrai.feather.R

/**
 * The branch the owl sits on (`perch_branch`, a 2 to 1 picture), [width] board units wide with its
 * top left corner at ([x], [y]). Its top edge runs a little above a fifth of the way down the
 * picture, and a twig rises at the right.
 */
internal class Perch(val x: Float, val y: Float, val width: Float) {
    /** The tip of the top twig, where a leaf can grow. */
    val twigTip: Offset get() = Offset(x + TWIG_TIP_X * width, y + TWIG_TIP_Y * width / 2f)

    companion object {
        // Measured on the Today header, where the owl is 84 dp and the branch 150 dp wide, hanging
        // 36.5 dp below the top of the owl's box and starting 36 dp to the left of the owl.
        private const val WIDTH_PER_OWL = 150f / 84f
        private const val LEFT_PER_OWL = 36f / 84f
        private const val TOP_PER_OWL = 36.5f / 84f
        private const val TWIG_TIP_X = 0.866f
        private const val TWIG_TIP_Y = 0.267f

        /** A branch under the owl placed at [owl], at the same size and place relative to it as on Today. */
        fun under(owl: BirdPlacement) = Perch(
            x = owl.x - LEFT_PER_OWL * owl.side,
            y = owl.y + TOP_PER_OWL * owl.side,
            width = WIDTH_PER_OWL * owl.side,
        )
    }
}

@Composable
internal fun SceneScope.PerchBranch(perch: Perch) {
    Image(
        painter = painterResource(R.drawable.perch_branch),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset(x = unit * perch.x, y = unit * perch.y)
            .size(width = unit * perch.width, height = unit * perch.width / 2f),
    )
}

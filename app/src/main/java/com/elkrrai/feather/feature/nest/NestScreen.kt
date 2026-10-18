package com.elkrrai.feather.feature.nest

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.nest.NestFacts
import com.elkrrai.feather.domain.nest.NestItem
import com.elkrrai.feather.domain.nest.progress
import com.elkrrai.feather.ui.brand.scenes.NestScene
import com.elkrrai.feather.ui.theme.DayPhase
import com.elkrrai.feather.ui.theme.NestShapes
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors

@Composable
fun NestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Looking at the nest is what clears the quiet dot on Settings.
    LaunchedEffect(Unit) { viewModel.markSeen() }
    NestContent(state, onBack, modifier)
}

@StringRes
private fun NestItem.nameRes(): Int = when (this) {
    NestItem.SCARF -> R.string.nest_item_scarf
    NestItem.FAIRY_LIGHTS -> R.string.nest_item_fairy_lights
    NestItem.BUNTING -> R.string.nest_item_bunting
    NestItem.LANTERN -> R.string.nest_item_lantern
    NestItem.MOSS -> R.string.nest_item_moss
    NestItem.GLASSES -> R.string.nest_item_glasses
    NestItem.FLOWER_POT -> R.string.nest_item_flower_pot
    NestItem.PARTY_HAT -> R.string.nest_item_party_hat
}

@StringRes
private fun NestItem.howRes(): Int = when (this) {
    NestItem.SCARF -> R.string.nest_how_scarf
    NestItem.FAIRY_LIGHTS -> R.string.nest_how_fairy_lights
    NestItem.BUNTING -> R.string.nest_how_bunting
    NestItem.LANTERN -> R.string.nest_how_lantern
    NestItem.MOSS -> R.string.nest_how_moss
    NestItem.GLASSES -> R.string.nest_how_glasses
    NestItem.FLOWER_POT -> R.string.nest_how_flower_pot
    NestItem.PARTY_HAT -> R.string.nest_how_party_hat
}

/**
 * The bird's nest: a picture with everything earned so far, and under it every item with how it is
 * earned and how far along it is. Nothing is time-limited, random or paid; locked items are simply
 * not there yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestContent(state: NestUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nest_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.gutter,
                end = Space.gutter,
                top = padding.calculateTopPadding() + Space.x2,
                bottom = padding.calculateBottomPadding() + Space.x8,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            item(key = "scene") {
                val (top, bottom) = MaterialTheme.featherColors.sky.getValue(DayPhase.Day)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(NestShapes.extraLarge)
                        .background(Brush.verticalGradient(listOf(top, bottom)))
                        .padding(vertical = Space.x4),
                    contentAlignment = Alignment.Center,
                ) {
                    NestScene(items = state.earned, size = 280.dp)
                }
            }
            item(key = "intro") {
                Text(
                    text = stringResource(R.string.nest_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(NestItem.entries, key = { it.id }) { item ->
                NestRow(item, earned = item in state.earned, fresh = item in state.news, facts = state.facts)
            }
        }
    }
}

@Composable
private fun NestRow(item: NestItem, earned: Boolean, fresh: Boolean, facts: NestFacts) {
    val name = stringResource(item.nameRes())
    val how = stringResource(item.howRes())
    val progress = item.progress(facts)
    val status = when {
        earned -> stringResource(R.string.nest_earned)
        progress != null -> stringResource(R.string.nest_progress, progress.first, progress.second)
        else -> stringResource(R.string.nest_not_yet)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NestShapes.large)
            .background(MaterialTheme.featherColors.card)
            .heightIn(min = 64.dp)
            .padding(horizontal = Space.x4, vertical = Space.x3)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x3),
    ) {
        StatusMark(earned)
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = how, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            color = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (fresh) {
            Box(Modifier.size(8.dp).clip(NestShapes.extraSmall).background(MaterialTheme.colorScheme.primary))
        }
    }
}

/** A filled circle with a check for what is in the nest, a ring for what is not there yet. */
@Composable
private fun StatusMark(earned: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(24.dp)) {
            if (earned) {
                drawCircle(primary)
            } else {
                drawCircle(outline, radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.8.dp.toPx()))
            }
        }
        if (earned) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

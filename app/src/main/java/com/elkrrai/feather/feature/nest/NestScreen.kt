package com.elkrrai.feather.feature.nest

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.nest.NestFacts
import com.elkrrai.feather.domain.nest.NestItem
import com.elkrrai.feather.domain.nest.NestOutfit
import com.elkrrai.feather.domain.nest.NestStyle
import com.elkrrai.feather.domain.nest.progress
import com.elkrrai.feather.ui.brand.scenes.NestScene
import com.elkrrai.feather.ui.brand.scenes.pictureRes
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
    NestContent(state, onBack, viewModel::pickStyle, viewModel::wear, modifier)
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
fun NestContent(
    state: NestUiState,
    onBack: () -> Unit,
    onPickStyle: (NestStyle) -> Unit,
    onWear: (NestOutfit) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    NestScene(items = state.earned, size = 280.dp, style = state.style, outfit = state.outfit)
                }
            }
            item(key = "styles") {
                NestPicker(selected = state.style, onPick = onPickStyle)
            }
            if (Wardrobe.any { slot -> slot.item in state.earned }) {
                item(key = "wardrobe") {
                    WardrobePicker(state.earned, state.outfit, onWear)
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

/** The nests to choose from, side by side: a small picture of each with its name; the chosen one is ringed. */
@Composable
private fun NestPicker(selected: NestStyle, onPick: (NestStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
        Text(
            text = stringResource(R.string.nest_choose),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
            for (style in NestStyle.entries) {
                val chosen = style == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(NestShapes.large)
                        .background(MaterialTheme.featherColors.card)
                        .border(2.dp, if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent, NestShapes.large)
                        .selectable(selected = chosen, role = Role.RadioButton, onClick = { onPick(style) })
                        .padding(Space.x2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.x1),
                ) {
                    Image(
                        painter = painterResource(style.pictureRes()),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.4f),
                    )
                    Text(
                        text = stringResource(style.nameRes()),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** One kind of garment the owl can wear: the item that earns it, its choices, and how to put the choice in an outfit. */
private class Slot(
    val item: NestItem,
    val thumbs: IntArray,
    val chosen: (NestOutfit) -> Int,
    val with: (NestOutfit, Int) -> NestOutfit,
)

private val Wardrobe = listOf(
    Slot(
        NestItem.SCARF,
        intArrayOf(
            R.drawable.dress_thumb_scarf_0, R.drawable.dress_thumb_scarf_1, R.drawable.dress_thumb_scarf_2,
            R.drawable.dress_thumb_scarf_3, R.drawable.dress_thumb_scarf_4, R.drawable.dress_thumb_scarf_5,
        ),
        { it.scarf },
        { outfit, choice -> outfit.copy(scarf = choice) },
    ),
    Slot(
        NestItem.GLASSES,
        intArrayOf(R.drawable.dress_thumb_glasses_0, R.drawable.dress_thumb_glasses_1),
        { it.glasses },
        { outfit, choice -> outfit.copy(glasses = choice) },
    ),
    Slot(
        NestItem.FLOWER_POT,
        intArrayOf(
            R.drawable.dress_thumb_pot_0, R.drawable.dress_thumb_pot_1, R.drawable.dress_thumb_pot_2, R.drawable.dress_thumb_pot_3,
        ),
        { it.pot },
        { outfit, choice -> outfit.copy(pot = choice) },
    ),
    Slot(
        NestItem.PARTY_HAT,
        intArrayOf(
            R.drawable.dress_thumb_hat_0, R.drawable.dress_thumb_hat_1, R.drawable.dress_thumb_hat_2, R.drawable.dress_thumb_hat_3,
        ),
        { it.hat },
        { outfit, choice -> outfit.copy(hat = choice) },
    ),
)

/** What the owl wears and what stands by the nest: for each item that has been earned, a row of its choices, and "None". */
@Composable
private fun WardrobePicker(earned: Set<NestItem>, outfit: NestOutfit, onWear: (NestOutfit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
        Text(
            text = stringResource(R.string.nest_dress),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        for (slot in Wardrobe.filter { it.item in earned }) {
            val name = stringResource(slot.item.nameRes())
            val worn = slot.chosen(outfit)
            Text(text = name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Space.x2),
            ) {
                GarmentChoice(selected = worn == NestOutfit.NONE, label = stringResource(R.string.nest_dress_none)) {
                    onWear(slot.with(outfit, NestOutfit.NONE))
                }
                slot.thumbs.forEachIndexed { choice, thumb ->
                    GarmentChoice(
                        selected = worn == choice,
                        label = stringResource(R.string.nest_dress_choice, name, choice + 1),
                        picture = thumb,
                    ) { onWear(slot.with(outfit, choice)) }
                }
            }
        }
    }
}

/** A square to tap: a small picture of the garment, or the word "None". The chosen one is ringed. */
@Composable
private fun GarmentChoice(selected: Boolean, label: String, picture: Int? = null, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(NestShapes.large)
            .background(MaterialTheme.featherColors.card)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, NestShapes.large)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (picture != null) {
            Image(
                painter = painterResource(picture),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(Space.x1),
            )
        } else {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@StringRes
private fun NestStyle.nameRes(): Int = when (this) {
    NestStyle.BRAIDED -> R.string.nest_style_braided
    NestStyle.TWIGS -> R.string.nest_style_twigs
    NestStyle.STRAW -> R.string.nest_style_straw
    NestStyle.ROPE -> R.string.nest_style_rope
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

package com.elkrrai.feather.feature.tip

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.support.TipOffer
import com.elkrrai.feather.domain.support.TipResult
import com.elkrrai.feather.ui.brand.BirdIllustration

/** The words for a tip amount, by its Play Console product id. */
@StringRes
private fun tipName(id: String): Int = when (id) {
    "tip_small" -> R.string.tip_small
    "tip_medium" -> R.string.tip_medium
    else -> R.string.tip_large
}

/**
 * The tip sheet: a bird, a few kind words and the amounts Google Play offers. A tip unlocks
 * nothing, and the sheet says so. [thanks] counts up with every tip that went through, and the
 * bird flies a lap each time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipSheet(
    offers: List<TipOffer>,
    status: TipResult?,
    thanks: Int,
    onTip: (offerId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BirdIllustration(modifier = Modifier.size(96.dp), flightTrigger = thanks)
            Text(
                text = stringResource(R.string.tip_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.tip_sheet_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            offers.forEach { offer ->
                FilledTonalButton(
                    onClick = { onTip(offer.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(tipName(offer.id)), style = MaterialTheme.typography.titleMedium)
                        Text(offer.price, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            val message = when (status) {
                TipResult.THANKS -> R.string.tip_thanks
                TipResult.PENDING -> R.string.tip_pending
                TipResult.FAILED -> R.string.tip_failed
                TipResult.CANCELLED, null -> null
            }
            if (message != null) {
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The activity a composable is shown in (Google Play's payment dialog needs one), or null. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

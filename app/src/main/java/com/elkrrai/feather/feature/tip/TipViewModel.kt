package com.elkrrai.feather.feature.tip

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.data.billing.TipJar
import com.elkrrai.feather.domain.support.TipOffer
import com.elkrrai.feather.domain.support.TipResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Behind the "Leave a tip" menu item and its sheet. */
@HiltViewModel
class TipViewModel @Inject constructor(
    private val tipJar: TipJar,
) : ViewModel() {

    /** The amounts a person can tip; empty when tipping is not possible, so the menu item stays hidden. */
    val offers: StateFlow<List<TipOffer>> = tipJar.offers

    /** What happened to the tip just left. */
    val results: SharedFlow<TipResult> = tipJar.results

    init {
        viewModelScope.launch { tipJar.refresh() }
    }

    fun tip(activity: Activity, offerId: String) = tipJar.tip(activity, offerId)
}

package com.elkrrai.feather.data.billing

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.elkrrai.feather.domain.support.TipOffer
import com.elkrrai.feather.domain.support.TipProductIds
import com.elkrrai.feather.domain.support.TipResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Tips through Google Play Billing: three one-time products (see [TipProductIds]) that can be
 * bought again and again. A tip unlocks nothing, so each purchase is consumed as soon as it
 * clears, which also lets the same amount be tipped twice. Nothing about a tip is kept in the app
 * and nothing is sent anywhere but to Google Play.
 *
 * [offers] stays empty when tipping is not possible (the app was not installed from Google Play,
 * the products are not set up yet, no connection), and the Settings row stays hidden then. A
 * debuggable build shows demo amounts instead, so the screen can be looked at without a Play
 * testing track; its tips are pretend.
 */
@Singleton
class TipJar @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private val _offers = MutableStateFlow<List<TipOffer>>(emptyList())
    val offers: StateFlow<List<TipOffer>> = _offers.asStateFlow()

    private val _results = MutableSharedFlow<TipResult>(extraBufferCapacity = 4)

    /** What happened to the tip the person just left. */
    val results: SharedFlow<TipResult> = _results.asSharedFlow()

    private val details = HashMap<String, ProductDetails>()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> scope.launch { onPurchasesUpdated(result, purchases) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Connects to Google Play, reads the amounts and takes care of any tip that was left unfinished. */
    suspend fun refresh() {
        val connected = try {
            connect()
        } catch (e: Exception) {
            false
        }
        if (connected) {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    TipProductIds.map { id ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(id)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    },
                )
                .build()
            val result = client.queryProductDetails(params)
            if (result.billingResult.responseCode == BillingResponseCode.OK) {
                details.clear()
                result.productDetailsList.orEmpty().forEach { details[it.productId] = it }
                _offers.value = TipProductIds.mapNotNull { id ->
                    details[id]?.oneTimePurchaseOfferDetails?.let { TipOffer(id, it.formattedPrice) }
                }
            }
            finishLeftovers()
        }
        if (_offers.value.isEmpty() && debuggable) {
            _offers.value = TipProductIds.zip(listOf("$1.00", "$3.00", "$5.00")).map { (id, price) -> TipOffer(id, price) }
        }
    }

    /** Opens Google Play's payment dialog for [offerId]. The outcome arrives on [results]. */
    fun tip(activity: Activity, offerId: String) {
        val product = details[offerId]
        if (product == null) {
            // Only a debuggable build has offers without products: pretend it worked.
            _results.tryEmit(if (debuggable) TipResult.THANKS else TipResult.FAILED)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
            )
            .build()
        val launched = client.launchBillingFlow(activity, params)
        if (launched.responseCode != BillingResponseCode.OK) _results.tryEmit(TipResult.FAILED)
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
        }
    }

    private suspend fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach { handle(it) }
            BillingResponseCode.USER_CANCELED -> _results.emit(TipResult.CANCELLED)
            // An earlier tip was never consumed, which blocks a new one: clear it and ask to try again.
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                finishLeftovers()
                _results.emit(TipResult.FAILED)
            }
            else -> _results.emit(TipResult.FAILED)
        }
    }

    private suspend fun handle(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> _results.emit(if (consume(purchase)) TipResult.THANKS else TipResult.FAILED)
            Purchase.PurchaseState.PENDING -> _results.emit(TipResult.PENDING)
            else -> Unit
        }
    }

    /** Consuming also acknowledges the purchase, which Google Play requires within three days. */
    private suspend fun consume(purchase: Purchase): Boolean {
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        return client.consumePurchase(params).billingResult.responseCode == BillingResponseCode.OK
    }

    /** A tip that cleared while the app was closed (a pending one, say) is consumed on the next start. */
    private suspend fun finishLeftovers() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingResponseCode.OK) return
        result.purchasesList
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { consume(it) }
    }
}

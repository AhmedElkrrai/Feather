package com.elkrrai.feather

import android.app.Application
import com.elkrrai.feather.domain.repository.ListRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class FeatherApp : Application() {

    @Inject lateinit var listRepository: ListRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { listRepository.ensureInbox() }
    }
}

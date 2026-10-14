package com.elkrrai.feather.feature.quickadd

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.elkrrai.feather.R
import com.elkrrai.feather.reminders.withAppLocale

/** The "Add task" tile in the Quick Settings panel: collapses the panel and opens quick add. */
class QuickAddTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.label = withAppLocale().getString(R.string.tile_label)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        val intent = QuickAddActivity.intent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

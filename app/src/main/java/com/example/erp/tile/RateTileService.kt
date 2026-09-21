package com.example.erp.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.erp.MainActivity
import com.example.erp.R
import com.example.erp.overlay.OverlayService

class RateTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        // Check if overlay permission is granted
        if (Settings.canDrawOverlays(this)) {
            // Launch overlay service
            val serviceIntent = Intent(this, OverlayService::class.java)
            startForegroundService(serviceIntent)
        } else {
            // Open settings to grant permission
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    1,
                    settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(settingsIntent)
            }
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.label = "Dolita"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)
        tile.state = Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = "Ver tasas del dolar"
        }
        tile.updateTile()
    }
}

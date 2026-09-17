package com.example.erp.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.erp.MainActivity
import com.example.erp.R

class RateTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivityAndCollapse(intent)
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

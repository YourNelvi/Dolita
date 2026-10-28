package com.example.erp.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Hands a card to the system chooser together with the caption it was drawn
 * from.
 *
 * Both travel in one intent on purpose. The picture is what makes the rate look
 * like the day's rate instead of a line of chat, and the caption is what keeps
 * the number selectable so the reader can paste it into a calculator. Sending
 * only the image would fix the first problem and create the second.
 *
 * The image is a best-effort upgrade: when rendering or writing fails, the
 * share still goes out as text. A share button that silently does nothing is
 * worse than one that degrades to what the app already did.
 */
object ShareImage {

    private const val DIR = "share"
    private const val FILE_NAME = "dolita_tasa.png"

    fun share(context: Context, card: ShareCard?, caption: String, chooserTitle: String) {
        val uri = card?.let { runCatching { write(context, it) }.getOrNull() }

        val send = if (uri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, caption)
            }
        }

        // The grant also has to sit on the chooser, or the app the user picks
        // out of it is not the app the grant was issued to.
        val chooser = Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    private fun write(context: Context, card: ShareCard) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        renderToCache(context, card)
    )

    private fun renderToCache(context: Context, card: ShareCard): File {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        // Clear first: every share redraws the card, and a leftover copy is a
        // rate from an earlier hour sitting in a folder the provider still
        // serves. One file, always the one just drawn.
        dir.listFiles()?.forEach { it.delete() }

        val file = File(dir, FILE_NAME)
        val bitmap = ShareCardRenderer.render(card)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }
}

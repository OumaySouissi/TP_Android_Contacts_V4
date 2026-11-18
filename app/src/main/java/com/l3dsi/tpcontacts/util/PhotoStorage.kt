package com.l3dsi.tpcontacts.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Gestion des photos des contacts.
 *
 * Une URI de galerie ou de caméra peut devenir illisible après redémarrage
 * (URI temporaire de permission). On copie donc systématiquement l'image
 * dans le stockage interne de l'application (filesDir/contact_photos) et
 * on enregistre en base le chemin absolu du fichier, qui reste valide.
 */
object PhotoStorage {

    private const val DIRECTORY = "contact_photos"

    /**
     * Copie l'image désignée par [uri] dans filesDir et retourne
     * le chemin absolu du fichier copié, ou null en cas d'échec.
     */
    fun copyToInternal(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            val destination = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            destination.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Décode la photo à partir de son chemin interne. */
    fun loadBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return BitmapFactory.decodeFile(path)
    }

    /** Supprime le fichier photo s'il existe (ex. remplacement ou suppression du contact). */
    fun deleteFile(path: String?) {
        path?.let { File(it).delete() }
    }
}

package com.l3dsi.tpcontacts.prefs

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager

/**
 * Point d'accès unique aux SharedPreferences de l'application.
 *
 * Les fragments et l'adapter ne lisent jamais les préférences directement :
 * ils demandent ici des valeurs déjà converties (taille en sp, couleur en
 * Int, booléen), ce qui centralise les clés et les valeurs par défaut.
 */
object PreferencesManager {

    const val KEY_TAILLE_POLICE = "taille_police"
    const val KEY_COULEUR_PREFEREE = "couleur_preferee"
    const val KEY_APPEL_DEPUIS_LISTE = "appel_depuis_liste"

    private const val DEFAULT_TAILLE = "16"
    private const val DEFAULT_COULEUR = "#000000"

    private fun prefs(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context)

    /** Taille de police en sp (14, 16 ou 20). */
    fun textSizeSp(context: Context): Float =
        prefs(context).getString(KEY_TAILLE_POLICE, DEFAULT_TAILLE)
            ?.toFloatOrNull() ?: DEFAULT_TAILLE.toFloat()

    /** Couleur préférée convertie en Int ARGB. */
    fun textColor(context: Context): Int =
        try {
            Color.parseColor(
                prefs(context).getString(KEY_COULEUR_PREFEREE, DEFAULT_COULEUR)
                    ?: DEFAULT_COULEUR
            )
        } catch (e: IllegalArgumentException) {
            Color.BLACK
        }

    /** Vrai si le bouton Appeler doit être affiché dans la liste. */
    fun callEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_APPEL_DEPUIS_LISTE, false)
}

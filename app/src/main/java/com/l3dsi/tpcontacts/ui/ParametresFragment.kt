package com.l3dsi.tpcontacts.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.l3dsi.tpcontacts.R

/**
 * Écran des préférences utilisateur (SharedPreferences, pas SQLite).
 * Les trois préférences sont déclarées dans res/xml/preferences.xml et
 * résumées avec leur valeur actuelle (useSimpleSummaryProvider).
 */
class ParametresFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}

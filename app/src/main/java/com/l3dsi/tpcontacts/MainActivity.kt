package com.l3dsi.tpcontacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.l3dsi.tpcontacts.databinding.ActivityMainBinding
import com.l3dsi.tpcontacts.ui.ListeContactsFragment
import com.l3dsi.tpcontacts.ui.ParametresFragment

/**
 * Activité unique hébergeant tous les fragments dans un
 * FragmentContainerView, avec la barre d'application et l'accès aux
 * paramètres.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Ne recrée le fragment de liste que lors du premier lancement :
        // après rotation, le FragmentManager restaure l'état existant.
        if (savedInstanceState == null) {
            showFragment(ListeContactsFragment(), addToBackStack = false)
        }
    }

    /** Remplace le contenu du conteneur ; addToBackStack empile pour le bouton Retour. */
    fun showFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .apply { if (addToBackStack) addToBackStack(null) }
            .commit()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_parametres -> {
                showFragment(ParametresFragment())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

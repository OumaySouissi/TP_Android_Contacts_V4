package com.l3dsi.tpcontacts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.l3dsi.tpcontacts.MainActivity
import com.l3dsi.tpcontacts.data.Contact
import com.l3dsi.tpcontacts.databinding.FragmentListeContactsBinding
import com.l3dsi.tpcontacts.prefs.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Liste des contacts : RecyclerView, filtrage au fil de la saisie,
 * bouton « + » et bouton Appeler par ligne selon la préférence.
 *
 * Le filtrage est fait en mémoire sur la liste déjà chargée depuis la base
 * (voir RAPPORT.md pour la justification).
 */
class ListeContactsFragment : Fragment() {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()

    private lateinit var adapter: ContactAdapter

    /** Liste complète issue de la base ; le filtre est appliqué par-dessus. */
    private var allContacts: List<Contact> = emptyList()
    private var currentFilter: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListeContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ContactAdapter(
            onItemClick = { contact -> openDetail(contact.id) },
            onCallClick = { contact -> ContactAdapter.dial(requireContext(), contact.telephone) }
        )
        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        // Restaurer le filtre après rotation
        currentFilter = savedInstanceState?.getString(KEY_FILTER) ?: ""
        binding.editFiltre.setText(currentFilter)

        binding.editFiltre.doAfterTextChanged { editable ->
            currentFilter = editable?.toString() ?: ""
            applyFilter()
        }

        binding.fabAjouter.setOnClickListener {
            openDetail(contactId = 0L)
        }

        // La liste est rechargée depuis SQLite chaque fois que le
        // ViewModel notifie une modification (ajout, édition, suppression).
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contacts.collectLatest { contacts ->
                    allContacts = contacts
                    applyFilter()
                }
            }
        }
    }

    /** Réapplique les préférences à chaque retour sur cet écran, sans redémarrage. */
    override fun onResume() {
        super.onResume()
        applyPreferences()
    }

    private fun applyPreferences() {
        val ctx = requireContext()
        adapter.applyPreferences(
            PreferencesManager.textSizeSp(ctx),
            PreferencesManager.textColor(ctx),
            PreferencesManager.callEnabled(ctx)
        )
    }

    /** Filtrage en mémoire : insensible à la casse sur nom, prénom, téléphone. */
    private fun applyFilter() {
        val q = currentFilter.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            allContacts
        } else {
            allContacts.filter { contact ->
                contact.nom.lowercase().contains(q) ||
                    contact.prenom.lowercase().contains(q) ||
                    contact.telephone.lowercase().contains(q)
            }
        }
        adapter.submitList(filtered)
    }

    private fun openDetail(contactId: Long) {
        (requireActivity() as MainActivity).showFragment(
            DetailContactFragment.newInstance(contactId)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_FILTER, currentFilter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_FILTER = "filter"
    }
}

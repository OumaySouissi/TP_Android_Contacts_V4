package com.l3dsi.tpcontacts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.l3dsi.tpcontacts.data.Contact
import com.l3dsi.tpcontacts.data.ContactDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel partagé (scope de l'activité) entre la liste, le détail et la carte.
 *
 * Expose la liste des contacts sous forme de StateFlow et lance tous les
 * accès à la base dans viewModelScope sur Dispatchers.IO : aucun accès
 * disque ne se fait sur le thread principal.
 */
class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ContactDao(application)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    init {
        refresh()
    }

    /** Recharge toute la liste depuis SQLite. */
    fun refresh() {
        viewModelScope.launch {
            _contacts.value = withContext(Dispatchers.IO) { dao.getAll() }
        }
    }

    /** Retourne un contact par son _id, ou null s'il n'existe pas. */
    suspend fun getContact(id: Long): Contact? =
        withContext(Dispatchers.IO) { dao.getById(id) }

    /**
     * Insère (id == 0) ou met à jour le contact.
     * Retourne true si l'opération a modifié au moins une ligne.
     */
    suspend fun saveContact(contact: Contact): Boolean {
        val rows = withContext(Dispatchers.IO) {
            if (contact.id == 0L) dao.insert(contact) else dao.update(contact).toLong()
        }
        if (rows > 0) refresh()
        return rows > 0
    }

    /** Supprime le contact [id] puis recharge la liste. Retourne true si supprimé. */
    suspend fun deleteContact(id: Long): Boolean {
        val rows = withContext(Dispatchers.IO) { dao.delete(id) }
        if (rows > 0) refresh()
        return rows > 0
    }
}

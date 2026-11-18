package com.l3dsi.tpcontacts.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.l3dsi.tpcontacts.R
import com.l3dsi.tpcontacts.data.Contact
import com.l3dsi.tpcontacts.util.PhotoStorage

/**
 * Adapter de la liste des contacts.
 *
 * Les valeurs d'affichage (taille en sp, couleur en Int, affichage du bouton
 * Appeler) sont reçues déjà converties depuis PreferencesManager : l'adapter
 * ne connaît ni les clés ni le format des préférences.
 */
class ContactAdapter(
    private val onItemClick: (Contact) -> Unit,
    private val onCallClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    var textSizeSp: Float = 16f
        private set
    var textColor: Int = 0xFF000000.toInt()
        private set
    var callEnabled: Boolean = false
        private set

    /** Applique les préférences converties puis redessine les lignes visibles. */
    fun applyPreferences(sizeSp: Float, color: Int, showCall: Boolean) {
        textSizeSp = sizeSp
        textColor = color
        callEnabled = showCall
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgPhoto: ImageView = itemView.findViewById(R.id.img_photo)
        private val tvNom: TextView = itemView.findViewById(R.id.tv_nom)
        private val tvPrenom: TextView = itemView.findViewById(R.id.tv_prenom)
        private val tvTelephone: TextView = itemView.findViewById(R.id.tv_telephone)
        private val btnAppeler: android.widget.ImageButton =
            itemView.findViewById(R.id.btn_appeler)

        fun bind(contact: Contact) {
            tvNom.text = contact.nom
            tvPrenom.text = contact.prenom
            tvTelephone.text = contact.telephone

            tvNom.textSize = textSizeSp
            tvPrenom.textSize = textSizeSp
            tvTelephone.textSize = textSizeSp

            tvNom.setTextColor(textColor)
            tvPrenom.setTextColor(textColor)

            val bitmap = PhotoStorage.loadBitmap(contact.photoUri)
            if (bitmap != null) {
                imgPhoto.setImageBitmap(bitmap)
            } else {
                imgPhoto.setImageResource(R.drawable.ic_person_placeholder)
            }

            btnAppeler.visibility = if (callEnabled) View.VISIBLE else View.GONE

            // Le clic sur le bouton Appeler ne doit PAS ouvrir la fiche détaillée.
            btnAppeler.setOnClickListener { onCallClick(contact) }
            itemView.setOnClickListener { onItemClick(contact) }
        }
    }

    /** Lance le composeur téléphonique via ACTION_DIAL (aucune permission requise). */
    companion object {
        fun dial(context: android.content.Context, phone: String) {
            if (phone.isBlank()) {
                Toast.makeText(context, R.string.aucun_numero, Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, R.string.aucune_app_telephone, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
    override fun areItemsTheSame(oldItem: Contact, newItem: Contact) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Contact, newItem: Contact) = oldItem == newItem
}

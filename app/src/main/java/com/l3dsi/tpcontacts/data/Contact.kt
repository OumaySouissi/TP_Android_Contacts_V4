package com.l3dsi.tpcontacts.data

/**
 * Modèle d'un contact.
 * [id] vaut 0 tant que le contact n'est pas enregistré en base,
 * puis prend la valeur de la colonne _id après insertion.
 */
data class Contact(
    val id: Long = 0,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

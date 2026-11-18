package com.l3dsi.tpcontacts.data

/**
 * Contrat de la table contacts : noms de table et de colonnes
 * regroupés en constantes. Aucun nom de colonne n'est écrit en dur
 * ailleurs dans le code.
 */
object ContactContract {
    const val TABLE_NAME = "contacts"

    const val COL_ID = "_id"
    const val COL_NOM = "nom"
    const val COL_PRENOM = "prenom"
    const val COL_TELEPHONE = "telephone"
    const val COL_EMAIL = "email"
    const val COL_PHOTO_URI = "photo_uri"
    const val COL_LATITUDE = "latitude"
    const val COL_LONGITUDE = "longitude"

    const val ALL_COLUMNS = "$COL_ID, $COL_NOM, $COL_PRENOM, $COL_TELEPHONE, " +
        "$COL_EMAIL, $COL_PHOTO_URI, $COL_LATITUDE, $COL_LONGITUDE"

    /** Tri demandé : nom puis prénom, sans tenir compte de la casse. */
    const val ORDER_BY = "$COL_NOM COLLATE NOCASE ASC, $COL_PRENOM COLLATE NOCASE ASC"
}

package com.l3dsi.tpcontacts.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/**
 * Seule classe contenant du SQL.
 *
 * Toutes les requêtes à valeurs variables utilisent des arguments `?`
 * (selectionArgs) : aucune saisie utilisateur n'est concaténée dans le SQL,
 * ce qui protège contre l'injection SQL et les apostrophes (ex. « O'Neil »).
 *
 * Les méthodes sont bloquantes : elles doivent être appelées hors du thread
 * principal (viewModelScope + withContext(Dispatchers.IO)).
 */
class ContactDao(context: Context) {

    private val dbHelper = ContactDbHelper.getInstance(context)

    private val readableDb: SQLiteDatabase get() = dbHelper.readableDatabase
    private val writableDb: SQLiteDatabase get() = dbHelper.writableDatabase

    // region Insert / Update / Delete

    /** Insère un contact et retourne son _id (rowId), ou -1 en cas d'échec. */
    fun insert(contact: Contact): Long {
        return writableDb.insert(
            ContactContract.TABLE_NAME,
            null,
            contact.toContentValues()
        )
    }

    /** Met à jour le contact identifié par [Contact.id] ; retourne le nombre de lignes modifiées. */
    fun update(contact: Contact): Int {
        return writableDb.update(
            ContactContract.TABLE_NAME,
            contact.toContentValues(),
            "${ContactContract.COL_ID} = ?",
            arrayOf(contact.id.toString())
        )
    }

    /** Supprime le contact [id] ; retourne le nombre de lignes supprimées. */
    fun delete(id: Long): Int {
        return writableDb.delete(
            ContactContract.TABLE_NAME,
            "${ContactContract.COL_ID} = ?",
            arrayOf(id.toString())
        )
    }

    // endregion

    // region Requêtes

    /** Tous les contacts, triés par nom puis prénom (COLLATE NOCASE). */
    fun getAll(): List<Contact> = query(
        selection = null,
        selectionArgs = null
    )

    /** Un contact par son _id, ou null s'il n'existe pas. */
    fun getById(id: Long): Contact? {
        return readableDb.query(
            ContactContract.TABLE_NAME,
            ContactContract.ALL_COLUMNS.split(", ").toTypedArray(),
            "${ContactContract.COL_ID} = ?",
            arrayOf(id.toString()),
            null, null,
            ContactContract.ORDER_BY
        ).use { cursor ->
            if (cursor.moveToFirst()) cursorToContact(cursor) else null
        }
    }

    /**
     * Recherche insensible à la casse sur nom, prénom et téléphone.
     * Les jokers LIKE (%, _) éventuellement saisis sont neutralisés par un
     * échappement avec la clause ESCAPE.
     */
    fun search(texte: String): List<Contact> {
        if (texte.isBlank()) return getAll()
        val motif = "%${escapeLike(texte.trim())}%"
        return query(
            selection = "(${ContactContract.COL_NOM} LIKE ? ESCAPE '\\' " +
                "OR ${ContactContract.COL_PRENOM} LIKE ? ESCAPE '\\' " +
                "OR ${ContactContract.COL_TELEPHONE} LIKE ? ESCAPE '\\')",
            selectionArgs = arrayOf(motif, motif, motif)
        )
    }

    private fun query(selection: String?, selectionArgs: Array<String>?): List<Contact> {
        return readableDb.query(
            ContactContract.TABLE_NAME,
            ContactContract.ALL_COLUMNS.split(", ").toTypedArray(),
            selection,
            selectionArgs,
            null, null,
            ContactContract.ORDER_BY
        ).use { cursor ->
            val result = ArrayList<Contact>(cursor.count)
            while (cursor.moveToNext()) {
                result.add(cursorToContact(cursor))
            }
            result
        }
    }

    // endregion

    // region Helpers privés

    private fun Contact.toContentValues(): ContentValues = ContentValues().apply {
        put(ContactContract.COL_NOM, nom)
        put(ContactContract.COL_PRENOM, prenom)
        put(ContactContract.COL_TELEPHONE, telephone)
        if (email.isNullOrEmpty()) putNull(ContactContract.COL_EMAIL)
        else put(ContactContract.COL_EMAIL, email)
        if (photoUri.isNullOrEmpty()) putNull(ContactContract.COL_PHOTO_URI)
        else put(ContactContract.COL_PHOTO_URI, photoUri)
        val lat = latitude
        val lng = longitude
        if (lat == null || lng == null) {
            putNull(ContactContract.COL_LATITUDE)
            putNull(ContactContract.COL_LONGITUDE)
        } else {
            put(ContactContract.COL_LATITUDE, lat)
            put(ContactContract.COL_LONGITUDE, lng)
        }
    }

    private fun cursorToContact(cursor: Cursor): Contact = Contact(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(ContactContract.COL_ID)),
        nom = cursor.getString(cursor.getColumnIndexOrThrow(ContactContract.COL_NOM)),
        prenom = cursor.getString(cursor.getColumnIndexOrThrow(ContactContract.COL_PRENOM)),
        telephone = cursor.getString(cursor.getColumnIndexOrThrow(ContactContract.COL_TELEPHONE)),
        email = cursor.getStringOrNull(ContactContract.COL_EMAIL),
        photoUri = cursor.getStringOrNull(ContactContract.COL_PHOTO_URI),
        latitude = cursor.getDoubleOrNull(ContactContract.COL_LATITUDE),
        longitude = cursor.getDoubleOrNull(ContactContract.COL_LONGITUDE)
    )

    private fun Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getDoubleOrNull(column: String): Double? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getDouble(index)
    }

    private fun escapeLike(input: String): String =
        input.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    // endregion
}

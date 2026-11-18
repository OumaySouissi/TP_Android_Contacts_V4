package com.l3dsi.tpcontacts.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Helper SQLite de la base contacts.db.
 *
 * - onCreate : exécuté une seule fois, quand le fichier contacts.db
 *   n'existe pas encore. Crée la table et insère les contacts de démonstration.
 * - onUpgrade : exécuté quand DATABASE_VERSION augmente. Ici on recrée
 *   la table (perte des données) car le schéma est encore en évolution.
 *
 * L'instance est unique (singleton) et construite avec applicationContext
 * pour éviter toute fuite du contexte d'une Activity.
 */
class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "contacts.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also {
                    instance = it
                }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${ContactContract.TABLE_NAME} (
                ${ContactContract.COL_ID}        INTEGER PRIMARY KEY AUTOINCREMENT,
                ${ContactContract.COL_NOM}        TEXT NOT NULL,
                ${ContactContract.COL_PRENOM}     TEXT NOT NULL,
                ${ContactContract.COL_TELEPHONE}  TEXT NOT NULL,
                ${ContactContract.COL_EMAIL}      TEXT,
                ${ContactContract.COL_PHOTO_URI}  TEXT,
                ${ContactContract.COL_LATITUDE}   REAL,
                ${ContactContract.COL_LONGITUDE}  REAL
            )
            """.trimIndent()
        )
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${ContactContract.TABLE_NAME}")
        onCreate(db)
    }

    /** Contacts de démonstration : 5 contacts, dont 2 sans coordonnées GPS. */
    private fun seed(db: SQLiteDatabase) {
        val demo = listOf(
            Contact(
                nom = "Ben Ali", prenom = "Sami", telephone = "+216 20 000 001",
                email = "sami.benali@example.com",
                latitude = 36.8065, longitude = 10.1815
            ),
            Contact(
                nom = "Trabelsi", prenom = "Ines", telephone = "20 000 002",
                email = "ines.trabelsi@example.com",
                latitude = 33.8869, longitude = 10.4234
            ),
            Contact(
                nom = "Gharbi", prenom = "Mehdi", telephone = "+216 20 000 003",
                email = "mehdi.gharbi@example.com"
            ),
            Contact(
                nom = "Jlassi", prenom = "Rania", telephone = "20 000 004",
                email = null
            ),
            Contact(
                nom = "Mansouri", prenom = "Karim", telephone = "+216 20 000 005",
                email = "karim.mansouri@example.com",
                latitude = null, longitude = null
            )
        )
        demo.forEach { contact ->
            val values = ContentValues().apply {
                put(ContactContract.COL_NOM, contact.nom)
                put(ContactContract.COL_PRENOM, contact.prenom)
                put(ContactContract.COL_TELEPHONE, contact.telephone)
                put(ContactContract.COL_EMAIL, contact.email)
                put(ContactContract.COL_PHOTO_URI, contact.photoUri)
                put(ContactContract.COL_LATITUDE, contact.latitude)
                put(ContactContract.COL_LONGITUDE, contact.longitude)
            }
            db.insert(ContactContract.TABLE_NAME, null, values)
        }
    }
}

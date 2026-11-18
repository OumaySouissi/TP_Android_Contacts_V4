# TP Android V4 — Gestion des contacts

Application Android en **Kotlin** (interfaces XML) : gestion de contacts avec
persistance **SQLite** (API native `SQLiteOpenHelper`), **préférences
utilisateur** (`PreferenceFragmentCompat`) et **Google Maps**.

## Fonctionnalités

- Liste des contacts avec photo, nom, prénom, téléphone ; filtre au fil de la
  saisie ; bouton « + » ; bouton Appeler par ligne (optionnel).
- Fiche contact : création, modification, suppression (avec confirmation),
  actions Appeler (`ACTION_DIAL`), E-mail, Galerie, Caméra, Carte.
- Préférences : taille de police, couleur du texte, appel depuis la liste —
  appliquées immédiatement sans redémarrage.
- Persistance : base `contacts.db` + SharedPreferences, conservées après
  fermeture complète et redémarrage.

## Ouvrir le projet

1. Ouvrir le dossier `TP_Android_Contacts_V4` dans **Android Studio**
   (Ladybug ou plus récent).
2. Copier `local.properties.example` en `local.properties` et adapter
   `sdk.dir`.
3. Pour la carte, renseigner votre clé API Google Maps dans
   `local.properties` :

   ```
   MAPS_API_KEY=VOTRE_CLE_API_ICI
   ```

   La clé n'est jamais versionnée : elle est injectée via
   `manifestPlaceholders` au moment du build.

4. Laisser Gradle synchroniser puis lancer sur un émulateur/appareil
   (API 24+).

## Structure

```
app/src/main/java/com/l3dsi/tpcontacts/
├── MainActivity.kt                  # Activité unique, toolbar, navigation fragments
├── data/
│   ├── Contact.kt                   # Modèle
│   ├── ContactContract.kt           # Noms table/colonnes
│   ├── ContactDbHelper.kt           # SQLiteOpenHelper (singleton), seed 5 contacts
│   └── ContactDao.kt                # Tout le SQL, requêtes paramétrées
├── prefs/PreferencesManager.kt      # Lecture centralisée des SharedPreferences
├── ui/
│   ├── ContactViewModel.kt          # StateFlow + CRUD sur Dispatchers.IO
│   ├── ContactAdapter.kt            # RecyclerView
│   ├── ListeContactsFragment.kt     # Liste + filtre + préférences
│   ├── DetailContactFragment.kt     # CRUD + actions + validation
│   ├── CarteContactFragment.kt      # Google Maps + marqueur
│   └── ParametresFragment.kt        # PreferenceFragmentCompat
└── util/PhotoStorage.kt             # Copie interne des photos (filesDir)
```

## Documentation

Voir [`RAPPORT.md`](RAPPORT.md) pour le compte rendu complet (schéma,
stratégie threads, choix de filtrage, gestion photos, tests,
questions de compréhension).

## Clé API Google Maps

La carte nécessite une clé API. Créez-la dans la console Google Cloud,
restrez-la sur l'empreinte SHA-1 du keystore de debug, et placez-la dans
`local.properties` (non versionné) — voir `local.properties.example`.
Sans clé, l'application fonctionne mais la carte reste grise.

## Licence

Projet académique — TP Android V4.

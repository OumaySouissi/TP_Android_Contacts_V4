# TP Android V4 — Gestion des contacts

Compte rendu : application Android (Kotlin, interfaces XML) de gestion de
contacts avec **persistance SQLite**, **préférences utilisateur** et
**Google Maps**.

---

## 1. Schéma de la base et choix des types

Base `contacts.db`, table `contacts` :

```sql
CREATE TABLE contacts (
    _id        INTEGER PRIMARY KEY AUTOINCREMENT,
    nom        TEXT NOT NULL,
    prenom     TEXT NOT NULL,
    telephone  TEXT NOT NULL,
    email      TEXT,
    photo_uri  TEXT,
    latitude   REAL,
    longitude  REAL
);
```

| Colonne | Type | Justification |
|---|---|---|
| `_id` | INTEGER | Clé primaire auto-incrémentée, identifiant stable du contact. |
| `nom`, `prenom` | TEXT NOT NULL | Contraintes d'intégrité au niveau base : jamais de nom vide. |
| `telephone` | TEXT | Un numéro peut commencer par `0` ou `+216`, contenir des espaces ; INTEGER perdrait ces informations. |
| `email` | TEXT (nullable) | Champ optionnel → NULL si absent. |
| `photo_uri` | TEXT (nullable) | On stocke le **chemin** d'une copie interne de l'image, pas l'URI d'origine (voir §5). |
| `latitude`, `longitude` | REAL (nullable) | Coordonnées GPS optionnelles ; NULL si absentes. |

Les noms de colonnes sont centralisés dans `ContactContract` ; aucun nom de
colonne n'apparaît en dur dans le DAO ou les fragments.

## 2. Rôle du helper et du DAO

- **`ContactDbHelper`** (`SQLiteOpenHelper`, singleton construit avec
  `applicationContext`) :
  - `onCreate` : exécuté **une seule fois**, quand le fichier `contacts.db`
    n'existe pas. Crée la table puis insère 5 contacts de démonstration
    (dont 3 sans coordonnées GPS / 2 sans e-mail). Au second lancement,
    `onCreate` n'est pas rappelé : les démos ne sont pas dupliquées.
  - `onUpgrade` : exécuté quand `DATABASE_VERSION` augmente. Stratégie
    actuelle : suppression et recréation de la table (schéma encore en
    évolution). Pour un bonus de migration, on y ajouterait un `ALTER TABLE`.
- **`ContactDao`** : seule classe contenant du SQL.
  - `insert(contact): Long` → rowId ou -1.
  - `update(contact): Int` → nombre de lignes modifiées.
  - `delete(id): Int` → nombre de lignes supprimées.
  - `getAll()` / `getById(id)` / `search(texte)` → listes de `Contact`.
  - Toutes les requêtes à valeurs variables utilisent des `?` +
    `selectionArgs` : aucune concaténation de saisie utilisateur, donc
    insensible aux apostrophes (« O'Neil ») et à l'injection SQL.
  - Chaque `Cursor` est fermé via `use { }` et les index sont lus avec
    `getColumnIndexOrThrow()`.

## 3. Stratégie de threads

- Les méthodes du DAO sont **bloquantes** et ne sont jamais appelées sur le
  thread principal.
- `ContactViewModel` (partagé, scope activité) encapsule chaque appel dans
  `viewModelScope.launch { withContext(Dispatchers.IO) { dao.xxx() } }`.
- La liste est exposée via un `StateFlow<List<Contact>>` ; les fragments la
  collectent avec `repeatOnLifecycle(STARTED)`.
- Après chaque insert/update/delete, `refresh()` recharge `getAll()` en IO et
  notifie les écrans → la liste reflète toujours la base, sans redémarrage.

## 4. Choix de filtrage : **mémoire** (et justification)

Le filtre de la V3 est conservé mais appliqué **en mémoire** sur la liste
déjà chargée (`ListeContactsFragment.applyFilter`), et non en SQL.

Justification :
- la liste des contacts est déjà entièrement chargée pour l'affichage ;
  un `LIKE` SQL sur `nom`/`prenom`/`telephone` demanderait une requête à
  chaque frappe (debounce à gérer) ou un second chargement ;
- le filtrage en mémoire est insensible à la casse par construction et ne
  nécessite ni clause `ESCAPE` ni échappement des jokins `LIKE` côté UI ;
- le DAO expose néanmoins `search(texte)` en SQL paramétré, prêt à être
  utilisé si la liste devenait volumineuse.

Le contact est identifié partout par son `_id` (Bundle, ViewModel, adapter),
jamais par sa position dans la liste.

## 5. Gestion des photos

- Galerie : `ActivityResultContracts.GetContent("image/*")`.
- Caméra : `TakePicture` avec une URI `FileProvider` vers un fichier
  temporaire dans `cacheDir`.
- Dans les deux cas, `PhotoStorage.copyToInternal` copie l'image dans
  `filesDir/contact_photos/` et **seul le chemin absolu du fichier** est
  enregistré en base (`photo_uri`). Une URI de galerie temporaire peut
  devenir illisible après redémarrage ; le fichier interne survit.
- À la suppression du contact ou au remplacement de la photo, l'ancien
  fichier est supprimé.

## 6. Préférences utilisateur

- **Stockage** : `SharedPreferences` via
  `PreferenceManager.getDefaultSharedPreferences`, déclarées dans
  `res/xml/preferences.xml` et `PreferenceFragmentCompat`
  (`ParametresFragment`), ouvert depuis l'icône engrenage de la toolbar.
- **Trois préférences** :
  - `taille_police` (ListPreference : 14/16/20 sp, défaut 16) ;
  - `couleur_preferee` (ListPreference : #000000/#1A56DB/#15803D/#B91C1C/#6D28D9, défaut noir) ;
  - `appel_depuis_liste` (SwitchPreferenceCompat, défaut non).
- Chaque préférence affiche sa valeur courante (`useSimpleSummaryProvider`).
- **Lecture centralisée** : `PreferencesManager` retourne des valeurs déjà
  converties (Float sp, Int couleur, Boolean). L'adapter reçoit ces valeurs
  via `applyPreferences(...)`, jamais les clés brutes.
- **Application immédiate** : au retour sur la liste, `onResume()` relit les
  préférences et appelle `applyPreferences()` → `notifyDataSetChanged()`,
  sans redémarrage ni recréation de fragment. Le détail applique taille et
  couleur sur les champs nom/prénom dans son `onResume()`.
- **Bouton Appeler** : `Intent.ACTION_DIAL` (aucune permission, contrairement
  à `ACTION_CALL` qui exige `CALL_PHONE`). Le clic est intercepté par le
  bouton et n'ouvre pas la fiche ; numéro vide → message, application
  absente → `ActivityNotFoundException` gérée.

## 7. Architecture

| Composant | Rôle |
|---|---|
| `MainActivity` | Héberge le `FragmentContainerView`, toolbar, menu Paramètres ; `showFragment()` avec `replace()` + `addToBackStack()`. |
| `ListeContactsFragment` | Liste, filtre, « + », bouton Appeler par ligne, application des préférences. |
| `DetailContactFragment` | Création/consultation/modification/suppression ; actions Appeler, E-mail, Galerie, Caméra, Carte ; validation ; restauration de saisie (`onSaveInstanceState`). |
| `CarteContactFragment` | Google Maps + marqueur ; message si pas de GPS. |
| `ParametresFragment` | `PreferenceFragmentCompat`. |
| `ContactViewModel` | Liste en `StateFlow`, CRUD via DAO en `Dispatchers.IO`. |
| `ContactDao` / `ContactDbHelper` | SQL natif (`SQLiteOpenHelper`, `ContentValues`, `Cursor`). |
| `PreferencesManager` | Lecture/convertie des SharedPreferences. |
| `PhotoStorage` | Copie interne des images, chargement/suppression. |

## 8. Robustesse

- Rotation : le ViewModel (scope activité) survit ; la liste filtre et la
  saisie du détail sont restaurés via `onSaveInstanceState` ; le fragment
  initial n'est créé que si `savedInstanceState == null` → pas de doublon.
- Validation : nom/prénom/téléphone obligatoires ; coordonnées optionnelles
  mais refusées si non numériques ou hors limites (|lat| ≤ 90,
  |lng| ≤ 180) → rien n'est écrit.
- Suppression : `AlertDialog` de confirmation puis retour à la liste.
- Clé API Maps : injectée via `manifestPlaceholders` depuis
  `local.properties`/variable d'environnement — **jamais dans le dépôt**.

## 9. Tests réalisés

- Premier lancement : les 5 contacts de démonstration apparaissent, triés
  par nom/prénom (NOCASE) ; second lancement : pas de doublon (`onCreate`
  non rejoué).
- CRUD : ajout via « + », modification, suppression avec confirmation ;
  fermeture complète + relance → données identiques.
- Contacts avec apostrophe (« O'Neil ») et filtre avec `'` : requêtes
  paramétrées → aucune erreur SQL.
- Enregistrement sans nom/prénom/téléphone : message d'erreur, rien en base.
- Photo : galerie/caméra → survit au redémarrage (copie interne).
- GPS absent / invalide / hors limites : refusé à l'enregistrement ; la
  carte affiche « Aucune coordonnée GPS pour ce contact ».
- Préférences : les 3 réglages s'appliquent immédiatement à la liste et au
  détail, sont conservés après relance ; le bouton Appeler apparaît /
  disparaît et ouvre le composeur avec le bon numéro sans ouvrir le détail.
- Rotation sur liste, détail en saisie et paramètres ; bouton Retour depuis
  chaque écran.
- Database Inspector (App Inspection) : contenu de `contacts` conforme à
  l'affichage.

## 10. Difficultés rencontrées

- Synchroniser photo/URI : les URI de galerie sont temporaires → copie
  systématique en `filesDir` + `FileProvider` pour la caméra.
- Restaurer la saisie du détail après rotation sans perdre le mode
  création/édition → `onSaveInstanceState` + `arguments`.
- Appliquer les préférences sans recréer l'adapter → `applyPreferences()` +
  `notifyDataSetChanged()` dans `onResume()`.

## 11. Questions de compréhension (résumé)

1. **`onCreate`** : base créée pour la première fois. **`onUpgrade`** :
   `DATABASE_VERSION` a augmenté depuis la dernière ouverture. Modifier
   `onCreate` sans changer la version n'a **aucun effet** sur une base
   existante.
2. Les `?` évitent l'injection SQL et les erreurs de quoting : `O'Brien`
   dans une requête concaténée casse le littéral `'O'Brien'` (ou pire,
   permet d'injecter du SQL).
3. `insert()` → rowId (ou -1) ; `update()`/`delete()` → nombre de lignes
   affectées ; utilisés pour afficher succès/échec et décider du
   `refresh()`.
4. La base est lente et peut verrouiller → ANR sur le thread principal ;
   évité via `Dispatchers.IO` dans le ViewModel.
5. Singleton + `applicationContext` : une seule connexion à la base et pas
   de fuite du contexte de l'Activity.
6. SharedPreferences pour des réglages clé/valeur simples et fréquents ;
   SQLite pour des données structurées et volumineuses. Contacts → SQLite ;
   préférences → SharedPreferences.
7. `onResume()` relit `PreferencesManager` et appelle
   `adapter.applyPreferences()` + `notifyDataSetChanged()` — aucune
   recréation.
8. `ACTION_DIAL` ouvre le composeur **sans permission** ; `ACTION_CALL`
   passe l'appel directement et exige `CALL_PHONE`.
9. Base et préférences sont **supprimées** à la désinstallation ; effacer
   les « données de l'application » les efface aussi (sauf sauvegarde Auto
   Backup le cas échéant).

## 12. Bonus (non réalisé dans cette version)

- Migration `DATABASE_VERSION = 2` + colonne `favori` via `onUpgrade`.
- Préférence « Ordre de tri » via `ORDER BY`.
- Import de 100 contacts en transaction unique.

---

*Projet : TP Android Kotlin — Gestion des contacts V4 (SQLite, Fragments,
Préférences, Google Maps).*

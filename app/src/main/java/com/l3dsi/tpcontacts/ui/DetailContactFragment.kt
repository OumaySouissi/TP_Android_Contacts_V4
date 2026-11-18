package com.l3dsi.tpcontacts.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.l3dsi.tpcontacts.MainActivity
import com.l3dsi.tpcontacts.R
import com.l3dsi.tpcontacts.data.Contact
import com.l3dsi.tpcontacts.databinding.FragmentDetailContactBinding
import com.l3dsi.tpcontacts.prefs.PreferencesManager
import com.l3dsi.tpcontacts.util.PhotoStorage
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fiche contact : création (id = 0), consultation, modification et
 * suppression. Contient aussi les actions Appeler, E-mail, Galerie,
 * Caméra et Carte.
 *
 * La photo est copiée dans le stockage interne (filesDir) : seul le chemin
 * du fichier est conservé en base, de sorte qu'elle reste lisible après
 * redémarrage de l'application ou de l'appareil.
 */
class DetailContactFragment : Fragment() {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()

    private var contactId: Long = 0L
    private var photoPath: String? = null

    /** Fichier temporaire utilisé comme cible de la photo prise par la caméra. */
    private var cameraFile: File? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPhotoPicked(it) }
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraFile?.let { file ->
                val contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                onPhotoPicked(contentUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = arguments?.getLong(ARG_CONTACT_ID) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restauration de la saisie en cours après rotation / recréation.
        savedInstanceState?.let { state ->
            binding.editNom.setText(state.getString(KEY_NOM))
            binding.editPrenom.setText(state.getString(KEY_PRENOM))
            binding.editTelephone.setText(state.getString(KEY_TELEPHONE))
            binding.editEmail.setText(state.getString(KEY_EMAIL))
            binding.editLatitude.setText(state.getString(KEY_LATITUDE))
            binding.editLongitude.setText(state.getString(KEY_LONGITUDE))
            photoPath = state.getString(KEY_PHOTO)
            photoPath?.let { loadPhoto() }
        }

        if (savedInstanceState == null) {
            if (contactId == 0L) {
                // Mode création : champs vides.
                setTitle(getString(R.string.titre_creation))
                binding.btnSupprimer.visibility = View.GONE
                binding.btnCarte.visibility = View.GONE
            } else {
                loadContact()
            }
        } else if (contactId == 0L) {
            // Recréation en mode création : conserver le titre.
            setTitle(getString(R.string.titre_creation))
            binding.btnSupprimer.visibility = View.GONE
            binding.btnCarte.visibility = View.GONE
        } else {
            loadContact()
        }

        setupButtons()
        applyTextPreferences()
    }

    private fun setupButtons() {
        binding.btnEnregistrer.setOnClickListener { save() }
        binding.btnAnnuler.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnSupprimer.setOnClickListener { confirmDelete() }
        binding.btnGalerie.setOnClickListener { pickImage.launch("image/*") }
        binding.btnCamera.setOnClickListener { openCamera() }
        binding.btnAppeler.setOnClickListener {
            ContactAdapter.dial(requireContext(), binding.editTelephone.text.toString())
        }
        binding.btnEmail.setOnClickListener { sendEmail() }
        binding.btnCarte.setOnClickListener {
            (requireActivity() as MainActivity).showFragment(
                CarteContactFragment.newInstance(contactId)
            )
        }
    }

    /** Charge le contact depuis la base (jamais de données figées). */
    private fun loadContact() {
        viewLifecycleOwner.lifecycleScope.launch {
            val contact = viewModel.getContact(contactId)
            if (contact == null) {
                Toast.makeText(
                    requireContext(), R.string.contact_introuvable, Toast.LENGTH_LONG
                ).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
                return@launch
            }
            setTitle("${contact.nom} ${contact.prenom}")
            binding.editNom.setText(contact.nom)
            binding.editPrenom.setText(contact.prenom)
            binding.editTelephone.setText(contact.telephone)
            binding.editEmail.setText(contact.email ?: "")
            binding.editLatitude.setText(contact.latitude?.toString() ?: "")
            binding.editLongitude.setText(contact.longitude?.toString() ?: "")
            photoPath = contact.photoUri
            loadPhoto()
        }
    }

    private fun setTitle(text: String) {
        (requireActivity() as? MainActivity)?.supportActionBar?.title = text
    }

    private fun onPhotoPicked(uri: Uri) {
        // Copie dans le stockage interne : l'URI source peut devenir illisible.
        val path = PhotoStorage.copyToInternal(requireContext(), uri)
        if (path != null) {
            val oldPath = photoPath
            photoPath = path
            loadPhoto()
            oldPath?.let { PhotoStorage.deleteFile(it) }
        } else {
            Toast.makeText(requireContext(), R.string.photo_absente, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPhoto() {
        val bitmap = PhotoStorage.loadBitmap(photoPath)
        if (bitmap != null) binding.imgPhoto.setImageBitmap(bitmap)
        else binding.imgPhoto.setImageResource(R.drawable.ic_person_placeholder)
    }

    private fun openCamera() {
        val dir = File(requireContext().cacheDir, "camera").apply { mkdirs() }
        cameraFile = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            cameraFile!!
        )
        takePicture.launch(uri)
    }

    /**
     * Validation : nom, prénom et téléphone obligatoires ; coordonnées
     * GPS optionnelles mais, si saisies, numériques et dans les limites.
     * Rien d'invalide n'est écrit en base.
     */
    private fun save() {
        val nom = binding.editNom.text.toString().trim()
        val prenom = binding.editPrenom.text.toString().trim()
        val telephone = binding.editTelephone.text.toString().trim()
        val email = binding.editEmail.text.toString().trim().ifEmpty { null }
        val latText = binding.editLatitude.text.toString().trim()
        val lngText = binding.editLongitude.text.toString().trim()

        if (nom.isEmpty() || prenom.isEmpty() || telephone.isEmpty()) {
            Toast.makeText(requireContext(), R.string.erreur_champs_requis, Toast.LENGTH_LONG).show()
            return
        }

        val latitude: Double?
        val longitude: Double?
        if (latText.isEmpty() && lngText.isEmpty()) {
            latitude = null
            longitude = null
        } else {
            val lat = latText.toDoubleOrNull()
            val lng = lngText.toDoubleOrNull()
            if (lat == null || lng == null ||
                lat !in -90.0..90.0 || lng !in -180.0..180.0
            ) {
                Toast.makeText(requireContext(), R.string.erreur_gps, Toast.LENGTH_LONG).show()
                return
            }
            latitude = lat
            longitude = lng
        }

        val contact = Contact(
            id = contactId,
            nom = nom,
            prenom = prenom,
            telephone = telephone,
            email = email,
            photoUri = photoPath,
            latitude = latitude,
            longitude = longitude
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val ok = viewModel.saveContact(contact)
            if (ok) {
                Toast.makeText(requireContext(), "Contact enregistré", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                Toast.makeText(requireContext(), "Échec de l'enregistrement", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.btn_supprimer)
            .setMessage(R.string.confirmation_suppression)
            .setPositiveButton(R.string.btn_supprimer) { _, _ -> deleteContact() }
            .setNegativeButton(R.string.btn_annuler, null)
            .show()
    }

    private fun deleteContact() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = viewModel.deleteContact(contactId)
            if (ok) {
                PhotoStorage.deleteFile(photoPath)
                Toast.makeText(requireContext(), "Contact supprimé", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                Toast.makeText(requireContext(), "Échec de la suppression", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendEmail() {
        val email = binding.editEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), R.string.aucune_app_email, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.aucune_app_email, Toast.LENGTH_SHORT).show()
        }
    }

    /** Applique taille et couleur des préférences sur nom/prénom du détail. */
    private fun applyTextPreferences() {
        val ctx = requireContext()
        val size = PreferencesManager.textSizeSp(ctx)
        val color = PreferencesManager.textColor(ctx)
        binding.editNom.textSize = size
        binding.editPrenom.textSize = size
        binding.editNom.setTextColor(color)
        binding.editPrenom.setTextColor(color)
    }

    override fun onResume() {
        super.onResume()
        // Les préférences peuvent avoir changé depuis Paramètres.
        applyTextPreferences()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_NOM, binding.editNom.text.toString())
        outState.putString(KEY_PRENOM, binding.editPrenom.text.toString())
        outState.putString(KEY_TELEPHONE, binding.editTelephone.text.toString())
        outState.putString(KEY_EMAIL, binding.editEmail.text.toString())
        outState.putString(KEY_LATITUDE, binding.editLatitude.text.toString())
        outState.putString(KEY_LONGITUDE, binding.editLongitude.text.toString())
        outState.putString(KEY_PHOTO, photoPath)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CONTACT_ID = "contact_id"
        private const val KEY_NOM = "nom"
        private const val KEY_PRENOM = "prenom"
        private const val KEY_TELEPHONE = "telephone"
        private const val KEY_EMAIL = "email"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_PHOTO = "photo"

        /** contactId = 0 → création ; sinon édition du contact correspondant. */
        fun newInstance(contactId: Long): DetailContactFragment =
            DetailContactFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_CONTACT_ID, contactId)
                }
            }
    }
}

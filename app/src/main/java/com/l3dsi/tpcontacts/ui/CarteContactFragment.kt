package com.l3dsi.tpcontacts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.l3dsi.tpcontacts.databinding.FragmentCarteContactBinding
import kotlinx.coroutines.launch

/**
 * Affiche la position du contact sur Google Maps avec un marqueur.
 * Si le contact n'a pas de coordonnées GPS, un message explicite est affiché.
 */
class CarteContactFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCarteContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()

    private var contactId: Long = 0L
    private var googleMap: GoogleMap? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = arguments?.getLong(ARG_CONTACT_ID) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarteContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Le SupportMapFragment est déclaré dans le layout : on récupère
        // l'instance et on demande le callback de carte prête.
        val mapFragment = childFragmentManager.findFragmentById(
            com.l3dsi.tpcontacts.R.id.map
        ) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        viewLifecycleOwner.lifecycleScope.launch {
            val contact = viewModel.getContact(contactId)
            latitude = contact?.latitude
            longitude = contact?.longitude
            updateMapOrMessage()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        updateMapOrMessage()
    }

    /** Affiche le marqueur si les coordonnées sont valides, sinon le message. */
    private fun updateMapOrMessage() {
        val map = googleMap ?: return
        val lat = latitude
        val lng = longitude
        if (lat != null && lng != null &&
            lat in -90.0..90.0 && lng in -180.0..180.0
        ) {
            binding.tvNoPosition.visibility = View.GONE
            map.addMarker(MarkerOptions().position(LatLng(lat, lng)))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
        } else {
            binding.tvNoPosition.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        _binding = null
    }

    companion object {
        private const val ARG_CONTACT_ID = "contact_id"

        fun newInstance(contactId: Long): CarteContactFragment =
            CarteContactFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_CONTACT_ID, contactId)
                }
            }
    }
}

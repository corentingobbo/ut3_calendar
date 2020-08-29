package com.edt.ut3.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.edt.ut3.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_maps.*
import java.util.*
import kotlin.collections.HashSet
import com.edt.ut3.ui.map.SearchPlaceAdapter.Place
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

class MapsFragment : Fragment() {

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        val paulSabatier = LatLng(43.5618994,1.4678633)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(paulSabatier, 15f))

        googleMap.setOnMapClickListener {
            filters_container.visibility = GONE
            search_result.visibility = GONE
            search_bar.clearFocus()
        }

        googleMap.setOnCameraMoveListener {
            filters_container.visibility = GONE
            search_result.visibility = GONE
            search_bar.clearFocus()
        }
    }

    private val selectedCategories = HashSet<String>()
    private val allCategories = HashSet<String>()
    private val places = hashMapOf<String, List<Place>>()

    private var searchJob : Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        setupListeners()

        places["caféteria"] = listOf(
            Place("caféteria", "blbl", LatLng(1.0, 1.0)),
            Place("caféteria", "ohoh", LatLng(1.0, 1.0)),
            Place("caféteria", "ahah", LatLng(1.0, 1.0)),
            Place("caféteria", "uhuh", LatLng(1.0, 1.0))
        )

        places["batiment"] = listOf(
            Place("batiment", "asas", LatLng(1.0, 1.0)),
            Place("batiment", "qzdqzd", LatLng(1.0, 1.0)),
            Place("batiment", "wdvdvx", LatLng(1.0, 1.0)),
            Place("batiment", "qzggfdqz", LatLng(1.0, 1.0)),
            Place("batiment", "asas", LatLng(1.0, 1.0)),
            Place("batiment", "qzdqzd", LatLng(1.0, 1.0)),
            Place("batiment", "wdvdvx", LatLng(1.0, 1.0)),
            Place("batiment", "qzggfdqz", LatLng(1.0, 1.0)),
            Place("batiment", "asas", LatLng(1.0, 1.0)),
            Place("batiment", "qzdqzd", LatLng(1.0, 1.0)),
            Place("batiment", "wdvdvx", LatLng(1.0, 1.0)),
            Place("batiment", "qzggfdqz", LatLng(1.0, 1.0)),
            Place("batiment", "asas", LatLng(1.0, 1.0)),
            Place("batiment", "qzdqzd", LatLng(1.0, 1.0)),
            Place("batiment", "wdvdvx", LatLng(1.0, 1.0)),
            Place("batiment", "qzggfdqz", LatLng(1.0, 1.0)),
            Place("batiment", "asas", LatLng(1.0, 1.0)),
            Place("batiment", "qzdqzd", LatLng(1.0, 1.0)),
            Place("batiment", "wdvdvx", LatLng(1.0, 1.0)),
            Place("batiment", "qzggfdqz", LatLng(1.0, 1.0))
        )

        filters_group.run {
            places.keys.forEach { category ->
                addView(
                    Chip(requireContext()).apply {
                        setChipDrawable(
                            ChipDrawable.createFromAttributes(
                                requireContext(),
                                null,
                                0,
                                R.style.Widget_MaterialComponents_Chip_Filter
                            )
                        )

                        text = category
                        isClickable = true

                        setOnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
                            val cate = text.toString()
                            if (b) {
                                selectedCategories.add(cate)
                                println("Added: $cate")
                            } else {
                                selectedCategories.remove(cate)
                                println("Removed: $cate")
                            }

                            filterResults(search_bar.text.toString())
                        }
                    }
                )
            }
        }
    }

    private fun setupListeners() {
        //text, start, before, count
        search_bar.doOnTextChanged { text, _, _, _ ->
            filterResults(text.toString())
        }

        search_bar.setOnClickListener {
            search_result.visibility = VISIBLE
            filters_container.visibility = VISIBLE
        }
    }

    private fun filterResults(text: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launchWhenResumed {
            val lowerCaseText = text.toLowerCase(Locale.getDefault())
            val result = withContext(Default) {
                places.filterKeys { selectedCategories.isEmpty() || selectedCategories.contains(it) }
                    .flatMap { it.value }
                    .filter { it.value.toLowerCase(Locale.getDefault()).contains(lowerCaseText) }
                    .toTypedArray()
            }

            withContext(Main) {
                search_result.adapter = SearchPlaceAdapter(requireContext(), result)
            }
        }
    }
}
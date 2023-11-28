package com.example.transitapp.ui.Map

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.transitapp.R
import com.example.transitapp.databinding.FragmentMapBinding
import com.example.transitapp.ui.Map.MapViewModel
import com.google.transit.realtime.GtfsRealtime
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import java.net.URL
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager


class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapboxMap: MapboxMap
    private lateinit var pointAnnotationManager: PointAnnotationManager



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.mapView.getMapboxMap().apply {
            loadStyleUri(Style.MAPBOX_STREETS) { style ->

                mapboxMap = this

                // Setup the annotation manager here, after the style has loaded
                pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager(binding.mapView)

                // Fetch and parse the GTFS data
                Thread {
                    val url = URL("https://gtfs.halifax.ca/realtime/Vehicle/VehiclePositions.pb")
                    val feed: GtfsRealtime.FeedMessage = GtfsRealtime.FeedMessage.parseFrom(url.openStream())
                    val annotations = mutableListOf<PointAnnotationOptions>()
                    for (entity in feed.entityList) {
                        if (entity.hasVehicle()) {
                            val vehicle = entity.vehicle
                            val routeId = vehicle.trip.routeId
                            val latitude = vehicle.position.latitude
                            val longitude = vehicle.position.longitude

                            // Create a point annotation for each bus position
                            val pointAnnotation = PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(longitude.toDouble(), latitude.toDouble()))
                                .withTextField(routeId) // This sets the text field

                            annotations.add(pointAnnotation)
                        }
                    }
                    // Update the map with annotations on the main thread
                    activity?.runOnUiThread {
                        pointAnnotationManager.create(annotations)
                    }
                }.start()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
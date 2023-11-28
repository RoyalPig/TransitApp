package com.example.transitapp.ui.Map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.transitapp.R
import com.example.transitapp.databinding.FragmentMapBinding
import com.google.transit.realtime.GtfsRealtime
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import java.net.URL

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

                            // Custom annotation
                            val customAnnotationView = LayoutInflater.from(context).inflate(R.layout.bus_annotation_layout, null)
                            val textView = customAnnotationView.findViewById<TextView>(R.id.busNumberText)
                            textView.text = routeId // Set the bus number

                            // Convert the view to Bitmap
                            customAnnotationView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                            customAnnotationView.layout(0, 0, customAnnotationView.measuredWidth, customAnnotationView.measuredHeight)
                            val bitmap = Bitmap.createBitmap(customAnnotationView.measuredWidth, customAnnotationView.measuredHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            customAnnotationView.draw(canvas)

                            // Create a point annotation for each bus position with custom view
                            val pointAnnotation = PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(longitude.toDouble(), latitude.toDouble()))
                                .withIconImage(bitmap)

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

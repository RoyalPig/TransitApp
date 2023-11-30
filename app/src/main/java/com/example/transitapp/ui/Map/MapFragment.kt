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
import java.net.URL
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding

    private lateinit var mapboxMap: MapboxMap
    private var pointAnnotationManager: PointAnnotationManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root = _binding?.root

        _binding?.mapView?.getMapboxMap()?.apply {
            loadStyleUri(Style.MAPBOX_STREETS) { style ->
                mapboxMap = this

                // Initialize the PointAnnotationManager here
                pointAnnotationManager = binding?.mapView?.annotations?.createPointAnnotationManager()

                // Set the camera position to the received location
                val latitude = arguments?.getDouble("latitude", 0.0) ?: 0.0
                val longitude = arguments?.getDouble("longitude", 0.0) ?: 0.0
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(longitude, latitude))
                        .zoom(15.0)
                        .build()
                )

                // Start fetching and parsing GTFS data
                fetchData()
            }
        }

        return root
    }

    private fun fetchData() {
        Thread {
            val url = URL("https://gtfs.halifax.ca/realtime/Vehicle/VehiclePositions.pb")
            val feed: GtfsRealtime.FeedMessage = GtfsRealtime.FeedMessage.parseFrom(url.openStream())
            val annotations = mutableListOf<PointAnnotationOptions>()
            for (entity in feed.entityList) {
                if (entity.hasVehicle()) {
                    val vehicle = entity.vehicle
                    val routeId = vehicle.trip.routeId
                    val vehicleLatitude = vehicle.position.latitude
                    val vehicleLongitude = vehicle.position.longitude

                    val customAnnotationView = LayoutInflater.from(context).inflate(R.layout.bus_annotation_layout, null)
                    val textView = customAnnotationView.findViewById<TextView>(R.id.busNumberText)
                    textView.text = routeId

                    customAnnotationView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    customAnnotationView.layout(0, 0, customAnnotationView.measuredWidth, customAnnotationView.measuredHeight)
                    val bitmap = Bitmap.createBitmap(customAnnotationView.measuredWidth, customAnnotationView.measuredHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    customAnnotationView.draw(canvas)

                    val pointAnnotation = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(vehicleLongitude.toDouble(), vehicleLatitude.toDouble()))
                        .withIconImage(bitmap)

                    annotations.add(pointAnnotation)
                }
            }



            // Update the map with annotations on the main thread
            activity?.runOnUiThread {
                pointAnnotationManager?.create(annotations)
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pointAnnotationManager = null // Avoid memory leaks
        _binding = null
    }
}

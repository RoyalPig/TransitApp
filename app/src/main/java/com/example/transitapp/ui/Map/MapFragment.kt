package com.example.transitapp.ui.Map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.transitapp.R
import com.example.transitapp.databinding.FragmentMapBinding
import com.google.transit.realtime.GtfsRealtime
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import java.net.URL
import java.io.IOException

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapboxMap: MapboxMap
    private var pointAnnotationManager: PointAnnotationManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 20000L // 20 seconds

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding?.let { binding ->
            binding.mapView.getMapboxMap().apply {
                loadStyleUri(Style.MAPBOX_STREETS) { _ ->
                    mapboxMap = this
                    pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()

                    val latitude = arguments?.getDouble("latitude", 0.0) ?: 0.0
                    val longitude = arguments?.getDouble("longitude", 0.0) ?: 0.0
                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(longitude, latitude))
                            .zoom(15.0)
                            .build()
                    )

                    fetchData()
                }
            }
        }
    }

    private fun fetchData() {
        Thread {
            val preferredRoutes = loadPreferredRoutes()
            val url = URL("https://gtfs.halifax.ca/realtime/Vehicle/VehiclePositions.pb")
            val feed: GtfsRealtime.FeedMessage =
                GtfsRealtime.FeedMessage.parseFrom(url.openStream())
            val annotations = mutableListOf<PointAnnotationOptions>()

            for (entity in feed.entityList) {
                if (entity.hasVehicle()) {
                    val vehicle = entity.vehicle
                    val routeId = vehicle.trip.routeId
                    val vehicleLatitude = vehicle.position.latitude
                    val vehicleLongitude = vehicle.position.longitude

                    val customAnnotationView =
                        LayoutInflater.from(context).inflate(R.layout.bus_annotation_layout, null)
                    val textView = customAnnotationView.findViewById<TextView>(R.id.busNumberText)
                    textView.text = routeId

                    customAnnotationView.measure(
                        View.MeasureSpec.UNSPECIFIED,
                        View.MeasureSpec.UNSPECIFIED
                    )
                    customAnnotationView.layout(
                        0,
                        0,
                        customAnnotationView.measuredWidth,
                        customAnnotationView.measuredHeight
                    )
                    val bitmap = Bitmap.createBitmap(
                        customAnnotationView.measuredWidth,
                        customAnnotationView.measuredHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    customAnnotationView.draw(canvas)

                    val isPreferredRoute = preferredRoutes.contains(routeId)
                    val annotationIcon = if (isPreferredRoute) {
                        createCustomAnnotationIcon(Color.BLUE) // Updated this line
                    } else {
                        bitmap
                    }

                    val pointAnnotation = PointAnnotationOptions()
                        .withPoint(
                            Point.fromLngLat(
                                vehicleLongitude.toDouble(),
                                vehicleLatitude.toDouble()
                            )
                        )
                        .withIconImage(annotationIcon)

                    annotations.add(pointAnnotation)
                }
            }

            activity?.runOnUiThread {
                if (isAdded) {
                    pointAnnotationManager?.deleteAll()
                    pointAnnotationManager?.create(annotations)
                }
            }
        }.start()
    }

    private fun loadPreferredRoutes(): Set<String> {
        val preferredRoutes = mutableSetOf<String>()
        val filename = "preferred_routes.txt"
        try {
            requireContext().openFileInput(filename).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    preferredRoutes.addAll(line.split(","))
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return preferredRoutes
    }

    private fun createCustomAnnotationIcon(color: Int): Bitmap {
        // Example dimensions, you might need to adjust the size
        val width = 50
        val height = 50

        // Create a new bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw the custom annotation, for example, a circle with the specified color
        val paint = Paint().apply {
            this.color = color // Set the color to the specified color
            style = Paint.Style.FILL // Fill the circle
            isAntiAlias = true
        }

        // Draw the circle onto the canvas
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, paint)

        // Optionally, you can add text or other styling to the icon here

        return bitmap
    }

    override fun onResume() {
        super.onResume()
        startRepeatingTask()
    }

    override fun onPause() {
        super.onPause()
        stopRepeatingTask()
    }

    private fun startRepeatingTask() {
        fetchData()
        handler.postDelayed(runnableCode, refreshInterval)
    }

    private fun stopRepeatingTask() {
        handler.removeCallbacks(runnableCode)
    }

    private val runnableCode = object : Runnable {
        override fun run() {
            fetchData()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pointAnnotationManager = null
        _binding = null
    }
}

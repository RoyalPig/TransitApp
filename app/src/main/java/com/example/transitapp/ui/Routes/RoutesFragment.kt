package com.example.transitapp.ui.Routes

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.transitapp.R
import com.example.transitapp.databinding.FragmentRoutesBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException

class RoutesFragment : Fragment() {

    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private val busRoutes = mutableListOf<String>()
    private val preferredRoutes = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRoutesBinding.inflate(inflater, container, false)

        autoCompleteTextView = binding.autoCompleteRoutes
        loadBusRoutes(requireContext())
        loadPreferredRoutes()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, busRoutes)
        autoCompleteTextView.setAdapter(adapter)

        binding.addButton.setOnClickListener {
            val route = autoCompleteTextView.text.toString()
            if (route.isNotEmpty() && busRoutes.contains(route)) {
                addPreferredRoute(route)
                updatePreferredRoutesDisplay() // Update the UI here
            } else {
                // Handle invalid input
            }
        }

        return binding.root
    }

    private fun showDeleteConfirmationDialog(route: String) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("Do you want to remove the route $route?")
            .setCancelable(false)
            .setPositiveButton("Remove") { dialog, id ->
                deletePreferredRoute(route)
                updatePreferredRoutesDisplay()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }

        val alert = dialogBuilder.create()
        alert.setTitle("Remove Route")
        alert.show()
    }

    private fun deletePreferredRoute(route: String) {
        preferredRoutes.remove(route)
        savePreferredRoutes()
    }


    private fun loadBusRoutes(context: Context) {
        val inputStream = context.resources.openRawResource(R.raw.routes)
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.useLines { lines ->
            lines.forEach { line ->
                val routeId = line.split(",")[0].trim()
                if (routeId != "route_id") { // Skip the header
                    busRoutes.add(routeId)
                }
            }
        }
    }

    private fun addPreferredRoute(route: String) {
        preferredRoutes.add(route)
        savePreferredRoutes()
    }

    private fun savePreferredRoutes() {
        val filename = "preferred_routes.txt"
        try {
            context?.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                output?.write(preferredRoutes.joinToString(",").toByteArray())
                Log.d("RoutesFragment", "Saved preferred routes: ${preferredRoutes.joinToString(",")}")
            }
        } catch (e: IOException) {
            Log.e("RoutesFragment", "Error saving preferred routes", e)
        }
    }


    private fun loadPreferredRoutes() {
        val filename = "preferred_routes.txt"

        try {
            context?.openFileInput(filename)?.bufferedReader().use { reader ->
                reader?.let {
                    val storedRoutes = it.readLine()
                    if (!storedRoutes.isNullOrEmpty()) {
                        preferredRoutes.addAll(storedRoutes.split(","))
                        Log.d("RoutesFragment", "Loaded preferred routes: $storedRoutes")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("RoutesFragment", "Error loading preferred routes", e)
        }

        updatePreferredRoutesDisplay() // Ensure UI is updated after loading routes
    }

    private fun updatePreferredRoutesDisplay() {
        binding.linearLayoutPreferredRoutes.removeAllViews()
        for (route in preferredRoutes) {
            val textView = TextView(context).apply {
                text = route
                textSize = 30f
                setPadding(20, 20, 20, 20)
            }
            textView.setOnClickListener {
                showDeleteConfirmationDialog(route)
            }
            binding.linearLayoutPreferredRoutes.addView(textView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

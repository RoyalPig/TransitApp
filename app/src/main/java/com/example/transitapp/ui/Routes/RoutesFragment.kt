package com.example.transitapp.ui.Routes

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import com.example.transitapp.R
import com.example.transitapp.databinding.FragmentRoutesBinding
import java.io.BufferedReader
import java.io.InputStreamReader

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

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, busRoutes)
        autoCompleteTextView.setAdapter(adapter)

        binding.addButton.setOnClickListener {
            val route = autoCompleteTextView.text.toString()
            if (route.isNotEmpty() && busRoutes.contains(route)) {
                addPreferredRoute(route)
                // Update your UI to reflect the new list of preferred routes
            } else {
                // Handle the case where the route is not in the list or empty
            }
        }

        // TODO: Implement functionality to display preferred routes and update UI

        return binding.root
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
        // Save the preferred routes using SharedPreferences or another method
        savePreferredRoutes()
    }

    private fun savePreferredRoutes() {
        // TODO: Implement the method to save preferred routes in SharedPreferences or a database
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

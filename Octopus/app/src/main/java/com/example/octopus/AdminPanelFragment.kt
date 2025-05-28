package com.example.octopus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class AdminPanelFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_panel, container, false)

        val calendarButton = view.findViewById<Button>(R.id.calendar_button)
        calendarButton.setOnClickListener {
            findNavController().navigate(R.id.action_adminPanel_to_calendarFragment)
        }
        val statsButton = view.findViewById<Button>(R.id.stats_button)
        statsButton.setOnClickListener {
            findNavController().navigate(R.id.action_adminPanel_to_statisticsFragment)
        }
        val manageRolesButton = view.findViewById<Button>(R.id.manage_roles_button)
        manageRolesButton.setOnClickListener {
            findNavController().navigate(R.id.action_adminPanel_to_manageRolesFragment)
        }
        return view
    }
}
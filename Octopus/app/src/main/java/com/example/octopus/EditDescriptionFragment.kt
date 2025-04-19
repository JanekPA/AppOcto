package com.example.octopus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class EditDescriptionFragment : Fragment() {
    val day = arguments?.getString("day")
    val time = arguments?.getString("time")
    companion object {
        fun newInstance(day: String, time: String): EditDescriptionFragment {
            val fragment = EditDescriptionFragment()
            val args = Bundle()
            args.putString("day", day)
            args.putString("time", time)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_description, container, false)
    }
}

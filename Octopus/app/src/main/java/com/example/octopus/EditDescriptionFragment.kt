package com.example.octopus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditDescriptionFragment : Fragment() {

    private lateinit var editDescription: EditText
    private lateinit var editTrainer: EditText
    private lateinit var editDuration: EditText
    private lateinit var editLevel: EditText
    private lateinit var saveButton: Button
    private var currentKey: String? = null
    private lateinit var database: FirebaseDatabase

    companion object {
        fun newInstance(classType: String, groupLevel: String): EditDescriptionFragment {
            val fragment = EditDescriptionFragment()
            val args = Bundle()
            args.putString("classType", classType)
            args.putString("groupLevel", groupLevel)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = EditDescriptionFragmentArgs.fromBundle(requireArguments())
        val classType = args.classType
        val groupLevel = args.groupLevel
        currentKey = "${classType}_${groupLevel}".replace(" ", "_")
        database = FirebaseDatabase.getInstance()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        saveButton = view.findViewById(R.id.save_button)
        editDescription = view.findViewById(R.id.edit_description)
        editTrainer = view.findViewById(R.id.edit_trainer)
        editDuration = view.findViewById(R.id.edit_duration)
        editLevel = view.findViewById(R.id.edit_level)

        val imageView: ImageView = view.findViewById(R.id.group_image)

        val args = EditDescriptionFragmentArgs.fromBundle(requireArguments())
        val classType = args.classType
        val groupLevel = args.groupLevel
        currentKey = "${classType}_${groupLevel}".replace(" ", "_")

        val normalizedKey = currentKey!!.lowercase()

        if (normalizedKey == "bjj_łączona") {
            imageView.setImageResource(R.drawable.sample_photo)
        } else {
            imageView.setImageDrawable(null)
            imageView.setOnClickListener {
                Toast.makeText(requireContext(), "Zdjęcie grupy", Toast.LENGTH_SHORT).show()
            }
        }

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid

        val userRef = database.getReference("UsersPersonalization/$uid")
        userRef.child("role").get().addOnSuccessListener { snapshot ->
            val isAdmin = snapshot.getValue(String::class.java) == "admin"

            // Ustaw edytowalność pól
            editDescription.isEnabled = isAdmin
            editTrainer.isEnabled = isAdmin
            editDuration.isEnabled = isAdmin
            editLevel.isEnabled = isAdmin
            saveButton.isEnabled = isAdmin
            saveButton.isVisible = isAdmin
            loadDescriptionData(isAdmin)
            saveButton.setOnClickListener {
                val newDesc = editDescription.text.toString()
                val newCoach = editTrainer.text.toString()
                val newDuration = editDuration.text.toString()
                val newLevel = editLevel.text.toString()

                val updates = mapOf(
                    "description" to newDesc,
                    "coach" to newCoach,
                    "duration" to newDuration,
                    "level" to newLevel
                )

                currentKey?.let { key ->
                    database.getReference("classDescriptions/$key")
                        .setValue(updates)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(requireContext(), "Zapisano", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show()
                            }
                        }
                } ?: run {
                    Toast.makeText(requireContext(), "Brak poprawnego klucza zajęć", Toast.LENGTH_SHORT).show()
                }
            }


        }
    }

    private fun loadDescriptionData(isAdmin: Boolean) {
        val key = currentKey ?: return
        val descRef = database.getReference("classDescriptions").child(key)

        descRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                editDescription.setText(snapshot.child("description").getValue(String::class.java) ?: "")
                editTrainer.setText(snapshot.child("coach").getValue(String::class.java) ?: "")
                editDuration.setText(snapshot.child("duration").getValue(String::class.java) ?: "")
                editLevel.setText(snapshot.child("level").getValue(String::class.java) ?: "")

                if (isAdmin) saveButton.isEnabled = true
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

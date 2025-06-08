package com.example.octopus

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import java.io.File

class EditDescriptionFragment : Fragment() {

    private lateinit var editDescription: EditText
    private lateinit var editTrainer: EditText
    private lateinit var editDuration: EditText
    private lateinit var editLevel: EditText
    private lateinit var saveButton: Button
    private var currentKey: String? = null
    private lateinit var database: FirebaseDatabase
    private var selectedImageUri: Uri? = null
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

        val user = FirebaseAuth.getInstance().currentUser
        if(user != null) {
            val uid = user.uid
            val userRef = database.getReference("UsersPersonalization/$uid")
            userRef.child("role").get().addOnSuccessListener { snapshot ->
                val isAdmin = snapshot.getValue(String::class.java) == "admin"

                // Ustaw edytowalność pól
                editDescription.isEnabled = isAdmin
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
                        database.getReference("classDescriptions/$key").setValue(updates)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    Toast.makeText(requireContext(), "Zapisano", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Błąd zapisu",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } ?: run {
                        Toast.makeText(
                            requireContext(),
                            "Brak poprawnego klucza zajęć",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                editTrainer.isFocusable = false
                editTrainer.isClickable = true // kliknięcie działa, ale nie można edytować tekstu
                editTrainer.setOnClickListener {
                    showCoachDialog()
                }

            }
        }
        else{
            // Ustaw edytowalność pól
            editDescription.isEnabled = false
            editDuration.isEnabled = false
            editLevel.isEnabled = false
            saveButton.isEnabled = false
            saveButton.isVisible = false
            loadDescriptionData(false)
            editTrainer.isFocusable = false
            editTrainer.isClickable = true // kliknięcie działa, ale nie można edytować tekstu
            editTrainer.setOnClickListener {
                showCoachDialog()
            }
        }
    }

    private fun loadDescriptionData(isAdmin: Boolean) {
        val key = currentKey ?: return
        val descRef = database.getReference("classDescriptions").child(key)

        descRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                editDescription.setText(snapshot.child("description").getValue(String::class.java) ?: "")
                val coachName = snapshot.child("coach").child("name").getValue(String::class.java) ?: ""
                val coachSurname = snapshot.child("coach").child("surname").getValue(String::class.java) ?: ""
                editTrainer.setText("$coachName $coachSurname")
                editDuration.setText(snapshot.child("duration").getValue(String::class.java) ?: "")
                editLevel.setText(snapshot.child("level").getValue(String::class.java) ?: "")

                if (isAdmin) saveButton.isEnabled = true
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_image.jpg"))
            val uCropIntent = UCrop.of(it, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(800, 800)
                .getIntent(requireContext())

            uCropLauncher.launch(uCropIntent)
        }
    }

    private val uCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                selectedImageUri = uri

                tempCoachImageView?.let { imageView ->
                    Glide.with(this)
                        .load(uri)
                        .apply(RequestOptions().circleCrop())
                        .into(imageView)
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Błąd kadrowania: ${error?.message}", Toast.LENGTH_SHORT).show()
        }
    }



    // Zmienna pomocnicza (tymczasowy dostęp do ImageView)
    private var tempCoachImageView: ImageView? = null
    private fun showCoachDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_coach, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        val selectImageBtn = dialogView.findViewById<Button>(R.id.btn_select_image)
        val coachImage = dialogView.findViewById<ImageView>(R.id.coach_image)
        val coachName = dialogView.findViewById<EditText>(R.id.coach_name)
        val coachSurname = dialogView.findViewById<EditText>(R.id.coach_surname)
        val coachPhone = dialogView.findViewById<EditText>(R.id.coach_phone)
        val coachEmail = dialogView.findViewById<EditText>(R.id.coach_email)
        val coachFacebook = dialogView.findViewById<EditText>(R.id.coach_facebook)
        val coachInstagram = dialogView.findViewById<EditText>(R.id.coach_instagram)
        val saveButton = dialogView.findViewById<Button>(R.id.btn_save_coach)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val userRef = database.getReference("UsersPersonalization/$uid")

            userRef.child("role").get().addOnSuccessListener { snapshot ->
                val isAdmin = snapshot.getValue(String::class.java) == "admin"
                coachName.isEnabled = isAdmin
                coachSurname.isEnabled = isAdmin
                coachPhone.isEnabled = isAdmin
                coachEmail.isEnabled = isAdmin
                coachFacebook.isEnabled = isAdmin
                coachInstagram.isEnabled = isAdmin
                saveButton.isVisible = isAdmin
                selectImageBtn.isVisible = isAdmin
                val coachRef = database.getReference("classDescriptions/$currentKey/coach")
                coachRef.get().addOnSuccessListener { coachSnapshot ->
                    val name = coachSnapshot.child("name").getValue(String::class.java) ?: ""
                    val surname = coachSnapshot.child("surname").getValue(String::class.java) ?: ""
                    coachName.setText(name)
                    coachSurname.setText(surname)
                    coachPhone.setText(
                        coachSnapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                    )
                    coachEmail.setText(
                        coachSnapshot.child("email").getValue(String::class.java) ?: ""
                    )
                    coachFacebook.setText(
                        coachSnapshot.child("facebook").getValue(String::class.java) ?: ""
                    )
                    coachInstagram.setText(
                        coachSnapshot.child("instagram").getValue(String::class.java) ?: ""
                    )

                    // Ładowanie zdjęcia z Firebase Storage
                    val imageRefPath = "trainers_images/${name}_${surname}".replace(" ", "_")
                    val storageRef = FirebaseStorage.getInstance()
                        .reference.child(imageRefPath)

                    storageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            Glide.with(this)
                                .load(uri)
                                .apply(RequestOptions.circleCropTransform())
                                .into(coachImage)

                        }
                        .addOnFailureListener {
                            coachImage.setImageResource(R.drawable.ic_person)
                        }
                    tempCoachImageView = coachImage

                    selectImageBtn.setOnClickListener {
                        imagePickerLauncher.launch("image/*")
                    }
                    // Zapisz dane jeśli admin
                    saveButton.setOnClickListener {
                        val updatedData = mapOf(
                            "name" to coachName.text.toString(),
                            "surname" to coachSurname.text.toString(),
                            "phoneNumber" to coachPhone.text.toString(),
                            "email" to coachEmail.text.toString(),
                            "facebook" to coachFacebook.text.toString(),
                            "instagram" to coachInstagram.text.toString()
                        )
                        coachRef.setValue(updatedData).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Zapisano dane trenera",
                                    Toast.LENGTH_SHORT
                                ).show()
                                editTrainer.setText("${coachName.text} ${coachSurname.text}")
                                dialog.dismiss()
                            } else {
                                Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        selectedImageUri?.let { uri ->
                            val fullName =
                                "${coachName.text}_${coachSurname.text}".replace(" ", "_")
                            val storageRef =
                                FirebaseStorage.getInstance().reference.child("trainers_images/$fullName")
                            Glide.with(this)
                                .load(uri)
                                .apply(RequestOptions().circleCrop().fitCenter())
                                .into(coachImage)

                            storageRef.putFile(uri)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Zdjęcie przesłane",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Błąd przesyłania zdjęcia",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                    }

                    dialog.show()
                }
            }
        }
        else{
            coachName.isEnabled = false
            coachSurname.isEnabled = false
            coachPhone.isEnabled = false
            coachEmail.isEnabled = false
            coachFacebook.isEnabled = false
            coachInstagram.isEnabled = false
            saveButton.isVisible = false
            selectImageBtn.isVisible = false
            val coachRef = database.getReference("classDescriptions/$currentKey/coach")
            coachRef.get().addOnSuccessListener { coachSnapshot ->
                val name = coachSnapshot.child("name").getValue(String::class.java) ?: ""
                val surname = coachSnapshot.child("surname").getValue(String::class.java) ?: ""
                coachName.setText(name)
                coachSurname.setText(surname)
                coachPhone.setText(
                    coachSnapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                )
                coachEmail.setText(
                    coachSnapshot.child("email").getValue(String::class.java) ?: ""
                )
                coachFacebook.setText(
                    coachSnapshot.child("facebook").getValue(String::class.java) ?: ""
                )
                coachInstagram.setText(
                    coachSnapshot.child("instagram").getValue(String::class.java) ?: ""
                )

                // Ładowanie zdjęcia z Firebase Storage
                val imageRefPath = "trainers_images/${name}_${surname}".replace(" ", "_")
                val storageRef = FirebaseStorage.getInstance()
                    .reference.child(imageRefPath)

                storageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        Glide.with(this)
                            .load(uri)
                            .apply(RequestOptions.circleCropTransform())
                            .into(coachImage)

                    }
                    .addOnFailureListener {
                        coachImage.setImageResource(R.drawable.ic_person)
                    }
                tempCoachImageView = coachImage

                selectImageBtn.setOnClickListener {
                    imagePickerLauncher.launch("image/*")
                }
                dialog.show()
            }
        }
    }
}

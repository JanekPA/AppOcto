package com.example.octopus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.octopus.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        firebaseAuth = FirebaseAuth.getInstance()

        binding.redirectToLog.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.registerclick.setOnClickListener {
            val name = binding.textLoginName.text.toString()
            val email = binding.textEmail.text.toString()
            val pass = binding.textRegisterpassword.text.toString()
            val username = binding.textUsername.text.toString()
            val role = "user"
            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "Nieprawidłowy format e-maila", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isNotEmpty() && pass.isNotEmpty() && username.isNotEmpty() && name.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = firebaseAuth.currentUser?.uid
                            uid?.let {
                                firebaseRef = FirebaseDatabase.getInstance()
                                    .getReference("UsersPersonalization")
                                    .child(uid)

                                val userMap = mapOf(
                                    "name" to name,
                                    "username" to username,
                                    "email" to email,
                                    "role" to role
                                )

                                firebaseRef.setValue(userMap)
                                    .addOnSuccessListener {
                                        (activity as? MainActivity)?.updateNavigationUI()
                                        findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(requireContext(), "Błąd zapisu danych użytkownika", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            val exception = task.exception
                            if (exception is FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(requireContext(), "Zbyt słabe hasło", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), exception?.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            } else {
                Toast.makeText(requireContext(), "Wszystkie pola muszą być wypełnione", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})")
        return email.matches(emailRegex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

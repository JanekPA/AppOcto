package com.example.octopus

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.octopus.databinding.ActivityLoginNewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginFragment : Fragment() {

    private var _binding: ActivityLoginNewBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityLoginNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        firebaseAuth = FirebaseAuth.getInstance()

        val preferences = requireActivity().getSharedPreferences("checkbox", Context.MODE_PRIVATE)
        val checkBox = preferences.getString("remember", "")
        val currentUser = firebaseAuth.currentUser
        if (checkBox == "true" && currentUser != null) {
            (activity as? MainActivity)?.updateNavigationUI()
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
        }

        binding.rememberMe.setOnCheckedChangeListener { _, isChecked ->
            val editor = preferences.edit()
            editor.putString("remember", if (isChecked) "true" else "false")
            editor.apply()
        }

        binding.redirectToReg.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.loginclick.setOnClickListener {
            val email = binding.textLogin.text.toString()
            val pass = binding.textLoginpassword.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                if (isInternetAvailable()) {
                    firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            (activity as? MainActivity)?.updateNavigationUI()
                            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                        } else {
                            val message = when (val exception = task.exception) {
                                is FirebaseAuthInvalidUserException -> "Invalid email format"
                                else -> exception?.message ?: "Unknown error"
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "No internet connection!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Empty fields are not allowed!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.devlog.setOnClickListener {
            val devEmail = "emailf@gmail.com"
            val devPassword = "password"
            if (isInternetAvailable()) {
                firebaseAuth.signInWithEmailAndPassword(devEmail, devPassword).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        (activity as? MainActivity)?.updateNavigationUI()
                        Toast.makeText(requireContext(), "DevLogin", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                    } else {
                        Toast.makeText(requireContext(), "Failed to log in with developer credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "No internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

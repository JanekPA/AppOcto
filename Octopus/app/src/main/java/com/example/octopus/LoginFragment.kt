package com.example.octopus

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.octopus.databinding.ActivityLoginNewBinding
import com.google.firebase.auth.*

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
            preferences.edit().putString("remember", if (isChecked) "true" else "false").apply()
        }

        binding.redirectToReg.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.loginclick.setOnClickListener {
            val email = binding.textLogin.text.toString().trim()
            val pass = binding.textLoginpassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                showToast("Wszystkie pola muszą być wypełnione.")
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Nieprawidłowy format adresu e-mail.")
                return@setOnClickListener
            }

            if (pass.length < 6) {
                showToast("Hasło musi mieć co najmniej 6 znaków.")
                return@setOnClickListener
            }

            if (!isInternetAvailable()) {
                showToast("Brak połączenia z internetem.")
                return@setOnClickListener
            }

            binding.loginclick.isEnabled = false

            firebaseAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    binding.loginclick.isEnabled = true
                    if (task.isSuccessful) {
                        (activity as? MainActivity)?.updateNavigationUI()
                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                    } else {
                        handleLoginError(task.exception)
                    }
                }
        }

        binding.devlog.setOnClickListener {
            val devEmail = "forxon56@gmail.com"
            val devPassword = "Paparapa31"
            if (isInternetAvailable()) {
                firebaseAuth.signInWithEmailAndPassword(devEmail, devPassword).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        (activity as? MainActivity)?.updateNavigationUI()
                        showToast("Zalogowano jako deweloper.")
                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                    } else {
                        showToast("Błąd logowania dewelopera.")
                    }
                }
            } else {
                showToast("Brak połączenia z internetem.")
            }
        }
    }

    private fun handleLoginError(exception: Exception?) {
        val message = when (exception) {
            is FirebaseAuthInvalidUserException -> "Nie znaleziono konta z tym adresem e-mail."
            is FirebaseAuthInvalidCredentialsException -> "Nieprawidłowe dane logowania."
            is FirebaseAuthUserCollisionException -> "Użytkownik już istnieje."
            is FirebaseAuthException -> "Błąd uwierzytelniania: ${exception.localizedMessage}"
            else -> "Nieznany błąd: ${exception?.localizedMessage}"
        }
        showToast(message)
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
            actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

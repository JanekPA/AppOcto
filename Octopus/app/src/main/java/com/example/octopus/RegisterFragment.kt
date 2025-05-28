package com.example.octopus

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Patterns
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.octopus.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

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
            val name = binding.textLoginName.text.toString().trim()
            val email = binding.textEmail.text.toString().trim()
            val pass = binding.textRegisterpassword.text.toString().trim()
            val username = binding.textUsername.text.toString().trim()
            val emailConfirm = binding.textEmailConfirm.text.toString().trim()
            val role = "user"
            val currentTime = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())

            // WALIDACJE POLE PO POLU
            val emptyFields = listOf(name, username, email, emailConfirm, pass).count { it.isEmpty() }

            if (emptyFields > 1) {
                showToast("Uzupełnij dane!")
                return@setOnClickListener
            }
            when {
                name.isEmpty() -> {
                    showToast("Wpisz swoje imię!")
                    return@setOnClickListener
                }
                !name.matches(Regex("^[A-Za-zżźćńółęąśŻŹĆĄŚĘŁÓŃ\\s]{2,40}$")) -> {
                    showToast("Imię może zawierać tylko litery.")
                    return@setOnClickListener
                }
                username.isEmpty() -> {
                    showToast("Wpisz nazwę użytkownika!")
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    showToast("Wpisz adres e-mail!")
                    return@setOnClickListener
                }
                emailConfirm.isEmpty() -> {
                    showToast("Potwierdź adres e-mail!")
                    return@setOnClickListener
                }
                email != emailConfirm -> {
                    showToast("Adresy e-mail muszą być takie same!")
                    return@setOnClickListener
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showToast("Nieprawidłowy format e-maila.")
                    return@setOnClickListener
                }
                pass.isEmpty() -> {
                    showToast("Wpisz hasło!")
                    return@setOnClickListener
                }
                pass.length < 6 -> {
                    showToast("Hasło musi mieć co najmniej 6 znaków.")
                    return@setOnClickListener
                }
                !isInternetAvailable() -> {
                    showToast("Brak połączenia z internetem.")
                    return@setOnClickListener
                }
            }

            binding.registerclick.isEnabled = false

            firebaseAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    binding.registerclick.isEnabled = true
                    if (task.isSuccessful) {
                        val uid = firebaseAuth.currentUser?.uid
                        uid?.let {
                            firebaseRef = FirebaseDatabase.getInstance()
                                .getReference("UsersPersonalization")
                                .child(it)

                            val userMap = mapOf(
                                "name" to name,
                                "username" to username,
                                "email" to email,
                                "role" to role,
                                "registerTime" to currentTime
                            )

                            firebaseRef.setValue(userMap)
                                .addOnSuccessListener {
                                    (activity as? MainActivity)?.updateNavigationUI()
                                    findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                                }
                                .addOnFailureListener {
                                    showToast("Błąd zapisu danych użytkownika.")
                                }
                        }
                    } else {
                        val message = when (val exception = task.exception) {
                            is FirebaseAuthInvalidCredentialsException -> "Zbyt słabe hasło."
                            is FirebaseAuthUserCollisionException -> "Ten e-mail jest już zajęty."
                            else -> "Błąd: ${exception?.localizedMessage}"
                        }
                        showToast(message)
                    }
                }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.octopus

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class HelpFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FaqAdapter
    private val faqList = mutableListOf<FaqItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_help, container, false)

        recyclerView = view.findViewById(R.id.faqRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val addButton = view.findViewById<Button>(R.id.buttonAddQuestion)
        addButton.setOnClickListener {
            showAddFaqDialog()
        }

        val phoneText: TextView = view.findViewById(R.id.phoneContact)
        val facebookText: TextView = view.findViewById(R.id.fbContact)
        val instagramText: TextView = view.findViewById(R.id.instagramContact)
        val mailText: TextView = view.findViewById(R.id.mailContact)

        database = FirebaseDatabase.getInstance().getReference("Contact")
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val phone = snapshot.child("phone").getValue(String::class.java)
                val facebook = snapshot.child("facebook").getValue(String::class.java)
                val instagram = snapshot.child("instagram").getValue(String::class.java)
                mailText.text = "Email: " + snapshot.child("email").getValue(String::class.java)

                phone?.let {
                    phoneText.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse("tel:$phone")
                        startActivity(intent)
                    }
                }

                facebook?.let {
                    facebookText.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(facebook))
                        startActivity(intent)
                    }
                }

                instagram?.let {
                    instagramText.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(instagram))
                        startActivity(intent)
                    }
                }
                loadFaq() // <--- DODAJ TO TUTAJ
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        adapter = FaqAdapter(faqList)
        recyclerView.adapter = adapter

        return view
    }
    private fun loadFaq() {
        val ref = FirebaseDatabase.getInstance().getReference("FAQ/Questions")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                faqList.clear()
                for (item in snapshot.children) {
                    val question = item.child("question").getValue(String::class.java) ?: ""
                    val answer = item.child("answer").getValue(String::class.java) ?: ""
                    faqList.add(FaqItem(question, answer))
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun showAddFaqDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_faq, null)
        val questionEditText = dialogView.findViewById<EditText>(R.id.editTextQuestion)
        val answerEditText = dialogView.findViewById<EditText>(R.id.editTextAnswer)

        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj nowe pytanie")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val question = questionEditText.text.toString().trim()
                val answer = answerEditText.text.toString().trim()

                if (question.isNotEmpty() && answer.isNotEmpty()) {
                    val ref = FirebaseDatabase.getInstance().getReference("FAQ/Questions")
                    val newId = ref.push().key ?: UUID.randomUUID().toString()
                    val data = mapOf("question" to question, "answer" to answer)
                    ref.child(newId).setValue(data).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(requireContext(), "Dodano pytanie!", Toast.LENGTH_SHORT).show()
                            loadFaq()
                        } else {
                            Toast.makeText(requireContext(), "Błąd dodawania", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Wprowadź pytanie i odpowiedź", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

}

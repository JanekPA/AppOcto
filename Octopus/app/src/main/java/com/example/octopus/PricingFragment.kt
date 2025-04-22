package com.example.octopus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class PricingFragment : Fragment() {

    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pricing, container, false)

        val phoneText: TextView = view.findViewById(R.id.contactPhone)
        val facebookText: TextView = view.findViewById(R.id.contactFacebook)
        val instagramText: TextView = view.findViewById(R.id.contactInstagram)
        val mailText: TextView = view.findViewById(R.id.contactMail)

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
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        return view
    }
}


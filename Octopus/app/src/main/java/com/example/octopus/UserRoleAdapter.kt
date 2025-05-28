package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class UserRoleAdapter(private val users: List<UserInfo>) :
    RecyclerView.Adapter<UserRoleAdapter.ViewHolder>() {

    private val roles = listOf("user", "trainer", "admin")

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emailText: TextView = view.findViewById(R.id.email_text)
        val usernameText: TextView = view.findViewById(R.id.username_text)
        val roleSpinner: Spinner = view.findViewById(R.id.role_spinner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_role, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.emailText.text = user.email
        holder.usernameText.text = user.username

        val adapter = ArrayAdapter(holder.itemView.context, R.layout.spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.roleSpinner.adapter = adapter
        holder.roleSpinner.setSelection(roles.indexOf(user.role))

        holder.roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selectedRole = roles[pos]
                if (selectedRole != user.role) {
                    FirebaseDatabase.getInstance().getReference("UsersPersonalization")
                        .child(user.uid)
                        .child("role")
                        .setValue(selectedRole)
                    user.role = selectedRole
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun getItemCount(): Int = users.size
}

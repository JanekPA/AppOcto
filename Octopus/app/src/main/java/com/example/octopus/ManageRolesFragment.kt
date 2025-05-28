package com.example.octopus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ManageRolesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserRoleAdapter
    private val userList = mutableListOf<UserInfo>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_roles, container, false)

        recyclerView = view.findViewById(R.id.users_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserRoleAdapter(userList)
        recyclerView.adapter = adapter

        fetchUsersFromDatabase()

        return view
    }

    private fun fetchUsersFromDatabase() {
        val dbRef = FirebaseDatabase.getInstance().getReference("UsersPersonalization")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnap in snapshot.children) {
                    val userId = userSnap.key ?: continue
                    val email = userSnap.child("email").value as? String ?: ""
                    val username = userSnap.child("username").value as? String ?: ""
                    val role = userSnap.child("role").value as? String ?: "user"

                    userList.add(UserInfo(userId, email, username, role))
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

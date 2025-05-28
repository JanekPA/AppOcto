package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UpcomingSessionsAdapter(
    private val sessions: List<MainFragment.DisplaySession>
) : RecyclerView.Adapter<UpcomingSessionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.session_title)
        val time: TextView = view.findViewById(R.id.session_time)
        val day: TextView = view.findViewById(R.id.session_day)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]

        val group = session.groupLevel?.let { "gr. $it" } ?: ""
        holder.day.text = session.day
        holder.title.text = session.classType + ", " + group
        holder.time.text = session.time
    }

    override fun getItemCount(): Int = sessions.size
}

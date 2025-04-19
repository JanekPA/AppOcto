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
        val title: TextView = view.findViewById(R.id.item_title)
        val time: TextView = view.findViewById(R.id.item_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.schedule_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]

        val group = session.groupLevel?.let { "gr. $it" } ?: ""
        val fullTitle = "${session.day}, ${session.time} – ${session.classType} $group, ${session.room}"

        holder.title.text = fullTitle.trim()
        holder.time.text = "" // Jeśli chcesz wyświetlać czas osobno, wpisz tu session.time
    }

    override fun getItemCount(): Int = sessions.size
}

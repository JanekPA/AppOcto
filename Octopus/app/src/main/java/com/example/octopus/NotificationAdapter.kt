package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val itemKeys: MutableList<String>,
    private val onDelete: (key: String, position: Int) -> Unit,
    private val onClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notification_title)
        val message: TextView = view.findViewById(R.id.notification_message)
        val timestamp: TextView = view.findViewById(R.id.notification_time)
        init {
            itemView.setOnClickListener {
                onClick(notifications[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.title.text = notifications[position].title
        holder.message.text = notifications[position].message

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        holder.timestamp.text = sdf.format(Date(notifications[position].timestamp))
    }

    override fun getItemCount(): Int = notifications.size

    fun removeItem(position: Int) {
        (notifications as MutableList).removeAt(position)
        (itemKeys).removeAt(position)
        notifyItemRemoved(position)
    }

    fun getItemKey(position: Int): String = itemKeys[position]
}


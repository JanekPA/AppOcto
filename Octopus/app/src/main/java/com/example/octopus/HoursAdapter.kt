package com.example.octopus

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView



class HoursAdapter(
    private val onHourSelected: (String) -> Unit
) : RecyclerView.Adapter<HoursAdapter.HourViewHolder>() {

    private var hours: List<String> = listOf()
    internal var statuses: List<HourStatus>? = null
    private var selectedPosition = -1

    fun updateData(newHours: List<String>, newStatuses: List<HourStatus>? = null) {
        hours = newHours
        statuses = newStatuses
        notifyDataSetChanged()
    }

    inner class HourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hourTextView: TextView = itemView.findViewById(R.id.textViewHour)
        val cardView: CardView = itemView.findViewById(R.id.cardViewHour)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hour, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        val hour = hours[position]
        holder.hourTextView.text = hour

        val context = holder.itemView.context

        val status = statuses?.firstOrNull { it.time == hour }?.status ?: HourStatus.Status.FREE

        val backgroundColor = when (status) {
            HourStatus.Status.CONFIRMED -> ContextCompat.getColor(context, android.R.color.holo_green_light)
            HourStatus.Status.PENDING -> ContextCompat.getColor(context, android.R.color.holo_orange_light)
            HourStatus.Status.FREE -> ContextCompat.getColor(context, android.R.color.white)
        }

        val finalColor = if (position == selectedPosition) {
            ContextCompat.getColor(context, R.color.teal_200)
        } else {
            backgroundColor
        }

        holder.cardView.setCardBackgroundColor(finalColor)
        holder.hourTextView.setTypeface(null, if (position == selectedPosition) Typeface.BOLD else Typeface.NORMAL)

        holder.cardView.setOnClickListener {
            selectedPosition = holder.adapterPosition
            notifyDataSetChanged()
            onHourSelected(hour)
        }
    }

    override fun getItemCount(): Int = hours.size
}

package com.example.octopus

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class HoursAdapter(
    private val hours: List<String>,
    private val onHourSelected: (String) -> Unit
) : RecyclerView.Adapter<HoursAdapter.HourViewHolder>() {

    private var selectedPosition = -1

    inner class HourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hourTextView: TextView = itemView.findViewById(R.id.textViewHour)
        val cardView: CardView = itemView.findViewById(R.id.cardViewHour)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hour, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val hour = hours[position]
        holder.hourTextView.text = hour

        holder.cardView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onHourSelected(hour)
        }

        // Zmiana stylu zaznaczenia
        if (position == selectedPosition) {
            holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.teal_200))
            holder.hourTextView.setTypeface(null, Typeface.BOLD)
        } else {
            holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
            holder.hourTextView.setTypeface(null, Typeface.NORMAL)
        }
    }

    override fun getItemCount(): Int = hours.size
}

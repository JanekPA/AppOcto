package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DayPagerAdapter(
    private val days: List<String>,
    private val onDaySelected: (String) -> Unit
) : RecyclerView.Adapter<DayPagerAdapter.DayViewHolder>() {

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.day_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_selector, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.dayText.text = day
        holder.dayText.setOnClickListener {
            onDaySelected(day)
        }
    }

    override fun getItemCount(): Int = days.size
}

package com.example.octopus

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(

    private var scheduleMap: Map<String, ScheduleItem>,
    private val onItemClicked: (String, ScheduleItem) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    // Posortowana lista zajęć według godziny rozpoczęcia
    private val scheduleList get() = scheduleMap.toList().sortedBy {
        val time = it.second.time ?: ""
        val start = time.split("-").firstOrNull()?.trim() ?: "99:99"
        val parts = start.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 99
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 99
        hour * 60 + minute
    }


    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_title)
        val time: TextView = view.findViewById(R.id.item_time)
        val favoriteButton: View = view.findViewById(R.id.favorite_button)
        val favoriteButtonBackground: View = view.findViewById(R.id.favorite_button_background)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.schedule_item, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val (id, item) = scheduleList[position]
        holder.itemView.setBackgroundColor(
            getBackgroundColor(item.classType, item.groupLevel)
        )
        holder.title.text = if (item.groupLevel != null)
            "${item.classType} gr. ${item.groupLevel}"
        else
            item.classType

        holder.time.text = item.time
        holder.itemView.setOnClickListener { onItemClicked(id, item)
            }
        holder.favoriteButton.setOnClickListener {
            holder.favoriteButtonBackground.setBackgroundResource(R.drawable.favorite_background_selected)
            Toast.makeText(
                holder.itemView.context,
                "Zajęcie zostało dodane do ulubionych!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun getItemCount(): Int = scheduleList.size

    fun updateList(newMap: Map<String, ScheduleItem>) {
        scheduleMap = newMap
        notifyDataSetChanged()
    }
    private fun getBackgroundColor(classType: String?, groupLevel: String?): Int {
        return when (classType?.lowercase()) {
            "mma" -> when (groupLevel?.lowercase()) {
                "podstawowa" -> Color.parseColor("#D3D3D3")
                "średnio zaawansowana" -> Color.parseColor("#EAEAEA")
                "pro" -> Color.parseColor("#444444")
                else -> Color.parseColor("#EAEAEA")
            }
            "bjj" -> when (groupLevel?.lowercase()) {
                "początkująca" -> Color.parseColor("#D0E7FF")
                "łączona" -> Color.parseColor("#7DBBFF")
                "sparingi" -> Color.parseColor("#4589BF")
                "pro" -> Color.parseColor("#1E3F66")
                "kids", "family" -> Color.parseColor("#CFFFE5")
                else -> Color.parseColor("#7DBBFF")
            }
            "zapasy" -> Color.parseColor("#FF9999")
            "no-gi" -> Color.parseColor("#5D9CEC")
            "boks" -> when (groupLevel?.lowercase()) {
                "średnio zaawansowany" -> Color.parseColor("#FFF9C4")
                "zaawansowany" -> Color.parseColor("#FFD700")
                "łączona" -> Color.parseColor("#FFEB3B")
                else -> Color.parseColor("#FFEB3B")
            }
            "kick-boxing" -> when (groupLevel?.lowercase()) {
                "junior" -> Color.parseColor("#CCFFCC")
                "łączona" -> Color.parseColor("#66CC66")
                "początkująca" -> Color.parseColor("#99CC99")
                "średnio zaawansowana", "pro" -> Color.parseColor("#2E7D32")
                else -> Color.parseColor("#66CC66")
            }
            "wolna mata" -> Color.parseColor("#E1BEE7")
            else -> Color.LTGRAY
        }
    }


}

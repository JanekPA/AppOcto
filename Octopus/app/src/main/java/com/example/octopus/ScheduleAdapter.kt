package com.example.octopus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.collection.emptyLongSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class ScheduleAdapter(
    private var scheduleMap: Map<String, ScheduleItem>,
    private val onItemClicked: (String, ScheduleItem) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    // Posortowana lista zajęć według godziny rozpoczęcia
    private val scheduleList
        get() = scheduleMap.toList().sortedBy {
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
        val groupLeveltext: TextView = view.findViewById(R.id.item_level_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.schedule_item, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val (id, item) = scheduleList[position]
        val bgColor = getBackgroundColor(item.classType, item.groupLevel)
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                adjustColorBrightness(bgColor, 1.1f),  // jaśniejszy
                adjustColorBrightness(bgColor, 0.9f)   // ciemniejszy
            )
        )
        gradientDrawable.cornerRadius = 32f
        holder.itemView.background = gradientDrawable

        holder.title.text = item.classType
        if(item.groupLevel!=null && item.groupLevel != "sparingi"){
            holder.groupLeveltext.text = "gr. "+item.groupLevel
        }
        else if (item.groupLevel == "sparingi"){
            holder.groupLeveltext.text = item.groupLevel
        }
        else{
            holder.groupLeveltext.text=""
        }
        val user = FirebaseAuth.getInstance().currentUser
        val favKey = "${item.classType}_${item.groupLevel}_${item.time}"
        var isFavorite = false

        if (user != null) {
            val uid = user.uid
            val favRef = FirebaseDatabase.getInstance()
                .getReference("UsersPersonalization")
                .child(uid)
                .child("FavouriteClasses")
                .child(favKey)

            favRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    isFavorite = true
                    holder.favoriteButtonBackground.setBackgroundResource(R.drawable.favorite_background_selected)
                } else {
                    isFavorite = false
                    holder.favoriteButtonBackground.setBackgroundResource(R.drawable.favorite_background_default)
                }
            }.addOnFailureListener {
                // Jeśli nie można pobrać danych – ustaw jako nieulubione
                isFavorite = false
                holder.favoriteButtonBackground.setBackgroundResource(R.drawable.favorite_background_default)
            }
        }

        if (user == null) {
            holder.favoriteButton.visibility = View.GONE
            holder.favoriteButtonBackground.visibility = View.GONE
        } else {
            holder.favoriteButton.visibility = View.VISIBLE
            holder.favoriteButtonBackground.visibility = View.VISIBLE
        }
        holder.time.text = item.time
        holder.itemView.setOnClickListener {
            onItemClicked(id, item)
        }
        holder.favoriteButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val uid = user.uid
                val favRef = FirebaseDatabase.getInstance()
                    .getReference("UsersPersonalization")
                    .child(uid)
                    .child("FavouriteClasses")
                val favKey = "${item.classType}_${item.groupLevel}_${item.time}"

                if (!isFavorite) {
                    val favData = mapOf(
                        "classType" to item.classType,
                        "groupLevel" to item.groupLevel,
                        "day" to id,
                        "hour" to item.time
                    )
                    favRef.child(favKey).setValue(favData)
                    holder.favoriteButtonBackground.setBackgroundResource(R.drawable.favorite_background_selected)
                    Toast.makeText(
                        holder.itemView.context,
                        "Zajęcie zostało dodane do ulubionych!",
                        Toast.LENGTH_SHORT
                    ).show()
                    isFavorite = true
                } else {
                    favRef.child(favKey).removeValue()
                    holder.favoriteButtonBackground.setBackgroundResource(R.drawable.favorite_background_default)
                    Toast.makeText(
                        holder.itemView.context,
                        "Zajęcie zostało usunięte z ulubionych!",
                        Toast.LENGTH_SHORT
                    ).show()
                    isFavorite = false
                }
            }
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
                "początkująca" -> Color.parseColor("#747472")
                "średnio zaawansowana" -> Color.parseColor("#C4C4C4")
                "pro" -> Color.parseColor("#4D4B4A")
                else -> Color.parseColor("#EAEAEA")
            }

            "bjj" -> when (groupLevel?.lowercase()) {
                "początkująca" -> Color.parseColor("#26ADE5")
                "łączona" -> Color.parseColor("#273B6C")
                "sparingi" -> Color.parseColor("#435481")
                "pro" -> Color.parseColor("#232F50")
                "kids", "family" -> Color.parseColor("#BCCD44")
                else -> Color.parseColor("#7DBBFF")
            }

            "zapasy" -> Color.parseColor("#B32C26")
            "no-gi" -> Color.parseColor("#3288BF")
            "boks" -> when (groupLevel?.lowercase()) {
                "średnio zaawansowany" -> Color.parseColor("#F1BA1A")
                "zaawansowany" -> Color.parseColor("#D08C1D")
                "łączona" -> Color.parseColor("#D59824")
                else -> Color.parseColor("#FFEB3B")
            }

            "kick-boxing" -> when (groupLevel?.lowercase()) {
                "junior" -> Color.parseColor("#5CAA80")
                "łączona" -> Color.parseColor("#3CA390")
                "początkująca" -> Color.parseColor("#3EA294")
                "średnio zaawansowana", "pro" -> Color.parseColor("#36907A")
                else -> Color.parseColor("#36907A")
            }

            "wolna mata" -> Color.parseColor("#BBACD5")
            else -> Color.LTGRAY
        }
    }
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = ((Color.red(color) * factor).coerceAtMost(255f)).toInt()
        val g = ((Color.green(color) * factor).coerceAtMost(255f)).toInt()
        val b = ((Color.blue(color) * factor).coerceAtMost(255f)).toInt()
        return Color.rgb(r, g, b)
    }
}

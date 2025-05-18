package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class TrainingEntryAdapter : RecyclerView.Adapter<TrainingEntryAdapter.ViewHolder>() {

    private val trainings = mutableListOf<TrainingEntry>()
    private var currentDay: String = ""

    fun submitList(newList: List<TrainingEntry>, day: String) {
        trainings.clear()
        trainings.addAll(newList.map { it.copy() }) // uniknij używania tej samej referencji!
        currentDay = day
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.time_text)
        val nameText: TextView = view.findViewById(R.id.name_text)
        val trainerEdit: EditText = view.findViewById(R.id.trainer_edit)
        val paidCheckbox: CheckBox = view.findViewById(R.id.paid_checkbox)
        val saveButton: Button = view.findViewById(R.id.save_button)
        val participantsEdit: EditText = view.findViewById(R.id.participants_count_edit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_entry, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = trainings.size
    private var isSummaryMode = false

    fun setSummaryMode(summary: Boolean) {
        isSummaryMode = summary
        notifyDataSetChanged()
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = trainings[position]
        holder.timeText.text = entry.time
        holder.nameText.text = "${entry.classType}_${entry.groupLevel}"
// Ustaw dane zawsze niezależnie od stanu
        holder.trainerEdit.setText(entry.trainer)
        holder.paidCheckbox.isChecked = entry.paid
        holder.participantsEdit.setText(entry.participantsCount.toString())
// Ustaw tryb edycji lub podglądu w zależności od isSaved
        if (entry.isSaved) {
            setFieldsEditable(holder, false)
            holder.saveButton.text = "Edytuj"
        } else {
            setFieldsEditable(holder, true)
            holder.saveButton.text = "Zapisz"
        }


        holder.saveButton.setOnClickListener {
            if (entry.isSaved) {
                // tryb edycji
                entry.isSaved = false
                setFieldsEditable(holder, true)
                holder.saveButton.text = "Zapisz"
            } else {
                // zapisujemy dane
                val trainerName = holder.trainerEdit.text.toString()
                val paid = holder.paidCheckbox.isChecked

                val statRef = FirebaseDatabase.getInstance()
                    .getReference("ScheduleStatistics")
                    .child(currentDay)
                    .child("trainings")
                    .child(entry.id)

                val participantsCount = holder.participantsEdit.text.toString().toIntOrNull() ?: 0

                statRef.setValue(
                    mapOf(
                        "trainer" to trainerName,
                        "paymentReceived" to paid,
                        "participantsCount" to participantsCount,
                        "classType" to entry.classType,
                        "groupLevel" to entry.groupLevel
                    )
                ).addOnSuccessListener {
                    entry.trainer = trainerName
                    entry.paid = paid
                    entry.participantsCount = participantsCount
                    entry.isSaved = true
                    Toast.makeText(holder.itemView.context, "Zapisano", Toast.LENGTH_SHORT).show()
                    setFieldsEditable(holder, false)
                    holder.saveButton.text = "Edytuj"
                }

            }
        }
        if (isSummaryMode) {
            holder.trainerEdit.visibility = View.GONE
            holder.paidCheckbox.visibility = View.GONE
            holder.saveButton.visibility = View.GONE
            holder.participantsEdit.visibility = View.GONE
            // zamiast tego pokażemy tylko informacyjne TextView np.:
            holder.nameText.text = "${entry.classType}_${entry.groupLevel} | Trener: ${entry.trainer} | ${if (entry.paid) "Wypłacone" else "Nie wypłacone"} | Uczestnicy: ${entry.participantsCount}"
        } else {
            holder.trainerEdit.visibility = View.VISIBLE
            holder.paidCheckbox.visibility = View.VISIBLE
            holder.saveButton.visibility = View.VISIBLE
            holder.participantsEdit.visibility = View.VISIBLE
            holder.nameText.text = "${entry.classType}_${entry.groupLevel} | Trener: ${entry.trainer} | ${if (entry.paid) "Wypłacone" else "Nie wypłacone"} | Uczestnicy: ${entry.participantsCount}"

        }
    }
    private fun setFieldsEditable(holder: ViewHolder, editable: Boolean) {
        holder.trainerEdit.isEnabled = editable
        holder.paidCheckbox.isEnabled = editable
    }

}
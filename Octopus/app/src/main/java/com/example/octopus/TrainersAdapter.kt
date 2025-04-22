package com.example.octopus

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TrainersAdapter(
    private val trainers: List<TrainersFragment.Trainer>,
    private val onTrainerSelected: (TrainersFragment.Trainer) -> Unit
) : RecyclerView.Adapter<TrainersAdapter.TrainerViewHolder>() {

    private var selectedPosition = -1

    inner class TrainerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewTrainerName)
        val typesTextView: TextView = itemView.findViewById(R.id.textViewTrainerTypes)
        val cardView: CardView = itemView.findViewById(R.id.cardViewTrainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trainer, parent, false)
        return TrainerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainerViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val trainer = trainers[position]
        holder.nameTextView.text = "${trainer.name} ${trainer.surname}"
        holder.typesTextView.text = "Zajęcia: ${trainer.classTypes.joinToString(", ")}"

        holder.cardView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onTrainerSelected(trainer)
        }

        if (position == selectedPosition) {
            holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.teal_200))
            holder.nameTextView.setTypeface(null, Typeface.BOLD)
        } else {
            holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
            holder.nameTextView.setTypeface(null, Typeface.NORMAL)
        }
    }

    override fun getItemCount(): Int = trainers.size
}

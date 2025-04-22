package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReservationAdapter(private val reservationList: List<Map<String, Any>>) :
    RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.itemNameText)
        val quantityText: TextView = view.findViewById(R.id.quantityText)
        val paymentText: TextView = view.findViewById(R.id.paymentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = reservationList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reservation = reservationList[position]
        holder.nameText.text = "Przedmiot: ${reservation["itemName"]}"
        holder.quantityText.text = "Ilość: ${reservation["quantity"]}"
        holder.paymentText.text = "Płatność: ${reservation["payment"]}"
    }
}

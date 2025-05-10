package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReservationAdapter(
    private val reservationList: List<Map<String, Any>>,
    private val onItemClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.itemNameText)
        val quantityText: TextView = view.findViewById(R.id.quantityText)
        val paymentText: TextView = view.findViewById(R.id.paymentText)
        val pickupDateText: TextView = view.findViewById(R.id.pickupDateText)
        val statusText: TextView = view.findViewById(R.id.statusText)
        val priceTag: TextView = view.findViewById(R.id.priceText)

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
        holder.pickupDateText.text = "Data odbioru: ${reservation["pickupDate"] ?: "-"}"
        holder.statusText.text = "Status: " + (reservation["status"] as? String ?: "Brak danych")
        val price = (reservation["price"] as? Number)?.toDouble() ?: 0.0
        holder.priceTag.text = "Cena: %.2f zł".format(price)
        holder.itemView.setOnClickListener {
            onItemClick(reservation)
        }
    }
}

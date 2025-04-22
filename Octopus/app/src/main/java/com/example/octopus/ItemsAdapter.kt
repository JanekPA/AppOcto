package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemsAdapter(
    private val items: List<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.textViewName)
        private val colorText: TextView = itemView.findViewById(R.id.textViewColor)
        private val typeText: TextView = itemView.findViewById(R.id.textViewType)
        private val sizeText: TextView = itemView.findViewById(R.id.textViewSize)
        private val quantityText: TextView = itemView.findViewById(R.id.textViewQuantity)

        fun bind(item: Item) {
            nameText.text = "${item.name}"
            colorText.text = if (item.color.isNotBlank()) "Kolor: ${item.color}" else "Kolor: -"
            typeText.text = if (item.type.isNotBlank()) "Typ: ${item.type}" else "Typ: -"
            sizeText.text = if (item.size.isNotBlank()) "Rozmiar: ${item.size}" else "Rozmiar: -"
            quantityText.text = "Ilość: ${item.quantity}"

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

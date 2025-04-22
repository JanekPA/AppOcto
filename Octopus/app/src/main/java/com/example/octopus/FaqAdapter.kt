package com.example.octopus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FaqAdapter(private val faqList: List<FaqItem>) :
    RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    inner class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questionText: TextView = itemView.findViewById(R.id.faq_question)
        val answerText: TextView = itemView.findViewById(R.id.faq_answer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faq, parent, false)
        return FaqViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val item = faqList[position]
        holder.questionText.text = item.question
        holder.answerText.text = item.answer
        holder.answerText.visibility = if (item.isExpanded) View.VISIBLE else View.GONE

        holder.questionText.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = faqList.size
}

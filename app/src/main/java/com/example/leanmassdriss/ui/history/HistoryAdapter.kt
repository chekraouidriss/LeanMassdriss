package com.example.leanmassdriss.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.leanmassdriss.databinding.ItemLbmRecordBinding
import com.example.leanmassdriss.domain.model.LbmRecord
import com.example.leanmassdriss.utils.format2
import com.example.leanmassdriss.utils.toFormattedString

class HistoryAdapter(
    private val onDeleteClicked: (LbmRecord) -> Unit
) : ListAdapter<LbmRecord, HistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLbmRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClicked)
    }

    class ViewHolder(private val binding: ItemLbmRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: LbmRecord, onDeleteClicked: (LbmRecord) -> Unit) {
            with(binding) {
                tvItemLbmValue.text = "${record.lbmValue.format2()} kg"
                tvItemStatus.text = record.statut

                // Coloration dynamique du statut dans l'historique
                if (record.statut.contains("✅")) {
                    tvItemStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                } else {
                    tvItemStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                }

                val genreStr = if (record.isHomme) "♂ Homme" else "♀ Femme"
                tvItemDetails.text = "Poids: ${record.poids}kg | Taille: ${record.taille}cm | $genreStr"
                tvItemDate.text = record.dateCalcul.toFormattedString()

                btnDeleteItem.setOnClickListener { onDeleteClicked(record) }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<LbmRecord>() {
        override fun areItemsTheSame(oldItem: LbmRecord, newItem: LbmRecord): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: LbmRecord, newItem: LbmRecord): Boolean {
            return oldItem == newItem
        }
    }
}
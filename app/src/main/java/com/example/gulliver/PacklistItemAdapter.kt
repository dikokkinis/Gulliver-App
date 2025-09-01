package com.example.gulliver

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView


class PacklistItemAdapter(
    private val context: Context,
    private val onItemToggled: (Item) -> Unit,
    private val onItemLongPressed: (Item) -> Unit
) : ListAdapter<Item, PacklistItemAdapter.ItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_packlist, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

        holder.imageViewCheck.setOnClickListener {
            onItemToggled(item)

        }

        holder.itemView.setOnLongClickListener {
            onItemLongPressed(item)
            true
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewItemText: TextView = itemView.findViewById(R.id.textViewItemText)
        val imageViewCheck: ImageView = itemView.findViewById(R.id.imageViewCheck)
        private val itemRootLayout: RelativeLayout = itemView.findViewById(R.id.item_root_layout)

        fun bind(item: Item) {
            textViewItemText.text = item.text
            if (item.isPacked) {
                imageViewCheck.setImageResource(R.drawable.ticked_button)
                textViewItemText.setBackgroundResource(R.drawable.pack_checked)
            } else {
                imageViewCheck.setImageResource(R.drawable.unticked_button)
                textViewItemText.setBackgroundResource(R.drawable.ticket_input)
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {

            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}
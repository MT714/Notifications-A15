package com.embedded2025.notificationsa15.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.embedded2025.notificationsa15.R

class MessageAdapter : ListAdapter<Message, MessageAdapter.MessageViewHolder>(DiffCallback()) {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val roleTextView: TextView = itemView.findViewById(R.id.roleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)

        fun bind(message: Message) {
            roleTextView.text = message.role
            contentTextView.text = message.content
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
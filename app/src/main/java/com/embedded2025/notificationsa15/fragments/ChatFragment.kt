package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.embedded2025.notificationsa15.NotificationsLabApplication
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.chat.ChatViewModel
import com.embedded2025.notificationsa15.chat.ChatViewModelFactory
import com.embedded2025.notificationsa15.chat.MessageAdapter
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import com.embedded2025.notificationsa15.utils.NotificationsHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class ChatFragment: Fragment() {
    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(NotificationsLabApplication.chatRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MessageAdapter()
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { listOfMessages ->
                    adapter.submitList(listOfMessages)
                }
            }
        }

        view.findViewById<Button>(R.id.send_message).setOnClickListener {
            val editText = view.findViewById<EditText>(R.id.message_input)
            val messageText = editText.text.toString()
            if (messageText.isNotBlank()) {
                viewModel.sendUserMessage(messageText)
                editText.text.clear()
            }
        }

        view.findViewById<Button>(R.id.btn_clear_chat).setOnClickListener {
            NotificationsHelper.cancel(DemoNotificationsHelper.NotificationID.CHAT)
            runBlocking { viewModel.repository.clearChat() }
        }
    }
}
package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.embedded2025.notificationsa15.NotificationsLabApplication
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.chat.MessageAdapter
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.NotificationID
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class  ChatFragment: Fragment() {

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
            NotificationsLabApplication.chatRepository.messages.collect { listOfMessages ->
                adapter.submitList(listOfMessages)
            }
        }

        view.findViewById<Button>(R.id.send_message).setOnClickListener {
            val editText = view.findViewById<EditText>(R.id.message_input)
            val messageText = editText.text.toString()
            if (messageText.isNotBlank()) {
                lifecycleScope.launch {
                    NotificationsLabApplication.chatRepository.processUserMessageAndGetResponse(messageText)
                    NotificationHelper.showMessageNotification(NotificationsLabApplication.chatRepository.getLastMessages(4))
                }
                editText.text.clear()
            }
        }

        view.findViewById<Button>(R.id.btn_clear_chat).setOnClickListener {
            NotificationHelper.cancel(NotificationID.CHAT)
            runBlocking { NotificationsLabApplication.chatRepository.clearChat() }
        }

        view.findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            findNavController().navigate(R.id.actionsNotificationFragment)
        }

        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            findNavController().navigate(R.id.progressNotificationFragment)
        }
    }
}
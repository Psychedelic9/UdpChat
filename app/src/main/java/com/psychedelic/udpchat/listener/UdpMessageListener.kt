package com.psychedelic.udpchat.listener

import com.psychedelic.udpchat.ChatEntity

interface UdpMessageListener {
    fun onMessageReceive(msg:ChatEntity)
}
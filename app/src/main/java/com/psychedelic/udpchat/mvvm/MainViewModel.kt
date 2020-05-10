package com.psychedelic.udpchat.mvvm

import androidx.lifecycle.ViewModel
import com.psychedelic.udpchat.ChatEntity
import com.psychedelic.udpchat.listener.UdpMessageListener
import com.psychedelic.udpchat.listener.UdpMessageSendListener
import com.psychedelic.udpchat.net.UdpManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainViewModel():ViewModel(){
    private lateinit var mReceiveExecutor: ExecutorService
    private lateinit var mSendExecutor: ExecutorService
    private lateinit var mUdpManager: UdpManager

    fun startReceiveUdpMsg(intIpAddress:Int,listener:UdpMessageListener){
        mUdpManager = UdpManager(intIpAddress,listener)
        mReceiveExecutor = Executors.newSingleThreadExecutor()
        mSendExecutor = Executors.newFixedThreadPool(5)
        mReceiveExecutor.submit { mUdpManager.receiverUdpMsg()}
    }

    fun sendUdpMsg(msg:String,listener: UdpMessageSendListener){
        mSendExecutor.submit {
            mUdpManager.sendUdpMsg(msg,listener)
        }
    }

    fun shutDownExecutor(){
        mSendExecutor.shutdown()
        mReceiveExecutor.shutdown()
    }

}
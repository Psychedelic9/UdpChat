package com.psychedelic.udpchat.net

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import com.psychedelic.udpchat.ChatEntity
import com.psychedelic.udpchat.FROM_OTHERS
import com.psychedelic.udpchat.FROM_SELF
import com.psychedelic.udpchat.TAG
import com.psychedelic.udpchat.listener.UdpMessageListener
import com.psychedelic.udpchat.listener.UdpMessageSendListener
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

class UdpManager(ipAddress: Int,listener:UdpMessageListener) {
    private var mLocalIp:String ?=null
    private val mIntIpAddress = ipAddress
    private val mPort = 8211
    private val mListener = listener
    fun sendUdpMsg(data: String,sendListener: UdpMessageSendListener) {
        if (data.isEmpty() || data.isBlank()){
            return
        }
        /*这一步就是将本机的IP地址转换成xxx.xxx.xxx.255*/
        val broadCastIP = mIntIpAddress or -0x1000000
        mLocalIp = "/${Formatter.formatIpAddress(mIntIpAddress)}"
        Log.d(TAG, "sendUdpMsg ip = $broadCastIP")

        var sendSocket: DatagramSocket? = null
        try {
            val server: InetAddress = InetAddress.getByName(Formatter.formatIpAddress(broadCastIP))
            Log.d(TAG, "sendUdpMsg server = $server")

            sendSocket = DatagramSocket()
            val msg = String(data.toByteArray(),Charsets.UTF_8)
            Log.d(TAG,"msg = $msg")
            val theOutput = DatagramPacket((msg).toByteArray(), msg.toByteArray().size, server, mPort)
            Log.d(TAG, "mLocalIp = $mLocalIp")

            sendSocket.send(theOutput)
            sendListener.sendSuccess()
            Log.d(TAG, "sendUdpMsg send !!!")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            sendSocket?.close()
        }
    }

    fun receiverUdpMsg() {
        Log.d(TAG, "receiverUdpMsg")
        val buffer = ByteArray(1024)
        /*在这里同样使用约定好的端口*/
        var server: DatagramSocket? = null
        try {
            server = DatagramSocket(mPort)
            val packet = DatagramPacket(buffer, buffer.size)

            while (true) {
                try {
                    server.receive(packet)
                    val content = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    Log.d(TAG,"content = $content ")
                    Log.d(TAG, "get ip = ${packet.address} mLocalIP = $mLocalIp")
                    val msg = ChatEntity().apply { text = content }
                    if (packet.address.toString() == mLocalIp){
                        msg.fromWho = FROM_SELF
                    }else{
                        msg.fromWho = FROM_OTHERS
                    }
                    mListener.onMessageReceive(msg)
                    Log.d(TAG,"address : " + packet.address + ", port : " + packet.port + ", content : " + content)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: SocketException) {
            Log.d(TAG, "err")
            e.printStackTrace()
        } finally {
            server?.close()
        }
    }

}
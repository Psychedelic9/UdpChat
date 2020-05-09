package com.psychedelic.udpchat

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.psychedelic.udpchat.databinding.ActivityMainBinding
import com.psychedelic.udpchat.util.StatusBarUtil
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private val mContext = this
    private val mPort = 8211
    private var mLocalIp:String ?=null
    private lateinit var mReceiveExecutor: ExecutorService
    private lateinit var mSendExecutor:ExecutorService
    private var mList = ArrayList<ChatEntity>()
    private lateinit var mAdapter: ChatRvAdapter
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mWifiManager: WifiManager
    private lateinit var mWifiInfo: WifiInfo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        window.statusBarColor = resources.getColor(R.color.bar_color)
        StatusBarUtil.setStatusTextColor(true, this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        requestPermission()
        mWifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mWifiInfo =  mWifiManager.connectionInfo
        Log.d(TAG,"SSID = ${mWifiInfo.ssid}")
        mBinding.chatToolBar.title = mWifiInfo.ssid
        setSupportActionBar(mBinding.chatToolBar)
        mAdapter = ChatRvAdapter(this, mList, BR.item)
        mBinding.chatRecycleView.layoutManager = LinearLayoutManager(this)
        mBinding.chatRecycleView.adapter = mAdapter
        mReceiveExecutor = Executors.newSingleThreadExecutor()
        mSendExecutor = Executors.newFixedThreadPool(5)
        mReceiveExecutor.submit { receiverUdpMsg() }
        mBinding.chatToolBar.setNavigationOnClickListener {
            Log.d(TAG,"NavigationOnClick")
            finish()
        }


    }

    private fun requestPermission(){
        val permissions = arrayOf<String>(ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION)
        if (lackPermission(permissions)) {
            ActivityCompat.requestPermissions(
                mContext,
                permissions,
                REQUEST_PERMISSIONS
            )
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS){
            for ((index,permission) in permissions.withIndex()){
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG,"permission = $permission grantResults[index] = ${grantResults[index]}")
//                   Toast.makeText(mContext,"不获取 $permission 程序无法正常运行，请授权",Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun lackPermission(permissions: Array<String>):Boolean{
        for (permission in permissions){
            if (ContextCompat.checkSelfPermission(mContext,permission)!= PackageManager.PERMISSION_GRANTED){
                return true
            }
        }
        return false
    }
    private fun sendUdpMsg(data: String) {
        if (data.isEmpty() || data.isBlank()){
            return
        }
        /*这里获取了IP地址，获取到的IP地址还是int类型的。*/
        val ip = mWifiInfo.ipAddress
        Log.d(TAG,"mLocalIp = $mLocalIp")
        /*这一步就是将本机的IP地址转换成xxx.xxx.xxx.255*/
        val broadCastIP = ip or -0x1000000
        mLocalIp = "/${Formatter.formatIpAddress(ip)}"
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
            Log.d(TAG, "sendUdpMsg send !!!")


        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            sendSocket?.close()
        }
    }

    private fun receiverUdpMsg() {
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
                    runOnUiThread {
                        refreshNewMessage(msg)
                    }
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

    fun refreshNewMessage(msg: ChatEntity) {
        Log.d(TAG,"refreshNewMessage msg = ${msg.text}")
        mList.add(msg)
        mAdapter.refreshData(mList)
        scrollToEnd()
    }

    fun sendMessageButtonClick(view: View) {
        if (mBinding.chatEditText.text.isNotEmpty()){
            mSendExecutor.submit {
                Log.d(TAG, "onClick")
                sendUdpMsg(mBinding.chatEditText.text.toString())
                mBinding.chatEditText.text.clear()
            }
        }

    }

     fun scrollToEnd() {
        if (mBinding.chatRecycleView.adapter!!.itemCount > 0) {
            mBinding.chatRecycleView.smoothScrollToPosition(mBinding.chatRecycleView.adapter!!.itemCount)
        }
    }
}

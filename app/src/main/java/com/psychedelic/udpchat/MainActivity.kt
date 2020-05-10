package com.psychedelic.udpchat

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.psychedelic.udpchat.databinding.ActivityMainBinding
import com.psychedelic.udpchat.listener.MainActivityObserver
import com.psychedelic.udpchat.listener.UdpMessageListener
import com.psychedelic.udpchat.listener.UdpMessageSendListener
import com.psychedelic.udpchat.mvvm.MainViewModel
import com.psychedelic.udpchat.util.StatusBarUtil


const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(),UdpMessageListener {
    private val mContext = this

    private var mList = ArrayList<ChatEntity>()
    private lateinit var mAdapter: ChatRvAdapter
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mWifiManager: WifiManager
    private lateinit var mWifiInfo: WifiInfo
    private lateinit var mViewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        StatusBarUtil.setStatusTextColor(true, this)
        window.statusBarColor = resources.getColor(R.color.bar_color)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        mWifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mWifiInfo = mWifiManager.connectionInfo
        Log.d(TAG, "SSID = ${mWifiInfo.ssid}")
        mBinding.chatToolBar.title = mWifiInfo.ssid
        setSupportActionBar(mBinding.chatToolBar)
        mAdapter = ChatRvAdapter(this, mList, BR.item)
        mBinding.chatRecycleView.layoutManager = LinearLayoutManager(this)
        mBinding.chatRecycleView.adapter = mAdapter
        mBinding.chatToolBar.setNavigationOnClickListener {
            finish()
        }
        lifecycle.addObserver(MainActivityObserver(mContext,mWifiManager,mViewModel,this))
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS) {
            for ((index, permission) in permissions.withIndex()) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(
                        TAG,
                        "permission = $permission grantResults[index] = ${grantResults[index]}"
                    )
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun refreshNewMessage(msg: ChatEntity) {
        Log.d(TAG, "refreshNewMessage msg = ${msg.text}")
        runOnUiThread {
            mList.add(msg)
            mAdapter.refreshData(mList)
            scrollToEnd()
        }
    }

    fun sendMessageButtonClick(view: View) {
        if (mBinding.chatEditText.text.isNotEmpty()) {
            mViewModel.sendUdpMsg(mBinding.chatEditText.text.toString(),
                object : UdpMessageSendListener {
                    override fun sendSuccess() {
                        runOnUiThread {
                            mBinding.chatEditText.text.clear()
                        }
                    }
                })
        }
    }

    private fun scrollToEnd() {
        if (mBinding.chatRecycleView.adapter!!.itemCount > 0) {
            mBinding.chatRecycleView.smoothScrollToPosition(mBinding.chatRecycleView.adapter!!.itemCount)
        }
    }

    override fun onMessageReceive(msg: ChatEntity) {
        refreshNewMessage(msg)
    }
}

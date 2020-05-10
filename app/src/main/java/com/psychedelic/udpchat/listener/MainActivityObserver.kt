package com.psychedelic.udpchat.listener

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.psychedelic.udpchat.REQUEST_PERMISSIONS
import com.psychedelic.udpchat.mvvm.MainViewModel


class MainActivityObserver(context: Context,wifiManager: WifiManager,viewModel:MainViewModel,listener: UdpMessageListener):LifecycleObserver {
    private val mContext = context
    private val mListener = listener
    private val mViewModel = viewModel
    private var mWifiManager: WifiManager = wifiManager
    private val permissions = arrayOf<String>(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create(){
        requestPermission()
        if (!lackPermission()){
            val ipAddress = mWifiManager.connectionInfo.ipAddress
            mViewModel.startReceiveUdpMsg(ipAddress,mListener)
        }else{
            Toast.makeText(mContext,"缺少网络权限，请授权后重试",Toast.LENGTH_LONG).show()
            (mContext as Activity).finish()
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy(){
        mViewModel.shutDownExecutor()
    }

    private fun requestPermission(){

        if (lackPermission()) {
            ActivityCompat.requestPermissions(
                mContext as Activity,
                permissions,
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun lackPermission():Boolean{
        for (permission in permissions){
            if (ContextCompat.checkSelfPermission(mContext,permission)!= PackageManager.PERMISSION_GRANTED){
                return true
            }
        }
        return false
    }


}
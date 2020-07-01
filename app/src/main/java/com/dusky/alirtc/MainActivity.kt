package com.dusky.alirtc
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.readboy.alirtc.bean.RTCAuthInfo
import com.readboy.alirtc.ui.AliRTCFragment

class MainActivity : AppCompatActivity() ,AliRTCFragment.OnStatusChangeListener{
     val TAG_ROOM_FRAG_ALI = "AliRoomFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mRtcAuthInfo= RTCAuthInfo()
        //以下数据接口返回,请自行配置
        //mRtcAuthInfo.data= RTCAuthInfo.RTCAuthInfo_Data()
        //mRtcAuthInfo.data.turn= it.aliyun?.turn
        //mRtcAuthInfo.data.appid=it.aliyun?.appid
        //mRtcAuthInfo.data.nonce=it.aliyun?.nonce
        //mRtcAuthInfo.data.timestamp=it.aliyun?.timestamp!!
        //mRtcAuthInfo.data.userid=it.aliyun?.userid
        //mRtcAuthInfo.data.gslb=it.aliyun?.gslb!!
        //mRtcAuthInfo.data.token=it.aliyun?.token
        val fragment= AliRTCFragment.newInstance("dusky","10086",mRtcAuthInfo,this)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment, TAG_ROOM_FRAG_ALI)
    }

    override fun onJoinRoom() {

    }

    override fun onDisconnect(duration: Long) {

    }
}

package com.readboy.alirtc.ui


import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.alivc.rtc.AliRtcAuthInfo
import com.alivc.rtc.AliRtcEngine
import com.alivc.rtc.AliRtcEngine.*
import com.alivc.rtc.AliRtcEngineEventListener
import com.alivc.rtc.AliRtcEngineNotify
import com.readboy.alirtc.R
import com.readboy.alirtc.adapter.RtcAdapter
import com.readboy.alirtc.bean.ChartUserBean
import com.readboy.alirtc.bean.RTCAuthInfo
import com.readboy.alirtc.utils.AliRtcConstants
import com.readboy.alirtc.utils.ThreadUtils.runOnUiThread
import kotlinx.android.synthetic.main.fragment_rtc.*
import org.webrtc.alirtcInterface.ALI_RTC_INTERFACE.AliRTCSDK_Client_Role
import org.webrtc.alirtcInterface.ALI_RTC_INTERFACE.AliRtcStats
import org.webrtc.alirtcInterface.AliParticipantInfo
import org.webrtc.alirtcInterface.AliStatusInfo
import org.webrtc.alirtcInterface.AliSubscriberInfo


/**
 * RTC 音视频通讯界面
 */
class AliRTCFragment : Fragment() {
    interface OnStatusChangeListener {
        fun onJoinRoom()
        fun onDisconnect(duration: Long)
    }

    var onStatusChangeListener:OnStatusChangeListener?=null

    /**
     * 权限判断
     */
    private var mGrantPermission = false
    /**
     * rtcAuthInfo，本地用户加入房间的时候返回的json
     */
    var rtcAuthInfo: RTCAuthInfo? = null
    /**
     * SDK提供的对音视频通话处理的引擎类
     */
    var mAliRtcEngine: AliRtcEngine? = null
    val aliVideoCanvas = AliVideoCanvas()
    private val datas= ArrayList<ChartUserBean>()
    var mUserListAdapter: RtcAdapter?=null
    private var firstJoinedRoomTs: Long = 0
    var mCamera = Camera.CameraInfo.CAMERA_FACING_FRONT  //摄像头方向
    companion object {
        private const val USER_ID = "userId"
        private const val USER_NAME = "userName"
        private const val CHANNEL = "channel"

        @JvmStatic
        fun newInstance(userName:String,channel: String,mRtcAuthInfo:RTCAuthInfo,onStatusChangeListener:OnStatusChangeListener): AliRTCFragment =
                AliRTCFragment().apply {
                    arguments = Bundle().apply {
                        putString(CHANNEL, channel)
                        putString(USER_NAME, userName)
                    }
                    this.onStatusChangeListener=onStatusChangeListener
                    rtcAuthInfo=mRtcAuthInfo
                }
    }

    private fun joinChannel() {
        if (mAliRtcEngine == null) {
            return
        }
        if (firstJoinedRoomTs == 0L)
            firstJoinedRoomTs = System.currentTimeMillis()
        onStatusChangeListener?.onJoinRoom()
        val userInfo = AliRtcAuthInfo()
        userInfo.appid = rtcAuthInfo?.data?.appid
        userInfo.nonce = rtcAuthInfo?.data?.nonce
        userInfo.timestamp = rtcAuthInfo?.data?.timestamp!!
        userInfo.userId = rtcAuthInfo?.data?.userid
        userInfo.gslb = rtcAuthInfo?.data?.gslb
        userInfo.token = rtcAuthInfo?.data?.token
        userInfo.conferenceId = arguments?.getString(CHANNEL)
        /*
         *设置自动发布和订阅，只能在joinChannel之前设置
         *参数1    true表示自动发布；false表示手动发布
         *参数2    true表示自动订阅；false表示手动订阅
         */mAliRtcEngine!!.setAutoPublish(true, true)
        // 加入频道
        mAliRtcEngine?.joinChannel(userInfo, arguments?.getString(USER_NAME))
        val me=ChartUserBean()
        me.mUserId= userInfo.mUserId
        me.mUserName=arguments?.getString(USER_NAME)
        me.type=AliRtcVideoTrack.AliRtcVideoTrackCamera
        datas.add(me)
        mUserListAdapter?.notifyDataSetChanged()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rtc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRTCEngineAndStartPreview();
        initView()
        joinChannel()
    }

    private fun checkPermission(permission: String): Boolean {
        try {
            val i: Int = ActivityCompat.checkSelfPermission(context!!, permission)
            if (i != PackageManager.PERMISSION_GRANTED) {
                return true
            }
        } catch (e: RuntimeException) {
            return true
        }
        return false
    }

    private fun initRTCEngineAndStartPreview() {
        if (checkPermission(Manifest.permission.CAMERA) || checkPermission(
                        Manifest.permission.MODIFY_AUDIO_SETTINGS)) {
            Toast.makeText(context, "需要开启权限才可进行正常使用", Toast.LENGTH_SHORT).show()
            mGrantPermission = false
            return
        }
        mGrantPermission = true
        // 防止初始化过多
        if (mAliRtcEngine == null) { //实例化,必须在主线程进行。
            //getH5CompatibleMode：检查当前是否兼容H5，返回1表示兼容，0表示不兼容。
            setH5CompatibleMode(1)
            mAliRtcEngine = getInstance(context)
            //设置事件的回调监听
            mAliRtcEngine?.setRtcEngineEventListener(mEventListener)
            //设置接受通知事件的回调
            mAliRtcEngine?.setRtcEngineNotify(mEngineNotify)


            //屏幕方向配置
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, info)
            Log.i("rotation:",info.orientation.toString())
            when (info.orientation) {
                0 ->  mAliRtcEngine?.setDeviceOrientationMode(AliRtcOrientationMode.AliRtcOrientationModePortrait)
                90 ->  mAliRtcEngine?.setDeviceOrientationMode(AliRtcOrientationMode.AliRtcOrientationModeLandscapeLeft)
                180 -> mAliRtcEngine?.setDeviceOrientationMode(AliRtcOrientationMode.AliRtcOrientationModeLandscapeRight)
                270 -> mAliRtcEngine?.setDeviceOrientationMode(AliRtcOrientationMode.AliRtcOrientationModeLandscapeLeft)
                else->  mAliRtcEngine?.setDeviceOrientationMode(AliRtcOrientationMode.AliRtcOrientationModePortrait)
            }
            mAliRtcEngine?.startAudioCapture()
            mAliRtcEngine?.startAudioPlayer()
            //声音录入配置0-400
//mAliRtcEngine.setRecordingVolume(100);
//声音输出配置0-400
//mAliRtcEngine.setPlayoutVolume(200);
// 初始化本地视图
            //开启预览
        }
    }




    fun initView(){
        // 承载User的Adapter
        initAdminView()
        mUserListAdapter = context?.let { RtcAdapter(it, datas, mAliRtcEngine!!) }
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        chart_content_userlist.layoutManager = layoutManager
        val anim = DefaultItemAnimator()
        anim.supportsChangeAnimations = false
        chart_content_userlist.itemAnimator = anim
        chart_content_userlist.adapter = mUserListAdapter
    }
    fun initAdminView(){
        sf_remote_view.holder.setFormat(PixelFormat.TRANSLUCENT)
        sf_remote_view.setZOrderOnTop(false)
        sf_remote_view.setZOrderMediaOverlay(false)
        aliVideoCanvas.view = sf_remote_view
        aliVideoCanvas.renderMode = AliRtcRenderMode.AliRtcRenderModeAuto
    }

    private fun updateRemoteDisplay(uid: String, at: AliRtcAudioTrack, vt: AliRtcVideoTrack) {
        runOnUiThread(Runnable {
            if (null == mAliRtcEngine||uid=="admin_android") {//助教的不需要更新显示
                return@Runnable
            }
            val remoteUserInfo = mAliRtcEngine!!.getUserInfo(uid)
            // 如果没有，说明已经退出了或者不存在。则不需要添加，并且删除
            if (remoteUserInfo == null) { // remote user exit room
                Log.e("RTC", "updateRemoteDisplay remoteUserInfo = null, uid = $uid")
                return@Runnable
            }
            val chartUserBean=ChartUserBean()
            chartUserBean.mUserId=remoteUserInfo.userID
            chartUserBean.mUserName=remoteUserInfo.displayName
            chartUserBean.type=vt
            if(chartUserBean.mUserId=="admin"){//老师，显示在本地窗口
                when(vt){
                    AliRtcVideoTrack.AliRtcVideoTrackCamera->  mAliRtcEngine?.setRemoteViewConfig(aliVideoCanvas,chartUserBean.mUserId,AliRtcVideoTrack.AliRtcVideoTrackCamera)
                    AliRtcVideoTrack.AliRtcVideoTrackScreen->  mAliRtcEngine?.setRemoteViewConfig(aliVideoCanvas,chartUserBean.mUserId,AliRtcVideoTrack.AliRtcVideoTrackScreen)
                    AliRtcVideoTrack.AliRtcVideoTrackBoth->{
                        mAliRtcEngine?.setRemoteViewConfig(aliVideoCanvas,chartUserBean.mUserId,AliRtcVideoTrack.AliRtcVideoTrackCamera)
                        mAliRtcEngine?.setRemoteViewConfig(aliVideoCanvas,chartUserBean.mUserId,AliRtcVideoTrack.AliRtcVideoTrackScreen)
                    }
                    else -> {

                    }
                }
            }else{
                var added=false
                var position=0
                for ((index, element) in datas.withIndex()) {
                    if(element.mUserId==chartUserBean.mUserId){
                        added=true
                        position=index
                    }
                }
                if(!added){
                    datas.add(chartUserBean)
                }
                if(position!=0){
                    mUserListAdapter?.notifyItemChanged(position)
                }
            }



        })
    }




    private fun removeRemoteUser(uid: String) {
        runOnUiThread {
            if (null == mAliRtcEngine) {
                return@runOnUiThread
            }
            if(uid=="admin") {//老师断开
                disconnect()
                return@runOnUiThread
            }
            var position=0
            for ((index, element) in datas.withIndex()) {
                if(element.mUserId==uid){
                    position=index
                }
            }
            if(position!=0){
                datas.removeAt(position)
                mUserListAdapter?.notifyItemRemoved(position)
                mUserListAdapter?.notifyItemRangeChanged(position,datas.size)
            }


        }
    }


    /**
     * 特殊错误码回调的处理方法
     *
     * @param error 错误码
     */
    private fun processOccurError(error: Int) {
        when (error) {
            AliRtcConstants.SOPHON_SERVER_ERROR_POLLING, AliRtcConstants.SOPHON_RESULT_SIGNAL_HEARTBEAT_TIMEOUT -> noSessionExit(error)
            else -> {
            }
        }
    }

    /**
     * 错误处理
     *
     * @param error 错误码
     */
    private fun noSessionExit(error: Int) {
        runOnUiThread {
            context?.let {
                AlertDialog.Builder(it)
                        .setTitle("ErrorCode : $error")
                        .setMessage("网络超时，请退出房间")
                        .setPositiveButton("确定", DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                            dialog.dismiss()
                            disconnect()
                        })
                        .create()
                        .show()
            }
        }
    }


    /**
     * 用户操作回调监听(回调接口都在子线程)
     */
    private val mEventListener: AliRtcEngineEventListener = object : AliRtcEngineEventListener {
        /**
         * 加入房间的回调
         * @param i 结果码
         */
        override fun onJoinChannelResult(i: Int) {
            Log.d("mEventListener", "onJoinChannelResult:$i")
        }

        /**
         * 离开房间的回调
         * @param i 结果码
         */
        override fun onLeaveChannelResult(i: Int) {
            Log.d("mEventListener", "onLeaveChannelResult:$i")
        }


        /**
         * 推流的回调
         * @param i 结果码
         * @param s publishId
         */
        override fun onPublishResult(i: Int, s: String) {

        }

        /**
         * 取消发布本地流回调
         * @param i 结果码
         */
        override fun onUnpublishResult(i: Int) {}

        /**
         * 订阅成功的回调
         * @param s userid
         * @param i 结果码
         * @param aliRtcVideoTrack 视频的track
         * @param aliRtcAudioTrack 音频的track
         */
        override fun onSubscribeResult(s: String, i: Int, aliRtcVideoTrack: AliRtcVideoTrack,
                                       aliRtcAudioTrack: AliRtcAudioTrack) {
            if (i == 0) {
                updateRemoteDisplay(s, aliRtcAudioTrack, aliRtcVideoTrack)
            }
        }

        /**
         * 取消的回调
         * @param i 结果码
         * @param s userid
         */
        override fun onUnsubscribeResult(i: Int, s: String) {
            updateRemoteDisplay(s, AliRtcAudioTrack.AliRtcAudioTrackNo, AliRtcVideoTrack.AliRtcVideoTrackNo)
        }

        /**
         * 网络状态变化的回调
         * @param aliRtcNetworkQuality
         */
        override fun onNetworkQualityChanged(s: String, aliRtcNetworkQuality: AliRtcNetworkQuality, aliRtcNetworkQuality1: AliRtcNetworkQuality) {}

        /**
         * 出现警告的回调
         * @param i
         */
        override fun onOccurWarning(i: Int) {}

        /**
         * 出现错误的回调
         * @param error 错误码
         */
        override fun onOccurError(error: Int) { //错误处理
            processOccurError(error)
        }

        /**
         * 当前设备性能不足
         */
        override fun onPerformanceLow() {}

        /**
         * 当前设备性能恢复
         */
        override fun onPermormanceRecovery() {}

        /**
         * 连接丢失
         */
        override fun onConnectionLost() {}

        /**
         * 尝试恢复连接
         */
        override fun onTryToReconnect() {}

        /**
         * 连接已恢复
         */
        override fun onConnectionRecovery() {}

        /**
         * @param aliRTCSDK_client_role
         * @param aliRTCSDK_client_role1
         * 用户角色更新
         */
        override fun onUpdateRoleNotify(aliRTCSDK_client_role: AliRTCSDK_Client_Role, aliRTCSDK_client_role1: AliRTCSDK_Client_Role) {}
    }

    /**
     * SDK事件通知(回调接口都在子线程)
     */
    private val mEngineNotify: AliRtcEngineNotify = object : AliRtcEngineNotify {
        /**
         * 远端用户停止发布通知，处于OB（observer）状态
         * @param aliRtcEngine 核心引擎对象
         * @param s userid
         */
        override fun onRemoteUserUnPublish(aliRtcEngine: AliRtcEngine, s: String) {
            updateRemoteDisplay(s, AliRtcAudioTrack.AliRtcAudioTrackNo, AliRtcVideoTrack.AliRtcVideoTrackNo)
        }

        /**
         * 远端用户上线通知
         * @param s userid
         */
        override fun onRemoteUserOnLineNotify(s: String) {

        }

        /**
         * 远端用户下线通知
         * @param s userid
         */
        override fun onRemoteUserOffLineNotify(s: String) {
            removeRemoteUser(s)
        }

        /**
         * 远端用户发布音视频流变化通知
         * @param s userid
         * @param aliRtcAudioTrack 音频流
         * @param aliRtcVideoTrack 相机流
         */
        override fun onRemoteTrackAvailableNotify(s: String, aliRtcAudioTrack: AliRtcAudioTrack,
                                                  aliRtcVideoTrack: AliRtcVideoTrack) {
            updateRemoteDisplay(s, aliRtcAudioTrack, aliRtcVideoTrack)
        }

        /**
         * 订阅流回调，可以做UI及数据的更新
         * @param s userid
         * @param aliRtcAudioTrack 音频流
         * @param aliRtcVideoTrack 相机流
         */
        override fun onSubscribeChangedNotify(s: String, aliRtcAudioTrack: AliRtcAudioTrack,
                                              aliRtcVideoTrack: AliRtcVideoTrack) {

        }

        /**
         * 订阅信息
         * @param aliSubscriberInfos 订阅自己这边流的user信息
         * @param i 当前订阅人数
         */
        override fun onParticipantSubscribeNotify(aliSubscriberInfos: Array<AliSubscriberInfo>, i: Int) {

        }

        /**
         * 首帧的接收回调
         * @param s callId
         * @param s1 stream_label
         * @param s2 track_label 分为video和audio
         * @param i 时间
         */
        override fun onFirstFramereceived(s: String, s1: String, s2: String, i: Int) {
            Log.d("RTC","首帧接受成功")
        }

        /**
         * 首包的发送回调
         * @param s callId
         * @param s1 stream_label
         * @param s2 track_label 分为video和audio
         * @param i 时间
         */
        override fun onFirstPacketSent(s: String, s1: String, s2: String, i: Int) {
            Log.d("RTC","首包发送成功")
        }

        /**
         * 首包数据接收成功
         * @param callId 远端用户callId
         * @param streamLabel 远端用户的流标识
         * @param trackLabel 远端用户的媒体标识
         * @param timeCost 耗时
         */
        override fun onFirstPacketReceived(callId: String, streamLabel: String, trackLabel: String, timeCost: Int) {

        }

        /**
         * 取消订阅信息回调
         * @param aliParticipantInfos 订阅自己这边流的user信息
         * @param i 当前订阅人数
         */
        override fun onParticipantUnsubscribeNotify(aliParticipantInfos: Array<AliParticipantInfo>, i: Int) {}

        /**
         * 被服务器踢出或者频道关闭时回调
         * @param i
         */
        override fun onBye(i: Int) {
            runOnUiThread {
                Toast.makeText(context,"您被终止了连麦",Toast.LENGTH_SHORT).show()
                disconnect()
            }
        }

        override fun onParticipantStatusNotify(aliStatusInfos: Array<AliStatusInfo>, i: Int) {

        }
        /**
         * @param aliRtcStats
         * 实时数据回调(2s触发一次)
         */
        override fun onAliRtcStats(aliRtcStats: AliRtcStats) {

        }
    }


    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        mAliRtcEngine?.destroy()
    }

    private fun disconnect() {
        mAliRtcEngine?.leaveChannel();
        var duration = 0L
        if (firstJoinedRoomTs > 0)
            duration = System.currentTimeMillis() - firstJoinedRoomTs
        onStatusChangeListener?.onDisconnect(duration)
        onStatusChangeListener=null
    }
}

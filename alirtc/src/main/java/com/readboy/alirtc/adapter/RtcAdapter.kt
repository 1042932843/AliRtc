package com.readboy.alirtc.adapter


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alivc.rtc.AliRtcEngine
import com.readboy.alirtc.R
import com.readboy.alirtc.bean.ChartUserBean
import org.webrtc.sdk.SophonSurfaceView


/**
 * @AUTHOR: dsy
 * @TIME: 2020/4/1
 * @DESCRIPTION:
 */
class RtcAdapter(internal var context: Context, private val datas: ArrayList<ChartUserBean>,private val mAliRtcEngine: AliRtcEngine) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var onItemClickListener: OnItemClickListener

    /**
     * item显示类型
     * @param parent
     * @param viewType
     * @return
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(R.layout.rtc_item, parent, false)
        return  ViewHolderTypeDef(view)
    }

    /**
     * 数据的绑定显示
     * @param holder
     * @param position
     */
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = datas[position]
        val id=data.mUserId
        if (holder is ViewHolderTypeDef) {
            holder.sf_remote_view.holder.setFormat(PixelFormat.TRANSLUCENT)
            holder.sf_remote_view.setZOrderOnTop(true)//必须为true画面才能显示在最前（不被覆盖遮挡）
            holder.sf_remote_view.setZOrderMediaOverlay(true)
            val aliVideoCanvas = AliRtcEngine.AliVideoCanvas()
            aliVideoCanvas.view = holder.sf_remote_view
            aliVideoCanvas.renderMode = AliRtcEngine.AliRtcRenderMode.AliRtcRenderModeClip
            if(position==0){//0的位置永远属于自己
                mAliRtcEngine.setLocalViewConfig(aliVideoCanvas, AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackCamera)
                startPreview()
            }else{
                when(data.type){
                    AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackCamera->  mAliRtcEngine.setRemoteViewConfig(aliVideoCanvas,id,AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackCamera)
                    AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackScreen->  mAliRtcEngine.setRemoteViewConfig(aliVideoCanvas,id,AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackScreen)
                    AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackBoth->{
                        mAliRtcEngine.setRemoteViewConfig(aliVideoCanvas,id,AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackCamera)
                        mAliRtcEngine.setRemoteViewConfig(aliVideoCanvas,id,AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackScreen)
                    }
                    else -> {

                    }
                }

            }
        }

    }

    override fun getItemCount(): Int {
        return  datas.size
    }

    //自定义的ViewHolder，持有每个Item的的所有界面元素
    inner class ViewHolderTypeDef(view: View) : RecyclerView.ViewHolder(view) {
        var sf_remote_view= itemView.findViewById<SophonSurfaceView>(R.id.sf_remote_view)
    }


    interface OnItemClickListener {
        fun onClick(position: Int)

    }

    private fun startPreview() {
        try {
            mAliRtcEngine.startPreview()
            mAliRtcEngine.startAudioCapture()
            mAliRtcEngine.startAudioPlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }
}


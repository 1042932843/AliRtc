package com.readboy.alirtc.utils

/**
 * 常量类。包含网络请求，错误码
 */
object AliRtcConstants {
    /**
     * 需要特殊处理的错误码，信令错误与心跳超时
     */
    const val SOPHON_SERVER_ERROR_POLLING = 0x02010105
    const val SOPHON_RESULT_SIGNAL_HEARTBEAT_TIMEOUT = 0x0102020C
    /**
     * 手机机型
     */
    const val BRAND_OPPO = "OPPO"
    const val MODEL_OPPO_R17 = "PBDM00"
    const val CAMERA = 1001
    const val SCREEN = 1002
    val VIDEO_INFO_KEYS =
        arrayOf("Width", "Height", "FPS", "LossRate")
}
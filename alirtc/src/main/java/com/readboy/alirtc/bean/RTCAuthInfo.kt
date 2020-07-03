package com.readboy.alirtc.bean

import java.io.Serializable

/**
 * 服务器返回的包含加入频道信息的业务类
 */
class RTCAuthInfo : Serializable {
    var code = 0
    var data: RTCAuthInfo_Data? = null

    class RTCAuthInfo_Data : Serializable {
        var appid: String? = null
        var userid: String? = null
        var nonce: String? = null
        var timestamp: Long = 0
        var token: String? = null
        var turn: RTCAuthInfo_Data_Turn? = null

        class RTCAuthInfo_Data_Turn : Serializable {
            var username: String? = null
            var password: String? = null

        }

        lateinit var gslb: Array<String>
        var key: String? = null
    }

    var server = 0

}
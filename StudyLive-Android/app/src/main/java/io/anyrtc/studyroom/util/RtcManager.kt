package io.anyrtc.studyroom.util

import android.content.Context
import android.view.TextureView
import io.anyrtc.studyroom.App
import io.anyrtc.studyroom.utils.MyConst
import io.anyrtc.studyroom.utils.SpUtil
import org.ar.rtc.Constants
import org.ar.rtc.IRtcEngineEventHandler
import org.ar.rtc.RtcEngine
import org.ar.rtc.video.VideoCanvas
import org.ar.rtm.*

class RtcManager private constructor(){

    companion object {
        val INSTANCE by lazy {
            RtcManager()
        }
    }

    private lateinit var rtmClient: RtmClient
    private lateinit var rtcEngine: RtcEngine

    private var rtmChannel: RtmChannel? = null

    fun initRtc(context: Context, rtcListener: IRtcEngineEventHandler) {
        rtcEngine = RtcEngine.create(
            context, SpUtil.get().getString(MyConst.APP_ID, ""), rtcListener
        )
    }

    fun initRtm(listener: RtmClientListener) {
        rtmClient = RtmClient.createInstance(
            App.app.applicationContext,
            SpUtil.get().getString(MyConst.APP_ID, "").toString(),
            listener
        )
    }

    fun loginRtm(rtmToken: String, uid: String, loginFailedCallback: () -> Unit) {
        logoutRtm()
        rtmClient.login(rtmToken, uid, object : ResultCallback<Void> {
            override fun onSuccess(var1: Void?) {
            }

            override fun onFailure(var1: ErrorInfo?) {
                loginFailedCallback.invoke()
            }
        })
    }

    private fun logoutRtm() {
        rtmClient.logout(null)
    }

    fun enableVideo() = rtcEngine.enableVideo()

    fun enableAudio(enable: Boolean) {
        rtcEngine.enableLocalAudio(enable)
    }

    fun setChannelAttributes(channelId: String, list: List<RtmChannelAttribute>) {
        rtmClient.addOrUpdateChannelAttributes(
            channelId,
            list,
            ChannelAttributeOptions(true),
            null
        )
    }

    fun switchCamera() {
        rtcEngine.switchCamera()
    }

    /*fun getMemberInfo(uid: String, callback: (RtmChannelMember?) -> Unit) {
        this.getMemberList { list ->
            list ?: return@getMemberList
            list.forEach {
                if (it.userId == uid) {
                    callback.invoke(it)
                    return@getMemberList
                }
            }
            callback.invoke(null)
        }
    }*/

    fun joinChannel(
        rtcToken: String, roomId: String,
        userId: String, listener: RtmChannelListener,
        joinCallback: (failed: Boolean) -> Unit
    ) {
        rtcEngine.run {
            setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
            joinChannel(rtcToken, roomId, "", userId)
			setEnableSpeakerphone(true)
        }
        rtmChannel?.leave(null)

        rtmChannel = rtmClient.createChannel(roomId, listener)
        rtmChannel?.join(object : ResultCallback<Void> {
            override fun onSuccess(var1: Void?) {
                joinCallback.invoke(false)
            }

            override fun onFailure(var1: ErrorInfo?) {
                joinCallback.invoke(true)
            }
        })
    }

    fun sendMessage(json: String) {
        rtmChannel?.sendMessage(
            rtmClient.createMessage(json), null
        )
    }

    fun changeRoleToBroadcaster() {
        rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
    }

    fun sendChannelMsg(msg: String) {
        rtmChannel?.sendMessage(rtmClient.createMessage(msg), null)
    }

    fun setUpTextureView(isLocal: Boolean, textureView: TextureView, uid: String) {
        val videoCanvas = VideoCanvas(textureView, Constants.RENDER_MODE_HIDDEN, uid)
        if (isLocal)
            rtcEngine.setupLocalVideo(videoCanvas)
        else
            rtcEngine.setupRemoteVideo(videoCanvas)
    }

    fun clearSelf() {
        RtcEngine.destroy()
        rtmChannel?.let {
            it.leave(null)
            it.release()
        }
        logoutRtm()
        //rtmClient.logout(null)
    }

    fun getMemberList(callback: (List<RtmChannelMember>?) -> Unit) {
        if (rtmChannel == null) {
            callback.invoke(null)
            return
        }
        rtmChannel?.getMembers(object : ResultCallback<List<RtmChannelMember>> {
            override fun onSuccess(var1: List<RtmChannelMember>?) {
                callback.invoke(var1)
            }

            override fun onFailure(var1: ErrorInfo?) {
                callback.invoke(null)
            }
        })
    }
}

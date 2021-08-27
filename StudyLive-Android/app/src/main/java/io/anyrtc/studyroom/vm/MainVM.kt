package io.anyrtc.studyroom.vm

import android.content.Context
import android.view.TextureView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.anyrtc.studyroom.util.RtcManager
import io.anyrtc.studyroom.utils.MyConst
import io.anyrtc.studyroom.utils.SpUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ar.rtc.Constants
import org.ar.rtc.IRtcEngineEventHandler
import org.ar.rtm.*
import rxhttp.*
import rxhttp.wrapper.param.RxHttp


class MainVM : ViewModel() {

    val observerRoomListResult = MutableLiveData<List<MyConst.RoomListItemBean>>()
    val observerSignResult = MutableLiveData<ErrorInfo>()
    val observerJoinFailed = MutableLiveData<ErrorInfo>()
    val observerJoinSuccess = MutableLiveData<MyConst.JoinRoomBean>()

    val joinRtmObserver = MutableLiveData<Boolean>()

    val onJoinChannelSuccess = MutableLiveData<Unit>()
    val onUserJoined = MutableLiveData<String>()
    val onUserOffline = MutableLiveData<String>()

    val selfLostNetwork = MutableLiveData<Boolean>()
    val channelMuteAudio = MutableLiveData<Boolean>()
    val changeLocalAudioState = MutableLiveData<Boolean>()

    val onRtmMessageReceive = MutableLiveData<ChannelMsgData>()

    val onRemoteAudioStateChange = MutableLiveData<AudioStateChangeData>()

    val onMemberChange = MutableLiveData<Unit>()
    val observerModifyNameResult = MutableLiveData<ErrorInfo>()

    val onTokenExperienceTimeout = MutableLiveData<Unit>()

    private var isLogin = false

    private val rtmListener by lazy {
        RtmListener.INSTANCE.also {
            it.addCallback(RtcCallback())
        }
    }
    //val internetLost = MutableLiveData<Boolean>()

    private val firstName = mutableListOf(
        "清蒸", "红烧", "盐焗", "烧烤", "水煮", "油炸", "炖",
        "小炒", "干煸", "麻辣", "爆炒"
    )
    private val secondName = mutableListOf(
        "西红柿", "小龙虾", "四季豆", "肥牛", "土豆", "鲍鱼",
        "大闸蟹", "辣椒", "牛排", "鸡翅", "鸭脖", "鸡蛋"
    )

    private fun getRandomName(): String {
        return firstName.shuffled().take(1).first() + secondName.shuffled().take(1).first()
    }

    fun login() = viewModelScope.launch {
        if (isLogin)
            return@launch

        val uid = SpUtil.get().getString(MyConst.USER_ID, "")
        if (uid.isNullOrEmpty()) {
            RxHttp.postJson(MyConst.SIGN_UP)
                .add("sex", 0)
                .add("userName", getRandomName()).toClass<MyConst.SignUpResult>()
                .awaitResult { result ->
                    if (result.code == 0) SpUtil.edit {
                        it.putString(MyConst.USER_ID, result.data.uid)
                        it.putString(MyConst.USER_NAME, result.data.userName)
                        routeLogin(result.data.uid)
                    } else {
                        observerSignResult.value = ErrorInfo(result.code, result.msg)
                    }
                }.onFailure {
                    observerSignResult.value = ErrorInfo(-1, it.message)
                }
        } else {
            routeLogin(uid)
        }
    }

    private fun routeLogin(uid: String) = viewModelScope.launch {
        RxHttp.postJson(MyConst.LOGIN)
            .add("cType", 1)
            .add("pkg", MyConst.PKG)
            .add("uid", uid).toClass<MyConst.LoginResult>()
            .awaitResult { result ->
                if (result.code == 0) SpUtil.edit {
                    it.putString(MyConst.USER_AVATAR, result.data.avatar)
                    it.putString(MyConst.APP_ID, result.data.appid)
                    it.putString(MyConst.HTTP_TOKEN, result.data.userToken)
                    RtcManager.INSTANCE.initRtm(rtmListener)
                    isLogin = true
                    observerSignResult.value = ErrorInfo(0, uid)
                } else {
                    observerSignResult.value = ErrorInfo(result.code, result.msg)
                }
            }.onFailure {
                observerSignResult.value = ErrorInfo(-1, it.message)
            }
    }

    fun loginRtm(rtmToken: String, uid: String) {
        RtcManager.INSTANCE.loginRtm(rtmToken, uid) {
            observerSignResult.postValue(ErrorInfo(-1, "Login rtm failed."))
        }
    }

    fun getRoomList() = viewModelScope.launch {
        RxHttp.get(MyConst.ROOM_LIST).toClass<MyConst.AResult<List<MyConst.RoomListItemBean>>>()
            .awaitResult { result ->
                observerRoomListResult.value = if (result.code == 0) result.data else arrayListOf()
            }.onFailure {
                observerSignResult.value = ErrorInfo(-1, it.message)
            }
    }

    fun joinRoom(roomId: String) = viewModelScope.launch {
        RxHttp.postJson(MyConst.JOIN_ROOM)
            .add("roomId", roomId)
            .toClass<MyConst.AResult<MyConst.JoinRoomBean>>().awaitResult { result ->
                if (result.code == 0) {
                    val data = result.data
                    observerJoinSuccess.value = data
                } else {
                    observerJoinFailed.value = ErrorInfo(result.code, result.msg)
                }
            }.onFailure {
                observerJoinFailed.value = ErrorInfo(-1, it.message)
            }
    }

    // type 2 = visitor 1 = streamer
    fun getRoomUserList(
        roomId: String,
        type: Int,
        callback: (List<MyConst.RoomUserList>?) -> Unit
    ) = viewModelScope.launch {
        RxHttp.postJson(MyConst.ROOM_USER_LIST)
            .add("roomId", roomId)
            .add("uType", type).toClass<MyConst.AResult<List<MyConst.RoomUserList>>>()
            .awaitResult {
                callback.invoke(it.data)
            }.onFailure {
                callback.invoke(null)
            }
    }

    fun joinChannel(
        context: Context, rtcToken: String, roomId: String,
        uid: String, nickname: String, avatar: String
    ) {
        RtcManager.INSTANCE.let {
            it.initRtc(context, RtcListener())
            it.enableVideo()
            it.joinChannel(rtcToken, roomId, uid, rtmListener) { err ->
                joinRtmObserver.postValue(err)
                sendMessage("{\"cmd\": \"enterTip\", \"userName\": \"$nickname\", \"avatar\": \"$avatar\"}")
            }
        }
    }

    fun sendMessage(json: String) {
        RtcManager.INSTANCE.sendMessage(json)
    }

    fun requestApplyMike(roomId: String, callback: (List<MyConst.ApplyMikeResult>?) -> Unit) =
        viewModelScope.launch {
            RxHttp.postJson(MyConst.APPLY_MIKE)
                .add("roomId", roomId).toClass<MyConst.AResult<List<MyConst.ApplyMikeResult>>>()
                .awaitResult {
                    callback.invoke(it.data)
                }.onFailure {
                    callback.invoke(null)
                }
        }

    fun sendChannelMsg(msg: String) {
        RtcManager.INSTANCE.sendChannelMsg(msg)
    }

    fun getMemberList(callback: (List<RtmChannelMember>?) -> Unit) {
        RtcManager.INSTANCE.getMemberList {
            viewModelScope.launch { callback.invoke(it) }
        }
    }

    fun setUpTextureView(isLocal: Boolean, textureView: TextureView, uid: String) {
        if (isLocal) {
            RtcManager.INSTANCE.changeRoleToBroadcaster()
            RtcManager.INSTANCE.enableAudio(false)
        }
        RtcManager.INSTANCE.setUpTextureView(isLocal, textureView, uid)
    }

    private var underControlled = false
    private var audioEnabled = false
    fun enableAudio(enable: Boolean, channelCmd: Boolean = false, owner: Boolean = false): Boolean {
        if (channelCmd) {
            underControlled = !enable
            RtcManager.INSTANCE.enableAudio(if (enable) audioEnabled else false)
            if (enable)
                changeLocalAudioState.postValue(audioEnabled)

            return true
        }

        if (underControlled) {
            if (owner) {
                RtcManager.INSTANCE.enableAudio(enable)
                audioEnabled = enable
                return true
            }
            return false
        }

        audioEnabled = enable
        RtcManager.INSTANCE.enableAudio(enable)
        return true
    }

    fun switchCamera() {
        RtcManager.INSTANCE.switchCamera()
    }

    fun setChannelAttributes(channelId: String, list: List<RtmChannelAttribute>) {
        RtcManager.INSTANCE.setChannelAttributes(channelId, list)
    }

    fun getMemberInfo(uid: String, callback: (MyConst.UserInfo?) -> Unit) = viewModelScope.launch {
        RxHttp.postJson(MyConst.GET_USER_INFO)
            .add("uid", uid)
            .toClass<MyConst.UserInfo>().awaitResult {
                callback.invoke(it)
            }.onFailure {
                callback.invoke(null)
            }
    }

    @DelicateCoroutinesApi
    fun leaveRoom(roomId: String, nickname: String, avatar: String) {
        GlobalScope.launch {
            try {
                RxHttp.postJson(MyConst.LEAVE_ROOM).add("roomId", roomId).await<Any>()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sendMessage("{\"cmd\": \"leaveTip\", \"userName\": \"$nickname\", \"avatar\": \"$avatar\"}")
            RtcManager.INSTANCE.clearSelf()
        }
    }

    fun modifyName(newName: String) {
        viewModelScope.launch {
            RxHttp.postJson(MyConst.EDIT_NICKNAME)
                .add("userName", newName)
                .toClass<MyConst.AResult<MyConst.EditNicknameResult>>()
                .awaitResult {
                    observerModifyNameResult.postValue(ErrorInfo(it.code, it.data.userName))
                }.onFailure {
                    observerModifyNameResult.postValue(ErrorInfo(-1, it.message))
                }
        }
    }

    private inner class RtcCallback {
        fun selfLostNetwork(boolean: Boolean) {
            selfLostNetwork.postValue(boolean)
        }

        fun onAttributesUpdated(mute: Boolean) {
            channelMuteAudio.postValue(mute)
            enableAudio(!mute, channelCmd = true)
        }

        fun onMessageReceived(var1: RtmMessage, var2: RtmChannelMember) {
            onRtmMessageReceive.postValue(ChannelMsgData(var2.userId, var1.text))
        }

        fun onMemberChanged() {
            onMemberChange.postValue(Unit)

        }
    }

    private class RtmListener private constructor() : RtmClientListener, RtmChannelListener {

        companion object {
            val INSTANCE by lazy {
                RtmListener()
            }
        }
        private var disconnecting = false
        private var callbackList: MutableList<RtcCallback?> = mutableListOf()

        fun addCallback(cb: RtcCallback) {
            callbackList.add(cb)
        }

        // -- client listener --
        override fun onConnectionStateChanged(var1: Int, var2: Int) {
            when (var1) {
                Constants.CONNECTION_STATE_RECONNECTING -> { // offline
                    if (!disconnecting) {
                        disconnecting = true
                        callbackList.forEach {
                            it?.selfLostNetwork(true)
                        }
                    }
                }
                Constants.CONNECTION_STATE_CONNECTED -> { // reconnected
                    if (disconnecting) {
                        disconnecting = false
                        callbackList.forEach {
                            it?.selfLostNetwork(false)
                        }
                    }
                }
            }
        }

        override fun onMessageReceived(var1: RtmMessage?, var2: String?) {
        }

        override fun onTokenExpired() {
        }

        override fun onPeersOnlineStatusChanged(var1: MutableMap<String, Int>?) {
        }

        // -- channel listener --
        override fun onMemberCountUpdated(var1: Int) {
        }

        override fun onAttributesUpdated(var1: MutableList<RtmChannelAttribute>?) {
            var1 ?: return
            var1.forEach {
                if (it.key == "allAudioState") {
                    val mute = it.value == "1"
                    callbackList.forEach { item ->
                        item?.onAttributesUpdated(mute)
                    }
                }
            }
        }

        override fun onMessageReceived(var1: RtmMessage?, var2: RtmChannelMember?) {
            var1 ?: return
            var2 ?: return
            callbackList.forEach { item ->
                item?.onMessageReceived(var1, var2)
            }
        }

        override fun onMemberJoined(var1: RtmChannelMember?) {
            callbackList.forEach { item ->
                item?.onMemberChanged()
            }
        }

        override fun onMemberLeft(var1: RtmChannelMember?) {
            callbackList.forEach { item ->
                item?.onMemberChanged()
            }
        }
    }

    private inner class RtcListener : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: String?, elapsed: Int) {
            onJoinChannelSuccess.postValue(Unit)
        }

        override fun onUserJoined(uid: String?, elapsed: Int) {
            uid ?: return
            onUserJoined.postValue(uid)
        }

        override fun onUserOffline(uid: String?, reason: Int) {
            uid ?: return
            onUserOffline.postValue(uid)
        }

        override fun onRemoteAudioStateChanged(
            uid: String?,
            state: Int,
            reason: Int,
            elapsed: Int
        ) {
            uid ?: return
            if (reason in 5..6) onRemoteAudioStateChange.postValue(
                AudioStateChangeData(uid, reason == Constants.REMOTE_AUDIO_REASON_REMOTE_MUTED)
            )
        }

        //override fun onRequestToken() {
            //onTokenExperienceTimeout.postValue(Unit)
        //}

        override fun onTokenPrivilegeWillExpire(token: String?) {
            //super.onTokenPrivilegeWillExpire(token)
            onTokenExperienceTimeout.postValue(Unit)
        }
    }

    data class ChannelMsgData(
        val uid: String,
        val json: String
    )

    data class AudioStateChangeData(
        val uid: String,
        val mute: Boolean
    )
}
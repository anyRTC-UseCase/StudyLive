package io.anyrtc.studyroom.utils

import android.view.TextureView
import rxhttp.wrapper.annotation.DefaultDomain

object MyConst {

    @DefaultDomain
    const val BASE_URL = "https://arlive.agrtc.cn/arapi/arlive/v1/studyroom/"

    // network interface
    const val LOGIN = "signIn"
    const val SIGN_UP = "signUp"
    const val ROOM_LIST = "getRoomList"
    const val JOIN_ROOM = "joinRoom"
    const val LEAVE_ROOM = "leaveRoom"
    const val ROOM_USER_LIST = "getSrUserList"
    const val GET_USER_INFO = "getUserInfo"
    const val APPLY_MIKE = "applyMike"
    const val EDIT_NICKNAME = "updateUserName"

    // const variable
    const val USER_ID = "user_id"
    const val USER_NAME = "user_name"
    const val USER_AVATAR = "user_avatar"
    const val HTTP_TOKEN = "http_token"
    const val APP_ID = "app_id"
    const val ROOM_ID = "room_id"
    const val ROOM_NAME = "room_name"
    const val RTC_TOKEN = "rtc_token"
    const val RTM_TOKEN = "rtm_token"

    // pkg
    const val PKG = "org.ar.studyroom"

    data class AResult<T>(
        val code: Int,
        val msg: String,
        val data: T
    )
    // register user result
    data class SignUpResult(
        val code: Int,
        val msg: String,
        val data: SignUpBean
    )
    data class SignUpBean(
        val uid: String,
        val userName: String
    )
    // login result
    data class LoginResult(
        val code: Int,
        val msg: String,
        val data: LoginBean
    )
    data class LoginBean(
        val appid: String,
        val avatar: String,
        val userName: String,
        val userToken: String
    )
    // room list result
    /*data class RoomListResult(
        val code: Int,
        val msg: String,
        val data: RoomListBean
    )*/
    data class RoomListItemBean(
        val avatarStr: String,
        val avatars: List<String>,
        val roomId: String,
        val roomName: String
    )
    // join room result
    /*data class JoinRoomResult(
        val code: Int,
        val msg: String
    )*/
    data class JoinRoomBean(
        val roomId: String,
        val roomName: String,
        val rtcToken: String,
        val rtmToken: String
    )

    // chat data
    data class ChatData(
        val isSelf: Boolean = false,
        val avatar: String = "",
        val nickname: String = "",
        val content: String = "",
        val img: String = "",
        val isStreamer: Boolean = false,
        val streamerNum: Int = 0,
        val isNotify: Boolean = false
    )

    // room member data
    data class RoomMemberInfo(
        var uid: String,
        var nickname: String = "",
        var avatar: String = "",
        var sitting: Boolean = false,
        var seatNum: Int = -1,
        var sittingTime: Long = 0L,
        var textureView: TextureView? = null
    )

    /* get user info */
    data class UserInfo(
        val uid: String,
        val nickName: String,
        val avatar: String
    )
    /* get room user list */
    data class RoomUserList(
        val uid: String,
        val userName: String,
        val avatar: String,
        val seat: Int,
        val seatTime: Int
    )

    /* apply mike */
    data class ApplyMikeResult(
        val avatar: String,
        val seat: Int,
        val seatTime: Int,
        val uid: String,
        val userName: String
    )

    data class EditNicknameResult(
        val uid: String,
        val userName: String
    )
}
package io.anyrtc.studyroom.activity

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import com.kongzue.dialog.v3.CustomDialog
import com.kongzue.dialog.v3.MessageDialog
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.activity.fragment.InputDialogFragment
import io.anyrtc.studyroom.activity.fragment.ListenerListFragment
import io.anyrtc.studyroom.activity.fragment.StreamerListFragment
import io.anyrtc.studyroom.databinding.ActivityLiveBinding
import io.anyrtc.studyroom.databinding.LayoutVideoContentBinding
import io.anyrtc.studyroom.utils.MyConst
import io.anyrtc.studyroom.utils.SpUtil
import io.anyrtc.studyroom.vm.MainVM
import io.anyrtc.studyroom.widget.ChatAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import org.ar.rtc.RtcEngine
import org.ar.rtm.RtmChannelAttribute
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class LiveActivity : BaseActivity() {

    private lateinit var binding: ActivityLiveBinding
    private val vm: MainVM by viewModels()

    private lateinit var mUid: String
    private lateinit var mNickname: String
    private lateinit var mAvatar: String
    private lateinit var mRoomId: String
    private lateinit var mRoomName: String
    private lateinit var mRtcToken: String
    private lateinit var mRtmToken: String

    private var isSitting = false
    private var mSeatNum = -1

    private val roomMembers = mutableListOf<MyConst.RoomMemberInfo>()
    private val roomSittingMembers = mutableListOf<MyConst.RoomMemberInfo>()

    private val streamerListFragment by lazy {
        StreamerListFragment()
    }
    private val listenerListFragment by lazy {
        ListenerListFragment()
    }

    private var firstLoadingDialog: CustomDialog? = null
    private var applyLoadingDialog: CustomDialog? = null
    private var networkLostLoading: CustomDialog? = null

    private val chatAdapter by lazy {
        ChatAdapter()
    }
    private val inputFragmentDialog by lazy {
        InputDialogFragment().also {
            it.textChangeListener = { inputContent ->
                if (inputContent.isNotEmpty()) {
                    binding.visitorChatHints.text = inputContent
                    binding.sittingChatHints.text = inputContent
                } else {
                    binding.visitorChatHints.setText(R.string.saySomething)
                    binding.sittingChatHints.setText(R.string.saySomething)
                }
            }
        }
    }

    private lateinit var mBehavior: BottomSheetBehavior<View>

    private var requestingApply = false
    private var notifyCountingDown = 0
    private var networkLostCountingDown = 0
    //private var experienceTimeCounting = 580
    private val timer by lazy {
        Timer().also { it ->
            it.schedule(object : TimerTask() {
                override fun run() {
                    if (roomSittingMembers.isNotEmpty()) {
                        roomSittingMembers.forEach { item ->
                            if (item.sitting) item.sittingTime += 1L
                        }
                        runOnUiThread { updateTime() }
                    }

                    if (notifyCountingDown > 0) {
                        notifyCountingDown--
                        if (notifyCountingDown == 0 && binding.notifyLinear.childCount > 0) {
                            val removedViewAnim = createTranslationAnim(
                                binding.notifyLinear.getChildAt(0),
                                translationY = false,
                                isLeave = true
                            )

                            removedViewAnim.addListener(countingDownAnim)
                            if (binding.notifyLinear.childCount > 1) runOnUiThread {
                                val oldAnim =
                                    createTranslationAnim(binding.notifyLinear.getChildAt(1))
                                val set = AnimatorSet()
                                set.playTogether(oldAnim, removedViewAnim)
                                set.duration = 230L
                                set.interpolator =
                                    TimeInterpolator { (1.0f - (1.0f - it) * (1.0f - it)) }
                                set.start()
                            } else runOnUiThread {
                                removedViewAnim.start()
                            }
                        }
                    }
                    if (networkLostCountingDown > 0) {
                        networkLostCountingDown--
                        if (networkLostCountingDown == 0) runOnUiThread {
                            Toast.makeText(applicationContext, "网络连接断开", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }

                    /*if (experienceTimeCounting-- <= 0) runOnUiThread {
                        Toast.makeText(applicationContext, "10分钟体验时间已到", Toast.LENGTH_SHORT).show()
                        finish()
                    }*/
                }
            }, 0L, 1000L)
        }
    }
    private val countingDownAnim by lazy {
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                binding.notifyLinear.removeViewAt(0)
                if (binding.notifyLinear.childCount > 0) {
                    binding.notifyLinear.getChildAt(0).post {
                        binding.notifyLinear.getChildAt(0).translationY = 0.0f
                    }
                    notifyCountingDown = 3
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }
        }
    }

    private val handler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                binding.visitorChatSendImg.setOnClickListener(sendImgClick)
            }
        }
    }
    private val sendImgClick: View.OnClickListener = View.OnClickListener {
        // TODO: Send Images
        binding.visitorChatSendImg.setOnClickListener(null)
        handler.sendMessageDelayed(Message().also { it.arg1 = 1 }, 500)

        val imgUrl = imgUrlArr[random.nextInt(imgUrlArr.size)]
        chatAdapter.addData(
            MyConst.ChatData(
                true, mAvatar, mNickname, img = imgUrl,
                isStreamer = isSitting, streamerNum = mSeatNum + 1
            )
        )

        Thread {
            val opt = BitmapFactory.Options()
            opt.inJustDecodeBounds = true

            try {
                val url = URL(imgUrl).openConnection() as HttpURLConnection
                url.connectTimeout = 5000
                url.readTimeout = 5000
                url.requestMethod = "GET"
                val stream = url.inputStream

                BitmapFactory.decodeStream(stream, null, opt)
                stream.close()
                url.disconnect()
                vm.sendMessage("{\"cmd\": \"picMsg\", \"imgUrl\": \"$imgUrl\", \"imageWidth\": ${opt.outWidth}, \"imageHeight\": ${opt.outHeight}, \"avatar\": \"$mAvatar\", \"userName\": \"$mNickname\", \"setNum\": ${mSeatNum + 1}}")
            } catch (e: Exception) {
                Log.e(":::", "Exception: $e")
            }
        }.start()

    }

    private val imgUrlArr = arrayOf(
        "https://oss.agrtc.cn/oss/fdfs/afb630f7f7bee1350d2c396ae8d42d4f.jpg",
        "https://oss.agrtc.cn/oss/fdfs/42702300dd588d94a2fb74906518508f.jpg",
        "https://oss.agrtc.cn/oss/fdfs/ad470a15f3d3099fbb4084d0a2221538.jpg",
        "https://oss.agrtc.cn/oss/fdfs/7d6f6dae0438c2a45378e7c1da75a128.jpg",
        "https://oss.agrtc.cn/oss/fdfs/817536c89991d5114cd7464b73d568a6.jpg",
        "https://oss.agrtc.cn/oss/fdfs/72cb6cdc06d46a4cb7764c580f7495aa.jpg",
        "https://oss.agrtc.cn/oss/fdfs/89cb70f3f83977793b16dc126a48385e.jpg",
        "https://oss.agrtc.cn/oss/fdfs/211e80c98003e284bc5a67a01c5d393d.jpg",
        "https://oss.agrtc.cn/oss/fdfs/ddfa78d5cca8c6356f3ef0d99624d1a9.jpg",
        "https://oss.agrtc.cn/oss/fdfs/b5c1ad5b754c2e889b8fc91bc92a007f.jpg",
        "https://oss.agrtc.cn/oss/fdfs/0fd53a3cd0d7385f54d3c2f16572df5b.jpg",
        "https://oss.agrtc.cn/oss/fdfs/a465359c553c079e368d1186084e338f.jpg",
        "https://oss.agrtc.cn/oss/fdfs/5e300c45d57078658935c3494a87b000.jpg",
        "https://oss.agrtc.cn/oss/fdfs/9d77d556fb4bf79c77ff371c20e5bca3.jpg",
        "https://oss.agrtc.cn/oss/fdfs/494a8224da62324882579df179478c61.jpg",
        "https://oss.agrtc.cn/oss/fdfs/f37de6fc1f2b97dc9b77ef5c7e9e12d5.jpg",
        "https://oss.agrtc.cn/oss/fdfs/f5affe29c5c9cc334a55ed0aa30558d9.jpg",
        "https://oss.agrtc.cn/oss/fdfs/820894ae4d7680b05499e95e3c424ccb.jpg",
        "https://oss.agrtc.cn/oss/fdfs/bd86fe34fa8821b919110ac9fbae2378.jpg",
        "https://oss.agrtc.cn/oss/fdfs/096c94e6346ce1a3b5dfa7e801b957a9.jpg"
    )
    private val random by lazy {
        Random()
    }

    override fun statusBarColor(): Int {
        return R.color.black
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.run {
            mRoomId = getStringExtra(MyConst.ROOM_ID)!!
            mRoomName = getStringExtra(MyConst.ROOM_NAME)!!
            mRtcToken = getStringExtra(MyConst.RTC_TOKEN)!!
            mRtmToken = getStringExtra(MyConst.RTM_TOKEN)!!
            SpUtil.get().run {
                mUid = getString(MyConst.USER_ID, "").toString()
                mNickname = getString(MyConst.USER_NAME, "").toString()
                mAvatar = getString(MyConst.USER_AVATAR, "").toString()
            }
        }

        firstLoadingDialog =
            CustomDialog.build(this as AppCompatActivity, R.layout.layout_loading) { _, _ -> }
                .setCancelable(false).setFullScreen(true)
        firstLoadingDialog!!.show()
        initRtc()
        initWidget()
    }

    private fun initRtc() {
        vm.joinChannel(this, mRtcToken, mRoomId, mUid, mNickname, mAvatar)
        timer.hashCode() // start

        vm.joinRtmObserver.observe(this) { loginFailed ->
            if (loginFailed) return@observe

            vm.getMemberList {
                if (it != null && it.isNotEmpty()) it.forEach { member ->
                    if (member.userId == mUid)
                        roomMembers.add(MyConst.RoomMemberInfo(mUid, mNickname, mAvatar))
                    else
                        roomMembers.add(MyConst.RoomMemberInfo(member.userId))
                }
                getStreamerList()
                getListenerList()
            }
        }
        vm.onUserJoined.observe(this) {
            getStreamerList()
            getListenerList()
        }
        vm.onUserOffline.observe(this) {
            getStreamerList()
            /*rearrangeSeat(
                roomSittingMembers
                    .filter { item -> uid != item.uid }
                    .sortedBy { it.seatNum }
                    .toTypedArray()
            )*/
        }
        vm.onMemberChange.observe(this) {
            getListenerList()
        }
        vm.onRtmMessageReceive.observe(this) {
            if (it.uid.isBlank())
                return@observe

            val jsonObj = JSONObject(it.json)
            when (val cmd = jsonObj.getString("cmd")) {
                "enterTip", "leaveTip" -> {
                    val nickname = jsonObj.getString("userName")
                    val avatar = jsonObj.getString("avatar")
                    addRoomNotify(nickname, avatar, cmd == "enterTip")
                }
                "hostTip" -> {
                    val nickname = jsonObj.getString("userName")
                    chatAdapter.addData(
                        MyConst.ChatData(
                            content = "${nickname}成为主持人", isNotify = true
                        )
                    )
                }
                "msg" -> {
                    val content = jsonObj.getString("content")
                    val nickname = jsonObj.getString("userName")
                    val avatar = jsonObj.getString("avatar")
                    val setNum = jsonObj.getInt("setNum")
                    chatAdapter.addData(
                        MyConst.ChatData(
                            false, avatar, nickname, content,
                            isStreamer = setNum != 0, streamerNum = setNum
                        )
                    )
                }
                "seatChange" -> {
                    val contentStr = jsonObj.getString("data")
                    val dataArr = JSONArray(contentStr.replace("\\", ""))
                    /*if (dataArr.length() == 0) {
                        Toast.makeText(this@LiveActivity, "所有直播者已离开", Toast.LENGTH_SHORT).show()
                        finish()
                        return@observe
                    }*/

                    val newSeat = arrayOfNulls<MyConst.RoomMemberInfo>(dataArr.length())
                    for (i in 0 until dataArr.length()) {
                        val item = dataArr.getJSONObject(i)
                        val uid = item.getString("uid")
                        val nickname = item.getString("userName")
                        val avatar = item.getString("avatar")
                        val seatNum = item.getInt("seat") - 1
                        val seatTime = item.getInt("seatTime")
                        newSeat[i] = MyConst.RoomMemberInfo(
                            uid, nickname, avatar, true, seatNum = seatNum, sittingTime = (System.currentTimeMillis() / 1000) - seatTime
                        )
                    }
                    streamerListFragment.refreshData(newSeat.filterNotNull().map { mapItem ->
                        MyConst.RoomUserList(
                            mapItem.uid, mapItem.nickname, mapItem.avatar,
                            mapItem.seatNum, mapItem.sittingTime.toInt()
                        )
                    })
                    rearrangeSeat(newSeat)
                }
                "picMsg" -> {
                    val imgUrl = jsonObj.getString("imgUrl")
                    val nickname = jsonObj.getString("userName")
                    val avatar = jsonObj.getString("avatar")
                    val seatNum = jsonObj.getInt("setNum") - 1

                    chatAdapter.addData(
                        MyConst.ChatData(
                            avatar = avatar, nickname = nickname,
                            img = imgUrl, isStreamer = seatNum >= 0,
                            streamerNum = seatNum
                        )
                    )
                }
            }
        }
        vm.channelMuteAudio.observe(this) { mute ->
            chatAdapter.addData(
                MyConst.ChatData(
                    false, content = if (mute) "主持人开启了全体静音" else "主持人解除了全体静音", isNotify = true
                )
            )
            if (mute && !binding.sittingMike.isChecked) {
                binding.sittingMike.isChecked = true
            }
            binding.sittingMuteAll.isChecked = mute

            if (mSeatNum != 0 && mute) {
                binding.sittingMike.setButtonDrawable(R.drawable.channel_mike_disabled)
                if (isSitting)
                    videoParentViews[mSeatNum].mikeStatus.setImageResource(R.drawable.mike_disable)
            } else if (mSeatNum == 0 || !mute) {
                binding.sittingMike.setButtonDrawable(R.drawable.selector_mike)
                if (!mute && isSitting)
                    videoParentViews[mSeatNum] .mikeStatus.setImageResource(R.drawable.mike_enable)
            }
        }
        vm.changeLocalAudioState.observe(this) { enable ->
            if (mSeatNum >= 0) {
                videoParentViews[mSeatNum].mikeStatus.setImageResource(
                    if (enable) R.drawable.mike_enable else R.drawable.mike_disable
                )
                binding.sittingMike.isChecked = !enable
            }
        }
        vm.onRemoteAudioStateChange.observe(this) { stateData ->
            if (roomSittingMembers.isNotEmpty()) roomSittingMembers.forEach { item ->
                if (item.uid == stateData.uid) {
                    videoParentViews[item.seatNum].mikeStatus.setImageResource(
                        if (stateData.mute) R.drawable.mike_disable else R.drawable.mike_enable
                    )
                    return@observe
                }
            }
        }
        vm.selfLostNetwork.observe(this) { bool ->
            if (bool) {
                if (networkLostCountingDown <= 0) {
                    networkLostCountingDown = 10
                    networkLostLoading = CustomDialog.build(
                        this@LiveActivity as AppCompatActivity,
                        R.layout.layout_loading
                    ) { _, v ->
                        val tip = v.findViewById<TextView>(R.id.tv_tip)
                        tip.text = "网络中断"
                    }.setCancelable(false).setFullScreen(true)
                    networkLostLoading!!.show()
                }
            } else {
                networkLostLoading?.doDismiss()
                networkLostLoading = null
                networkLostCountingDown = 0
            }
        }
        vm.onTokenExperienceTimeout.observe(this) {
            Toast.makeText(applicationContext, "10分钟体验时间已到", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initWidget() = binding.run {
        mBehavior = BottomSheetBehavior.from(binding.bottomSheetParent)
        mBehavior.setPeekHeight(0, false)
        title.text = mRoomName
        chatRecycle.layoutManager = LinearLayoutManager(
            this@LiveActivity, LinearLayoutManager.VERTICAL, false
        )
        chatRecycle.adapter = chatAdapter

        val showInputClick = View.OnClickListener {
            inputFragmentDialog.show(supportFragmentManager) { msg ->
                vm.sendChannelMsg("{\"cmd\": \"msg\", \"content\": \"$msg\", \"userName\": \"$mNickname\", \"avatar\": \"$mAvatar\", \"setNum\": ${mSeatNum + 1}}")
                chatAdapter.addData(
                    MyConst.ChatData(
                        true,
                        mAvatar,
                        mNickname,
                        msg,
                        isStreamer = isSitting,
                        streamerNum = mSeatNum + 1
                    )
                )
            }
        }
        visitorChatInputBg.setOnClickListener(showInputClick)
        sittingChatInputBg.setOnClickListener(showInputClick)
        visitorChatSendImg.setOnClickListener(sendImgClick)
        sittingChatSendImg.setOnClickListener(sendImgClick)

        val applyMikeClick = View.OnClickListener {
            if (requestingApply) {
                return@OnClickListener
            }
            requestingApply = true

            if (isSitting)
                return@OnClickListener
            if (roomSittingMembers.size >= 4) {
                Toast.makeText(this@LiveActivity, "麦位已满，请稍后重试", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            applyLoadingDialog = CustomDialog.build(
                this@LiveActivity as AppCompatActivity,
                R.layout.layout_loading
            ) { _, _ -> }
                .setCancelable(false).setFullScreen(true)
            applyLoadingDialog!!.show()
            startVideo()
        }

        visitorParticipate.setOnClickListener(applyMikeClick)
        val videoClick = View.OnClickListener {
            videosLayout.toTopicMode(it.tag as Int)
        }
        videoParentViews = arrayOf(video1, video2, video3, video4).apply {
            forEachIndexed { index, layoutVideoContentBinding ->
                layoutVideoContentBinding.videoParent.tag = index
                layoutVideoContentBinding.videoParent.setOnClickListener(videoClick)
                layoutVideoContentBinding.placeholderBg.setOnClickListener(applyMikeClick)
            }
        }

        sittingMike.setOnClickListener {
            val changeSuccess = vm.enableAudio(!sittingMike.isChecked, owner = mSeatNum == 0)
            if (!changeSuccess) {
                Toast.makeText(this@LiveActivity, "全体静音中", Toast.LENGTH_SHORT).show()
                sittingMike.isChecked = !sittingMike.isChecked
                return@setOnClickListener
            }
            videoParentViews[mSeatNum].mikeStatus.setImageResource(
                if (sittingMike.isChecked) R.drawable.mike_disable else R.drawable.mike_enable
            )
        }
        sittingMuteAll.setOnClickListener {
            if (mSeatNum != 0) {
                Toast.makeText(this@LiveActivity, "全体禁言为1号主持人特权", Toast.LENGTH_SHORT).show()
                sittingMuteAll.isChecked = false
                return@setOnClickListener
            }

            vm.setChannelAttributes(
                mRoomId, listOf(
                    RtmChannelAttribute(
                        "allAudioState", if (sittingMuteAll.isChecked) "1" else "0"
                    )
                )
            )
            videoParentViews[mSeatNum].mikeStatus.setImageResource(R.drawable.mike_disable)
            Toast.makeText(
                this@LiveActivity,
                if (sittingMuteAll.isChecked) "已开启全体静音" else "已关闭全体静音",
                Toast.LENGTH_SHORT
            ).show()
        }
        sittingSwitchCamera.setOnClickListener {
            vm.switchCamera()
        }

        val exitClick = View.OnClickListener {
            if (it == sittingExit)
                onBackPressed()
            else
                finish()
        }
        sittingExit.setOnClickListener(exitClick)
        visitorLeave.setOnClickListener(exitClick)

        chatAdapter.addData(
            MyConst.ChatData(
                content = "系统：严禁传播违法违规、低俗色情、血腥暴力、造谣诈骗等不良信息。欢迎同学监督不良行为，净化学习环境，营造绿色自习室！",
                isNotify = true
            )
        )

        binding.list6.setOnClickListener {
            mBehavior.state = if (mBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                BottomSheetBehavior.STATE_EXPANDED
            else
                BottomSheetBehavior.STATE_COLLAPSED
        }

        pager.adapter = object : FragmentStateAdapter(this@LiveActivity) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int): Fragment {
                return if (position == 0)
                    streamerListFragment
                else
                    listenerListFragment
            }
        }

        val tabArr = arrayOf("自习室成员", "观众")
        TabLayoutMediator(tabLayout, pager) { tab, position ->
            tab.text = tabArr[position]
        }.attach()
    }

    private fun getStreamerList() {
        vm.getRoomUserList(mRoomId, 1) { roomList ->
            if (roomList == null) {
                firstLoadingDialog?.doDismiss()
                firstLoadingDialog = null
                Toast.makeText(applicationContext, "请检查网络", Toast.LENGTH_SHORT).show()
                finish()
                return@getRoomUserList
            }
            //if (roomList.isEmpty()) startVideo()
            rearrangeSeat(
                roomList.sortedBy { sort -> sort.seat }
                    .map {
                        /*if (it.uid == mUid) {
                            this.mSeatNum = it.seat - 1
                        }*/
                        MyConst.RoomMemberInfo(
                            it.uid, it.userName, it.avatar, it.seat > 0,
                            it.seat - 1, (System.currentTimeMillis() / 1000) - it.seatTime.toLong()
                        )
                    }.toTypedArray()
            )
            streamerListFragment.refreshData(roomList.sortedBy { it.seat })

            firstLoadingDialog?.doDismiss()
            firstLoadingDialog = null
        }
    }

    private val listenerImgArr by lazy {
        arrayOf(binding.img1, binding.img2, binding.img3)
    }

    private fun getListenerList() {
        vm.getRoomUserList(mRoomId, 2) { roomList ->
            if (roomList.isNullOrEmpty()) {
                listenerImgArr.forEach { it.load("") }
                binding.watchingTitle.text = "0个观众"
                listenerListFragment.refreshData(roomList ?: mutableListOf())
                return@getRoomUserList
            }

            for (i in 0 until 3) {
                val avatar = if (roomList.size - 1 >= i) roomList[i].avatar else ""
                listenerImgArr[2 - i].load(avatar)
            }
            listenerListFragment.refreshData(roomList)
            binding.watchingTitle.text = String.format("%s个观众", roomList.size)
        }
    }

    private val dp7 by lazy {
        resources.getDimensionPixelOffset(R.dimen.dp7)
    }
    private val dp15 by lazy {
        resources.getDimensionPixelOffset(R.dimen.dp15)
    }

    private fun rearrangeSeat(newSeat: Array<MyConst.RoomMemberInfo?>) {
        binding.visitorParticipate.let {
            val fulled = newSeat.size == 4
            if (fulled) {
                it.text = "座位已满"
                it.setTextColor(Color.parseColor("#757575"))
                it.setBackgroundResource(R.drawable.shape_gray_bg)
                it.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                it.setPadding(dp15, 0, dp15, 0)
            } else {
                it.text = "加入"
                it.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
                it.setBackgroundResource(R.drawable.shape_red_bg)
                it.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_participate, 0, 0, 0)
                it.setPadding(dp7, 0, dp15, 0)
            }
        }
        /*var startIndex = 0
        for (i in newSeat.indices) {
            val roomMemberInfo = newSeat[i]
            if (
                i >= roomSittingMembers.size - 1 ||
                roomMemberInfo!!.uid != roomSittingMembers[i].uid ||
                roomMemberInfo.textureView == null ||
                videoParentViews[i].videoParent.childCount == 0
            ) {
                startIndex = i
                break
            }
        }*/
        val seatSet = newSeat.map { it!!.seatNum }

        for (i in 0 until 4) {
            if (i <= newSeat.size - 1) {
                val newData = newSeat[i]!!
                if (newData.uid == mUid) {
                    if (mSeatNum != newData.seatNum && newData.seatNum == 0) {
                        vm.sendMessage("{\"cmd\": \"hostTip\", \"userName\": \"$mNickname\"}")
                        chatAdapter.addData(
                            MyConst.ChatData(
                                false,
                                content = "${mNickname}成为主持人",
                                isNotify = true
                            )
                        )
                    }
                    isSitting = true
                    mSeatNum = newData.seatNum
                    binding.sittingMuteAll.visibility =
                        if (this.mSeatNum == 0) View.VISIBLE else View.GONE
                    if (this.mSeatNum == 0)
                        binding.sittingMike.setButtonDrawable(R.drawable.selector_mike)
                }

                lateinit var tv: TextureView
                if (i > roomSittingMembers.size - 1) {
                    tv = createTextureView(newData.uid, newData.uid == mUid)
                    roomSittingMembers.add(newData.also { it.textureView = tv })
                    if (newData.uid == mUid) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        binding.visitorChatGroup.visibility = View.GONE
                        binding.sittingChatGroup.visibility = View.VISIBLE
                    }
                } else {
                    var find: MyConst.RoomMemberInfo? = null
                    roomSittingMembers.forEach { item ->
                        if (item.uid == newData.uid) {
                            find = item
                            return@forEach
                        }
                    }
                    if (find != null && find!!.seatNum == newData.seatNum) {
                        find?.sittingTime = newData.sittingTime
                        continue
                    }

                    tv = if (find != null) {
                        if (find!!.textureView == null)
                            createTextureView(find!!.uid, find!!.uid == mUid)
                        else
                            find!!.textureView!!
                    } else {
                        createTextureView(newData.uid)
                    }

                    if (find != null) find!!.run {
                        uid = newData.uid
                        nickname = newData.nickname
                        avatar = newData.avatar
                        sitting = newData.sitting
                        seatNum = newData.seatNum
                        sittingTime = newData.sittingTime
                        textureView = tv
                    } else {
                        roomSittingMembers.add(newData.also { it.textureView = tv })
                    }
                }

                videoParentViews[newData.seatNum].run {
                    videoParent.removeAllViews()
                    if (tv.parent != null) (tv.parent as ViewGroup).removeAllViews()
                    videoParent.addView(tv)
                    emptyGroup.visibility = View.GONE
                    videoGroup.visibility = View.VISIBLE
                    num.text = (newData.seatNum + 1).toString()
                    avatar.load(newData.avatar)
                }
            }
        }

        if (seatSet.size < 4) {
            for (i in 0 until 4) if (!seatSet.contains(i)) {
                videoParentViews[i].run {
                    videoParent.removeAllViews()
                    emptyGroup.visibility = View.VISIBLE
                    videoGroup.visibility = View.GONE
                }
            }
        }

        val iterator = roomSittingMembers.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            var findIndex = -1
            for (j in newSeat.indices) {
                val seatItem = newSeat[j]
                if (seatItem?.uid ?: "" == next.uid) {
                    findIndex = j
                    break
                }
            }

            if (findIndex == -1)
                iterator.remove()
        }
    }

    private lateinit var videoParentViews: Array<LayoutVideoContentBinding>
    private fun updateTime() {
        if (roomSittingMembers.isEmpty())
            return

        for (i in 0 until roomSittingMembers.size) {
            val item = roomSittingMembers[i]
            if (!item.sitting)
                break

            val hours = item.sittingTime / 3600
            val minutes = (item.sittingTime - hours * 3600) / 60
            val seconds = item.sittingTime % 60
            videoParentViews[item.seatNum].counting.text = String.format(
                "%02d:%02d:%02d",
                hours, minutes, seconds
            )
        }
    }

    private fun createTextureView(uid: String, isLocal: Boolean = false): TextureView {
        val textureView = RtcEngine.CreateRendererView(this)
        vm.setUpTextureView(isLocal, textureView, uid)
        return textureView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.videosLayout.post {
            binding.videosLayout.refreshOrientation()
        }
    }

    /**
     * turn on the camera
     */
    private fun startVideo() {
        vm.requestApplyMike(mRoomId) {
            if (null == it) {
                Toast.makeText(this@LiveActivity, "上麦失败，请重试", Toast.LENGTH_SHORT).show()
                requestingApply = false
                applyLoadingDialog?.doDismiss()
                applyLoadingDialog = null
                return@requestApplyMike
            }
            requestingApply = false
            applyLoadingDialog?.doDismiss()
            applyLoadingDialog = null

            val seatNum = it.find { item -> item.uid == mUid }!!.seat - 1
            var removeIndex = 0
            roomMembers.forEachIndexed { index, roomMemberInfo ->
                if (roomMemberInfo.uid == mUid) {
                    removeIndex = index
                    return@forEachIndexed
                }
            }

            val textureView = createTextureView(mUid, true)
            val removedItem = roomMembers.removeAt(removeIndex)
            removedItem.let { info ->
                info.sitting = true
                info.textureView = textureView
                info.seatNum = seatNum
            }
            sitDown(seatNum, textureView)
            //binding.list6.gravity = Gravity.END or Gravity.CENTER_VERTICAL

            isSitting = true
            this.mSeatNum = seatNum
            binding.sittingMuteAll.visibility = if (seatNum == 0) View.VISIBLE else View.GONE

            roomSittingMembers.add(removedItem)
        }
    }

    private fun sitDown(seatNum: Int, textureView: TextureView) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        binding.run {
            videoParentViews[seatNum].run {
                videoParent.addView(textureView)
                emptyGroup.visibility = View.GONE
                videoGroup.visibility = View.VISIBLE
                num.text = (seatNum + 1).toString()
                avatar.load(mAvatar)
            }
            visitorChatGroup.visibility = View.GONE
            sittingChatGroup.visibility = View.VISIBLE
        }
        getListenerList()
        getStreamerList()
    }

    @SuppressLint("InflateParams")
    private fun addRoomNotify(nickname: String, avatar: String, isEnter: Boolean = false) =
        binding.run {
            val newNotify = layoutInflater.inflate(R.layout.item_notify_layout, null)
            newNotify.visibility = View.INVISIBLE
            notifyCountingDown = 3

            newNotify.findViewById<AppCompatImageView>(R.id.notify_avatar).load(avatar)
            val nicknameView = newNotify.findViewById<TextView>(R.id.notify_nickname)
            val notifyStatus = newNotify.findViewById<TextView>(R.id.notify_status)

            nicknameView.text = nickname
            notifyStatus.setText(if (isEnter) R.string.joinRoom else R.string.leaveRoom)

            if (notifyLinear.childCount >= 2) {
                val removedViewAnim = createTranslationAnim(notifyLinear.getChildAt(0))
                val oldViewAnim = createTranslationAnim(notifyLinear.getChildAt(1))
                removedViewAnim.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        notifyLinear.removeViewAt(0)
                        notifyLinear.getChildAt(0).post {
                            notifyLinear.getChildAt(0).translationY = 0.0f
                        }

                        notifyLinear.addView(newNotify)
                        newNotify.post {
                            val newViewAnim = createTranslationAnim(
                                newNotify, translationY = false, isLeave = false
                            )
                            newViewAnim.start()
                        }
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationRepeat(animation: Animator?) {
                    }
                })
                val animSet = AnimatorSet()
                animSet.playTogether(removedViewAnim, oldViewAnim)
                animSet.duration = 230L
                animSet.start()
            } else {
                notifyLinear.addView(newNotify)
                newNotify.post {
                    val newViewAnim = createTranslationAnim(
                        newNotify, translationY = false, isLeave = false
                    )
                    /*if (notifyLinear.childCount > 1) {
                        val oldViewAnim = createTranslationAnim(notifyLinear.getChildAt(0))
                        val animSet = AnimatorSet()
                        animSet.playTogether(oldViewAnim, newViewAnim)
                        animSet.duration = 230L
                        animSet.start()
                    } else {
                    }*/
                    newViewAnim.start()
                }
            }
        }

    override fun onBackPressed() {
        if (isSitting) MessageDialog.show(this, "退出自习室", "是否退出").setOkButton("确认")
            .setCancelButton("取消")
            .setOnOkButtonClickListener { _, _ ->
                super.onBackPressed()
                true
            }
        else super.onBackPressed()
    }

    private fun createTranslationAnim(
        view: View,
        translationY: Boolean = true,
        isLeave: Boolean = false
    ): ObjectAnimator {
        val ofFloat = ObjectAnimator.ofFloat(
            view,
            if (translationY) "translationY" else "translationX",
            if (translationY || isLeave) 0.0f else view.measuredWidth.toFloat(),
            if (translationY) -view.measuredHeight.toFloat() else if (isLeave) view.measuredWidth.toFloat() else 0.0f
        )
        ofFloat.interpolator = TimeInterpolator { (1.0f - (1.0f - it) * (1.0f - it)) }
        ofFloat.duration = 230L
        view.visibility = View.VISIBLE
        return ofFloat
    }

    @DelicateCoroutinesApi
    override fun onDestroy() {
        timer.cancel()
        timer.purge()
        vm.leaveRoom(mRoomId, mNickname, mAvatar)
        super.onDestroy()
    }
}
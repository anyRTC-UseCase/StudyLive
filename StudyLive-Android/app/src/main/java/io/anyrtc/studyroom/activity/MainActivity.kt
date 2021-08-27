package io.anyrtc.studyroom.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.activity.fragment.HomeFragment
import io.anyrtc.studyroom.activity.fragment.MineFragment
import io.anyrtc.studyroom.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private  val fragments by lazy {
        arrayListOf(HomeFragment(), MineFragment())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        replaceFragment(fragments[0])
        binding.bottomView.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.rb_home->{
                    replaceFragment(fragments[0])
                }
                R.id.rb_mine->{
                    replaceFragment(fragments[1])
                }
            }
        }
    }

    private fun replaceFragment(replaceFragment: Fragment, id: Int = R.id.frameLayout) {
        val tag = replaceFragment::class.java.name
        var tempFragment = supportFragmentManager.findFragmentByTag(tag)
        val transaction = supportFragmentManager.beginTransaction()
        if (tempFragment == null) {
            try {
                tempFragment = replaceFragment
                transaction
                    .add(id, tempFragment, tag)
                    .setMaxLifecycle(tempFragment, Lifecycle.State.RESUMED)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val fragments = supportFragmentManager.fragments

        for (i in fragments.indices) {
            val fragment = fragments[i]
            if (fragment.tag == tag) {
                transaction
                    .show(fragment)
            } else {
                transaction
                    .hide(fragment)
            }
        }
        transaction.commitAllowingStateLoss()
    }

    /*private lateinit var binding: ActivityMainBinding
    private val vm: MainVM by viewModels()
    private var uid = ""

    private val roomAdapter by lazy {
        HomeGridAdapter()
    }

    private var loadingDialog: CustomDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initObserve()
        initWidget()
    }

    private fun initWidget() {
        binding.run {
            recycle.layoutManager = GridLayoutManager(root.context, 2, GridLayoutManager.VERTICAL, false)
            recycle.adapter = roomAdapter

            smartRefresh.setOnRefreshListener {
                if (it.isRefreshing) {
                    vm.getRoomList()
                }
            }

            roomAdapter.setOnItemClickListener { _, _, position ->
                loadingDialog = CustomDialog.build(this@MainActivity, R.layout.layout_loading) { _, _ -> }
                        .setCancelable(false).setFullScreen(true)
                loadingDialog!!.show()
                val data = roomAdapter.data[position]
                vm.joinRoom(data.roomId)
            }
        }
    }

    private fun initObserve() {
        vm.observerSignResult.observe(this) {
            if (binding.smartRefresh.isRefreshing) {
                binding.smartRefresh.finishRefresh()
            }
            if (it.errorCode == -1) {
                binding.internetLostGroup.visibility = View.VISIBLE
                Toast.makeText(this, it.errorDescription, Toast.LENGTH_SHORT).show()
                return@observe
            }
            if (it.errorCode == 0 && !it.errorDescription.isNullOrBlank()) {
                uid = it.errorDescription
            }
            binding.internetLostGroup.visibility = View.GONE

            vm.getRoomList()
        }
        vm.observerRoomListResult.observe(this) {
            if (binding.internetLostGroup.visibility == View.VISIBLE)
                binding.internetLostGroup.visibility = View.GONE

            roomAdapter.data.clear()
            roomAdapter.addData(it)
            if (binding.smartRefresh.isRefreshing) {
                binding.smartRefresh.finishRefresh()
            }
        }

        vm.observerJoinFailed.observe(this) {
            loadingDialog?.doDismiss()
            binding.smartRefresh.setStateRefresh(true)
            TipDialog.show(this, "加入房间失败，请检查网络", TipDialog.TYPE.WARNING)
        }
        vm.observerJoinSuccess.observe(this) {
            loadingDialog?.doDismiss()
            val intent = Intent(this@MainActivity, LiveActivity::class.java)
            intent.run {
                putExtra(MyConst.RTC_TOKEN, it.rtcToken)
                putExtra(MyConst.RTM_TOKEN, it.rtmToken)
                putExtra(MyConst.ROOM_NAME, it.roomName)
                putExtra(MyConst.ROOM_ID, it.roomId)
            }
            vm.loginRtm(it.rtmToken, uid)
            startActivity(intent)
        }

        vm.login()
    }

    override fun onResume() {
        super.onResume()
        binding.smartRefresh.setStateRefresh(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        RtcManager.INSTANCE.clearSelf()
    }*/
}
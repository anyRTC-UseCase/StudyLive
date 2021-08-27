package io.anyrtc.studyroom.activity.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.kongzue.dialog.v3.CustomDialog
import com.kongzue.dialog.v3.TipDialog
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.activity.LiveActivity
import io.anyrtc.studyroom.databinding.FragmentMainBinding
import io.anyrtc.studyroom.utils.MyConst
import io.anyrtc.studyroom.vm.MainVM
import io.anyrtc.studyroom.widget.HomeGridAdapter

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private val vm: MainVM by viewModels()
    private var uid = ""

    private val roomAdapter by lazy {
        HomeGridAdapter()
    }

    private var loadingDialog: CustomDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                loadingDialog = CustomDialog.build(activity as AppCompatActivity, R.layout.layout_loading) { _, _ -> }
                    .setCancelable(false).setFullScreen(true)
                loadingDialog!!.show()
                val data = roomAdapter.data[position]
                vm.joinRoom(data.roomId)
            }
        }
    }

    private fun initObserve() {
        vm.observerSignResult.observe(requireActivity()) {
            if (binding.smartRefresh.isRefreshing) {
                binding.smartRefresh.finishRefresh()
            }
            if (it.errorCode == -1) {
                binding.internetLostGroup.visibility = View.VISIBLE
                binding.recycle.visibility = View.GONE
                //Toast.makeText(requireContext(), it.errorDescription, Toast.LENGTH_SHORT).show()
                return@observe
            }
            if (it.errorCode == 0 && !it.errorDescription.isNullOrBlank()) {
                uid = it.errorDescription
            }
            binding.internetLostGroup.visibility = View.GONE

            vm.getRoomList()
        }
        vm.observerRoomListResult.observe(requireActivity()) {
            if (binding.internetLostGroup.visibility == View.VISIBLE) {
                binding.internetLostGroup.visibility = View.GONE
                binding.recycle.visibility = View.VISIBLE
            }

            roomAdapter.data.clear()
            roomAdapter.addData(it)
            if (binding.smartRefresh.isRefreshing) {
                binding.smartRefresh.finishRefresh()
            }
            vm.login()
        }

        vm.observerJoinFailed.observe(requireActivity()) {
            loadingDialog?.doDismiss()
            binding.smartRefresh.setStateRefresh(true)
            TipDialog.show(requireActivity() as AppCompatActivity, "加入房间失败，请检查网络", TipDialog.TYPE.WARNING)
        }
        vm.observerJoinSuccess.observe(requireActivity()) {
            loadingDialog?.doDismiss()
            val intent = Intent(requireContext(), LiveActivity::class.java)
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

    override fun onStart() {
        super.onStart()
        binding.smartRefresh.setStateRefresh(true)
    }
}
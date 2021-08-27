package io.anyrtc.studyroom.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.anyrtc.studyroom.databinding.FragmentListBinding
import io.anyrtc.studyroom.utils.MyConst
import io.anyrtc.studyroom.widget.UserInfoAdapter

class StreamerListFragment : Fragment() {

    private lateinit var binding: FragmentListBinding

    private val adapter by lazy {
        UserInfoAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recycle.layoutManager = LinearLayoutManager(
            view.context, LinearLayoutManager.VERTICAL, false
        )
        binding.recycle.adapter = adapter
    }

    fun refreshData(data: List<MyConst.RoomUserList>) {
        adapter.data.clear()
        adapter.addData(data)
    }
}
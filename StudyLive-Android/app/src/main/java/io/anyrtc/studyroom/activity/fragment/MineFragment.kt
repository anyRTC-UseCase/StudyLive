package io.anyrtc.studyroom.activity.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil.load
import com.kongzue.dialog.v3.MessageDialog
import com.kongzue.dialog.v3.TipDialog
import com.kongzue.dialog.v3.WaitDialog
import io.anyrtc.studyroom.BuildConfig
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.activity.DisclaimerActivity
import io.anyrtc.studyroom.databinding.FragmentMineBinding
import io.anyrtc.studyroom.utils.MyConst
import io.anyrtc.studyroom.utils.SpUtil
import io.anyrtc.studyroom.vm.MainVM
import org.ar.rtc.RtcEngine

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!
    private var etName: EditText? = null
    private val vm: MainVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivIcon.load(SpUtil.get().getString(MyConst.USER_AVATAR, ""))
        binding.tvName.text = SpUtil.get().getString(MyConst.USER_NAME, "")

        binding.tvYinsi.setOnClickListener {
            goH5("https://anyrtc.io/termsOfService")
        }
        binding.tvRegister.setOnClickListener {
            goH5("https://console.anyrtc.io/signup")
        }
        binding.tvMianze.setOnClickListener {
            //goH5("https://www.anyrtc.io/termsOfService")
            startActivity(Intent(requireContext(), DisclaimerActivity::class.java))
            requireActivity().overridePendingTransition(0, R.anim.picture_anim_fade_in)
        }

        binding.tvSdkVersion.text = String.format("v %s", RtcEngine.getSdkVersion())
        binding.tvAppVersion.text = String.format("v %s", BuildConfig.VERSION_NAME)
        //binding.tvPubTime.text = activity?.packageManager?.getApplicationInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA)!!.metaData["releaseTime"].toString()

        binding.editNickname.setOnClickListener {
            showModifyNameDialog(binding.tvName.text.toString())
        }
        vm.observerModifyNameResult.observe(requireActivity()) {
            if (it.errorCode == 0) {
                binding.tvName.text = it.errorDescription
                SpUtil.edit { editor ->
                    editor.putString(MyConst.USER_NAME, it.errorDescription)
                }
                TipDialog.show(requireActivity() as AppCompatActivity, "修改成功", TipDialog.TYPE.SUCCESS)
            } else {
                TipDialog.show(requireActivity() as AppCompatActivity, it.errorDescription, TipDialog.TYPE.ERROR)
            }
        }

    }

    private fun goH5(url: String) {
        startActivity(Intent().apply {
            action = "android.intent.action.VIEW"
            data = Uri.parse(url)
        })
    }

    private fun showModifyNameDialog(name: String = "") {
        MessageDialog.show(requireActivity() as AppCompatActivity, "修改昵称", "最多输入 9 个字符")
            .setCancelable(true)
            .setCustomView(
                R.layout.layout_modify_name
            ) { dialog, v ->
                etName = v.findViewById(R.id.et_name)
                etName?.let {
                    it.setText(name)
                    it.setSelection(name.length)
                }

            }
            .setOkButton("确定")
            .setCancelButton("取消") { baseDialog, v ->
                baseDialog.doDismiss()
                true
            }.setOnOkButtonClickListener { baseDialog, v ->
                if (etName?.text.toString().trim().isEmpty()) {
                    Toast.makeText(requireContext(), "昵称不能为空", Toast.LENGTH_SHORT).show()
                } else {
                    baseDialog.doDismiss()
                    WaitDialog.show(requireActivity() as AppCompatActivity, "正在修改...")
                    vm.modifyName(etName?.text.toString())
                }
                true
            }
    }
}
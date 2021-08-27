package io.anyrtc.studyroom.activity

import android.os.Bundle
import android.view.View
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.databinding.ActivityDisclaimerBinding

class DisclaimerActivity : BaseActivity() {

    private lateinit var binding: ActivityDisclaimerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisclaimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val backClick = View.OnClickListener {
            finish()
            overridePendingTransition(0, R.anim.picture_anim_fade_out)
        }
        binding.run {
            backArrow.setOnClickListener(backClick)
            backText.setOnClickListener(backClick)
        }
    }
}
package io.anyrtc.studyroom.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.databinding.ActivitySplashBinding
import io.anyrtc.studyroom.util.Interval
import java.util.concurrent.TimeUnit

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        XXPermissions.with(this).permission(
            Permission.CAMERA,
            Permission.RECORD_AUDIO,
            Permission.WRITE_EXTERNAL_STORAGE
        ).request { _, all ->
            if (all) {
                Interval(0, 1, TimeUnit.SECONDS, 1).life(this).finish {
                    startActivity(Intent().apply {
                        setClass(this@SplashActivity, MainActivity::class.java)
                        finish()
                    })
                    overridePendingTransition(0, R.anim.picture_anim_fade_in)
                }.start()
            } else {
                Toast.makeText(this@SplashActivity, "请开启权限", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
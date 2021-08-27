package io.anyrtc.studyroom.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.util.ScreenUtils

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ScreenUtils.adapterScreen(this, 375, false)
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).fitsSystemWindows(true).statusBarColor(statusBarColor()).statusBarDarkFont(
            statusBarColor() == R.color.statusBarColor
        ).navigationBarColor(statusBarColor(), 0.2f).navigationBarDarkIcon(statusBarColor() == R.color.statusBarColor).init()
    }

    protected open fun statusBarColor() = R.color.statusBarColor

    fun changeScreenAdapter(isVertical: Boolean, dpi: Int) {
        ScreenUtils.resetScreen(this)
        ScreenUtils.adapterScreen(this, dpi, !isVertical)
    }

    override fun onDestroy() {
        ScreenUtils.resetScreen(this)
        super.onDestroy()
    }
}
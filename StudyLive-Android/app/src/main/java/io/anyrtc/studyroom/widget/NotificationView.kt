package io.anyrtc.studyroom.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import io.anyrtc.studyroom.R

class NotificationView
@JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {

    private var notifyCounting = 0
    private val maximumNotify = 2

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rectF = RectF()

    private val maxWidth = resources.getDimensionPixelSize(R.dimen.dp157)
    private val mHeight = resources.getDimensionPixelSize(R.dimen.dp52)
    private val notifyHeight = resources.getDimensionPixelSize(R.dimen.dp22)
    private val padding = resources.getDimensionPixelSize(R.dimen.dp8)
    private val textMaxWidth = resources.getDimensionPixelSize(R.dimen.dp64)

    private val notifyArr = arrayOfNulls<DrawInfo>(2)

    private lateinit var bufferBitmap: Bitmap
    private lateinit var bufferCanvas: Canvas
    private val mMatrix = Matrix()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!this::bufferCanvas.isInitialized) {
            bufferBitmap = Bitmap.createBitmap(maxWidth, mHeight, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(bufferBitmap)
        }
        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY),
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bufferBitmap, 0.0f, 0.0f, paint)
    }

    private fun insertNotify(drawInfo: DrawInfo, xFloat: Float) {
        //mMatrix.postTranslate()

        mMatrix.reset()
        bufferCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        bufferCanvas.save()
        path.moveTo(notifyHeight.shr(1).toFloat(), 0.0f)
        path.lineTo(maxWidth.toFloat(), 0.0f)
        path.lineTo(maxWidth.toFloat(), notifyHeight.toFloat())
        path.lineTo(notifyHeight.shr(1).toFloat(), notifyHeight.toFloat())
        rectF.set(0.0f, 0.0f, notifyHeight.toFloat(), notifyHeight.toFloat())
        path.arcTo(rectF, 90.0f, -90.0f, false)
        path.close()

        bufferCanvas.clipPath(path)

        paint.color = Color.parseColor("#")
        paint.style = Paint.Style.FILL
        bufferCanvas.drawPath(path, paint)

        bufferCanvas.restore()
    }

    fun addNotify(avatar: String, nickname: String, roomStatus: String) {
        val curIndex = notifyCounting xor maximumNotify
        notifyCounting++

        val newInfo = DrawInfo(avatar, nickname, roomStatus, curIndex)
        val oldInfo = notifyArr[curIndex]
        notifyArr[curIndex] = newInfo

        if (oldInfo != null) {
            // TODO: hide it first
            return
        }
    }

    data class DrawInfo(
        val avatar: String,
        val nickname: String,
        val roomStatus: String,
        val notifyCount: Int
    )
}
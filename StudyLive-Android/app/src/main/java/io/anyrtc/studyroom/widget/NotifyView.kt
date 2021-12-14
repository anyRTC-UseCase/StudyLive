package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

class NotifyView
@JvmOverloads
constructor(
  context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


  private val fontSize = context.resources.getDimensionPixelSize(R.dimen.sp12)
  private val avatarPadding = context.resources.getDimensionPixelSize(R.dimen.dp2)
  private val statusTextPadding = context.resources.getDimensionPixelSize(R.dimen.dp5)
  private val messagePadding = context.resources.getDimensionPixelSize(R.dimen.dp8)
  private var basedMessageWidth = 0f

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val path = Path()

  private var animRunning = false
  private var fontCenterOffset = 0f

  private var limitMessageSize = 2

  private val waitList = LinkedList<Message>()
  private var waitingRemove = 0

  private val animArr = LinkedList<AnimInfo>()
  private val dataArr = LinkedList<Message>()

  private val timer: Timer

  private lateinit var mBufferBitmap: Bitmap
  private lateinit var mBufferCanvas: Canvas
  private lateinit var mBufferMatrix: Matrix

  private var messageHeight = 0
  private var avatarHeight = 0

  init {
    paint.textSize = fontSize.toFloat()
    paint.style = Paint.Style.FILL
    val metrics = paint.fontMetrics
    fontCenterOffset = (abs(metrics.top) - metrics.bottom) / 2f

    timer = Timer()
    timer.schedule(object : TimerTask() {
      override fun run() {
        if (dataArr.isNotEmpty()) {
          dataArr.forEach {
            it.life += 17L
          }
          val first = dataArr.first
          if (first.life >= first.lifeTime) {
            removeFirstMessage(true)
          }
        }

        if (animArr.isEmpty()) {
          return
        }

        val i = animArr.iterator()
        while (i.hasNext()) {
          val next = i.next()
          next.progress += 17L

          var percentage = next.progress.toFloat() / next.duration
          if (percentage > 1.0f)
            percentage = 1.0f
          post { next.block.invoke(interpolator(percentage)) }

          if (next.progress >= next.duration) {
            post { next.done.invoke() }
            i.remove()
          }
        }
      }
    }, 0, 17)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    messageHeight = fontSize + statusTextPadding.shl(1)
    avatarHeight = (messageHeight - avatarPadding.shl(1))

    val width = 11 * fontSize + avatarPadding.shl(1) + statusTextPadding.shl(2) + avatarHeight
    val height = messageHeight.shl(1) + messagePadding

    setMeasuredDimension(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    )

    if (!this::mBufferBitmap.isInitialized) {
      mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      mBufferCanvas = Canvas()
      mBufferCanvas.setBitmap(mBufferBitmap)
      mBufferMatrix = Matrix()
    }

    basedMessageWidth =
      avatarHeight + avatarPadding.shl(1) + messagePadding.shl(1) + paint.measureText("进入直播间")
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawBitmap(mBufferBitmap, 0f, 0f, null)
  }

  private fun interpolator(x: Float): Float = (1.0f - (1.0f - x) * (1.0f - x))

  /*fun upgradeOffsets(xProgress: Float, yProgress: Float) {
    dataArr[0].let {
      it.xProgress = xProgress
      it.yProgress = yProgress
    }
    drawMessage()
  }*/

  fun addMessage(msg: Message) {
    if (!this::mBufferBitmap.isInitialized) {
      post { addMessage(msg) }
      return
    }

    if (animRunning || dataArr.size == limitMessageSize) {
      if (dataArr.size == limitMessageSize)
        removeFirstMessage()
      waitList.add(msg)
      return
    }

    animRunning = true
    dataArr.add(msg)

    val nicknameWidth = paint.measureText(msg.nickname)
    val msgWidth = nicknameWidth + basedMessageWidth

    loadImage(msg.avatar) { bitmap, b ->
      if (!b) return@loadImage

      val shader = BitmapShader(bitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
      msg.let {
        it.bitmap = bitmap
        it.shader = shader
      }
    }

    val yOffset = (dataArr.size - 1) * (messageHeight + messagePadding).toFloat()
    registerAnimator(AnimInfo({ percentage ->
      val xOffset = msgWidth + -(percentage * msgWidth)
      mBufferMatrix.reset()
      mBufferMatrix.postTranslate(xOffset, yOffset)
      mBufferCanvas.setMatrix(mBufferMatrix)
      drawMsg(msg, msgWidth, nicknameWidth)
      invalidate()
    }) {
      animRunning = false
      if (waitingRemove > 0) {
        removeFirstMessage()
      } else if (waitList.isNotEmpty()) {
        addMessage(waitList.removeFirst())
      }
    })
  }

  fun removeFirstMessage(isFromTimer: Boolean = false) {
    if (dataArr.isEmpty())
      return

    if (animRunning) {
      if (!isFromTimer) waitingRemove++
      return
    }

    animRunning = true
    registerAnimator(AnimInfo({ percentage ->
      mBufferBitmap.eraseColor(Color.TRANSPARENT)
      for (i in 0 until dataArr.size) {
        val item = dataArr[i]
        val nicknameWidth = paint.measureText(item.nickname)
        val msgWidth = nicknameWidth + basedMessageWidth
        val msgHeight = (messageHeight + messagePadding).toFloat()
        mBufferMatrix.reset()
        mBufferMatrix.setTranslate(0f, (i * msgHeight) - (percentage * msgHeight))
        mBufferCanvas.setMatrix(mBufferMatrix)
        drawMsg(item, msgWidth, nicknameWidth)
        invalidate()
      }
    }) {
      animRunning = false
      dataArr.removeFirst().bitmap?.recycle()
      if (waitList.size > 0) {
        addMessage(waitList.removeFirst())
      } else if (waitingRemove > 0) {
        if (dataArr.isNotEmpty()) {
          waitingRemove--
          removeFirstMessage()
          return@AnimInfo
        }
        waitingRemove = 0
      }
    })
  }

  private fun drawMsg(msg: Message, messageWidth: Float, nicknameWidth: Float) {
    path.reset()
    paint.color = Color.parseColor("#F3F3F3")
    val statusText = if (msg.joinRoom == 1) "进入直播间" else "退出直播间"
    val messageLeft = measuredWidth - messageWidth

    path.addArc(
      messageLeft,
      0f,
      messageLeft + avatarPadding + avatarHeight.toFloat(),
      messageHeight.toFloat(),
      90f,
      180f
    )
    path.moveTo(messageLeft + avatarHeight.shr(1).toFloat(), 0f)
    path.lineTo(measuredWidth.toFloat(), 0f)
    path.lineTo(measuredWidth.toFloat(), messageHeight.toFloat())
    path.lineTo(
      messageLeft + avatarHeight.shr(1).toFloat(),
      messageHeight.toFloat()
    )

    paint.color = Color.parseColor("#434343")
    mBufferCanvas.drawPath(path, paint)

    paint.color = Color.WHITE
    mBufferCanvas.drawText(
      statusText,
      messageLeft + avatarHeight + avatarPadding.shl(1) + nicknameWidth + messagePadding,
      messageHeight.shr(1) + fontCenterOffset,
      paint
    )

    paint.color = Color.parseColor("#BCBCBC")
    mBufferCanvas.drawText(
      msg.nickname,
      messageLeft + avatarPadding.shl(1) + avatarHeight,
      messageHeight.shr(1) + fontCenterOffset,
      paint
    )

    msg.bitmap?.let {
      mBufferCanvas.save()
      paint.shader = msg.shader
      val translateOffset = (messageHeight - it.width).shr(1)
      mBufferCanvas.translate(
        messageLeft + translateOffset,
        0f + translateOffset
      )
      mBufferCanvas.drawCircle(
        it.width.shr(1).toFloat(),
        it.width.shr(1).toFloat(),
        avatarHeight.shr(1).toFloat(),
        paint
      )
      paint.shader = null
      mBufferCanvas.restore()
    }
  }

  private fun loadImage(uri: String, callback: (Bitmap?, Boolean) -> Unit) {
    Thread {
      try {
        var http = URL(uri).openConnection() as HttpURLConnection
        http.connectTimeout = 5000
        http.readTimeout = 5000
        http.requestMethod = "GET"
        http.connect()

        var iStream = http.inputStream
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        BitmapFactory.decodeStream(iStream, null, options)
        val outWidth = options.outWidth
        val outHeight = options.outHeight

        val minDimension = outWidth.coerceAtMost(outHeight)
        options.inSampleSize =
          floor((minDimension.toFloat() / avatarHeight).toDouble()).toInt()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        options.inJustDecodeBounds = false

        iStream.close()

        http = URL(uri).openConnection() as HttpURLConnection
        http.connectTimeout = 5000
        http.readTimeout = 5000
        http.requestMethod = "GET"
        http.connect()
        iStream = http.inputStream

        val bitmap =
          BitmapFactory.decodeStream(iStream, null, options)
            ?: throw IOException("bitmap is null")
        iStream.close()

        post { callback.invoke(bitmap, true) }
      } catch (e: IOException) {
        callback.invoke(null, false)
        e.printStackTrace()
      } catch (e: SocketTimeoutException) {
      }
    }.start()
  }

  private fun registerAnimator(animInfo: AnimInfo) {
    animArr.add(animInfo)
  }

  override fun onDetachedFromWindow() {
    timer.cancel()
    timer.purge()
    super.onDetachedFromWindow()
  }

  data class Message(
    val avatar: String,
    val nickname: String,
    val joinRoom: Int = 1,
    var shader: BitmapShader? = null,
    var bitmap: Bitmap? = null,
    var life: Long = 0L,
    val lifeTime: Long = 5000L, // 存在多久
  )

  private data class AnimInfo(
    val block: (percentage: Float) -> Unit,
    val duration: Long = 510L,
    var progress: Long = 0L,
    val done: () -> Unit = {}
  )
}

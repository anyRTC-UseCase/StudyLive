package io.anyrtc.studyroom.layout

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import io.anyrtc.studyroom.R

class VideosLayout
@JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var videosWidth = 0
    private var topicViewHeight = 0
    private var horizontalSmallVideoWidth = 0
    private var horizontalSmallVideoHeight = 0
    private val videoViewSpacing = 6
    private val titleHeight = resources.getDimensionPixelSize(R.dimen.dp45)

    private var isVertical = true
    private var isSmallMode = false
    private var topicIndex = -1

    private var isAnimRunning = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount < 7) {
            return
        }

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        isVertical = height > width

        if (!isVertical) {
            videosWidth = (width * 0.548f).toInt()
            horizontalSmallVideoWidth = (videosWidth / 3.0f).toInt()
            horizontalSmallVideoHeight = (horizontalSmallVideoWidth * 0.6803f).toInt()
            topicViewHeight = height - horizontalSmallVideoHeight - videoViewSpacing
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)

            val location = if (child.tag == null) {
                val location = createAndCalcCoordinates(child, i, width, height)
                if (location.fromLeft == 0 && location.fromRight == 0) location.run {
                    fromLeft = toLeft
                    fromTop = toTop
                    fromRight = toRight
                    fromBottom = toBottom
                }
                location
            } else {
                child.tag as ViewLocation
            }

            child.tag = location
            child.measure(
                MeasureSpec.makeMeasureSpec(location.right - location.left, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(location.bottom - location.top, MeasureSpec.EXACTLY)
            )
        }

        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val location = child.tag as ViewLocation

            child.layout(location.left, location.top, location.right, location.bottom)
        }
    }

    private fun createAndCalcCoordinates(
        child: View,
        index: Int,
        relativeWidth: Int,
        relativeHeight: Int
    ): ViewLocation {
        val location = ViewLocation(child)
        calcCoordinates(index, location, relativeWidth, relativeHeight)
        location.run {
            left = toLeft
            top = toTop
            right = toRight
            bottom = toBottom
        }
        return location
    }

    private fun calcCoordinates(
        index: Int,
        location: ViewLocation,
        relativeWidth: Int,
        relativeHeight: Int,
    ) {
        if (!isVertical) when {
            index == 0 -> location.apply {
                startOffset = .1f
                toLeft = 0
                toTop = 0
                toRight = videosWidth
                toBottom = topicViewHeight
            }
            index < 4 -> location.apply {
                val mIndex = index - 1

                startOffset = calcStartOffset(index)
                toLeft = mIndex * horizontalSmallVideoWidth + if (mIndex > 0)
                    mIndex * videoViewSpacing.shr(1) else 0
                toTop = topicViewHeight + videoViewSpacing
                toRight = toLeft + horizontalSmallVideoWidth - if (mIndex < 3) videoViewSpacing.shr(1) else 0
                toBottom = relativeHeight
            }
            index in 4..5 -> location.apply {
                val titleWidth = (relativeWidth - videosWidth).shr(1)
                startOffset = calcStartOffset(index)
                toLeft = videosWidth + (index - 4) * titleWidth
                toTop = 0
                toRight = toLeft + titleWidth
                toBottom = toTop + titleHeight
            }
            else -> location.apply {
                startOffset = calcStartOffset(index)
                toLeft = videosWidth
                toTop = titleHeight
                toRight = relativeWidth
                toBottom = relativeHeight
            }
        } else {
            val videoHeight = ((relativeWidth.shr(1) - videoViewSpacing.shr(1)) * 0.6803f).toInt()
            when (index) {
                4 -> location.apply {
                    startOffset = 0.1f
                    toLeft = 0
                    toTop = 0
                    toRight = relativeWidth
                    toBottom = titleHeight
                }
                in 0..3 -> location.apply {
                    startOffset = calcStartOffset(index + 1)
                    toLeft = (index % 2) * (relativeWidth.shr(1) + videoViewSpacing.shr(1))
                    toTop =
                        (if (index >= 2) index / 2 else 0) * videoHeight + (if (index >= 2) videoViewSpacing else 0) + titleHeight
                    toRight = toLeft + relativeWidth.shr(1) - videoViewSpacing.shr(1)
                    toBottom = toTop + videoHeight
                }
                5 -> location.apply {
                    startOffset = calcStartOffset(index + 1)
                    toLeft = 0
                    toTop = titleHeight + videoViewSpacing + videoHeight.shl(1)
                    toRight = relativeWidth
                    toBottom = toTop + titleHeight
                }
                else -> location.apply {
                    startOffset = calcStartOffset(index + 1)
                    toLeft = 0
                    toTop = titleHeight.shl(1) + videoViewSpacing + videoHeight.shl(1)
                    toRight = relativeWidth
                    toBottom = relativeHeight
                }
            }
        }
    }

    private fun calcStartOffset(index: Int) = .2f * index * .2f

    /**
     * 切换横竖屏
     */
    fun refreshOrientation() {
        requestLayout()
        post {
            val array = arrayOfNulls<ViewLocation>(childCount)
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val location = child.tag as ViewLocation
                array[i] = location
                calcCoordinates(i, location, measuredWidth, measuredHeight)
                location.run {
                    fromLeft = toLeft
                    fromTop = toTop
                    fromRight = toRight
                    fromBottom = toBottom
                }
            }

            isSmallMode = false
            topicIndex = if (isVertical) -1 else 0
            requestLayout()
        }
    }

    fun toSmallVideos() {
        if (isAnimRunning) {
            return
        }
        if (!isVertical || isSmallMode) {
            return
        }
        isAnimRunning = true
        isSmallMode = true
        topicIndex = -1

        val arr = arrayOfNulls<ViewLocation>(childCount)
        for (i in 0 until childCount) {
            val location = getChildLocationAndResetFromLocations(i)
            arr[i] = location

            val videoWidth = measuredWidth.shr(2)
            val videoHeight = (videoWidth.toFloat() * 0.6882f).toInt()
            when (i) {
                in 0..3 -> location.run {
                    toLeft = i * videoWidth + if (i > 0) videoViewSpacing.shr(1) else 0
                    toTop = titleHeight
                    toRight = toLeft + videoWidth - if (i > 0) videoViewSpacing.shr(1) else 0
                    toBottom = toTop + videoHeight
                }
                5 -> location.run {
                    toTop = titleHeight + videoHeight
                    toBottom = toTop + titleHeight
                }
                6 -> location.run {
                    toTop = titleHeight.shl(1) + videoHeight
                }
            }
        }

        post(AnimRunnable(arr.map { it!! }.toTypedArray(), mDuration = 200L, isRotation = false))
    }

    fun toEquallyDividedVideos() {
        if (isAnimRunning) {
            return
        }
        if (!isVertical/* || !isSmallMode*/) {
            return
        }
        isAnimRunning = true
        isSmallMode = false
        topicIndex = -1

        val videoWidth = measuredWidth.shr(1)
        val videoHeight = (videoWidth.toFloat() * 0.6882f).toInt()

        val arr = arrayOfNulls<ViewLocation>(childCount)
        for (i in 0 until childCount) {
            val location = getChildLocationAndResetFromLocations(i)
            arr[i] = location

            val remainder = (i % 2)
            when (i) {
                in 0..3 -> location.run {
                    toLeft = remainder * videoWidth + remainder * videoViewSpacing.shr(1)
                    toTop =
                        (if (i >= 2) i / 2 else 0) * (videoViewSpacing + videoHeight) + titleHeight
                    toRight = toLeft + videoWidth - ((i + 1) % 2) * videoViewSpacing.shr(1)
                    toBottom = toTop + videoHeight
                }
                5 -> location.run {
                    toTop = titleHeight + videoHeight.shl(1) + videoViewSpacing
                    toBottom = toTop + titleHeight
                }
                6 -> location.run {
                    toTop = titleHeight.shl(1) + videoHeight.shl(1) + videoViewSpacing
                }
            }
        }

        post(AnimRunnable(arr.map { it!! }.toTypedArray(), mDuration = 200L, isRotation = false))
    }

    fun toTopicMode(targetIndex: Int) {
        if (isAnimRunning) {
            return
        }
        if (topicIndex == targetIndex) {
            toEquallyDividedVideos()
            return
        }
        if (targetIndex < 0 || targetIndex > 3) {
            return
        }
        isAnimRunning = true
        topicIndex = targetIndex

        val topicHeight = (measuredWidth.toFloat() * 0.6882f).toInt()
        val smallWidth = (measuredWidth.toFloat() / 3.0f).toInt()
        val smallHeight = (smallWidth.toFloat() * 0.6882f).toInt()
        val halfVideoSpacing = videoViewSpacing.shr(1)

        val arr = arrayOfNulls<ViewLocation>(childCount)
        var smallVideoIndex = 0
        for (i in 0 until childCount) {
            val location = getChildLocationAndResetFromLocations(i)
            arr[i] = location

            if (isVertical) when (i) {
                in 0..3 -> location.run {
                    if (i == targetIndex) {
                        toLeft = 0
                        toTop = titleHeight
                        toRight = measuredWidth
                        toBottom = toTop + topicHeight
                    } else {
                        toLeft = smallVideoIndex * (halfVideoSpacing + smallWidth)
                        toTop = titleHeight + topicHeight + videoViewSpacing
                        toRight = toLeft + smallWidth - halfVideoSpacing
                        toBottom = toTop + smallHeight
                        smallVideoIndex++
                    }
                }
                5 -> location.run {
                    toTop = titleHeight + topicHeight + smallHeight
                    toBottom = toTop + titleHeight
                }
                6 -> location.run {
                    toTop = titleHeight.shl(1) + topicHeight + smallHeight
                }
            } else {
                if (i in 0..3) location.run {
                    if (i == targetIndex) {
                        toLeft = 0
                        toTop = 0
                        toRight = toLeft + videosWidth
                        toBottom = topicViewHeight - videoViewSpacing.shr(1)
                    } else {
                        toLeft =
                            smallVideoIndex * (horizontalSmallVideoWidth) + if (smallVideoIndex > 0) videoViewSpacing.shr(
                                1
                            ) else 0
                        toTop = topicViewHeight + videoViewSpacing.shr(1)
                        toRight =
                            toLeft + horizontalSmallVideoWidth - if (smallVideoIndex < 3) videoViewSpacing.shr(
                                1
                            ) else 0
                        toBottom = toTop + horizontalSmallVideoHeight
                        smallVideoIndex++
                    }
                }
            }
        }

        post(AnimRunnable(arr.map { it!! }.toTypedArray(), mDuration = 175L, isRotation = false))
    }

    private fun getChildLocationAndResetFromLocations(i: Int): ViewLocation {
        val child = getChildAt(i)
        val location = child.tag as ViewLocation
        return location.apply {
            fromLeft = left
            fromTop = top
            fromRight = right
            fromBottom = bottom
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    data class ViewLocation(
        var view: View? = null,
        var startOffset: Float = 0.0f,
        var progress: Float = 0.0f,
        val x: Float = 3.75f,
        var left: Int = 0,
        var top: Int = 0,
        var right: Int = 0,
        var bottom: Int = 0,
        var toLeft: Int = 0,
        var toTop: Int = 0,
        var toRight: Int = 0,
        var toBottom: Int = 0
    ) {

        var fromLeft: Int = 0
            set(value) {
                field = value
                left = value
            }
        var fromTop: Int = 0
            set(value) {
                field = value
                top = value
            }
        var fromRight: Int = 0
            set(value) {
                field = value
                right = value
            }
        var fromBottom: Int = 0
            set(value) {
                field = value
                bottom = value
            }
    }

    private inner class AnimRunnable(
        private val viewLocations: Array<ViewLocation>,
        private var mProgress: Long = 0L,
        private val mDuration: Long = 230L,
        private val step: Long = 8L,
        private val isRotation: Boolean,
        private val toSmall: Boolean = true
    ) : Runnable {

        private fun defInterpolator(percent: Float) = (1.0f - (1.0f - percent) * (1.0f - percent))

        override fun run() {
            var percent = mProgress.toFloat() / mDuration.toFloat()
            if (mProgress >= mDuration) {
                percent = 1.0f
            }

            viewLocations.forEach { item ->
                if (isRotation) {
                    var c = percent - item.startOffset
                    if (c < 0.0f)
                        c = 0.0f

                    val scaleProgress = (c * item.x).let { if (it > 1.0f) 1.0f else it }
                    val horizontalProgress =
                        ((item.toRight - item.toLeft).shr(1) * scaleProgress).toInt()
                    val verticalProgress =
                        ((item.toBottom - item.toTop).shr(1) * scaleProgress).toInt()

                    item.run {
                        if (toSmall) {
                            left = toLeft + horizontalProgress
                            top = toTop + verticalProgress
                            right = toRight - horizontalProgress
                            bottom = toBottom - verticalProgress
                        } else {
                            left = fromLeft - horizontalProgress
                            top = fromTop - verticalProgress
                            right = fromRight + horizontalProgress
                            bottom = fromBottom + verticalProgress
                        }
                    }
                } else item.run {
                    progress = percent
                    val calculatedPercent = defInterpolator(percent)

                    val diffLeft = toLeft - fromLeft
                    val diffTop = toTop - fromTop
                    val diffRight = toRight - fromRight
                    val diffBottom = toBottom - fromBottom

                    left = fromLeft + (diffLeft * calculatedPercent).toInt()
                    top = fromTop + (diffTop * calculatedPercent).toInt()
                    right = fromRight + (diffRight * calculatedPercent).toInt()
                    bottom = fromBottom + (diffBottom * calculatedPercent).toInt()
                }

                if (percent == 1.0f) item.run {
                    if (isRotation) {
                        fromLeft = left
                        fromTop = top
                        fromRight = right
                        fromBottom = bottom
                    } else {
                        fromLeft = toLeft
                        fromTop = toTop
                        fromRight = toRight
                        fromBottom = toBottom
                    }
                }
            }

            requestLayout()
            mProgress += step
            if (percent != 1.0f) {
                postDelayed(this, step)
            } else {
                isAnimRunning = false
            }
        }
    }
}
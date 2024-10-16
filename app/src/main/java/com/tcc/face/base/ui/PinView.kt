package com.tcc.face.base.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.tcc.face.R
import com.tcc.face.base.dp
import kotlin.math.min


class PinView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var defaultColor: Int = -1
    private var defaultReverseColor: Int = -1
    private var defaultErrorColor: Int = -1
    private var currentColor: Int = -1

    private var defaultLength = 4
    private var currentLength = 0

    private var indicatorMargin = -1
    private var indicatorWidth = -1
    private var indicatorHeight = -1

    private val fadeInAnimation
        get() = ScaleAnimation(
            0f,
            1f,
            0f,
            1f,
            RELATIVE_TO_SELF,
            0.5f,
            RELATIVE_TO_SELF,
            0.5f
        ).apply {
            duration = 200
            fillAfter = true
        }

    private val fadeOutAnimation
        get() = ScaleAnimation(
            1f,
            0f,
            1f,
            0f,
            RELATIVE_TO_SELF,
            0.5f,
            RELATIVE_TO_SELF,
            0.5f
        ).apply {
            duration = 200
            fillAfter = true
        }

    private val shakeAnimation
        get() = TranslateAnimation(
            -2.dp.toFloat(),
            2.dp.toFloat(),
            0f,
            0f
        ).apply {
            duration = 10
            repeatCount = 10
        }

    private var reverseColor = false

    init {
        isSaveEnabled = true
        orientation = HORIZONTAL

        val miniSize = 16.dp

        indicatorWidth = miniSize
        indicatorHeight = miniSize
        indicatorMargin = miniSize / 4

        gravity = Gravity.CENTER

        defaultColor = ContextCompat.getColor(context, R.color.black)
        defaultReverseColor = ContextCompat.getColor(context, R.color.white)
        defaultErrorColor = ContextCompat.getColor(context, R.color.red)
    }

    private var isError: Boolean = false

    fun init(reverseColor: Boolean = false) {
        this.reverseColor = reverseColor
        currentColor = if (reverseColor) {
            defaultReverseColor
        } else {
            defaultColor
        }
        createEmptyIndicators()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.currentLength = currentLength
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = (state as? SavedState)
        super.onRestoreInstanceState(savedState?.superState)
        currentLength = savedState?.currentLength ?: 0
        updateState()
    }

    fun setDefaultLength(length: Int) {
        defaultLength = length
        createEmptyIndicators()
        updateState()
    }

    fun remove() {
        if (currentLength == 0) return

        currentLength--
        val circle = getChildAt(currentLength) as CircleView?
        if (circle != null) {
            if (currentLength >= defaultLength) {
                removeCircle(circle)
            } else {
                clearCircle(circle)
            }
        }
    }

    fun add() {
        currentLength++
        val circle = getChildAt(currentLength - 1) as CircleView?
        if (circle != null) {
            fillCircle(circle)
        } else {
            addNewCircle()
        }
    }

    fun setError(isError: Boolean) {
        this.isError = isError
        for (circle in children) {
            (circle as CircleView).setError(isError)
        }
    }

    fun shake() {
        startAnimation(shakeAnimation)
    }

    fun clear() {
        if (currentLength == 0) return

        currentLength = 0
        createEmptyIndicators()
    }

    private fun createEmptyIndicators() {
        removeAllViews()
        for (i in 0 until defaultLength) {
            val circle = createCircle(
                color = currentColor,
                errorColor = defaultErrorColor,
                filled = false,
                isError = isError
            )
            addView(circle)
            circle.startAnimation(fadeInAnimation)
        }
    }

    private fun updateState() {
        for (i in 0 until currentLength) {
            val circle = getChildAt(i) as CircleView?
            if (circle != null) {
                circle.setFilled(true)
            } else {
                val newCircle = createCircle(
                    color = currentColor,
                    errorColor = defaultErrorColor,
                    filled = true,
                    isError = isError
                )
                addView(newCircle)
            }
        }
    }

    private fun fillCircle(circle: CircleView) {
        circle.setFilled(true)
        circle.startAnimation(fadeInAnimation)
    }

    private fun clearCircle(circle: CircleView) {
        circle.setFilled(false)
        circle.startAnimation(fadeInAnimation)
    }

    private fun addNewCircle() {
        val circle = createCircle(
            color = currentColor,
            errorColor = defaultErrorColor,
            filled = true,
            isError = isError
        )
        addView(circle)
        fillCircle(circle)
    }

    private fun removeCircle(circle: View) {
        circle.startAnimation(fadeOutAnimation)
        postDelayed({ removeView(circle) }, 200)
    }

    private fun createCircle(color: Int, errorColor: Int, filled: Boolean, isError: Boolean): CircleView {
        val circle = CircleView(context, color, errorColor, filled, isError)
        val params = generateDefaultLayoutParams()
        params.width = indicatorWidth
        params.height = indicatorHeight
        params.leftMargin = indicatorMargin
        params.rightMargin = indicatorMargin
        circle.layoutParams = params

        return circle
    }

    private class SavedState : BaseSavedState {
        internal var currentLength: Int = 0

        internal constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            currentLength = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(currentLength)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    class CircleView : View {

        private var color: Int = Color.BLACK
        private var errorColor: Int = Color.RED
        private var strokeWidth: Float = 2f.dp
        private var filled: Boolean = false
        private var error: Boolean = false

        @JvmOverloads constructor(
            context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
        ) : super(context, attrs, defStyleAttr) {
            init(attrs)
        }

        @JvmOverloads constructor(
            context: Context, color: Int, errorColor: Int, filled: Boolean = false, isError: Boolean = false
        ) : super(context) {
            this.color = color
            this.errorColor = errorColor
            this.filled = filled
            this.error = isError
            init()
        }

        override fun getAccessibilityClassName(): CharSequence {
            return CircleView::class.qualifiedName ?: "com.xinfotech.CircleView"
        }

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private fun init(attrs: AttributeSet? = null) {
//            context.theme.obtainStyledAttributes(attrs, R.styleable.CircleView, 0, 0).apply {
//                try {
//                    filled = getBoolean(R.styleable.CircleView_filled, false)
//                    error = getBoolean(R.styleable.CircleView_error, false)
//                } finally {
//                    recycle()
//                }
//            }

            paint.strokeWidth = strokeWidth

            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            updateContentDescription()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width/2f
            val cy = height/2f
            paint.color = if (error) {
                errorColor
            } else {
                color
            }
            val radius: Float
            if (filled) {
                radius = min(width, height)/2f
                paint.style = Paint.Style.FILL
            } else {
                radius = min(width, height)/2f - strokeWidth/2
                paint.style = Paint.Style.STROKE
            }

            canvas.drawCircle(cx, cy, radius, paint)
        }

        fun setFilled(filled: Boolean) {
            if (this.filled != filled) {
                this.filled = filled
                updateContentDescription()
                invalidate()
            }
        }

        fun setError(isError: Boolean) {
            if (this.error != isError) {
                this.error = isError
                updateContentDescription()
                invalidate()
            }
        }

        fun isFilled() = filled
        fun isError() = error

        private fun updateContentDescription() {
            val attrList = mutableListOf<String>()
            if (filled) {
                attrList.add("filled")
            } else {
                attrList.add("not_filled")
            }

            if (error) {
                attrList.add("error")
            } else {
                attrList.add("not_error")
            }

            contentDescription = attrList.joinToString("|")
        }
    }
}
package com.tcc.face.base.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tcc.face.R
import com.tcc.face.base.dp


class PinKeyboard @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {

    var onClickListener: ((key: Key) -> Unit)? = null
    var onClearAllClickListener: (() -> Unit)? = null

    init {
        columnCount = 3
        useDefaultMargins = true
        if (isInEditMode) {
            setupKeyboard(MODE_ACTION_OK, false)
        }
    }

    fun setupKeyboard(mode: String, reverseColors: Boolean) {
        removeAllViews()
        addView(createCodeKey(Key.Code(1), reverseColors))
        addView(createCodeKey(Key.Code(2), reverseColors))
        addView(createCodeKey(Key.Code(3), reverseColors))
        addView(createCodeKey(Key.Code(4), reverseColors))
        addView(createCodeKey(Key.Code(5), reverseColors))
        addView(createCodeKey(Key.Code(6), reverseColors))
        addView(createCodeKey(Key.Code(7), reverseColors))
        addView(createCodeKey(Key.Code(8), reverseColors))
        addView(createCodeKey(Key.Code(9), reverseColors))
        if (reverseColors) {
            addView(createClearKey(Key.Clear(R.drawable.ic_clear_reverse)))
        } else {
            addView(createClearKey(Key.Clear(R.drawable.ic_clear)))
        }
        addView(createCodeKey(Key.Code(0), reverseColors))
        when (mode) {
            MODE_ACTION_OK -> {
                addView(createActionKey(Key.Action(resources.getString(R.string.common_ok)), reverseColors))
            }
            MODE_ACTION_CANCEL -> {
                addView(createActionKey(Key.Action(resources.getString(R.string.common_cancel)), reverseColors))
            }
            MODE_ACTION_NONE -> {}
            else -> {}
        }
    }

    private fun createClearKey(key: Key.Clear): View {
        val keyView = FrameLayout(context)
        val params = LayoutParams(spec(UNDEFINED, 1f), spec(UNDEFINED, 1f))
        params.width = 0
        keyView.layoutParams = params
        TypedValue().also {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
            keyView.setBackgroundResource(it.resourceId)
        }

        val icon = ImageView(context)
        icon.layoutParams = FrameLayout.LayoutParams(24.dp, 24.dp).apply {
            gravity = Gravity.CENTER
        }

        icon.setImageResource(key.iconRes)

        keyView.addView(icon)

        keyView.setOnClickListener { onClickListener?.invoke(key) }
        keyView.setOnLongClickListener {
            if (onClearAllClickListener != null) {
                onClearAllClickListener?.invoke()
                true
            } else {
                false
            }
        }
        return keyView
    }

    private fun createActionKey(key: Key.Action, reverseColors: Boolean): View {
        val keyView = TextView(context)

        val params = LayoutParams(spec(UNDEFINED, 1f), spec(UNDEFINED, 1f))
        params.width = 0
        keyView.layoutParams = params

        keyView.text = key.text
        keyView.textSize = 22f
        val color = if (!reverseColors)  R.color.black else  R.color.white
        keyView.setTextColor(ContextCompat.getColor(keyView.context, color))
        keyView.isAllCaps = true
        keyView.gravity = Gravity.CENTER
        keyView.setPadding(8.dp, 16.dp, 8.dp, 16.dp)

        TypedValue().also {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
            keyView.setBackgroundResource(it.resourceId)
        }

        keyView.setOnClickListener { onClickListener?.invoke(key) }

        return keyView
    }

    private fun createCodeKey(key: Key.Code, reverseColors: Boolean): View {
        val keyView = TextView(context)

        val params = LayoutParams(spec(UNDEFINED, 1f), spec(UNDEFINED, 1f))
        params.width = 0
        keyView.layoutParams = params

        keyView.text = "${key.digit}"
        keyView.textSize = 22f
        val color = if (!reverseColors)  R.color.black else  R.color.white
        keyView.setTextColor(ContextCompat.getColor(keyView.context, color))
        keyView.gravity = Gravity.CENTER
        keyView.setPadding(8.dp, 16.dp, 8.dp, 16.dp)

        TypedValue().also {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
            keyView.setBackgroundResource(it.resourceId)
        }

        keyView.setOnClickListener { onClickListener?.invoke(key) }

        return keyView
    }

    sealed class Key {
        class Code(val digit: Int) : Key()
        class Clear(val iconRes: Int) : Key()
        class Action(val text: String) : Key()
    }

    companion object {
        const val MODE_ACTION_OK = "mode_ok"
        const val MODE_ACTION_CANCEL = "mode_cancel"
        const val MODE_ACTION_NONE = "mode_none"
    }
}
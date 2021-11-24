package com.example.novameet

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

class ClearableEditText : AppCompatEditText, TextWatcher, OnTouchListener, OnFocusChangeListener {
    private var clearDrawable: Drawable? = null
    //private var onFocusChangeListener: OnFocusChangeListener? = null
    private var onTouchListener: OnTouchListener? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
            context!!, attrs
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context!!, attrs, defStyleAttr
    ) {
        init()
    }

    override fun setOnFocusChangeListener(onFocusChangeListener: OnFocusChangeListener) {
        this.onFocusChangeListener = onFocusChangeListener
    }

    override fun setOnTouchListener(onTouchListener: OnTouchListener) {
        this.onTouchListener = onTouchListener
    }

    private fun init() {
        val tempDrawable = ContextCompat.getDrawable(context, R.drawable.ic_edit_24px)
        clearDrawable = DrawableCompat.wrap(tempDrawable!!)

        clearDrawable?.let {
            DrawableCompat.setTintList(it, hintTextColors)
        }

        clearDrawable?.setBounds(
                0,
                0,
                clearDrawable?.getIntrinsicWidth()?:0,
                clearDrawable?.getIntrinsicHeight()?:0
        )

        setClearIconVisible(true)
        super.setOnTouchListener(this)
        super.setOnFocusChangeListener(this)
        addTextChangedListener(this)
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        val tempDrawable =
                if (hasFocus) ContextCompat.getDrawable(context, R.drawable.ic_close_24px)
                else ContextCompat.getDrawable(context, R.drawable.ic_edit_24px)

        clearDrawable = DrawableCompat.wrap(tempDrawable!!)

        clearDrawable?.let {
            DrawableCompat.setTintList(it, hintTextColors)
        }

        clearDrawable?.setBounds(
                0,
                0,
                clearDrawable?.getIntrinsicWidth()?:0,
                clearDrawable?.getIntrinsicHeight()?:0
        )

        setClearIconVisible(true)

        if (onFocusChangeListener != null) {
            //onFocusChangeListener!!.onFocusChange(view, hasFocus)
            // onFocusChangeListener?.onFocusChange(view, hasFocus)
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val x = motionEvent.x.toInt()
        if (clearDrawable!!.isVisible && x > width - paddingRight - clearDrawable!!.intrinsicWidth) {
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                error = null
                setText(null)
            }
            return true
        }
        return if (onTouchListener != null) {
            onTouchListener!!.onTouch(view, motionEvent)
        } else {
            false
        }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        //if (isFocused) {
        //    setClearIconVisible(s.length > 0)
        //}
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
    private fun setClearIconVisible(visible: Boolean) {
        //clearDrawable!!.setVisible(visible, false)
        clearDrawable?.setVisible(visible, false)
        setCompoundDrawables(null, null, if (visible) clearDrawable else null, null)
    }
}
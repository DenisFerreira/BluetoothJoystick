package com.denisferreira.bluetoothjoystick.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

class DualJoystick : LinearLayout {
    private val D = false
    private var dbgPaint1: Paint? = null
    private var stickL: Joystick
    private var stickR: Joystick
    private var pad: View? = null

    constructor(context: Context?) : super(context) {
        stickL = Joystick(context)
        stickR = Joystick(context)
        initDualJoystickView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        stickL = Joystick(context, attrs)
        stickR = Joystick(context, attrs)
        initDualJoystickView()
    }

    private fun initDualJoystickView() {
        orientation = HORIZONTAL
        stickL.setHandleColor(Color.BLACK)
        stickR.setHandleColor(Color.WHITE)
        if (D) {
            dbgPaint1 = Paint(Paint.ANTI_ALIAS_FLAG)
            dbgPaint1!!.color = Color.CYAN
            dbgPaint1!!.strokeWidth = 1f
            dbgPaint1!!.style = Paint.Style.STROKE
        }
        pad = View(context)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        removeView(stickL)
        removeView(stickR)
        val padW = (measuredWidth - measuredHeight * 2).toFloat()
        val joyWidth = ((measuredWidth - padW) / 2).toInt()
        val joyLParams = LayoutParams(
            joyWidth,
            measuredHeight
        )
        stickL.setLayoutParams(joyLParams)
        stickR.setLayoutParams(joyLParams)
        stickL.TAG = "L"
        stickR.TAG = "R"
        stickL.pointerId = Joystick.INVALID_POINTER_ID
        stickR.pointerId = Joystick.INVALID_POINTER_ID
        addView(stickL)
        val padLParams = ViewGroup.LayoutParams(
            padW.toInt(),
            measuredHeight
        )
        removeView(pad)
        pad!!.layoutParams = padLParams
        addView(pad)
        addView(stickR)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        stickR.setTouchOffset(stickR.getLeft(), stickR.getTop())
    }

    fun setAutoReturnToCenter(left: Boolean, right: Boolean) {
        stickL.isAutoReturnToCenter = left
        stickR.isAutoReturnToCenter = right
    }

    fun setOnJostickMovedListener(left: JoystickMovedListener?, right: JoystickMovedListener?) {
        stickL.setOnJoystickMovedListener(left)
        stickR.setOnJoystickMovedListener(right)
    }

    fun setOnJostickClickedListener(
        left: JoystickClickedListener?,
        right: JoystickClickedListener?
    ) {
        stickL.setOnJoystickClickedListener(left)
        stickR.setOnJoystickClickedListener(right)
    }

    fun setYAxisInverted(leftYAxisInverted: Boolean, rightYAxisInverted: Boolean) {
        stickL.isYAxisInverted = leftYAxisInverted
        stickR.isYAxisInverted = rightYAxisInverted
    }

    fun setMovementConstraint(movementConstraint: Int) {
        stickL.setMovementConstraint(movementConstraint)
        stickR.setMovementConstraint(movementConstraint)
    }

    fun setMovementRange(movementRangeLeft: Float, movementRangeRight: Float) {
        stickL.movementRange = movementRangeLeft
        stickR.movementRange = movementRangeRight
    }

    fun setMoveResolution(leftMoveResolution: Float, rightMoveResolution: Float) {
        stickL.moveResolution = leftMoveResolution
        stickR.moveResolution = rightMoveResolution
    }

    fun setUserCoordinateSystem(leftCoordinateSystem: Int, rightCoordinateSystem: Int) {
        stickL.setUserCoordinateSystem(leftCoordinateSystem)
        stickR.setUserCoordinateSystem(rightCoordinateSystem)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (D) {
            canvas.drawRect(
                1f, 1f, (measuredWidth - 1).toFloat(), (measuredHeight - 1).toFloat(),
                dbgPaint1!!
            )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val l: Boolean = stickL.dispatchTouchEvent(ev)
        val r: Boolean = stickR.dispatchTouchEvent(ev)
        return l || r
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val l: Boolean = stickL.onTouchEvent(ev)
        val r: Boolean = stickR.onTouchEvent(ev)
        return l || r
    }

    companion object {
        private val TAG = DualJoystick::class.java.simpleName
    }
}

package com.denisferreira.bluetoothjoystick.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

import android.graphics.BitmapFactory

import android.graphics.Bitmap
import com.denisferreira.bluetoothjoystick.R


class Joystick : View {
    // =========================================
    // Private Members
    // =========================================
    private val D = false
    var TAG = "JoystickView"

    // joystick model
    private var bgPaint: Paint? = null
    private var handlePaint: Paint? = null
    private var stickPaint: Paint? = null
    private var basePaint: Paint? = null
    private var innerPadding = 0
    private var bgRadius = 0
    private var handleRadius = 0
    private var movementRadius = 0
    private var handleInnerBoundaries = 0
    private var moveListener: JoystickMovedListener? = null
    private var clickListener: JoystickClickedListener? = null

    //# of pixels movement required between reporting to the listener
    var moveResolution = 0f
    var isYAxisInverted = false
    var isAutoReturnToCenter = false
    private var movementConstraint = 0
    var movementRange = 0f
    private var userCoordinateSystem = 0

    //Records touch pressure for click handling
    private var touchPressure = 0f
    private var clicked = false
    private var clickThreshold = 0f

    //Last touch point in view coordinates
    var pointerId = INVALID_POINTER_ID
    private var touchX = 0f
    private var touchY = 0f

    //Last reported position in view coordinates (allows different reporting sensitivities)
    private var reportX = 0f
    private var reportY = 0f

    //Handle center in view coordinates
    private var handleX = 0f
    private var handleY = 0f

    //Center of the view in view coordinates
    private var cX = 0
    private var cY = 0

    //Size of the view in view coordinates
    private var dimX //, dimY;
            = 0

    //Cartesian coordinates of last touch point - joystick center is (0,0)
    private var cartX = 0
    private var cartY = 0

    //Polar coordinates of the touch point from joystick center
    private var radial = 0.0
    private var angle = 0.0

    //User coordinates of last touch point
    private var userX = 0
    private var userY = 0

    //Offset co-ordinates (used when touch events are received from parent's coordinate origin)
    private var offsetX = 0
    private var offsetY = 0

    private val destRect = Rect()
    private var balltopBitmap:Bitmap = BitmapFactory.decodeResource(context.resources,
        R.drawable.balltop_black
    )

    // =========================================
    // Constructors
    // =========================================
    constructor(context: Context?) : super(context) {
        initJoystickView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initJoystickView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initJoystickView()
    }

    // =========================================
    // Initialization
    // =========================================
    private fun initJoystickView() {
        isFocusable = true
        bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint!!.color = Color.GRAY
        bgPaint!!.strokeWidth = 1f
        bgPaint!!.style = Paint.Style.FILL_AND_STROKE
        handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        handlePaint!!.color = Color.DKGRAY
        handlePaint!!.strokeWidth = 1f
        handlePaint!!.style = Paint.Style.FILL_AND_STROKE
        stickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        stickPaint!!.color = Color.rgb(0x30, 0x30, 0x40)
        stickPaint!!.strokeWidth = 80f
        stickPaint!!.style = Paint.Style.FILL_AND_STROKE
        basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        basePaint!!.color = Color.rgb(0x40, 0x40, 0x20)
        basePaint!!.strokeWidth = 1f
        basePaint!!.style = Paint.Style.FILL_AND_STROKE
        innerPadding = 10
        movementRange = 10f
        moveResolution = 1.0f
        setClickThreshold(0.4f)
        isYAxisInverted = true
        setUserCoordinateSystem(COORDINATE_CARTESIAN)
        isAutoReturnToCenter = true
    }

    fun setHandleColor(color: Int) {
        balltopBitmap = when(color) {
            Color.RED -> BitmapFactory.decodeResource(context.resources,
                R.drawable.balltop_red
            )
            Color.GREEN -> BitmapFactory.decodeResource(context.resources,
                R.drawable.balltop_green
            )
            Color.WHITE -> BitmapFactory.decodeResource(context.resources,
                R.drawable.balltop_white
            )
            Color.YELLOW -> BitmapFactory.decodeResource(context.resources,
                R.drawable.balltop_yellow
            )
            Color.BLUE -> BitmapFactory.decodeResource(context.resources,
                R.drawable.balltop_blue
            )
            else -> BitmapFactory.decodeResource(context.resources,
                R.drawable.balltop_black
            )

        }
    }

    fun setUserCoordinateSystem(userCoordinateSystem: Int) {
        if (userCoordinateSystem < COORDINATE_CARTESIAN || movementConstraint > COORDINATE_DIFFERENTIAL) Log.e(
            TAG,
            "invalid value for userCoordinateSystem"
        ) else this.userCoordinateSystem = userCoordinateSystem
    }

    fun getUserCoordinateSystem(): Int {
        return userCoordinateSystem
    }

    fun setMovementConstraint(movementConstraint: Int) {
        if (movementConstraint < CONSTRAIN_BOX || movementConstraint > CONSTRAIN_CIRCLE) Log.e(
            TAG,
            "invalid value for movementConstraint"
        ) else this.movementConstraint = movementConstraint
    }

    fun getMovementConstraint(): Int {
        return movementConstraint
    }

    /**
     * Set the pressure sensitivity for registering a click
     * @param clickThreshold threshold 0...1.0f inclusive. 0 will cause clicks to never be reported, 1.0 is a very hard click
     */
    fun setClickThreshold(clickThreshold: Float) {
        if (clickThreshold < 0 || clickThreshold > 1.0f) Log.e(
            TAG,
            "clickThreshold must range from 0...1.0f inclusive"
        ) else this.clickThreshold = clickThreshold
    }

    fun getClickThreshold(): Float {
        return clickThreshold
    }

    // =========================================
    // Public Methods
    // =========================================
    fun setOnJoystickMovedListener(listener: JoystickMovedListener?) {
        moveListener = listener
    }

    fun setOnJoystickClickedListener(listener: JoystickClickedListener?) {
        clickListener = listener
    }

    // =========================================
    // Drawing Functionality
    // =========================================
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Here we make sure that we have a perfect circle
        val measuredWidth = measure(widthMeasureSpec)
        val measuredHeight = measure(heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val d = measuredWidth.coerceAtMost(measuredHeight)
        dimX = d
        //dimY = d;
        cX = d / 2
        cY = d / 2
        bgRadius = dimX / 2 - innerPadding
        //handleRadius = (int)(d * 0.25);
        handleRadius = (d * 0.22).toInt()
        handleInnerBoundaries = handleRadius
        movementRadius = cX.coerceAtMost(cY) - handleInnerBoundaries
    }

    private fun measure(measureSpec: Int): Int {
        var result = 0
        // Decode the measurement specifications.
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        result = if (specMode == MeasureSpec.UNSPECIFIED) {
            // Return a default size of 200 if no bounds are specified.
            200
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            specSize
        }
        return result
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        // Draw the background
        canvas.drawCircle(cX.toFloat(), cY.toFloat(), bgRadius.toFloat(), bgPaint!!)

        // Draw the handle
        handleX = touchX + cX
        handleY = touchY + cY
        canvas.drawCircle(cX.toFloat(), cY.toFloat(), (handleRadius shr 1).toFloat(), basePaint!!)
        canvas.drawLine(cX.toFloat(), cY.toFloat(), handleX, handleY, stickPaint!!)
        canvas.drawCircle(cX.toFloat(), cY.toFloat(), 8f, stickPaint!!)

        destRect.set((handleX - handleRadius).toInt(),
            (handleY - handleRadius.toFloat()).toInt(),
            (handleX + handleRadius.toFloat()).toInt(), (handleY + handleRadius.toFloat()).toInt()
        )
        //canvas.drawCircle(handleX, handleY, handleRadius.toFloat(), handlePaint!!)
        canvas.drawBitmap(balltopBitmap!!, null, destRect, handlePaint)
        if (D) {
            Log.d(TAG, String.format("(%.0f, %.0f)", touchX, touchY))
            Log.d(TAG, String.format("(%.0f, %.0f\u00B0)", radial, angle * 180.0 / Math.PI))
        }

//		Log.d(TAG, String.format("touch(%f,%f)", touchX, touchY));
//		Log.d(TAG, String.format("onDraw(%.1f,%.1f)\n\n", handleX, handleY));
        canvas.restore()
    }

    // Constrain touch within a box
    private fun constrainBox() {
        touchX =
            touchX.coerceAtMost(movementRadius.toFloat()).coerceAtLeast(-movementRadius.toFloat())
        touchY =
            touchY.coerceAtMost(movementRadius.toFloat()).coerceAtLeast(-movementRadius.toFloat())
    }

    // Constrain touch within a circle
    private fun constrainCircle() {
        val diffX = touchX
        val diffY = touchY
        val radial = sqrt((diffX * diffX + diffY * diffY).toDouble())
        if (radial > movementRadius) {
            touchX = ((diffX / radial * movementRadius).toFloat())
            touchY = ((diffY / radial * movementRadius).toFloat())
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                return processMoveEvent(ev)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (pointerId != INVALID_POINTER_ID) {
//			    	Log.d(TAG, "ACTION_UP");
                    returnHandleToCenter()
                    pointerId = INVALID_POINTER_ID
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (pointerId != INVALID_POINTER_ID) {
                    val pointerIndex =
                        action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                    var pointerId = ev.getPointerId(pointerIndex)
                    if (pointerId == this.pointerId) {
//			        	Log.d(TAG, "ACTION_POINTER_UP: " + pointerId);
                        returnHandleToCenter()
                        pointerId = INVALID_POINTER_ID
                        return true
                    }
                }
            }
            MotionEvent.ACTION_DOWN -> {
                if (pointerId == INVALID_POINTER_ID) {
                    val x = ev.x.toInt()
                    if (x >= offsetX && x < offsetX + dimX) {
                        pointerId = ev.getPointerId(0)
                        //			        	Log.d(TAG, "ACTION_DOWN: " + getPointerId());
                        return true
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerId == INVALID_POINTER_ID) {
                    val pointerIndex =
                        action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                    val pointerId = ev.getPointerId(pointerIndex)
                    val x = ev.getX(pointerId).toInt()
                    if (x >= offsetX && x < offsetX + dimX) {
//			        	Log.d(TAG, "ACTION_POINTER_DOWN: " + pointerId);
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun processMoveEvent(ev: MotionEvent): Boolean {
        if (pointerId != INVALID_POINTER_ID) {
            val pointerIndex = ev.findPointerIndex(pointerId)

            // Translate touch position to center of view
            val x = ev.getX(pointerIndex)
            touchX = x - cX - offsetX
            val y = ev.getY(pointerIndex)
            touchY = y - cY - offsetY

//        	Log.d(TAG, String.format("ACTION_MOVE: (%03.0f, %03.0f) => (%03.0f, %03.0f)", x, y, touchX, touchY));
            reportOnMoved()
            invalidate()
            touchPressure = ev.getPressure(pointerIndex)
            reportOnPressure()
            return true
        }
        return false
    }

    private fun reportOnMoved() {
        if (movementConstraint == CONSTRAIN_CIRCLE) constrainCircle() else constrainBox()
        calcUserCoordinates()
        if (moveListener != null) {
            val rx = abs(touchX - reportX) >= moveResolution
            val ry = abs(touchY - reportY) >= moveResolution
            if (rx || ry) {
                reportX = touchX
                reportY = touchY

//				Log.d(TAG, String.format("moveListener.OnMoved(%d,%d)", (int)userX, (int)userY));
                //moveListener.OnMoved(userX, userY);
                moveListener!!.OnMoved(cartX, cartY)
            }
        }
    }

    private fun calcUserCoordinates() {
        //First convert to cartesian coordinates
        cartX = (touchX / movementRadius * movementRange).toInt()
        cartY = (touchY / movementRadius * movementRange).toInt()
        radial = sqrt((cartX * cartX + cartY * cartY).toDouble())
        angle = atan2(cartY.toDouble(), cartX.toDouble())

        //Invert Y axis if requested
        if (!isYAxisInverted) cartY *= -1
        if (userCoordinateSystem == COORDINATE_CARTESIAN) {
            userX = cartX
            userY = cartY
        } else if (userCoordinateSystem == COORDINATE_DIFFERENTIAL) {
            userX = cartY + cartX / 4
            userY = cartY - cartX / 4
            if (userX < -movementRange) userX = (-movementRange).toInt()
            if (userX > movementRange) userX = movementRange.toInt()
            if (userY < -movementRange) userY = (-movementRange).toInt()
            if (userY > movementRange) userY = movementRange.toInt()
        }
    }

    //Simple pressure click
    private fun reportOnPressure() {
//		Log.d(TAG, String.format("touchPressure=%.2f", this.touchPressure));
        if (clickListener != null) {
            if (clicked && touchPressure < clickThreshold) {
                clickListener!!.OnReleased()
                clicked = false
                //				Log.d(TAG, "reset click");
                invalidate()
            } else if (!clicked && touchPressure >= clickThreshold) {
                clicked = true
                clickListener!!.OnClicked()
                //				Log.d(TAG, "click");
                invalidate()
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }

    private fun returnHandleToCenter() {
        if (isAutoReturnToCenter) {
            val numberOfFrames = 5
            val intervalsX = ((0 - touchX) / numberOfFrames).toDouble()
            val intervalsY = ((0 - touchY) / numberOfFrames).toDouble()
            for (i in 0 until numberOfFrames) {
                postDelayed({
                    touchX += intervalsX.toFloat()
                    touchY += intervalsY.toFloat()
                    reportOnMoved()
                    invalidate()
                    if (moveListener != null && i == numberOfFrames - 1) {
                        moveListener!!.OnReturnedToCenter()
                    }
                }, (i * 40).toLong())
            }
            moveListener?.OnReleased()
        }
    }

    fun setTouchOffset(x: Int, y: Int) {
        offsetX = x
        offsetY = y
    }

    companion object {
        const val INVALID_POINTER_ID = -1

        //Max range of movement in user coordinate system
        const val CONSTRAIN_BOX = 0
        const val CONSTRAIN_CIRCLE = 1
        const val COORDINATE_CARTESIAN = 0 //Regular cartesian coordinates
        const val COORDINATE_DIFFERENTIAL =
            1 //Uses polar rotation of 45 degrees to calc differential drive parameters
    }
}
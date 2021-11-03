package com.denisferreira.bluetoothjoystick.ui.views


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import com.denisferreira.bluetoothjoystick.R


class ArcadeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    init {
        val transparent = ColorDrawable(resources.getColor(R.color.transparent))
        this.background = transparent
        this.setImageResource(R.drawable.arcade_button_red)
        this.scaleType = ScaleType.FIT_CENTER
        this.isClickable = true
    }

    override fun performClick(): Boolean {
        val mp = MediaPlayer.create(context, R.raw.arcade_click)
        mp.start()
        val res = super.performClick()
        mp.setOnCompletionListener {
            mp.release()
        }
        return res
    }

    fun setColor(color: Int) {
        when (color) {
            Color.BLUE -> this.setImageResource(R.drawable.arcade_button_blue)
            else -> this.setImageResource(R.drawable.arcade_button_red)

        }
    }


}
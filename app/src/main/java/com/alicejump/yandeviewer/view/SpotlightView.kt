package com.alicejump.yandeviewer.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.alicejump.yandeviewer.R

class SpotlightView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint()
    private val spotlightPaint = Paint()
    private var spotlightRect: RectF? = null
    private val cornerRadius = 16f // Radius for the rounded corners of the spotlight

    init {
        // Force software rendering to ensure PorterDuff.Mode.CLEAR works correctly.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // This paint will draw the semi-transparent background
        backgroundPaint.color = ContextCompat.getColor(context, R.color.spotlight_background)

        // This paint will "clear" the spotlight area
        spotlightPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        spotlightPaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // First, draw the semi-transparent background over the whole view
        canvas.drawPaint(backgroundPaint)

        // Then, if we have a spotlight position, "cut out" the spotlight area
        spotlightRect?.let {
            canvas.drawRoundRect(it, cornerRadius, cornerRadius, spotlightPaint)
        }
    }

    /**
     * Sets the position and size of the spotlight.
     * @param view The view to be highlighted. The spotlight will be drawn around its location.
     */
    fun setSpotlight(view: View) {
        val itemViewLocation = IntArray(2)
        view.getLocationInWindow(itemViewLocation)

        val spotlightViewLocation = IntArray(2)
        this.getLocationInWindow(spotlightViewLocation)

        val left = itemViewLocation[0] - spotlightViewLocation[0]
        val top = itemViewLocation[1] - spotlightViewLocation[1]

        spotlightRect = RectF(
            left.toFloat(),
            top.toFloat(),
            (left + view.width).toFloat(),
            (top + view.height).toFloat()
        )

        // Invalidate the view to trigger a redraw with the new spotlight position
        invalidate()
    }

    /**
     * Clears the spotlight, making the view just a solid semi-transparent overlay.
     */
    fun clearSpotlight() {
        spotlightRect = null
        invalidate()
    }
}

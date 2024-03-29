package com.d101.presentation.calendar.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.d101.domain.model.Fruit
import com.d101.presentation.R
import com.d101.presentation.calendar.adapter.LittleFruitImageUrl

private const val IMAGE_SIZE = 80

class JuiceGraph(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : View(
    context,
    attrs,
    defStyleAttr,
) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private var fruitList: List<Fruit> = emptyList()
    private val fruitBitmaps = mutableMapOf<LittleFruitImageUrl, Bitmap?>()
    private val basketDrawable = ContextCompat.getDrawable(context, R.drawable.basket)
    private val emptyBitmap = drawableToBitmap(basketDrawable)

    private val linePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        typeface = ResourcesCompat.getFont(context, R.font.binggrae_bold)
    }

    fun setFruitList(fruitList: List<Fruit>) {
        this.fruitList = fruitList
        fruitList.forEach { fruit ->
            loadBitmapForFruit(fruit.calendarImageUrl)
        }
        invalidate()
    }

    private fun loadBitmapForFruit(imageUrl: String) {
        Glide.with(context).asBitmap().load(imageUrl).override(IMAGE_SIZE, IMAGE_SIZE)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    fruitBitmaps[imageUrl] = resource
                    invalidate()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // X축 그리기
        canvas.drawLine(150f, height - 150f, width - 150f, height - 150f, linePaint)

        // Y축 그리기
        canvas.drawLine(150f, 150f, 150f, height - 150f, linePaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            "(Feel)",
            150f - (textPaint.textSize / 2),
            150f - (textPaint.textSize / 2),
            textPaint,
        )
        canvas.drawText(
            "good",
            150f - (textPaint.textSize / 2),
            150f + textPaint.textSize,
            textPaint,
        )
        canvas.drawText("bad", 150f - (textPaint.textSize / 2), height - 150f, textPaint)

        textPaint.textAlign = Paint.Align.LEFT
        // 눈금 및 라벨 추가
        val xIntervalCount = fruitList.lastIndex
        val yIntervalCount = 21
        val xInterval = (width - 400f) / xIntervalCount
        val yInterval = (height - 400f) / yIntervalCount

        for (index in 0..<xIntervalCount) {
            val x1 = 125f + xInterval + index * xInterval
            val y1 = height + 40f - 200f - (yInterval * fruitList[index].score)
            val x2 = 125f + xInterval + (index + 1) * xInterval
            val y2 = height + 40f - 200f - (yInterval * fruitList[index + 1].score)
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        for (index in fruitList.indices) {
            val x = 100f + (index + 1) * xInterval
            val dateString = fruitList[index].date.toString()
            val monthDay =
                if (dateString.length > 2) {
                    dateString.substring(dateString.length - 2)
                } else {
                    dateString
                }
            canvas.drawText((monthDay), x, height - (IMAGE_SIZE - 5).toFloat(), textPaint)
            val imageX = 125 + (index + 1) * xInterval
            val imageY = height - 200f - (yInterval * fruitList[index].score)

            fruitBitmaps[fruitList[index].calendarImageUrl]?.let { bitmap ->
                canvas.drawBitmap(bitmap, imageX - (IMAGE_SIZE / 2).toFloat(), imageY, null)
            } ?: run {
                canvas.drawBitmap(emptyBitmap, imageX - (IMAGE_SIZE / 2).toFloat(), imageY, null)
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap {
        val bitmap = Bitmap.createBitmap(IMAGE_SIZE - 10, IMAGE_SIZE - 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)
        return bitmap
    }
}

package com.workit.workit.ui.theme.custom

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.workit.workit.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TimePickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hour = 12
    private var minute = 0
    private var isSelectingHour = true
    private var onTimeSelectedListener: ((Int, Int) -> Unit)? = null

    // Paint objects
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.FILL
    }

    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val selectedNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val selectedBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 2 - 40

        // Draw outer circle
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // Draw numbers
        if (isSelectingHour) {
            drawHourNumbers(canvas, centerX, centerY, radius)
        } else {
            drawMinuteNumbers(canvas, centerX, centerY, radius)
        }

        // Draw center dot
        canvas.drawCircle(centerX, centerY, 15f, centerPaint)

        // Draw hand
        drawHand(canvas, centerX, centerY, radius)
    }

    private fun drawHourNumbers(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        for (h in 1..12) {
            val angle = (h * 30 - 90).toDouble() * Math.PI / 180
            val x = centerX + radius * 0.7f * cos(angle).toFloat()
            val y = centerY + radius * 0.7f * sin(angle).toFloat()

            if (h == hour) {
                canvas.drawCircle(x, y, 35f, selectedBgPaint)
                canvas.drawText(h.toString(), x, y + 15f, selectedNumberPaint)
            } else {
                canvas.drawText(h.toString(), x, y + 15f, textPaint)
            }
        }
    }

    private fun drawMinuteNumbers(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        for (m in 0..59 step 5) {
            val angle = (m * 6 - 90).toDouble() * Math.PI / 180
            val x = centerX + radius * 0.7f * cos(angle).toFloat()
            val y = centerY + radius * 0.7f * sin(angle).toFloat()

            if (m == minute) {
                canvas.drawCircle(x, y, 35f, selectedBgPaint)
                canvas.drawText(String.format("%02d", m), x, y + 15f, selectedNumberPaint)
            } else {
                canvas.drawText(String.format("%02d", m), x, y + 15f, textPaint)
            }
        }
    }

    private fun drawHand(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val angle = if (isSelectingHour) {
            ((hour % 12) * 30 + minute / 2 - 90).toDouble() * Math.PI / 180
        } else {
            (minute * 6 - 90).toDouble() * Math.PI / 180
        }

        val handLength = radius * 0.5f
        val endX = centerX + handLength * cos(angle).toFloat()
        val endY = centerY + handLength * sin(angle).toFloat()

        canvas.drawLine(centerX, centerY, endX, endY, handPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val centerX = width / 2f
            val centerY = height / 2f

            val x = event.x - centerX
            val y = event.y - centerY

            if (sqrt((x * x + y * y).toDouble()) > 50) {
                var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble()))
                angle = (angle + 90 + 360) % 360

                if (isSelectingHour) {
                    val newHour = ((angle / 30).toInt()) % 12
                    hour = if (newHour == 0) 12 else newHour
                } else {
                    minute = ((angle / 6).toInt()) % 60
                }

                invalidate()
            }
        }

        return true
    }

    fun setTime(hour: Int, minute: Int) {
        this.hour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        this.minute = minute
        invalidate()
    }

    fun getHour() = hour
    fun getMinute() = minute
    fun isSelectingHour() = isSelectingHour

    fun setSelectingHour(selecting: Boolean) {
        isSelectingHour = selecting
        invalidate()
    }

    fun setOnTimeSelectedListener(listener: (Int, Int) -> Unit) {
        onTimeSelectedListener = listener
    }
}

class CustomTimePickerDialog(
    private val context: Context,
    private var initialHour: Int = 12,
    private var initialMinute: Int = 0,
    private val onTimeSelected: (Int, Int) -> Unit
) : Dialog(context, android.R.style.Theme_DeviceDefault_Light_Dialog) {

    private lateinit var timePickerView: TimePickerView
    private lateinit var tvTime: TextView
    private lateinit var tvHourMinute: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var btnAM: Button
    private lateinit var btnPM: Button

    private var isAM = initialHour < 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create dialog layout
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                800, // width
                950  // height - increased for AM/PM buttons
            )
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.WHITE)
        }

        // Title
        val tvTitle = TextView(context).apply {
            text = "Select Time"
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            setTextColor(Color.parseColor("#333333"))
        }

        // Time Display
        tvTime = TextView(context).apply {
            text = "Selecting Hour"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
            setTextColor(Color.parseColor("#666666"))
        }

        tvHourMinute = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            textSize = 32f
            setTextColor(Color.parseColor("#4CAF50"))
        }

        // AM/PM Toggle Buttons
        val amPmLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        btnAM = Button(context).apply {
            text = "AM"
            layoutParams = LinearLayout.LayoutParams(
                0,
                48,
                1f
            ).apply {
                rightMargin = 4
            }
            setTextColor(Color.WHITE)
            setOnClickListener {
                isAM = true
                updateAMPMButtons()
                updateTimeDisplay()
            }
        }

        btnPM = Button(context).apply {
            text = "PM"
            layoutParams = LinearLayout.LayoutParams(
                0,
                48,
                1f
            ).apply {
                leftMargin = 4
            }
            setTextColor(Color.WHITE)
            setOnClickListener {
                isAM = false
                updateAMPMButtons()
                updateTimeDisplay()
            }
        }

        amPmLayout.addView(btnAM)
        amPmLayout.addView(btnPM)

        // Time Picker View
        timePickerView = TimePickerView(context).apply {
            val display12Hour = if (initialHour == 0) 12 else if (initialHour > 12) initialHour - 12 else initialHour
            setTime(display12Hour, initialMinute)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
            ).apply {
                bottomMargin = 16
            }
        }

        // Mode Toggle Buttons
        val modeButtonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val btnHour = Button(context).apply {
            text = "Hour"
            layoutParams = LinearLayout.LayoutParams(
                0,
                48,
                1f
            ).apply {
                rightMargin = 4
            }
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                timePickerView.setSelectingHour(true)
                tvTime.text = "Selecting Hour"
                updateTimeDisplay()
            }
        }

        val btnMinute = Button(context).apply {
            text = "Minute"
            layoutParams = LinearLayout.LayoutParams(
                0,
                48,
                1f
            ).apply {
                leftMargin = 4
            }
            setBackgroundColor(Color.parseColor("#1976D2"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                timePickerView.setSelectingHour(false)
                tvTime.text = "Selecting Minute"
                updateTimeDisplay()
            }
        }

        modeButtonsLayout.addView(btnHour)
        modeButtonsLayout.addView(btnMinute)

        // Buttons Layout
        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        btnCancel = Button(context).apply {
            text = "Cancel"
            layoutParams = LinearLayout.LayoutParams(
                0,
                48,
                1f
            ).apply {
                rightMargin = 4
            }
            setBackgroundColor(Color.parseColor("#F44336"))
            setTextColor(Color.WHITE)
            setOnClickListener { dismiss() }
        }

        btnConfirm = Button(context).apply {
            text = "OK"
            layoutParams = LinearLayout.LayoutParams(
                0,
                48,
                1f
            ).apply {
                leftMargin = 4
            }
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val selectedHour = timePickerView.getHour()
                val selectedMinute = timePickerView.getMinute()

                // Convert to 24-hour format
                val hour24 = when {
                    isAM && selectedHour == 12 -> 0
                    !isAM && selectedHour != 12 -> selectedHour + 12
                    else -> selectedHour
                }

                onTimeSelected(hour24, selectedMinute)
                dismiss()
            }
        }

        buttonsLayout.addView(btnCancel)
        buttonsLayout.addView(btnConfirm)

        // Add all views to root layout
        rootLayout.addView(tvTitle)
        rootLayout.addView(tvTime)
        rootLayout.addView(tvHourMinute)
        rootLayout.addView(amPmLayout)
        rootLayout.addView(timePickerView)
        rootLayout.addView(modeButtonsLayout)
        rootLayout.addView(buttonsLayout)

        setContentView(rootLayout)

        // Initialize AM/PM buttons and display
        isAM = initialHour < 12
        updateAMPMButtons()
        updateTimeDisplay()
    }

    private fun updateAMPMButtons() {
        if (isAM) {
            btnAM.setBackgroundColor(Color.parseColor("#4CAF50"))
            btnPM.setBackgroundColor(Color.parseColor("#CCCCCC"))
        } else {
            btnAM.setBackgroundColor(Color.parseColor("#CCCCCC"))
            btnPM.setBackgroundColor(Color.parseColor("#4CAF50"))
        }
    }

    private fun updateTimeDisplay() {
        val hour = timePickerView.getHour()
        val minute = timePickerView.getMinute()
        val amPmText = if (isAM) "AM" else "PM"

        tvHourMinute.text = String.format("%d:%02d %s", hour, minute, amPmText)
    }
}
package com.chesschat.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream

class MoveDetectionOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var moveLogTextView: TextView? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var previousBitmap: Bitmap? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isDetecting = false
    
    private var boardX = 50
    private var boardY = 300
    private var boardSize = 800
    private var isFlipped = false
    
    private val DETECTION_INTERVAL = 1000L
    private val detectionRunnable = object : Runnable {
        override fun run() {
            if (isDetecting) {
                captureScreen()
                handler.postDelayed(this, DETECTION_INTERVAL)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val resultCode = it.getIntExtra("resultCode", 0)
            val data = it.getParcelableExtra<Intent>("data")
            val x = it.getIntExtra("boardX", 50)
            val y = it.getIntExtra("boardY", 300)
            val size = it.getIntExtra("boardSize", 800)
            
            boardX = x
            boardY = y
            boardSize = size
            
            if (resultCode != 0 && data != null) {
                startScreenCapture(resultCode, data)
            }
        }
        return START_STICKY
    }

    private fun createOverlayView() {
        val layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 100

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E0FFFFFF"))
            setPadding(16, 16, 16, 16)
        }

        val controlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val startButton = Button(this).apply {
            text = "▶ Start"
            setOnClickListener {
                startDetection()
            }
        }

        val stopButton = Button(this).apply {
            text = "⏹ Stop"
            setOnClickListener {
                stopDetection()
            }
        }

        val flipButton = Button(this).apply {
            text = "🔄 Flip"
            setOnClickListener {
                isFlipped = !isFlipped
                logMove("Board flipped: ${if (isFlipped) "Black bottom" else "White bottom"}")
            }
        }

        val closeButton = Button(this).apply {
            text = "✖"
            setOnClickListener {
                stopSelf()
            }
        }

        controlsLayout.addView(startButton)
        controlsLayout.addView(stopButton)
        controlsLayout.addView(flipButton)
        controlsLayout.addView(closeButton)

        val scrollView = ScrollView(this)
        scrollView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            400
        )

        moveLogTextView = TextView(this).apply {
            text = "Move Detection Ready\n"
            textSize = 12f
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
        }

        scrollView.addView(moveLogTextView)
        container.addView(controlsLayout)
        container.addView(scrollView)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(container, layoutParams)
                    true
                }
                else -> false
            }
        }

        overlayView = container
        windowManager?.addView(overlayView, layoutParams)
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ChessMoveDetection",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        logMove("Screen capture initialized\nBoard area: x=$boardX, y=$boardY, size=$boardSize")
    }

    private fun startDetection() {
        isDetecting = true
        logMove("Detection started...")
        handler.post(detectionRunnable)
    }

    private fun stopDetection() {
        isDetecting = false
        handler.removeCallbacks(detectionRunnable)
        logMove("Detection stopped")
    }

    private fun captureScreen() {
        var fullBitmap: Bitmap? = null
        try {
            val image = imageReader?.acquireLatestImage() ?: return
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            fullBitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            fullBitmap.copyPixelsFromBuffer(buffer)
            image.close()

            val boardBitmap = extractBoardArea(fullBitmap)
            fullBitmap.recycle()
            
            if (previousBitmap != null) {
                val move = detectMove(previousBitmap!!, boardBitmap)
                if (move != null) {
                    logMove("Move detected: $move")
                }
                previousBitmap?.recycle()
            }
            
            previousBitmap = boardBitmap
        } catch (e: Exception) {
            logMove("Error: ${e.message}")
            fullBitmap?.recycle()
        }
    }

    private fun extractBoardArea(fullBitmap: Bitmap): Bitmap {
        return try {
            Bitmap.createBitmap(fullBitmap, boardX, boardY, boardSize, boardSize)
        } catch (e: Exception) {
            logMove("Error extracting board: ${e.message}")
            fullBitmap
        }
    }

    private fun detectMove(oldBoard: Bitmap, newBoard: Bitmap): String? {
        val squareSize = oldBoard.width / 8
        val changes = mutableListOf<Pair<Int, Int>>()

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val x = col * squareSize
                val y = row * squareSize
                
                if (hasSquareChanged(oldBoard, newBoard, x, y, squareSize)) {
                    changes.add(Pair(row, col))
                }
            }
        }

        if (changes.size == 2) {
            val from = changes[0]
            val to = changes[1]
            return squareToUCI(from.first, from.second, isFlipped) + 
                   squareToUCI(to.first, to.second, isFlipped)
        }

        return null
    }

    private fun hasSquareChanged(old: Bitmap, new: Bitmap, x: Int, y: Int, size: Int): Boolean {
        var diffPixels = 0
        val totalPixels = size * size
        val threshold = 0.15

        for (dy in 0 until size) {
            for (dx in 0 until size) {
                if (x + dx < old.width && y + dy < old.height &&
                    x + dx < new.width && y + dy < new.height) {
                    
                    val oldPixel = old.getPixel(x + dx, y + dy)
                    val newPixel = new.getPixel(x + dx, y + dy)
                    
                    if (colorDifference(oldPixel, newPixel) > 30) {
                        diffPixels++
                    }
                }
            }
        }

        return (diffPixels.toFloat() / totalPixels) > threshold
    }

    private fun colorDifference(color1: Int, color2: Int): Int {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2)
    }

    private fun squareToUCI(row: Int, col: Int, flipped: Boolean): String {
        val actualRow = if (flipped) row else 7 - row
        val actualCol = if (flipped) 7 - col else col
        
        val file = ('a' + actualCol).toString()
        val rank = (actualRow + 1).toString()
        
        return "$file$rank"
    }

    private fun logMove(message: String) {
        handler.post {
            moveLogTextView?.append("$message\n")
            (moveLogTextView?.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "move_detection",
                "Chess Move Detection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "move_detection")
            .setContentTitle("Chess Move Detection Active")
            .setContentText("Detecting moves in overlay mode")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDetection()
        overlayView?.let { windowManager?.removeView(it) }
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        previousBitmap?.recycle()
    }
}

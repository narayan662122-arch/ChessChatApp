package com.chesschat.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var chatTextView: TextView
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var menuButton: Button
    private lateinit var menuOverlay: View
    private lateinit var startButton: Button
    private lateinit var blackButton: Button
    private lateinit var whiteButton: Button
    private lateinit var detectButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()
    private var baseUrl: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var gameStarted: Boolean = false
    private var menuHideRunnable: Runnable? = null

    private val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    private val SCREEN_CAPTURE_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatTextView = findViewById(R.id.chatTextView)
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        menuButton = findViewById(R.id.menuButton)
        menuOverlay = findViewById(R.id.menuOverlay)
        startButton = findViewById(R.id.startButton)
        blackButton = findViewById(R.id.blackButton)
        whiteButton = findViewById(R.id.whiteButton)
        detectButton = findViewById(R.id.detectButton)

        sharedPreferences = getSharedPreferences("ChessChatPrefs", Context.MODE_PRIVATE)
        baseUrl = sharedPreferences.getString("base_url", "") ?: ""

        checkOverlayPermission()

        if (baseUrl.isEmpty()) {
            showBaseUrlDialog()
        } else {
            appendToChat("🔥 Brutal Chess Engine Chat Started!\n")
            appendToChat("Connected to: $baseUrl\n")
            appendToChat("Press ⋮ to see options.\n")
        }

        sendButton.setOnClickListener {
            sendMove()
        }

        menuButton.setOnClickListener {
            toggleMenu()
        }

        startButton.setOnClickListener {
            sendStartCommand()
            hideMenu()
        }

        blackButton.setOnClickListener {
            sendColorCommand("black")
            hideMenu()
        }

        whiteButton.setOnClickListener {
            sendColorCommand("white")
            hideMenu()
        }

        detectButton.setOnClickListener {
            showBoardConfigDialog()
            hideMenu()
        }

        inputEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                sendMove()
                true
            } else {
                false
            }
        }
    }

    private fun showBoardConfigDialog() {
        val dialogView = layoutInflater.inflate(R.layout.board_config_dialog, null)
        val xInput = dialogView.findViewById<EditText>(R.id.boardXInput)
        val yInput = dialogView.findViewById<EditText>(R.id.boardYInput)
        val sizeInput = dialogView.findViewById<EditText>(R.id.boardSizeInput)

        val savedX = sharedPreferences.getInt("board_x", 12)
        val savedY = sharedPreferences.getInt("board_y", 502)
        val savedSize = sharedPreferences.getInt("board_size", 698)

        xInput.setText(savedX.toString())
        yInput.setText(savedY.toString())
        sizeInput.setText(savedSize.toString())

        AlertDialog.Builder(this)
            .setTitle("Configure Chessboard Area")
            .setMessage("Your board coordinates:\nX: 12-710, Y: 502-1203\nSize: 698×701px\n\nAdjust if needed:")
            .setView(dialogView)
            .setPositiveButton("Start Detection") { dialog, _ ->
                val x = xInput.text.toString().toIntOrNull() ?: 12
                val y = yInput.text.toString().toIntOrNull() ?: 502
                val size = sizeInput.text.toString().toIntOrNull() ?: 698

                sharedPreferences.edit()
                    .putInt("board_x", x)
                    .putInt("board_y", y)
                    .putInt("board_size", size)
                    .apply()

                startMoveDetection(x, y, size)
                dialog.dismiss()
            }
            .setNegativeButton("Use Defaults") { dialog, _ ->
                sharedPreferences.edit()
                    .putInt("board_x", 12)
                    .putInt("board_y", 502)
                    .putInt("board_size", 698)
                    .apply()
                
                startMoveDetection(12, 502, 698)
                dialog.dismiss()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun startMoveDetection(boardX: Int, boardY: Int, boardSize: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                checkOverlayPermission()
                return
            }
        }

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            SCREEN_CAPTURE_REQUEST_CODE
        )
        
        sharedPreferences.edit()
            .putInt("pending_board_x", boardX)
            .putInt("pending_board_y", boardY)
            .putInt("pending_board_size", boardSize)
            .apply()
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                
                AlertDialog.Builder(this)
                    .setTitle("Overlay Permission Required")
                    .setMessage("This app needs permission to display content over other apps for move detection overlay.")
                    .setPositiveButton("Grant Permission") { dialog, _ ->
                        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        Toast.makeText(this, "Overlay permission denied. Some features may not work.", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                appendToChat("✓ Overlay permission granted\n")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        appendToChat("✓ Overlay permission granted\n")
                        Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show()
                    } else {
                        appendToChat("⚠️ Overlay permission denied\n")
                        Toast.makeText(this, "Overlay permission denied.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            SCREEN_CAPTURE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val boardX = sharedPreferences.getInt("pending_board_x", 12)
                    val boardY = sharedPreferences.getInt("pending_board_y", 502)
                    val boardSize = sharedPreferences.getInt("pending_board_size", 698)
                    
                    val serviceIntent = Intent(this, MoveDetectionOverlayService::class.java).apply {
                        putExtra("resultCode", resultCode)
                        putExtra("data", data)
                        putExtra("boardX", boardX)
                        putExtra("boardY", boardY)
                        putExtra("boardSize", boardSize)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    
                    Toast.makeText(this, "Move detection overlay started!", Toast.LENGTH_SHORT).show()
                    appendToChat("✓ Move detection active\n")
                    appendToChat("Board: x=$boardX, y=$boardY, size=$boardSize\n")
                } else {
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showBaseUrlDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Server URL")
        builder.setMessage("Please enter your chess engine server URL\n(e.g., https://xxxx.ngrok-free.app)")

        val input = EditText(this)
        input.hint = "https://xxxx.ngrok-free.app"
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val url = input.text.toString().trim()
            if (url.isNotEmpty()) {
                baseUrl = url
                sharedPreferences.edit().putString("base_url", url).apply()
                appendToChat("🔥 Brutal Chess Engine Chat Started!\n")
                appendToChat("Connected to: $url\n")
                appendToChat("Press ⋮ to see options.\n")
                dialog.dismiss()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
            finish()
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun toggleMenu() {
        if (menuOverlay.visibility == View.VISIBLE) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    private fun showMenu() {
        menuOverlay.visibility = View.VISIBLE
        
        menuHideRunnable?.let { handler.removeCallbacks(it) }
        
        menuHideRunnable = Runnable {
            hideMenu()
        }
        handler.postDelayed(menuHideRunnable!!, 5000)
    }

    private fun hideMenu() {
        menuOverlay.visibility = View.GONE
        menuHideRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun sendStartCommand() {
        if (baseUrl.isEmpty()) {
            appendToChat("⚠️ Error: Server URL not set\n")
            return
        }

        appendToChat("You: start\n")

        Thread {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/start")
                    .post("".toRequestBody(null))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        handler.post {
                            appendToChat("⚠️ Error: ${e.message}\n")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: "No response"
                        handler.post {
                            if (response.isSuccessful) {
                                appendToChat("Engine: $responseBody\n")
                                gameStarted = true
                            } else {
                                appendToChat("⚠️ Error (${response.code}): $responseBody\n")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                handler.post {
                    appendToChat("⚠️ Error: ${e.message}\n")
                }
            }
        }.start()
    }

    private fun sendColorCommand(color: String) {
        if (baseUrl.isEmpty()) {
            appendToChat("⚠️ Error: Server URL not set\n")
            return
        }

        if (!gameStarted) {
            appendToChat("⚠️ Type 'start' to begin the game first.\n")
            return
        }

        appendToChat("You: $color\n")

        Thread {
            try {
                val requestBody = color.toRequestBody(null)

                val request = Request.Builder()
                    .url("$baseUrl/move")
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        handler.post {
                            appendToChat("⚠️ Error: ${e.message}\n")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: "No response"
                        handler.post {
                            if (response.isSuccessful) {
                                appendToChat("Engine: $responseBody\n")
                            } else {
                                appendToChat("⚠️ Error (${response.code}): $responseBody\n")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                handler.post {
                    appendToChat("⚠️ Error: ${e.message}\n")
                }
            }
        }.start()
    }

    private fun sendMove() {
        val move = inputEditText.text.toString().trim()
        if (move.isEmpty()) return

        if (baseUrl.isEmpty()) {
            appendToChat("⚠️ Error: Server URL not set\n")
            return
        }

        if (!gameStarted) {
            appendToChat("⚠️ Press 'Start' button in menu (⋮) to begin the game first.\n")
            return
        }

        appendToChat("You: $move\n")
        inputEditText.text.clear()

        Thread {
            try {
                val requestBody = move.toRequestBody(null)

                val request = Request.Builder()
                    .url("$baseUrl/move")
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        handler.post {
                            appendToChat("⚠️ Error: ${e.message}\n")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: "No response"
                        handler.post {
                            if (response.isSuccessful) {
                                appendToChat("Engine: $responseBody\n")
                            } else {
                                appendToChat("⚠️ Error (${response.code}): $responseBody\n")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                handler.post {
                    appendToChat("⚠️ Error: ${e.message}\n")
                }
            }
        }.start()
    }

    private fun appendToChat(text: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        chatTextView.append("[$timestamp] $text")
    }

    override fun onDestroy() {
        super.onDestroy()
        menuHideRunnable?.let { handler.removeCallbacks(it) }
    }
}

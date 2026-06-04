package com.windowfactory

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var engine: GameEngine
    private lateinit var gameView: GameView
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        engine = GameEngine()
        gameView = GameView(this, engine)

        // Začni s 50 scoring
        engine.score = 50.0

        setContentView(gameView)
        startGameLoop()
    }

    private fun startGameLoop() {
        isRunning = true
        var lastTime = System.currentTimeMillis()

        val tickRunnable = object : Runnable {
            override fun run() {
                if (!isRunning) return

                val now = System.currentTimeMillis()
                val delta = (now - lastTime) / 1000.0
                lastTime = now

                // Obmedzenie delta na max 2s (ak bol telefón v spánku)
                val cappedDelta = Math.min(delta, 2.0)

                if (cappedDelta > 0) {
                    engine.tick(cappedDelta)
                }

                handler.postDelayed(this, 1000) // tik každú sekundu
            }
        }

        handler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        if (!isRunning) {
            startGameLoop()
        }
    }
}

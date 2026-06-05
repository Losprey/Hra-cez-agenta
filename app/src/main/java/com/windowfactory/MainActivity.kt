package com.windowfactory

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var engine: GameEngine
    private lateinit var gameView: GameView
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var pausedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("window_factory", Context.MODE_PRIVATE)

        engine = GameEngine()
        loadGame()

        gameView = GameView(this, engine)
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

                val cappedDelta = delta.coerceIn(0.0, 2.0)

                if (cappedDelta > 0) {
                    engine.tick(cappedDelta)
                }

                // Save every 30 ticks (~30s)
                if ((now / 30_000) != ((now - 1000) / 30_000)) {
                    saveGame()
                }

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        pausedAt = System.currentTimeMillis()
        saveGame()
    }

    override fun onResume() {
        super.onResume()

        val offlineMs = System.currentTimeMillis() - pausedAt
        if (offlineMs > 10_000 && pausedAt > 0) { // viac ako 10s preč
            val secsAway = offlineMs / 1000
            val msg = engine.computeOfflineEarnings(secsAway)
            if (msg.isNotEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        if (!isRunning) {
            startGameLoop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        saveGame()
    }

    // ── Uloženie / Načítanie ──
    private fun saveGame() {
        val e = editor()
        e.putLong("totalWindows", engine.totalWindows)
        e.putLong("totalPlaySeconds", engine.totalPlaySeconds)
        e.putLong("autoBuyActiveSeconds", engine.autoBuyActiveSeconds)
        e.putInt("prestigeLevel", engine.prestigeLevel)
        e.putInt("totalResearchLevels", engine.totalResearchLevels)
        e.putFloat("prestigeBonus", engine.prestigeBonus.toFloat())
        e.putFloat("score", engine.score.toFloat())
        e.putFloat("totalEarned", engine.totalEarned.toFloat())

        // Machines
        engine.allMachines().forEachIndexed { i, m ->
            e.putInt("m_${i}_level", m.level)
            e.putInt("m_${i}_count", m.count)
            e.putBoolean("m_${i}_unlocked", m.isUnlocked)
        }

        // Resources
        engine.resources.forEachIndexed { i, r ->
            e.putFloat("r_${i}_amount", r.amount.toFloat())
        }

        // Researches
        engine.researches.forEachIndexed { i, r ->
            e.putInt("res_${i}_level", r.level)
        }

        // Achievements
        engine.achievements.forEachIndexed { i, a ->
            e.putBoolean("ach_${i}", a.earned)
        }

        e.putBoolean("autoBuyEnabled", engine.autoBuyEnabled)
        e.apply()
    }

    private fun loadGame() {
        val p = prefs
        engine.totalWindows = p.getLong("totalWindows", 0)
        engine.totalPlaySeconds = p.getLong("totalPlaySeconds", 0)
        engine.autoBuyActiveSeconds = p.getLong("autoBuyActiveSeconds", 0)
        engine.prestigeLevel = p.getInt("prestigeLevel", 0)
        engine.totalResearchLevels = p.getInt("totalResearchLevels", 0)
        engine.prestigeBonus = p.getFloat("prestigeBonus", 1.0f).toDouble()
        engine.score = p.getFloat("score", 50.0f).toDouble()
        engine.totalEarned = p.getFloat("totalEarned", 0.0f).toDouble()

        // Machines
        engine.allMachines().forEachIndexed { i, m ->
            m.level = p.getInt("m_${i}_level", if (i == 0) 1 else 1)
            m.count = p.getInt("m_${i}_count", 1)
            m.isUnlocked = p.getBoolean("m_${i}_unlocked", i == 0)
        }

        // Resources
        engine.resources.forEachIndexed { i, r ->
            r.amount = p.getFloat("r_${i}_amount", 0.0f).toDouble()
        }

        // Researches
        engine.researches.forEachIndexed { i, r ->
            r.level = p.getInt("res_${i}_level", 0)
        }

        // Achievements
        engine.achievements.forEachIndexed { i, a ->
            a.earned = p.getBoolean("ach_${i}", false)
        }

        engine.autoBuyEnabled = p.getBoolean("autoBuyEnabled", true)
    }

    private fun editor(): SharedPreferences.Editor = prefs.edit()
}

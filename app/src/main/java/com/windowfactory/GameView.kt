package com.windowfactory

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class GameView(context: Context, val engine: GameEngine) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val paintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 24f
    }
    private val paintGold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 215, 0)
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val paintBtn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(76, 175, 80)
    }
    private val paintBtnText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val paintBtnLocked = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(100, 100, 100)
    }
    private val paintBg = Paint().apply {
        color = Color.rgb(44, 44, 44)
    }
    private val paintFloor = Paint().apply {
        color = Color.rgb(58, 58, 58)
    }

    // Tlačidlá
    data class Button(val x: Float, val y: Float, val w: Float, val h: Float, val label: String, val action: String, val machineIdx: Int)

    private val buttons = mutableListOf<Button>()

    private var animTime = 0f

    override fun onDraw(canvas: Canvas) {
        animTime += 0.05f
        val w = width.toFloat()
        val h = height.toFloat()

        // Pozadie
        canvas.drawRect(0f, 0f, w, h, paintBg)

        // ---- HORNÝ PANEL ----
        drawHeader(canvas, w)

        // ---- STROJE ----
        val machines = listOf(engine.piskovna, engine.pec)
        buttons.clear()
        val startY = 150f
        val machineH = 220f
        val gap = 20f

        machines.forEachIndexed { idx, machine ->
            val y = startY + idx * (machineH + gap)
            drawMachine(canvas, w, y, machineH, machine, idx)
        }

        // ---- PRESTÍŽ INFO ----
        val prestizY = startY + machines.size * (machineH + gap) + 20f
        paintSmall.textSize = 22f
        paintSmall.color = Color.rgb(255, 215, 0)
        canvas.drawText("🏆 Prestíž: ${engine.prestigeLevel} | Celkom okien: ${engine.formatNumber(engine.totalWindows.toDouble())}", 20f, prestizY, paintSmall)

        // ---- TIK ----
        invalidate()
    }

    private fun drawHeader(canvas: Canvas, w: Float) {
        // Score
        paintGold.textSize = 40f
        paintGold.color = Color.rgb(255, 215, 0)
        canvas.drawText("💰 ${engine.formatNumber(engine.score)}", 20f, 60f, paintGold)

        // Resources row
        paintSmall.textSize = 28f
        paintSmall.color = Color.WHITE
        val resStr = "🏖️ ${engine.formatNumber(engine.piesok.amount)}  |  🔵 ${engine.formatNumber(engine.sklovina.amount)}"
        canvas.drawText(resStr, 20f, 100f, paintSmall)

        // Výroba za sekundu
        paintSmall.textSize = 22f
        paintSmall.color = Color.LTGRAY
        var prodStr = "Výroba/s: ⛏️ ${engine.formatNumber(engine.piskovna.speed)}"
        if (engine.pec.isUnlocked) {
            prodStr += "  |  🔥 ${engine.formatNumber(engine.pec.speed)}"
        }
        canvas.drawText(prodStr, 20f, 130f, paintSmall)

        // Sell buttons
        val sellW = 80f
        val sellH = 40f
        val sellY = 140f
        paintBtnText.textSize = 20f

        // Sell piesok
        val sellSandX = 20f
        val canSellSand = engine.piesok.amount >= 1.0
        val sandBtnPaint = if (canSellSand) paintBtn else paintBtnLocked
        canvas.drawRoundRect(sellSandX, sellY, sellSandX + sellW, sellY + sellH, 10f, 10f, sandBtnPaint)
        canvas.drawText("💰", sellSandX + 28f, sellY + 28f, paintBtnText)
        buttons.add(Button(sellSandX, sellY, sellW, sellH, "sell", "sell_sand", 0))

        paintSmall.textSize = 18f
        paintSmall.color = Color.LTGRAY
        canvas.drawText("${engine.formatNumber(1.0)} piesku", sellSandX + sellW + 8f, sellY + 28f, paintSmall)
    }

    private fun drawMachine(canvas: Canvas, w: Float, y: Float, h: Float, machine: Machine, idx: Int) {
        val margin = 20f
        val innerW = w - 2 * margin

        // Machine background
        val bgPaint = if (machine.isUnlocked) Paint().apply { color = Color.rgb(50, 50, 60) }
                       else Paint().apply { color = Color.rgb(35, 35, 40) }
        canvas.drawRoundRect(margin, y, margin + innerW, y + h, 16f, 16f, bgPaint)

        // Názov a level
        paintText.textSize = 32f
        paintText.color = if (machine.isUnlocked) Color.WHITE else Color.GRAY
        val nameStr = "${machine.shortName} ${machine.name}"
        canvas.drawText(nameStr, margin + 20f, y + 40f, paintText)

        if (machine.isUnlocked && machine.level > 0) {
            paintSmall.textSize = 24f
            paintSmall.color = Color.rgb(100, 200, 255)
            canvas.drawText("Lv.${machine.level} ×${machine.count}", margin + 20f, y + 70f, paintSmall)

            // Rýchlosť
            paintSmall.textSize = 20f
            paintSmall.color = Color.LTGRAY
            canvas.drawText("${engine.formatNumber(machine.speed)}/s", margin + 20f, y + 95f, paintSmall)
        }

        // Vizuálna reprezentácia stroja (Canvas kreslenie)
        drawMachineVisual(canvas, margin + 20f, y + 105f, machine, idx)

        // Tlačidlá
        val btnW = 140f
        val btnH = 50f
        val btnY = y + h - btnH - 15f
        val btnX = w - margin - btnW - 20f

        if (machine.isUnlocked) {
            // Upgrade tlačidlo
            val cost = engine.formatNumber(machine.upgradeCost(machine.level))
            val canAfford = engine.score >= machine.upgradeCost(machine.level)
            val upPaint = if (canAfford) paintBtn else paintBtnLocked

            // Ak nie je level 20, ukáž upgrade
            if (machine.level < 20) {
                canvas.drawRoundRect(btnX, btnY, btnX + btnW, btnY + btnH, 12f, 12f, upPaint)
                paintBtnText.textSize = 22f
                canvas.drawText("⬆️ $cost", btnX + 10f, btnY + 33f, paintBtnText)
                buttons.add(Button(btnX, btnY, btnW, btnH, "upgrade", "upgrade", idx))
            }

            // Kúpiť ďalší stroj
            val buyCost = engine.formatNumber(machine.buyCost())
            val canBuy = engine.score >= machine.buyCost()
            val buyPaint = if (canBuy) paintBtn else paintBtnLocked
            val buyBtnX = btnX - btnW - 15f
            canvas.drawRoundRect(buyBtnX, btnY, buyBtnX + btnW, btnY + btnH, 12f, 12f, buyPaint)
            paintBtnText.textSize = 22f
            canvas.drawText("➕ $buyCost", buyBtnX + 10f, btnY + 33f, paintBtnText)
            buttons.add(Button(buyBtnX, btnY, btnW, btnH, "buy", "buy", idx))

        } else {
            // Odomknúť tlačidlo
            val unlockCost = engine.formatNumber(machine.baseCost * 3)
            val canUnlock = engine.score >= machine.baseCost * 3
            val lockPaint = if (canUnlock) paintBtn else paintBtnLocked
            canvas.drawRoundRect(btnX, btnY, btnX + btnW, btnY + btnH, 12f, 12f, lockPaint)
            paintBtnText.textSize = 22f
            canvas.drawText("🔓 $unlockCost", btnX + 10f, btnY + 33f, paintBtnText)
            buttons.add(Button(btnX, btnY, btnW, btnH, "unlock", "unlock", idx))

            // Zámok ikona
            paintText.textSize = 50f
            paintText.color = Color.GRAY
            canvas.drawText("🔒", margin + 20f, y + 160f, paintText)
        }
    }

    private fun drawMachineVisual(canvas: Canvas, x: Float, y: Float, machine: Machine, idx: Int) {
        when (idx) {
            0 -> drawSandPit(canvas, x, y, machine)
            1 -> drawFurnace(canvas, x, y, machine)
        }
    }

    private fun drawSandPit(canvas: Canvas, x: Float, y: Float, machine: Machine) {
        if (!machine.isUnlocked) return
        val pit = Paint().apply { color = Color.rgb(210, 180, 140) }
        val pitDark = Paint().apply { color = Color.rgb(180, 150, 110) }
        val pitLight = Paint().apply { color = Color.rgb(240, 220, 180) }

        // Jamy
        val cx = x + 40f
        val cy = y + 20f
        val wide = 40f + machine.level * 2f
        val high = 20f + machine.level.toFloat()

        canvas.drawOval(cx - wide / 2, cy - high / 2, cx + wide / 2, cy + high / 2, pitDark)
        canvas.drawOval(cx - wide / 2 + 4f, cy - high / 2 + 3f, cx + wide / 2 - 4f, cy + high / 2 - 3f, pit)

        // Piesok padá (animácia)
        val sandPaint = Paint().apply { color = Color.rgb(240, 220, 180) }
        if (machine.level >= 3) {
            val particles = (machine.level / 3).coerceAtMost(5)
            for (i in 0 until particles) {
                val px = x + 30f + ((animTime * 50 + i * 80) % 80f)
                val py = y + 60f + i * 8f + ((animTime * 30) % 20f)
                canvas.drawCircle(px, py, 3f, sandPaint)
            }
        }

        // Kôpka piesku (čím vyšší level, tým viac)
        paintSmall.textSize = 18f
        paintSmall.color = Color.rgb(200, 180, 140)
        val piles = (machine.level / 3).coerceAtLeast(1).coerceAtMost(3)
        for (i in 0 until piles) {
            val px = x + 80f + i * 25f
            val py = y + 35f
            canvas.drawCircle(px, py, 8f + machine.level.toFloat(), pitLight)
        }
    }

    private fun drawFurnace(canvas: Canvas, x: Float, y: Float, machine: Machine) {
        if (!machine.isUnlocked) return

        val body = Paint().apply { color = Color.rgb(80, 80, 90) }
        val door = Paint().apply { color = Color.rgb(60, 60, 70) }

        // Pec telo
        canvas.drawRoundRect(x, y + 5f, x + 80f, y + 50f, 8f, 8f, body)
        canvas.drawRoundRect(x + 10f, y + 12f, x + 70f, y + 45f, 4f, 4f, door)

        // Plameň (animácia) - čím vyšší level, tým viac
        val flamePaint = Paint().apply { color = Color.rgb(255, 100 + (animTime * 50).roundToInt() % 100, 0) }
        val flameH = 15f + machine.level * 1.5f
        val flameW = 10f + machine.level.toFloat()
        canvas.drawOval(x + 40f - flameW / 2, y - flameH, x + 40f + flameW / 2, y + 5f, flamePaint)

        // Komín (level >= 3)
        if (machine.level >= 3) {
            val chimney = Paint().apply { color = Color.rgb(70, 70, 80) }
            canvas.drawRect(x + 60f, y - 15f, x + 75f, y + 5f, chimney)
            // Dym
            val smokePaint = Paint().apply { color = Color.argb(80, 150, 150, 150) }
            canvas.drawCircle(x + 68f, y - 25f + (animTime * 10 % 15f), 6f, smokePaint)
        }

        // Sklo vo vnútri (level zvyšuje lesk)
        val glassPaint = Paint().apply { color = Color.argb(80, 100, 200, 255) }
        canvas.drawRect(x + 15f, y + 15f, x + 65f, y + 40f, glassPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            val machines = listOf(engine.piskovna, engine.pec)

            for (btn in buttons) {
                if (x >= btn.x && x <= btn.x + btn.w && y >= btn.y && y <= btn.y + btn.h) {
                    when (btn.action) {
                        "upgrade" -> {
                            if (btn.machineIdx < machines.size) {
                                engine.upgradeMachine(machines[btn.machineIdx])
                            }
                        }
                        "buy" -> {
                            if (btn.machineIdx < machines.size) {
                                engine.buyMachine(machines[btn.machineIdx])
                            }
                        }
                        "unlock" -> {
                            if (btn.machineIdx < machines.size) {
                                engine.unlockMachine(machines[btn.machineIdx])
                            }
                        }
                        "sell_sand" -> {
                            engine.sellSand()
                        }
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}

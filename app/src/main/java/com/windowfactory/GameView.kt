package com.windowfactory

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

data class Button(val x: Float, val y: Float, val w: Float, val h: Float, val action: String, val data: Int = 0)

class GameView(context: Context, val engine: GameEngine) : View(context) {

    private val paintW = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD }
    private val paintS = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 22f }
    private val paintG = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255,215,0); textSize = 36f; typeface = Typeface.DEFAULT_BOLD }
    private val paintBtn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(76,175,80) }
    private val paintBtnL = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(100,100,100) }
    private val paintBg = Paint().apply { color = Color.rgb(44,44,44) }
    private val paintMach = Paint().apply { color = Color.rgb(50,50,60) }

    private val buttons = mutableListOf<Button>()
    private var scrollY = 0f
    private var animTime = 0f

    override fun onDraw(canvas: Canvas) {
        animTime += 0.05f
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, paintBg)
        buttons.clear()

        // Header
        paintG.textSize = 36f
        canvas.drawText("💰 ${engine.formatNumber(engine.score)}", 20f, 50f, paintG)
        paintS.textSize = 22f
        canvas.drawText("🏖️${engine.formatNumber(engine.piesok.amount)} 🔵${engine.formatNumber(engine.sklovina.amount)} 🟢${engine.formatNumber(engine.hotoveOkno.amount)}", 20f, 85f, paintS)

        // Sell sklovina button
        drawSellBtn(canvas, w)

        // Machine list
        val machines = engine.allMachines()
        val mStart = 120f
        val mH = 190f
        val mGap = 12f

        // Scroll area
        canvas.save()
        canvas.clipRect(0f, mStart, w, h - 60f)
        canvas.translate(0f, scrollY)

        machines.forEachIndexed { idx, m ->
            val y = idx * (mH + mGap)
            drawMachine(canvas, w, y, mH, m, idx)
        }

        // Buyable resources
        val buyStart = machines.size * (mH + mGap) + 10f
        drawShop(canvas, w, buyStart)

        canvas.restore()

        // Scroll hint
        paintS.textSize = 18f
        paintS.color = Color.argb(100, 255, 255, 255)
        canvas.drawText("⬆⬇ scrolluj", w - 90f, h - 20f, paintS)

        invalidate()
    }

    private fun drawSellBtn(canvas: Canvas, w: Float) {
        val x = w - 100f
        val y = 10f
        val bw = 85f
        val bh = 40f
        val canSell = engine.sklovina.amount >= 1.0
        canvas.drawRoundRect(x, y, x + bw, y + bh, 10f, 10f, if (canSell) paintBtn else paintBtnL)
        paintW.textSize = 22f
        paintW.color = Color.WHITE
        canvas.drawText("💰 Sklovina", x + 6f, y + 28f, paintW)
        buttons.add(Button(x, y, bw, bh, "sell_glass"))
    }

    private fun drawMachine(canvas: Canvas, w: Float, y: Float, h: Float, m: Machine, idx: Int) {
        val margin = 12f
        val iw = w - 2 * margin

        canvas.drawRoundRect(margin, y, margin + iw, y + h, 12f, 12f, paintMach)

        // Názov + level
        paintW.textSize = 28f
        paintW.color = if (m.isUnlocked) Color.WHITE else Color.GRAY
        canvas.drawText("${m.shortName} ${m.name}", margin + 14f, y + 32f, paintW)

        if (m.isUnlocked && m.level > 0) {
            paintS.textSize = 20f
            paintS.color = Color.rgb(100, 200, 255)
            canvas.drawText("Lv.${m.level} ×${m.count} | ${engine.formatNumber(m.speed)}/s", margin + 14f, y + 58f, paintS)

            // Input/output info
            paintS.textSize = 17f
            paintS.color = Color.LTGRAY
            val info = when (idx) {
                0 -> "🏖️→ piesok"
                1 -> "🏖️→🔵 (0.8x)"
                2 -> "⚪→🤍"
                3 -> "🤍→⬜"
                4 -> "🔵+⬜+⚫+🔩→🟢"
                5 -> "🟢→💰 (15x)"
                else -> ""
            }
            canvas.drawText(info, margin + 14f, y + 80f, paintS)
        }

        // Stroj vzhľad
        drawMachineIcon(canvas, margin + 14f, y + 85f, m, idx)

        // Tlačidlá
        val btnW = 110f
        val btnH = 38f
        val btnY = y + h - btnH - 10f
        val btnX = w - margin - btnW - 10f

        if (!m.isUnlocked) {
            val cost = engine.formatNumber(m.baseCost * 3)
            val can = engine.score >= m.baseCost * 3
            canvas.drawRoundRect(btnX, btnY, btnX + btnW, btnY + btnH, 10f, 10f, if (can) paintBtn else paintBtnL)
            paintW.textSize = 18f
            canvas.drawText("🔓 $cost", btnX + 6f, btnY + 26f, paintW)
            buttons.add(Button(btnX, btnY, btnW, btnH, "unlock", idx))
            // Zámok
            paintW.textSize = 28f
            paintW.color = Color.GRAY
            canvas.drawText("🔒", margin + 14f, y + 150f, paintW)
        } else if (m.level < 20) {
            // Upgrade tlačidlo
            val cost = engine.formatNumber(m.upgradeCost())
            val can = engine.score >= m.upgradeCost()
            canvas.drawRoundRect(btnX, btnY, btnX + btnW, btnY + btnH, 10f, 10f, if (can) paintBtn else paintBtnL)
            paintW.textSize = 18f
            canvas.drawText("⬆️ $cost", btnX + 6f, btnY + 26f, paintW)
            buttons.add(Button(btnX, btnY, btnW, btnH, "upgrade", idx))

            // Buy tlačidlo
            val bX = btnX - btnW - 10f
            val bc = engine.formatNumber(m.buyCost())
            val bCan = engine.score >= m.buyCost()
            canvas.drawRoundRect(bX, btnY, bX + btnW, btnY + btnH, 10f, 10f, if (bCan) paintBtn else paintBtnL)
            paintW.textSize = 18f
            canvas.drawText("➕ $bc", bX + 6f, btnY + 26f, paintW)
            buttons.add(Button(bX, btnY, btnW, btnH, "buy", idx))
        }
    }

    private fun drawMachineIcon(canvas: Canvas, x: Float, y: Float, m: Machine, idx: Int) {
        if (!m.isUnlocked) return
        when (idx) {
            0 -> { // Pískovňa
                val pitC = Paint().apply { color = Color.rgb(210, 180, 140) }
                canvas.drawCircle(x + 25f, y + 20f, 18f + m.level.toFloat(), pitC)
                if (m.level >= 3) {
                    val sp = Paint().apply { color = Color.rgb(240, 220, 180) }
                    for (i in 0 until (m.level / 3).coerceAtMost(3)) {
                        canvas.drawCircle(x + 30f + i * 12f + (animTime * 20 % 20f), y + 10f - (animTime * 30 % 15f), 4f, sp)
                    }
                }
            }
            1 -> { // Pec
                val body = Paint().apply { color = Color.rgb(80, 80, 90) }
                canvas.drawRoundRect(x, y + 5f, x + 50f, y + 40f, 6f, 6f, body)
                val flame = Paint().apply { color = Color.rgb(255, 100 + (animTime * 50).roundToInt() % 100, 0) }
                canvas.drawOval(x + 18f, y - (5f + m.level.toFloat() * 0.5f), x + 32f, y + 6f, flame)
                if (m.level >= 3) {
                    val sm = Paint().apply { color = Color.argb(60, 150, 150, 150) }
                    canvas.drawCircle(x + 42f, y - 6f + (animTime * 8 % 10f), 5f, sm)
                }
            }
            2 -> { // Extrúzia
                val body = Paint().apply { color = Color.rgb(90, 90, 100) }
                canvas.drawRoundRect(x, y + 5f, x + 60f, y + 35f, 6f, 6f, body)
                val pipe = Paint().apply { color = Color.rgb(200, 200, 200) }
                canvas.drawRect(x + 20f, y - 5f, x + 40f, y + 7f, pipe)
            }
            3 -> { // Zváračka
                val body = Paint().apply { color = Color.rgb(100, 80, 70) }
                canvas.drawRoundRect(x, y + 5f, x + 55f, y + 38f, 6f, 6f, body)
                val spark = Paint().apply { color = Color.rgb(255, 200, 0) }
                if (animTime % 1f < 0.3f) {
                    canvas.drawCircle(x + 28f, y + 2f, 6f, spark)
                }
            }
            4 -> { // Montáž
                val body = Paint().apply { color = Color.rgb(60, 100, 70) }
                canvas.drawRoundRect(x, y + 3f, x + 65f, y + 38f, 6f, 6f, body)
                val belt = Paint().apply { color = Color.rgb(80, 80, 80) }
                canvas.drawRect(x + 5f, y + 18f, x + 60f, y + 22f, belt)
                val item = Paint().apply { color = Color.rgb(100, 200, 100) }
                canvas.drawRect(x + 10f + (animTime * 30 % 50f), y + 16f, x + 18f + (animTime * 30 % 50f), y + 24f, item)
            }
            5 -> { // Balenie
                val body = Paint().apply { color = Color.rgb(70, 70, 50) }
                canvas.drawRoundRect(x, y + 3f, x + 55f, y + 38f, 6f, 6f, body)
                val box = Paint().apply { color = Color.rgb(150, 120, 70) }
                canvas.drawRect(x + 15f, y + 10f, x + 40f, y + 30f, box)
                val coin = Paint().apply { color = Color.rgb(255, 215, 0) }
                canvas.drawCircle(x + 48f + (animTime * 20 % 10f), y + 20f, 4f, coin)
            }
        }
    }

    private fun drawShop(canvas: Canvas, w: Float, y: Float) {
        val buyable = engine.resources.filter { it.isBuyable }
        if (buyable.isEmpty()) return

        paintW.textSize = 24f
        paintW.color = Color.rgb(255, 215, 0)
        canvas.drawText("🛒 Obchod", 20f, y, paintW)

        var cy = y + 15f
        buyable.forEach { r ->
            val bw = 130f
            val bh = 36f
            val cost = engine.formatNumber(r.buyPrice * 5)
            val can = engine.score >= r.buyPrice * 5
            val bx = 20f
            canvas.drawRoundRect(bx, cy, bx + bw, cy + bh, 8f, 8f, if (can) paintBtn else paintBtnL)
            paintW.textSize = 18f
            paintW.color = Color.WHITE
            canvas.drawText("${r.emoji} +5 ($cost)", bx + 6f, cy + 24f, paintW)
            buttons.add(Button(bx, cy, bw, bh, "buy_res", engine.resources.indexOf(r)))

            paintS.textSize = 18f
            paintS.color = Color.LTGRAY
            canvas.drawText("Máš: ${engine.formatNumber(r.amount)}", bx + bw + 10f, cy + 24f, paintS)

            cy += bh + 8f
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y - scrollY // Convert to canvas space
                val machines = engine.allMachines()

                for (btn in buttons) {
                    if (x >= btn.x && x <= btn.x + btn.w && y >= btn.y && y <= btn.y + btn.h) {
                        when (btn.action) {
                            "upgrade" -> if (btn.data < machines.size) engine.upgradeMachine(machines[btn.data])
                            "buy" -> if (btn.data < machines.size) engine.buyMachine(machines[btn.data])
                            "unlock" -> if (btn.data < machines.size) engine.unlockMachine(machines[btn.data])
                            "sell_glass" -> engine.tickManualSell()
                            "buy_res" -> {
                                val res = engine.resources.getOrNull(btn.data)
                                if (res != null) engine.buyResource(res)
                            }
                        }
                        return true
                    }
                }
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastTouchY
                lastTouchY = event.y
                val mStart = 120f
                val mH = 190f + 12f
                val totalContent = engine.allMachines().size * mH + 200f
                val visibleH = height.toFloat() - mStart - 60f
                val maxScroll = Math.max(0f, totalContent - visibleH)
                scrollY = (scrollY + dy).coerceIn(-maxScroll, 0f)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private var lastTouchY = 0f
}

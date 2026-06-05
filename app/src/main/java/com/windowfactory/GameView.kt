package com.windowfactory

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

data class ButtonV2(val x: Float, val y: Float, val w: Float, val h: Float, val action: String, val data: Int = 0, val extra: String = "")

class GameView(context: Context, val engine: GameEngine) : View(context) {

    // Colours
    private val bg = Paint().apply { color = Color.rgb(44,44,44) }
    private val card = Paint().apply { color = Color.rgb(55,55,65) }
    private val cardAccent = Paint().apply { color = Color.rgb(70,70,85) }
    private val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(76,175,80) }
    private val greenDim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(56,135,60) }
    private val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255,215,0); typeface = Typeface.DEFAULT_BOLD }
    private val whiteB = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val gray = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY }
    private val grayDim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120,200,200,200) }
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(100,200,255) }
    private val orange = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255,165,0) }
    private val purple = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(180,130,255) }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255,80,80) }
    private val tabInactive = Paint().apply { color = Color.rgb(60,60,70) }
    private val tabActive = Paint().apply { color = Color.rgb(76,175,80) }

    private val buttons = mutableListOf<ButtonV2>()
    private var scrollY = 0f
    private var animTime = 0f
    private var toastText = ""
    private var toastTimer = 0

    // ── Draw ──
    override fun onDraw(canvas: Canvas) {
        animTime += 0.05f
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bg)
        buttons.clear()

        // Header
        drawHeader(canvas, w)
        // Tabs
        drawTabs(canvas, w, h)
        // Toast
        drawToast(canvas, w)

        // Check achievement toast from engine
        val ach = engine.consumeAchievementToast()
        if (ach != null) {
            toastText = ach
            toastTimer = 120 // frames ~2s
        }
        if (toastTimer > 0) {
            toastTimer--
            if (toastTimer == 0) toastText = ""
        }

        invalidate()
    }

    // ── Header ──
    private fun drawHeader(canvas: Canvas, w: Float) {
        gold.textSize = 32f
        canvas.drawText("💰 ${engine.formatNumber(engine.score)}", 16f, 38f, gold)
        if (engine.prestigeLevel > 0) {
            gray.textSize = 16f
            gray.color = Color.argb(150,180,130,255)
            canvas.drawText("⭐ P${engine.prestigeLevel}", 16f, 56f, gray)
            gray.color = Color.LTGRAY
        }
    }

    // ── Tabs ──
    private fun drawTabs(canvas: Canvas, w: Float, h: Float) {
        val tabH = 44f; val tabW = w / 3
        val tabY = h - tabH
        val tabs = listOf(
            Triple("🏭 Produkcia", GameTab.PRODUCTION, 0),
            Triple("🔬 Výskum", GameTab.RESEARCH, 1),
            Triple("📊 Štatistiky", GameTab.STATS, 2)
        )
        for ((i, (label, tab, idx)) in tabs.withIndex()) {
            val x = idx * tabW
            val active = engine.currentTab == tab
            canvas.drawRect(x, tabY, x + tabW, tabY + tabH, if (active) tabActive else tabInactive)
            if (idx > 0) {
                val line = Paint().apply { color = Color.argb(50,255,255,255) }
                canvas.drawRect(x, tabY, x + 1f, tabY + tabH, line)
            }
            white.textSize = 20f
            white.color = if (active) Color.WHITE else Color.argb(180,255,255,255)
            canvas.drawText(label, x + 12f, tabY + 29f, white)
            buttons.add(ButtonV2(x, tabY, tabW, tabH, "tab", idx))
        }

        // Content area
        canvas.save()
        canvas.clipRect(0f, 62f, w, tabY - 2f)
        canvas.translate(0f, scrollY)

        when (engine.currentTab) {
            GameTab.PRODUCTION -> drawProductionTab(canvas, w)
            GameTab.RESEARCH -> drawResearchTab(canvas, w)
            GameTab.STATS -> drawStatsTab(canvas, w)
        }

        canvas.restore()
    }

    // ── Production Tab ──
    private fun drawProductionTab(canvas: Canvas, w: Float) {
        val yBase = 0f
        // Resource bar
        gray.textSize = 18f
        val resText = buildString {
            append("🏖️${engine.formatNumber(engine.piesok.amount)} ")
            append("🔵${engine.formatNumber(engine.sklovina.amount)} ")
            append("⚪${engine.formatNumber(engine.pvc.amount)} ")
            append("⚫${engine.formatNumber(engine.tesnenie.amount)} ")
            append("🔩${engine.formatNumber(engine.kovanie.amount)} ")
            append("🤍${engine.formatNumber(engine.plastovyProfil.amount)} ")
            append("⬜${engine.formatNumber(engine.ram.amount)} ")
            append("🟢${engine.formatNumber(engine.hotoveOkno.amount)}")
        }
        canvas.drawText(resText, 14f, yBase + 20f, gray)

        // Sell sklovina button
        val sellCan = engine.sklovina.amount >= 1.0
        drawBtn(canvas, w - 96f, yBase + 2f, 84f, 34f, if (sellCan) green else greenDim, "💰 Predaj", 16f)
        buttons.add(ButtonV2(w - 96f, yBase + 2f, 84f, 34f, "sell_glass"))

        // Machine cards
        val mY = yBase + 34f
        val mH = 170f; val mGap = 8f
        val machines = engine.allMachines()

        machines.forEachIndexed { idx, m ->
            val y = mY + idx * (mH + mGap)
            drawMachineCard(canvas, w, y, mH, m, idx)
        }

        // Shop
        val shopY = mY + machines.size * (mH + mGap) + 8f
        drawShop(canvas, w, shopY)

        // Auto-buy toggle
        val abY = shopY + 200f
        drawAutoBuy(canvas, w, abY)

        // Prestige button
        if (engine.totalWindows >= 1000) {
            drawPrestige(canvas, w, abY + 50f)
        }
    }

    private fun drawMachineCard(canvas: Canvas, w: Float, y: Float, h: Float, m: Machine, idx: Int) {
        val mgn = 10f
        canvas.drawRoundRect(mgn, y, w - mgn, y + h, 10f, 10f, card)

        // Name + level
        whiteB.textSize = 24f
        whiteB.color = if (m.isUnlocked) Color.WHITE else Color.argb(100,200,200,200)
        canvas.drawText("${m.shortName} ${m.name}", mgn + 12f, y + 26f, whiteB)

        if (m.isUnlocked && m.level > 0) {
            blue.textSize = 17f
            canvas.drawText("Lv.${m.level} ×${m.count} | ${engine.formatNumber(m.speed)}/s", mgn + 12f, y + 48f, blue)

            // Info
            gray.textSize = 15f
            val info = when (idx) {
                0 -> "🏖️ piesok"
                1 -> "🏖️(${engine.formatNumber(engine.getPecEfficiency())}x)→🔵"
                2 -> "⚪→🤍"
                3 -> "🤍→⬜"
                4 -> "🔵+⬜+⚫+🔩→🟢"
                5 -> "🟢→💰 (15x)"
                else -> ""
            }
            canvas.drawText(info, mgn + 12f, y + 68f, gray)
        }

        // Buttons
        val btnW = 100f; val btnH = 34f
        val btnY = y + h - btnH - 8f
        val rX = w - mgn - btnW - 6f

        if (!m.isUnlocked) {
            val cost = engine.formatNumber(m.baseCost * 3)
            val can = engine.score >= m.baseCost * 3
            drawBtn(canvas, rX, btnY, btnW, btnH, if (can) green else greenDim, "🔓 $cost", 16f)
            buttons.add(ButtonV2(rX, btnY, btnW, btnH, "unlock", idx))
            white.textSize = 26f; canvas.drawText("🔒", mgn + 12f, y + h - 16f, white)
        } else if (m.level < 20) {
            val cost = engine.formatNumber(m.upgradeCost())
            val can = engine.score >= m.upgradeCost()
            drawBtn(canvas, rX, btnY, btnW, btnH, if (can) green else greenDim, "⬆️ $cost", 16f)
            buttons.add(ButtonV2(rX, btnY, btnW, btnH, "upgrade", idx))

            val bX = rX - btnW - 6f
            val bc = engine.formatNumber(m.buyCost())
            val bCan = engine.score >= m.buyCost()
            drawBtn(canvas, bX, btnY, btnW, btnH, if (bCan) green else greenDim, "➕ $bc", 16f)
            buttons.add(ButtonV2(bX, btnY, btnW, btnH, "buy", idx))
        } else {
            gray.textSize = 18f; gray.color = Color.rgb(255,215,0)
            canvas.drawText("⚡ MAX", rX, btnY + 24f, gray)
            gray.color = Color.LTGRAY
        }
    }

    private fun drawShop(canvas: Canvas, w: Float, y: Float) {
        val buyable = engine.resources.filter { it.isBuyable }
        if (buyable.isEmpty()) return
        gold.textSize = 22f
        canvas.drawText("🛒 Obchod", 16f, y + 6f, gold)
        var cy = y + 14f
        buyable.forEach { r ->
            val discount = engine.getBuyDiscount()
            val price = engine.formatNumber(r.buyPrice * 5 * discount)
            val can = engine.score >= r.buyPrice * 5 * discount
            drawBtn(canvas, 16f, cy, 120f, 32f, if (can) green else greenDim, "${r.emoji} +5 $price", 16f)
            buttons.add(ButtonV2(16f, cy, 120f, 32f, "buy_res", engine.resources.indexOf(r)))
            gray.textSize = 18f
            canvas.drawText("Máš: ${engine.formatNumber(r.amount)}", 145f, cy + 22f, gray)
            cy += 38f
        }
    }

    private fun drawAutoBuy(canvas: Canvas, w: Float, y: Float) {
        gray.textSize = 18f
        val status = if (engine.autoBuyEnabled) "✅" else "❌"
        drawBtn(canvas, 16f, y, 170f, 36f, if (engine.autoBuyEnabled) green else greenDim, "$status Auto-buy", 18f)
        buttons.add(ButtonV2(16f, y, 170f, 36f, "auto_buy"))
        gray.textSize = 16f
        canvas.drawText("Min. hladina: ${engine.autoBuyThreshold.toInt()}", 200f, y + 24f, gray)
    }

    private fun drawPrestige(canvas: Canvas, w: Float, y: Float) {
        if (engine.showPrestigeConfirm) {
            gold.textSize = 20f
            canvas.drawText("🌟 Prestige? Všetko sa resetuje! Bonus +5%", 16f, y + 16f, gold)
            drawBtn(canvas, 16f, y + 24f, 100f, 38f, red, "ÁNO", 20f)
            buttons.add(ButtonV2(16f, y + 24f, 100f, 38f, "prestige_confirm"))
            drawBtn(canvas, 130f, y + 24f, 100f, 38f, greenDim, "NIE", 20f)
            buttons.add(ButtonV2(130f, y + 24f, 100f, 38f, "prestige_cancel"))
        } else {
            drawBtn(canvas, 16f, y, 200f, 42f, purple, "🌟 Prestige (${engine.formatInt(engine.totalWindows)} okien)", 18f)
            buttons.add(ButtonV2(16f, y, 200f, 42f, "prestige"))
        }
    }

    // ── Research Tab ──
    private fun drawResearchTab(canvas: Canvas, w: Float) {
        gold.textSize = 24f
        canvas.drawText("🔬 Výskum a vylepšenia", 16f, 24f, gold)
        gray.textSize = 17f
        canvas.drawText("Body výskumu: ${engine.totalResearchLevels}", 16f, 48f, gray)

        var y = 58f
        val cardH = 110f; val cardGap = 8f
        for (r in engine.researches) {
            val x = 10f
            val rw = w - 20f
            canvas.drawRoundRect(x, y, x + rw, y + cardH, 10f, 10f, card)

            val isMaxed = r.isMaxed()
            whiteB.textSize = 20f
            whiteB.color = if (isMaxed) Color.argb(100,255,215,0) else Color.WHITE
            canvas.drawText("${r.name} (${r.level}/${r.maxLevel})", x + 14f, y + 24f, whiteB)

            gray.textSize = 16f
            canvas.drawText(r.desc, x + 14f, y + 46f, gray)

            if (!isMaxed) {
                val cost = engine.formatNumber(r.cost())
                val can = engine.score >= r.cost()
                drawBtn(canvas, x + rw - 110f, y + cardH - 42f, 100f, 34f, if (can) green else greenDim, "⬆️ $cost", 16f)
                buttons.add(ButtonV2(x + rw - 110f, y + cardH - 42f, 100f, 34f, "research", engine.researches.indexOf(r)))
            } else {
                gold.textSize = 18f
                canvas.drawText("✅ MAX", x + rw - 90f, y + cardH - 16f, gold)
            }

            // Progress bar
            val progW = 120f; val progH = 8f
            val progX = x + 14f; val progY = y + cardH - 32f
            val prog = Paint().apply { color = Color.argb(60,100,100,100) }
            canvas.drawRoundRect(progX, progY, progX + progW, progY + progH, 4f, 4f, prog)
            val fill = Paint().apply { color = if (isMaxed) Color.rgb(255,215,0) else Color.rgb(76,175,80) }
            val fillW = progW * (r.level.toFloat() / r.maxLevel)
            if (fillW > 0) canvas.drawRoundRect(progX, progY, progX + fillW, progY + progH, 4f, 4f, fill)

            y += cardH + cardGap
        }
    }

    // ── Stats Tab ──
    private fun drawStatsTab(canvas: Canvas, w: Float) {
        val col1 = 16f; val col2 = 200f
        gold.textSize = 24f
        canvas.drawText("📊 Štatistiky", col1, 24f, gold)

        val stats = listOf(
            "💰 Celkovo zarobené" to engine.formatNumber(engine.totalEarned),
            "🪟 Celkovo okien" to engine.formatInt(engine.totalWindows),
            "⭐ Prestige level" to engine.prestigeLevel.toString(),
            "⏱️ Čas hry" to formatTime(engine.totalPlaySeconds),
            "🔬 Výskumné levely" to engine.totalResearchLevels.toString(),
            "⚙️ Auto-buy čas" to formatTime(engine.autoBuyActiveSeconds)
        )

        var y = 40f
        gray.textSize = 18f
        for ((label, value) in stats) {
            canvas.drawText(label, col1, y + 20f, gray)
            white.textSize = 18f
            canvas.drawText(value, col2, y + 20f, white)
            y += 32f
        }

        // Achievements
        y += 8f
        gold.textSize = 22f
        canvas.drawText("🏆 Achievements", col1, y + 8f, gold)
        y += 24f

        for (ach in engine.achievements) {
            val earned = ach.earned
            val icon = if (earned) "✅" else "⬜"
            val color = if (earned) Color.rgb(255,215,0) else Color.argb(100,200,200,200)
            white.textSize = 17f; white.color = color
            canvas.drawText("$icon ${ach.name} – ${ach.desc}", col1, y + 16f, white)
            y += 26f
        }
        white.color = Color.WHITE
    }

    // ── Toast ──
    private fun drawToast(canvas: Canvas, w: Float) {
        if (toastText.isEmpty()) return
        val alpha = (toastTimer.coerceAtMost(40) / 40f * 255).toInt().coerceIn(0, 255)
        val bgToast = Paint().apply { color = Color.argb((alpha * 0.75).toInt(), 30, 30, 40) }
        val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alpha, 255, 215, 0); textSize = 22f; typeface = Typeface.DEFAULT_BOLD
        }
        val tw = textP.measureText(toastText)
        val tx = (w - tw) / 2f - 10f; val ty = 80f
        canvas.drawRoundRect(tx - 10f, ty - 6f, tx + tw + 20f, ty + 30f, 10f, 10f, bgToast)
        canvas.drawText(toastText, tx, ty + 20f, textP)
    }

    // ── Utils ──
    private fun drawBtn(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, p: Paint, text: String, ts: Float) {
        canvas.drawRoundRect(x, y, x + w, y + h, 8f, 8f, p)
        white.textSize = ts; white.color = Color.WHITE
        val tw = white.measureText(text)
        canvas.drawText(text, x + (w - tw) / 2f, y + h - 10f, white)
    }

    private fun formatTime(secs: Long): String {
        val m = secs / 60; val s = secs % 60
        return "${m}m ${s}s"
    }

    // ── Touch ──
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                val x = event.x; val y = event.y - scrollY - 62f

                for (btn in buttons) {
                    if (x >= btn.x && x <= btn.x + btn.w && y >= btn.y && y <= btn.y + btn.h) {
                        handleAction(btn)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastTouchY
                lastTouchY = event.y
                scrollY = (scrollY + dy).coerceIn(-maxScroll(), 0f)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun maxScroll(): Float {
        val contentH = when (engine.currentTab) {
            GameTab.PRODUCTION -> 2200f
            GameTab.RESEARCH -> 700f
            GameTab.STATS -> 1200f
        }
        val visibleH = height.toFloat() - 106f
        return (contentH - visibleH).coerceAtLeast(0f)
    }

    private fun handleAction(btn: ButtonV2) {
        val machines = engine.allMachines()
        when (btn.action) {
            "tab" -> { engine.currentTab = GameTab.entries[btn.data]; scrollY = 0f }
            "upgrade" -> if (btn.data < machines.size) engine.upgradeMachine(machines[btn.data])
            "buy" -> if (btn.data < machines.size) engine.buyMachine(machines[btn.data])
            "unlock" -> if (btn.data < machines.size) engine.unlockMachine(machines[btn.data])
            "sell_glass" -> engine.tickManualSell()
            "buy_res" -> {
                val res = engine.resources.getOrNull(btn.data)
                if (res != null) engine.buyResource(res)
            }
            "research" -> {
                val r = engine.researches.getOrNull(btn.data)
                if (r != null) engine.buyResearch(r)
            }
            "auto_buy" -> engine.autoBuyEnabled = !engine.autoBuyEnabled
            "prestige" -> { engine.showPrestigeConfirm = true; toastText = "🌟 Naozaj resetovať?"; toastTimer = 60 }
            "prestige_confirm" -> engine.doPrestige()
            "prestige_cancel" -> engine.showPrestigeConfirm = false
        }
    }

    private var lastTouchY = 0f
}

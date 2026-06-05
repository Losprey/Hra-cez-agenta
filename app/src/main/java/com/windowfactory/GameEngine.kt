package com.windowfactory

import java.io.*
import kotlin.math.pow
import kotlin.math.roundToLong

// ── Dáta ──
data class Machine(
    val name: String, val shortName: String,
    var level: Int = 1, var count: Int = 1,
    val baseSpeed: Double, val baseCost: Double,
    val costMultiplier: Double = 1.15,
    var isUnlocked: Boolean = false,
    var input1: String? = null, var input2: String? = null,
    val inputRate1: Double = 0.0, val inputRate2: Double = 0.0
) {
    val speed: Double get() = baseSpeed * (1 + 0.1 * (level - 1)) * count
    fun upgradeCost(): Double = baseCost * costMultiplier.pow((level - 1).toDouble())
    fun buyCost(): Double = baseCost * count
}

data class Resource(
    val name: String, val emoji: String,
    var amount: Double = 0.0,
    var isBuyable: Boolean = false, val buyPrice: Double = 0.0
)

data class Achievement(
    val id: String, val name: String, val desc: String,
    var earned: Boolean = false
)

data class Research(
    val id: String, val name: String, val desc: String,
    var level: Int = 0, val maxLevel: Int, val baseCost: Double,
    val costMultiplier: Double = 2.0, val effectPerLevel: Double
) {
    fun cost(): Double = baseCost * costMultiplier.pow(level.toDouble())
    fun isMaxed(): Boolean = level >= maxLevel
}

enum class GameTab { PRODUCTION, RESEARCH, STATS }

// ── Engine ──
class GameEngine {
    // Resources
    val resources = mutableListOf(
        Resource("Piesok", "🏖️"),
        Resource("Sklovina", "🔵"),
        Resource("PVC granule", "⚪", isBuyable = true, buyPrice = 3.0),
        Resource("Tesnenie", "⚫", isBuyable = true, buyPrice = 2.0),
        Resource("Kovanie", "🔩", isBuyable = true, buyPrice = 4.0),
        Resource("Plastový profil", "🤍"),
        Resource("Hotový rám", "⬜"),
        Resource("Hotové okno", "🟢")
    )
    val piesok get() = resources[0]
    val sklovina get() = resources[1]
    val pvc get() = resources[2]
    val tesnenie get() = resources[3]
    val kovanie get() = resources[4]
    val plastovyProfil get() = resources[5]
    val ram get() = resources[6]
    val hotoveOkno get() = resources[7]

    // Machines
    val piskovna = Machine("Pískovňa", "⛏️", baseSpeed = 0.5, baseCost = 10.0)
    val pec = Machine("Pec", "🔥", baseSpeed = 0.3, baseCost = 25.0)
    val extruzia = Machine("Extrúzia", "🧪", baseSpeed = 0.2, baseCost = 200.0)
    val zvaracka = Machine("Zváračka", "🔩", baseSpeed = 0.15, baseCost = 500.0)
    val montaz = Machine("Montáž", "🔧", baseSpeed = 0.1, baseCost = 1500.0,
        input1 = "Sklovina", input2 = "Hotový rám", inputRate1 = 1.0, inputRate2 = 1.0)
    val balenie = Machine("Balenie", "📦", baseSpeed = 0.2, baseCost = 3000.0)

    var score = 50.0
    var totalWindows = 0L
    var totalEarned = 0.0
    var totalPlaySeconds = 0L
    var prestigeLevel = 0
    var prestigeBonus = 1.0          // multiplikátor rýchlosti z prestigov
    var autoBuyEnabled = true
    var autoBuyThreshold = 10.0      // minimum PVC/tesnenie/kovanie pred auto-nákupom
    var nextAchievementToast = ""    // zobrazí sa ako toast
    var nextAchievementTimer = 0     // počet tickov do zmiznutia
    var showPrestigeConfirm = false

    var currentTab = GameTab.PRODUCTION
    var selectedPrestige = false

    // Research
    val researches = mutableListOf(
        Research("eff_sand",   "Efektivita piesku",  "Pískovňa +15% rýchlosť",         maxLevel=5, baseCost=200.0, effectPerLevel=0.15),
        Research("eff_pec",    "Vyhrievanie pece",    "Pec 0.8→0.9→1.0 pomer tavenia",  maxLevel=3, baseCost=500.0, effectPerLevel=0.1),
        Research("logistika",  "Logistika",           "Všetky stroje +10% rýchlosť",     maxLevel=5, baseCost=800.0, effectPerLevel=0.10),
        Research("velkoobchod","Veľkoobchod",         "-15% cena surovín",               maxLevel=4, baseCost=300.0, effectPerLevel=0.15)
    )

    // Achievements
    val achievements = mutableListOf(
        Achievement("first_10",       "Prvých 10 okien",    "Vyrob 10 okien"),
        Achievement("first_100",      "Stovka!",            "Vyrob 100 okien"),
        Achievement("first_1k",       "Tisícka!",           "Vyrob 1 000 okien"),
        Achievement("prestige_1",     "Nový začiatok",      "Prestige 1x"),
        Achievement("prestige_5",     "Ostrieľaný hráč",    "Prestige 5x"),
        Achievement("researcher",     "Výskumník",          "Kúp 5 levelov výskumu"),
        Achievement("milionar",       "Milionár",           "Zarob celkovo 1 000 000💰"),
        Achievement("auto_supply",    "Automatizácia",      "Auto-buy aktívny 10 minút")
    )

    var totalResearchLevels = 0
    var autoBuyActiveSeconds = 0L

    // ── Helpery ──
    fun getSpeedMultiplier(): Double = prestigeBonus *
        (1 + researches.first { it.id == "logistika" }.level * 0.10) *
        (1 + researches.first { it.id == "eff_sand" }.level * 0.15)

    fun getPecEfficiency(): Double = 0.8 + researches.first { it.id == "eff_pec" }.level * 0.1

    fun getBuyDiscount(): Double = 1.0 - researches.first { it.id == "velkoobchod" }.level * 0.15

    init {
        piskovna.isUnlocked = true
    }

    // ── Hlavný tick ──
    fun tick(deltaSeconds: Double) {
        val capped = deltaSeconds.coerceIn(0.0, 2.0)
        if (capped <= 0) return

        totalPlaySeconds += capped.toLong()
        if (autoBuyEnabled) autoBuyActiveSeconds += capped.toLong()

        val mult = getSpeedMultiplier()

        // Pískovňa
        if (piskovna.isUnlocked) piesok.amount += piskovna.speed * mult * capped

        // Pec
        if (pec.isUnlocked && sklovina.amount < 9999) {
            val maxConsume = pec.speed * mult * capped
            val actual = piesok.amount.coerceAtMost(maxConsume)
            piesok.amount -= actual
            sklovina.amount += actual * getPecEfficiency()
        }

        // Extrúzia
        if (extruzia.isUnlocked) {
            val maxConsume = extruzia.speed * mult * capped
            val actual = pvc.amount.coerceAtMost(maxConsume)
            pvc.amount -= actual
            plastovyProfil.amount += actual * 0.85
        }

        // Zváračka
        if (zvaracka.isUnlocked) {
            val maxConsume = zvaracka.speed * mult * capped
            val actual = plastovyProfil.amount.coerceAtMost(maxConsume)
            plastovyProfil.amount -= actual
            ram.amount += actual * 0.9
        }

        // Montáž
        if (montaz.isUnlocked) {
            val rate = montaz.speed * mult * capped
            val s = sklovina.amount.coerceAtMost(rate)
            val r = ram.amount.coerceAtMost(rate)
            val t = tesnenie.amount.coerceAtMost(rate)
            val k = kovanie.amount.coerceAtMost(rate)
            val canMake = listOf(s, r, t, k).minOrNull() ?: 0.0
            if (canMake >= 0.1) {
                sklovina.amount -= canMake
                ram.amount -= canMake
                tesnenie.amount -= canMake
                kovanie.amount -= canMake
                hotoveOkno.amount += canMake * 0.95
            }
        }

        // Balenie ->💰
        if (balenie.isUnlocked && hotoveOkno.amount > 0) {
            val maxSell = balenie.speed * mult * capped
            val toSell = hotoveOkno.amount.coerceAtMost(maxSell)
            hotoveOkno.amount -= toSell
            val earned = toSell * 15.0
            score += earned
            totalEarned += earned
            totalWindows += toSell.roundToLong()
        }

        // Auto-buy resources
        if (autoBuyEnabled) {
            autoBuyResources()
        }

        // Check achievements
        checkAchievements()
    }

    fun tickManualSell() {
        if (sklovina.amount >= 1.0) {
            val toSell = sklovina.amount.coerceAtMost(100.0)
            sklovina.amount -= toSell
            score += toSell
        }
    }

    // ── Auto-buy ──
    private fun autoBuyResources() {
        val discount = getBuyDiscount()
        val targets = listOf(
            pvc to 3.0,
            tesnenie to 2.0,
            kovanie to 4.0
        )
        for ((res, price) in targets) {
            if (res.amount < autoBuyThreshold) {
                val need = (autoBuyThreshold - res.amount).coerceAtLeast(5.0)
                val cost = need * price * discount
                if (score >= cost) {
                    score -= cost
                    res.amount += need
                }
            }
        }
    }

    // ── Achievements ──
    private fun checkAchievements() {
        val list = listOf(
            achievements[0] to (totalWindows >= 10),
            achievements[1] to (totalWindows >= 100),
            achievements[2] to (totalWindows >= 1000),
            achievements[3] to (prestigeLevel >= 1),
            achievements[4] to (prestigeLevel >= 5),
            achievements[5] to (totalResearchLevels >= 5),
            achievements[6] to (totalEarned >= 1_000_000),
            achievements[7] to (autoBuyActiveSeconds >= 600)
        )
        for ((ach, cond) in list) {
            if (!ach.earned && cond) {
                ach.earned = true
                nextAchievementToast = "🏆 ${ach.name}: ${ach.desc}"
                nextAchievementTimer = 25  // tickov
            }
        }
    }

    fun consumeAchievementToast(): String? {
        if (nextAchievementTimer > 0) {
            nextAchievementTimer--
            if (nextAchievementTimer == 0) {
                val t = nextAchievementToast
                nextAchievementToast = ""
                return t
            }
        }
        return null
    }

    // ── Prestige ──
    fun canPrestige(): Boolean = totalWindows >= 1000 && !showPrestigeConfirm

    fun doPrestige() {
        if (totalWindows < 1000) return
        prestigeLevel++
        prestigeBonus = 1.0 + prestigeLevel * 0.05

        // Reset všetkého
        score = 100.0
        totalWindows = 0L
        totalEarned = 0.0
        for (r in resources) r.amount = 0.0
        for (m in allMachines()) {
            m.level = 1
            m.count = 1
            m.isUnlocked = (m == piskovna)
        }
        // Výskumy ostanú
        showPrestigeConfirm = false
        selectedPrestige = true

        nextAchievementToast = "🌟 Prestige ${prestigeLevel}! Všetky stroje +5%"
        nextAchievementTimer = 30
    }

    fun tryPrestige(): Boolean {
        if (!canPrestige()) return false
        showPrestigeConfirm = true
        return true
    }

    // ── Machine actions ──
    fun upgradeMachine(m: Machine): Boolean {
        val cost = m.upgradeCost()
        if (score >= cost && m.level < 20) { score -= cost; m.level++; return true }
        return false
    }
    fun buyMachine(m: Machine): Boolean {
        val cost = m.buyCost()
        if (score >= cost) { score -= cost; m.count++; return true }
        return false
    }
    fun unlockMachine(m: Machine): Boolean {
        val cost = m.baseCost * 3
        if (score >= cost) { score -= cost; m.isUnlocked = true; m.level = 1; m.count = 1; return true }
        return false
    }
    fun buyResource(res: Resource): Boolean {
        if (!res.isBuyable) return false
        val discount = getBuyDiscount()
        val cost = res.buyPrice * 5 * discount
        if (score >= cost) { score -= cost; res.amount += 5.0; return true }
        return false
    }

    // ── Research ──
    fun buyResearch(r: Research): Boolean {
        if (r.isMaxed()) return false
        val c = r.cost()
        if (score >= c) {
            score -= c
            r.level++
            totalResearchLevels++
            return true
        }
        return false
    }

    fun hasEarnedAchievement(id: String): Boolean =
        achievements.find { it.id == id }?.earned ?: false

    fun allMachines(): List<Machine> = listOf(piskovna, pec, extruzia, zvaracka, montaz, balenie)

    fun formatNumber(n: Double): String = when {
        n >= 1_000_000_000 -> String.format("%.1fB", n / 1_000_000_000)
        n >= 1_000_000 -> String.format("%.2fM", n / 1_000_000)
        n >= 1_000 -> String.format("%.1fK", n / 1_000)
        n >= 1 -> String.format("%.1f", n)
        else -> String.format("%.2f", n)
    }
    fun formatInt(n: Long): String = when {
        n >= 1_000_000_000 -> String.format("%.1fB", n / 1_000_000_000.0)
        n >= 1_000_000 -> String.format("%.2fM", n / 1_000_000.0)
        n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
        else -> n.toString()
    }

    // ── Offline earnings ──
    fun computeOfflineEarnings(secondsAway: Long): String {
        if (secondsAway < 10) return ""
        val cappedAway = secondsAway.coerceAtMost(3600L) // max 1h
        val mult = getSpeedMultiplier()
        val dt = cappedAway.toDouble() * 0.5 // 50% produkcie

        if (piskovna.isUnlocked) piesok.amount += piskovna.speed * mult * dt
        if (pec.isUnlocked) sklovina.amount += pec.speed * mult * dt * getPecEfficiency()
        if (extruzia.isUnlocked) plastovyProfil.amount += extruzia.speed * mult * dt * 0.85
        if (zvaracka.isUnlocked) ram.amount += zvaracka.speed * mult * dt * 0.9

        val mins = cappedAway / 60
        return "💤 Preč si bol ${mins} min. Dostávaš 50% produkcie!"
    }
}

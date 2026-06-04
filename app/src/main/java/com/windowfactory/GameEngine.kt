package com.windowfactory

// Herné dáta a logika
data class Machine(
    val name: String,
    val shortName: String,
    var level: Int = 1,
    var count: Int = 1,
    val baseSpeed: Double,
    val baseCost: Double,
    val costMultiplier: Double = 1.15,
    var isUnlocked: Boolean = false,
    var input1: String? = null,     // čo stroj spotrebúva
    var input2: String? = null,
    val inputRate1: Double = 0.0,   // koľko spotrebuje na jednotku outputu
    val inputRate2: Double = 0.0
) {
    val speed: Double get() = baseSpeed * (1 + 0.1 * (level - 1)) * count
    fun upgradeCost(): Double = baseCost * Math.pow(costMultiplier, (level - 1).toDouble())
    fun buyCost(): Double = baseCost * count
}

data class Resource(
    val name: String,
    val emoji: String,
    var amount: Double = 0.0,
    var isBuyable: Boolean = false,
    val buyPrice: Double = 0.0
)

class GameEngine {
    // Suroviny
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

    // Stroje
    val piskovna = Machine("Pískovňa", "⛏️", baseSpeed = 0.5, baseCost = 10.0)
    val pec = Machine("Pec", "🔥", baseSpeed = 0.3, baseCost = 25.0)
    val extruzia = Machine("Extrúzia", "🧪", baseSpeed = 0.2, baseCost = 200.0)
    val zvaracka = Machine("Zváračka", "🔩", baseSpeed = 0.15, baseCost = 500.0)
    val montaz = Machine("Montáž", "🔧", baseSpeed = 0.1, baseCost = 1500.0,
        input1 = "Sklovina", input2 = "Hotový rám", inputRate1 = 1.0, inputRate2 = 1.0)
    val balenie = Machine("Balenie", "📦", baseSpeed = 0.2, baseCost = 3000.0)

    var score = 50.0
    var totalWindows = 0L
    var prestigeLevel = 0

    init {
        piskovna.isUnlocked = true
    }

    fun tick(deltaSeconds: Double) {
        val capped = Math.min(deltaSeconds, 2.0)
        if (capped <= 0) return

        // Pískovňa -> piesok
        if (piskovna.isUnlocked) piesok.amount += piskovna.speed * capped

        // Pec: piesok -> sklovina
        if (pec.isUnlocked && sklovina.amount < 9999) {
            val maxConsume = pec.speed * capped
            val actual = Math.min(piesok.amount, maxConsume)
            piesok.amount -= actual
            sklovina.amount += actual * 0.8
        }

        // Extrúzia: PVC -> plastový profil
        if (extruzia.isUnlocked) {
            val maxConsume = extruzia.speed * capped
            val actual = Math.min(pvc.amount, maxConsume)
            pvc.amount -= actual
            plastovyProfil.amount += actual * 0.85
        }

        // Zváračka: plastový profil -> rám
        if (zvaracka.isUnlocked) {
            val maxConsume = zvaracka.speed * capped
            val actual = Math.min(plastovyProfil.amount, maxConsume)
            plastovyProfil.amount -= actual
            ram.amount += actual * 0.9
        }

        // Montáž: sklovina + rám -> hotové okno (spotrebuje tesnenie + kovanie)
        if (montaz.isUnlocked) {
            val rate = montaz.speed * capped
            val s = Math.min(sklovina.amount, rate)
            val r = Math.min(ram.amount, rate)
            val t = Math.min(tesnenie.amount, rate)
            val k = Math.min(kovanie.amount, rate)
            val canMake = listOf(s, r, t, k).minOrNull() ?: 0.0
            if (canMake >= 0.1) {
                sklovina.amount -= canMake
                ram.amount -= canMake
                tesnenie.amount -= canMake
                kovanie.amount -= canMake
                hotoveOkno.amount += canMake * 0.95
            }
        }

        // Balenie: hotové okno -> predaj
        if (balenie.isUnlocked && hotoveOkno.amount > 0) {
            val maxSell = balenie.speed * capped
            val toSell = Math.min(hotoveOkno.amount, maxSell)
            hotoveOkno.amount -= toSell
            score += toSell * 15.0 // každé okno = 15💰
            totalWindows += toSell.roundToLong()
        }
    }

    fun tickManualSell() {
        // Manuálny predaj skloviny
        if (sklovina.amount >= 1.0) {
            val toSell = Math.min(sklovina.amount, 100.0)
            sklovina.amount -= toSell
            score += toSell
        }
    }

    private fun Double.roundToLong(): Long = Math.round(this)

    fun upgradeMachine(machine: Machine): Boolean {
        val cost = machine.upgradeCost()
        if (score >= cost && machine.level < 20) {
            score -= cost
            machine.level++
            return true
        }
        return false
    }

    fun buyMachine(machine: Machine): Boolean {
        val cost = machine.buyCost()
        if (score >= cost) {
            score -= cost
            machine.count++
            return true
        }
        return false
    }

    fun unlockMachine(machine: Machine): Boolean {
        val cost = machine.baseCost * 3
        if (score >= cost) {
            score -= cost
            machine.isUnlocked = true
            machine.level = 1
            machine.count = 1
            return true
        }
        return false
    }

    fun buyResource(res: Resource): Boolean {
        if (!res.isBuyable) return false
        val cost = res.buyPrice * 5
        if (score >= cost) {
            score -= cost
            res.amount += 5.0
            return true
        }
        return false
    }

    fun formatNumber(n: Double): String = when {
        n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000)
        n >= 1_000 -> String.format("%.1fK", n / 1_000)
        else -> String.format("%.0f", n)
    }

    fun allMachines(): List<Machine> = listOf(piskovna, pec, extruzia, zvaracka, montaz, balenie)
}

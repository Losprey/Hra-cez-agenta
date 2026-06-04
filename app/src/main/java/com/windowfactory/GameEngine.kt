package com.windowfactory

// Herné dáta a logika
data class Machine(
    val name: String,
    val shortName: String,
    var level: Int = 1,
    var count: Int = 1,
    val baseSpeed: Double,        // jednotiek za sekundu na 1 stroj
    val baseCost: Double,         // cena za upgrade na level 1
    val costMultiplier: Double = 1.15,  // každý level stojí viac
    var isUnlocked: Boolean = false
) {
    val speed: Double get() = baseSpeed * level * count * (1 + 0.1 * (level - 1))
    fun upgradeCost(currentLevel: Int): Double =
        baseCost * Math.pow(costMultiplier, (currentLevel - 1).toDouble())

    fun buyCost(): Double = baseCost * count
}

data class Product(
    val name: String,
    val shortName: String,
    var amount: Double = 0.0,
    val icon: String = ""
)

class GameEngine {
    // Suroviny / produkty
    val piesok = Product("Piesok", "🏖️")
    val sklovina = Product("Sklovina", "🔵")

    // Stroje
    val piskovna = Machine("Pískovňa", "⛏️", baseSpeed = 0.5, baseCost = 10.0)
    val pec = Machine("Pec", "🔥", baseSpeed = 0.3, baseCost = 25.0)

    var totalWindows = 0L       // celkovo vyrobených okien (pre prestíž)
    var prestigeLevel = 0
    var score = 0.0             // herné peniaze

    init {
        piskovna.isUnlocked = true
    }

    // Tick hry - volá sa každú sekundu
    fun tick(deltaSeconds: Double) {
        if (piskovna.isUnlocked) {
            val sandProduced = piskovna.speed * deltaSeconds
            piesok.amount += sandProduced
        }

        if (pec.isUnlocked) {
            val glassPerSecond = pec.speed
            val maxConsume = glassPerSecond * deltaSeconds
            val actualConsume = Math.min(piesok.amount, maxConsume)
            piesok.amount -= actualConsume
            sklovina.amount += actualConsume * 0.8 // 80% výťažnosť
        }
    }

    // Upgrade stroja
    fun upgradeMachine(machine: Machine): Boolean {
        val cost = machine.upgradeCost(machine.level)
        if (score >= cost) {
            score -= cost
            machine.level++
            return true
        }
        return false
    }

    // Kúpa nového stroja (zvýšenie count)
    fun buyMachine(machine: Machine): Boolean {
        val cost = machine.buyCost()
        if (score >= cost) {
            score -= cost
            machine.count++
            return true
        }
        return false
    }

    // Predaj surovín
    fun sellSand(): Boolean {
        if (piesok.amount >= 1.0) {
            val sellAmount = Math.min(piesok.amount, 100.0)
            piesok.amount -= sellAmount
            score += sellAmount
            return true
        }
        return false
    }

    // Odomknutie stroja
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

    fun formatNumber(n: Double): String {
        return when {
            n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000)
            n >= 1_000 -> String.format("%.1fK", n / 1_000)
            else -> String.format("%.0f", n)
        }
    }
}

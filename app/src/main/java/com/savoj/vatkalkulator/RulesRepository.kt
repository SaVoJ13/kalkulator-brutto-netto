package com.savoj.vatkalkulator

import android.content.Context
import java.io.File
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

object RulesRepository {

    private const val RULES_URL =
        "https://raw.githubusercontent.com/SaVoJ13/wynagrodzenia-rules/main/rules.json"
    private const val CACHE_FILE = "rules_cache.json"
    private const val MAX_BYTES = 100_000

    /** Швидко, без інтернету: новіша з двох копій (збережена або вбудована). */
    fun loadLocal(context: Context): Rules {
        val bundled = Rules.load(context)
        val cached = readCache(context)
        return if (cached != null && compareVersions(cached.version, bundled.version) > 0) {
            cached
        } else {
            bundled
        }
    }

    /** Викликати у фоні. Повертає нові правила або null, якщо оновлення немає чи воно некоректне. */
    fun fetchRemote(context: Context): Rules? {
        return try {
            val connection = URL(RULES_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            try {
                if (connection.responseCode != 200) return null
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                if (text.length > MAX_BYTES) return null
                val remote = Rules.parse(text)
                if (!isSane(remote)) return null
                val bundled = Rules.load(context)
                if (compareVersions(remote.version, bundled.version) < 0) return null
                File(context.filesDir, CACHE_FILE).writeText(text)
                remote
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readCache(context: Context): Rules? {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.exists()) return null
        return try {
            val rules = Rules.parse(file.readText())
            if (isSane(rules)) rules else null
        } catch (e: Exception) {
            null
        }
    }

    /** "2026.10" > "2026.2": порівняння по числових частинах. */
    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    /** Захист від помилки в JSON: ставки мають бути в розумних межах. */
    private fun isSane(r: Rules): Boolean {
        fun rate(v: BigDecimal) = v.signum() >= 0 && v <= BigDecimal.ONE
        fun positive(v: BigDecimal) = v.signum() > 0
        return rate(r.emerytalna) && rate(r.rentowa) && rate(r.chorobowa) &&
                rate(r.healthRate) && rate(r.pitRateLow) && rate(r.pitRateHigh) &&
                r.pitRateLow <= r.pitRateHigh &&
                rate(r.ppkEmployeeRate) && rate(r.ppkEmployeeReducedRate) &&
                rate(r.ppkEmployerRate) && rate(r.civilCostsRate) &&
                rate(r.civilCostsCopyrightRate) &&
                positive(r.minimumWage) && positive(r.zusAnnualLimit) &&
                positive(r.pitThreshold) && positive(r.costsStandard) &&
                positive(r.costsOutOfTown) && positive(r.youthAnnualLimit) &&
                positive(r.civilCostsCopyrightAnnualLimit) &&
                positive(r.ppkReducedThresholdMultiplier) &&
                r.monthlyReducingAmount.signum() >= 0
    }
}
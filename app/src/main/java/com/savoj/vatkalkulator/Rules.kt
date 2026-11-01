package com.savoj.vatkalkulator

import android.content.Context
import org.json.JSONObject
import java.math.BigDecimal

data class Rules(
    val version: String,
    val minimumWage: BigDecimal,
    val emerytalna: BigDecimal,
    val rentowa: BigDecimal,
    val chorobowa: BigDecimal,
    val zusAnnualLimit: BigDecimal,
    val healthRate: BigDecimal,
    val pitRateLow: BigDecimal,
    val pitRateHigh: BigDecimal,
    val pitThreshold: BigDecimal,
    val monthlyReducingAmount: BigDecimal,
    val costsStandard: BigDecimal,
    val costsOutOfTown: BigDecimal,
    val youthAnnualLimit: BigDecimal,
    val ppkEmployeeRate: BigDecimal,
    val ppkEmployeeReducedRate: BigDecimal,
    val ppkReducedThresholdMultiplier: BigDecimal,
    val ppkEmployerRate: BigDecimal,
    val civilCostsRate: BigDecimal,
    val civilCostsCopyrightRate: BigDecimal,
    val civilCostsCopyrightAnnualLimit: BigDecimal
) {
    companion object {
        fun load(context: Context): Rules {
            val text = context.assets.open("rules.json").bufferedReader().use { it.readText() }
            return parse(text)
        }

        fun parse(text: String): Rules {
            val root = JSONObject(text)
            val zus = root.getJSONObject("zus")
            val pit = root.getJSONObject("pit")
            val ppk = root.getJSONObject("ppk")
            val civil = root.getJSONObject("civil")

            fun JSONObject.num(key: String) = BigDecimal(get(key).toString())

            return Rules(
                version = root.getString("version"),
                minimumWage = root.num("minimumWage"),
                emerytalna = zus.num("emerytalnaEmployee"),
                rentowa = zus.num("rentowaEmployee"),
                chorobowa = zus.num("chorobowaEmployee"),
                zusAnnualLimit = zus.num("annualBaseLimit"),
                healthRate = root.getJSONObject("health").num("rate"),
                pitRateLow = pit.num("rateLow"),
                pitRateHigh = pit.num("rateHigh"),
                pitThreshold = pit.num("threshold"),
                monthlyReducingAmount = pit.num("monthlyReducingAmount"),
                costsStandard = pit.num("costsStandard"),
                costsOutOfTown = pit.num("costsOutOfTown"),
                youthAnnualLimit = root.getJSONObject("youthRelief").num("annualLimit"),
                ppkEmployeeRate = ppk.num("employeeRate"),
                ppkEmployeeReducedRate = ppk.num("employeeReducedRate"),
                ppkReducedThresholdMultiplier = ppk.num("reducedThresholdMultiplier"),
                ppkEmployerRate = ppk.num("employerRate"),
                civilCostsRate = civil.num("costsRate"),
                civilCostsCopyrightRate = civil.num("costsCopyrightRate"),
                civilCostsCopyrightAnnualLimit = civil.num("costsCopyrightAnnualLimit")
            )
        }
    }
}
package com.savoj.vatkalkulator

import java.math.BigDecimal
import java.math.RoundingMode

enum class ContractType { EMPLOYMENT, ZLECENIE, DZIELO }

enum class ZlecenieStatus {
    ONLY_TITLE,            // zlecenie є єдиним титулом до страхування
    OTHER_EMPLOYMENT_MIN,  // є etat не нижче мінімальної зарплати: лише zdrowotna
    STUDENT_UNDER_26       // студент до 26 років: без ZUS і без zdrowotnej
}

data class Options(
    val under26: Boolean = false,
    val costsOutOfTown: Boolean = false,
    val pit2: Boolean = true,
    val ppk: Boolean = false,
    val ppkReduced: Boolean = false,
    val contract: ContractType = ContractType.EMPLOYMENT,
    val zlecenieStatus: ZlecenieStatus = ZlecenieStatus.ONLY_TITLE,
    val sickness: Boolean = false,
    val copyright: Boolean = false
)

data class SalaryResult(
    val brutto: BigDecimal,
    val emerytalna: BigDecimal,
    val rentowa: BigDecimal,
    val chorobowa: BigDecimal,
    val zdrowotna: BigDecimal,
    val pit: BigDecimal,
    val ppkEmployee: BigDecimal,
    val ppkEmployer: BigDecimal,
    val netto: BigDecimal,
    val warnings: List<String>
)

object SalaryCalculator {

    private fun BigDecimal.toGrosz(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
    private fun BigDecimal.toZloty(): BigDecimal = setScale(0, RoundingMode.HALF_UP)

    /**
     * Рахує виплату за вказаний місяць року (1-12), припускаючи,
     * що брутто однакове в усі місяці від січня.
     */
    fun calculate(brutto: BigDecimal, month: Int, options: Options, rules: Rules): SalaryResult {
        require(month in 1..12)

        val contract = options.contract
        var cumZusBase = BigDecimal.ZERO
        var cumTaxBase = BigDecimal.ZERO
        var cumExempt = BigDecimal.ZERO
        var cumCosts = BigDecimal.ZERO
        var result: SalaryResult? = null

        for (m in 1..month) {
            val warnings = mutableListOf<String>()

            // Які внески платяться
            val (paysSocial, paysHealth, paysSickness) = when (contract) {
                ContractType.EMPLOYMENT -> Triple(true, true, true)
                ContractType.ZLECENIE -> when (options.zlecenieStatus) {
                    ZlecenieStatus.ONLY_TITLE -> Triple(true, true, options.sickness)
                    ZlecenieStatus.OTHER_EMPLOYMENT_MIN -> Triple(false, true, false)
                    ZlecenieStatus.STUDENT_UNDER_26 -> Triple(false, false, false)
                }
                ContractType.DZIELO -> Triple(false, false, false)
            }

            // ZUS: emerytalna і rentowa лише до річного ліміту, chorobowa без ліміту
            val zusRoom = (rules.zusAnnualLimit - cumZusBase).max(BigDecimal.ZERO)
            val zusBase = if (paysSocial) brutto.min(zusRoom) else BigDecimal.ZERO
            val emerytalna = (zusBase * rules.emerytalna).toGrosz()
            val rentowa = (zusBase * rules.rentowa).toGrosz()
            val chorobowa = if (paysSickness) {
                (brutto * rules.chorobowa).toGrosz()
            } else {
                BigDecimal.ZERO.toGrosz()
            }
            val social = emerytalna + rentowa + chorobowa

            // Składka zdrowotna
            val zdrowotna = if (paysHealth) {
                ((brutto - social) * rules.healthRate).toGrosz()
            } else {
                BigDecimal.ZERO.toGrosz()
            }

            // PPK (лише для umowy o pracę)
            var ppkEmployee = BigDecimal.ZERO
            var ppkEmployer = BigDecimal.ZERO
            if (contract == ContractType.EMPLOYMENT && options.ppk) {
                val canReduce = brutto <= rules.minimumWage * rules.ppkReducedThresholdMultiplier
                val rate = if (options.ppkReduced && canReduce) {
                    rules.ppkEmployeeReducedRate
                } else {
                    rules.ppkEmployeeRate
                }
                ppkEmployee = (brutto * rate).toGrosz()
                ppkEmployer = (brutto * rules.ppkEmployerRate).toGrosz()
            }

            // Дохід для PIT
            val revenue = brutto + ppkEmployer

            // Ulga dla młodych (не діє на umowę o dzieło)
            var taxableRevenue = revenue
            if (options.under26 && contract != ContractType.DZIELO) {
                val room = (rules.youthAnnualLimit - cumExempt).max(BigDecimal.ZERO)
                val exempt = revenue.min(room)
                taxableRevenue = revenue - exempt
                cumExempt += exempt
                if (taxableRevenue.signum() > 0) warnings.add("YOUTH_LIMIT_EXCEEDED")
            }

            // Koszty uzyskania przychodu
            val costs: BigDecimal = when (contract) {
                ContractType.EMPLOYMENT ->
                    if (options.costsOutOfTown) rules.costsOutOfTown else rules.costsStandard
                ContractType.ZLECENIE ->
                    ((taxableRevenue - social).max(BigDecimal.ZERO) * rules.civilCostsRate).toGrosz()
                ContractType.DZIELO ->
                    if (options.copyright) {
                        val wanted = (taxableRevenue * rules.civilCostsCopyrightRate).toGrosz()
                        val room = (rules.civilCostsCopyrightAnnualLimit - cumCosts).max(BigDecimal.ZERO)
                        val allowed = wanted.min(room)
                        if (allowed < wanted) warnings.add("COPYRIGHT_LIMIT_REACHED")
                        cumCosts += allowed
                        allowed
                    } else {
                        (taxableRevenue * rules.civilCostsRate).toGrosz()
                    }
            }

            // PIT
            val taxBase = if (taxableRevenue.signum() == 0) {
                BigDecimal.ZERO
            } else {
                (taxableRevenue - costs - social).max(BigDecimal.ZERO).toZloty()
            }
            val lowPart = (rules.pitThreshold - cumTaxBase).max(BigDecimal.ZERO).min(taxBase)
            val highPart = taxBase - lowPart
            val tax = lowPart * rules.pitRateLow + highPart * rules.pitRateHigh
            val reduction = if (options.pit2 && cumTaxBase < rules.pitThreshold) {
                rules.monthlyReducingAmount
            } else {
                BigDecimal.ZERO
            }
            val pit = (tax - reduction).max(BigDecimal.ZERO).toZloty()

            cumTaxBase += taxBase
            cumZusBase += zusBase

            val netto = brutto - social - zdrowotna - pit - ppkEmployee

            result = SalaryResult(
                brutto = brutto,
                emerytalna = emerytalna,
                rentowa = rentowa,
                chorobowa = chorobowa,
                zdrowotna = zdrowotna,
                pit = pit,
                ppkEmployee = ppkEmployee,
                ppkEmployer = ppkEmployer,
                netto = netto,
                warnings = warnings
            )
        }
        return result!!
    }
}
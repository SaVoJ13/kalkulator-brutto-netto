package com.savoj.vatkalkulator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class SalaryCalculatorTest {

    private fun d(s: String) = BigDecimal(s)

    private val rules = Rules(
        version = "test",
        minimumWage = d("4806"),
        emerytalna = d("0.0976"),
        rentowa = d("0.015"),
        chorobowa = d("0.0245"),
        zusAnnualLimit = d("282600"),
        healthRate = d("0.09"),
        pitRateLow = d("0.12"),
        pitRateHigh = d("0.32"),
        pitThreshold = d("120000"),
        monthlyReducingAmount = d("300"),
        costsStandard = d("250"),
        costsOutOfTown = d("300"),
        youthAnnualLimit = d("85528"),
        ppkEmployeeRate = d("0.02"),
        ppkEmployeeReducedRate = d("0.005"),
        ppkReducedThresholdMultiplier = d("1.2"),
        ppkEmployerRate = d("0.015"),
        civilCostsRate = d("0.20"),
        civilCostsCopyrightRate = d("0.50"),
        civilCostsCopyrightAnnualLimit = d("120000")
    )

    private fun netto(brutto: String, options: Options = Options(), month: Int = 1): String =
        SalaryCalculator.calculate(d(brutto), month, options, rules)
            .netto.setScale(2, RoundingMode.HALF_UP).toPlainString()

    // --- umowa o pracę ---
    @Test fun etat_minimalna() = assertEquals("3605.85", netto("4806"))
    @Test fun etat_8000() = assertEquals("5783.91", netto("8000"))
    @Test fun etat_do26() = assertEquals("3773.85", netto("4806", Options(under26 = true)))
    @Test fun etat_koszty300() = assertEquals("3611.85", netto("4806", Options(costsOutOfTown = true)))
    @Test fun etat_bez_pit2() = assertEquals("3305.85", netto("4806", Options(pit2 = false)))
    @Test fun etat_ppk() = assertEquals("3501.73", netto("4806", Options(ppk = true)))
    @Test fun etat_ppk_obnizone() =
        assertEquals("3573.82", netto("4806", Options(ppk = true, ppkReduced = true)))
    @Test fun etat_30000_maj() = assertEquals("19144.17", netto("30000", month = 5))
    @Test fun etat_30000_grudzien() = assertEquals("17346.15", netto("30000", month = 12))

    // --- umowa zlecenie ---
    @Test fun zlecenie_6500_z_chorobowa() =
        assertEquals("4866.05", netto("6500", Options(contract = ContractType.ZLECENIE, sickness = true)))

    @Test fun zlecenie_6500_bez_chorobowej() =
        assertEquals("4994.97", netto("6500", Options(contract = ContractType.ZLECENIE)))

    @Test fun zlecenie_etat_co_najmniej_minimalna() = assertEquals(
        "3963.00",
        netto("4500", Options(
            contract = ContractType.ZLECENIE,
            zlecenieStatus = ZlecenieStatus.OTHER_EMPLOYMENT_MIN
        ))
    )

    @Test fun zlecenie_student_do_26() = assertEquals(
        "4000.00",
        netto("4000", Options(
            contract = ContractType.ZLECENIE,
            zlecenieStatus = ZlecenieStatus.STUDENT_UNDER_26,
            under26 = true
        ))
    )

    @Test fun zlecenie_do_26_nie_student() = assertEquals(
        "3230.14",
        netto("4000", Options(contract = ContractType.ZLECENIE, under26 = true))
    )

    // --- umowa o dzieło ---
    @Test fun dzielo_4000() =
        assertEquals("3916.00", netto("4000", Options(contract = ContractType.DZIELO)))

    @Test fun dzielo_10000_prawa_autorskie() = assertEquals(
        "9400.00",
        netto("10000", Options(contract = ContractType.DZIELO, copyright = true, pit2 = false))
    )

    @Test fun dzielo_3600_bez_pit2() =
        assertEquals("3254.00", netto("3600", Options(contract = ContractType.DZIELO, pit2 = false)))

    @Test fun dzielo_ulga_dla_mlodych_nie_dziala() =
        assertEquals("3916.00", netto("4000", Options(contract = ContractType.DZIELO, under26 = true)))
}
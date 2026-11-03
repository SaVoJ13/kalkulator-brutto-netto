package com.savoj.vatkalkulator

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** Копія assets/rules.json, щоб прев'ю працювали без Context. Лише для Android Studio. */
private val PreviewRules = Rules.parse(
    """
    {
      "version": "preview",
      "year": 2026,
      "minimumWage": 4806.00,
      "zus": { "emerytalnaEmployee": 0.0976, "rentowaEmployee": 0.015, "chorobowaEmployee": 0.0245, "annualBaseLimit": 282600.00 },
      "health": { "rate": 0.09 },
      "pit": { "rateLow": 0.12, "rateHigh": 0.32, "threshold": 120000.00, "monthlyReducingAmount": 300.00, "costsStandard": 250.00, "costsOutOfTown": 300.00 },
      "youthRelief": { "maxAge": 26, "annualLimit": 85528.00 },
      "ppk": { "employeeRate": 0.02, "employeeReducedRate": 0.005, "reducedThresholdMultiplier": 1.2, "employerRate": 0.015 },
      "civil": { "costsRate": 0.20, "costsCopyrightRate": 0.50, "costsCopyrightAnnualLimit": 120000.00 }
    }
    """.trimIndent()
)

@Composable
private fun PreviewScreen(dark: Boolean, initialBrutto: String) {
    AppTheme(dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SalaryScreen(PreviewRules, dark, onToggleTheme = {}, initialBrutto = initialBrutto)
        }
    }
}

@Preview(name = "Telefon, jasny, wynik", widthDp = 393, heightDp = 852)
@Composable
private fun PhoneLightPreview() = PreviewScreen(dark = false, initialBrutto = "8000")

@Preview(name = "Telefon, ciemny, wynik", widthDp = 393, heightDp = 852)
@Composable
private fun PhoneDarkPreview() = PreviewScreen(dark = true, initialBrutto = "8000")

@Preview(name = "Telefon, pusty", widthDp = 393, heightDp = 852)
@Composable
private fun PhoneEmptyPreview() = PreviewScreen(dark = false, initialBrutto = "")

@Preview(name = "Mały telefon, duża czcionka", widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun SmallPhoneLargeFontPreview() = PreviewScreen(dark = false, initialBrutto = "4806")

@Preview(name = "Tablet, dwie kolumny", widthDp = 1000, heightDp = 700)
@Composable
private fun TabletPreview() = PreviewScreen(dark = false, initialBrutto = "8000")

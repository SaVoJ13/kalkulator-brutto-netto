package com.savoj.vatkalkulator

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode

/** Від цієї ширини екран ділиться на дві колонки: введення ліворуч, результат праворуч. */
private val WideBreakpoint = 840.dp

/** Ширина колонки на телефоні й невеликому планшеті. */
private val PhoneContentWidth = 560.dp

/** Максимальна ширина вмісту на широких екранах. */
private val WideContentWidth = 1040.dp

/** Лише для відображення помилки: порожнє поле не є помилкою. */
private fun invalidAmount(text: String): Boolean {
    if (text.isBlank()) return false
    val value = text.replace(",", ".").toBigDecimalOrNull()
    return value == null || value.signum() <= 0
}

@Composable
fun SalaryScreen(
    rules: Rules,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    initialBrutto: String = ""
) {
    var text by rememberSaveable { mutableStateOf(initialBrutto) }
    var hourly by rememberSaveable { mutableStateOf(false) }
    var rateText by rememberSaveable { mutableStateOf("") }
    var hoursText by rememberSaveable { mutableStateOf("") }
    var contract by rememberSaveable { mutableStateOf(ContractType.EMPLOYMENT) }
    var zlecenieStatus by rememberSaveable { mutableStateOf(ZlecenieStatus.ONLY_TITLE) }
    var under26 by rememberSaveable { mutableStateOf(false) }
    var outOfTown by rememberSaveable { mutableStateOf(false) }
    var pit2 by rememberSaveable { mutableStateOf(true) }
    var ppk by rememberSaveable { mutableStateOf(false) }
    var ppkReduced by rememberSaveable { mutableStateOf(false) }
    var sickness by rememberSaveable { mutableStateOf(false) }
    var copyright by rememberSaveable { mutableStateOf(false) }

    // Статус студента можливий лише разом із "до 26 років"
    val status = if (zlecenieStatus == ZlecenieStatus.STUDENT_UNDER_26 && !under26) {
        ZlecenieStatus.ONLY_TITLE
    } else {
        zlecenieStatus
    }

    // Погодинна оплата доступна для umowy o pracę і zlecenia
    val hourlyMode = hourly && contract != ContractType.DZIELO

    val brutto: BigDecimal? = if (hourlyMode) {
        val rate = rateText.replace(",", ".").toBigDecimalOrNull()
        val hours = hoursText.replace(",", ".").toBigDecimalOrNull()
        if (rate != null && hours != null) {
            (rate * hours).setScale(2, RoundingMode.HALF_UP)
        } else null
    } else {
        text.replace(",", ".").toBigDecimalOrNull()
    }

    val canReduce = brutto != null &&
            brutto <= rules.minimumWage * rules.ppkReducedThresholdMultiplier
    val options = Options(
        under26 = under26 && contract != ContractType.DZIELO,
        costsOutOfTown = outOfTown,
        pit2 = pit2,
        ppk = ppk,
        ppkReduced = ppkReduced,
        contract = contract,
        zlecenieStatus = status,
        sickness = sickness,
        copyright = copyright
    )
    val result = if (brutto != null && brutto.signum() > 0) {
        SalaryCalculator.calculate(brutto, 1, options, rules)
    } else null

    val scheme = MaterialTheme.colorScheme
    val colors = appColors
    val focusManager = LocalFocusManager.current

    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.app_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            ThemeButton(dark, onToggleTheme)
        }
    }

    // ── Введення: тип договору, сума, опції ──
    val inputContent: @Composable ColumnScope.() -> Unit = {
        SectionLabel(stringResource(R.string.contract_label))
        Segmented(
            options = listOf(
                stringResource(R.string.contract_employment),
                stringResource(R.string.contract_zlecenie),
                stringResource(R.string.contract_dzielo)
            ),
            selectedIndex = contract.ordinal,
            onSelect = { contract = ContractType.values()[it] }
        )

        SectionLabel(stringResource(R.string.section_amount))
        AppCard {
            if (contract != ContractType.DZIELO) {
                Segmented(
                    options = listOf(
                        stringResource(R.string.input_monthly),
                        stringResource(R.string.input_hourly)
                    ),
                    selectedIndex = if (hourly) 1 else 0,
                    onSelect = { hourly = it == 1 },
                    style = SegmentedStyle.Subtle
                )
            }

            if (hourlyMode) {
                AmountField(
                    label = stringResource(R.string.rate_label),
                    value = rateText,
                    onChange = { rateText = it },
                    suffix = "zł/h",
                    isError = invalidAmount(rateText),
                    supportingText = stringResource(R.string.error_amount)
                )
                AmountField(
                    label = stringResource(R.string.hours_label),
                    value = hoursText,
                    onChange = { hoursText = it },
                    isError = invalidAmount(hoursText),
                    supportingText = stringResource(R.string.error_amount)
                )
                if (brutto != null) {
                    BreakdownRow(stringResource(R.string.line_brutto), brutto, bold = true)
                }
            } else {
                AmountField(
                    label = stringResource(R.string.brutto_label),
                    value = text,
                    onChange = { text = it },
                    suffix = "zł",
                    isError = invalidAmount(text),
                    supportingText = stringResource(R.string.error_amount),
                    large = true
                )
            }
        }

        SectionLabel(stringResource(R.string.section_options))
        AppCard(contentPadding = PaddingValues(vertical = 4.dp), spacing = 0.dp) {
            if (contract != ContractType.DZIELO) {
                OptionRow(stringResource(R.string.switch_under26), under26, { under26 = it })
            }

            when (contract) {
                ContractType.EMPLOYMENT -> {
                    OptionRow(stringResource(R.string.switch_out_of_town), outOfTown, { outOfTown = it })
                    OptionRow(stringResource(R.string.switch_ppk), ppk, { ppk = it })
                    OptionRow(
                        stringResource(R.string.switch_ppk_reduced),
                        ppkReduced && canReduce,
                        { ppkReduced = it },
                        enabled = ppk && canReduce,
                        supporting = if (ppk && brutto != null && !canReduce) {
                            stringResource(
                                R.string.ppk_reduced_hint,
                                money(rules.minimumWage * rules.ppkReducedThresholdMultiplier)
                            )
                        } else {
                            null
                        }
                    )
                }
                ContractType.ZLECENIE -> {
                    RowDivider()
                    RadioOptionRow(
                        stringResource(R.string.status_only_title),
                        status == ZlecenieStatus.ONLY_TITLE,
                        { zlecenieStatus = ZlecenieStatus.ONLY_TITLE }
                    )
                    RadioOptionRow(
                        stringResource(R.string.status_other_employment),
                        status == ZlecenieStatus.OTHER_EMPLOYMENT_MIN,
                        { zlecenieStatus = ZlecenieStatus.OTHER_EMPLOYMENT_MIN }
                    )
                    RadioOptionRow(
                        stringResource(R.string.status_student),
                        status == ZlecenieStatus.STUDENT_UNDER_26,
                        { zlecenieStatus = ZlecenieStatus.STUDENT_UNDER_26 },
                        enabled = under26
                    )
                    if (status == ZlecenieStatus.ONLY_TITLE) {
                        OptionRow(stringResource(R.string.switch_sickness), sickness, { sickness = it })
                    }
                    if (status == ZlecenieStatus.STUDENT_UNDER_26) {
                        Text(
                            stringResource(R.string.note_student),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                ContractType.DZIELO -> {
                    OptionRow(stringResource(R.string.switch_copyright), copyright, { copyright = it })
                }
            }

            RowDivider()
            OptionRow(stringResource(R.string.switch_pit2), pit2, { pit2 = it })
        }
    }

    // ── Результат: розподіл брутто, попередження, примітки ──
    val resultContent: @Composable ColumnScope.() -> Unit = {
        if (result != null) {
            val zus = result.emerytalna + result.rentowa + result.chorobowa

            SectionLabel(stringResource(R.string.section_breakdown))
            AppCard {
                SplitBar(
                    listOf(
                        result.netto.toFloat() to colors.netto,
                        zus.toFloat() to colors.zus,
                        result.zdrowotna.toFloat() to colors.health,
                        result.pit.toFloat() to colors.tax,
                        result.ppkEmployee.toFloat() to colors.ppk
                    )
                )
                Column {
                    BreakdownRow(stringResource(R.string.line_brutto), result.brutto, bold = true)
                    if (result.emerytalna.signum() != 0) {
                        BreakdownRow(stringResource(R.string.line_emerytalna), result.emerytalna, dot = colors.zus, deduction = true)
                    }
                    if (result.rentowa.signum() != 0) {
                        BreakdownRow(stringResource(R.string.line_rentowa), result.rentowa, dot = colors.zus, deduction = true)
                    }
                    if (result.chorobowa.signum() != 0) {
                        BreakdownRow(stringResource(R.string.line_chorobowa), result.chorobowa, dot = colors.zus, deduction = true)
                    }
                    if (result.zdrowotna.signum() != 0) {
                        BreakdownRow(stringResource(R.string.line_zdrowotna), result.zdrowotna, dot = colors.health, deduction = true)
                    }
                    BreakdownRow(stringResource(R.string.line_pit), result.pit, dot = colors.tax, deduction = true)
                    if (result.ppkEmployee.signum() != 0) {
                        BreakdownRow(stringResource(R.string.line_ppk_employee), result.ppkEmployee, dot = colors.ppk, deduction = true)
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = scheme.outlineVariant
                    )
                    BreakdownRow(stringResource(R.string.line_netto), result.netto, dot = colors.netto, bold = true)
                }
            }

            if ("YOUTH_LIMIT_EXCEEDED" in result.warnings) {
                Callout(stringResource(R.string.warning_youth_limit))
            }
            if ("COPYRIGHT_LIMIT_REACHED" in result.warnings) {
                Callout(stringResource(R.string.warning_copyright_limit))
            }
        } else {
            Text(
                stringResource(R.string.empty_hint),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.rules_version, rules.version),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }

    val nettoShown = result?.netto
    val bruttoShown = if (result != null) brutto else null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideBreakpoint
        val gutter = if (wide) 24.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                )
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            if (wide) {
                // Планшет і ландшафт: дві незалежно прокручувані колонки, результат завжди перед очима
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = WideContentWidth)
                            .fillMaxSize()
                            .padding(horizontal = gutter)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp)) { header() }
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                content = inputContent
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ResultSummary(nettoShown, bruttoShown)
                                resultContent()
                            }
                        }
                    }
                }
            } else {
                // Телефон: одна колонка, підсумок «на руки» закріплений унизу
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = PhoneContentWidth)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = gutter, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        header()
                        inputContent()
                        resultContent()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ResultSummary(
                        netto = nettoShown,
                        brutto = bruttoShown,
                        modifier = Modifier
                            .widthIn(max = PhoneContentWidth)
                            .padding(horizontal = gutter, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

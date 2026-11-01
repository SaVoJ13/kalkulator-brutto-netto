package com.savoj.vatkalkulator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            // Правила: спочатку збережена/вбудована копія, потім оновлення з інтернету у фоні
            var rules by remember { mutableStateOf(RulesRepository.loadLocal(context)) }
            LaunchedEffect(Unit) {
                val fresh = withContext(Dispatchers.IO) { RulesRepository.fetchRemote(context) }
                if (fresh != null) rules = fresh
            }

            val systemDark = isSystemInDarkTheme()
            var dark by rememberSaveable {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                mutableStateOf(
                    if (prefs.contains("dark_theme")) prefs.getBoolean("dark_theme", false)
                    else systemDark
                )
            }
            AppTheme(dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SalaryScreen(rules, dark) {
                        dark = !dark
                        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                            .edit().putBoolean("dark_theme", dark).apply()
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeButton(dark: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .clickable(
                onClickLabel = stringResource(R.string.theme_label),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        ThemeIcon(dark, scheme.onSurface)
    }
}

@Composable
fun ThemeIcon(dark: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val c = center
        val r = size.minDimension / 2f
        if (dark) {
            // темна тема: показуємо сонце (натиснути = світла тема)
            val stroke = 2.dp.toPx()
            drawCircle(color, radius = r * 0.38f, style = Stroke(width = stroke))
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val dx = cos(a).toFloat()
                val dy = sin(a).toFloat()
                drawLine(
                    color,
                    Offset(c.x + dx * r * 0.62f, c.y + dy * r * 0.62f),
                    Offset(c.x + dx * r * 0.92f, c.y + dy * r * 0.92f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        } else {
            // світла тема: показуємо місяць
            val outer = Path().apply { addOval(Rect(c, r * 0.85f)) }
            val cut = Path().apply {
                addOval(Rect(Offset(c.x + r * 0.45f, c.y - r * 0.25f), r * 0.75f))
            }
            val moon = Path()
            moon.op(outer, cut, PathOperation.Difference)
            drawPath(moon, color)
        }
    }
}

@Composable
fun SalaryScreen(
    rules: Rules,
    dark: Boolean,
    onToggleTheme: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
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
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.app_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                ThemeButton(dark, onToggleTheme)
            }

            Segmented(
                options = listOf(
                    stringResource(R.string.contract_employment),
                    stringResource(R.string.contract_zlecenie),
                    stringResource(R.string.contract_dzielo)
                ),
                selectedIndex = contract.ordinal,
                onSelect = { contract = ContractType.values()[it] }
            )

            AppCard {
                if (contract != ContractType.DZIELO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !hourly,
                            onClick = { hourly = false },
                            label = { Text(stringResource(R.string.input_monthly)) }
                        )
                        FilterChip(
                            selected = hourly,
                            onClick = { hourly = true },
                            label = { Text(stringResource(R.string.input_hourly)) }
                        )
                    }
                }

                if (hourlyMode) {
                    FieldRow(stringResource(R.string.rate_label), rateText) { rateText = it }
                    FieldRow(stringResource(R.string.hours_label), hoursText) { hoursText = it }
                    if (brutto != null) {
                        Line(stringResource(R.string.line_brutto), brutto)
                    }
                } else {
                    FieldRow(stringResource(R.string.brutto_label), text) { text = it }
                }
            }

            AppCard {
                if (contract != ContractType.DZIELO) {
                    SwitchRow(stringResource(R.string.switch_under26), under26, { under26 = it })
                }

                when (contract) {
                    ContractType.EMPLOYMENT -> {
                        SwitchRow(stringResource(R.string.switch_out_of_town), outOfTown, { outOfTown = it })
                        SwitchRow(stringResource(R.string.switch_ppk), ppk, { ppk = it })
                        SwitchRow(
                            stringResource(R.string.switch_ppk_reduced),
                            ppkReduced && canReduce,
                            { ppkReduced = it },
                            enabled = ppk && canReduce
                        )
                    }
                    ContractType.ZLECENIE -> {
                        RadioRow(
                            stringResource(R.string.status_only_title),
                            status == ZlecenieStatus.ONLY_TITLE,
                            { zlecenieStatus = ZlecenieStatus.ONLY_TITLE }
                        )
                        RadioRow(
                            stringResource(R.string.status_other_employment),
                            status == ZlecenieStatus.OTHER_EMPLOYMENT_MIN,
                            { zlecenieStatus = ZlecenieStatus.OTHER_EMPLOYMENT_MIN }
                        )
                        RadioRow(
                            stringResource(R.string.status_student),
                            status == ZlecenieStatus.STUDENT_UNDER_26,
                            { zlecenieStatus = ZlecenieStatus.STUDENT_UNDER_26 },
                            enabled = under26
                        )
                        if (status == ZlecenieStatus.ONLY_TITLE) {
                            SwitchRow(stringResource(R.string.switch_sickness), sickness, { sickness = it })
                        }
                        if (status == ZlecenieStatus.STUDENT_UNDER_26) {
                            Text(
                                stringResource(R.string.note_student),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                    ContractType.DZIELO -> {
                        SwitchRow(stringResource(R.string.switch_copyright), copyright, { copyright = it })
                    }
                }

                SwitchRow(stringResource(R.string.switch_pit2), pit2, { pit2 = it })
            }

            if (result != null) {
                AppCard {
                    val zus = result.emerytalna + result.rentowa + result.chorobowa
                    val taxes = result.pit + result.ppkEmployee
                    SplitBar(
                        listOf(
                            result.netto.toFloat() to Color(0xFF2FBF8A),
                            zus.toFloat() to Color(0xFFF2B84B),
                            result.zdrowotna.toFloat() to Color(0xFF5B9BF0),
                            taxes.toFloat() to Color(0xFFEF7B66)
                        )
                    )
                    if (result.emerytalna.signum() != 0) Line(stringResource(R.string.line_emerytalna), result.emerytalna)
                    if (result.rentowa.signum() != 0) Line(stringResource(R.string.line_rentowa), result.rentowa)
                    if (result.chorobowa.signum() != 0) Line(stringResource(R.string.line_chorobowa), result.chorobowa)
                    if (result.zdrowotna.signum() != 0) Line(stringResource(R.string.line_zdrowotna), result.zdrowotna)
                    Line(stringResource(R.string.line_pit), result.pit)
                    if (result.ppkEmployee.signum() != 0) Line(stringResource(R.string.line_ppk_employee), result.ppkEmployee)
                }

                if ("YOUTH_LIMIT_EXCEEDED" in result.warnings) {
                    Text(
                        stringResource(R.string.warning_youth_limit),
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if ("COPYRIGHT_LIMIT_REACHED" in result.warnings) {
                    Text(
                        stringResource(R.string.warning_copyright_limit),
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                "rules " + rules.version,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        NettoBar(
            netto = result?.netto ?: BigDecimal.ZERO,
            brutto = brutto ?: BigDecimal.ZERO
        )
    }
}

@Composable
fun NettoBar(netto: BigDecimal, brutto: BigDecimal) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(shape)
            .background(scheme.primaryContainer)
            .border(1.dp, scheme.outlineVariant, shape)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    stringResource(R.string.line_netto),
                    color = scheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    money(netto),
                    color = scheme.secondary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.line_brutto),
                    color = scheme.onPrimaryContainer.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
                Text(
                    money(brutto),
                    color = scheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun AppCard(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = scheme.surface,
            contentColor = scheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun FieldRow(label: String, value: String, onChange: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            fontWeight = FontWeight.SemiBold
        )
        TextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Bold
            ),
            placeholder = {
                Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = scheme.surfaceVariant,
                unfocusedContainerColor = scheme.surfaceVariant,
                disabledContainerColor = scheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.width(150.dp)
        )
    }
}

@Composable
fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) scheme.primary else Color.Transparent)
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(index) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun SplitBar(parts: List<Pair<Float, Color>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        parts.forEach { (weight, color) ->
            if (weight > 0f) {
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, lineHeight = 18.sp)
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun Line(label: String, value: BigDecimal, bold: Boolean = false) {
    val weight = if (bold) FontWeight.Bold else null
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 12.dp), fontWeight = weight)
        Text(money(value), fontWeight = weight)
    }
}

fun money(value: BigDecimal): String =
    String.format(Locale.forLanguageTag("pl-PL"), "%,.2f zł", value)
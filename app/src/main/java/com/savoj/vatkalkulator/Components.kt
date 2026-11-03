package com.savoj.vatkalkulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Мінімальна зона дотику за рекомендаціями Material та WCAG. */
private val MinTouch = 48.dp

// ───────────────────────── Контейнери ─────────────────────────

/** Заголовок секції. Позначений як heading, щоб TalkBack міг стрибати між секціями. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .padding(start = 4.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = scheme.surface,
            contentColor = scheme.onSurface
        ),
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// ───────────────────────── Вибір ─────────────────────────

enum class SegmentedStyle {
    /** Основний вибір: залита акцентом активна опція. */
    Primary,

    /** Другорядний вибір: біла «пігулка» на сірій доріжці. */
    Subtle
}

@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: SegmentedStyle = SegmentedStyle.Primary
) {
    val scheme = MaterialTheme.colorScheme
    val trackShape = RoundedCornerShape(16.dp)
    val itemShape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(scheme.surfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val container = when {
                !selected -> Color.Transparent
                style == SegmentedStyle.Primary -> scheme.primary
                else -> scheme.surfaceBright
            }
            val content = when {
                !selected -> scheme.onSurfaceVariant
                style == SegmentedStyle.Primary -> scheme.onPrimary
                else -> scheme.onSurface
            }
            val outline = if (selected && style == SegmentedStyle.Subtle) {
                Modifier.border(1.dp, scheme.outlineVariant, itemShape)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinTouch)
                    .clip(itemShape)
                    .background(container)
                    .then(outline)
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(index) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = content,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

/** Рядок із перемикачем. Натискається весь рядок, а не лише сам перемикач. */
@Composable
fun OptionRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    supporting: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val textColor = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = textColor)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Рядок із радіокнопкою. Натискається весь рядок. */
@Composable
fun RadioOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val textColor = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            label,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

// ───────────────────────── Введення ─────────────────────────

@Composable
fun AmountField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    large: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        suffix = if (suffix != null) {
            { Text(suffix) }
        } else {
            null
        },
        isError = isError,
        supportingText = if (isError && supportingText != null) {
            { Text(supportingText) }
        } else {
            null
        },
        textStyle = if (large) {
            MaterialTheme.typography.headlineSmall
        } else {
            MaterialTheme.typography.titleMedium
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
            errorContainerColor = scheme.surfaceVariant,
            focusedBorderColor = scheme.primary,
            unfocusedBorderColor = scheme.outline,
            errorBorderColor = scheme.error
        )
    )
}

// ───────────────────────── Результат ─────────────────────────

/**
 * Смуга розподілу брутто. Лише ілюстрація: ті самі дані читаються з рядків під нею,
 * тому для скрінрідера вона прихована.
 */
@Composable
fun SplitBar(parts: List<Pair<Float, Color>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .clearAndSetSemantics { },
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

/** Рядок розрахунку. [dot] збігається з кольором сегмента у [SplitBar]. */
@Composable
fun BreakdownRow(
    label: String,
    value: BigDecimal,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    bold: Boolean = false,
    deduction: Boolean = false
) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            label,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = weight
        )
        Text(
            (if (deduction && value.signum() != 0) "\u2212" else "") + money(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = weight,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Підсумок «на руки»: головне число екрана. Оголошується скрінрідером при зміні.
 * Мітки в одному рядку, а велика сума займає всю ширину, тож довгі числа
 * й вузькі екрани не накладаються на «Brutto».
 */
@Composable
fun ResultSummary(
    netto: BigDecimal?,
    brutto: BigDecimal?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val nettoText = if (netto != null) money(netto) else "\u2014"
    val bruttoText = stringResource(R.string.line_brutto) + "  " + (if (brutto != null) money(brutto) else "\u2014")
    // Дуже великі суми трохи зменшуємо, щоб вони завжди вміщалися в один рядок
    val amountStyle = if (nettoText.length > 12) {
        MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 34.sp)
    } else {
        MaterialTheme.typography.displaySmall
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(scheme.primaryContainer)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                stringResource(R.string.line_netto),
                modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp),
                color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                bruttoText,
                color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End
            )
        }
        Text(
            nettoText,
            color = scheme.secondary,
            style = amountStyle,
            maxLines = 1
        )
    }
}

/** Попередження про наближений результат. */
@Composable
fun Callout(text: String) {
    val colors = appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.warningContainer)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        WarningIcon(colors.onWarningContainer)
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onWarningContainer
        )
    }
}

// ───────────────────────── Іконки ─────────────────────────

@Composable
fun WarningIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp).clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        val triangle = Path().apply {
            moveTo(w / 2f, h * 0.10f)
            lineTo(w * 0.94f, h * 0.88f)
            lineTo(w * 0.06f, h * 0.88f)
            close()
        }
        drawPath(triangle, color, style = Stroke(width = stroke, join = StrokeJoin.Round))
        drawLine(
            color,
            Offset(w / 2f, h * 0.38f),
            Offset(w / 2f, h * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(color, radius = stroke * 0.6f, center = Offset(w / 2f, h * 0.76f))
    }
}

@Composable
fun ThemeButton(dark: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(R.string.theme_label)
    Box(
        modifier = Modifier
            .size(MinTouch)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        ThemeIcon(dark, scheme.onSurface)
    }
}

@Composable
fun ThemeIcon(dark: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(24.dp).clearAndSetSemantics { }) {
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

/** Сума у польському форматі. Пробіл перед «zł» нерозривний, щоб символ не відривався від числа. */
fun money(value: BigDecimal): String =
    String.format(Locale.forLanguageTag("pl-PL"), "%,.2f\u00A0zł", value)

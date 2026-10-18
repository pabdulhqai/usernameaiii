package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@Composable
fun QuickCalculatorDialog(
    onDismiss: () -> Unit
) {
    var display by remember { mutableStateOf("0") }
    var accumulator by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var resetNext by remember { mutableStateOf(false) }

    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer

    fun onDigit(digit: String) {
        if (resetNext || display == "0") {
            display = if (digit == ".") "0." else digit
            resetNext = false
        } else if (digit != "." || !display.contains(".")) {
            display += digit
        }
    }

    fun evaluate(rhs: Double): Double {
        val lhs = accumulator ?: return rhs
        return when (pendingOp) {
            "+" -> lhs + rhs
            "−" -> lhs - rhs
            "×" -> lhs * rhs
            "÷" -> if (rhs != 0.0) lhs / rhs else 0.0
            else -> rhs
        }
    }

    fun formatNum(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun onOperator(op: String) {
        val current = display.toDoubleOrNull() ?: 0.0
        if (accumulator != null && !resetNext) {
            val res = evaluate(current)
            accumulator = res
            display = formatNum(res)
        } else {
            accumulator = current
        }
        pendingOp = op
        resetNext = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, accent, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OLED ZERO-POWER CALC",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                    TextButton(onClick = onDismiss) {
                        Text("CLOSE", color = accent)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF070D0A))
                        .border(1.dp, dimAccent, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = display,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color.White,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val rows = listOf(
                    listOf("C", "±", "÷", "×"),
                    listOf("7", "8", "9", "−"),
                    listOf("4", "5", "6", "+"),
                    listOf("1", "2", "3", "="),
                    listOf("0", ".")
                )

                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val isOperator = key in listOf("÷", "×", "−", "+", "=")
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(if (key == "0") 2f else 1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isOperator) accent.copy(alpha = 0.2f)
                                        else Color(0xFF0B130F)
                                    )
                                    .border(
                                        1.dp,
                                        if (isOperator) accent else dimAccent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        when (key) {
                                            "C" -> {
                                                display = "0"
                                                accumulator = null
                                                pendingOp = null
                                                resetNext = false
                                            }
                                            "±" -> {
                                                val v = display.toDoubleOrNull() ?: 0.0
                                                display = formatNum(-v)
                                            }
                                            "+", "−", "×", "÷" -> onOperator(key)
                                            "=" -> {
                                                val current = display.toDoubleOrNull() ?: 0.0
                                                val res = evaluate(current)
                                                display = formatNum(res)
                                                accumulator = null
                                                pendingOp = null
                                                resetNext = true
                                            }
                                            else -> onDigit(key)
                                        }
                                    }
                            ) {
                                Text(
                                    text = key,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isOperator) accent else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.fritangui.wakeup.alarm.ui.challenge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class MathProblem(val text: String, val answer: Int)

private fun generateProblem(difficulty: Int): MathProblem {
    val max = 10 + difficulty * 15
    val a = Random.nextInt(2, max)
    val b = Random.nextInt(2, max)
    return when (Random.nextInt(if (difficulty >= 2) 3 else 2)) {
        0 -> MathProblem("$a + $b", a + b)
        1 -> MathProblem("$a - $b", a - b)
        else -> {
            val small1 = Random.nextInt(2, 12)
            val small2 = Random.nextInt(2, 12)
            MathProblem("$small1 × $small2", small1 * small2)
        }
    }
}

/** Reto de operación matemática: obliga a que el cerebro despierte lo suficiente para calcular. */
@Composable
fun MathChallenge(difficulty: Int, onCompleted: () -> Unit) {
    var problem by remember { mutableStateOf(generateProblem(difficulty)) }
    var input by rememberSaveable { mutableStateOf("") }
    var isWrong by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Resuelve para apagar la alarma")
        Text(problem.text, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it.filter { c -> c.isDigit() || c == '-' }
                isWrong = false
            },
            label = { Text("Respuesta") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isWrong,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (isWrong) Text("Intenta de nuevo", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = {
                if (input.toIntOrNull() == problem.answer) {
                    onCompleted()
                } else {
                    isWrong = true
                    input = ""
                    problem = generateProblem(difficulty)
                }
            }) {
                Text("Confirmar")
            }
        }
    }
}

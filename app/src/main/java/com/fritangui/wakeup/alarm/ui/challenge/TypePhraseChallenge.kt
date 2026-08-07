package com.fritangui.wakeup.alarm.ui.challenge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private val SHORT_PHRASES = listOf(
    "ya me desperté",
    "vamos a clase",
    "un café ayudaría",
    "hoy toca estudiar",
    "arriba y a la carga",
    "buenos días mundo",
    "listo para el día",
)

/** Reto de escribir exactamente una frase corta generada al azar: obliga a leer y teclear con cuidado. */
@Composable
fun TypePhraseChallenge(difficulty: Int, onCompleted: () -> Unit) {
    val target = remember { SHORT_PHRASES.random(Random(System.nanoTime())) }
    var input by rememberSaveable { mutableStateOf("") }
    var isWrong by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Escribe exactamente esta frase:")
        Text(
            text = "“$target”",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                isWrong = false
            },
            label = { Text("Tu respuesta") },
            singleLine = true,
            isError = isWrong,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        if (isWrong) Text("No coincide, revisa mayúsculas y tildes", color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                if (input.trim().equals(target, ignoreCase = false)) {
                    onCompleted()
                } else {
                    isWrong = true
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Confirmar")
        }
    }
}

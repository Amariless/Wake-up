package com.fritangui.wakeup.ui.clock.alarms

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.alarm.sound.AlarmSoundPreviewPlayer

/**
 * Elegir un sonido: los del banco propio de la app (con vista previa) o cualquier tono del
 * sistema. Genérico para reutilizarlo tanto con el banco de alarmas como con el de
 * notificaciones (los "recordatorios", ver #59) — cada llamador pasa su propia lista de
 * (etiqueta, uri) y el tipo de selector de tonos del sistema que le corresponde.
 */
@Composable
fun AlarmSoundPickerDialog(
    currentUri: String?,
    bundledSounds: List<Pair<String, String>>,
    title: String = "Sonido de la alarma",
    ringtoneType: Int = RingtoneManager.TYPE_ALARM,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val previewPlayer: AlarmSoundPreviewPlayer = hiltViewModel<AlarmSoundPreviewHolderViewModel>().previewPlayer
    val playingUri by previewPlayer.playingUri.collectAsState()

    val systemPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            ?: @Suppress("DEPRECATION") result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) onSelect(uri.toString())
    }

    AlertDialog(
        onDismissRequest = { previewPlayer.stop(); onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                bundledSounds.forEachIndexed { index, (label, uri) ->
                    val isDefault = currentUri == null && index == 0
                    Row2(
                        selected = currentUri == uri || isDefault,
                        label = label,
                        isPlaying = playingUri == uri,
                        onSelect = { onSelect(uri) },
                        onPreview = { previewPlayer.toggle(uri) },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TextButton(onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                        currentUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                    }
                    systemPickerLauncher.launch(intent)
                }) { Text("Elegir un tono del sistema…") }
            }
        },
        confirmButton = { TextButton(onClick = { previewPlayer.stop(); onDismiss() }) { Text("Listo") } },
    )
}

@Composable
private fun Row2(selected: Boolean, label: String, isPlaying: Boolean, onSelect: () -> Unit, onPreview: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = onPreview) {
            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "Previsualizar $label")
        }
    }
}

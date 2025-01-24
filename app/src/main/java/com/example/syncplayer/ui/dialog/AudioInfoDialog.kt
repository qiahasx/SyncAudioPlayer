package com.example.syncplayer.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.syncplayer.LocalDialogManager
import com.example.syncplayer.audio.AudioTranscoder
import com.example.syncplayer.model.AudioItem

class AudioInfoDialog(
    val audioItem: AudioItem,
    val isCancel: Boolean = true,
    val onDismissRequest: (AudioInfoDialog) -> Unit = {},
) : DialogController()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioInfoDialog(
    controller: AudioInfoDialog,
) {
    val scope = rememberCoroutineScope()
    val transcoder = remember(controller) { AudioTranscoder(controller.audioItem, scope) }
    var sample by remember(controller) { mutableStateOf("${transcoder.getInputFormat().sampleRate}") }
    var channels by remember(controller) { mutableStateOf(transcoder.getInputFormat().channelNum) }
    val dialogManager = LocalDialogManager.current
    controller.setDismiss {
        dialogManager.dismiss(controller)
        transcoder.release()
    }

    Dialog(onDismissRequest = { if (controller.isCancel) controller.dismiss() else controller.onDismissRequest(controller) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                text = "Audio Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight(900),
            )
            OutlinedTextField(
                value = sample,
                onValueChange = { input ->
                    val newValue = if (input.isEmpty()) "" else input.toIntOrNull() ?: return@OutlinedTextField
                    sample = newValue.toString()
                },
                label = { Text("Sample Rate") },
                modifier = Modifier.padding(top = 14.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Channels", Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow {
                    AudioTranscoder.Channels.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AudioTranscoder.Channels.entries.size
                            ),
                            onClick = { channels = AudioTranscoder.Channels.entries[index] },
                            selected = index == channels.ordinal,
                            label = { Text(entry.name) }
                        )
                    }
                }
            }
            Row {
                OutlinedButton(
                    {
                        controller.dismiss()
                    },
                    Modifier
                        .weight(1f)
                        .padding(0.dp, 16.dp, 8.dp, 0.dp)
                        .fillMaxWidth()
                ) {
                    Text("Cancel")
                }
                Button(
                    {},
                    Modifier
                        .weight(1f)
                        .padding(8.dp, 16.dp, 0.dp, 0.dp)
                        .fillMaxWidth()
                ) {
                    Text("Transcoder")
                }
            }
        }
    }
}
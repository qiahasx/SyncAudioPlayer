package com.example.syncplayer.ui.dialog

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.syncplayer.LocalDialogManager
import com.example.syncplayer.R
import com.example.syncplayer.util.debug
import kotlinx.coroutines.flow.Flow

class ProgressDialogController(
    val progress: Flow<Float>,
    val text: String = "",
    val onSuccess: (ProgressDialogController) -> Unit,
) : DialogController()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDialog(
    controller: ProgressDialogController,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
        .padding(16.dp),
) {
    val dialogManager = LocalDialogManager.current
    controller.setDismiss { dialogManager.dismiss(controller) }
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = { controller.dismiss() },
    ) {
        Column(modifier = modifier) {
            LoadingImage(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(44.dp)
            )

            val progress by controller.progress.collectAsState(0f)
            debug("progress: $progress")
            if (progress < 0) {
                controller.onSuccess(controller)
            }
            Slider(
                progress, {},
                Modifier
                    .height(16.dp)
                    .padding(top = 6.dp),
                thumb = { SliderDefaults.Thumb(interactionSource, thumbSize = DpSize(0.dp, 0.dp)) }
            )
            if (controller.text.isNotEmpty()) {
                Text(
                    controller.text,
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LoadingImage(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )
    Image(
        painterResource(R.drawable.ic_loading), "",
        modifier.rotate(angle + 57f)
    )
}
@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.example.syncplayer.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.syncplayer.LocalPickFile
import com.example.syncplayer.LocalSyncPlayer
import com.example.syncplayer.ui.theme.ComposeTheme
import com.example.syncplayer.util.launchIO

@Suppress("ktlint:standard:function-naming")
@Composable
fun HomeLayout() {
    ComposeTheme {
        Scaffold { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PickFileButton()
                StartButton()
                SeekToButton()
            }
        }
    }
}

@Composable
fun PickFileButton() {
    val pickFile = LocalPickFile.current
    FilledTonalButton(onClick = {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf("audio/x-wav", "audio/x-aac", "audio/mpeg"),
                )
            }
        pickFile.launch(intent)
    }, Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        Text(text = "Pick File")
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun StartButton() {
    val play = LocalSyncPlayer.current
    OutlinedButton(onClick = { play.start() }, Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        Text(text = "start")
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SeekToButton() {
    val play = LocalSyncPlayer.current
    val scope = LocalLifecycleOwner.current.lifecycleScope
    ElevatedButton(onClick = {
        scope.launchIO {
            play.seekTo(60 * 1000 * 1000)
        }
    }, Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        Text(text = "seek to 1:00")
    }
}

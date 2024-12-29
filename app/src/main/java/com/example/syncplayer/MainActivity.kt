package com.example.syncplayer

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.example.syncplayer.audio.SyncPlayer
import com.example.syncplayer.ui.NavGraph
import com.example.syncplayer.util.debug
import com.example.syncplayer.util.launchMain
import com.example.syncplayer.viewModel.MainViewModel
import com.example.syncplayer.viewModel.MainViewModel.Companion.AUDIO_PATH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val syncPlayer = SyncPlayer(lifecycleScope)
    private val viewModel by viewModels<MainViewModel>()

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                launchMain {
                    val uri = it.data?.data ?: return@launchMain debug("uri is null")
                    val fileName = getFileNameFromUri(uri) ?: return@launchMain debug("fileName is null")
                    val file = File(getExternalFilesDir(AUDIO_PATH), fileName)
                    file.outputStream().use { outputStream ->
                        contentResolver.openInputStream(uri)?.copyTo(outputStream)
                    }
                    viewModel.updateItem()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalPickFile provides pickFile,
                LocalMainViewModel provides viewModel,
            ) {
                NavGraph()
            }
        }
    }

    private suspend fun getFileNameFromUri(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            var result: String? = null
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = it.getString(displayNameIndex)
                    }
                }
            }
            result
        }
}

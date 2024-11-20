package com.example.syncplayer

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.syncplayer.audio.SyncPlayer
import com.example.syncplayer.databinding.ActivityMainBinding
import com.example.syncplayer.util.debug
import java.io.File

class MainActivity : ComponentActivity() {
    private val pickFile =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val uri =
                    it.data?.data
                        ?: return@registerForActivityResult debug("uri is null")
                val file = File(getExternalFilesDir("picked"), "${System.currentTimeMillis()}.m4a")
                file.outputStream().use {
                    contentResolver.openInputStream(uri)?.copyTo(it)
                }
                val syncPlayer = SyncPlayer()
                syncPlayer.setDataSource(file.absolutePath)
                syncPlayer.start()
            }
        }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickFile.run {
            background =
                GradientDrawable().apply {
                    setColor(Color.CYAN)
                    cornerRadius = layoutParams.height / 2.0f
                }
            setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "audio/*"
                        putExtra(
                            Intent.EXTRA_MIME_TYPES,
                            arrayOf("audio/x-wav", "audio/x-aac", "audio/mpeg"),
                        )
                    }
                pickFile.launch(intent)
            }
        }

        binding.btnAction.run {
            background =
                GradientDrawable().apply {
                    setColor(Color.MAGENTA)
                    cornerRadius = layoutParams.height / 2.0f
                }
            setOnClickListener {
//                val pcmPlayer = PCMPlayer(lifecycleScope, pcm)
//                pcmPlayer.play(decoder.afterDecodeQueue)
            }
            isEnabled = false
        }
    }
}

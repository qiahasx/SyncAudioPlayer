package com.example.syncplayer

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.syncplayer.audio.Decoder
import com.example.syncplayer.audio.PCMPlayer
import com.example.syncplayer.databinding.ActivityMainBinding
import com.example.syncplayer.util.launchIO
import java.io.File

class MainActivity : ComponentActivity() {
    private val player by lazy {
        SyncPlayer(lifecycleScope)
    }
    private lateinit var pcm: File

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val uri =
                    it.data?.data
                        ?: return@registerForActivityResult com.example.syncplayer.debug("uri is null")
                val file = File(getExternalFilesDir("picked"), "${System.currentTimeMillis()}.m4a")
                file.outputStream().use {
                    contentResolver.openInputStream(uri)?.copyTo(it)
                }
                pcm = File(getExternalFilesDir("pcm"), "${System.currentTimeMillis()}.pcm")
                lifecycleScope.launchIO {
                    Decoder(file.absolutePath).decodeToPCMFile(pcm.absolutePath)
                }
//                AudioDecoder(lifecycleScope, file.absolutePath).run {
//                    val start = System.currentTimeMillis()
//                    debug("decoder $start")
//                    start()
//                    lifecycleScope.launchIO {
//                        pcm.outputStream().use { ops ->
//                            val bytes = ByteArray(AudioDecoder.BUFFER_SIZE)
//                            while (true) {
//                                val consume = afterDecodeQueue.consume() ?: return@launchIO
//                                consume.buffer.get(bytes, consume.offset, consume.size)
//                                ops.write(bytes)
//                                if (consume.flags == 4) break
//                            }
//                        }
//                        debug("decoder end${System.currentTimeMillis() - start} fileLength: ${pcm.length()}")
//                    }
//                }
//                debug("get picked ${file.absolutePath}")
//                player.setData(file.absolutePath)
//                binding.btnAction.isEnabled = true
//                binding.btnPickFile.isEnabled = false
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
                val pcmPlayer = PCMPlayer(lifecycleScope, pcm)
                pcmPlayer.play()
            }
            isEnabled = false
        }
    }

    /**
     * A native method that is implemented by the 'syncplayer' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'syncplayer' library on application startup.
        init {
            System.loadLibrary("syncPlayer")
        }
    }
}

package com.example.syncplayer.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncplayer.LocalMainViewModel
import com.example.syncplayer.R
import com.example.syncplayer.model.AudioItem
import com.example.syncplayer.ui.theme.ComposeTheme
import com.example.syncplayer.ui.theme.Purple200
import com.example.syncplayer.ui.theme.Teal200

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayLayout() {
    val viewModel = LocalMainViewModel.current
    remember {
        viewModel.initPlayer()
    }
    ComposeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors =
                    topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Play Track")
                    },
                )
            },
        ) {
            AudioTrackList(it)
        }
    }
}

@SuppressLint("RememberReturnType")
@Composable
fun PlayController() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
            .padding(12.dp),
    ) {
        val viewModel = LocalMainViewModel.current
        val duration by viewModel.totalDuration.collectAsState()
        val playing by viewModel.isPlaying.collectAsState()
        var progress by remember { mutableFloatStateOf(0f) }
        var isUserInteracting by remember { mutableStateOf(false) }
        LaunchedEffect(viewModel, isUserInteracting) {
            if (isUserInteracting) return@LaunchedEffect
            viewModel.playProgress.collect {
                progress = it
            }
        }
        Slider(
            value = progress,
            onValueChange = { newValue ->
                progress = newValue
                isUserInteracting = true
            },
            onValueChangeFinished = {
                isUserInteracting = false
                viewModel.seekTo(progress)
            },
            colors =
            SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White,
            ),
            valueRange = 0f..duration,
            modifier = Modifier
                .height(22.dp)
                .padding(end = 12.dp)
        )
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(0.dp, 12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.back),
                contentDescription = "",
                Modifier
                    .size(44.dp)
                    .padding(end = 12.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { viewModel.backward() },
            )
            Image(
                painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
                contentDescription = "",
                Modifier
                    .size(60.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { viewModel.togglePlay() },
            )
            Image(
                painter = painterResource(id = R.drawable.forwrad),
                contentDescription = "",
                Modifier
                    .size(44.dp)
                    .padding(start = 12.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { viewModel.forward() },
            )
        }
    }
}

@Composable
fun AudioTrackList(paddingValues: PaddingValues) {
    val viewModel = LocalMainViewModel.current
    val itemList by viewModel.items.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        LazyColumn(
            Modifier
                .weight(1f)
                .padding(0.dp, 6.dp),
        ) {
            var index = 0
            items(itemList) {
                TrackItem(it, index++)
            }
        }
        PlayController()
    }
}

@Composable
fun TrackItem(
    item: AudioItem,
    index: Int,
) {
    val viewModel = LocalMainViewModel.current
    var volume by remember(item) { item.volume }
    Row(
        Modifier
            .padding(16.dp, 6.dp)
            .background(Color.White, RoundedCornerShape(16.dp)),
    ) {
        val icon = painterResource(id = if (index % 2 == 0) R.drawable.track else R.drawable.track_2)
        val color = if (index % 2 == 0) Teal200 else Purple200
        Image(
            painter = icon,
            contentDescription = "",
            Modifier
                .size(44.dp)
                .padding(12.dp)
                .align(Alignment.CenterVertically),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(0.dp, 6.dp),
        ) {
            Text(text = item.name, fontWeight = FontWeight(400), fontSize = 12.sp, color = Color.Gray)
            Slider(
                value = volume,
                onValueChange = { volume = it },
                onValueChangeFinished = { viewModel.setVolume(item, volume) },
                colors =
                SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = Color.White,
                ),
                steps = 10,
                valueRange = 0f..1f,
                modifier = Modifier
                    .height(22.dp)
                    .padding(end = 12.dp),
            )
        }
    }
}

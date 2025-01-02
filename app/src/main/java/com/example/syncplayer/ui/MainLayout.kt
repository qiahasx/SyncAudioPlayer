package com.example.syncplayer.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncplayer.Destinations
import com.example.syncplayer.LocalMainViewModel
import com.example.syncplayer.LocalNavController
import com.example.syncplayer.LocalPickFile
import com.example.syncplayer.R
import com.example.syncplayer.model.AudioItem
import com.example.syncplayer.ui.theme.ComposeTheme
import com.example.syncplayer.viewModel.MainViewModel

@Composable
fun MainLayout() {
    ComposeTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = topBar("Pick File"),
            floatingActionButton = addFileButton(),
        ) { innerPadding ->
            ItemList(innerPadding, snackbarHostState)
        }
    }
}

@Composable
fun ItemList(
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel = LocalMainViewModel.current
    val itemList = viewModel.items.collectAsState().value
    val navController = LocalNavController.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
    ) {
        LazyColumn(
            Modifier
                .weight(1f)
                .padding(16.dp, 8.dp),
        ) {
            items(itemList) {
                AudioItem(it)
            }
        }
        ElevatedButton(
            onClick = {
                viewModel.onClickStart()
            },
            Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            Text(text = "start", fontWeight = FontWeight(600), fontSize = 20.sp)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is MainViewModel.NavigationEvent.NavigateToNextScreen -> {
                    navController.navigate(Destinations.PLAY_ROUTE)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
fun AudioItem(item: AudioItem) {
    val viewModel = LocalMainViewModel.current
    Row(
        Modifier
            .padding(0.dp, 6.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable {
                viewModel.deleteItem(item)
            },
    ) {
        Image(
            painter = painterResource(id = R.drawable.track),
            contentDescription = "",
            Modifier
                .size(44.dp)
                .padding(12.dp),
        )
        Text(
            item.name,
            Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
        )
        Image(
            painter = painterResource(id = R.drawable.close_2),
            contentDescription = "",
            Modifier
                .size(44.dp)
                .padding(4.dp)
                .clickable {
                    viewModel.deleteItem(item)
                },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun topBar(title: String) =
    @Composable {
        TopAppBar(
            colors =
                topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            title = {
                Text(title)
            },
        )
    }

fun addFileButton() =
    @Composable {
        val pickFile = LocalPickFile.current
        FloatingActionButton(onClick = {
            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "audio/*"
                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf("audio/x-wav", "audio/x-aac", "audio/mpeg"),
                    )
                }
            pickFile.launch(intent)
        }) {
            Icon(Icons.Default.Add, contentDescription = "Pick File")
        }
    }

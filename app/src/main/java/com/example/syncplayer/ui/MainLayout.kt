package com.example.syncplayer.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.syncplayer.Destinations
import com.example.syncplayer.LocalMainViewModel
import com.example.syncplayer.LocalNavController
import com.example.syncplayer.LocalPickFile
import com.example.syncplayer.R
import com.example.syncplayer.model.AudioItem
import com.example.syncplayer.util.debug
import com.example.syncplayer.viewModel.MainViewModel

@Composable
fun MainLayout() {
    MaterialTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = topBar(),
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
            Modifier.weight(1f),
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
            debug("Navigation event: $event")
            when (event) {
                is MainViewModel.NavigationEvent.NavigateToNextScreen -> {
                    navController.navigate(Destinations.PLAY_ROUTE)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            debug("message $message")
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
fun AudioItem(item: AudioItem) {
    val viewModel = LocalMainViewModel.current
    Row(
        Modifier
            .padding(0.dp, 8.dp)
            .fillMaxWidth()
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
            painter = painterResource(id = R.drawable.close),
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
fun topBar() =
    @Composable {
        TopAppBar(
            colors =
                topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            title = {
                Text("Top app bar")
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

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    start: String = Destinations.HOME_ROUTE, // 默认的初始页面为主页
) {
    CompositionLocalProvider(
        LocalNavController provides navController,
    ) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = start,
        ) {
            composable(
                route = Destinations.HOME_ROUTE,
                deepLinks =
                    listOf(
                        navDeepLink { uriPattern = "${Destinations.APP_URI}/${Destinations.HOME_ROUTE}" },
                    ),
            ) {
                MainLayout()
            }
            composable(
                route = Destinations.PLAY_ROUTE,
                deepLinks =
                    listOf(
                        navDeepLink { uriPattern = "${Destinations.APP_URI}/${Destinations.PLAY_ROUTE}" },
                    ),
                enterTransition = {
                    fadeIn(
                        animationSpec =
                            tween(
                                300,
                                easing = LinearEasing,
                            ),
                    ) +
                        slideIntoContainer(
                            animationSpec = tween(300, easing = EaseIn),
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec =
                            tween(
                                300,
                                easing = LinearEasing,
                            ),
                    ) +
                        slideOutOfContainer(
                            animationSpec = tween(300, easing = EaseOut),
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                        )
                },
            ) {
                HomeLayout()
            }
        }
    }
}

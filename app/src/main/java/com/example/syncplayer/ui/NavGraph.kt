package com.example.syncplayer.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.syncplayer.Destinations
import com.example.syncplayer.LocalNavViewModel
import com.example.syncplayer.ui.dialog.Dialog
import com.example.syncplayer.util.debug
import com.example.syncplayer.viewModel.NavViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    start: String = Destinations.HOME_ROUTE,
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
            PlayLayout()
        }
    }

    val navViewModel = LocalNavViewModel.current
    LaunchedEffect(navViewModel) {
        navViewModel.navigationEvent.collect { event ->
            debug("nav event: $event")
            when (event) {
                is NavViewModel.NavigationEvent.NavigationPlay -> {
                    navController.navigate(Destinations.PLAY_ROUTE)
                }
            }
        }
    }

    Dialog()
}

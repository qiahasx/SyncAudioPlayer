package com.example.syncplayer.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.syncplayer.Destinations
import com.example.syncplayer.LocalNavController

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    start: String = Destinations.HOME_ROUTE,
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
                PlayLayout()
            }
        }
    }
}

package com.magictap.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.magictap.ui.main.MainScreen
import com.magictap.ui.pc.PcEditScreen
import com.magictap.ui.profile.ProfileEditScreen
import com.magictap.ui.settings.SettingsScreen

@Composable
fun MagicTapApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn(tween(280))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeOut(tween(280))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeIn(tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeOut(tween(280))
        },
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onAddProfile = { navController.navigate(Routes.profileNew()) },
                onEditProfile = { id -> navController.navigate(Routes.profileEdit(id)) },
                onAddPc = { profileId -> navController.navigate(Routes.pcNew(profileId)) },
                onEditPc = { profileId, pcId -> navController.navigate(Routes.pcEdit(profileId, pcId)) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.PROFILE_EDIT_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_PROFILE_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val profileId = entry.arguments?.getString(Routes.ARG_PROFILE_ID)?.ifBlank { null }
            ProfileEditScreen(profileId = profileId, onDone = { navController.popBackStack() })
        }

        composable(
            route = Routes.PC_EDIT_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_PROFILE_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_PC_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val profileId = entry.arguments?.getString(Routes.ARG_PROFILE_ID).orEmpty()
            val pcId = entry.arguments?.getString(Routes.ARG_PC_ID)?.ifBlank { null }
            PcEditScreen(
                profileId = profileId,
                pcId = pcId,
                onDone = { navController.popBackStack() },
            )
        }
    }
}

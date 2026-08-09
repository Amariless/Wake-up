package com.fritangui.wakeup.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.fritangui.wakeup.ui.blocking.BlockingScreen
import com.fritangui.wakeup.ui.clock.ClockScreen
import com.fritangui.wakeup.ui.clock.alarms.AlarmEditorScreen
import com.fritangui.wakeup.ui.devtools.DevToolsScreen
import com.fritangui.wakeup.ui.folders.FolderDetailScreen
import com.fritangui.wakeup.ui.folders.FoldersScreen
import com.fritangui.wakeup.ui.home.HomeScreen
import com.fritangui.wakeup.ui.navigation.Routes
import com.fritangui.wakeup.ui.onboarding.XiaomiOnboardingScreen
import com.fritangui.wakeup.ui.screentime.ScreenTimeScreen
import com.fritangui.wakeup.ui.settings.SettingsScreen
import com.fritangui.wakeup.ui.subjects.SubjectEditorScreen
import com.fritangui.wakeup.ui.tasks.TaskEditorScreen
import com.fritangui.wakeup.ui.update.UpdateScreen

/**
 * [navRoute] es a dónde navegar al tocar el tab (puede ser una ruta concreta,
 * p.ej. "folder/7"). [matchPattern] es el patrón contra el que se compara el
 * destino actual para saber si el tab debe verse seleccionado — tienen que
 * ir separados porque [androidx.navigation.NavDestination.route] siempre
 * expone el patrón ("folder/{folderId}"), nunca la ruta ya resuelta.
 */
private data class BottomDestination(
    val navRoute: String,
    val matchPattern: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun WakeUpNavHost(mainNavViewModel: MainNavViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val pinnedFolder by mainNavViewModel.pinnedFolder.collectAsState()

    val bottomDestinations = listOf(
        BottomDestination(Routes.HOME, Routes.HOME, "Inicio", Icons.Default.Home),
        if (pinnedFolder != null) {
            BottomDestination(Routes.folderDetail(pinnedFolder!!.id), Routes.FOLDER_DETAIL, pinnedFolder!!.name, Icons.Default.Folder)
        } else {
            BottomDestination(Routes.FOLDERS, Routes.FOLDERS, "Carpetas", Icons.Default.Folder)
        },
        BottomDestination(Routes.CLOCK, Routes.CLOCK, "Reloj", Icons.Default.Alarm),
        BottomDestination(Routes.SCREEN_TIME, Routes.SCREEN_TIME, "Bienestar", Icons.Default.AccessTime),
        BottomDestination(Routes.SETTINGS, Routes.SETTINGS, "Ajustes", Icons.Default.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.matchPattern } == true,
                        onClick = {
                            // A propósito SIN saveState/restoreState: varias pantallas (Bloqueo, Xiaomi,
                            // Dev tools) son alcanzables desde más de un tab, y combinar eso con guardar/
                            // restaurar el estado de cada tab hacía que la navegación se "mezclara" entre
                            // Bienestar y Ajustes después de un rato. Cada tap en un tab navega limpio.
                            navController.navigate(dest.navRoute) {
                                popUpTo(Routes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { defaultEnterTransition() },
            exitTransition = { defaultExitTransition() },
            popEnterTransition = { defaultPopEnterTransition() },
            popExitTransition = { defaultPopExitTransition() },
        ) {
            composable(Routes.HOME) { HomeScreen() }

            composable(Routes.FOLDERS) {
                FoldersScreen(onOpenFolder = { navController.navigate(Routes.folderDetail(it)) })
            }

            composable(
                Routes.FOLDER_DETAIL,
                arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
            ) {
                FolderDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSubject = { folderId, subjectId -> navController.navigate(Routes.subjectEditor(folderId, subjectId)) },
                    onOpenTask = { folderId, taskId -> navController.navigate(Routes.taskEditor(folderId, taskId)) },
                    onOpenAlarm = { folderId, alarmId -> navController.navigate(Routes.alarmEditor(folderId, alarmId)) },
                )
            }

            composable(
                Routes.SUBJECT_EDITOR,
                arguments = listOf(
                    navArgument("folderId") { type = NavType.LongType },
                    navArgument("subjectId") { type = NavType.LongType },
                ),
            ) {
                SubjectEditorScreen(onBack = { navController.popBackStack() })
            }

            composable(
                Routes.TASK_EDITOR,
                arguments = listOf(
                    navArgument("folderId") { type = NavType.LongType },
                    navArgument("taskId") { type = NavType.LongType },
                ),
            ) {
                TaskEditorScreen(onBack = { navController.popBackStack() })
            }

            composable(
                Routes.ALARM_EDITOR,
                arguments = listOf(
                    navArgument("folderId") { type = NavType.LongType },
                    navArgument("alarmId") { type = NavType.LongType },
                ),
            ) {
                AlarmEditorScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CLOCK) {
                ClockScreen(
                    onOpenAlarm = { navController.navigate(Routes.alarmEditor(alarmId = it)) },
                    onNewAlarm = { navController.navigate(Routes.alarmEditor()) },
                )
            }

            composable(Routes.SCREEN_TIME) {
                ScreenTimeScreen(onOpenBlocking = { navController.navigate(Routes.BLOCKING) })
            }

            composable(Routes.BLOCKING) { BlockingScreen() }

            composable(Routes.XIAOMI_ONBOARDING) {
                XiaomiOnboardingScreen(onDone = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenXiaomiWizard = { navController.navigate(Routes.XIAOMI_ONBOARDING) },
                    onOpenScreenTime = { navController.navigate(Routes.SCREEN_TIME) },
                    onOpenBlocking = { navController.navigate(Routes.BLOCKING) },
                    onOpenDevTools = { navController.navigate(Routes.DEV_TOOLS) },
                    onOpenUpdate = { navController.navigate(Routes.UPDATE) },
                )
            }

            composable(Routes.DEV_TOOLS) { DevToolsScreen() }

            composable(Routes.UPDATE) { UpdateScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

private const val NAV_ANIM_MS = 260

private fun AnimatedContentTransitionScope<*>.defaultEnterTransition() =
    slideInHorizontally(animationSpec = tween(NAV_ANIM_MS), initialOffsetX = { it / 4 }) + fadeIn(tween(NAV_ANIM_MS))

private fun AnimatedContentTransitionScope<*>.defaultExitTransition() =
    slideOutHorizontally(animationSpec = tween(NAV_ANIM_MS), targetOffsetX = { -it / 4 }) + fadeOut(tween(NAV_ANIM_MS))

private fun AnimatedContentTransitionScope<*>.defaultPopEnterTransition() =
    slideInHorizontally(animationSpec = tween(NAV_ANIM_MS), initialOffsetX = { -it / 4 }) + fadeIn(tween(NAV_ANIM_MS))

private fun AnimatedContentTransitionScope<*>.defaultPopExitTransition() =
    slideOutHorizontally(animationSpec = tween(NAV_ANIM_MS), targetOffsetX = { it / 4 }) + fadeOut(tween(NAV_ANIM_MS))

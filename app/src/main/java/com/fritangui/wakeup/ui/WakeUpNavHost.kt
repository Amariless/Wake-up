package com.fritangui.wakeup.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_DESTINATIONS = listOf(
    BottomDestination(Routes.HOME, "Inicio", Icons.Default.Home),
    BottomDestination(Routes.FOLDERS, "Carpetas", Icons.Default.Folder),
    BottomDestination(Routes.CLOCK, "Reloj", Icons.Default.Alarm),
    BottomDestination(Routes.SCREEN_TIME, "Bienestar", Icons.Default.AccessTime),
    BottomDestination(Routes.SETTINGS, "Ajustes", Icons.Default.Settings),
)

@Composable
fun WakeUpNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                BOTTOM_DESTINATIONS.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
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
                )
            }

            composable(Routes.DEV_TOOLS) { DevToolsScreen() }
        }
    }
}

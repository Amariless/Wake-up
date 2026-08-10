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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
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
import com.fritangui.wakeup.ui.subjects.SessionEditorScreen
import com.fritangui.wakeup.ui.subjects.SubjectEditorScreen
import com.fritangui.wakeup.ui.tasks.TaskEditorScreen
import com.fritangui.wakeup.ui.update.UpdateScreen
import com.fritangui.wakeup.widget.WidgetDeepLink

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
    /** true si esta es la carpeta marcada como principal (necesita apilar la lista debajo, ver #43). */
    val isPinnedFolder: Boolean = false,
)

/** Los 4 destinos de la barra inferior; ninguna otra pantalla (detalle, editores) debería considerarse "de tab". */
private val TOP_LEVEL_ROUTES = setOf(Routes.HOME, Routes.FOLDERS, Routes.CLOCK, Routes.SCREEN_TIME)

@Composable
fun WakeUpNavHost(
    pendingDeepLink: MutableState<WidgetDeepLink?> = remember { mutableStateOf(null) },
    mainNavViewModel: MainNavViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val pinnedFolder by mainNavViewModel.pinnedFolder.collectAsState()

    // Al tocar una clase/tarea en un widget de home screen: arma la misma pila de navegación que
    // tendría si hubieras llegado ahí tocando dentro de la app (Inicio → Carpetas → esa carpeta →
    // la materia/tarea), para que "atrás" se sienta natural en vez de solo cerrar la app.
    LaunchedEffect(pendingDeepLink.value) {
        val target = pendingDeepLink.value ?: return@LaunchedEffect
        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
        navController.navigate(Routes.FOLDERS)
        when (target) {
            is WidgetDeepLink.Subject -> {
                navController.navigate(Routes.folderDetail(target.folderId))
                navController.navigate(Routes.subjectEditor(target.folderId, target.subjectId))
            }
            is WidgetDeepLink.Task -> {
                navController.navigate(Routes.folderDetail(target.folderId))
                navController.navigate(Routes.taskEditor(target.folderId, target.taskId))
            }
        }
        pendingDeepLink.value = null
    }

    val bottomDestinations = listOf(
        BottomDestination(Routes.HOME, Routes.HOME, "Inicio", Icons.Default.Home),
        if (pinnedFolder != null) {
            BottomDestination(
                Routes.folderDetail(pinnedFolder!!.id),
                Routes.FOLDER_DETAIL,
                pinnedFolder!!.name,
                Icons.Default.Folder,
                isPinnedFolder = true,
            )
        } else {
            BottomDestination(Routes.FOLDERS, Routes.FOLDERS, "Carpetas", Icons.Default.Folder)
        },
        BottomDestination(Routes.CLOCK, Routes.CLOCK, "Reloj", Icons.Default.Alarm),
        BottomDestination(Routes.SCREEN_TIME, Routes.SCREEN_TIME, "Bienestar", Icons.Default.AccessTime),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { dest ->
                    // Para la carpeta fija no basta con comparar el patrón de ruta: cualquier
                    // detalle de carpeta matchea "folder/{folderId}", así que hay que comparar
                    // también el folderId real para no confundir "ya estoy en la carpeta fija"
                    // con "estoy viendo otra carpeta distinta" (eso rompería el guard de abajo).
                    val isSelected = if (dest.isPinnedFolder) {
                        currentDestination?.route == Routes.FOLDER_DETAIL &&
                            backStackEntry?.arguments?.getLong("folderId") == pinnedFolder?.id
                    } else {
                        currentDestination?.hierarchy?.any { it.route == dest.matchPattern } == true
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            // Si ya estás en ese tab, no hay nada que navegar: evita relanzar la
                            // pantalla (recomposición completa + reconsulta a la BD) y la animación
                            // de transición cada vez que se vuelve a tocar el mismo ícono.
                            if (isSelected) return@NavigationBarItem
                            // saveState/restoreState: cada tab conserva su estado (scroll, lo que ya
                            // cargó de la BD) al volver a él en vez de reconstruirse desde cero cada
                            // vez — antes esto se evitaba porque Ajustes también era un tab y comparte
                            // pantallas (Bloqueo, Xiaomi, Dev tools) con Bienestar, lo que mezclaba sus
                            // estados guardados; ahora que Ajustes ya no es un tab (ver #42) cada uno
                            // de los 4 tabs tiene su propia rama independiente y esto ya no pasa.
                            if (dest.isPinnedFolder) {
                                // Apila primero la lista de carpetas para que "atrás" desde la carpeta
                                // principal caiga en la lista completa, no directo a Inicio.
                                navController.navigate(Routes.FOLDERS) {
                                    popUpTo(Routes.HOME) { inclusive = false; saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                navController.navigate(dest.navRoute) { launchSingleTop = true }
                            } else {
                                navController.navigate(dest.navRoute) {
                                    popUpTo(Routes.HOME) { inclusive = false; saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { pickEnterTransition() },
            exitTransition = { pickExitTransition() },
            popEnterTransition = { defaultPopEnterTransition() },
            popExitTransition = { defaultPopExitTransition() },
        ) {
            composable(Routes.HOME) { HomeScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) }) }

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
                SubjectEditorScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSession = { folderId, subjectId, sessionId ->
                        navController.navigate(Routes.sessionEditor(folderId, subjectId, sessionId))
                    },
                    onOpenTask = { folderId, taskId -> navController.navigate(Routes.taskEditor(folderId, taskId)) },
                )
            }

            composable(
                Routes.SESSION_EDITOR,
                arguments = listOf(
                    navArgument("folderId") { type = NavType.LongType },
                    navArgument("subjectId") { type = NavType.LongType },
                    navArgument("sessionId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                SessionEditorScreen(backStackEntry = backStackEntry, onBack = { navController.popBackStack() })
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

            composable(Routes.BLOCKING) { BlockingScreen(onBack = { navController.popBackStack() }) }

            composable(Routes.XIAOMI_ONBOARDING) {
                XiaomiOnboardingScreen(onDone = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
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
private const val TAB_FADE_MS = 180

/** Sin slide direccional entre tabs del bottom nav: no hay un "adelante/atrás" real entre ellos,
 *  así que cualquier dirección fija se sentía "incorrecta" la mitad de las veces. Un fade simple
 *  no tiene ese problema. El slide se reserva para cuando sí hay una jerarquía real (entrar/salir
 *  de una pantalla de detalle), donde enter/exit vs pop-enter/pop-exit ya representan bien el sentido. */
private fun isTopLevelSwitch(scope: AnimatedContentTransitionScope<NavBackStackEntry>): Boolean {
    val from = scope.initialState.destination.route
    val to = scope.targetState.destination.route
    return from in TOP_LEVEL_ROUTES && to in TOP_LEVEL_ROUTES
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pickEnterTransition() =
    if (isTopLevelSwitch(this)) {
        fadeIn(tween(TAB_FADE_MS))
    } else {
        slideInHorizontally(animationSpec = tween(NAV_ANIM_MS), initialOffsetX = { it / 4 }) + fadeIn(tween(NAV_ANIM_MS))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pickExitTransition() =
    if (isTopLevelSwitch(this)) {
        fadeOut(tween(TAB_FADE_MS))
    } else {
        slideOutHorizontally(animationSpec = tween(NAV_ANIM_MS), targetOffsetX = { -it / 4 }) + fadeOut(tween(NAV_ANIM_MS))
    }

private fun AnimatedContentTransitionScope<*>.defaultPopEnterTransition() =
    slideInHorizontally(animationSpec = tween(NAV_ANIM_MS), initialOffsetX = { -it / 4 }) + fadeIn(tween(NAV_ANIM_MS))

private fun AnimatedContentTransitionScope<*>.defaultPopExitTransition() =
    slideOutHorizontally(animationSpec = tween(NAV_ANIM_MS), targetOffsetX = { it / 4 }) + fadeOut(tween(NAV_ANIM_MS))

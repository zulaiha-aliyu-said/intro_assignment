package com.capcutmini.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.capcutmini.app.CapCutMiniApp
import com.capcutmini.app.R
import com.capcutmini.app.ui.aistudio.AiStudioScreen
import com.capcutmini.app.ui.editor.EditorScreen
import com.capcutmini.app.ui.editor.EditorViewModel
import com.capcutmini.app.ui.home.HomeScreen
import com.capcutmini.app.ui.projects.ProjectsScreen
import com.capcutmini.app.ui.projects.ProjectsViewModel

private data class BottomNavItem(
    val destination: AppDestination,
    val labelRes: Int,
    val icon: ImageVector
)

@Composable
fun CapCutMiniNavHost(
    application: CapCutMiniApp
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem(AppDestination.Home, R.string.nav_home, Icons.Outlined.Home),
        BottomNavItem(AppDestination.Projects, R.string.nav_projects, Icons.Outlined.FolderOpen),
        BottomNavItem(AppDestination.Editor, R.string.nav_editor, Icons.Outlined.Movie),
        BottomNavItem(AppDestination.AiStudio, R.string.nav_ai_studio, Icons.Outlined.AutoAwesome)
    )

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.destination.route,
                        onClick = { navigateTo(item.destination.route) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onOpenEditor = { navigateTo(AppDestination.Editor.route) },
                    onOpenProjects = { navigateTo(AppDestination.Projects.route) },
                    onOpenAiStudio = { navigateTo(AppDestination.AiStudio.route) }
                )
            }
            composable(AppDestination.Projects.route) {
                val projectsViewModel: ProjectsViewModel = viewModel(
                    factory = ProjectsViewModel.Factory(application.container.projectRepository)
                )
                ProjectsScreen(
                    viewModel = projectsViewModel,
                    onOpenEditor = { navigateTo(AppDestination.Editor.route) }
                )
            }
            composable(AppDestination.Editor.route) {
                val editorViewModel: EditorViewModel = viewModel(
                    factory = EditorViewModel.Factory(
                        application = application,
                        repository = application.container.projectRepository
                    )
                )
                EditorScreen(viewModel = editorViewModel)
            }
            composable(AppDestination.AiStudio.route) {
                AiStudioScreen()
            }
        }
    }
}

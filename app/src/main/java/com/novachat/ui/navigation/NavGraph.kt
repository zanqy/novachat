package com.novachat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novachat.AppContainer
import com.novachat.ui.chat.ChatScreen
import com.novachat.ui.chatlist.ChatListScreen
import com.novachat.ui.settings.SettingsScreen

object Routes {
    const val LIST = "chatlist"
    const val SETTINGS = "settings"
    const val CHAT = "chat/{chatId}"
    fun chat(chatId: String) = "chat/$chatId"
}

@Composable
fun NavGraph(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            ChatListScreen(container = container, navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(container = container, navController = navController)
        }
        composable(
            Routes.CHAT,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { entry ->
            val chatId = entry.arguments?.getString("chatId").orEmpty()
            ChatScreen(
                container = container,
                navController = navController,
                chatId = chatId
            )
        }
    }
}

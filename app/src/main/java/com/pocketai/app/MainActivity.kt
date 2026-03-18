package com.pocketai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketai.app.ui.screens.ChatScreen
import com.pocketai.app.ui.screens.HuggingFaceAuthScreen
import com.pocketai.app.ui.screens.HomeScreen
import com.pocketai.app.ui.screens.ModelBrowserScreen
import com.pocketai.app.ui.screens.SettingsScreen
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToModels = { navController.navigate("models") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("models") {
            ModelBrowserScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAuth = { navController.navigate("auth") },
                onModelDownloaded = { filePath, modelName ->
                    val app = navController.context.applicationContext as PocketAIApp
                    runBlocking {
                        app.settingsManager.saveLastModel(filePath, modelName)
                    }
                    app.inferenceEngine.loadModel(filePath, modelName)
                }
            )
        }

        composable("auth") {
            HuggingFaceAuthScreen(
                onNavigateBack = { navController.popBackStack() },
                onTokenExtracted = { token ->
                    val app = navController.context.applicationContext as PocketAIApp
                    runBlocking {
                        app.settingsManager.saveHfAuthToken(token)
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

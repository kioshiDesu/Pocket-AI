package com.pocketai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketai.app.ui.screens.ChatScreen
import com.pocketai.app.ui.screens.HuggingFaceAuthScreen
import com.pocketai.app.ui.screens.HomeScreen
import com.pocketai.app.ui.screens.ModelBrowserScreen
import com.pocketai.app.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

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
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as PocketAIApp
    val scope = rememberCoroutineScope()

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
                    scope.launch {
                        app.inferenceEngine.loadModel(filePath, modelName)
                    }
                    navController.navigate("chat") {
                        popUpTo("home")
                    }
                }
            )
        }

        composable("auth") {
            HuggingFaceAuthScreen(
                onNavigateBack = { navController.popBackStack() },
                onTokenExtracted = { token ->
                    app.settingsManager.saveHfAuthToken(token)
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

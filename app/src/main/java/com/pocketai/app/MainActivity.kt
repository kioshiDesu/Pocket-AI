package com.pocketai.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
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
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle incoming intent for model files
        handleIncomingIntent(intent)
        
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
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }
    
    private fun handleIncomingIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type
        
        if (Intent.ACTION_VIEW == action && type != null) {
            val uri = intent.data
            if (uri != null) {
                // Copy the file to app's model directory and load it
                copyAndLoadModel(uri)
            }
        }
    }
    
    private fun copyAndLoadModel(uri: Uri) {
        val app = application as PocketAIApp
        
        try {
            // Get file name from URI
            val fileName = getFileName(uri) ?: "imported_model.task"
            
            // Copy file to models directory
            val modelsDir = File(getExternalFilesDir(null), "models")
            if (!modelsDir.exists()) modelsDir.mkdirs()
            
            val outputFile = File(modelsDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Load the model
            val scope = rememberCoroutineScope()
            scope.launch {
                app.inferenceEngine.loadModel(outputFile.absolutePath, fileName)
            }
            
            Toast.makeText(this, "Model imported successfully: $fileName", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to import model: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && it.moveToFirst()) {
                        return it.getString(nameIndex)
                    }
                }
                null
            }
            "file" -> {
                uri.lastPathSegment
            }
            else -> null
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

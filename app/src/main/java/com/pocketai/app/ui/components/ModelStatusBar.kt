package com.pocketai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pocketai.app.domain.model.ModelState

@Composable
fun ModelStatusBar(
    modelState: ModelState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, text, icon) = when (modelState) {
        is ModelState.NotLoaded -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "No model loaded",
            Icons.Default.Memory
        )
        is ModelState.Loading -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            "Loading model...",
            null
        )
        is ModelState.Loaded -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            "Model: ${modelState.modelName}",
            Icons.Default.CheckCircle
        )
        is ModelState.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Error: ${modelState.message}",
            Icons.Default.Error
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        when {
            icon == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
            modelState is ModelState.Error -> {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            modelState is ModelState.Loaded -> {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = when (modelState) {
                is ModelState.Error -> MaterialTheme.colorScheme.onErrorContainer
                is ModelState.Loaded -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f)
        )
    }
}

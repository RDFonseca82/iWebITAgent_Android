package com.rdfonseca.iwebit.presentation.definitions.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.rdfonseca.iwebit.presentation.ui.theme.Yellow200
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme

@Composable
fun WarningListItem(
    icon: Painter,
    text: String,
    trailingAction: () -> Unit = {}
) {
    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { trailingAction() },
        headlineContent = { Text(text = text) },
        leadingContent = {
            Icon(painter = icon, contentDescription = "Icon of list item")
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Resolve problem"
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isDarkTheme())
                Color.Yellow.copy(alpha = 0.3f)
            else
                Yellow200.copy(alpha = 0.3f)
        )
    )
}
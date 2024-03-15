package com.rdfonseca.iwebit.presentation.definitions.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.rdfonseca.iwebit.presentation.ui.theme.Yellow200
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WarningListItem(
    icon: Painter,
    text: String,
    trailingAction: () -> Unit = {}
) {
    Surface(
        color = if (isDarkTheme())
            Color.Yellow.copy(alpha = 0.3f)
        else
            Yellow200.copy(alpha = 0.3f),
        shape = RoundedCornerShape(6.dp)
    ) {
        ListItem(
            modifier = Modifier.clickable { trailingAction() },
            icon = {
                   Icon(painter = icon, contentDescription = "Icon of list item")
            },
            text = {
                Text(text = text)
            },
            trailing = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Resolve problem"
                )
            }
        )
    }
}
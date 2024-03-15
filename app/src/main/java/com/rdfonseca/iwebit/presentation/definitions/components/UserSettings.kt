package com.rdfonseca.iwebit.presentation.definitions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.KeyboardArrowDown
import androidx.compose.material3.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdfonseca.iwebit.domain.model.DarkThemeMode
import com.rdfonseca.iwebit.presentation.LocalUserPreferences

@Composable
fun UserSettings(
    onThemeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(10.dp)
    ) {
        ChangeThemeItem(onThemeChange = onThemeChange)
    }
}


@Composable
fun ChangeThemeItem(
    onThemeChange: (String) -> Unit
) {
    val dropMenuState = rememberDropDownMenuState(
        items = listOf(
            "Claro", "Escuro", "Sistema"
        )
    )

    Row(
        modifier = Modifier.padding(10.dp, 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tema:",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.weight(1f))

        Box {
            TextButton(onClick = { dropMenuState.isExpanded = true }){
                Text(
                    text = when (LocalUserPreferences.current.theme.darkThemeMode) {
                        DarkThemeMode.LIGHT -> "Claro"
                        DarkThemeMode.DARK -> "Escuro"
                        DarkThemeMode.SYSTEM -> "Sistema"
                    },
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = dropMenuState.icon,
                    contentDescription = "Expand to see choices"
                )
            }
            DropdownMenu(
                expanded = dropMenuState.isExpanded,
                onDismissRequest = {
                    dropMenuState.isExpanded = false
                }
            ) {
                dropMenuState.items.forEach { text ->
                    DropdownMenuItem(
                        onClick = {
                            dropMenuState.isExpanded = false
                            onThemeChange(text)
                        }
                    ) {
                        androidx.compose.material3.Text(text = text)
                    }
                }
            }
        }

    }
}

class DropDownMenuState(
    val items: List<String>
) {

    var isExpanded by mutableStateOf(false)
    val icon: ImageVector
        @Composable get() = if (isExpanded) {
            Icons.Default.KeyboardArrowUp
        } else {
            Icons.Default.KeyboardArrowDown
        }
}


@Composable
fun rememberDropDownMenuState(items: List<String>) = remember {
    DropDownMenuState(items)
}

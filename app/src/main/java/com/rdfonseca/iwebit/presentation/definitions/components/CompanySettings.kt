package com.rdfonseca.iwebit.presentation.definitions.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdfonseca.iwebit.presentation.LocalUserPreferences


@Composable
fun CompanySettings() {
    val prefs = LocalUserPreferences.current
    Column(
        modifier = Modifier.padding(10.dp)
    ) {
        KeyValueRow(title = "ID do Dispositivo", text = prefs.uniqueId)
        KeyValueRow(title = "ID de Sincronização", text = prefs.idSync)
        KeyValueRow(title = "ID da Empresa", text = prefs.idCompany)
        KeyValueRow(title = "Nome da Empresa", text = prefs.companyName)
    }

}

@Composable
fun KeyValueRow(
    title: String,
    text: String
) {
    SelectionContainer {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$title:",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                text = text,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

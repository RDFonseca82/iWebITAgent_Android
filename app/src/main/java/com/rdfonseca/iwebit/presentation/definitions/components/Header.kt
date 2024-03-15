package com.rdfonseca.iwebit.presentation.definitions.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Header(
    title: String
) {
    Text(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        text = title,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}
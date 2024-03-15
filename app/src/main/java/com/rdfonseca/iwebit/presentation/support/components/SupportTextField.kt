package com.rdfonseca.iwebit.presentation.support.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdfonseca.iwebit.presentation.components.CustomTextField
import com.rdfonseca.iwebit.presentation.ui.theme.*

@Composable
fun SupportTextField(
    textValue: String,
    onTextChange: (String) -> Unit,
    singleLine: Boolean,
    placeholder: String = "",
    height: Dp = 50.dp,
    imeAction: ImeAction = ImeAction.Default,
    maxLength: Int? = null
) {
    val bgFocused = if (isDarkTheme()) Gray500 else Gray200
    val bgUnfocused = if (isDarkTheme()) Gray700 else Gray400
    var focused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(targetValue = if (focused) bgFocused else bgUnfocused)

    Column {
        CustomTextField(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(height)
                .onFocusChanged {
                    focused = it.isFocused
                },
            textValue = textValue,
            onTextChange = {
                if (maxLength == null || it.length <= maxLength) {
                    onTextChange(it)
                } else {
                    onTextChange(it.slice(0 until maxLength))
                }
            },
            bgColor = bgColor,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = if (singleLine) 0.dp else 10.dp),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
                ) {
                    if (textValue.isEmpty()) Text(
                        text = placeholder,
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    )
                    innerTextField()
                }
            },
            singleLine = singleLine,
            imeAction = imeAction
        )
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.End
        ) {
            if (focused && maxLength != null) {
                Text(
                    text = "${textValue.length}/$maxLength",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            }
        }
    }
}


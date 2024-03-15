package com.rdfonseca.iwebit.presentation.recents.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdfonseca.iwebit.presentation.components.CustomTextField
import com.rdfonseca.iwebit.presentation.ui.theme.*

@Composable
fun SearchTextField(
    modifier: Modifier,
    textValue: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    keyboardActions: KeyboardActions
) {
    val bgFocused = if (isDarkTheme()) Gray500 else Gray200
    val bgUnfocused = if (isDarkTheme()) Gray700 else Gray400
    var focused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(targetValue = if (focused) bgFocused else bgUnfocused)

    Row(
        modifier = Modifier
            .clickable(
            interactionSource =  remember { MutableInteractionSource() },
            indication = null,
            onClick = {}
        )
            .background(MaterialTheme.colorScheme.background)
            .padding(10.dp)
            .fillMaxWidth()
            .height(50.dp)
            .background(bgColor, MaterialTheme.shapes.small)
            ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomTextField(
            modifier = modifier
                .onFocusChanged {
                    focused = it.isFocused
                }
                .weight(1f),
            textValue = textValue,
            elevation = 0.dp,
            onTextChange = {
                onTextChange(it.replace("\n", ""))
            },
            bgColor = Color.Transparent,
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textValue.isEmpty()) Text(
                        text = "Pesquise um pedido de suporte...",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    )
                    innerTextField()
                }
            },
            singleLine = true,
            imeAction = ImeAction.Search,
            keyboardActions = keyboardActions
        )
        Icon(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable { onClose() },
            imageVector = Icons.Default.Close,
            contentDescription = "Close search field",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(10.dp))
    }
}


//private fun getListAnnotatedString(
//    text: String,
//    search: String,
//    highlightedColor: Color,
//): AnnotatedString {
//    val startIndex = text.indexOf(search, ignoreCase = true)
//    val endIndex = startIndex + search.length
//    return with(AnnotatedString.Builder()) {
//        append(text.substring(0 until startIndex))
//        withStyle(SpanStyle(color = highlightedColor)) {
//            append(text.substring(startIndex until endIndex))
//        }
//        append(text.substring(endIndex until text.length))
//        toAnnotatedString()
//    }
//}
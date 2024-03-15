package com.rdfonseca.iwebit.presentation.recents.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdfonseca.iwebit.presentation.ui.theme.*
import com.rdfonseca.iwebit.presentation.utils.isScrolled
import com.rdfonseca.iwebit.presentation.utils.lerp

const val LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit," +
        " sed do eiusmod tempor incididunt ut labore et dolore magna " +
        "aliqua. Ut enim ad minim veniam, quis nostrud exercitation u" +
        "llamco laboris nisi ut aliquip ex ea commodo consequat. Duis" +
        " aute irure dolor in reprehenderit in voluptate velit esse c" +
        "illum dolore eu fugiat nulla pariatur. Excepteur sint occaec" +
        "at cupidatat non proident, sunt in culpa qui officia deserun" +
        "t mollit anim id est laborum."

@Composable
fun MessageCard(
    modifier: Modifier = Modifier,
    header: AnnotatedString,
    message: AnnotatedString,
    date: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .background(if (isDarkTheme()) Blue900 else Blue50, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(10.dp),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = header,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 18.sp,
                color = if (isDarkTheme()) Blue200 else Blue700
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = date,
            modifier = Modifier.padding(5.dp, 10.dp,10.dp,0.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}


@Composable
fun MessageCardAnimatable(
    offsetY: Float,
    openFraction: Float,
    header: AnnotatedString,
    message: AnnotatedString,
    deltaDate: String,
    date: String,
    cardHeight: Int,
    onBackClick: () -> Unit
) {
    val density = LocalDensity.current
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() } + 1f
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val widthF = lerp(0.95f, 1f, openFraction)
    val height = lerp(cardHeight.toFloat(), screenHeight, openFraction)
    val offsetYF = lerp(offsetY, 0f, openFraction)
    val color = lerp(
        if (isDarkTheme()) Blue900 else Blue50,
        MaterialTheme.colorScheme.background,
        openFraction
    )
    val cornerRadius = lerp(8f, 0f,  openFraction)

    Box(
        modifier = Modifier
            .fillMaxWidth(widthF)
            .height(with(density) { height.toDp() })
            .offset(
                x = screenWidth * (1 - widthF) / 2,
                y = with(density) { offsetYF.toDp() }
            )
            .background(color, RoundedCornerShape(cornerRadius.dp))
    ) {
        val messageAlpha = lerp(1f, 0f, 0f, 0.5f, openFraction)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(messageAlpha)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
            ) {
                Text(
                    text = header,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    color = if (isDarkTheme()) Blue200 else Blue700
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = deltaDate,
                modifier = Modifier.padding(
                    (5 * (1 - openFraction)).dp,
                    (10 * (1 - openFraction)).dp,
                    (10 * (1 - openFraction)).dp,
                    0.dp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        val expandedAlpha = lerp(0f, 1f, 0.6f, 1f, openFraction)
        val lazyListState = rememberLazyListState()
        val topBarAlpha by animateFloatAsState(
            targetValue = if (lazyListState.isScrolled) 0.8f else 1f,
            animationSpec = tween(500)
        )

        Box(
            modifier = Modifier
                .alpha(expandedAlpha)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, top = 70.dp, end = 10.dp),
                state = lazyListState
            ) {
                item {
                    SelectionContainer {
                        Column {
                            Text(
                                text = header,
                                fontSize = 18.sp,
                                color = if (isDarkTheme()) Blue200 else Blue700
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = date,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = topBarAlpha))
                    .padding(10.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Dismiss detail screen"
                    )
                }
            }
        }
    }
}



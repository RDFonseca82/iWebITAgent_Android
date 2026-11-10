package com.rdfonseca.iwebit.presentation.recents

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.common.cancelAllNotifications
import com.rdfonseca.iwebit.common.setupFullSyncWorker
import com.rdfonseca.iwebit.common.setupMinSyncWorker
import com.rdfonseca.iwebit.domain.model.SupportMsg
import com.rdfonseca.iwebit.presentation.Screen
import com.rdfonseca.iwebit.presentation.components.DefaultCenteredTopBar
import com.rdfonseca.iwebit.presentation.components.DefaultScaffoldWithNavDrawer
import com.rdfonseca.iwebit.presentation.components.LottieLoader
import com.rdfonseca.iwebit.presentation.components.UserViewModel
import com.rdfonseca.iwebit.presentation.recents.components.MessageCard
import com.rdfonseca.iwebit.presentation.recents.components.MessageCardAnimatable
import com.rdfonseca.iwebit.presentation.recents.components.SearchTextField
import com.rdfonseca.iwebit.presentation.utils.formatDateToHumanReadable
import com.rdfonseca.iwebit.presentation.utils.isScrolled
import com.rdfonseca.iwebit.presentation.utils.lerp
import com.rdfonseca.iwebit.services.ScreenCaptureService
import com.rdfonseca.iwebit.services.SincLocationService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val viewModel: RecentsViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val lazyListState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    val searchValue = viewModel.searchText
    var searchFieldIsVisible by rememberSaveable { mutableStateOf(false) }
    val data = viewModel.state.let { if (searchFieldIsVisible) it.filteredMsgs else it.allMsgs }

    var cardExpanded by rememberSaveable { mutableStateOf(false) }
    var currentOpenedPopupID by rememberSaveable { mutableIntStateOf(-1) }
    val pullToRefresh = rememberPullToRefreshState(50.dp) { currentOpenedPopupID < 0 }
    var cardHeight by rememberSaveable { mutableIntStateOf(0) }
    val shouldRespondToUserInteraction = currentOpenedPopupID < 0 && !pullToRefresh.isRefreshing

    val toolbarHeight = 64.dp
    val toolbarHeightPx = with(density) { toolbarHeight.roundToPx().toFloat() }
    val toolbarOffsetHeightPx = rememberSaveable { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!pullToRefresh.isSwipeInProgress && !pullToRefresh.isRefreshing && currentOpenedPopupID < 0) {
                    val delta = available.y
                    val newOffset = toolbarOffsetHeightPx.floatValue + delta
                    toolbarOffsetHeightPx.floatValue = newOffset.coerceIn(-toolbarHeightPx, 0f)
                }
                return Offset.Zero
            }
        }
    }

    val fraction by animateFloatAsState(
        targetValue = if (cardExpanded) 1f else 0f,
        finishedListener = {
            if (it == 0f) currentOpenedPopupID = -1
            if (it == 1f && searchFieldIsVisible) focusManager.clearFocus()
        },
        animationSpec = tween(250),
        label = "fractionAimation"
    )

    LaunchedEffect(searchFieldIsVisible) {
        if (searchFieldIsVisible) focusRequester.requestFocus()
        toolbarOffsetHeightPx.floatValue = 0f
        lazyListState.scrollToItem(0)
    }

    LaunchedEffect(viewModel.state.error) {
        if (viewModel.state.error.isNotBlank()) snackbarHostState.showSnackbar(
            viewModel.state.error
        )
    }

    LaunchedEffect(true) {
        setupFullSyncWorker(context)
        setupMinSyncWorker(context)
    }

    BackHandler(currentOpenedPopupID >= 0) {
        cardExpanded = false
    }

    BackHandler(searchFieldIsVisible && currentOpenedPopupID < 0) {
        searchFieldIsVisible = false
        viewModel.onSearch("")
    }

    BackHandler(currentOpenedPopupID < 0 && !searchFieldIsVisible) {
        if (lazyListState.isScrolled)
            scope.launch { lazyListState.scrollToItem(0) }
        else
            (context as Activity).finish()
    }

    if (pullToRefresh.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshSupportedMessages()
        }
    }

    LaunchedEffect(viewModel.state.isLoading) {
        if (viewModel.state.isLoading) {
            pullToRefresh.startRefresh()
        } else {
            pullToRefresh.endRefresh()
        }
    }

    DefaultScaffoldWithNavDrawer(
        navController = navController,
        snackbarHostState = snackbarHostState,
        drawerState = drawerState,
        onLogOut = {
            userViewModel.logOut {
                cancelAllNotifications(context)
                context.startService(
                    Intent(
                        context,
                        ScreenCaptureService::class.java
                    )
                )// cancel any active projections
                context.startService(
                    Intent(
                        context,
                        SincLocationService::class.java
                    )
                )// cancel service location
                navController.popBackStack()
                navController.navigate(Screen.LogInScreen.route)
            }
        }
    ) { innerPaddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .nestedScroll(pullToRefresh.nestedScrollConnection)
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPaddingValues)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = toolbarHeight + 8.dp),
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                userScrollEnabled = shouldRespondToUserInteraction,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(data) { index, item ->
                    val modifierToCalculate = if (index == 0) {
                        Modifier.onGloballyPositioned {
                            cardHeight = it.size.height
                        }
                    } else {
                        Modifier
                    }

                    MessageCard(
                        modifier = modifierToCalculate,
                        header = formatToAnnotatedString(item.header, searchValue),
                        message = formatToAnnotatedString(item.message, searchValue),
                        date = formatDateToHumanReadable(item.date, sdf),
                        onClick = {
                            if (shouldRespondToUserInteraction) {
                                currentOpenedPopupID = index
                                cardExpanded = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }


            if (!searchFieldIsVisible) {
                DefaultCenteredTopBar(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = if (!lazyListState.isScrolled) 0 else toolbarOffsetHeightPx.floatValue.roundToInt()
                            )
                        },
                    onNavigationClick = {
                        if (shouldRespondToUserInteraction)
                            scope.launch {
                                drawerState.open()
                            }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (shouldRespondToUserInteraction) searchFieldIsVisible = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "hey",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            } else {
                SearchTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester),
                    textValue = searchValue,
                    onTextChange = viewModel::onSearch,
                    onClose = {
                        searchFieldIsVisible = false
                        viewModel.onSearch("")
                    },
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                        }
                    )
                )
            }

            AnimatedVisibility(
                visible = !searchFieldIsVisible,
                enter = fadeIn(keyframes {
                    0f at 300
                    durationMillis = 500
                }),
                exit = ExitTransition.None,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        .padding(20.dp)
                        .size(56.dp)
                        .offset(y = if (!lazyListState.isScrolled) 0.dp else (-80).dp * (0f + toolbarOffsetHeightPx.floatValue / toolbarHeightPx)),
                    onClick = { if (currentOpenedPopupID < 0) navController.navigate(Screen.SupportScreen.route) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_support_agent_24),
                        contentDescription = "hey",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (currentOpenedPopupID >= 0) {
                val months = listOf(
                    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
                )
                val itemOpenedOffset by remember {
                    derivedStateOf {
                        val fstVisibleItemIndex = lazyListState.firstVisibleItemIndex
                        val fstVisibleItemOffset = lazyListState.firstVisibleItemScrollOffset
                        with(density) {
                            (toolbarHeight + 8.dp +
                                    ((currentOpenedPopupID - fstVisibleItemIndex) * cardHeight).toDp() +
                                    ((currentOpenedPopupID - fstVisibleItemIndex) * 20).dp) - fstVisibleItemOffset.toDp()
                        }
                    }
                }
                val item = data.getOrElse(currentOpenedPopupID, defaultValue = { SupportMsg() })
                val formattedData = let {
                    val year = item.date.slice(0..3)
                    val month = item.date.slice(5..6)
                    val day = item.date.slice(8..9)
                    val hour = item.date.slice(11..12)
                    val minute = item.date.slice(14..15)

                    "$day de ${months.get(month.toInt() - 1)} de $year, $hour:$minute"
                }

                MessageCardAnimatable(
                    header = formatToAnnotatedString(item.header, searchValue),
                    message = formatToAnnotatedString(item.message, searchValue),
                    deltaDate = formatDateToHumanReadable(item.date, sdf),
                    date = formattedData,
                    offsetY = with(density) { itemOpenedOffset.toPx() },
                    openFraction = fraction,
                    cardHeight = cardHeight,
                    onBackClick = {
                        cardExpanded = false
                    }
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .wrapContentHeight()
                    .align(Alignment.Center)
                    .alpha(0.7f),
                visible = data.isEmpty() && !searchFieldIsVisible && !pullToRefresh.isRefreshing && viewModel.state.error.isEmpty(),
                enter = fadeIn(keyframes {
                    0f at 300
                    durationMillis = 400
                }),
                exit = ExitTransition.None,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LottieLoader(
                        modifier = Modifier.height(200.dp),
                        lottie = R.raw.empty_lottie
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        text = "Aparentemente, ainda não reportou ocorrências.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .wrapContentHeight()
                    .align(Alignment.Center)
                    .alpha(0.7f),
                visible = data.isEmpty() && !searchFieldIsVisible && !pullToRefresh.isRefreshing && viewModel.state.error.isNotEmpty(),
                enter = fadeIn(keyframes {
                    0f at 300
                    durationMillis = 400
                }),
                exit = ExitTransition.None,
            ) {
                val noConText =
                    "Sem conexão à internet. Por favor, verifique a sua conexão e tente novamente."
                val noConTextAnnotated = buildAnnotatedString {
                    append(noConText)
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        start = 61,
                        end = 77
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LottieLoader(
                        modifier = Modifier.size(200.dp),
                        lottie = R.raw.no_internet_connection
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                null,
                                true,
                                null,
                                null,
                                viewModel::refreshSupportedMessages
                            ),
                        text = noConTextAnnotated,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (pullToRefresh.isRefreshing)
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = toolbarHeight)
                )
            else if (pullToRefresh.isSwipeInProgress) {
                val indicatorRefreshTrigger = pullToRefresh.positionalThreshold
                val triggerDistanceFraction =
                    (pullToRefresh.verticalOffset / indicatorRefreshTrigger)
                        .coerceAtMost(1f)
                val height = lerp(0f, 1f, 0f, 0.2f, triggerDistanceFraction)
                val progress = lerp(0f, 1f, 0.2f, 1f, triggerDistanceFraction)

                LinearProgressIndicator(
                    progress = {
                        progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = toolbarHeight)
                        .scale(scaleX = 1f, scaleY = height),
                )
            }
        }
    }
}


fun formatToAnnotatedString(
    fullText: String,
    subString: String
) = buildAnnotatedString {
    append(fullText)

    if (subString.isNotBlank()) fullText.indexesOf(subString).forEach { index ->
        addStyle(
            style = SpanStyle(
                background = Color.Yellow
            ),
            start = index,
            end = index + subString.length
        )
    }
}


fun String.indexesOf(text: String): List<Int> =
    text.toRegex(setOf(RegexOption.IGNORE_CASE))
        .findAll(this)
        .map { it.range.first }
        .toList()

@OptIn(ExperimentalMaterial3Api::class)
val PullToRefreshState.isSwipeInProgress: Boolean
    get() = this.progress > 0


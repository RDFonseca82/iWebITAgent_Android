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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun RecentsScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val viewModel: RecentsViewModel = hiltViewModel()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val lazyListState = rememberLazyListState()
    val swipeState = rememberSwipeRefreshState(viewModel.state.isLoading)
    val density = LocalDensity.current
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    val searchValue = viewModel.searchText
    var searchFieldIsVisible by rememberSaveable { mutableStateOf(false) }
    val data = viewModel.state.let { if (searchFieldIsVisible) it.filteredMsgs else it.allMsgs}

    val toolbarHeight = 64.dp
    val toolbarHeightPx = with(density) { toolbarHeight.roundToPx().toFloat() }
    val toolbarOffsetHeightPx = rememberSaveable { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!swipeState.isSwipeInProgress && !swipeState.isRefreshing) {
                    val delta = available.y
                    val newOffset = toolbarOffsetHeightPx.value + delta
                    toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                }
                return Offset.Zero
            }
        }
    }

    var cardExpanded by rememberSaveable { mutableStateOf(false) }
    var currentOpenedPopupID by rememberSaveable { mutableStateOf(-1) }
    var cardHeight by rememberSaveable { mutableStateOf(0) }
    val shouldRespondToUserInteraction = currentOpenedPopupID < 0 || !swipeState.isRefreshing
    val fraction by animateFloatAsState(
        targetValue = if (cardExpanded) 1f else 0f,
        finishedListener = {
            if (it == 0f) currentOpenedPopupID = -1
            if (it == 1f && searchFieldIsVisible) focusManager.clearFocus()
        },
        animationSpec = tween(250)
    )

    LaunchedEffect(key1 = searchFieldIsVisible) {
        if (searchFieldIsVisible) focusRequester.requestFocus()
        toolbarOffsetHeightPx.value = 0f
        lazyListState.scrollToItem(0)
    }

    LaunchedEffect(key1 = viewModel.state.error) {
        if (viewModel.state.error.isNotBlank()) scaffoldState.snackbarHostState.showSnackbar(viewModel.state.error)
    }

    LaunchedEffect(key1 = true) {
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

    BackHandler(currentOpenedPopupID < 0 && !(searchFieldIsVisible && currentOpenedPopupID < 0)) {
        (context as Activity).finish()
    }

    DefaultScaffoldWithNavDrawer(
        navController = navController,
        scaffoldState = scaffoldState,
        onLogOut = { userViewModel.logOut {
            cancelAllNotifications(context)
            context.startService(Intent(context, ScreenCaptureService::class.java))// cancel any active projections
            context.startService(Intent(context, SincLocationService::class.java))// cancel service location
            navController.popBackStack()
            navController.navigate(Screen.LogInScreen.route)
        } }
    ) { innerPaddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPaddingValues)
        ) {
            SwipeRefresh(
                state = swipeState,
                onRefresh = viewModel::refreshSupportedMessages,
                indicatorPadding =  PaddingValues(top = toolbarHeight),
                refreshTriggerDistance = 50.dp,
                swipeEnabled = currentOpenedPopupID < 0 && !searchFieldIsVisible,
                indicator = { state, triggerDistance ->
                    if (state.isRefreshing)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    else if (state.isSwipeInProgress) {
                        val indicatorRefreshTrigger = with(density) { triggerDistance.toPx() }
                        val triggerDistanceFraction = (state.indicatorOffset / indicatorRefreshTrigger)
                            .coerceAtMost(1f)
                        val height = lerp(0f, 1f, 0f, 0.2f, triggerDistanceFraction)
                        val progress = lerp(0f, 1f, 0.2f, 1f, triggerDistanceFraction)

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scaleX = 1f, scaleY = height),
                            progress = progress
                        )
                    }
                }
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(top = toolbarHeight + 8.dp),
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    userScrollEnabled = shouldRespondToUserInteraction,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed (data) { index, item ->
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
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            if (!searchFieldIsVisible) {
                DefaultCenteredTopBar(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = if (!lazyListState.isScrolled) 0 else toolbarOffsetHeightPx.value.roundToInt()
                            )
                                },
                    onNavigationClick = {
                        if (shouldRespondToUserInteraction)
                            scope.launch {
                                scaffoldState.drawerState.open()
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
                    onTextChange =  viewModel::onSearch,
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
                        .offset(y = if (!lazyListState.isScrolled) 0.dp else (-80).dp * (0f + toolbarOffsetHeightPx.value / toolbarHeightPx)),
                    onClick = { if (currentOpenedPopupID < 0) navController.navigate(Screen.SupportScreen.route) },
                    backgroundColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_support_agent_24),
                        contentDescription = "hey",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if(currentOpenedPopupID >= 0) {
                val itemOpenedOffset by remember {
                    derivedStateOf {
                        val fstVisibleItemIndex = lazyListState.firstVisibleItemIndex
                        val fstVisibleItemOffset = lazyListState.firstVisibleItemScrollOffset
                        with (density) {
                            (toolbarHeight + 8.dp +
                                    ((currentOpenedPopupID - fstVisibleItemIndex) * cardHeight).toDp() +
                                    ((currentOpenedPopupID - fstVisibleItemIndex) * 20).dp) - fstVisibleItemOffset.toDp()
                        }
                    }
                }
                val item = data.getOrElse(currentOpenedPopupID,defaultValue = { SupportMsg() })
                val formattedData = let {
                    val year = item.date.slice(0..3)
                    val month = item.date.slice(5..6)
                    val day = item.date.slice(8..9)
                    val hour = item.date.slice(11..12)
                    val minute = item.date.slice(14..15)

                    "$day de $month de $year, $hour:$minute"
                }

                MessageCardAnimatable(
                    header = formatToAnnotatedString(item.header, searchValue),
                    message = formatToAnnotatedString(item.message, searchValue),
                    deltaDate = formatDateToHumanReadable(item.date, sdf),
                    date = formattedData,
                    offsetY = with(density) {itemOpenedOffset.toPx()},
                    openFraction = fraction,
                    cardHeight = cardHeight,
                    onBackClick = {
                        cardExpanded = false
                    }
                )
            }

            AnimatedVisibility (
                modifier = Modifier
                    .wrapContentHeight()
                    .align(Alignment.Center)
                    .alpha(0.7f),
                visible = data.isEmpty() && !searchFieldIsVisible && !swipeState.isRefreshing && viewModel.state.error.isEmpty(),
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

            AnimatedVisibility (
                modifier = Modifier
                    .wrapContentHeight()
                    .align(Alignment.Center)
                    .alpha(0.7f),
                visible = data.isEmpty() && !searchFieldIsVisible && !swipeState.isRefreshing && viewModel.state.error.isNotEmpty(),
                enter = fadeIn(keyframes {
                    0f at 300
                    durationMillis = 400
                }),
                exit = ExitTransition.None,
            ) {
                val noConText = "Sem conexão à internet. Por favor, verifique a sua conexão e tente novamente."
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
                                interactionSource = MutableInteractionSource(),
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



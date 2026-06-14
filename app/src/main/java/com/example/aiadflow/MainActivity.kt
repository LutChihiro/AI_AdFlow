package com.example.aiadflow

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiadflow.data.local.SharedPreferencesAdLocalStateStore
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.ui.feed.AdFeedUiState
import com.example.aiadflow.ui.feed.AdFeedViewModel
import com.example.aiadflow.ui.feed.ConversationSearchMessage
import com.example.aiadflow.ui.media.AdVideoPlayerCard
import com.example.aiadflow.ui.media.AsyncAdImage
import com.example.aiadflow.ui.media.mediaCacheKeyFor
import com.example.aiadflow.ui.media.mediaUrlFor
import com.example.aiadflow.ui.media.rememberRetryImageLoader
import com.example.aiadflow.ui.media.videoStreamUrlFor
import com.example.aiadflow.ui.theme.AIAdFlowTheme
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RefreshSnackbarVisibleMillis = 1200L
private val HomeBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF4F8FF),
        Color(0xFFEEF6FF),
        Color(0xFFFFFFFF)
    )
)
private val PrimaryGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2563EB),
        Color(0xFF7C3AED)
    )
)
private val TagBackgroundColors = listOf(
    Color(0xFFEFF6FF),
    Color(0xFFF3EEFF),
    Color(0xFFF3F6FA)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAdFlowTheme {
                val viewModel = remember {
                    AdFeedViewModel(
                        localStateStore = SharedPreferencesAdLocalStateStore(this)
                    )
                }
                val uiState by viewModel.uiState.collectAsState()
                var selectedAd by remember { mutableStateOf<AdItem?>(null) }
                val shareAd: (Long) -> Unit = { adId ->
                    viewModel.shareAd(adId)?.let { shareText ->
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        startActivity(
                            Intent.createChooser(sendIntent, "\u5206\u4eab\u5e7f\u544a")
                        )
                    }
                }

                AnimatedContent(
                    targetState = selectedAd,
                    label = "adDetailTransition",
                    transitionSpec = {
                        val goingToDetail = initialState == null && targetState != null
                        if (goingToDetail) {
                            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 4 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { it / 3 } + fadeOut())
                        }
                    }
                ) { detailAd ->
                    if (detailAd == null) {
                        HomeScreen(
                            uiState = uiState,
                            onChannelSelected = viewModel::switchChannel,
                            onSearchChange = viewModel::updateSearchText,
                            onConversationDraftChange = viewModel::updateConversationDraft,
                            onConversationSubmit = viewModel::submitConversationalSearch,
                            onConversationClear = viewModel::clearConversation,
                            onTagSelected = viewModel::selectTag,
                            onClearFilters = viewModel::clearFilters,
                            onCollectedFilterClick = viewModel::toggleCollectedOnly,
                            onRefresh = viewModel::refreshAds,
                            onLoadMore = viewModel::loadMoreAds,
                            onRetryLoadMore = viewModel::retryLoadMoreAds,
                            onLikeClick = viewModel::toggleLike,
                            onCollectClick = viewModel::toggleCollect,
                            onShareClick = shareAd,
                            onAdClick = { adId ->
                                viewModel.getAdDetail(adId)?.let { ad ->
                                    viewModel.trackAdClick(ad)
                                    selectedAd = ad
                                }
                            }
                        )
                    } else {
                        AdDetailScreen(
                            ad = detailAd,
                            liked = uiState.likedOverridesByAdId[detailAd.id] ?: detailAd.liked,
                            collected = uiState.collectedOverridesByAdId[detailAd.id] ?: detailAd.collected,
                            onBackClick = { selectedAd = null },
                            onLikeClick = { viewModel.toggleLike(detailAd.id) },
                            onCollectClick = { viewModel.toggleCollect(detailAd.id) },
                            onShareClick = { shareAd(detailAd.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: AdFeedUiState,
    onChannelSelected: (Channel?) -> Unit,
    onSearchChange: (String) -> Unit,
    onConversationDraftChange: (String) -> Unit,
    onConversationSubmit: () -> Unit,
    onConversationClear: () -> Unit,
    onTagSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onCollectedFilterClick: () -> Unit,
    onRefresh: () -> Boolean,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onLikeClick: (Long) -> Unit,
    onCollectClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit,
    onAdClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showConversationSearch by remember { mutableStateOf(false) }
    val shouldLoadMore by remember(uiState.hasMoreAds, uiState.isLoadingMore, uiState.ads.size) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val totalItems = layoutInfo.totalItemsCount

            uiState.ads.isNotEmpty() &&
                uiState.hasMoreAds &&
                !uiState.isLoadingMore &&
                uiState.loadMoreErrorMessage == null &&
                totalItems > 0 &&
                lastVisibleIndex >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    val refreshWithFeedback: () -> Unit = {
        coroutineScope.launch {
            val isSuccess = onRefresh()
            val snackbarJob = launch {
                snackbarHostState.showSnackbar(
                    message = if (isSuccess) "刷新成功" else "刷新失败，请稍后重试",
                    duration = SnackbarDuration.Indefinite
                )
            }
            delay(RefreshSnackbarVisibleMillis)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarJob.join()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeBackgroundBrush)
                .padding(innerPadding)
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AdFeedRefreshContainer(
                isRefreshing = uiState.isLoading,
                onRefresh = refreshWithFeedback
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.PageHorizontal),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
            item(key = "status-bars-spacer") {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }
            item(key = "header") {
                HomeHeader(
                    showCollectedOnly = uiState.showCollectedOnly,
                    collectedCount = uiState.collectedCount,
                    onCollectedFilterClick = onCollectedFilterClick,
                    onAiSearchClick = { showConversationSearch = !showConversationSearch }
                )
            }
            item(key = "channel-tabs") {
                CategoryTabs(
                    channels = uiState.channels,
                    selectedChannel = uiState.selectedChannel,
                    onChannelSelected = onChannelSelected
                )
            }
            item(key = "search-bar") {
                SearchBar(
                    query = uiState.searchText,
                    onQueryChange = onSearchChange
                )
            }
            if (showConversationSearch) {
                item(key = "conversation-search") {
                    ConversationSearchCard(
                        draft = uiState.conversationDraft,
                        messages = uiState.conversationMessages,
                        resultCount = uiState.aiSearchResultCount,
                        suggestedTags = uiState.aiSearchSuggestedTags,
                        onDraftChange = onConversationDraftChange,
                        onSubmit = onConversationSubmit,
                        onClear = onConversationClear
                    )
                }
            }
            if (uiState.hasActiveFilters()) {
                item(key = "active-filters") {
                    ActiveFiltersBar(
                        uiState = uiState,
                        onClearFilters = onClearFilters
                    )
                }
            }
            if (uiState.ads.isEmpty()) {
                item(key = "empty-feed") {
                    EmptyFeed(showCollectedOnly = uiState.showCollectedOnly)
                }
            } else {
                items(
                    items = uiState.ads,
                    key = { it.id }
                ) { ad ->
                    AdCard(
                        ad = ad,
                        liked = uiState.likedOverridesByAdId[ad.id] ?: ad.liked,
                        collected = uiState.collectedOverridesByAdId[ad.id] ?: ad.collected,
                        selectedTag = uiState.selectedTag,
                        onLikeClick = { onLikeClick(ad.id) },
                        onCollectClick = { onCollectClick(ad.id) },
                        onShareClick = { onShareClick(ad.id) },
                        onViewClick = {
                            onAdClick(ad.id)
                        },
                        onTagClick = onTagSelected
                    )
                }
                item(key = "load-more-footer") {
                    LoadMoreFooter(
                        isLoadingMore = uiState.isLoadingMore,
                        hasMoreAds = uiState.hasMoreAds,
                        errorMessage = uiState.loadMoreErrorMessage,
                        onRetryClick = onRetryLoadMore
                    )
                }
            }
            }
            }
        }
    }
    }
}

@Composable
private fun LoadMoreFooter(
    isLoadingMore: Boolean,
    hasMoreAds: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (!hasMoreAds) {
            Text(
                text = "\u6ca1\u6709\u66f4\u591a\u5e7f\u544a\u4e86",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(AppSpacing.Small))
            ActionChip(
                text = "\u91cd\u8bd5",
                selected = true,
                onClick = onRetryClick
            )
        } else if (isLoadingMore) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppSpacing.LoadMoreIndicatorSize),
                strokeWidth = AppSpacing.LoadMoreIndicatorStroke,
                color = AppColors.Primary
            )
            Spacer(modifier = Modifier.width(AppSpacing.Small))
            Text(
                text = "\u6b63\u5728\u52a0\u8f7d\u66f4\u591a",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "\u4e0a\u62c9\u52a0\u8f7d\u66f4\u591a",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HomeHeader(
    showCollectedOnly: Boolean,
    collectedCount: Int,
    onCollectedFilterClick: () -> Unit,
    onAiSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AIAdFlow",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "\u0041\u0049 \u5e7f\u544a\u4fe1\u606f\u6d41",
                color = Color(0xFF6B7A90),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionChip(
                text = if (showCollectedOnly) {
                    "\u5df2\u6536\u85cf $collectedCount"
                } else {
                    "\u6536\u85cf $collectedCount"
                },
                selected = showCollectedOnly,
                onClick = onCollectedFilterClick
            )
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x332563EB),
                        spotColor = Color(0x332563EB)
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onAiSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    color = AppColors.Primary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun ConversationSearchCard(
    draft: String,
    messages: List<ConversationSearchMessage>,
    resultCount: Int,
    suggestedTags: List<String>,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit
) {
    val sendButtonBrush = if (draft.isBlank()) {
        Brush.linearGradient(listOf(Color(0xFFE8EEF7), Color(0xFFE8EEF7)))
    } else {
        PrimaryGradientBrush
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x140F2D5C),
                spotColor = Color(0x140F2D5C)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = Color(0xFFE5ECF6),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryGradientBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "对话式搜索",
                    color = AppColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "用自然语言描述广告场景",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (messages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(AppRadius.Full)
                        .background(Color(0xFFF3F6FA))
                        .clickable { onClear() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "清空",
                        color = AppColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        if (messages.isEmpty()) {
            ConversationHintRow(
                examples = listOf(
                    "找适合学生的学习广告",
                    "推荐本地咖啡优惠",
                    "我要健身视频素材"
                ),
                onExampleClick = onDraftChange
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                messages.takeLast(4).forEach { message ->
                    ConversationMessageBubble(message = message)
                }
            }
        }

        if (messages.isNotEmpty()) {
            Text(
                text = "当前结果 $resultCount 条" + suggestedTags.takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = "  ", separator = " ") { "#$it" }
                    .orEmpty(),
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF7FAFF))
                .border(
                    width = 1.dp,
                    color = if (draft.isBlank()) Color(0xFFE5ECF6) else Color(0xFFB9C7FF),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { innerTextField ->
                    if (draft.isBlank()) {
                        Text(
                            text = "例如：找适合通勤的数码广告",
                            color = Color(0xFF9AA8BB),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            )
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(64.dp)
                    .clip(AppRadius.Full)
                    .background(sendButtonBrush)
                    .clickable(enabled = draft.isNotBlank()) { onSubmit() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "发送",
                    color = if (draft.isBlank()) Color(0xFF94A3B8) else Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun ConversationHintRow(
    examples: List<String>,
    onExampleClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        examples.forEach { example ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF7FAFF))
                    .clickable { onExampleClick(example) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = example,
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ConversationMessageBubble(message: ConversationSearchMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 6.dp,
                        bottomEnd = if (message.isUser) 6.dp else 18.dp
                    )
                )
                .background(if (message.isUser) Color(0xFFEEF2FF) else Color(0xFFF7FAFF))
                .border(
                    width = 1.dp,
                    color = if (message.isUser) Color(0xFFD8DFFF) else Color(0xFFE5ECF6),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 6.dp,
                        bottomEnd = if (message.isUser) 6.dp else 18.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) Color(0xFF3730A3) else Color(0xFF475569),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryTabs(
    channels: List<Channel>,
    selectedChannel: Channel?,
    onChannelSelected: (Channel?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x140F2D5C),
                spotColor = Color(0x140F2D5C)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
        ) {
            ChannelTabChip(
                label = "\u5168\u90e8",
                selected = selectedChannel == null,
                onClick = { onChannelSelected(null) }
            )
            channels.forEach { channel ->
                ChannelTabChip(
                    label = channelLabelFor(channel),
                    selected = selectedChannel == channel,
                    onClick = { onChannelSelected(channel) }
                )
            }
        }
    }
}

@Composable
private fun ChannelTabChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else Color(0xFFE4EAF3),
        label = "channelTabBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFF66758B),
        label = "channelTabText"
    )
    val tabHeight by animateDpAsState(
        targetValue = if (selected) AppSpacing.TabSelectedHeight else AppSpacing.TabHeight,
        label = "channelTabHeight"
    )
    val textWeight = if (selected) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = modifier
            .width(AppSpacing.TabWidth)
            .height(tabHeight)
            .clip(AppRadius.Full)
            .background(if (selected) PrimaryGradientBrush else Brush.linearGradient(listOf(Color.White, Color.White)))
            .border(
                width = AppSpacing.TabBorderWidth,
                color = borderColor,
                shape = AppRadius.Full
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = textWeight)
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x120F2D5C),
                spotColor = Color(0x120F2D5C)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(
                width = AppSpacing.SearchBorderWidth,
                color = if (query.isBlank()) Color(0xFFE5ECF6) else Color(0xFF7C3AED),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "\u2315",
            color = Color(0xFF8EA0B8),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        HorizontalDivider(
            modifier = Modifier
                .height(AppSpacing.SearchDividerHeight)
                .width(AppSpacing.SearchDividerWidth),
            color = Color(0xFFE5ECF6)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextPrimary),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = "\u641c\u7d22\u5e7f\u544a\u3001\u54c1\u724c\u3001\u6807\u7b7e",
                        color = Color(0xFF9AA8BB),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(AppSpacing.SearchClearButton)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5FB))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u00d7",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun ActiveFiltersBar(
    uiState: AdFeedUiState,
    onClearFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Text(
            text = activeFilterLabel(uiState),
            modifier = Modifier.weight(1f),
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        ActionChip(
            text = "\u6e05\u9664",
            selected = false,
            onClick = onClearFilters
        )
    }
}

@Composable
private fun AdCard(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    selectedTag: String?,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewClick: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val mediaSpec = mediaSpecFor(ad.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x140F2D5C),
                spotColor = Color(0x140F2D5C)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .clickable(onClick = onViewClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (ad.type) {
            AdType.SmallImage -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Medium),
                    verticalAlignment = Alignment.Top
                ) {
                    AdMediaBlock(
                        ad = ad,
                        mediaSpec = mediaSpec,
                        modifier = Modifier
                            .width(AppSpacing.SmallImageMediaWidth)
                            .height(AppSpacing.CompactMediaHeight)
                    )
                    AdSummaryContent(
                        ad = ad,
                        modifier = Modifier.weight(1f),
                        showChannelInline = true,
                        selectedTag = selectedTag,
                        onTagClick = onTagClick
                    )
                }
            }
            AdType.ImageText -> {
                AdMediaBlock(
                    ad = ad,
                    mediaSpec = mediaSpec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaSpec.height)
                )
                AdSummaryContent(
                    ad = ad,
                    selectedTag = selectedTag,
                    onTagClick = onTagClick
                )
            }
            AdType.Video -> {
                AdSummaryHeader(ad = ad)
                AdMediaBlock(
                    ad = ad,
                    mediaSpec = mediaSpec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaSpec.height)
                )
                AiSummaryText(summary = ad.summary)
                TagRow(
                    tags = ad.tags,
                    selectedTag = selectedTag,
                    onTagClick = onTagClick
                )
            }
            AdType.LargeImage -> {
                AdMediaBlock(
                    ad = ad,
                    mediaSpec = mediaSpec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaSpec.height)
                )
                AdSummaryContent(
                    ad = ad,
                    titleFirst = true,
                    selectedTag = selectedTag,
                    onTagClick = onTagClick
                )
            }
        }
        ActionRow(
            liked = liked,
            collected = collected,
            onLikeClick = onLikeClick,
            onCollectClick = onCollectClick,
            onShareClick = onShareClick,
            onViewClick = onViewClick
        )
    }
}

@Composable
private fun AdMediaBlock(
    ad: AdItem,
    mediaSpec: AdMediaSpec,
    modifier: Modifier = Modifier
) {
    var isVideoPlaying by remember(ad.id) { mutableStateOf(false) }
    var isVideoMuted by remember(ad.id) { mutableStateOf(false) }
    var isPlayerVisible by remember(ad.id) { mutableStateOf(false) }
    var playbackPositionMs by remember(ad.id) { mutableStateOf(0L) }
    val imageLoader = rememberRetryImageLoader(
        data = mediaUrlFor(ad),
        cacheKey = mediaCacheKeyFor(ad)
    )

    if (ad.type == AdType.Video) {
        VideoPreview(
            ad = ad,
            coverLoader = imageLoader,
            videoUrl = videoStreamUrlFor(ad),
            isPlayerVisible = isPlayerVisible,
            isPlaying = isVideoPlaying,
            isMuted = isVideoMuted,
            playbackPositionMs = playbackPositionMs,
            modifier = modifier,
            onPlayerVisibleChange = { isPlayerVisible = it },
            onPlayingChange = { isVideoPlaying = it },
            onMutedChange = { isVideoMuted = it },
            onPositionChange = { playbackPositionMs = it }
        )
    } else {
        Box(modifier = modifier.clip(RoundedCornerShape(20.dp))) {
            AsyncAdImage(
                loader = imageLoader,
                contentDescription = ad.mediaLabel,
                modifier = Modifier.fillMaxSize(),
                accentColor = mediaSpec.color,
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
            Text(
                text = channelLabelFor(ad.channel),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AppSpacing.Medium),
                color = AppColors.OnPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun VideoPreview(
    ad: AdItem,
    coverLoader: com.example.aiadflow.ui.media.RetryImageLoader,
    videoUrl: String?,
    isPlayerVisible: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    playbackPositionMs: Long,
    modifier: Modifier = Modifier,
    badgeLabel: String = "Video",
    showVideoPill: Boolean = false,
    onPlayerVisibleChange: (Boolean) -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onPositionChange: (Long) -> Unit
) {
    AdVideoPlayerCard(
        ad = ad,
        coverLoader = coverLoader,
        videoUrl = videoUrl,
        isPlayerVisible = isPlayerVisible,
        isPlaying = isPlaying,
        isMuted = isMuted,
        playbackPositionMs = playbackPositionMs,
        modifier = modifier,
        badgeLabel = badgeLabel,
        showVideoPill = showVideoPill,
        onPlayerVisibleChange = onPlayerVisibleChange,
        onPlayingChange = onPlayingChange,
        onMutedChange = onMutedChange,
        onPositionChange = onPositionChange
    )
}

@Composable
private fun VideoMuteButton(
    isMuted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(AppSpacing.VideoMuteIcon)) {
            val speaker = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.38f)
                lineTo(size.width * 0.30f, size.height * 0.38f)
                lineTo(size.width * 0.52f, size.height * 0.20f)
                lineTo(size.width * 0.52f, size.height * 0.80f)
                lineTo(size.width * 0.30f, size.height * 0.62f)
                lineTo(size.width * 0.12f, size.height * 0.62f)
                close()
            }
            drawPath(path = speaker, color = Color.White)

            if (isMuted) {
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.34f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.90f, size.height * 0.66f),
                    strokeWidth = size.width * 0.09f
                )
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.90f, size.height * 0.34f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.66f),
                    strokeWidth = size.width * 0.09f
                )
            } else {
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.38f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.50f),
                    strokeWidth = size.width * 0.08f
                )
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.50f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.62f),
                    strokeWidth = size.width * 0.08f
                )
            }
        }
    }
}

@Composable
private fun VideoPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.88f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(AppSpacing.VideoPlayIcon)) {
            if (isPlaying) {
                val barWidth = size.width * 0.22f
                val gap = size.width * 0.16f
                val top = size.height * 0.22f
                val bottom = size.height * 0.78f
                val leftStart = (size.width - barWidth * 2 - gap) / 2f
                drawRect(
                    color = AppColors.Primary,
                    topLeft = androidx.compose.ui.geometry.Offset(leftStart, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, bottom - top)
                )
                drawRect(
                    color = AppColors.Primary,
                    topLeft = androidx.compose.ui.geometry.Offset(leftStart + barWidth + gap, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, bottom - top)
                )
            } else {
                val path = Path().apply {
                    moveTo(size.width * 0.36f, size.height * 0.22f)
                    lineTo(size.width * 0.36f, size.height * 0.78f)
                    lineTo(size.width * 0.82f, size.height * 0.5f)
                    close()
                }
                drawPath(path = path, color = AppColors.Primary)
            }
        }
    }
}

@Composable
private fun AdSummaryContent(
    ad: AdItem,
    modifier: Modifier = Modifier,
    titleFirst: Boolean = false,
    showChannelInline: Boolean = false,
    selectedTag: String?,
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        if (titleFirst) {
            Text(
                text = ad.title,
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            AdSummaryHeader(ad = ad, showTitle = false, showChannelInline = true)
        } else {
            AdSummaryHeader(ad = ad, showChannelInline = showChannelInline)
        }
        AiSummaryText(summary = ad.summary)
        TagRow(
            tags = ad.tags,
            selectedTag = selectedTag,
            onTagClick = onTagClick
        )
    }
}

@Composable
private fun AdSummaryHeader(
    ad: AdItem,
    showTitle: Boolean = true,
    showChannelInline: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ad.brandName,
                color = Color(0xFF71839A),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showTitle) {
                Text(
                    text = ad.title,
                    color = AppColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showChannelInline) {
            Text(
                text = channelLabelFor(ad.channel),
                modifier = Modifier
                    .clip(AppRadius.Full)
                    .background(Color(0xFFEFF5FF))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun AiSummaryText(summary: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("AI 摘要：")
            }
            append(summary.removePrefix("AI 摘要：").trimStart())
        },
        color = Color(0xFF60738D),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ActionRow(
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionChip(
                text = if (liked) "\u5df2\u70b9\u8d5e" else "\u70b9\u8d5e",
                selected = liked,
                onClick = onLikeClick
            )
            ActionChip(
                text = if (collected) "\u5df2\u6536\u85cf" else "\u6536\u85cf",
                selected = collected,
                onClick = onCollectClick
            )
            ActionChip(
                text = "\u5206\u4eab",
                selected = false,
                onClick = onShareClick
            )
        }
        PrimaryActionChip(
            text = "\u67e5\u770b",
            onClick = onViewClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(
    tags: List<String>,
    selectedTag: String?,
    onTagClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxLines = 2
    ) {
        tags.forEachIndexed { index, tag ->
            val selected = tag.equals(selectedTag, ignoreCase = true)
            val backgroundColor by animateColorAsState(
                targetValue = if (selected) AppColors.Primary else TagBackgroundColors[index % TagBackgroundColors.size],
                label = "tagBackground"
            )
            val borderColor by animateColorAsState(
                targetValue = if (selected) AppColors.Primary else Color.Transparent,
                label = "tagBorder"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) AppColors.OnPrimary else Color(0xFF63758C),
                label = "tagText"
            )
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(AppRadius.Full)
                    .background(backgroundColor)
                    .border(
                        width = AppSpacing.TagBorderWidth,
                        color = borderColor,
                        shape = AppRadius.Full
                    )
                    .clickable { onTagClick(tag) }
                    .widthIn(max = AppSpacing.TagMaxWidth)
                    .padding(
                        horizontal = 12.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$tag",
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(AppRadius.Full)
            .background(if (selected) Color(0xFFEFF5FF) else Color(0xFFF3F6FA))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AppColors.Primary else Color(0xFF66758B),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun PrimaryActionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(AppRadius.Full)
            .background(PrimaryGradientBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun EmptyFeed(showCollectedOnly: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.EmptyHeight)
            .clip(AppRadius.Large)
            .background(AppColors.Surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (showCollectedOnly) {
                "\u6682\u65e0\u6536\u85cf\u5e7f\u544a"
            } else {
                "\u6ca1\u6709\u5339\u914d\u7684\u5e7f\u544a"
            },
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private data class AdMediaSpec(
    val height: Dp,
    val color: Color,
    val labelPrefix: String = "",
    val showPlayButton: Boolean = false,
    val showChannelBadge: Boolean = false
)

private fun mediaSpecFor(adType: AdType): AdMediaSpec = when (adType) {
    AdType.SmallImage -> AdMediaSpec(
        height = AppSpacing.CompactMediaHeight,
        color = Color(0xFF0F766E),
        labelPrefix = "\u5c0f\u56fe / "
    )
    AdType.ImageText -> AdMediaSpec(
        height = AppSpacing.ImageTextMediaHeight,
        color = Color(0xFF7C3AED),
        labelPrefix = "\u56fe\u6587 / "
    )
    AdType.Video -> AdMediaSpec(
        height = AppSpacing.VideoMediaHeight,
        color = Color(0xFFDC2626),
        labelPrefix = "\u89c6\u9891 / ",
        showPlayButton = true
    )
    AdType.LargeImage -> AdMediaSpec(
        height = AppSpacing.LargeImageMediaHeight,
        color = Color(0xFF2563EB),
        labelPrefix = "\u5927\u56fe / ",
        showChannelBadge = true
    )
}

private fun mediaColorFor(adType: AdType): Color = mediaSpecFor(adType).color

private fun channelLabelFor(channel: Channel): String = when (channel) {
    Channel.Featured -> "\u63a8\u8350"
    Channel.Ecommerce -> "\u7535\u5546"
    Channel.Local -> "\u672c\u5730"
    Channel.NewArrival -> "\u65b0\u54c1"
    Channel.Finance -> "\u91d1\u878d"
    Channel.Health -> "\u5065\u5eb7"
    Channel.Travel -> "\u51fa\u884c"
    Channel.Education -> "\u6559\u80b2"
}

private fun AdFeedUiState.hasActiveFilters(): Boolean {
    return selectedChannel != null || searchText.isNotBlank() || !selectedTag.isNullOrBlank() || showCollectedOnly
}

private fun activeFilterLabel(uiState: AdFeedUiState): String {
    val filters = buildList {
        if (uiState.showCollectedOnly) {
            add("\u6536\u85cf")
        }
        uiState.selectedChannel?.let { add(channelLabelFor(it)) }
        uiState.searchText.takeIf { it.isNotBlank() }?.let { add("\"${it.trim()}\"") }
        uiState.selectedTag?.takeIf { it.isNotBlank() }?.let { add("#${it.trim()}") }
    }

    return filters.joinToString(separator = "  ")
}

@Preview(
    name = "Home feed",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun HomeScreenPreview() {
    AIAdFlowTheme {
        var selectedChannel by remember { mutableStateOf<Channel?>(null) }
        var searchText by remember { mutableStateOf("") }
        var selectedTag by remember { mutableStateOf<String?>(null) }
        var showCollectedOnly by remember { mutableStateOf(false) }
        var selectedAd by remember { mutableStateOf<AdItem?>(null) }
        var conversationDraft by remember { mutableStateOf("") }
        var conversationMessages by remember { mutableStateOf<List<ConversationSearchMessage>>(emptyList()) }
        val likedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
        val collectedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
        val visibleAds = PreviewAds
            .filter { selectedChannel == null || it.channel == selectedChannel }
            .filter { ad ->
                val query = searchText.trim()
                query.isBlank() ||
                    ad.title.contains(query, ignoreCase = true) ||
                    ad.summary.contains(query, ignoreCase = true) ||
                    ad.tags.any { it.contains(query, ignoreCase = true) }
            }
            .filter { ad ->
                selectedTag.isNullOrBlank() ||
                    ad.tags.any { it.equals(selectedTag, ignoreCase = true) }
            }
            .filter { ad ->
                !showCollectedOnly || (collectedOverrides[ad.id] ?: ad.collected)
            }

        AnimatedContent(
            targetState = selectedAd,
            label = "previewAdDetailTransition",
            transitionSpec = {
                val goingToDetail = initialState == null && targetState != null
                if (goingToDetail) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            }
        ) { ad ->
            if (ad != null) {
                AdDetailScreen(
                    ad = ad,
                    liked = likedOverrides[ad.id] ?: ad.liked,
                    collected = collectedOverrides[ad.id] ?: ad.collected,
                    onBackClick = { selectedAd = null },
                    onLikeClick = { likedOverrides[ad.id] = !(likedOverrides[ad.id] ?: ad.liked) },
                    onCollectClick = { collectedOverrides[ad.id] = !(collectedOverrides[ad.id] ?: ad.collected) },
                    onShareClick = {}
                )
            } else {
                HomeScreen(
                    uiState = AdFeedUiState(
                        channels = Channel.entries,
                        selectedChannel = selectedChannel,
                        searchText = searchText,
                        selectedTag = selectedTag,
                        ads = visibleAds,
                        likedOverridesByAdId = likedOverrides,
                        collectedOverridesByAdId = collectedOverrides,
                        showCollectedOnly = showCollectedOnly,
                        collectedCount = PreviewAds.count { ad -> collectedOverrides[ad.id] ?: ad.collected },
                        isLoadingMore = false,
                        hasMoreAds = false,
                        loadMoreErrorMessage = null,
                        conversationDraft = conversationDraft,
                        conversationMessages = conversationMessages,
                        aiSearchResultCount = visibleAds.size
                    ),
                    onChannelSelected = { selectedChannel = it },
                    onSearchChange = { searchText = it },
                    onConversationDraftChange = { conversationDraft = it },
                    onConversationSubmit = {
                        val query = conversationDraft.trim()
                        if (query.isNotBlank()) {
                            searchText = query
                            conversationMessages = conversationMessages + listOf(
                                ConversationSearchMessage(1L + conversationMessages.size, query, true),
                                ConversationSearchMessage(
                                    2L + conversationMessages.size,
                                    "已按“$query”为你筛选首页广告。",
                                    false
                                )
                            )
                            conversationDraft = ""
                        }
                    },
                    onConversationClear = {
                        conversationDraft = ""
                        conversationMessages = emptyList()
                    },
                    onTagSelected = { tag ->
                        selectedTag = tag
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { nextTag ->
                                if (nextTag.equals(selectedTag, ignoreCase = true)) null else nextTag
                            }
                    },
                    onClearFilters = {
                        selectedChannel = null
                        searchText = ""
                        selectedTag = null
                        showCollectedOnly = false
                    },
                    onCollectedFilterClick = { showCollectedOnly = !showCollectedOnly },
                    onRefresh = { true },
                    onLoadMore = {},
                    onRetryLoadMore = {},
                    onLikeClick = { adId ->
                        PreviewAds.firstOrNull { it.id == adId }?.let { ad ->
                            likedOverrides[adId] = !(likedOverrides[adId] ?: ad.liked)
                        }
                    },
                    onCollectClick = { adId ->
                        PreviewAds.firstOrNull { it.id == adId }?.let { ad ->
                            collectedOverrides[adId] = !(collectedOverrides[adId] ?: ad.collected)
                        }
                    },
                    onShareClick = {},
                    onAdClick = { adId ->
                        selectedAd = PreviewAds.firstOrNull { it.id == adId }
                    }
                )
            }
        }
    }
}

@Preview(
    name = "Ad detail",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun AdDetailScreenPreview() {
    AIAdFlowTheme {
        AdDetailScreen(
            ad = PreviewAds.first(),
            liked = false,
            collected = false,
            onBackClick = {},
            onLikeClick = {},
            onCollectClick = {},
            onShareClick = {}
        )
    }
}

@Composable
private fun AdDetailScreen(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    onBackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }
            item {
                DetailTopBar(onBackClick = onBackClick)
            }
            item {
                DetailVideoCard(ad = ad)
            }
            item {
                DetailInfoCard(
                    ad = ad,
                    liked = liked,
                    collected = collected,
                    onLikeClick = onLikeClick,
                    onCollectClick = onCollectClick,
                    onShareClick = onShareClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdFeedRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
private fun DetailTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = AppRadius.Full,
                    ambientColor = Color(0x120F2D5C),
                    spotColor = Color(0x120F2D5C)
                )
                .clip(AppRadius.Full)
                .background(Color.White)
                .clickable(onClick = onBackClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u8fd4\u56de",
                color = Color(0xFF60738D),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = "\u5e7f\u544a\u8be6\u60c5",
            color = Color(0xFF102033),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun DetailVideoCard(ad: AdItem) {
    val mediaSpec = mediaSpecFor(ad.type)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (ad.type == AdType.Video) 220.dp else mediaSpec.height)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x180F2D5C),
                spotColor = Color(0x180F2D5C)
            )
            .clip(RoundedCornerShape(26.dp))
    ) {
        if (ad.type == AdType.Video) {
            var isVideoPlaying by remember(ad.id) { mutableStateOf(false) }
            var isVideoMuted by remember(ad.id) { mutableStateOf(false) }
            var isPlayerVisible by remember(ad.id) { mutableStateOf(false) }
            var playbackPositionMs by remember(ad.id) { mutableStateOf(0L) }
            val imageLoader = rememberRetryImageLoader(
                data = mediaUrlFor(ad),
                cacheKey = mediaCacheKeyFor(ad)
            )
            VideoPreview(
                ad = ad,
                coverLoader = imageLoader,
                videoUrl = videoStreamUrlFor(ad),
                isPlayerVisible = isPlayerVisible,
                isPlaying = isVideoPlaying,
                isMuted = isVideoMuted,
                playbackPositionMs = playbackPositionMs,
                modifier = Modifier.fillMaxSize(),
                badgeLabel = "视频素材",
                showVideoPill = true,
                onPlayerVisibleChange = { isPlayerVisible = it },
                onPlayingChange = { isVideoPlaying = it },
                onMutedChange = { isVideoMuted = it },
                onPositionChange = { playbackPositionMs = it }
            )
        } else {
            AdMediaBlock(
                ad = ad,
                mediaSpec = mediaSpec,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DetailInfoCard(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x140F2D5C),
                spotColor = Color(0x140F2D5C)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ad.brandName,
                    color = Color(0xFF71839A),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = ad.title,
                    color = Color(0xFF102033),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DetailChannelPill(channel = ad.channel)
        }
        DetailMediaInfoBlock(ad = ad)
        DetailSummarySection(summary = ad.summary)
        DetailTagRow(tags = ad.tags)
        DetailActionRow(
            liked = liked,
            collected = collected,
            onLikeClick = onLikeClick,
            onCollectClick = onCollectClick,
            onShareClick = onShareClick
        )
    }
}

@Composable
private fun DetailChannelPill(channel: Channel) {
    Text(
        text = channelLabelFor(channel),
        modifier = Modifier
            .clip(AppRadius.Full)
            .background(Color(0xFFEFF5FF))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = AppColors.Primary,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun DetailMediaInfoBlock(ad: AdItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF3F7FC))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = ad.mediaLabel,
            color = AppColors.Primary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = if (ad.type == AdType.Video) {
                ad.videoUrl ?: "本地视频素材"
            } else {
                ad.coverUrl ?: "本地图片素材"
            },
            color = Color(0xFF60738D),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailSummarySection(summary: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(AppRadius.Full)
                    .background(PrimaryGradientBrush)
            )
            Text(
                text = "AI 摘要",
                color = Color(0xFF102033),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = summary.removePrefix("AI 摘要：").trimStart(),
            color = Color(0xFF60738D),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTagRow(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEachIndexed { index, tag ->
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(AppRadius.Full)
                    .background(TagBackgroundColors[index % TagBackgroundColors.size])
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$tag",
                    color = Color(0xFF63758C),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailActionRow(
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailActionChip(
            text = if (liked) "已点赞" else "点赞",
            selected = liked,
            onClick = onLikeClick
        )
        DetailActionChip(
            text = if (collected) "已收藏" else "收藏",
            selected = collected,
            onClick = onCollectClick
        )
        DetailActionChip(
            text = "分享",
            selected = false,
            onClick = onShareClick
        )
    }
}

@Composable
private fun DetailActionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(AppRadius.Full)
            .background(if (selected) PrimaryGradientBrush else Brush.linearGradient(listOf(Color(0xFFF3F6FA), Color(0xFFF3F6FA))))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF66758B),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

private val PreviewAds = listOf(
    AdItem(
        id = 100,
        channel = Channel.Featured,
        type = AdType.Video,
        brandName = "\u8dc3\u52a8\u5de5\u574a",
        title = "\u4e03\u5929\u521b\u4f5c\u8005\u6311\u6218",
        summary = "\u0041\u0049 \u6458\u8981\uff1a\u77ed\u89c6\u9891\u7d20\u6750\u7a81\u51fa\u8bad\u7ec3\u8fdb\u5ea6\u3001\u53ef\u5206\u4eab\u91cc\u7a0b\u7891\u548c\u4f4e\u95e8\u69db\u8bd5\u7528\u62a5\u540d\u8def\u5f84\u3002",
        mediaLabel = "\u89c6\u9891\u7d20\u6750",
        videoUrl = "https://cdn.example.com/ads/runlab-creator-challenge.mp4",
        coverUrl = "https://cdn.example.com/ads/runlab-creator-challenge-cover.jpg",
        tags = listOf("\u5065\u8eab", "\u521b\u4f5c\u8005", "\u8bd5\u7528")
    ),
    AdItem(
        id = 101,
        channel = Channel.Ecommerce,
        type = AdType.LargeImage,
        brandName = "\u5317\u7ebf\u88c5\u5907",
        title = "\u8f7b\u91cf\u901a\u52e4\u53cc\u80a9\u5305",
        summary = "\u0041\u0049 \u6458\u8981\uff1a\u901a\u52e4\u4eba\u7fa4\u5bf9\u9632\u6c34\u7535\u8111\u5305\u7684\u641c\u7d22\u610f\u5411\u8f83\u9ad8\uff0c\u9002\u5408\u6295\u653e\u6548\u7387\u578b\u7d20\u6750\u3002",
        mediaLabel = "\u5546\u54c1\u5927\u56fe",
        tags = listOf("\u53cc\u80a9\u5305", "\u901a\u52e4", "\u9632\u6c34")
    ),
    AdItem(
        id = 102,
        channel = Channel.Local,
        type = AdType.ImageText,
        brandName = "\u8857\u89d2\u5c0f\u9986",
        title = "\u9644\u8fd1\u56e2\u961f\u5de5\u4f5c\u65e5\u5348\u9910\u5957\u9910",
        summary = "\u0041\u0049 \u6458\u8981\uff1a\u5728\u4e34\u8fd1\u51b3\u7b56\u65f6\u6bb5\uff0c\u5411\u9644\u8fd1\u7528\u6237\u63a8\u5e7f\u5de5\u4f5c\u65e5\u5348\u9910\u7ec4\u5408\u4f18\u60e0\u3002",
        mediaLabel = "\u56fe\u6587\u7d20\u6750",
        tags = listOf("\u9910\u996e", "\u9644\u8fd1", "\u5348\u9910")
    ),
    AdItem(
        id = 103,
        channel = Channel.Featured,
        type = AdType.SmallImage,
        brandName = "\u6696\u5c45\u751f\u6d3b",
        title = "\u667a\u80fd\u9999\u85b0\u673a\u7ec4\u5408\u88c5",
        summary = "\u0041\u0049 \u6458\u8981\uff1a\u591c\u95f4\u653e\u677e\u573a\u666f\u4e0e\u9650\u65f6\u7ec4\u5408\u6298\u6263\u7684\u7ed3\u5408\u8868\u73b0\u66f4\u7a33\u5b9a\u3002",
        mediaLabel = "\u5c0f\u56fe\u7d20\u6750",
        tags = listOf("\u5bb6\u5c45", "\u7597\u6108", "\u7ec4\u5408")
    ),
    AdItem(
        id = 104,
        channel = Channel.Finance,
        type = AdType.ImageText,
        brandName = "Bluebird Pay",
        title = "Weekend cashback boost",
        summary = "AI suggests highlighting groceries, transport, and dining as everyday cashback scenes.",
        mediaLabel = "Finance card",
        tags = listOf("Finance", "Cashback", "Dining")
    ),
    AdItem(
        id = 105,
        channel = Channel.Health,
        type = AdType.LargeImage,
        brandName = "Daily Greens",
        title = "Morning nutrition subscription",
        summary = "Best-performing copy connects breakfast routines with simple energy and wellness habits.",
        mediaLabel = "Health visual",
        tags = listOf("Health", "Wellness", "Subscription")
    ),
    AdItem(
        id = 106,
        channel = Channel.Education,
        type = AdType.SmallImage,
        brandName = "SkillForge",
        title = "AI design course trial lesson",
        summary = "Campaign should highlight portfolio outcomes, guided practice, and a short trial format.",
        mediaLabel = "Course image",
        tags = listOf("Education", "AI", "Creator")
    )
)

package com.example.aiadflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiadflow.ui.theme.AIAdFlowTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 频道页签的轻量模型，id 用于筛选数据，title 用于界面展示。
private data class ChannelTab(val id: String, val title: String)

// 当前 Demo 支持的三个广告频道，需要和 MockAdProvider 中 AdItem.channel 对应。
private val ChannelTabs = listOf(
    ChannelTab("featured", "精选"),
    ChannelTab("ecommerce", "电商"),
    ChannelTab("local", "本地")
)

// 页面统一背景渐变，列表页和详情页共用，保证切换详情时视觉连续。
private val PageBackground = Brush.verticalGradient(
    listOf(Color(0xFFEAF4FF), Color(0xFFF8FBFF), Color(0xFFFFFFFF))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAdFlowTheme(dynamicColor = false) {
                AiAdFlowApp()
            }
        }
    }
}

@Composable
private fun AiAdFlowApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Mock 数据只需要在首次组合时读取一次；后续交互通过本地状态派生展示结果。
    val baseAds = remember { MockAdProvider.ads() }

    // 使用 rememberSaveable 保存核心交互状态，避免屏幕旋转或进程恢复时丢失当前操作。
    var selectedChannel by rememberSaveable { mutableStateOf("featured") }
    var searchDraft by rememberSaveable { mutableStateOf("") }
    var committedQuery by rememberSaveable { mutableStateOf("") }
    var selectedAdId by rememberSaveable { mutableStateOf<Long?>(null) }

    // 原始广告数据保持不变；用户点赞/收藏只记录覆盖值，便于刷新、筛选和详情页共用状态。
    var likedOverrides by rememberSaveable { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
    var collectedOverrides by rememberSaveable { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }

    // refreshTick 用于模拟刷新后数据顺序变化；visibleCount 控制列表分批加载数量。
    var refreshTick by rememberSaveable { mutableIntStateOf(0) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var visibleCount by rememberSaveable { mutableIntStateOf(4) }
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // 将原始广告和用户本地操作状态合并，所有列表/详情展示都通过它得到最终状态。
    fun resolveAd(ad: AdItem): AdItem = ad.copy(
        liked = likedOverrides[ad.id] ?: ad.liked,
        collected = collectedOverrides[ad.id] ?: ad.collected
    )

    fun toggleLiked(id: Long, liked: Boolean) {
        likedOverrides = likedOverrides + (id to liked)
    }

    fun toggleCollected(id: Long, collected: Boolean) {
        collectedOverrides = collectedOverrides + (id to collected)
    }

    fun refresh() {
        scope.launch {
            isRefreshing = true
            // 模拟网络刷新耗时，让下拉刷新状态有可见反馈。
            delay(500)
            refreshTick += 1
            visibleCount = 4
            listState.animateScrollToItem(0)
            isRefreshing = false
        }
    }

    // 先按频道筛选，再根据 refreshTick 翻转顺序，用最少逻辑模拟“刷新得到新排序”。
    val channelAds = remember(baseAds, selectedChannel, refreshTick) {
        val ads = baseAds.filter { it.channel == selectedChannel }
        if (refreshTick % 2 == 0) ads else ads.reversed()
    }

    // 搜索只在提交后生效，避免输入过程中的每个字符都改变列表；同时合并点赞/收藏覆盖状态。
    val filteredAds = remember(channelAds, committedQuery, likedOverrides, collectedOverrides) {
        val query = committedQuery.trim()
        channelAds.filter { ad ->
            query.isBlank() ||
                ad.title.contains(query, ignoreCase = true) ||
                ad.summary.contains(query, ignoreCase = true) ||
                ad.tags.any { it.contains(query, ignoreCase = true) }
        }.map(::resolveAd)
    }
    val visibleAds = filteredAds.take(visibleCount)

    // 详情页只保存选中的广告 id，展示时再从原始列表解析，确保状态覆盖始终同步。
    val selectedAd = selectedAdId?.let { id ->
        baseAds.firstOrNull { it.id == id }?.let(::resolveAd)
    }

    // selectedAdId 为空时显示信息流，否则显示详情页；两个页面共享同一组交互状态。
    if (selectedAd == null) {
        AdFeedScreen(
            selectedChannel = selectedChannel,
            searchDraft = searchDraft,
            committedQuery = committedQuery,
            listState = listState,
            ads = visibleAds,
            totalCount = filteredAds.size,
            isRefreshing = isRefreshing,
            onRefresh = ::refresh,
            onChannelChange = {
                selectedChannel = it
                visibleCount = 4
                // 切换频道后回到顶部，避免沿用上一个频道的滚动位置造成内容错位感。
                scope.launch { listState.animateScrollToItem(0) }
            },
            onSearchDraftChange = { searchDraft = it },
            onSearchCommit = {
                committedQuery = searchDraft.trim()
                visibleCount = 4
                // 提交搜索后重置分页数量，并滚回顶部展示最相关的第一批结果。
                scope.launch { listState.animateScrollToItem(0) }
            },
            onLoadMore = {
                if (visibleCount < filteredAds.size) {
                    visibleCount = (visibleCount + 4).coerceAtMost(filteredAds.size)
                }
            },
            onToggleLiked = { ad -> toggleLiked(ad.id, !ad.liked) },
            onToggleCollected = { ad -> toggleCollected(ad.id, !ad.collected) },
            onShare = { Toast.makeText(context, "已分享", Toast.LENGTH_SHORT).show() },
            onOpenDetail = { selectedAdId = it.id },
            onInfo = {
                Toast.makeText(context, "AI 广告信息流 Demo", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        AdDetailScreen(
            ad = selectedAd,
            onBack = { selectedAdId = null },
            onToggleLiked = { toggleLiked(selectedAd.id, !selectedAd.liked) },
            onToggleCollected = { toggleCollected(selectedAd.id, !selectedAd.collected) },
            onShare = { Toast.makeText(context, "已分享", Toast.LENGTH_SHORT).show() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdFeedScreen(
    selectedChannel: String,
    searchDraft: String,
    committedQuery: String,
    listState: LazyListState,
    ads: List<AdItem>,
    totalCount: Int,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onChannelChange: (String) -> Unit,
    onSearchDraftChange: (String) -> Unit,
    onSearchCommit: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleLiked: (AdItem) -> Unit,
    onToggleCollected: (AdItem) -> Unit,
    onShare: () -> Unit,
    onOpenDetail: (AdItem) -> Unit,
    onInfo: () -> Unit
) {
    val shouldLoadMore by remember(ads.size, totalCount) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            /*
             * LazyColumn 的下标不是广告列表下标：
             * 0 是列表头，1..ads.size 才是广告卡片，最后还会有一个底部状态 item。
             * 因此需要用 ads.size 作为“最后一条广告在 LazyColumn 中的下标”，
             * 避免用 ads.lastIndex 导致刚进入列表就提前加载下一页。
             */
            ads.isNotEmpty() &&
                ads.size < totalCount &&
                lastVisible >= ads.size
        }
    }

    // 将滚动状态转换成一次性的副作用；ads.size 变化后才允许继续触发下一批加载。
    LaunchedEffect(shouldLoadMore, ads.size, totalCount) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // 列表头包含固定操作区，但仍作为 LazyColumn 的 item 参与滚动和边距计算。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Spacer(Modifier.height(10.dp))
                    TopBar(onInfo = onInfo, onRefresh = onRefresh)
                    Spacer(Modifier.height(18.dp))
                    ChannelTabs(
                        selectedChannel = selectedChannel,
                        onChannelChange = onChannelChange
                    )
                    Spacer(Modifier.height(14.dp))
                    SearchBar(
                        value = searchDraft,
                        onValueChange = onSearchDraftChange,
                        onSearch = onSearchCommit
                    )
                    Spacer(Modifier.height(10.dp))
                    FilterStatus(committedQuery)
                }
            }
            if (ads.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(items = ads, key = { it.id }) { ad ->
                    AdCard(
                        ad = ad,
                        onClick = { onOpenDetail(ad) },
                        onToggleLiked = { onToggleLiked(ad) },
                        onToggleCollected = { onToggleCollected(ad) },
                        onShare = onShare,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
                item {
                    Text(
                        text = if (ads.size < totalCount) "正在加载更多..." else "已经到底了",
                        color = Color(0xFF8B95A5),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onInfo: () -> Unit, onRefresh: () -> Unit) {
    // 顶部栏只负责触发页面级动作，具体 Toast 和刷新逻辑由父组件注入。
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "AI广告信息流",
            color = Color(0xFF172033),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Spacer(Modifier.weight(1f))
        CircleIconButton(text = "i", onClick = onInfo)
        Spacer(Modifier.width(8.dp))
        CircleIconButton(text = "↻", onClick = onRefresh)
    }
}

@Composable
private fun CircleIconButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(8.dp, CircleShape, spotColor = Color(0x1A4C6FFF))
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color(0xFF516079), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChannelTabs(selectedChannel: String, onChannelChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ChannelTabs.forEach { tab ->
            val selected = tab.id == selectedChannel
            // 选中态和未选中态都使用渐变，保持控件样式统一，只通过颜色区分层级。
            val background = if (selected) {
                Brush.horizontalGradient(listOf(Color(0xFFC9E8FF), Color(0xFFE0D7FF)))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFFF8FAFD), Color(0xFFF1F4F8)))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(background)
                    .clickable { onChannelChange(tab.id) }
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(
                    text = tab.title,
                    color = if (selected) Color(0xFF24375E) else Color(0xFF586274),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit) {
    // 输入值由父组件持有，点击按钮或键盘 Search 才会提交到 committedQuery。
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0x12000000))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = "⌕", color = Color(0xFF708096), fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFF1F2937), fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = "对话式搜索：比如‘找运动相关的广告’",
                        color = Color(0xFF9AA5B4),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )
        TextButton(
            onClick = onSearch,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6172FF)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("点击", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterStatus(query: String) {
    Text(
        text = "当前筛选：${query.ifBlank { "无" }}",
        color = Color(0xFF6B7484),
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 2.dp)
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text("没有匹配的广告", color = Color(0xFF8B95A5))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdCard(
    ad: AdItem,
    onClick: () -> Unit,
    onToggleLiked: () -> Unit,
    onToggleCollected: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据 adType 分发到不同卡片布局；未知类型兜底使用大图布局，避免数据异常导致空白。
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        when (ad.adType) {
            "smallImage" -> SmallImageAd(ad, onToggleLiked, onToggleCollected, onShare)
            "imageText" -> ImageTextAd(ad, onToggleLiked, onToggleCollected, onShare)
            "video" -> LargeAd(ad, onToggleLiked, onToggleCollected, onShare, isVideo = true)
            else -> LargeAd(ad, onToggleLiked, onToggleCollected, onShare, isVideo = false)
        }
    }
}

@Composable
private fun SmallImageAd(
    ad: AdItem,
    onToggleLiked: () -> Unit,
    onToggleCollected: () -> Unit,
    onShare: () -> Unit
) {
    // 小图布局突出文字内容，图片作为右侧辅助信息，适合列表中较短的推广素材。
    Column(Modifier.padding(16.dp)) {
        CardHeader(ad)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdTextContent(ad, modifier = Modifier.weight(1f), titleMaxLines = 2, summaryMaxLines = 3)
            ImagePlaceholder(
                imageType = ad.imageType,
                isVideo = false,
                modifier = Modifier
                    .width(116.dp)
                    .height(104.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        TagsAndActions(ad, onToggleLiked, onToggleCollected, onShare)
    }
}

@Composable
private fun ImageTextAd(
    ad: AdItem,
    onToggleLiked: () -> Unit,
    onToggleCollected: () -> Unit,
    onShare: () -> Unit
) {
    // 图文布局把图片放在左侧，适合商品或门店类广告先展示视觉元素。
    Column(Modifier.padding(16.dp)) {
        CardHeader(ad)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ImagePlaceholder(
                imageType = ad.imageType,
                isVideo = false,
                modifier = Modifier
                    .width(124.dp)
                    .height(132.dp)
            )
            AdTextContent(ad, modifier = Modifier.weight(1f), titleMaxLines = 2, summaryMaxLines = 3)
        }
        Spacer(Modifier.height(12.dp))
        TagsAndActions(ad, onToggleLiked, onToggleCollected, onShare)
    }
}

@Composable
private fun LargeAd(
    ad: AdItem,
    onToggleLiked: () -> Unit,
    onToggleCollected: () -> Unit,
    onShare: () -> Unit,
    isVideo: Boolean
) {
    // 大图和视频共用主视觉布局，通过 isVideo 控制是否叠加播放入口。
    Column(Modifier.padding(16.dp)) {
        CardHeader(ad)
        Spacer(Modifier.height(10.dp))
        AdTextContent(ad, titleMaxLines = 2, summaryMaxLines = 2)
        Spacer(Modifier.height(12.dp))
        ImagePlaceholder(
            imageType = ad.imageType,
            isVideo = isVideo,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.9f)
        )
        Spacer(Modifier.height(12.dp))
        TagsAndActions(ad, onToggleLiked, onToggleCollected, onShare)
    }
}

@Composable
private fun CardHeader(ad: AdItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "广告",
            color = Color(0xFF1B8F60),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE8F7EF))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = ad.brandName,
            color = Color(0xFF536071),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AdTextContent(
    ad: AdItem,
    modifier: Modifier = Modifier,
    titleMaxLines: Int,
    summaryMaxLines: Int
) {
    Column(modifier) {
        Text(
            text = ad.title,
            color = Color(0xFF172033),
            fontSize = 18.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Bold,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "AI 摘要：${ad.summary}",
            color = Color(0xFF697385),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = summaryMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ImagePlaceholder(imageType: String, isVideo: Boolean, modifier: Modifier = Modifier) {
    // 当前 Demo 没有真实图片资源，使用渐变占位块模拟素材区域，并保留类型标签。
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFBFE7FF), Color(0xFFDCD5FF), Color(0xFFFFEDF7))
                )
            )
    ) {
        Text(
            text = imageType,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x66000000))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0x88FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color(0xFF6172FF), fontSize = 24.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsAndActions(
    ad: AdItem,
    onToggleLiked: () -> Unit,
    onToggleCollected: () -> Unit,
    onShare: () -> Unit
) {
    // FlowRow 允许标签在窄屏自动换行，避免固定 Row 造成文本截断或按钮挤压。
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ad.tags.forEach { tag ->
            Text(
                text = tag,
                color = Color(0xFF60708A),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF2F6FB))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "AI 标签 · 摘要已生成",
            color = Color(0xFF98A1B0),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        ActionButton("赞", ad.liked, onToggleLiked)
        ActionButton("藏", ad.collected, onToggleCollected)
        ActionButton("转", false, onShare)
    }
}

@Composable
private fun ActionButton(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (active) Color.White else Color(0xFF687386),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (active) Color(0xFF6574FF) else Color(0xFFF4F7FB)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdDetailScreen(
    ad: AdItem,
    onBack: () -> Unit,
    onToggleLiked: () -> Unit,
    onToggleCollected: () -> Unit,
    onShare: () -> Unit
) {
    // 详情页复用列表中的展示组件，保证标题、主视觉、标签和互动状态一致。
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp)
    ) {
        item {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(text = "‹", onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ad.brandName, color = Color(0xFF657086), fontSize = 14.sp)
                    Text(
                        text = ad.title,
                        color = Color(0xFF172033),
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            ImagePlaceholder(
                imageType = ad.imageType,
                isVideo = ad.adType == "video",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    CardHeader(ad)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "AI 摘要：${ad.summary}",
                        color = Color(0xFF4F5D73),
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ad.tags.forEach { tag ->
                            Text(
                                text = tag,
                                color = Color(0xFF60708A),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFF2F6FB))
                                    .padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailActionButton("赞", ad.liked, onToggleLiked, Modifier.weight(1f))
                        DetailActionButton("藏", ad.collected, onToggleCollected, Modifier.weight(1f))
                        DetailActionButton("转", false, onShare, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailActionButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (active) Color(0xFF6574FF) else Color(0xFFE1E7F0)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) Color(0xFF6574FF) else Color.White,
            contentColor = if (active) Color.White else Color(0xFF5C687A)
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
private fun AiAdFlowPreview() {
    AIAdFlowTheme(dynamicColor = false) {
        AiAdFlowApp()
    }
}

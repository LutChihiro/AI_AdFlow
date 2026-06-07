package com.example.aiadflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.aiadflow.data.mock.MockAdProvider
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.ui.theme.AIAdFlowTheme
import com.example.aiadflow.ui.theme.AppColors
import com.example.aiadflow.ui.theme.AppRadius
import com.example.aiadflow.ui.theme.AppSpacing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAdFlowTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    var selectedChannel by remember { mutableStateOf(ChannelTabs.first().id) }
    var searchQuery by remember { mutableStateOf("") }
    val likedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
    val collectedOverrides = remember { mutableStateMapOf<Long, Boolean>() }
    val visibleAds = remember(selectedChannel, searchQuery, likedOverrides.size, collectedOverrides.size) {
        SampleAds.filter { ad ->
            val matchesChannel = selectedChannel == "all" || ad.channel.id == selectedChannel
            val query = searchQuery.trim()
            val matchesSearch = query.isBlank() ||
                ad.brandName.contains(query, ignoreCase = true) ||
                ad.title.contains(query, ignoreCase = true) ||
                ad.summary.contains(query, ignoreCase = true) ||
                ad.tags.any { it.contains(query, ignoreCase = true) }

            matchesChannel && matchesSearch
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.PageBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.PageHorizontal),
            contentPadding = PaddingValues(bottom = AppSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Section)
        ) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }
            item {
                HeaderBar()
            }
            item {
                ChannelTabs(
                    selectedChannel = selectedChannel,
                    onChannelSelected = { selectedChannel = it }
                )
            }
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }
            if (visibleAds.isEmpty()) {
                item {
                    EmptyFeed()
                }
            } else {
                items(
                    items = visibleAds,
                    key = { it.id }
                ) { ad ->
                    AdCard(
                        ad = ad,
                        liked = likedOverrides[ad.id] ?: ad.liked,
                        collected = collectedOverrides[ad.id] ?: ad.collected,
                        onLikeClick = {
                            likedOverrides[ad.id] = !(likedOverrides[ad.id] ?: ad.liked)
                        },
                        onCollectClick = {
                            collectedOverrides[ad.id] = !(collectedOverrides[ad.id] ?: ad.collected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AIAdFlow",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "\u0041\u0049 \u5e7f\u544a\u4fe1\u606f\u6d41",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Box(
            modifier = Modifier
                .size(AppSpacing.IconButton)
                .clip(CircleShape)
                .background(AppColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ChannelTabs(
    selectedChannel: String,
    onChannelSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        ChannelTabs.forEach { tab ->
            val selected = tab.id == selectedChannel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppSpacing.TabHeight)
                    .clip(AppRadius.Full)
                    .background(if (selected) AppColors.Primary else AppColors.Surface)
                    .clickable { onChannelSelected(tab.id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.SearchHeight)
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(horizontal = AppSpacing.Medium),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextPrimary),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = "\u641c\u7d22\u5e7f\u544a\u3001\u54c1\u724c\u3001\u6807\u7b7e",
                        color = AppColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun AdCard(
    ad: AdItem,
    liked: Boolean,
    collected: Boolean,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.Large)
            .background(AppColors.Surface)
            .padding(AppSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (ad.type == AdType.LargeImage || ad.type == AdType.Video) AppSpacing.AdMediaHeight else AppSpacing.CompactMediaHeight)
                .clip(AppRadius.Medium)
                .background(mediaColorFor(ad.type))
                .padding(AppSpacing.Medium)
        ) {
            Text(
                text = ad.mediaLabel,
                color = AppColors.OnPrimary,
                style = MaterialTheme.typography.labelLarge
            )
            if (ad.type == AdType.Video) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(AppSpacing.PlayButton)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u64ad\u653e",
                        color = AppColors.Primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ad.brandName,
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = ad.title,
                    color = AppColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = channelLabelFor(ad.channel),
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            text = ad.summary,
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        TagRow(tags = ad.tags)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
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
            Spacer(modifier = Modifier.weight(1f))
            ActionChip(
                text = "\u67e5\u770b",
                selected = true,
                onClick = {}
            )
        }
    }
}

@Composable
private fun TagRow(tags: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Small)
    ) {
        tags.take(3).forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(AppRadius.Full)
                    .background(AppColors.PageBackground)
                    .padding(
                        horizontal = AppSpacing.Small,
                        vertical = AppSpacing.TagVertical
                    )
            ) {
                Text(
                    text = "#$tag",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
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
            .height(AppSpacing.ActionHeight)
            .clip(AppRadius.Full)
            .background(if (selected) AppColors.Primary else AppColors.PageBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AppColors.OnPrimary else AppColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun EmptyFeed() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppSpacing.EmptyHeight)
            .clip(AppRadius.Large)
            .background(AppColors.Surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u6ca1\u6709\u5339\u914d\u7684\u5e7f\u544a",
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun mediaColorFor(adType: AdType): Color = when (adType) {
    AdType.SmallImage -> Color(0xFF0F766E)
    AdType.ImageText -> Color(0xFF7C3AED)
    AdType.Video -> Color(0xFFDC2626)
    AdType.LargeImage -> Color(0xFF2563EB)
}

private fun channelLabelFor(channel: Channel): String =
    ChannelTabs.firstOrNull { it.id == channel.id }?.label ?: channel.title

private data class ChannelTab(
    val id: String,
    val label: String
)

private val ChannelTabs = listOf(
    ChannelTab("all", "\u5168\u90e8"),
    ChannelTab("featured", "\u63a8\u8350"),
    ChannelTab("ecommerce", "\u7535\u5546"),
    ChannelTab("local", "\u672c\u5730")
)

private val SampleAds = MockAdProvider.ads()

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AIAdFlowTheme {
        HomeScreen()
    }
}

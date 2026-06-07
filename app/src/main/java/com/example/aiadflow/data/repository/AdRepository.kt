package com.example.aiadflow.data.repository

import com.example.aiadflow.data.mock.MockAdProvider
import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.Channel
import com.example.aiadflow.data.model.TrackEvent

/**
 * 广告数据仓库。
 *
 * 负责从数据源读取频道和广告，并提供搜索过滤与埋点记录能力。
 */
class AdRepository(
    /** 广告数据来源，当前默认使用本地 mock provider。 */
    private val adProvider: MockAdProvider = MockAdProvider
) {
    /** 内存中的埋点事件列表，便于开发阶段验证点击和曝光行为。 */
    private val trackedEvents = mutableListOf<TrackEvent>()

    /** 获取信息流支持的频道列表。 */
    fun getChannels(): List<Channel> = adProvider.channels()

    /**
     * 按频道和搜索词获取广告。
     *
     * 搜索词会匹配品牌名、标题、摘要和标签。
     */
    fun getAds(
        channel: Channel? = null,
        query: String = ""
    ): List<AdItem> {
        val normalizedQuery = query.trim()

        return adProvider.ads()
            .filter { channel == null || it.channel == channel }
            .filter { ad ->
                normalizedQuery.isEmpty() ||
                    ad.brandName.contains(normalizedQuery, ignoreCase = true) ||
                    ad.title.contains(normalizedQuery, ignoreCase = true) ||
                    ad.summary.contains(normalizedQuery, ignoreCase = true) ||
                    ad.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
            }
    }

    /** 记录一条广告行为事件。 */
    fun track(event: TrackEvent) {
        trackedEvents += event
    }

    /** 返回已记录埋点事件的只读副本，避免外部直接修改内部列表。 */
    fun getTrackedEvents(): List<TrackEvent> = trackedEvents.toList()
}

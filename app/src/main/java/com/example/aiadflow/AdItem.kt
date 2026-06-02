package com.example.aiadflow

/**
 * 广告信息流中单条广告的数据模型。
 *
 * 使用本地 Mock 数据驱动页面，后续接入接口时也可以继续沿用这个模型作为
 * UI 层的展示数据。`liked` 和 `collected` 表示数据源给出的初始状态，用户在页面上的
 * 临时操作会在 MainActivity 中通过 override map 覆盖，而不是直接修改原始列表。
 */
data class AdItem(
    /** 广告唯一标识，用于列表 key、详情跳转以及点赞/收藏状态覆盖。 */
    val id: Long,
    /** 所属频道，需要和 MainActivity 中 ChannelTab.id 保持一致。 */
    val channel: String,
    /** 品牌或商家名称，展示在广告卡片头部。 */
    val brandName: String,
    /** 广告主标题。 */
    val title: String,
    /** AI 摘要文案，列表和详情页都会复用。 */
    val summary: String,
    /** 图片占位类型文案，例如小图、图文、视频、大图。 */
    val imageType: String,
    /** 卡片布局类型：smallImage、imageText、video 或 largeImage。 */
    val adType: String,
    /** 用于展示标签，也参与搜索匹配。 */
    val tags: List<String>,
    /** 初始点赞状态。 */
    val liked: Boolean,
    /** 初始收藏状态。 */
    val collected: Boolean
)

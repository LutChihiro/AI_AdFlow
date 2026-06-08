package com.example.aiadflow.data.model

/**
 * 广告信息流页面使用的广告展示数据。
 *
 * 后续接入接口时，可以在仓库层把接口 DTO 转成这个模型，
 * 这样 Compose UI 不需要直接依赖网络结构或数据库结构。
 */
data class AdItem(
    /** 广告唯一标识，用于 LazyColumn key、埋点和本地交互状态覆盖。 */
    val id: Long,
    /** 广告所属频道。 */
    val channel: Channel,
    /** 广告卡片媒体区域的展示类型。 */
    val type: AdType,
    /** 品牌、商家或广告主名称，显示在卡片头部。 */
    val brandName: String,
    /** 广告主标题，作为卡片的主要标题展示。 */
    val title: String,
    /** AI 摘要或投放说明，显示在标题下方。 */
    val summary: String,
    /** 媒体占位区域里的标签文案，例如图片或视频类型说明。 */
    val mediaLabel: String,
    /** Video ad playback URL. Non-video ads keep this as null. */
    val videoUrl: String? = null,
    /** Video ad cover image URL. Non-video ads keep this as null. */
    val coverUrl: String? = null,
    /** 可搜索标签，会在广告摘要下方渲染为标签 chip。 */
    val tags: List<String>,
    /** 数据源给出的初始点赞状态，用户临时点击状态不直接修改该模型。 */
    val liked: Boolean = false,
    /** 数据源给出的初始收藏状态，用户临时点击状态不直接修改该模型。 */
    val collected: Boolean = false
)

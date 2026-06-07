package com.example.aiadflow.data.model

/**
 * 广告频道枚举。
 *
 * id 用于筛选、埋点和与 UI tab 对齐；title 是默认展示名称。
 */
enum class Channel(
    /** 频道稳定标识，适合用于筛选条件和服务端字段。 */
    val id: String,
    /** 频道默认标题，UI 可以按需要再映射成本地化文案。 */
    val title: String
) {
    /** 推荐频道。 */
    Featured("featured", "Featured"),
    /** 电商广告频道。 */
    Ecommerce("ecommerce", "Ecommerce"),
    /** 本地生活广告频道。 */
    Local("local", "Local")
}

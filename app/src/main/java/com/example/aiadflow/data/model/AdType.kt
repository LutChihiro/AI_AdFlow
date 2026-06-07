package com.example.aiadflow.data.model

/**
 * 广告卡片的素材展示类型。
 *
 * UI 根据该类型决定媒体区域高度、颜色和是否显示播放入口。
 */
enum class AdType {
    /** 小图广告，适合紧凑信息展示。 */
    SmallImage,
    /** 图文广告，适合图片和说明文案结合展示。 */
    ImageText,
    /** 视频广告，卡片中会显示播放入口。 */
    Video,
    /** 大图广告，使用更高的媒体区域突出视觉素材。 */
    LargeImage
}

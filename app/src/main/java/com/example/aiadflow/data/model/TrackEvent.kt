package com.example.aiadflow.data.model

/**
 * 广告行为埋点事件。
 *
 * 当前仓库层先保存在内存中，后续可以替换为上报接口或本地日志。
 */
data class TrackEvent(
    /** 发生行为的广告 id。 */
    val adId: Long,
    /** 事件发生时广告所在频道。 */
    val channel: Channel,
    /** 事件名称，例如 impression 或 click。 */
    val eventName: String,
    /** 事件创建时间，默认使用当前系统时间。 */
    val timestampMillis: Long = System.currentTimeMillis()
)

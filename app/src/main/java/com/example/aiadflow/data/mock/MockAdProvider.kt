package com.example.aiadflow.data.mock

import com.example.aiadflow.data.model.AdItem
import com.example.aiadflow.data.model.AdType
import com.example.aiadflow.data.model.Channel

object MockAdProvider {
    fun channels(): List<Channel> = Channel.entries

    fun ads(): List<AdItem> {
        val ads = listOf(
            AdItem(
                id = 1,
                channel = Channel.Featured,
                type = AdType.LargeImage,
                brandName = "Northline Gear",
                title = "Lightweight commuter backpack",
                summary = "AI predicts strong purchase intent for waterproof laptop bags among weekday commuters.",
                mediaLabel = "Product hero",
                tags = listOf("Backpack", "Commute", "Waterproof"),
                collected = true
            ),
            AdItem(
                id = 2,
                channel = Channel.Featured,
                type = AdType.Video,
                brandName = "RunLab",
                title = "Seven-day creator fitness challenge",
                summary = "Short video creative highlights daily progress, shareable milestones, and a low-friction trial path.",
                mediaLabel = "Video creative",
                tags = listOf("Fitness", "Creator", "Trial")
            ),
            AdItem(
                id = 3,
                channel = Channel.Featured,
                type = AdType.ImageText,
                brandName = "Bluebird Pay",
                title = "Cashback benefits onboarding story",
                summary = "A three-card sequence explains everyday savings before guiding users to compare cashback tiers.",
                mediaLabel = "Brand visual",
                tags = listOf("Finance", "Cashback", "Onboarding")
            ),
            AdItem(
                id = 4,
                channel = Channel.Featured,
                type = AdType.SmallImage,
                brandName = "Nova Audio",
                title = "Noise cancelling earbuds for study",
                summary = "Balanced sound, long battery life, and a compact case for daily use.",
                mediaLabel = "Small image",
                tags = listOf("Digital", "Student", "Budget"),
                liked = true
            ),
            AdItem(
                id = 5,
                channel = Channel.Ecommerce,
                type = AdType.LargeImage,
                brandName = "Orbit Phone",
                title = "Slim phone with strong night photos",
                summary = "A daily phone focused on battery, portrait mode, and low-light image details.",
                mediaLabel = "Large image",
                tags = listOf("Digital", "Deal", "Trend")
            ),
            AdItem(
                id = 6,
                channel = Channel.Ecommerce,
                type = AdType.ImageText,
                brandName = "Simple Pack",
                title = "Laptop backpack for work and class",
                summary = "Clear compartments, water resistant fabric, and a protected laptop sleeve.",
                mediaLabel = "Image text",
                tags = listOf("Commute", "Student", "Work"),
                liked = true
            ),
            AdItem(
                id = 7,
                channel = Channel.Ecommerce,
                type = AdType.SmallImage,
                brandName = "Warm Home",
                title = "Smart aroma diffuser bundle",
                summary = "Top-performing copy pairs nighttime relaxation with a limited-time bundle discount.",
                mediaLabel = "Small image",
                tags = listOf("Home", "Wellness", "Bundle"),
                collected = true
            ),
            AdItem(
                id = 8,
                channel = Channel.Ecommerce,
                type = AdType.Video,
                brandName = "DeskLite",
                title = "Adjustable desk lamp for focused work",
                summary = "Video creative shows brightness modes, cable management, and compact desk setups.",
                mediaLabel = "Video",
                tags = listOf("Office", "Study", "Lighting")
            ),
            AdItem(
                id = 9,
                channel = Channel.Local,
                type = AdType.ImageText,
                brandName = "Corner Bakery",
                title = "Evening bakery discount",
                summary = "Fresh bread and desserts with a local pickup offer after work.",
                mediaLabel = "Local offer",
                tags = listOf("Food", "Local", "Deal")
            ),
            AdItem(
                id = 10,
                channel = Channel.Local,
                type = AdType.Video,
                brandName = "Blue Bridge Gym",
                title = "First visit posture assessment",
                summary = "A short AI-assisted movement check with beginner training suggestions.",
                mediaLabel = "Video",
                tags = listOf("Sports", "Local", "Health")
            ),
            AdItem(
                id = 11,
                channel = Channel.Local,
                type = AdType.LargeImage,
                brandName = "Street Cafe",
                title = "Weekday lunch set near the office",
                summary = "Promote a decision-time lunch bundle to users within three kilometers before noon.",
                mediaLabel = "Nearby deal",
                tags = listOf("Dining", "Nearby", "Lunch"),
                liked = true
            ),
            AdItem(
                id = 12,
                channel = Channel.Local,
                type = AdType.SmallImage,
                brandName = "City Cleaners",
                title = "Same-day shirt cleaning pickup",
                summary = "Local service ads emphasize evening pickup, transparent pricing, and first-order savings.",
                mediaLabel = "Service image",
                tags = listOf("Service", "Local", "Pickup")
        )
        )

        return requireUniqueIds(ads)
    }

    private fun requireUniqueIds(ads: List<AdItem>): List<AdItem> {
        val duplicateIds = ads
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys

        require(duplicateIds.isEmpty()) {
            "Ad ids must be unique. Duplicate ids: ${duplicateIds.joinToString()}"
        }

        return ads
    }
}

package com.example.aiadflow

/**
 * 本地广告数据提供器。
 *
 * 这个对象只负责提供固定样例数据，方便在没有后端接口的情况下验证信息流、筛选、
 * 详情页、点赞收藏等交互。后续如果接入真实接口，可以保留 AdItem 数据结构，
 * 将这里替换成 Repository 或网络请求层。
 */
object MockAdProvider {
    /**
     * 返回完整广告列表。
     *
     * 数据按照 channel 分组，MainActivity 会根据当前频道筛选；adType 决定卡片布局，
     * tags 会同时用于展示和搜索匹配。
     */
    fun ads(): List<AdItem> = listOf(
        AdItem(
            id = 1,
            channel = "featured",
            brandName = "青柠数码",
            title = "学生党入门降噪耳机：地铁也能听清细节",
            summary = "中低频更饱满，降噪对人声与车厢噪音有效；预算友好，适合学习与通勤。",
            imageType = "小图",
            adType = "smallImage",
            tags = listOf("数码", "学生党", "省钱"),
            liked = false,
            collected = false
        ),
        AdItem(
            id = 2,
            channel = "featured",
            brandName = "拾光咖啡",
            title = "本周限定「燕麦拿铁」：轻甜不腻，满满幸福感",
            summary = "口感更加顺滑，低糖更友好；适合下午提神与打卡拍照。",
            imageType = "图文",
            adType = "imageText",
            tags = listOf("美食", "本地探店", "潮流"),
            liked = true,
            collected = false
        ),
        AdItem(
            id = 3,
            channel = "featured",
            brandName = "轻跑实验室",
            title = "城市晨跑装备清单：轻量跑鞋与速干衣一站配齐",
            summary = "针对日常 3-5 公里训练推荐，兼顾缓震、透气与易打理，适合通勤前后的运动安排。",
            imageType = "视频",
            adType = "video",
            tags = listOf("运动", "通勤", "潮流"),
            liked = false,
            collected = true
        ),
        AdItem(
            id = 4,
            channel = "featured",
            brandName = "云间书桌",
            title = "宿舍桌面改造：百元内提升学习效率",
            summary = "收纳架、护眼灯与磁吸线夹组合，让小桌面更清爽；适合学生党低预算升级。",
            imageType = "大图",
            adType = "largeImage",
            tags = listOf("学生党", "省钱", "数码"),
            liked = false,
            collected = false
        ),
        AdItem(
            id = 5,
            channel = "ecommerce",
            brandName = "星河手机",
            title = "新款轻薄手机限时补贴：夜景、人像、续航都在线",
            summary = "主打轻薄机身与长续航，影像算法增强夜景细节；适合换机预算有限的年轻用户。",
            imageType = "大图",
            adType = "largeImage",
            tags = listOf("数码", "省钱", "潮流"),
            liked = false,
            collected = false
        ),
        AdItem(
            id = 6,
            channel = "ecommerce",
            brandName = "极简通勤包",
            title = "能装电脑也不臃肿：通勤双肩包今日直降",
            summary = "分区清晰，15 英寸电脑独立保护；面料耐磨防泼水，适合上班、上课和短途出行。",
            imageType = "小图",
            adType = "smallImage",
            tags = listOf("通勤", "学生党", "省钱"),
            liked = false,
            collected = true
        ),
        AdItem(
            id = 7,
            channel = "ecommerce",
            brandName = "元气厨房",
            title = "空气炸锅懒人套餐：少油也能做出酥脆口感",
            summary = "适配早餐、夜宵与轻食场景，预设菜单减少操作成本；适合租房党和新手下厨。",
            imageType = "图文",
            adType = "imageText",
            tags = listOf("美食", "省钱", "本地探店"),
            liked = true,
            collected = false
        ),
        AdItem(
            id = 8,
            channel = "ecommerce",
            brandName = "潮玩衣橱",
            title = "夏日基础款三件套：通勤和周末都能穿",
            summary = "版型利落，颜色低饱和更好搭；组合购价格更友好，适合快速搭出清爽造型。",
            imageType = "视频",
            adType = "video",
            tags = listOf("潮流", "通勤", "省钱"),
            liked = false,
            collected = false
        ),
        AdItem(
            id = 9,
            channel = "local",
            brandName = "巷口面包房",
            title = "晚七点后第二件半价：热卖可颂别错过",
            summary = "黄油香气明显，外酥内软；晚间折扣更适合下班顺路购买或周末探店。",
            imageType = "图文",
            adType = "imageText",
            tags = listOf("美食", "本地探店", "省钱"),
            liked = false,
            collected = false
        ),
        AdItem(
            id = 10,
            channel = "local",
            brandName = "蓝桥健身",
            title = "新客体验课：30 分钟 AI 体态评估",
            summary = "结合基础动作筛查给出训练建议，适合想开始运动但不知道如何入门的人群。",
            imageType = "视频",
            adType = "video",
            tags = listOf("运动", "本地探店", "潮流"),
            liked = false,
            collected = true
        ),
        AdItem(
            id = 11,
            channel = "local",
            brandName = "城南洗衣社",
            title = "通勤衬衫护理套餐：三件起送，次日可取",
            summary = "主打省时和稳定护理效果，适合高频通勤用户；小程序下单可查看进度。",
            imageType = "小图",
            adType = "smallImage",
            tags = listOf("通勤", "本地探店", "省钱"),
            liked = true,
            collected = false
        ),
        AdItem(
            id = 12,
            channel = "local",
            brandName = "周末市集",
            title = "城市露台市集开放：咖啡、手作和落日音乐",
            summary = "集合独立咖啡、手作饰品与轻音乐演出，适合朋友聚会、拍照打卡和周末放松。",
            imageType = "大图",
            adType = "largeImage",
            tags = listOf("美食", "本地探店", "潮流"),
            liked = false,
            collected = false
        )
    )
}

package com.kenyatrip.app.core.knowledge

import com.kenyatrip.app.core.model.Activity
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Edge
import com.kenyatrip.app.core.model.EntryRule
import com.kenyatrip.app.core.model.Lodge
import com.kenyatrip.app.core.model.OriginCity
import com.kenyatrip.app.core.model.Place
import com.kenyatrip.app.core.model.PlaceKind
import com.kenyatrip.app.core.model.Severity
import com.kenyatrip.app.core.model.Slot
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TransportMode
import com.kenyatrip.app.core.model.TravelStyle.ADVENTURE
import com.kenyatrip.app.core.model.TravelStyle.BUSINESS
import com.kenyatrip.app.core.model.TravelStyle.CULTURE
import com.kenyatrip.app.core.model.TravelStyle.FAMILY
import com.kenyatrip.app.core.model.TravelStyle.HONEYMOON
import com.kenyatrip.app.core.model.TravelStyle.PHOTOGRAPHY
import com.kenyatrip.app.core.model.TravelStyle.RELAXED
import com.kenyatrip.app.core.model.TravelStyle.SENIOR

/**
 * Demo knowledge for Kenyan travellers visiting China. Prices are indicative only.
 */
internal object ChinaData {
    private val C = Destination.CHINA
    val CANTON_FAIR_MONTHS = setOf(4, 10)

    val places = listOf(
        Place(
            id = "guangzhou", destination = C, name = Text("广州", "Guangzhou"), kind = PlaceKind.CITY,
            aliases = listOf("广州", "guangzhou", "canton", "广交会"),
            seasonScore = listOf(6, 5, 7, 8, 7, 6, 6, 6, 7, 8, 8, 7),
            affinity = mapOf(BUSINESS to 0.8, CULTURE to 0.2, FAMILY to 0.2),
            minNights = 2, maxNights = 5,
            highlight = Text("广交会、批发市场、早茶", "Canton Fair, wholesale markets, dim sum"),
            hotelPeakMonths = CANTON_FAIR_MONTHS, hotelPeakMultiplier = 1.8,
            internationalAirport = true,
        ),
        Place(
            id = "shenzhen", destination = C, name = Text("深圳", "Shenzhen"), kind = PlaceKind.CITY,
            aliases = listOf("深圳", "shenzhen", "华强北", "huaqiangbei"),
            seasonScore = listOf(7, 6, 7, 8, 7, 6, 6, 6, 7, 8, 8, 7),
            affinity = mapOf(BUSINESS to 0.7, FAMILY to 0.2),
            minNights = 1, maxNights = 3,
            highlight = Text("电子产业、工厂考察", "Electronics hub and factory visits"),
        ),
        Place(
            id = "yiwu", destination = C, name = Text("义乌", "Yiwu"), kind = PlaceKind.CITY,
            aliases = listOf("义乌", "yiwu"),
            seasonScore = listOf(7, 3, 8, 8, 8, 7, 7, 7, 8, 8, 8, 7),
            affinity = mapOf(BUSINESS to 0.9),
            minNights = 1, maxNights = 3,
            highlight = Text("全球最大小商品市场", "World's largest small-commodities market"),
        ),
        Place(
            id = "shanghai", destination = C, name = Text("上海", "Shanghai"), kind = PlaceKind.CITY,
            aliases = listOf("上海", "shanghai"),
            seasonScore = listOf(5, 5, 7, 9, 9, 6, 6, 6, 8, 9, 8, 6),
            affinity = mapOf(HONEYMOON to 0.4, FAMILY to 0.3, CULTURE to 0.3, BUSINESS to 0.3),
            minNights = 2, maxNights = 4,
            highlight = Text("外滩夜景、迪士尼", "The Bund skyline and Disneyland"),
            internationalAirport = true,
        ),
        Place(
            id = "beijing", destination = C, name = Text("北京", "Beijing"), kind = PlaceKind.CITY,
            aliases = listOf("北京", "beijing", "长城", "great wall"),
            seasonScore = listOf(5, 5, 6, 9, 9, 7, 6, 6, 9, 9, 7, 5),
            affinity = mapOf(CULTURE to 0.6, FAMILY to 0.4, SENIOR to 0.3),
            minNights = 2, maxNights = 4,
            highlight = Text("长城、故宫", "Great Wall and Forbidden City"),
            internationalAirport = true,
        ),
        Place(
            id = "guilin", destination = C, name = Text("桂林", "Guilin"), kind = PlaceKind.CITY,
            aliases = listOf("桂林", "阳朔", "guilin", "yangshuo", "li river"),
            seasonScore = listOf(5, 5, 7, 8, 8, 7, 7, 7, 9, 9, 7, 6),
            affinity = mapOf(PHOTOGRAPHY to 0.5, HONEYMOON to 0.5, RELAXED to 0.4, SENIOR to 0.3),
            minNights = 2, maxNights = 3,
            highlight = Text("漓江山水", "Li River karst scenery"),
        ),
        Place(
            id = "zhangjiajie", destination = C, name = Text("张家界", "Zhangjiajie"), kind = PlaceKind.PARK,
            aliases = listOf("张家界", "zhangjiajie", "avatar"),
            seasonScore = listOf(4, 4, 6, 8, 9, 7, 7, 7, 9, 9, 6, 4),
            affinity = mapOf(ADVENTURE to 0.6, PHOTOGRAPHY to 0.5),
            minNights = 2, maxNights = 3,
            highlight = Text("阿凡达山、玻璃桥", "Avatar mountains and glass bridge"),
        ),
    )

    val activities = listOf(
        Activity(
            "canton_fair", "guangzhou", Text("广交会参展采购（含注册与翻译陪同）", "Canton Fair sourcing with registration & interpreter"), 120, 18,
            Slot.FULL_DAY, setOf(BUSINESS), listOf("广交会", "canton fair"), maxDays = 3, months = CANTON_FAIR_MONTHS,
        ),
        Activity(
            "gz_wholesale", "guangzhou", Text("广州批发市场采购导览", "Guangzhou wholesale market tour"), 60, 0,
            Slot.MORNING, setOf(BUSINESS), listOf("批发市场", "wholesale"),
        ),
        Activity(
            "gz_cruise", "guangzhou", Text("珠江夜游", "Pearl River night cruise"), 25, 0,
            Slot.EVENING, setOf(FAMILY, HONEYMOON, CULTURE), listOf("珠江", "pearl river"), default = true,
        ),
        Activity(
            "sz_factory", "shenzhen", Text("电子工厂考察（预约制）", "Electronics factory visit (by appointment)"), 80, 16,
            Slot.MORNING, setOf(BUSINESS), listOf("工厂", "factory"),
        ),
        Activity(
            "sz_hqb", "shenzhen", Text("华强北电子市场", "Huaqiangbei electronics market"), 30, 0,
            Slot.AFTERNOON, setOf(BUSINESS, FAMILY), listOf("华强北", "huaqiangbei", "electronics"), default = true,
        ),
        Activity(
            "yiwu_market", "yiwu", Text("义乌国际商贸城采购向导", "Yiwu International Trade City sourcing guide"), 90, 0,
            Slot.FULL_DAY, setOf(BUSINESS), listOf("商贸城", "trade city", "义乌市场", "yiwu market"), default = true, maxDays = 2,
        ),
        Activity(
            "sh_bund", "shanghai", Text("外滩与黄浦江夜游", "The Bund & Huangpu River cruise"), 30, 0,
            Slot.EVENING, setOf(HONEYMOON, FAMILY, CULTURE), listOf("外滩", "bund"), default = true,
        ),
        Activity(
            "sh_disney", "shanghai", Text("上海迪士尼乐园", "Shanghai Disneyland"), 90, 0,
            Slot.FULL_DAY, setOf(FAMILY), listOf("迪士尼", "disney"),
        ),
        Activity(
            "bj_wall", "beijing", Text("慕田峪长城", "Mutianyu Great Wall"), 50, 0,
            Slot.MORNING, setOf(CULTURE, FAMILY, SENIOR, PHOTOGRAPHY), listOf("长城", "great wall"), default = true,
        ),
        Activity(
            "bj_palace", "beijing", Text("故宫博物院", "Forbidden City"), 20, 0,
            Slot.MORNING, setOf(CULTURE, FAMILY, SENIOR), listOf("故宫", "forbidden city"), default = true,
        ),
        Activity(
            "gl_li_river", "guilin", Text("漓江游船至阳朔", "Li River cruise to Yangshuo"), 70, 0,
            Slot.FULL_DAY, setOf(PHOTOGRAPHY, HONEYMOON, RELAXED, SENIOR, FAMILY), listOf("漓江", "li river"), default = true,
        ),
        Activity(
            "zjj_avatar", "zhangjiajie", Text("张家界国家森林公园", "Zhangjiajie National Forest Park"), 75, 0,
            Slot.FULL_DAY, setOf(ADVENTURE, PHOTOGRAPHY, FAMILY), listOf("阿凡达", "avatar", "forest park"), default = true,
        ),
        Activity(
            "zjj_glass", "zhangjiajie", Text("大峡谷玻璃桥", "Grand Canyon glass bridge"), 40, 6,
            Slot.MORNING, setOf(ADVENTURE), listOf("玻璃桥", "glass bridge"),
        ),
    )

    private fun hotel(id: String, place: String, zh: String, en: String, tier: Tier, price: Int) =
        Lodge(id, place, Text("$zh（示例）", "$en (sample)"), tier, price)

    val lodges = places.flatMap { p ->
        val n = p.name
        listOf(
            hotel("${p.id}_b", p.id, "${n.zh}商务快捷酒店", "${n.en} Express Inn", Tier.BUDGET, 35),
            hotel("${p.id}_c", p.id, "${n.zh}国际酒店", "${n.en} International Hotel", Tier.COMFORT, 65),
            hotel("${p.id}_c2", p.id, "${n.zh}花园酒店", "${n.en} Garden Hotel", Tier.COMFORT, 70),
            hotel("${p.id}_l", p.id, "${n.zh}豪华酒店", "${n.en} Grand Luxury Hotel", Tier.LUXURY, 160),
        )
    }

    val edges = listOf(
        Edge("guangzhou", "shenzhen", TransportMode.TRAIN, 0.5, 12),
        Edge("guangzhou", "guilin", TransportMode.TRAIN, 2.5, 40),
        Edge("guangzhou", "yiwu", TransportMode.FLIGHT, 2.0, 120),
        Edge("guangzhou", "yiwu", TransportMode.TRAIN, 6.5, 75),
        Edge("guangzhou", "shanghai", TransportMode.FLIGHT, 2.3, 130),
        Edge("guangzhou", "shanghai", TransportMode.TRAIN, 7.0, 110),
        Edge("guangzhou", "beijing", TransportMode.FLIGHT, 3.0, 150),
        Edge("guangzhou", "zhangjiajie", TransportMode.TRAIN, 4.5, 60),
        Edge("shenzhen", "shanghai", TransportMode.FLIGHT, 2.3, 130),
        Edge("shenzhen", "beijing", TransportMode.FLIGHT, 3.2, 150),
        Edge("shenzhen", "guilin", TransportMode.TRAIN, 3.0, 45),
        Edge("yiwu", "shanghai", TransportMode.TRAIN, 1.5, 25),
        Edge("shanghai", "beijing", TransportMode.TRAIN, 4.5, 90),
        Edge("shanghai", "guilin", TransportMode.FLIGHT, 2.5, 120),
        Edge("shanghai", "zhangjiajie", TransportMode.FLIGHT, 2.3, 120),
        Edge("beijing", "guilin", TransportMode.FLIGHT, 3.0, 140),
        Edge("beijing", "zhangjiajie", TransportMode.FLIGHT, 2.5, 130),
        Edge("guilin", "zhangjiajie", TransportMode.TRAIN, 4.0, 50),
    )

    val entryRules = listOf(
        EntryRule(
            C, Text(
                "肯尼亚公民赴华需办理签证（旅游 L 签或商务 M 签），请预留 2–3 周准备材料并预约递交",
                "Kenyan citizens need a Chinese visa (L tourist or M business); allow 2–3 weeks for documents and appointment",
            ), Severity.WARNING,
        ),
        EntryRule(
            C, Text(
                "从黄热病流行国家入境中国需出示黄热病疫苗接种证书",
                "A yellow fever vaccination certificate is required when arriving from Kenya",
            ), Severity.WARNING,
        ),
        EntryRule(
            C, Text(
                "出发前开通支付宝/微信支付并绑定国际银行卡，提前下载离线地图与翻译应用",
                "Set up Alipay / WeChat Pay with your international card and download offline maps and a translator before departure",
            ), Severity.INFO,
        ),
    )

    val originCities = listOf(
        OriginCity("NBO", Text("内罗毕", "Nairobi"), listOf("内罗毕", "nairobi")),
        OriginCity("MBA", Text("蒙巴萨", "Mombasa"), listOf("蒙巴萨", "mombasa")),
    )

    val ground = GroundRates(
        supplier = "China inbound ground handler (licensed, sample)",
        vehiclePerDayUsd = 90,
        seatsPerVehicle = 6,
        photoSeatsPerVehicle = 6,
        guidePerDayUsd = 60,
        guideLabel = Text("英语/斯瓦希里语陪同", "English / Swahili-speaking escort"),
        vehicleLabel = Text("专车接送", "Private car & driver"),
    )

    val flights = FlightRates(
        supplier = "Flight aggregator (sample)",
        roundTripUsd = 880,
        peakMonths = CANTON_FAIR_MONTHS,
        peakMultiplier = 1.2,
    )
}

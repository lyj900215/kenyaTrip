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
import com.kenyatrip.app.core.model.TravelStyle.BEACH
import com.kenyatrip.app.core.model.TravelStyle.CULTURE
import com.kenyatrip.app.core.model.TravelStyle.FAMILY
import com.kenyatrip.app.core.model.TravelStyle.HONEYMOON
import com.kenyatrip.app.core.model.TravelStyle.PHOTOGRAPHY
import com.kenyatrip.app.core.model.TravelStyle.RELAXED
import com.kenyatrip.app.core.model.TravelStyle.SENIOR

/**
 * Demo knowledge for Kenya. Prices, fees and drive times are indicative and must be replaced
 * by contracted rates and official tariffs before going live.
 */
internal object KenyaData {
    private val K = Destination.KENYA

    val places = listOf(
        Place(
            id = "nairobi", destination = K, name = Text("内罗毕", "Nairobi"), kind = PlaceKind.GATEWAY,
            aliases = listOf("内罗毕", "nairobi"),
            seasonScore = listOf(6, 6, 6, 5, 5, 6, 6, 6, 6, 6, 5, 6),
            affinity = mapOf(FAMILY to 0.3, CULTURE to 0.3),
            minNights = 1, maxNights = 2,
            highlight = Text("长颈鹿中心、大象孤儿院", "Giraffe Centre, elephant orphanage"),
            internationalAirport = true,
        ),
        Place(
            id = "masai_mara", destination = K, name = Text("马赛马拉", "Masai Mara"), kind = PlaceKind.PARK,
            aliases = listOf("马赛马拉", "马拉", "mara", "masai mara", "大迁徙", "migration"),
            seasonScore = listOf(7, 7, 6, 4, 4, 7, 10, 10, 10, 9, 6, 7),
            affinity = mapOf(PHOTOGRAPHY to 0.6, ADVENTURE to 0.4, HONEYMOON to 0.3, FAMILY to 0.3),
            minNights = 2, maxNights = 4,
            highlight = Text("动物大迁徙、马拉河过河、BIG FIVE", "Great Migration, Mara River crossings, Big Five"),
            parkFeePeakUsd = 200, parkFeeLowUsd = 100, parkPeakMonths = (7..12).toSet(),
        ),
        Place(
            id = "amboseli", destination = K, name = Text("安博塞利", "Amboseli"), kind = PlaceKind.PARK,
            aliases = listOf("安博塞利", "amboseli", "乞力马扎罗", "kilimanjaro"),
            seasonScore = listOf(9, 9, 7, 5, 5, 8, 9, 9, 9, 8, 6, 7),
            affinity = mapOf(PHOTOGRAPHY to 0.5, FAMILY to 0.3, SENIOR to 0.2),
            minNights = 1, maxNights = 3,
            highlight = Text("雪山下的象群", "Elephant herds beneath Kilimanjaro"),
            parkFeePeakUsd = 90, parkFeeLowUsd = 60, parkPeakMonths = setOf(1, 2, 7, 8, 9, 10, 12),
        ),
        Place(
            id = "nakuru", destination = K, name = Text("纳库鲁湖", "Lake Nakuru"), kind = PlaceKind.LAKE,
            aliases = listOf("纳库鲁", "nakuru", "火烈鸟", "flamingo"),
            seasonScore = listOf(7, 7, 7, 6, 6, 7, 7, 7, 7, 7, 7, 7),
            affinity = mapOf(FAMILY to 0.3, PHOTOGRAPHY to 0.3, SENIOR to 0.3),
            minNights = 1, maxNights = 2,
            highlight = Text("火烈鸟、白犀牛", "Flamingos and white rhinos"),
            parkFeePeakUsd = 80, parkFeeLowUsd = 60, parkPeakMonths = setOf(1, 2, 7, 8, 9, 10, 12),
        ),
        Place(
            id = "naivasha", destination = K, name = Text("奈瓦沙湖", "Lake Naivasha"), kind = PlaceKind.LAKE,
            aliases = listOf("奈瓦沙", "naivasha", "新月岛", "crescent island"),
            seasonScore = listOf(7, 7, 7, 5, 5, 7, 7, 7, 7, 7, 6, 7),
            affinity = mapOf(FAMILY to 0.4, RELAXED to 0.4, SENIOR to 0.3, HONEYMOON to 0.2),
            minNights = 1, maxNights = 2,
            highlight = Text("湖上游船、新月岛徒步看长颈鹿", "Boat safari and walking among giraffes"),
        ),
        Place(
            id = "samburu", destination = K, name = Text("桑布鲁", "Samburu"), kind = PlaceKind.PARK,
            aliases = listOf("桑布鲁", "samburu"),
            seasonScore = listOf(8, 8, 7, 5, 5, 8, 8, 8, 8, 7, 5, 7),
            affinity = mapOf(PHOTOGRAPHY to 0.4, ADVENTURE to 0.5),
            minNights = 2, maxNights = 3,
            highlight = Text("北部特有五种动物（细纹斑马、网纹长颈鹿等）", "The Samburu Special Five"),
            parkFeePeakUsd = 70, parkFeeLowUsd = 70,
        ),
        Place(
            id = "diani", destination = K, name = Text("迪亚尼海滩", "Diani Beach"), kind = PlaceKind.BEACH,
            aliases = listOf("迪亚尼", "diani", "海边", "海滩", "蒙巴萨", "mombasa", "beach"),
            seasonScore = listOf(9, 9, 8, 5, 4, 6, 7, 8, 8, 8, 7, 9),
            affinity = mapOf(BEACH to 0.8, HONEYMOON to 0.6, RELAXED to 0.5, FAMILY to 0.3),
            minNights = 2, maxNights = 4,
            highlight = Text("印度洋白沙滩、浮潜", "Indian Ocean white sand and snorkelling"),
        ),
    )

    val activities = listOf(
        Activity(
            "giraffe_centre", "nairobi", Text("长颈鹿中心喂长颈鹿", "Feed giraffes at the Giraffe Centre"), 20, 0,
            Slot.MORNING, setOf(FAMILY), listOf("长颈鹿中心", "giraffe centre"), default = true,
        ),
        Activity(
            "elephant_orphanage", "nairobi", Text("大象孤儿院", "Elephant orphanage visit"), 20, 0,
            Slot.AFTERNOON, setOf(FAMILY, CULTURE), listOf("大象孤儿院", "elephant orphanage"),
        ),
        Activity(
            "mara_balloon", "masai_mara", Text("马赛马拉热气球 + 草原香槟早餐", "Mara hot-air balloon with bush breakfast"), 480, 7,
            Slot.DAWN, setOf(PHOTOGRAPHY, HONEYMOON), listOf("热气球", "balloon"),
        ),
        Activity(
            "maasai_village", "masai_mara", Text("马赛村落文化探访", "Maasai village visit"), 30, 0,
            Slot.AFTERNOON, setOf(CULTURE, FAMILY), listOf("马赛村", "maasai village", "部落"), default = true,
        ),
        Activity(
            "amboseli_hill", "amboseli", Text("观景山远眺乞力马扎罗", "Observation Hill viewpoint"), 0, 0,
            Slot.AFTERNOON, setOf(PHOTOGRAPHY, SENIOR), listOf("观景山", "observation hill"), default = true,
        ),
        Activity(
            "naivasha_boat", "naivasha", Text("奈瓦沙湖游船看河马", "Lake Naivasha boat ride"), 35, 0,
            Slot.MORNING, setOf(FAMILY, RELAXED), listOf("游船", "boat"), default = true,
        ),
        Activity(
            "crescent_walk", "naivasha", Text("新月岛步行游猎", "Crescent Island walking safari"), 40, 5,
            Slot.AFTERNOON, setOf(FAMILY, ADVENTURE, PHOTOGRAPHY), listOf("新月岛", "crescent", "步行"),
        ),
        Activity(
            "diani_snorkel", "diani", Text("基西特海洋公园浮潜看海豚", "Kisite marine park snorkelling & dolphins"), 90, 6,
            Slot.FULL_DAY, setOf(BEACH, FAMILY, HONEYMOON, ADVENTURE), listOf("浮潜", "snorkel", "海豚", "dolphin"),
        ),
    )

    private fun lodge(id: String, place: String, zh: String, en: String, tier: Tier, price: Int, cn: Boolean = false) =
        Lodge(id, place, Text("$zh（示例）", "$en (sample)"), tier, price, cn)

    val lodges = listOf(
        lodge("nbo_b", "nairobi", "城市便捷酒店", "City Express Hotel", Tier.BUDGET, 55),
        lodge("nbo_c", "nairobi", "西区花园酒店", "Westlands Garden Hotel", Tier.COMFORT, 110, cn = true),
        lodge("nbo_c2", "nairobi", "卡伦森林酒店", "Karen Forest Hotel", Tier.COMFORT, 120),
        lodge("nbo_l", "nairobi", "长颈鹿庄园", "Giraffe Manor Estate", Tier.LUXURY, 260),
        lodge("mara_b", "masai_mara", "塔莱克帐篷营地", "Talek Tented Camp", Tier.BUDGET, 150),
        lodge("mara_c", "masai_mara", "马拉河畔营地", "Mara Riverside Camp", Tier.COMFORT, 300, cn = true),
        lodge("mara_c2", "masai_mara", "草原之心营地", "Savanna Heart Camp", Tier.COMFORT, 320),
        lodge("mara_l", "masai_mara", "悬崖豪华营地", "Escarpment Luxury Camp", Tier.LUXURY, 650),
        lodge("amb_b", "amboseli", "雪山脚下营地", "Kili View Camp", Tier.BUDGET, 130),
        lodge("amb_c", "amboseli", "象群生态旅馆", "Elephant Eco Lodge", Tier.COMFORT, 260, cn = true),
        lodge("amb_c2", "amboseli", "沼泽景观营地", "Swamp View Camp", Tier.COMFORT, 270),
        lodge("amb_l", "amboseli", "乞力马扎罗豪华营地", "Kilimanjaro Luxury Camp", Tier.LUXURY, 550),
        lodge("nak_b", "nakuru", "湖畔旅馆", "Lakeside Lodge", Tier.BUDGET, 120),
        lodge("nak_c", "nakuru", "火烈鸟山庄", "Flamingo Hill Lodge", Tier.COMFORT, 220),
        lodge("nak_c2", "nakuru", "犀牛谷酒店", "Rhino Valley Hotel", Tier.COMFORT, 230, cn = true),
        lodge("nak_l", "nakuru", "悬崖湖景营地", "Cliffside Lake Camp", Tier.LUXURY, 450),
        lodge("nai_b", "naivasha", "湖光酒店", "Lake Light Hotel", Tier.BUDGET, 90),
        lodge("nai_c", "naivasha", "纸莎草度假村", "Papyrus Resort", Tier.COMFORT, 170, cn = true),
        lodge("nai_c2", "naivasha", "河马湾旅馆", "Hippo Bay Lodge", Tier.COMFORT, 180),
        lodge("nai_l", "naivasha", "湖心岛别墅", "Island Villa", Tier.LUXURY, 350),
        lodge("sam_b", "samburu", "河岸营地", "Riverbank Camp", Tier.BUDGET, 130),
        lodge("sam_c", "samburu", "北境生态营地", "Northern Eco Camp", Tier.COMFORT, 260),
        lodge("sam_c2", "samburu", "棕榈林营地", "Doum Palm Camp", Tier.COMFORT, 270),
        lodge("sam_l", "samburu", "星空豪华营地", "Starbed Luxury Camp", Tier.LUXURY, 500),
        lodge("dia_b", "diani", "珊瑚海滩酒店", "Coral Beach Hotel", Tier.BUDGET, 70),
        lodge("dia_c", "diani", "印度洋度假村", "Indian Ocean Resort", Tier.COMFORT, 150, cn = true),
        lodge("dia_c2", "diani", "棕榈海岸酒店", "Palm Coast Hotel", Tier.COMFORT, 160),
        lodge("dia_l", "diani", "白沙私人别墅", "White Sands Private Villa", Tier.LUXURY, 350),
    )

    val edges = listOf(
        Edge("nairobi", "masai_mara", TransportMode.DRIVE, 5.5),
        Edge("nairobi", "masai_mara", TransportMode.FLIGHT, 1.0, 250),
        Edge("nairobi", "amboseli", TransportMode.DRIVE, 4.5),
        Edge("nairobi", "amboseli", TransportMode.FLIGHT, 0.8, 220),
        Edge("nairobi", "nakuru", TransportMode.DRIVE, 3.0),
        Edge("nairobi", "naivasha", TransportMode.DRIVE, 2.0),
        Edge("nairobi", "samburu", TransportMode.DRIVE, 6.0),
        Edge("nairobi", "samburu", TransportMode.FLIGHT, 1.2, 260),
        Edge("nairobi", "diani", TransportMode.FLIGHT, 1.2, 150),
        Edge("naivasha", "nakuru", TransportMode.DRIVE, 1.5),
        Edge("naivasha", "masai_mara", TransportMode.DRIVE, 5.0),
        Edge("nakuru", "masai_mara", TransportMode.DRIVE, 5.5),
        Edge("nakuru", "samburu", TransportMode.DRIVE, 6.0),
        Edge("masai_mara", "amboseli", TransportMode.FLIGHT, 1.5, 350),
        Edge("masai_mara", "diani", TransportMode.FLIGHT, 2.0, 330),
        Edge("amboseli", "diani", TransportMode.DRIVE, 6.5),
        Edge("amboseli", "diani", TransportMode.FLIGHT, 1.0, 280),
    )

    val entryRules = listOf(
        EntryRule(
            K, Text(
                "中国公民入境肯尼亚需在出发前办理电子旅行授权（eTA），建议提前至少 7 天申请",
                "Chinese citizens need a Kenya Electronic Travel Authorisation (eTA); apply at least 7 days before departure",
            ), Severity.WARNING,
        ),
        EntryRule(
            K, Text(
                "建议接种黄热病疫苗并携带国际预防接种证书（小黄本），准备防蚊与疟疾预防措施",
                "Yellow fever vaccination (with certificate) and malaria precautions are recommended",
            ), Severity.INFO,
        ),
    )

    val originCities = listOf(
        OriginCity("CAN", Text("广州", "Guangzhou"), listOf("广州", "guangzhou")),
        OriginCity("PEK", Text("北京", "Beijing"), listOf("北京", "beijing")),
        OriginCity("PVG", Text("上海", "Shanghai"), listOf("上海", "shanghai")),
        OriginCity("SZX", Text("深圳", "Shenzhen"), listOf("深圳", "shenzhen")),
        OriginCity("CTU", Text("成都", "Chengdu"), listOf("成都", "chengdu")),
    )

    val ground = GroundRates(
        supplier = "Savanna Safaris Ltd (TRA licensed, sample)",
        vehiclePerDayUsd = 250,
        seatsPerVehicle = 6,
        photoSeatsPerVehicle = 4,
        guidePerDayUsd = 100,
        guideLabel = Text("中文导游", "Chinese-speaking guide"),
        vehicleLabel = Text("开顶 4x4 越野车 + 持证司机向导", "Pop-up roof 4x4 with licensed driver-guide"),
    )

    val flights = FlightRates(
        supplier = "Flight aggregator (sample)",
        roundTripUsd = 950,
        peakMonths = setOf(7, 8, 10),
        peakMultiplier = 1.3,
    )
}

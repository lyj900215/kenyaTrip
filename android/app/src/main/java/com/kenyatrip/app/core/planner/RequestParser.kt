package com.kenyatrip.app.core.planner

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Tier
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.model.TripRequest
import com.kenyatrip.app.core.pricing.Currency
import java.time.LocalDate
import kotlin.math.roundToInt

enum class ParseSource { LOCAL, AI }

enum class Slot { DESTINATION, MONTH, DAYS, TRAVELERS, BUDGET, STYLES, PLACES, TIER }

data class ParseResult(
    val request: TripRequest,
    val recognized: Set<Slot>,
    val source: ParseSource = ParseSource.LOCAL,
)

/**
 * Rule-based extraction of a [TripRequest] from free text in Chinese or English.
 * It is the offline fallback for the LLM parser and also the source of defaults.
 */
class RequestParser(
    private val kb: KnowledgeBase,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    fun parse(input: String, defaultDestination: Destination): ParseResult {
        val text = input.trim()
        val lower = text.lowercase()
        val recognized = mutableSetOf<Slot>()

        val destination = detectDestination(lower)?.also { recognized += Slot.DESTINATION } ?: defaultDestination
        val month = parseMonth(lower)?.also { recognized += Slot.MONTH } ?: defaultMonth()
        val days = parseDays(lower)?.also { recognized += Slot.DAYS }
            ?: if (destination == Destination.KENYA) 8 else 7

        val styles = parseStyles(lower)
        val (adults, childAges, travelersFound) = parseTravelers(lower, styles)
        if (travelersFound) recognized += Slot.TRAVELERS
        val finalStyles = if (childAges.isNotEmpty()) styles + TravelStyle.FAMILY else styles
        if (finalStyles.isNotEmpty()) recognized += Slot.STYLES

        val budget = parseBudget(lower, adults + childAges.size)?.also { recognized += Slot.BUDGET }
        val tier = parseTier(lower)?.also { recognized += Slot.TIER }

        val mustActivities = kb.matchActivities(lower, destination)
        val mustPlaces = kb.matchPlaces(stripOrigin(lower), destination) +
            mustActivities.map { kb.activity(it).placeId }
        if (mustPlaces.isNotEmpty()) recognized += Slot.PLACES

        val request = TripRequest(
            destination = destination,
            month = month,
            days = days.coerceIn(MIN_DAYS, MAX_DAYS),
            adults = adults,
            childAges = childAges,
            budgetPerPersonUsd = budget,
            styles = finalStyles,
            mustPlaces = mustPlaces,
            mustActivities = mustActivities,
            tier = tier,
            originCity = kb.matchOrigin(lower, destination)?.id,
        )
        return ParseResult(request, recognized)
    }

    private fun defaultMonth(): Int = (today().monthValue + 2 - 1) % 12 + 1

    private fun stripOrigin(lower: String): String =
        lower.replace(Regex("从\\S{2,4}?出发"), " ").replace(Regex("from\\s+[a-z]+"), " ")

    private fun detectDestination(lower: String): Destination? {
        val text = stripOrigin(lower)
        val kenya = KENYA_WORDS.count { text.contains(it) }
        val china = CHINA_WORDS.count { text.contains(it) }
        return when {
            kenya > china -> Destination.KENYA
            china > kenya -> Destination.CHINA
            else -> null
        }
    }

    private fun parseMonth(lower: String): Int? {
        Regex("(\\d{1,2})\\s*月").find(lower)?.let { m ->
            m.groupValues[1].toInt().takeIf { it in 1..12 }?.let { return it }
        }
        CN_MONTHS.forEach { (word, month) -> if (lower.contains(word)) return month }
        EN_MONTHS.forEachIndexed { index, name ->
            if (Regex("\\b${name}[a-z]*\\b").containsMatchIn(lower)) return index + 1
        }
        HOLIDAYS.forEach { (word, month) -> if (lower.contains(word)) return month }
        if (lower.contains("广交会") || lower.contains("canton fair")) {
            val now = today().monthValue
            return if (now < 4 || now >= 10) 4 else 10
        }
        return null
    }

    private fun parseDays(lower: String): Int? {
        Regex("(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})\\s*(天|日游|days?)").find(lower)?.let {
            return it.groupValues[2].toInt()
        }
        Regex("(\\d{1,2})\\s*(天|日游)").find(lower)?.let { return it.groupValues[1].toInt() }
        Regex("(\\d{1,2})\\s*(-\\s*)?days?\\b").find(lower)?.let { return it.groupValues[1].toInt() }
        Regex("(\\d{1,2})\\s*(晚|nights?\\b)").find(lower)?.let { return it.groupValues[1].toInt() + 1 }
        Regex("([一二两三四五六七八九十]{1,3})\\s*天").find(lower)?.let { m -> cnNumber(m.groupValues[1])?.let { return it } }
        if (Regex("两周|两个星期|two weeks|2 weeks").containsMatchIn(lower)) return 14
        if (Regex("一周|一个星期|a week|one week").containsMatchIn(lower)) return 7
        return null
    }

    private fun parseStyles(lower: String): Set<TravelStyle> =
        STYLE_WORDS.filter { (_, words) -> words.any { lower.contains(it) } }.keys

    private data class Travelers(val adults: Int, val childAges: List<Int>, val found: Boolean)

    private fun parseTravelers(lower: String, styles: Set<TravelStyle>): Travelers {
        var adults: Int? = null
        var children: Int? = null
        var total: Int? = null

        Regex("一家([三四五六])口").find(lower)?.let {
            val n = cnNumber(it.groupValues[1]) ?: 3
            adults = 2; children = n - 2
        }
        number(lower, "(个大人|大人|位成人|成人|adults?)")?.let { adults = it }
        number(lower, "(个孩子|个小孩|孩子|小孩|kids?\\b|children|child\\b)")?.let { children = it }
        number(lower, "(个人|人(?!均)|位(?!成人)|people|persons|pax|travell?ers|traders)")?.let { total = it }

        val ages = Regex("(\\d{1,2})\\s*岁|(\\d{1,2})[-\\s]year[-\\s]old|aged?\\s*(\\d{1,2})")
            .findAll(lower)
            .mapNotNull { m -> m.groupValues.drop(1).firstOrNull { it.isNotEmpty() }?.toInt() }
            .filter { it < 18 }
            .toMutableList()

        if (children == null && (ages.isNotEmpty() || styles.contains(TravelStyle.FAMILY) && hasChildWord(lower))) {
            children = maxOf(ages.size, 1)
        }
        val childCount = children ?: 0
        if (adults == null) {
            adults = when {
                total != null -> (total!! - childCount).coerceAtLeast(1)
                Regex("情侣|蜜月|两个人|俩人|我和(老婆|老公|妻子|丈夫|女朋友|男朋友)|couple|honeymoon|my (wife|husband|partner)")
                    .containsMatchIn(lower) -> 2
                else -> null
            }
        }
        val found = adults != null || children != null
        while (ages.size < childCount) ages += DEFAULT_CHILD_AGE
        return Travelers(
            adults = (adults ?: 2).coerceIn(1, 20),
            childAges = ages.take(childCount),
            found = found,
        )
    }

    private fun hasChildWord(lower: String) = CHILD_WORDS.any { lower.contains(it) }

    private fun number(lower: String, unitPattern: String): Int? {
        Regex("(\\d{1,2})\\s*$unitPattern").find(lower)?.let { return it.groupValues[1].toInt() }
        Regex("([一二两三四五六七八九十]{1,2})\\s*$unitPattern").find(lower)?.let { return cnNumber(it.groupValues[1]) }
        return null
    }

    private fun parseBudget(lower: String, travelers: Int): Int? {
        val perPerson = Regex("人均|每人|一人|each|per person|\\bpp\\b|/person|a head").containsMatchIn(lower)
        val explicitTotal = Regex("总预算|一共|总共|in total|total|altogether").containsMatchIn(lower)
        val people = travelers.coerceAtLeast(1)

        fun split(usd: Int, defaultPerPerson: Boolean): Int =
            if (explicitTotal || (!perPerson && !defaultPerPerson)) usd / people else usd

        Regex("(\\d+(?:\\.\\d+)?)\\s*万").find(lower)?.let {
            val cny = it.groupValues[1].toDouble() * 10_000
            return split(toUsd(cny, Currency.CNY), defaultPerPerson = false)
        }
        Regex("(\\d{4,6})\\s*(元|块|rmb|cny)").find(lower)?.let {
            return split(toUsd(it.groupValues[1].toDouble(), Currency.CNY), defaultPerPerson = true)
        }
        Regex("(?:ksh|kes)\\s*(\\d[\\d,]*)\\s*(k)?|(\\d[\\d,]*)\\s*(k)?\\s*(?:ksh|kes|shillings|肯尼亚先令|肯先令|先令)").find(lower)?.let { m ->
            val raw = (m.groupValues[1].ifEmpty { m.groupValues[3] }).replace(",", "").toDouble()
            val k = m.groupValues[2].isNotEmpty() || m.groupValues[4].isNotEmpty()
            return split(toUsd(if (k) raw * 1000 else raw, Currency.KES), defaultPerPerson = true)
        }
        Regex("\\$\\s*(\\d[\\d,]*)\\s*(k)?|(\\d[\\d,]*)\\s*(k)?\\s*(?:usd|dollars|美元|美金)").find(lower)?.let { m ->
            val raw = (m.groupValues[1].ifEmpty { m.groupValues[3] }).replace(",", "").toDouble()
            val k = m.groupValues[2].isNotEmpty() || m.groupValues[4].isNotEmpty()
            return split((if (k) raw * 1000 else raw).roundToInt(), defaultPerPerson = true)
        }
        return null
    }

    private fun toUsd(amount: Double, currency: Currency) = (amount / currency.perUsd).roundToInt()

    private fun parseTier(lower: String): Tier? = when {
        Regex("豪华|奢华|高端|五星|luxury|high-end|5-star|five star").containsMatchIn(lower) -> Tier.LUXURY
        Regex("经济|省钱|便宜|穷游|budget|cheap|affordable").containsMatchIn(lower) -> Tier.BUDGET
        Regex("舒适|中档|comfort|mid-range").containsMatchIn(lower) -> Tier.COMFORT
        else -> null
    }

    companion object {
        const val MIN_DAYS = 3
        const val MAX_DAYS = 16
        const val DEFAULT_CHILD_AGE = 8

        private val KENYA_WORDS = listOf(
            "肯尼亚", "kenya", "safari", "马赛马拉", "大迁徙", "migration", "看动物", "动物迁徙",
            "安博塞利", "amboseli", "纳库鲁", "nakuru", "迪亚尼", "diani", "非洲", "africa", "热气球",
        )
        private val CHINA_WORDS = listOf(
            "中国", "china", "广交会", "canton", "广州", "guangzhou", "北京", "beijing", "上海", "shanghai",
            "义乌", "yiwu", "桂林", "guilin", "深圳", "shenzhen", "张家界", "zhangjiajie", "长城", "great wall",
        )
        private val CN_MONTHS = listOf(
            "十二月" to 12, "十一月" to 11, "十月" to 10, "九月" to 9, "八月" to 8, "七月" to 7,
            "六月" to 6, "五月" to 5, "四月" to 4, "三月" to 3, "二月" to 2, "一月" to 1,
        )
        private val EN_MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        private val HOLIDAYS = listOf(
            "暑假" to 7, "国庆" to 10, "春节" to 2, "寒假" to 1, "五一" to 5, "summer holiday" to 8, "christmas" to 12,
        )
        private val CHILD_WORDS = listOf("孩子", "小孩", "儿子", "女儿", "宝宝", "kid", "child", "daughter")

        private val STYLE_WORDS: Map<TravelStyle, List<String>> = mapOf(
            TravelStyle.FAMILY to listOf("亲子", "孩子", "小孩", "儿子", "女儿", "一家", "family", "kids", "children", "child"),
            TravelStyle.PHOTOGRAPHY to listOf("摄影", "拍照", "拍摄", "长焦", "photo"),
            TravelStyle.HONEYMOON to listOf("蜜月", "情侣", "结婚", "周年", "honeymoon", "couple", "romantic"),
            TravelStyle.SENIOR to listOf("父母", "爸妈", "老人", "长辈", "退休", "parents", "senior", "elderly", "retired"),
            TravelStyle.BUSINESS to listOf(
                "广交会", "采购", "商务", "工厂", "批发", "进货", "canton fair", "sourcing", "business", "trade", "factory",
                "wholesale", "traders", "supplier",
            ),
            TravelStyle.ADVENTURE to listOf("探险", "徒步", "刺激", "adventure", "hiking"),
            TravelStyle.BEACH to listOf("海边", "海滩", "海岛", "浮潜", "beach", "snorkel"),
            TravelStyle.CULTURE to listOf("文化", "历史", "部落", "人文", "culture", "history", "heritage"),
            TravelStyle.RELAXED to listOf("轻松", "不累", "慢游", "休闲", "relax", "easy pace", "slow"),
        )

        private val CN_DIGITS = mapOf(
            '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
        )

        fun cnNumber(s: String): Int? {
            if (s.isEmpty()) return null
            val tenIndex = s.indexOf('十')
            if (tenIndex < 0) return if (s.length == 1) CN_DIGITS[s[0]] else null
            val tens = if (tenIndex == 0) 1 else CN_DIGITS[s[0]] ?: return null
            val ones = if (tenIndex == s.length - 1) 0 else CN_DIGITS[s[tenIndex + 1]] ?: return null
            return tens * 10 + ones
        }
    }
}

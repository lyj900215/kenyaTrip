package com.kenyatrip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kenyatrip.app.core.model.Lang
import com.kenyatrip.app.core.model.Text
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocalLang = staticCompositionLocalOf { Lang.ZH }

@Composable
@ReadOnlyComposable
fun t(text: Text): String = text(LocalLang.current)

@Composable
@ReadOnlyComposable
fun t(zh: String, en: String): String = if (LocalLang.current == Lang.ZH) zh else en

object S {
    val appTitle = Text("AI 中非旅行规划师", "AI China–Africa Trip Planner")
    val appSubtitle = Text("说出想法，AI 规划可执行行程，一键预订机酒车", "Say what you want. AI plans a bookable trip. One tap books it all.")
    val tabPlan = Text("规划", "Plan")
    val tabTrips = Text("我的行程", "My trips")
    val tabSettings = Text("设置", "Settings")
    val toKenya = Text("去肯尼亚", "Visit Kenya")
    val toKenyaSub = Text("动物大迁徙 · Safari · 海滩", "Migration · Safari · Beach")
    val toChina = Text("去中国", "Visit China")
    val toChinaSub = Text("广交会 · 采购 · 观光", "Canton Fair · Sourcing · Sightseeing")
    val inputHintKenya = Text("例如：8月带孩子看动物大迁徙，8天，人均2万", "e.g. Migration safari in August with kids, 8 days")
    val inputHintChina = Text("例如：10月广交会，7天，3位采购商，每人2500美元", "e.g. Canton Fair in October, 7 days, 3 traders, \$2500 each")
    val planWithAi = Text("AI 规划", "Plan with AI")
    val tryThese = Text("试试这样说", "Try saying")
    val howItWorks = Text("怎么运作", "How it works")
    val step1 = Text("说出想法", "Describe it")
    val step2 = Text("AI 生成可行路线", "AI builds feasible routes")
    val step3 = Text("一键预订机酒车", "Book flights, stays & cars")
    val demoNotice = Text(
        "演示版本：价格、营地与供应商均为示例数据，预订通过模拟供应商完成，不会真实扣款。",
        "Demo build: prices, lodges and suppliers are sample data; bookings use simulated suppliers and no real payment is taken.",
    )
    val reviewTitle = Text("确认需求", "Confirm your trip")
    val parsedByAi = Text("由大模型解析", "Parsed by LLM")
    val parsedLocal = Text("本地智能解析", "Parsed on device")
    val recognized = Text("已识别", "Recognised")
    val destination = Text("目的地", "Destination")
    val month = Text("出行月份", "Month")
    val days = Text("天数", "Days")
    val adults = Text("成人", "Adults")
    val children = Text("儿童", "Children")
    val childAge = Text("岁", "yrs")
    val budget = Text("人均预算", "Budget per person")
    val budgetHint = Text("留空则按舒适档推荐", "Leave empty for comfort tier")
    val tier = Text("住宿档次", "Stay level")
    val auto = Text("自动", "Auto")
    val styles = Text("旅行风格", "Travel style")
    val mustPlaces = Text("必去目的地", "Must-visit")
    val mustActivities = Text("想体验", "Experiences")
    val generate = Text("生成 3 套方案", "Generate 3 options")
    val planning = Text("AI 正在规划路线、核验车程与实时价格…", "AI is planning routes, checking drive times and live prices…")
    val recommendationsTitle = Text("为你推荐", "Recommended for you")
    val bestMatch = Text("最佳匹配", "Best match")
    val option = Text("方案", "Option")
    val perPerson = Text("人均", "per person")
    val total = Text("总价", "Total")
    val viewItinerary = Text("查看行程", "View itinerary")
    val noRoutes = Text("没有找到满足条件的路线，试试增加天数或减少必去地点。", "No feasible route. Try more days or fewer must-visit places.")
    val night = Text("晚", "n")
    val tips = Text("条提醒", "notes")
    val itineraryTitle = Text("行程详情", "Itinerary")
    val travelersUnit = Text("人", "travellers")
    val aiAdjust = Text("让 AI 调整行程", "Ask AI to adjust")
    val aiAdjustHint = Text("例如：轻松一点 / 加热气球 / 不去安博塞利", "e.g. more relaxed / add balloon / skip Amboseli")
    val whyThisRoute = Text("推荐理由", "Why this route")
    val notices = Text("出行提醒", "Before you go")
    val dayPlan = Text("每日安排", "Day by day")
    val day = Text("第 %d 天", "Day %d")
    val priceBreakdown = Text("费用明细", "Price breakdown")
    val bookNow = Text("一键预订", "Book in one tap")
    val checkoutTitle = Text("确认订单", "Checkout")
    val leadTraveler = Text("联系人姓名（与护照一致）", "Lead traveller (as on passport)")
    val passport = Text("护照号", "Passport number")
    val phone = Text("手机号", "Phone")
    val paymentMethod = Text("支付方式", "Payment method")
    val refundPolicies = Text("退改规则", "Cancellation terms")
    val agree = Text("我已阅读并同意各项产品的退改规则", "I agree to the cancellation terms of each product")
    val payAndBook = Text("支付并预订", "Pay & book")
    val oneCharge = Text("一次付款，系统自动向航司、营地、车队下单；任一项失败将自动替换或全额退款。",
        "One payment. We book airlines, lodges and drivers for you; if anything fails we substitute or refund in full.")
    val bookingTitle = Text("正在预订", "Booking")
    val stepReprice = Text("实时核价", "Live repricing")
    val stepHold = Text("锁定库存", "Holding inventory")
    val stepPay = Text("支付", "Payment")
    val stepConfirm = Text("向供应商确认", "Supplier confirmation")
    val stepDone = Text("完成", "Done")
    val booked = Text("预订成功！", "You're booked!")
    val bookedSub = Text("所有项目已确认，出行准备清单已生成。", "Everything is confirmed and your prep checklist is ready.")
    val refunded = Text("已取消并全额退款", "Cancelled and fully refunded")
    val failedNoCharge = Text("库存不足，未扣款", "Unavailable — nothing was charged")
    val paymentFailed = Text("支付未成功", "Payment failed")
    val priceChanged = Text("价格有变动", "The price changed")
    val priceChangedBody = Text("供应商实时价格已更新，新的总价为 %s，是否继续预订？", "Live prices moved. The new total is %s. Continue?")
    val continueBooking = Text("继续预订", "Continue")
    val cancel = Text("取消", "Cancel")
    val viewTrip = Text("查看我的行程", "View my trip")
    val backHome = Text("返回首页", "Back to home")
    val retry = Text("返回修改", "Go back")
    val charged = Text("实付", "Charged")
    val refundedAmount = Text("已退款", "Refunded")
    val platformCovered = Text("平台承担差价", "Covered by us")
    val noTrips = Text("还没有行程，去规划第一次旅行吧", "No trips yet — plan your first one")
    val startPlanning = Text("开始规划", "Start planning")
    val bookingRef = Text("确认号", "Ref")
    val checklist = Text("出行准备清单", "Prep checklist")
    val bookings = Text("预订项目", "Bookings")
    val status = Text("状态", "Status")
    val language = Text("语言", "Language")
    val aiSection = Text("大模型接入（可选）", "LLM connection (optional)")
    val aiExplain = Text(
        "兼容 OpenAI 接口格式。面向中国用户请使用已备案的国产大模型（如通义千问、DeepSeek）。正式版本将通过服务端 LLM 网关调用，密钥不保存在手机上。",
        "Any OpenAI-compatible endpoint. Use a registered domestic model for users in China. Production builds call a server-side LLM gateway so keys never live on the device.",
    )
    val aiEnabled = Text("使用大模型解析需求", "Use the LLM to parse requests")
    val baseUrl = Text("接口地址 Base URL", "Base URL")
    val apiKey = Text("API Key", "API key")
    val model = Text("模型名称", "Model")
    val testConnection = Text("测试连接", "Test connection")
    val connectionOk = Text("连接成功", "Connected")
    val connectionFail = Text("连接失败，将使用本地解析", "Failed — on-device parsing will be used")
    val demoSection = Text("演示：模拟供应商异常", "Demo: simulate supplier failures")
    val demoSoldOut = Text("营地确认时满房（演示自动替换）", "Lodge sold out at confirmation (auto-substitute)")
    val demoNoAlt = Text("无可替代产品（演示自动退款）", "No substitutes (auto-refund)")
    val demoDecline = Text("支付被拒绝", "Payment declined")
    val about = Text("关于", "About")
    val langAuto = Text("跟随系统", "System")
}

fun formatDate(date: LocalDate, lang: Lang): String = if (lang == Lang.ZH) {
    date.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.SIMPLIFIED_CHINESE))
} else {
    date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))
}

fun formatDateRange(start: LocalDate, end: LocalDate, lang: Lang): String = if (lang == Lang.ZH) {
    "${start.year}年" + start.format(DateTimeFormatter.ofPattern("M月d日")) + " – " + end.format(DateTimeFormatter.ofPattern("M月d日"))
} else {
    start.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)) + " – " +
        end.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
}

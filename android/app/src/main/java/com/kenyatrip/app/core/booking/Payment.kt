package com.kenyatrip.app.core.booking

import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Text
import kotlinx.coroutines.delay

enum class PaymentMethod(val label: Text, val destination: Destination) {
    WECHAT_PAY(Text("微信支付", "WeChat Pay"), Destination.KENYA),
    ALIPAY(Text("支付宝", "Alipay"), Destination.KENYA),
    UNIONPAY(Text("银联卡", "UnionPay card"), Destination.KENYA),
    MPESA(Text("M-Pesa", "M-Pesa"), Destination.CHINA),
    CARD(Text("Visa / Mastercard", "Visa / Mastercard"), Destination.CHINA);

    companion object {
        fun forDestination(destination: Destination) = entries.filter { it.destination == destination }
    }
}

sealed interface PaymentResult {
    data class Paid(val reference: String) : PaymentResult
    data class Declined(val reason: Text) : PaymentResult
}

interface PaymentGateway {
    suspend fun charge(amountUsd: Int, method: PaymentMethod, idempotencyKey: String): PaymentResult
    suspend fun refund(reference: String, amountUsd: Int): Boolean
}

class MockPaymentGateway(
    private val decline: Boolean = false,
    private val latencyMs: Long = 0,
) : PaymentGateway {
    private val charges = mutableMapOf<String, Int>()
    private val byKey = mutableMapOf<String, String>()
    val refunds = mutableMapOf<String, Int>()

    val totalCharged: Int get() = charges.values.sum()
    val totalRefunded: Int get() = refunds.values.sum()

    override suspend fun charge(amountUsd: Int, method: PaymentMethod, idempotencyKey: String): PaymentResult {
        delay(latencyMs)
        byKey[idempotencyKey]?.let { return PaymentResult.Paid(it) }
        if (decline) return PaymentResult.Declined(Text("支付被拒绝，请更换支付方式", "Payment declined, please try another method"))
        val ref = "PAY-${method.name}-${charges.size + 1}"
        charges[ref] = amountUsd
        byKey[idempotencyKey] = ref
        return PaymentResult.Paid(ref)
    }

    override suspend fun refund(reference: String, amountUsd: Int): Boolean {
        delay(latencyMs)
        val charged = charges[reference] ?: return false
        val already = refunds[reference] ?: 0
        if (already + amountUsd > charged) return false
        refunds[reference] = already + amountUsd
        return true
    }
}

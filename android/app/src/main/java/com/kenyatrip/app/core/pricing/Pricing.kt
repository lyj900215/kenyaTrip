package com.kenyatrip.app.core.pricing

import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Destination
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

enum class Currency(val symbol: String, val perUsd: Double) {
    USD("$", 1.0),
    CNY("¥", 7.2),
    KES("KSh ", 129.0);

    companion object {
        /** Chinese travellers see CNY; Kenyan travellers see KES. */
        fun forDestination(destination: Destination) =
            if (destination == Destination.KENYA) CNY else KES
    }
}

object Money {
    fun convert(usd: Int, currency: Currency): Long = (usd * currency.perUsd).roundToInt().toLong()

    fun format(usd: Int, currency: Currency): String {
        val amount = convert(usd, currency)
        return currency.symbol + NumberFormat.getIntegerInstance(Locale.US).format(amount)
    }

    fun toUsd(amount: Double, currency: Currency): Int = (amount / currency.perUsd).roundToInt()
}

/**
 * Turns supplier net cost into the traveler price. Flights carry a fixed per-ticket service
 * fee; everything else uses a percentage markup that reflects how hard the inventory is to source.
 */
class PricingEngine(
    private val flightServiceFeeUsd: Int = 15,
    private val markups: Map<ComponentType, Double> = mapOf(
        ComponentType.FLIGHT to 0.0,
        ComponentType.DOMESTIC_TRANSPORT to 0.08,
        ComponentType.STAY to 0.12,
        ComponentType.GROUND to 0.20,
        ComponentType.PARK_FEES to 0.03,
        ComponentType.ACTIVITY to 0.15,
    ),
) {
    fun price(type: ComponentType, costUsd: Int, tickets: Int = 0): Int {
        val markup = markups[type] ?: 0.0
        val fee = if (type == ComponentType.FLIGHT) flightServiceFeeUsd * tickets else 0
        return (costUsd * (1 + markup)).roundToInt() + fee
    }
}

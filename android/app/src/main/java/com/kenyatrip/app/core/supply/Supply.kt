package com.kenyatrip.app.core.supply

import com.kenyatrip.app.core.knowledge.KnowledgeBase
import com.kenyatrip.app.core.model.BookableComponent
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.pricing.PricingEngine
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class Quote(val costUsd: Int, val priceUsd: Int, val available: Boolean)

sealed interface HoldResult {
    data class Held(val holdId: String) : HoldResult
    data class Unavailable(val reason: Text) : HoldResult
}

sealed interface ConfirmResult {
    data class Confirmed(val reference: String) : ConfirmResult
    data class Failed(val reason: Text) : ConfirmResult
}

/**
 * Uniform contract for every supplier (flight aggregator, hotel wholesaler, local operator).
 * [confirm] must be idempotent on [idempotencyKey].
 */
interface SupplierAdapter {
    suspend fun quote(component: BookableComponent): Quote
    suspend fun hold(component: BookableComponent, idempotencyKey: String): HoldResult
    suspend fun confirm(component: BookableComponent, holdId: String, paymentRef: String, idempotencyKey: String): ConfirmResult
    suspend fun release(holdId: String)
    suspend fun cancel(reference: String): Boolean
    suspend fun alternatives(component: BookableComponent): List<BookableComponent>
}

interface SupplyNetwork {
    fun adapterFor(type: ComponentType): SupplierAdapter
}

/** Knobs to rehearse failure paths in demos and tests. */
data class MockScenario(
    val latencyMs: Long = 0,
    val priceDriftPct: Double = 0.0,
    val soldOutOnConfirm: Set<ComponentType> = emptySet(),
    val unavailableOnHold: Set<ComponentType> = emptySet(),
    val noAlternatives: Boolean = false,
)

class MockSupplyNetwork(
    private val kb: KnowledgeBase,
    private val pricing: PricingEngine,
    private val scenario: MockScenario = MockScenario(),
) : SupplyNetwork {
    private val adapters = ComponentType.entries.associateWith { MockAdapter(it) }

    val confirmCalls: Map<ComponentType, Int> get() = adapters.mapValues { it.value.confirmCalls }
    val activeConfirmations: Int get() = adapters.values.sumOf { it.confirmed.size }
    val activeHolds: Int get() = adapters.values.sumOf { it.holds.size }

    override fun adapterFor(type: ComponentType): SupplierAdapter = adapters.getValue(type)

    private inner class MockAdapter(private val type: ComponentType) : SupplierAdapter {
        val holds = mutableMapOf<String, String>()
        val confirmed = mutableMapOf<String, String>()
        private val byKey = mutableMapOf<String, String>()
        private val soldOut = mutableSetOf<String>()
        private var failedOnce = false
        private var counter = 0
        private var refCounter = 0
        var confirmCalls = 0

        override suspend fun quote(component: BookableComponent): Quote {
            delay(scenario.latencyMs)
            val cost = (component.costUsd * (1 + scenario.priceDriftPct)).roundToInt()
            val tickets = if (type == ComponentType.FLIGHT) component.quantity else 0
            return Quote(cost, pricing.price(type, cost, tickets), component.refId !in soldOut)
        }

        override suspend fun hold(component: BookableComponent, idempotencyKey: String): HoldResult {
            delay(scenario.latencyMs)
            if (component.refId in soldOut || (type in scenario.unavailableOnHold && !component.id.endsWith(ALT_SUFFIX))) {
                soldOut += component.refId
                return HoldResult.Unavailable(Text("库存不足", "No availability"))
            }
            val id = "H-${type.name.take(3)}-${++counter}"
            holds[id] = component.refId
            return HoldResult.Held(id)
        }

        override suspend fun confirm(
            component: BookableComponent,
            holdId: String,
            paymentRef: String,
            idempotencyKey: String,
        ): ConfirmResult {
            delay(scenario.latencyMs)
            confirmCalls++
            byKey[idempotencyKey]?.let { return ConfirmResult.Confirmed(it) }
            if (type in scenario.soldOutOnConfirm && !failedOnce) {
                failedOnce = true
                soldOut += component.refId
                holds.remove(holdId)
                return ConfirmResult.Failed(Text("供应商确认时已满房/满座", "Supplier sold out at confirmation"))
            }
            holds.remove(holdId) ?: return ConfirmResult.Failed(Text("预留已失效", "Hold expired"))
            val ref = "${type.name.take(2)}${100000 + (++refCounter * 7919) % 900000}"
            byKey[idempotencyKey] = ref
            confirmed[ref] = component.refId
            return ConfirmResult.Confirmed(ref)
        }

        override suspend fun release(holdId: String) {
            holds.remove(holdId)
        }

        override suspend fun cancel(reference: String): Boolean {
            delay(scenario.latencyMs)
            return confirmed.remove(reference) != null
        }

        override suspend fun alternatives(component: BookableComponent): List<BookableComponent> {
            if (scenario.noAlternatives) return emptyList()
            return when (type) {
                ComponentType.STAY -> stayAlternatives(component)
                ComponentType.GROUND -> listOf(
                    component.copy(
                        id = component.id + ALT_SUFFIX,
                        refId = component.refId + "-backup",
                        supplier = "Backup TRA-licensed operator (sample)",
                    ),
                )
                else -> emptyList()
            }.filter { it.refId !in soldOut }
        }

        private fun stayAlternatives(component: BookableComponent): List<BookableComponent> {
            val original = kb.lodge(component.refId)
            val sameTier = kb.lodgesAt(original.placeId, original.tier).filter { it.id != original.id }
            val upgrade = original.tier.up()?.let { kb.lodgesAt(original.placeId, it) }.orEmpty()
            return (sameTier + upgrade).map { lodge ->
                val cost = (component.costUsd * lodge.pricePerNightUsd.toDouble() / original.pricePerNightUsd).roundToInt()
                component.copy(
                    id = component.id + ALT_SUFFIX,
                    refId = lodge.id,
                    supplier = lodge.name.en,
                    title = Text(
                        "${lodge.name.zh} · ${component.units} 晚",
                        "${lodge.name.en} · ${component.units} night(s)",
                    ),
                    costUsd = cost,
                    priceUsd = pricing.price(ComponentType.STAY, cost),
                    tier = lodge.tier,
                )
            }
        }
    }

    companion object {
        const val ALT_SUFFIX = "-alt"
    }
}

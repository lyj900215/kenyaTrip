package com.kenyatrip.app.booking

import com.kenyatrip.app.TestFixtures
import com.kenyatrip.app.core.booking.BookingEvent
import com.kenyatrip.app.core.booking.BookingOrchestrator
import com.kenyatrip.app.core.booking.BookingStatus
import com.kenyatrip.app.core.booking.BookingStep
import com.kenyatrip.app.core.booking.LegStatus
import com.kenyatrip.app.core.booking.MockPaymentGateway
import com.kenyatrip.app.core.booking.PaymentMethod
import com.kenyatrip.app.core.model.BookableComponent
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.TravelStyle
import com.kenyatrip.app.core.model.TripRequest
import com.kenyatrip.app.core.supply.ConfirmResult
import com.kenyatrip.app.core.supply.HoldResult
import com.kenyatrip.app.core.supply.MockScenario
import com.kenyatrip.app.core.supply.MockSupplyNetwork
import com.kenyatrip.app.core.supply.SupplierAdapter
import com.kenyatrip.app.core.supply.SupplyNetwork
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingOrchestratorTest {
    private val itinerary = TestFixtures.planner().recommend(
        TripRequest(
            destination = Destination.KENYA, month = 8, days = 8, adults = 2,
            styles = setOf(TravelStyle.PHOTOGRAPHY), mustActivities = setOf("mara_balloon"), mustPlaces = setOf("masai_mara"),
        ),
    ).first()

    private fun network(scenario: MockScenario = MockScenario()) =
        MockSupplyNetwork(TestFixtures.kb, TestFixtures.pricing, scenario)

    @Test
    fun `happy path books every leg with one charge`() = runTest {
        val supply = network()
        val pay = MockPaymentGateway()
        val steps = mutableListOf<BookingStep>()
        val result = BookingOrchestrator(supply, pay).book(itinerary, PaymentMethod.WECHAT_PAY) { e ->
            if (e is BookingEvent.StepStarted) steps += e.step
        }
        assertEquals(BookingStatus.BOOKED, result.status)
        assertTrue(result.legs.all { it.status == LegStatus.CONFIRMED && it.reference != null })
        assertEquals(itinerary.totalPriceUsd, result.chargedUsd)
        assertEquals(itinerary.totalPriceUsd, pay.totalCharged)
        assertEquals(0, supply.activeHolds)
        assertEquals(itinerary.components.size, supply.activeConfirmations)
        assertEquals(BookingStep.entries, steps)
    }

    @Test
    fun `sold out stay is substituted automatically`() = runTest {
        val supply = network(MockScenario(soldOutOnConfirm = setOf(ComponentType.STAY)))
        val pay = MockPaymentGateway()
        val result = BookingOrchestrator(supply, pay).book(itinerary, PaymentMethod.ALIPAY)
        assertEquals(BookingStatus.BOOKED, result.status)
        val sub = result.legs.single { it.status == LegStatus.SUBSTITUTED }
        assertEquals(ComponentType.STAY, sub.component.type)
        assertTrue(sub.replaced!!.refId != sub.component.refId)
        assertTrue(result.notes.isNotEmpty())
        assertEquals(pay.totalRefunded, result.refundedUsd)
    }

    @Test
    fun `no substitute compensates and refunds everything`() = runTest {
        val supply = network(MockScenario(soldOutOnConfirm = setOf(ComponentType.FLIGHT), noAlternatives = true))
        val pay = MockPaymentGateway()
        val result = BookingOrchestrator(supply, pay).book(itinerary, PaymentMethod.WECHAT_PAY)
        assertEquals(BookingStatus.REFUNDED, result.status)
        assertEquals(result.chargedUsd, result.refundedUsd)
        assertEquals(pay.totalCharged, pay.totalRefunded)
        assertEquals(0, supply.activeConfirmations)
        assertEquals(0, supply.activeHolds)
        assertTrue(result.legs.any { it.status == LegStatus.CANCELLED })
    }

    @Test
    fun `unavailable at hold with no substitute charges nothing`() = runTest {
        val supply = network(MockScenario(unavailableOnHold = setOf(ComponentType.ACTIVITY), noAlternatives = true))
        val pay = MockPaymentGateway()
        val result = BookingOrchestrator(supply, pay).book(itinerary, PaymentMethod.WECHAT_PAY)
        assertEquals(BookingStatus.FAILED_NO_CHARGE, result.status)
        assertEquals(0, pay.totalCharged)
        assertEquals(0, supply.activeHolds)
    }

    @Test
    fun `declined payment releases holds`() = runTest {
        val supply = network()
        val result = BookingOrchestrator(supply, MockPaymentGateway(decline = true)).book(itinerary, PaymentMethod.MPESA)
        assertEquals(BookingStatus.PAYMENT_FAILED, result.status)
        assertEquals(0, supply.activeHolds)
        assertEquals(0, supply.activeConfirmations)
    }

    @Test
    fun `price drift needs confirmation`() = runTest {
        val scenario = MockScenario(priceDriftPct = 0.05)
        val first = BookingOrchestrator(network(scenario), MockPaymentGateway()).book(itinerary, PaymentMethod.ALIPAY)
        assertEquals(BookingStatus.PRICE_CHANGED, first.status)
        assertTrue(first.quotedTotalUsd > itinerary.totalPriceUsd)
        val second = BookingOrchestrator(network(scenario), MockPaymentGateway())
            .book(itinerary, PaymentMethod.ALIPAY, acceptPriceChange = true)
        assertEquals(BookingStatus.BOOKED, second.status)
        assertEquals(first.quotedTotalUsd, second.chargedUsd)
    }

    @Test
    fun `stays are confirmed before flights and activities`() = runTest {
        val order = mutableListOf<ComponentType>()
        val inner = network()
        val recording = object : SupplyNetwork {
            override fun adapterFor(type: ComponentType): SupplierAdapter {
                val delegate = inner.adapterFor(type)
                return object : SupplierAdapter by delegate {
                    override suspend fun confirm(component: BookableComponent, holdId: String, paymentRef: String, idempotencyKey: String): ConfirmResult {
                        order += type
                        return delegate.confirm(component, holdId, paymentRef, idempotencyKey)
                    }
                }
            }
        }
        BookingOrchestrator(recording, MockPaymentGateway()).book(itinerary, PaymentMethod.WECHAT_PAY)
        assertEquals(ComponentType.STAY, order.first())
        assertTrue(order.indexOf(ComponentType.FLIGHT) > order.lastIndexOf(ComponentType.STAY))
        assertEquals(ComponentType.ACTIVITY, order.last())
    }

    @Test
    fun `confirm is idempotent per key`() = runTest {
        val adapter = network().adapterFor(ComponentType.GROUND)
        val component = itinerary.components.first { it.type == ComponentType.GROUND }
        val hold = adapter.hold(component, "k1") as HoldResult.Held
        val a = adapter.confirm(component, hold.holdId, "PAY-1", "k1") as ConfirmResult.Confirmed
        val b = adapter.confirm(component, hold.holdId, "PAY-1", "k1") as ConfirmResult.Confirmed
        assertEquals(a.reference, b.reference)
    }
}

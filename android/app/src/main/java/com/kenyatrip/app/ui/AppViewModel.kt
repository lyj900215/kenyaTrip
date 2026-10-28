package com.kenyatrip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenyatrip.app.KenyaTripApp
import com.kenyatrip.app.core.booking.BookingEvent
import com.kenyatrip.app.core.booking.BookingLeg
import com.kenyatrip.app.core.booking.BookingOrchestrator
import com.kenyatrip.app.core.booking.BookingResult
import com.kenyatrip.app.core.booking.BookingStatus
import com.kenyatrip.app.core.booking.BookingStep
import com.kenyatrip.app.core.booking.MockPaymentGateway
import com.kenyatrip.app.core.booking.PaymentMethod
import com.kenyatrip.app.core.model.ComponentType
import com.kenyatrip.app.core.model.Destination
import com.kenyatrip.app.core.model.Itinerary
import com.kenyatrip.app.core.model.Text
import com.kenyatrip.app.core.model.TripRequest
import com.kenyatrip.app.core.planner.ModifyOutcome
import com.kenyatrip.app.core.planner.ParseSource
import com.kenyatrip.app.core.planner.Slot
import com.kenyatrip.app.core.supply.MockScenario
import com.kenyatrip.app.core.supply.MockSupplyNetwork
import com.kenyatrip.app.data.AppSettings
import com.kenyatrip.app.data.TripRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewState(val request: TripRequest, val recognized: Set<Slot>, val source: ParseSource)

data class BookingUi(
    val step: BookingStep? = null,
    val legs: List<BookingLeg> = emptyList(),
    val notes: List<Text> = emptyList(),
    val result: BookingResult? = null,
    val running: Boolean = false,
    val method: PaymentMethod? = null,
    val leadTraveler: String = "",
)

enum class LangSetting { AUTO, ZH, EN }

data class UiState(
    val destination: Destination = Destination.KENYA,
    val input: String = "",
    val understanding: Boolean = false,
    val review: ReviewState? = null,
    val planning: Boolean = false,
    val recommendations: List<Itinerary> = emptyList(),
    val selected: Itinerary? = null,
    val modifying: Boolean = false,
    val modifyFeedback: Text? = null,
    val booking: BookingUi? = null,
    val trips: List<TripRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val langSetting: LangSetting = LangSetting.AUTO,
    val aiTest: Boolean? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as KenyaTripApp).container
    private val prefs = app.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        UiState(
            settings = c.settingsStore.load(),
            langSetting = LangSetting.valueOf(prefs.getString("lang", LangSetting.AUTO.name)!!),
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()
    val kb get() = c.kb

    init {
        viewModelScope.launch { _state.update { it.copy(trips = c.tripStore.load()) } }
    }

    fun setLang(setting: LangSetting) {
        prefs.edit().putString("lang", setting.name).apply()
        _state.update { it.copy(langSetting = setting) }
    }

    fun setDestination(d: Destination) = _state.update { it.copy(destination = d) }
    fun setInput(text: String) = _state.update { it.copy(input = text) }

    fun understand(onDone: () -> Unit) {
        val s = _state.value
        _state.update { it.copy(understanding = true) }
        viewModelScope.launch {
            val parsed = c.planner.understand(s.input, s.destination)
            _state.update {
                it.copy(
                    understanding = false,
                    destination = parsed.request.destination,
                    review = ReviewState(parsed.request, parsed.recognized, parsed.source),
                )
            }
            onDone()
        }
    }

    fun editRequest(transform: (TripRequest) -> TripRequest) = _state.update { s ->
        val review = s.review ?: return@update s
        s.copy(review = review.copy(request = transform(review.request)))
    }

    fun plan(onDone: () -> Unit) {
        val req = _state.value.review?.request ?: return
        _state.update { it.copy(planning = true) }
        viewModelScope.launch {
            val results = withContext(Dispatchers.Default) { c.planner.recommend(req) }
            _state.update { it.copy(planning = false, recommendations = results) }
            onDone()
        }
    }

    fun select(itinerary: Itinerary) = _state.update { it.copy(selected = itinerary, modifyFeedback = null) }

    fun modify(text: String) {
        val current = _state.value.selected ?: return
        if (text.isBlank()) return
        _state.update { it.copy(modifying = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.Default) { c.planner.modify(current, text) }
            _state.update { s ->
                when (outcome) {
                    is ModifyOutcome.Updated -> s.copy(
                        modifying = false,
                        selected = outcome.itinerary,
                        modifyFeedback = Text(
                            "已更新：" + outcome.changes.joinToString("；") { it.zh },
                            "Updated: " + outcome.changes.joinToString("; ") { it.en },
                        ),
                    )
                    is ModifyOutcome.NotUnderstood -> s.copy(modifying = false, modifyFeedback = outcome.hint)
                    is ModifyOutcome.NoFeasibleRoute -> s.copy(
                        modifying = false,
                        modifyFeedback = Text("这样调整后找不到可行路线，已保留原方案", "No feasible route after that change; kept the current plan"),
                    )
                }
            }
        }
    }

    fun startBooking(method: PaymentMethod, leadTraveler: String, acceptPriceChange: Boolean = false) {
        val itinerary = _state.value.selected ?: return
        val settings = _state.value.settings
        val scenario = MockScenario(
            latencyMs = 350,
            soldOutOnConfirm = if (settings.demoSoldOut) setOf(ComponentType.STAY) else emptySet(),
            noAlternatives = settings.demoNoAlternatives,
        )
        val orchestrator = BookingOrchestrator(
            MockSupplyNetwork(c.kb, c.pricing, scenario),
            MockPaymentGateway(decline = settings.demoPaymentDecline, latencyMs = 600),
        )
        _state.update { it.copy(booking = BookingUi(running = true, method = method, leadTraveler = leadTraveler)) }
        viewModelScope.launch {
            val result = orchestrator.book(itinerary, method, acceptPriceChange) { event ->
                _state.update { s ->
                    val b = s.booking ?: BookingUi()
                    s.copy(
                        booking = when (event) {
                            is BookingEvent.StepStarted -> b.copy(step = event.step)
                            is BookingEvent.LegsUpdated -> b.copy(legs = event.legs)
                            is BookingEvent.Note -> b.copy(notes = b.notes + event.text)
                        },
                    )
                }
            }
            if (result.status == BookingStatus.BOOKED || result.status == BookingStatus.REFUNDED) {
                val record = TripRecord.from(itinerary, result, method.name, leadTraveler, c.kb, System.currentTimeMillis())
                val trips = listOf(record) + _state.value.trips
                c.tripStore.save(trips)
                _state.update { it.copy(trips = trips) }
            }
            _state.update { s -> s.copy(booking = s.booking?.copy(running = false, result = result, legs = result.legs)) }
        }
    }

    fun toggleChecklist(bookingId: String, itemId: String) {
        val trips = _state.value.trips.map { t ->
            if (t.bookingId != bookingId) t
            else t.copy(checklist = t.checklist.map { if (it.id == itemId) it.copy(done = !it.done) else it })
        }
        _state.update { it.copy(trips = trips) }
        viewModelScope.launch { c.tripStore.save(trips) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_state.value.settings)
        c.settingsStore.save(updated)
        _state.update { it.copy(settings = updated, aiTest = null) }
    }

    fun testAi() {
        val cfg = _state.value.settings.copy(aiEnabled = true).llmConfig ?: run {
            _state.update { it.copy(aiTest = false) }
            return
        }
        viewModelScope.launch {
            val ok = c.llmParser.ping(cfg)
            _state.update { it.copy(aiTest = ok) }
        }
    }
}

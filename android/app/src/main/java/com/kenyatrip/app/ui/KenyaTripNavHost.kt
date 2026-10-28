package com.kenyatrip.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kenyatrip.app.core.model.Lang
import com.kenyatrip.app.ui.screens.BookingScreen
import com.kenyatrip.app.ui.screens.CheckoutScreen
import com.kenyatrip.app.ui.screens.HomeScreen
import com.kenyatrip.app.ui.screens.ItineraryScreen
import com.kenyatrip.app.ui.screens.RecommendationsScreen
import com.kenyatrip.app.ui.screens.ReviewScreen
import com.kenyatrip.app.ui.screens.SettingsScreen
import com.kenyatrip.app.ui.screens.TripDetailScreen
import com.kenyatrip.app.ui.screens.TripsScreen

private object Routes {
    const val HOME = "home"
    const val REVIEW = "review"
    const val RECOMMENDATIONS = "recommendations"
    const val ITINERARY = "itinerary"
    const val CHECKOUT = "checkout"
    const val BOOKING = "booking"
    const val TRIPS = "trips"
    const val TRIP = "trip/{id}"
    const val SETTINGS = "settings"
    val topLevel = setOf(HOME, TRIPS, SETTINGS)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KenyaTripNavHost(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val systemLang = if (LocalConfiguration.current.locales[0].language == "zh") Lang.ZH else Lang.EN
    val lang = when (state.langSetting) {
        LangSetting.AUTO -> systemLang
        LangSetting.ZH -> Lang.ZH
        LangSetting.EN -> Lang.EN
    }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.HOME

    CompositionLocalProvider(LocalLang provides lang) {
        Scaffold(
            topBar = {
                if (route != Routes.HOME) {
                    TopAppBar(
                        title = { Text(titleFor(route)) },
                        navigationIcon = {
                            if (route !in Routes.topLevel) {
                                IconButton(onClick = { nav.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    )
                }
            },
            bottomBar = {
                if (route in Routes.topLevel) {
                    NavigationBar {
                        listOf(
                            Triple(Routes.HOME, S.tabPlan, Icons.Filled.AutoAwesome),
                            Triple(Routes.TRIPS, S.tabTrips, Icons.Filled.Luggage),
                            Triple(Routes.SETTINGS, S.tabSettings, Icons.Filled.Settings),
                        ).forEach { (r, label, icon) ->
                            NavigationBarItem(
                                selected = route == r,
                                onClick = { nav.switchTab(r) },
                                icon = { Icon(icon, null) },
                                label = { Text(t(label)) },
                                modifier = Modifier.testTag("tab-$r"),
                            )
                        }
                    }
                }
            },
        ) { inner ->
            NavHost(nav, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreenHost(vm, state, inner) { nav.navigate(Routes.REVIEW) }
                }
                composable(Routes.REVIEW) {
                    state.review?.let { review ->
                        ReviewScreen(
                            review = review,
                            kb = vm.kb,
                            planning = state.planning,
                            onEdit = vm::editRequest,
                            onGenerate = { vm.plan { nav.navigate(Routes.RECOMMENDATIONS) } },
                            padding = inner,
                        )
                    }
                }
                composable(Routes.RECOMMENDATIONS) {
                    RecommendationsScreen(state.recommendations, vm.kb, onOpen = {
                        vm.select(it)
                        nav.navigate(Routes.ITINERARY)
                    }, padding = inner)
                }
                composable(Routes.ITINERARY) {
                    state.selected?.let { itn ->
                        ItineraryScreen(
                            itinerary = itn,
                            kb = vm.kb,
                            modifying = state.modifying,
                            feedback = state.modifyFeedback,
                            onModify = vm::modify,
                            onBook = { nav.navigate(Routes.CHECKOUT) },
                            padding = inner,
                        )
                    }
                }
                composable(Routes.CHECKOUT) {
                    state.selected?.let { itn ->
                        CheckoutScreen(itn, onPay = { method, name ->
                            vm.startBooking(method, name)
                            nav.navigate(Routes.BOOKING)
                        }, padding = inner)
                    }
                }
                composable(Routes.BOOKING) {
                    val itn = state.selected
                    val booking = state.booking
                    if (itn != null && booking != null) {
                        BookingScreen(
                            itinerary = itn,
                            booking = booking,
                            onAcceptPriceChange = {
                                vm.startBooking(booking.method!!, booking.leadTraveler, acceptPriceChange = true)
                            },
                            onBack = { nav.popBackStack(Routes.ITINERARY, inclusive = false) },
                            onViewTrip = { id ->
                                nav.navigate("trip/$id") { popUpTo(Routes.HOME) }
                            },
                            onHome = { nav.switchTab(Routes.HOME) },
                            padding = inner,
                        )
                    }
                }
                composable(Routes.TRIPS) {
                    TripsScreen(state.trips, onOpen = { nav.navigate("trip/$it") }, onPlan = { nav.switchTab(Routes.HOME) }, padding = inner)
                }
                composable(Routes.TRIP) { backStack ->
                    val id = backStack.arguments?.getString("id")
                    state.trips.firstOrNull { it.bookingId == id }?.let { trip ->
                        TripDetailScreen(trip, onToggle = { vm.toggleChecklist(trip.bookingId, it) }, padding = inner)
                    }
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settings = state.settings,
                        lang = state.langSetting,
                        aiTest = state.aiTest,
                        onLang = vm::setLang,
                        onUpdate = vm::updateSettings,
                        onTestAi = vm::testAi,
                        padding = inner,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreenHost(vm: AppViewModel, state: UiState, inner: PaddingValues, onReview: () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.padding(bottom = inner.calculateBottomPadding())) {
        HomeScreen(
            state = state,
            onDestination = vm::setDestination,
            onInput = vm::setInput,
            onPlan = { vm.understand(onReview) },
        )
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun titleFor(route: String): String = when (route) {
    Routes.REVIEW -> t(S.reviewTitle)
    Routes.RECOMMENDATIONS -> t(S.recommendationsTitle)
    Routes.ITINERARY -> t(S.itineraryTitle)
    Routes.CHECKOUT -> t(S.checkoutTitle)
    Routes.BOOKING -> t(S.bookingTitle)
    Routes.TRIPS -> t(S.tabTrips)
    Routes.TRIP -> t(S.tabTrips)
    Routes.SETTINGS -> t(S.tabSettings)
    else -> ""
}

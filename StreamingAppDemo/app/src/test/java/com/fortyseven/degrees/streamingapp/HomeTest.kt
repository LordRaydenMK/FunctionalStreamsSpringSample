package com.fortyseven.degrees.streamingapp

import arrow.core.extensions.either.foldable.firstOrNone
import com.fortyseven.degrees.streamingapp.home.*
import io.reactivex.Observable
import org.junit.Rule
import org.junit.Test

class HomeTest {

    @Rule
    @JvmField
    val rule: AndroidMainSchedulerRule = AndroidMainSchedulerRule()

    val empty_interactions = TestHomeInteractions(Observable.empty())

    fun home(
        interactions: HomeInteractions,
        viewModel: RxViewModel<HomeVM>
    ): HomeDependency =
        HomeDependency.create(
            interactions,
            MockRepository(),
            MockPersistence(Observable.empty()),
            viewModel
        )

    @Test
    fun Empty_Screen_Automatically_Refreshes_Data() {
        val empty = TestRxViewModel<HomeVM>(HomeVM.Empty)

        home(empty_interactions, empty)
            .program()
            .flatMap { empty.state() }
            .test()
            .awaitCount(3)
            .assertValueAt(0, HomeVM.Empty)
            .assertValueAt(1, HomeVM.Loading)
            .assertValueAt(2) { it is HomeVM.Full }
            .assertNotTerminated()
    }

    @Test
    fun Loaded_Screen_Does_Nothing() {
        val empty = TestRxViewModel<HomeVM>(HomeVM.Empty)

        empty.post(HomeVM.Full(emptyList()))
            .flatMap {
                eitherPar( // Run program & listen to state in parallel
                    home(empty_interactions, empty).program(),
                    empty.state()
                ).filterMap { it.firstOrNone() } // Ignore program output
            }
            .test()
            .awaitCount(2)
            .assertValueAt(0, HomeVM.Empty)
            .assertValueAt(1) { it is HomeVM.Full }
            .assertNotTerminated()
    }
}

fun TestHomeInteractions(refresh: Observable<Unit>): HomeInteractions =
    object : HomeInteractions {
        override fun pullToRefresh(): Observable<Unit> = refresh
    }
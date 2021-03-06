package com.fortyseven.degrees.streamingapp

import arrow.core.extensions.either.foldable.firstOrNone
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
        viewModel: RxViewModel<HomeViewState>
    ): HomeDependencies =
        HomeDependencies.create(
            interactions,
            MockRepository(),
            MockPersistence(Observable.empty()),
            viewModel
        )

    @Test
    fun `Empty screen automatically refreshes data`() {
        val empty = TestRxViewModel<HomeViewState>(HomeViewState.Idle)

        home(empty_interactions, empty)
            .program()
            .flatMap { empty.state() }
            .test()
            .awaitCount(3)
            .assertValueAt(0, HomeViewState.Idle)
            .assertValueAt(1, HomeViewState.Loading)
            .assertValueAt(2) { it is HomeViewState.Full }
            .assertNotTerminated()
    }

    @Test
    fun `Loaded screen does nothing`() {
        val empty = TestRxViewModel<HomeViewState>(HomeViewState.Idle)

        empty.post(HomeViewState.Full(emptyList()))
            .flatMap {
                parallelEither( // Run program & listen to state in parallel
                    home(empty_interactions, empty).program(),
                    empty.state()
                ).filterMap { it.firstOrNone() } // Ignore program output
            }
            .test()
            .awaitCount(2)
            .assertValueAt(0, HomeViewState.Idle)
            .assertValueAt(1) { it is HomeViewState.Full }
            .assertNotTerminated()
    }
}

fun TestHomeInteractions(refresh: Observable<Unit>): HomeInteractions =
    object : HomeInteractions {
        override fun pullToRefresh(): Observable<Unit> = refresh
    }
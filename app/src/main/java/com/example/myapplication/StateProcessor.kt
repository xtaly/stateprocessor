package com.example.myapplication

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.internal.disposables.DisposableContainer
import io.reactivex.processors.PublishProcessor
import org.reactivestreams.Subscriber
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class StateProcessor<State>(
    initialState: State,
    internalDisposableContainer: DisposableContainer
) : Flowable<State>() {
    companion object {
        fun <State> create(initialState: State) = StateProcessor(initialState, CompositeDisposable())
    }

    private val stateLock = ReentrantReadWriteLock()

    private val changeStateEvents = PublishProcessor.create<ChangeStateEvent<State>>()

    var currentValue: State = initialState

    private val statesProcessor = PublishProcessor.create<State>()

    init {
        internalDisposableContainer.add(
            changeStateEvents
                .flatMapCompletable { event ->
                    Completable
                        .fromAction {
                            stateLock.write {
                                val newState = event.action(currentValue)
                                currentValue = newState

                                when (event.changeType) {
                                    ChangeType.USUAL -> statesProcessor.onNext(newState)
                                    ChangeType.LAZY -> Unit
                                }
                            }
                        }
                        .onErrorComplete()
                }
                .subscribe()
        )
    }

    override fun subscribeActual(subscriber: Subscriber<in State>) {
        statesProcessor
            .startWith(currentValue)
            .subscribe(subscriber)
    }

    fun change(action: (currentState: State) -> State) =
        changeStateEvents.onNext(ChangeStateEvent(ChangeType.USUAL, action))

    fun changeLazy(action: (currentState: State) -> State) =
        changeStateEvents.onNext(ChangeStateEvent(ChangeType.LAZY, action))

    data class ChangeStateEvent<State>(
        val changeType: ChangeType,
        val action: (currentState: State) -> State
    )

    enum class ChangeType {
        USUAL,
        LAZY
    }
}
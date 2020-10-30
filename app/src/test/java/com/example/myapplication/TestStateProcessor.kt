package com.example.myapplication

import org.junit.Test

class TestStateProcessor {
    @Test
    fun change_checkResult() {
        val state = StateProcessor.create(0)
        val testSubscriber = state.test()

        for (i in 0..99) {
            state.change { it + 1 }
        }

        testSubscriber.assertValueSequence(0..100)
        assert(state.currentValue == 100)
    }

    @Test
    fun change_checkResultForMultiThread() {
        val state = StateProcessor.create(0)
        val testSubscriber = state.test()

        val threads = List(100) {
            Thread {
                state.change { it + 1 }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        testSubscriber.assertValueSequence(0..100)
        assert(state.currentValue == 100)
    }

    @Test
    fun changeLazy_checkResultValue() {
        val state = StateProcessor.create(0)
        val testSubscriber = state.test()

        for (i in 0..99) {
            state.changeLazy { it + 1 }
        }

        testSubscriber.assertValue(0)
        testSubscriber.assertValueCount(1)
        assert(state.currentValue == 100)
    }

    @Test
    fun changeLazy_checkChangeUsualAfterLazy() {
        val state = StateProcessor.create(0)
        val testSubscriber = state.test()

        for (i in 0..99) {
            state.changeLazy { it + 1 }
        }
        state.change { it + 1 }

        testSubscriber.assertValueSequence(listOf(0, 101))
        testSubscriber.assertValueCount(2)
        assert(state.currentValue == 101)
    }
}
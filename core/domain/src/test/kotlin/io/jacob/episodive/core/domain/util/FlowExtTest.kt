package io.jacob.episodive.core.domain.util

import app.cash.turbine.test
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FlowExtTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun combine5Test() = runTest {
        val combined = combine(
            flowOf("a"),
            flowOf("b"),
            flowOf("c"),
            flowOf("d"),
            flowOf("e"),
        ) { a, b, c, d, e ->
            a + b + c + d + e
        }
        combined.test {
            assertEquals("abcde", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun combine6Test() = runTest {
        val combined = combine(
            flowOf("a"),
            flowOf("b"),
            flowOf("c"),
            flowOf("d"),
            flowOf("e"),
            flowOf("f"),
        ) { a, b, c, d, e, f ->
            a + b + c + d + e + f
        }
        combined.test {
            assertEquals("abcdef", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun combine7Test() = runTest {
        val combined = combine(
            flowOf("a"),
            flowOf("b"),
            flowOf("c"),
            flowOf("d"),
            flowOf("e"),
            flowOf("f"),
            flowOf("g"),
        ) { a, b, c, d, e, f, g ->
            a + b + c + d + e + f + g
        }
        combined.test {
            assertEquals("abcdefg", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun combine8Test() = runTest {
        val combined = combine(
            flowOf("a"),
            flowOf("b"),
            flowOf("c"),
            flowOf("d"),
            flowOf("e"),
            flowOf("f"),
            flowOf("g"),
            flowOf("h"),
        ) { a, b, c, d, e, f, g, h ->
            a + b + c + d + e + f + g + h
        }
        combined.test {
            assertEquals("abcdefgh", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun combine9Test() = runTest {
        val combined = combine(
            flowOf("a"),
            flowOf("b"),
            flowOf("c"),
            flowOf("d"),
            flowOf("e"),
            flowOf("f"),
            flowOf("g"),
            flowOf("h"),
            flowOf("i"),
        ) { a, b, c, d, e, f, g, h, i ->
            a + b + c + d + e + f + g + h + i
        }
        combined.test {
            assertEquals("abcdefghi", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun combine10Test() = runTest {
        val combined = combine(
            flowOf("a"),
            flowOf("b"),
            flowOf("c"),
            flowOf("d"),
            flowOf("e"),
            flowOf("f"),
            flowOf("g"),
            flowOf("h"),
            flowOf("i"),
            flowOf("j"),
        ) { a, b, c, d, e, f, g, h, i, j ->
            a + b + c + d + e + f + g + h + i + j
        }
        combined.test {
            assertEquals("abcdefghij", awaitItem())
            awaitComplete()
        }
    }
}
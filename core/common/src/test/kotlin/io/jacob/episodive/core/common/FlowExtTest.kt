package io.jacob.episodive.core.common

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class FlowExtTest {
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
            Assert.assertEquals("abcde", awaitItem())
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
            Assert.assertEquals("abcdef", awaitItem())
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
            Assert.assertEquals("abcdefg", awaitItem())
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
            Assert.assertEquals("abcdefgh", awaitItem())
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
            Assert.assertEquals("abcdefghi", awaitItem())
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
            Assert.assertEquals("abcdefghij", awaitItem())
            awaitComplete()
        }
    }
}
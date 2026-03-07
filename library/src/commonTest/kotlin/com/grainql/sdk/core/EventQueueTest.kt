package com.grainql.sdk.core

import com.grainql.sdk.model.GrainEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EventQueueTest {

    private var counter = 0
    private fun event(id: String = "id-${++counter}") = GrainEvent(
        id = id,
        eventName = "test",
        userId = "user",
        properties = emptyMap(),
        systemProperties = emptyMap(),
        timestamp = 0L,
    )

    @Test
    fun enqueueAndPeek() = runTest {
        val queue = EventQueue(100)
        queue.enqueue(event("1"))
        queue.enqueue(event("2"))

        assertEquals(2, queue.size)
        val peeked = queue.peek(10)
        assertEquals(2, peeked.size)
        assertEquals(2, queue.size) // peek doesn't remove
    }

    @Test
    fun removeById() = runTest {
        val queue = EventQueue(100)
        queue.enqueue(event("a"))
        queue.enqueue(event("b"))
        queue.enqueue(event("c"))

        queue.remove(setOf("a", "c"))
        assertEquals(1, queue.size)
        assertEquals("b", queue.peek(1).first().id)
    }

    @Test
    fun enqueueReturnsNullWhenNoOverflow() = runTest {
        val queue = EventQueue(10)
        val evicted = queue.enqueue(event("1"))
        assertNull(evicted)
    }

    @Test
    fun overflowDropsOldestAndReturnsEvictedId() = runTest {
        val queue = EventQueue(2)
        assertNull(queue.enqueue(event("1")))
        assertNull(queue.enqueue(event("2")))
        val evicted = queue.enqueue(event("3"))
        assertNotNull(evicted)
        assertEquals("1", evicted)

        assertEquals(2, queue.size)
        val events = queue.peek(2)
        assertEquals("2", events[0].id)
        assertEquals("3", events[1].id)
    }

    @Test
    fun incrementRetryCount() = runTest {
        val queue = EventQueue(100)
        queue.enqueue(event("r1"))
        queue.enqueue(event("r2"))

        queue.incrementRetryCount(setOf("r1"))
        val events = queue.peek(2)
        assertEquals(1, events.first { it.id == "r1" }.retryCount)
        assertEquals(0, events.first { it.id == "r2" }.retryCount)

        queue.incrementRetryCount(setOf("r1"))
        val updated = queue.peek(2)
        assertEquals(2, updated.first { it.id == "r1" }.retryCount)
    }

    @Test
    fun drain() = runTest {
        val queue = EventQueue(100)
        queue.enqueue(event("x"))
        queue.enqueue(event("y"))

        val drained = queue.drain()
        assertEquals(2, drained.size)
        assertEquals(0, queue.size)
    }
}

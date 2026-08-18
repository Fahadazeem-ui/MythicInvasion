package io.github.mindzard.mythicinvasion.application.behaviour

import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourEvent
import java.util.concurrent.ConcurrentLinkedQueue

class BehaviourEventBuffer {

    private val queue = ConcurrentLinkedQueue<BehaviourEvent>()

    fun add(event: BehaviourEvent) {
        queue.offer(event)
    }

    fun drain(maxEvents: Int): List<BehaviourEvent> {
        if (maxEvents <= 0) {
            return emptyList()
        }

        val events = ArrayList<BehaviourEvent>(
            minOf(maxEvents, queue.size)
        )

        repeat(maxEvents) {
            val event = queue.poll() ?: return@repeat
            events.add(event)
        }

        return events
    }

    fun size(): Int {
        return queue.size
    }

    fun clear() {
        queue.clear()
    }
}

package io.github.mindzard.mythicinvasion.bootstrap

import kotlin.reflect.KClass

class ServiceRegistry {

    private val services: MutableMap<KClass<*>, Any> = mutableMapOf()

    fun <T : Any> register(
        type: KClass<T>,
        service: T
    ) {
        check(!services.containsKey(type)) {
            "A service of type ${type.qualifiedName} is already registered."
        }

        services[type] = service
    }

    inline fun <reified T : Any> register(service: T) {
        register(T::class, service)
    }

    fun <T : Any> get(type: KClass<T>): T {
        val service = services[type]
            ?: error("No service registered for ${type.qualifiedName}.")

        @Suppress("UNCHECKED_CAST")
        return service as T
    }

    inline fun <reified T : Any> get(): T {
        return get(T::class)
    }

    fun <T : Any> contains(type: KClass<T>): Boolean {
        return services.containsKey(type)
    }

    inline fun <reified T : Any> contains(): Boolean {
        return contains(T::class)
    }

    fun clear() {
        services.clear()
    }

    fun size(): Int {
        return services.size
    }
}

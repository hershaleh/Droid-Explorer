package com.droidexplorer.core.capabilities

/**
 * An operation that a storage provider or engine may support.
 * The UI shows an action only if the active provider declares the matching capability.
 */
enum class Capability {
    READ,
    WRITE,
    CREATE,
    RENAME,
    MOVE,
    COPY,
    DELETE_PERMANENT,
    TRASH,
    RESTORE,
    LIST_CHILDREN,
    PAUSE_RESUME,
    HASH_SERVER_SIDE,
    CHANGE_PERMISSIONS,
    ROOT_ACCESS,
}

/** Immutable set of capabilities declared by one provider or engine. */
class CapabilitySet private constructor(private val items: Set<Capability>) {

    fun supports(capability: Capability): Boolean = capability in items

    fun supportsAll(vararg capabilities: Capability): Boolean = capabilities.all { it in items }

    fun toSet(): Set<Capability> = items

    operator fun plus(other: CapabilitySet): CapabilitySet = CapabilitySet(items + other.items)

    override fun equals(other: Any?): Boolean = other is CapabilitySet && other.items == items

    override fun hashCode(): Int = items.hashCode()

    override fun toString(): String = "CapabilitySet($items)"

    companion object {
        val EMPTY = CapabilitySet(emptySet())

        fun of(vararg capabilities: Capability): CapabilitySet = CapabilitySet(capabilities.toSet())
    }
}

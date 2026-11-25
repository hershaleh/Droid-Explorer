package com.droidexplorer.core.capabilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitySetTest {

    @Test
    fun emptySetSupportsNothing() {
        assertFalse(CapabilitySet.EMPTY.supports(Capability.READ))
    }

    @Test
    fun declaredCapabilitiesAreSupportedAndOthersAreNot() {
        val set = CapabilitySet.of(Capability.READ, Capability.COPY)
        assertTrue(set.supports(Capability.READ))
        assertTrue(set.supports(Capability.COPY))
        assertFalse(set.supports(Capability.TRASH))
    }

    @Test
    fun supportsAllRequiresEveryCapability() {
        val set = CapabilitySet.of(Capability.READ, Capability.WRITE)
        assertTrue(set.supportsAll(Capability.READ, Capability.WRITE))
        assertFalse(set.supportsAll(Capability.READ, Capability.DELETE_PERMANENT))
    }

    @Test
    fun plusMergesSets() {
        val merged = CapabilitySet.of(Capability.READ) + CapabilitySet.of(Capability.RENAME)
        assertEquals(setOf(Capability.READ, Capability.RENAME), merged.toSet())
    }
}

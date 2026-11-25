package com.droidexplorer.core.capabilities

/**
 * Maturity of a feature, as defined in the product specification (section 20).
 * A level may be claimed only with evidence for it.
 */
enum class FeatureStatus {
    /** Included in the product vision only. */
    APPROVED,

    /** Proven technically possible on a specific device and Android version. */
    SUPPORTED,

    /** Exists in code. */
    IMPLEMENTED,

    /** Passed its defined tests. */
    TESTED,
}

/** State of root access, determined by a real check and not by detecting an installed tool. */
enum class RootState {
    AVAILABLE,
    DENIED,
    REVOKED,
    UNAVAILABLE,
    LIMITED,
}

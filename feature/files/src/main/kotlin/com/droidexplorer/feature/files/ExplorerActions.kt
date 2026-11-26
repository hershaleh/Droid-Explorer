package com.droidexplorer.feature.files

/** Everything the Explorer screen can ask its host to do. Defaults make the screen usable as a static preview. */
data class ExplorerActions(
    val canBack: Boolean = false,
    val canForward: Boolean = false,
    val canUp: Boolean = false,
    val onBack: () -> Unit = {},
    val onForward: () -> Unit = {},
    val onUp: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onOpen: (ExplorerItem) -> Unit = {},
    val onToggleSelect: (ExplorerItem) -> Unit = {},
    val onCrumb: (Int) -> Unit = {},
)

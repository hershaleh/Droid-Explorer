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
    val editablePath: String? = null,
    val onPathSubmit: (String) -> Unit = {},
    val onMenu: (() -> Unit)? = null,
    val canPaste: Boolean = false,
    val sortKey: com.droidexplorer.storage.api.SortKey = com.droidexplorer.storage.api.SortKey.NAME,
    val sortAscending: Boolean = true,
    val onNewFolder: () -> Unit = {},
    val onCut: () -> Unit = {},
    val onCopy: () -> Unit = {},
    val onPaste: () -> Unit = {},
    val onRename: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onSort: (com.droidexplorer.storage.api.SortKey, Boolean) -> Unit = { _, _ -> },
)

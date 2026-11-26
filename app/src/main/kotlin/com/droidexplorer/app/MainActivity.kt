package com.droidexplorer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.droidexplorer.feature.files.ExplorerScreen
import com.droidexplorer.ui.design.DroidExplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroidExplorerTheme {
                ExplorerScreen()
            }
        }
    }
}

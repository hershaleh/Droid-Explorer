package com.droidexplorer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.droidexplorer.ui.design.DroidExplorerTheme
import com.droidexplorer.ui.design.GalleryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroidExplorerTheme {
                GalleryScreen()
            }
        }
    }
}

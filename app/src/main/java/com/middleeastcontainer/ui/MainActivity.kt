package com.middleeastcontainer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.middleeastcontainer.ui.navigation.MecrcNavGraph
import com.middleeastcontainer.ui.theme.MecrcTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single-activity Compose host. All legacy Activities become Compose destinations. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MecrcTheme {
                Surface(Modifier.fillMaxSize()) {
                    MecrcNavGraph(navController = rememberNavController())
                }
            }
        }
    }
}

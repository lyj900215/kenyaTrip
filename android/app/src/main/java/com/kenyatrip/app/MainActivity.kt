package com.kenyatrip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kenyatrip.app.ui.AppViewModel
import com.kenyatrip.app.ui.KenyaTripNavHost
import com.kenyatrip.app.ui.theme.KenyaTripTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KenyaTripTheme {
                KenyaTripNavHost(vm)
            }
        }
    }
}

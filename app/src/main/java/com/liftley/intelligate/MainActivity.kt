package com.liftley.intelligate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liftley.intelligate.ui.scan.ScanRoute
import com.liftley.intelligate.ui.theme.IntelliGateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IntelliGateTheme { ScanRoute() } }
    }
}

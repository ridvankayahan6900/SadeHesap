package com.ridvan.sadehesap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ridvan.sadehesap.ui.HesapEkrani
import com.ridvan.sadehesap.ui.theme.SadeHesapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SadeHesapTheme {
                HesapEkrani()
            }
        }
    }
}

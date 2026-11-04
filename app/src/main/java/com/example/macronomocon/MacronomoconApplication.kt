package com.example.macronomocon

import android.app.Application
import com.example.macronomocon.di.AppContainer
import com.example.macronomocon.di.AuthAppContainer

class MacronomoconApplication : Application() {
    val container: AppContainer by lazy { AuthAppContainer(this) }
}

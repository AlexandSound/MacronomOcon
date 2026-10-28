package com.example.macronomocon

import android.app.Application
import com.example.macronomocon.data.mock.MockAppContainer
import com.example.macronomocon.di.AppContainer

class MacronomoconApplication : Application() {
    // Explicit demo composition root. Replace this binding when backend transports are available.
    val container: AppContainer by lazy { MockAppContainer.getInstance(resources) }
}

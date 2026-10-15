package com.rajdeepkonwar.bareplayer

import android.app.Application

class BareApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

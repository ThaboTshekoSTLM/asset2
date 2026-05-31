package za.gov.municipal.ictasset

import android.app.Application
import za.gov.municipal.ictasset.di.AppContainer

class ICTAssetApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }
}
